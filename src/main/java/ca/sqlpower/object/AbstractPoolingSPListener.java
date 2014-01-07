/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.object;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import ca.sqlpower.object.SPChildEvent.EventType;
import ca.sqlpower.util.SQLPowerUtils;
import ca.sqlpower.util.TransactionEvent;
import ca.sqlpower.util.TransactionEvent.TransactionState;

/**
 * Extend this class to add the behaviour to not respond to events when in a
 * transaction. Instead of responding to events while in a transaction the
 * listener will collect the events and then at the end of the transaction act
 * on each event in the order it was received. If a rollback event was received
 * the events will be discarded instead.
 */
public abstract class AbstractPoolingSPListener implements SPListener {

	private static final Logger logger = Logger
			.getLogger(AbstractPoolingSPListener.class);
	
    /**
     * Tracks the objects this listener is attached to that are in the middle of
     * a transaction. If the map stores a number greater than 0 for a given
     * object it is in a transaction state. If the set does not contain the
     * object it is not in a transaction. This is a map as one instance of this
     * object could be attached to several objects as a hierarchy listener.
     */
    private final Map<SPObject, Integer> inTransactionMap = 
        new IdentityHashMap<SPObject, Integer>();
    
    /**
	 * This creation counter is used for the creation time of the
	 * {@link EventObject}s so we can provide a specific order. There shouldn't
	 * be the case where two threads are firing events to the listener at the
	 * same time. If this case should arise it shouldn't matter who is first for
	 * the counter as it would end in a race condition in other locations.
	 */
    private long creationCounter = 0;
    
    /**
	 * This object stores the events (child added, removed, or property change)
	 * and the index of when the change occurred. We need to store when the
	 * change occurred in the unusual case where we have a parent and child both
	 * in a transaction and operations are occuring to both objects. In this
	 * case we want to preserve the order as we can't say all parent events must
	 * come before the child events (cases where the child has a descendant
	 * added then the parent refers to the descendant) nor can we say the child
	 * events come before the parents (a sibling of the child is added to the
	 * parent and the child refers to its new sibling).
	 */
    private class EventObject {
    	
    	private final Object event;
    	private final long creationTime;

		public EventObject(Object event) {
			this.event = event;
			creationTime = getCurrentCreationTime();
    	}
		
		public Object getEvent() {
			return event;
		}
		
		public long getCreationTime() {
			return creationTime;
		}
		
    }
    
    /**
     * Tracks the e that occur while an object is in a transaction state. These
     * events will be acted on when the transaction ends or are removed when the
     * transaction rolls back. The events can be {@link PropertyChangeEvent}s or
     * {@link SPChildEvent}s.
     */
    private final Map<SPObject, List<EventObject>> eventMap = 
        new IdentityHashMap<SPObject, List<EventObject>>();
    
    private final Multimap<SPObject, SPObject> ancestorTransactionMap = 
    		ArrayListMultimap.create();
    
    private long getCurrentCreationTime() {
    	creationCounter++;
    	return creationCounter;
    }

    public final void transactionEnded(TransactionEvent e) {
    	Integer lastTransactionCount;
        SPObject source = (SPObject) e.getSource();
		if (errorOnDanglingCommit && inTransactionMap.get(source) == null) {
            throw new IllegalStateException("An end transaction for object " + source 
                    + " of type " + source.getClass() + " was called while it was " +
            		"not in a transaction.");
        } else if (!errorOnDanglingCommit && inTransactionMap.get(source) == null) {
            return;
        } else {
        	lastTransactionCount = inTransactionMap.get(source);
        	logger.debug("Transaction count on " + this +  " for:" + source + ": " + inTransactionMap.get(source));
        }
        Integer nestedTransactionCount = lastTransactionCount - 1;
        if (nestedTransactionCount < 0) {
            throw new IllegalStateException("The transaction count was not removed properly.");
        } else if (nestedTransactionCount > 0) {
            inTransactionMap.put(source, nestedTransactionCount);
            transactionEndedImpl(e);
        } else {
            inTransactionMap.remove(source);
            
            if (!ancestorInTransaction(source)) {
            	handlePooledEvents(source);
            	transactionEndedImpl(e);
            } else {
            	addEventToMap(e, source);
            }
            
            finalCommitImpl(e);
        }
    }

	private void handlePooledEvents(SPObject source) {
		List<EventObject> eventsForSource = collectPooledEvents(source);
		Collections.sort(eventsForSource, new Comparator<EventObject>() {
			@Override
			public int compare(EventObject e1, EventObject e2) {
				return Long.valueOf(e1.getCreationTime()).compareTo(Long.valueOf(e2.getCreationTime()));
			}
		});
		
		for (EventObject event : eventsForSource) {
			Object evt = event.getEvent();
			if (evt instanceof PropertyChangeEvent) {
				propertyChangeImpl((PropertyChangeEvent) evt);
			} else if (evt instanceof SPChildEvent) {
				SPChildEvent childEvent = (SPChildEvent) evt;
				if (childEvent.getType().equals(EventType.ADDED)) {
					childAddedImpl(childEvent);
				} else if (childEvent.getType().equals(EventType.REMOVED)) {
					childRemovedImpl(childEvent);
				} else {
					throw new IllegalStateException("Unknown child event of type " + childEvent.getType());
				}
			} else if (evt instanceof TransactionEvent) {
				TransactionEvent transEvt = (TransactionEvent) evt;
				if (transEvt.getState().equals(TransactionState.END)) {
					transactionEndedImpl(transEvt);
				} else {
					throw new IllegalStateException("Unknown transaction event of type " + transEvt.getState());
				}
			} else {
				throw new IllegalStateException("Unknown event type " + evt.getClass());
			}
		}
	}

	private List<EventObject> collectPooledEvents(SPObject source) {
		List<EventObject> eventsForSource = new ArrayList<EventObject>();
		if (eventMap.get(source) != null) {
			//Copy of event list in case listener receiving events causes other events.
			eventsForSource.addAll(eventMap.get(source));
			eventMap.remove(source);
		}
		if (ancestorTransactionMap.get(source) != null) {
			for (SPObject childSource : ancestorTransactionMap.get(source)) {
				if (!isInTransaction(childSource)) {
					eventsForSource.addAll(collectPooledEvents(childSource));
				}
			}
			ancestorTransactionMap.removeAll(source);
		}
		return eventsForSource;
	}
    
    /**
     * For almost all pooling listeners we want an error to occur if there
     * is a commit outside of a transaction. However, in some specific listeners
     * it is possible to add them to a parent object inside of a transaction
     * which always results in a dangling commit at the end. For these specific
     * cases we will disable the error.
     */
    private final boolean errorOnDanglingCommit;
    
    public AbstractPoolingSPListener() {
        errorOnDanglingCommit = true;
    }
    
    public AbstractPoolingSPListener(boolean errorOnDanglingCommit) {
        this.errorOnDanglingCommit = errorOnDanglingCommit;
    }
    
    /**
     * Override this method if an action is required when a transaction ends.
     * This will be called when any transactionEnded event is fired, even if
     * it is the end of a transaction that is contained in another transaction.
     */
    protected void transactionEndedImpl(TransactionEvent e) {
        //meant to be overridden by classes extending this listener
    }

	/**
	 * Override this method if an action is required when the final transaction
	 * ends. This will only be called when the outermost transactionEnded event
	 * is fired. Note that transactionEndedImpl will be called before this
	 * method.
	 */
    protected void finalCommitImpl(TransactionEvent e) {
    	//meant to be overridden by classes extending this listener
    }

    public final void transactionRollback(TransactionEvent e) {
        inTransactionMap.remove(e.getSource());
        eventMap.remove(e.getSource());
        ancestorTransactionMap.removeAll(e.getSource());
        transactionRollbackImpl(e);
    }
    
    /**
     * Returns true if at least one ancestor is in a transaction.
     */
    private boolean ancestorInTransaction(SPObject obj) {
    	List<SPObject> ancestors = SQLPowerUtils.getAncestorList(obj);
    	for (SPObject ancestor : ancestors) {
    		if (isInTransaction(ancestor)) return true;
    	}
    	return false;
    }

	private boolean isInTransaction(SPObject ancestor) {
		return inTransactionMap.get(ancestor) != null 
		    && inTransactionMap.get(ancestor) > 0;
	}

    /**
     * Connects the given object to it's closest's ancestor's transaction.
     */
    private void addAncestorTransaction(SPObject obj) {
    	List<SPObject> ancestors = SQLPowerUtils.getAncestorList(obj);
    	Collections.reverse(ancestors);
    	for (SPObject ancestor : ancestors) {
    		if (isInTransaction(ancestor)) {
    			ancestorTransactionMap.put(ancestor, obj);
    			return;
    		}
    	}
    }
    
    /**
     * Override this method if an action is required when a transaction rolls back.
     */
    protected void transactionRollbackImpl(TransactionEvent e) {
        //meant to be overridden by classes extending this listener
    }

    public final void transactionStarted(TransactionEvent e) {
        Integer transactionCount = inTransactionMap.get(e.getSource());
        SPObject source = (SPObject) e.getSource();
		if (transactionCount == null) {
            inTransactionMap.put(source, 1);
        } else {
            inTransactionMap.put(source, transactionCount + 1);
        }
		if (ancestorInTransaction(source)) {
			addAncestorTransaction(source);
		}
        logger.debug("Transaction count on " + this +  " for:" + e.getSource() + ": " + inTransactionMap.get(source));
        transactionStartedImpl(e);
    }
    
    /**
     * Override this method if an action is required when a transaction starts.
     * This will be called when any transactionStarted event is fired, even if
     * it is the start of a transaction that is contained in another transaction.
     */
    protected void transactionStartedImpl(TransactionEvent e) {
        //meant to be overridden by classes extending this listener
    }

    public final void childAdded(SPChildEvent e) {
        SPObject source = e.getSource();
        boolean ancestorTrans = ancestorInTransaction(source);
		if (ancestorTrans || isInTransaction(source)) {
			if (ancestorTrans) {
				addAncestorTransaction(source);
			}
            addEventToMap(e, source);
        } else {
            childAddedImpl(e);
        }
    }
    
    /**
     * Override this method if an action is required when a child added event is
     * acted upon.
     */
    protected void childAddedImpl(SPChildEvent e) {
        //meant to be overridden by classes extending this listener
    }

    public final void childRemoved(SPChildEvent e) {
        SPObject source = e.getSource();
        boolean ancestorTrans = ancestorInTransaction(source);
		if (ancestorTrans || isInTransaction(source)) {
			if (ancestorTrans) {
				addAncestorTransaction(source);
			}
            addEventToMap(e, source);
        } else {
            childRemovedImpl(e);
        }
    }

	private void addEventToMap(Object e, SPObject source) {
		List<EventObject> events = eventMap.get(source);
		if (events == null) {
		    events = new ArrayList<EventObject>();
		    eventMap.put(source, events);
		}
		events.add(new EventObject(e));
	}
    
    /**
     * Override this method if an action is required when a child removed event is
     * acted upon.
     */
    protected void childRemovedImpl(SPChildEvent e) {
        //meant to be overridden by classes extending this listener
    }

    public final void propertyChanged(PropertyChangeEvent evt) {
        SPObject source = (SPObject) evt.getSource();
        boolean ancestorTrans = ancestorInTransaction(source);
		if (ancestorTrans || isInTransaction(source)) {
			if (ancestorTrans) {
				addAncestorTransaction(source);
			}
            addEventToMap(evt, source);
        } else {
            propertyChangeImpl(evt);
        }
    }
    
    /**
     * Override this method if an action is required when a property change event is
     * acted upon.
     */
    protected void propertyChangeImpl(PropertyChangeEvent evt) {
        //meant to be overridden by classes extending this listener
    }

}
