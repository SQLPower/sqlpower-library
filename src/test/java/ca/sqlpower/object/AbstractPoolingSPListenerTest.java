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
import java.util.List;

import junit.framework.TestCase;
import ca.sqlpower.util.TransactionEvent;

import com.google.common.collect.ImmutableList;

public class AbstractPoolingSPListenerTest extends TestCase {
    
    /**
     * This listener tracks the order events were handled.
     */
    private static class ExecutionOrderListener extends AbstractPoolingSPListener {

        /**
         * A list of events that were handled in the order they were handled by
         * this listener. Used to test that events are handled in the correct order
         * when in a transaction block.
         */
        private final List<Object> eventsInOrder = new ArrayList<Object>();
        
        @Override
        protected void transactionStartedImpl(TransactionEvent e) {
            eventsInOrder.add(e);
        }
        
        @Override
        protected void transactionEndedImpl(TransactionEvent e) {
            eventsInOrder.add(e);
        }
        
        @Override
        protected void transactionRollbackImpl(TransactionEvent e) {
            eventsInOrder.add(e);
        }
        
        @Override
        protected void propertyChangeImpl(PropertyChangeEvent evt) {
            eventsInOrder.add(evt);
        }
        
        @Override
        protected void childAddedImpl(SPChildEvent e) {
            eventsInOrder.add(e);
        }
        
        @Override
        protected void childRemovedImpl(SPChildEvent e) {
            eventsInOrder.add(e);
        }
        
        public List<Object> getEventsInOrder() {
            return Collections.unmodifiableList(eventsInOrder);
        }
    }
    
    public static class StubSPObject extends AbstractSPObject {
    	
    	public static final List<Class<? extends SPObject>> allowedChildTypes =
    			new ImmutableList.Builder<Class<? extends SPObject>>()
    				.add(StubSPObject.class)
    				.build();
    	
    	private final List<StubSPObject> children = new ArrayList<StubSPObject>();

        @Override
        protected boolean removeChildImpl(SPObject child) {
        	return children.remove(child);
        }
        
        @Override
        protected void addChildImpl(SPObject child, int index) {
        	children.add(index, (StubSPObject) child);
        }

        public boolean allowsChildren() {
            return true;
        }

        public int childPositionOffset(
                Class<? extends SPObject> childType) {
            return 0;
        }

        public List<? extends SPObject> getChildren() {
            return children;
        }

        public List<SPObject> getDependencies() {
            return Collections.emptyList();
        }

        public void removeDependency(SPObject dependency) {
            //do nothing
        }
        
        public List<Class<? extends SPObject>> getAllowedChildTypes() {
        	return allowedChildTypes;
        }

    };
    
    private final AbstractSPObject wo = new StubSPObject();

    /**
     * This tests that events fired.
     */
    public void testTransactionActsOnEvents() throws Exception {
        ExecutionOrderListener listener = new ExecutionOrderListener();
        wo.addSPListener(listener);
        SPChildEvent event1 = wo.fireChildAdded(SPObject.class, new StubSPObject(), 0);
        SPChildEvent event2 = wo.fireChildRemoved(SPObject.class, new StubSPObject(), 0);
        PropertyChangeEvent event3 = wo.firePropertyChange("dummyProperty", "oldValue", "newValue");
        
        List<Object> eventsInOrder = listener.getEventsInOrder();
        assertEquals(event1, eventsInOrder.get(0));
        assertEquals(event2, eventsInOrder.get(1));
        assertEquals(event3, eventsInOrder.get(2));
    }
    
    /**
     * This tests that events are fired  while in a transaction are acted
     * on when the transaction completes successfully.
     */
    public void testTransactionEndActsOnEvents() throws Exception {
        ExecutionOrderListener listener = new ExecutionOrderListener();
        wo.addSPListener(listener);
        TransactionEvent event1 = wo.fireTransactionStarted("Start");
        PropertyChangeEvent event2 = wo.firePropertyChange("dummyProperty", "oldValue", "newValue");
        SPChildEvent event3 = wo.fireChildAdded(SPObject.class, new StubSPObject(), 0);
        SPChildEvent event4 = wo.fireChildRemoved(SPObject.class, new StubSPObject(), 0);
        PropertyChangeEvent event5 = wo.firePropertyChange("dummyProperty", "oldValue", "newValue");
        
        assertEquals(1, listener.getEventsInOrder().size());
        
        TransactionEvent event6 = wo.fireTransactionEnded();
        
        List<Object> eventsInOrder = listener.getEventsInOrder();
        assertEquals(event1, eventsInOrder.get(0));
        assertEquals(event2, eventsInOrder.get(1));
        assertEquals(event3, eventsInOrder.get(2));
        assertEquals(event4, eventsInOrder.get(3));
        assertEquals(event5, eventsInOrder.get(4));
        assertEquals(event6, eventsInOrder.get(5));
    }
    
    /**
     * This tests that events are fired  while in a transaction are acted
     * on when the parent transaction completes successfully.
     */
    public void testParentTransactionEndActsOnEvents() throws Exception {
        ExecutionOrderListener listener = new ExecutionOrderListener();
        AbstractSPObject parent = new StubSPObject();
        parent.addChild(wo);
        wo.addSPListener(listener);
        parent.addSPListener(listener);
        assertTrue(parent.getChildren().contains(wo));
        TransactionEvent event1 = parent.fireTransactionStarted("Start");
        PropertyChangeEvent event2 = wo.firePropertyChange("dummyProperty", "oldValue", "newValue");
        SPChildEvent event3 = wo.fireChildAdded(SPObject.class, new StubSPObject(), 0);
        SPChildEvent event4 = wo.fireChildRemoved(SPObject.class, new StubSPObject(), 0);
        PropertyChangeEvent event5 = wo.firePropertyChange("dummyProperty", "oldValue", "newValue");
        
        assertEquals("Only the start event should be acted on at this point", 1, listener.getEventsInOrder().size());
        
        TransactionEvent event6 = parent.fireTransactionEnded();
        
        assertEquals("All events should be in the list once the transaction has ended", 6, listener.getEventsInOrder().size());
        List<Object> eventsInOrder = listener.getEventsInOrder();
        assertEquals(event1, eventsInOrder.get(0));
        assertEquals(event2, eventsInOrder.get(1));
        assertEquals(event3, eventsInOrder.get(2));
        assertEquals(event4, eventsInOrder.get(3));
        assertEquals(event5, eventsInOrder.get(4));
        assertEquals(event6, eventsInOrder.get(5));
    }
    
    /**
	 * This tests that events are fired while in a transaction are acted on when
	 * the parent transaction completes successfully. This also interleaves
	 * parent and child events so we know the result is the correct order as
	 * input.
	 */
    public void testParentInterleavedTransactionEndActsOnEvents() throws Exception {
        ExecutionOrderListener listener = new ExecutionOrderListener();
        AbstractSPObject parent = new StubSPObject();
        parent.addChild(wo);
        wo.addSPListener(listener);
        parent.addSPListener(listener);
        assertTrue(parent.getChildren().contains(wo));
        TransactionEvent event1 = parent.fireTransactionStarted("Start");
        PropertyChangeEvent event2 = wo.firePropertyChange("dummyProperty", "oldValue", "newValue");
        PropertyChangeEvent event3 = parent.firePropertyChange("dummyProperty", "oldValue", "newValue");
        SPChildEvent event4 = wo.fireChildAdded(SPObject.class, new StubSPObject(), 0);
        SPChildEvent event5 = wo.fireChildRemoved(SPObject.class, new StubSPObject(), 0);
        PropertyChangeEvent event6 = parent.firePropertyChange("dummyProperty", "oldValue", "newValue");
        PropertyChangeEvent event7 = wo.firePropertyChange("dummyProperty", "oldValue", "newValue");
        PropertyChangeEvent event8 = parent.firePropertyChange("dummyProperty", "oldValue", "newValue");
        
        assertEquals("Only the start event should be acted on at this point", 1, listener.getEventsInOrder().size());
        
        TransactionEvent event9 = parent.fireTransactionEnded();
        
        assertEquals("All events should be in the list once the transaction has ended", 9, listener.getEventsInOrder().size());
        List<Object> eventsInOrder = listener.getEventsInOrder();
        assertEquals(event1, eventsInOrder.get(0));
        assertEquals(event2, eventsInOrder.get(1));
        assertEquals(event3, eventsInOrder.get(2));
        assertEquals(event4, eventsInOrder.get(3));
        assertEquals(event5, eventsInOrder.get(4));
        assertEquals(event6, eventsInOrder.get(5));
        assertEquals(event7, eventsInOrder.get(6));
        assertEquals(event8, eventsInOrder.get(7));
        assertEquals(event9, eventsInOrder.get(8));
    }
    
    /**
	 * This tests that events are fired while in a transaction are acted on when
	 * the ancestor transactions completes successfully.<br>
	 * This test has both the parent of the event object and a grand parent
	 * causing transactions. For this case the parent transaction wraps the
	 * grandparent transaction.
	 */
    public void testParentGrandParentTransactionEndActsOnEvents() throws Exception {
        ExecutionOrderListener listener = new ExecutionOrderListener();
        AbstractSPObject parent = new StubSPObject();
        parent.addChild(wo);
        assertTrue(parent.getChildren().contains(wo));
        
        AbstractSPObject grandParent = new StubSPObject();
        grandParent.addChild(parent);
        assertTrue(grandParent.getChildren().contains(parent));
        
        wo.addSPListener(listener);
        parent.addSPListener(listener);
        grandParent.addSPListener(listener);
        
        TransactionEvent event1 = parent.fireTransactionStarted("Start");
        TransactionEvent event2 = grandParent.fireTransactionStarted("Start");
        PropertyChangeEvent event3 = wo.firePropertyChange("dummyProperty", "oldValue", "newValue");
        SPChildEvent event4 = wo.fireChildAdded(SPObject.class, new StubSPObject(), 0);
        SPChildEvent event5 = wo.fireChildRemoved(SPObject.class, new StubSPObject(), 0);
        PropertyChangeEvent event6 = wo.firePropertyChange("dummyProperty", "oldValue", "newValue");
        
        assertEquals("Only the start event should be acted on at this point", 2, listener.getEventsInOrder().size());
        
        TransactionEvent event7 = grandParent.fireTransactionEnded();
        
        //We allow the grandparent transaction end here and all grandparent changes. If we wanted the grandparent changes interleaved
        //with the other changes we should have the grandparent transaction start first.
        assertEquals("Only the start event should be acted on at this point", 3, listener.getEventsInOrder().size());
        
        TransactionEvent event8 = parent.fireTransactionEnded();
        
        assertEquals("All events should be in the list once the transaction has ended", 8, listener.getEventsInOrder().size());
        List<Object> eventsInOrder = listener.getEventsInOrder();
        assertEquals(event1, eventsInOrder.get(0));
        assertEquals(event2, eventsInOrder.get(1));
        assertEquals(event7, eventsInOrder.get(2));
        assertEquals(event3, eventsInOrder.get(3));
        assertEquals(event4, eventsInOrder.get(4));
        assertEquals(event5, eventsInOrder.get(5));
        assertEquals(event6, eventsInOrder.get(6));
        assertEquals(event8, eventsInOrder.get(7));
    }
    
    /**
	 * This tests that events are fired while in a transaction are acted on when
	 * the ancestor transactions completes successfully.<br>
	 * This test has both the parent of the event object and a grand parent
	 * causing transactions. For this case the grand parent transaction wraps the
	 * parent transaction.
	 */
    public void testGrandParentParentTransactionEndActsOnEvents() throws Exception {
        ExecutionOrderListener listener = new ExecutionOrderListener();
        AbstractSPObject parent = new StubSPObject();
        parent.addChild(wo);
        assertTrue(parent.getChildren().contains(wo));
        
        AbstractSPObject grandParent = new StubSPObject();
        grandParent.addChild(parent);
        assertTrue(grandParent.getChildren().contains(parent));
        
        wo.addSPListener(listener);
        parent.addSPListener(listener);
        grandParent.addSPListener(listener);
        
        TransactionEvent event1 = grandParent.fireTransactionStarted("Start");
        TransactionEvent event2 = parent.fireTransactionStarted("Start");
        PropertyChangeEvent event3 = wo.firePropertyChange("dummyProperty", "oldValue", "newValue");
        SPChildEvent event4 = wo.fireChildAdded(SPObject.class, new StubSPObject(), 0);
        SPChildEvent event5 = wo.fireChildRemoved(SPObject.class, new StubSPObject(), 0);
        PropertyChangeEvent event6 = wo.firePropertyChange("dummyProperty", "oldValue", "newValue");
        
        assertEquals("Only the start event should be acted on at this point", 2, listener.getEventsInOrder().size());
        
        TransactionEvent event7 = parent.fireTransactionEnded();
        
        assertEquals("Only the start event should be acted on at this point", 2, listener.getEventsInOrder().size());
        
        TransactionEvent event8 = grandParent.fireTransactionEnded();
        
        assertEquals("All events should be in the list once the transaction has ended", 8, listener.getEventsInOrder().size());
        List<Object> eventsInOrder = listener.getEventsInOrder();
        assertEquals(event1, eventsInOrder.get(0));
        assertEquals(event2, eventsInOrder.get(1));
        assertEquals(event3, eventsInOrder.get(2));
        assertEquals(event4, eventsInOrder.get(3));
        assertEquals(event5, eventsInOrder.get(4));
        assertEquals(event6, eventsInOrder.get(5));
        assertEquals(event7, eventsInOrder.get(6));
        assertEquals(event8, eventsInOrder.get(7));
    }
    
    public void testTransactionRollbackDoesNotActOnEvents() throws Exception {
        ExecutionOrderListener listener = new ExecutionOrderListener();
        wo.addSPListener(listener);
        TransactionEvent event1 = wo.fireTransactionStarted("Start");
        wo.firePropertyChange("dummyProperty", "oldValue", "newValue");
        wo.fireChildAdded(SPObject.class, new StubSPObject(), 0);
        wo.fireChildRemoved(SPObject.class, new StubSPObject(), 0);
        wo.firePropertyChange("dummyProperty", "oldValue", "newValue");
        
        assertEquals(1, listener.getEventsInOrder().size());
        
        TransactionEvent event2 = wo.fireTransactionRollback("Rollback");
        
        assertEquals(2, listener.getEventsInOrder().size());
        assertEquals(event1, listener.getEventsInOrder().get(0));
        assertEquals(event2, listener.getEventsInOrder().get(1));
    }
}
