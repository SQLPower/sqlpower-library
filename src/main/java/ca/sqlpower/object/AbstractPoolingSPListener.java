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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.sqlpower.object.SPChildEvent.EventType;
import ca.sqlpower.util.TransactionEvent;

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
     * Tracks the e that occur while an object is in a transaction state. These
     * events will be acted on when the transaction ends or are removed when the
     * transaction rolls back. The events can be {@link PropertyChangeEvent}s or
     * {@link SPChildEvent}s.
     */
    private final Map<SPObject, List<Object>> eventMap = 
        new IdentityHashMap<SPObject, List<Object>>();

    public final void transactionEnded(TransactionEvent e) {
    	Integer lastTransactionCount;
        if (errorOnDanglingCommit && inTransactionMap.get(e.getSource()) == null) {
            throw new IllegalStateException("An end transaction for object " + e.getSource() 
                    + " of type " + e.getSource().getClass() + " was called while it was " +
            		"not in a transaction.");
        } else if (!errorOnDanglingCommit && inTransactionMap.get(e.getSource()) == null) {
            return;
        } else {
        	lastTransactionCount = inTransactionMap.get(e.getSource());
        	logger.debug("Transaction count on " + this +  " for:" + e.getSource() + ": " + inTransactionMap.get((SPObject) e.getSource()));
        }
        Integer nestedTransactionCount = lastTransactionCount - 1;
        if (nestedTransactionCount < 0) {
            throw new IllegalStateException("The transaction count was not removed properly.");
        } else if (nestedTransactionCount > 0) {
            inTransactionMap.put((SPObject) e.getSource(), nestedTransactionCount);
            transactionEndedImpl(e);
        } else {
            inTransactionMap.remove(e.getSource());
            if (eventMap.get(e.getSource()) != null) {
            	//Copy of event list in case listener receiving events causes other events.
            	List<Object> eventsForSource = new ArrayList<Object>(eventMap.get(e.getSource()));
            	eventMap.remove(e.getSource());
                for (Object evt : eventsForSource) {
                    if (evt instanceof PropertyChangeEvent) {
                        propertyChangeImpl((PropertyChangeEvent) evt);
                    } else if (evt instanceof SPChildEvent) {
                        SPChildEvent childEvent = (SPChildEvent) evt;
                        if (childEvent.getType().equals(EventType.ADDED)) {
                            childAddedImpl(childEvent);
                        } else if (childEvent.getType().equals(EventType.REMOVED)) {
                            childRemovedImpl(childEvent);
                        } else {
                            throw new IllegalStateException("Unknown wabit child event of type " + childEvent.getType());
                        }
                    } else {
                        throw new IllegalStateException("Unknown event type " + evt.getClass());
                    }
                }
            }
            
            transactionEndedImpl(e);
            finalCommitImpl(e);
        }
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
        transactionRollbackImpl(e);
    }
    
    /**
     * Override this method if an action is required when a transaction rolls back.
     */
    protected void transactionRollbackImpl(TransactionEvent e) {
        //meant to be overridden by classes extending this listener
    }

    public final void transactionStarted(TransactionEvent e) {
        Integer transactionCount = inTransactionMap.get(e.getSource());
        if (transactionCount == null) {
            inTransactionMap.put((SPObject) e.getSource(), 1);
        } else {
            inTransactionMap.put((SPObject) e.getSource(), transactionCount + 1);
        }
        logger.debug("Transaction count on " + this +  " for:" + e.getSource() + ": " + inTransactionMap.get((SPObject) e.getSource()));
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
        if (inTransactionMap.get(e.getSource()) != null 
                && inTransactionMap.get(e.getSource()) > 0) {
            List<Object> events = eventMap.get(e.getSource());
            if (events == null) {
                events = new ArrayList<Object>();
                eventMap.put(e.getSource(), events);
            }
            events.add(e);
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
        if (inTransactionMap.get(e.getSource()) != null 
                && inTransactionMap.get(e.getSource()) > 0) {
            List<Object> events = eventMap.get(e.getSource());
            if (events == null) {
                events = new ArrayList<Object>();
                eventMap.put(e.getSource(), events);
            }
            events.add(e);
        } else {
            childRemovedImpl(e);
        }
    }
    
    /**
     * Override this method if an action is required when a child removed event is
     * acted upon.
     */
    protected void childRemovedImpl(SPChildEvent e) {
        //meant to be overridden by classes extending this listener
    }

    public final void propertyChanged(PropertyChangeEvent evt) {
        if (inTransactionMap.get(evt.getSource()) != null 
                && inTransactionMap.get(evt.getSource()) > 0) {
            List<Object> events = eventMap.get(evt.getSource());
            if (events == null) {
                events = new ArrayList<Object>();
                eventMap.put((SPObject) evt.getSource(), events);
            }
            events.add(evt);
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
