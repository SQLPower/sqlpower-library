/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.sqlobject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPObject;

public class SQLObjectTest extends BaseSQLObjectTestCase {

	public SQLObjectTest(String name) throws Exception {
        super(name);
    }

    SQLObject target;
	
	private static class SQLObjectImpl extends SQLObject {
		private List<SQLObject> children = new ArrayList<SQLObject>();
	    protected boolean allowsChildren = false;
		SQLObjectImpl() {
		}
		SQLObject parent = null;

		@Override
		public SQLObject getParent() {
			return parent;
		}
		@Override
		public void setParent(SPObject parent) {
			this.parent = (SQLObject) parent;
		}
		@Override
		protected void populateImpl() throws SQLObjectException {
			// System.err.println("Abstract test stub populate() invoked");
		}
		@Override
		public String getShortDisplayName() {
            return "short display name";
		}
		@Override
		public boolean allowsChildren() {
			//throw new RuntimeException("test abstract stub");
			return allowsChildren;	 // Used by setChildren().
		}
		
		// manually call fireDbObjecChanged, so it can be tested.
		public void fakeObjectChanged(String string,Object oldValue, Object newValue) {
			
			firePropertyChange(string,oldValue,newValue);
		}
		
		@Override
		public Class<? extends SQLObject> getChildType() {
			return SQLObject.class;
		}
		@Override
		public List<? extends SQLObject> getChildrenWithoutPopulating() {
			return Collections.unmodifiableList(children);
		}
		@Override
		protected boolean removeChildImpl(SPObject child) {
			int index = children.indexOf(child);
			if (index != -1) {
				children.remove(index);
				child.setParent(null);
				fireChildRemoved(child.getClass(), child, index);
				return true;
			}
			return false;
		}
		public int childPositionOffset(Class<? extends SPObject> childType) {
			return 0;
		}
		public List<? extends SPObject> getDependencies() {
			return Collections.emptyList();
		}
		public void removeDependency(SPObject dependency) {
			// no-op
		}
		@Override
		protected void addChildImpl(SPObject child, int index) {
			if (!allowsChildren) {
				throw new IllegalStateException("Cannot add child as this " +
						"SQLObjectImpl object does not allow children.");
			}
			children.add(index, (SQLObject) child);
			child.setParent(this);
			fireChildAdded(child.getClass(), child, index);
		}
	}
	
	public void setUp() throws Exception {
        super.setUp();
		target = new SQLObjectImpl();
	}
    
    @Override
    protected SQLObject getSQLObjectUnderTest() throws SQLObjectException {
        return target;
    }

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.isPopulated()'
	 */
	public final void testIsPopulated() {
		assertFalse(target.isPopulated());
	}

	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.setPopulated(boolean)'
	 */
	public final void testSetPopulated() {
		target.setPopulated(true);
		assertTrue(target.isPopulated());
	}
	
	/*
	 * Test method for 'ca.sqlpower.sqlobject.SQLObject.setChildren(List)'
	 * Note that setChildren copies elements, does not assign the list, and
	 * getChildren returns an unmodifiable copy of the current list.
	 */
	public final void testAllChildHandlingMethods() throws SQLObjectException, IllegalArgumentException, ObjectDependentException {
		((SQLObjectImpl) target).allowsChildren = true;
		assertEquals(0, target.getChildCount());

		SQLObject x = new SQLObjectImpl();
		
		target.addChild(x);
		assertEquals(1, target.getChildCount());
		assertEquals(x, target.getChild(0));
		
		SQLObject y = new SQLObjectImpl();
		
		// Test addChild(SQLObject, int)
		target.addChild(y, 0);
		assertEquals(y, target.getChild(0));
		assertEquals(x, target.getChild(1));
		
		target.removeChild(x);
		List<SQLObject> list2 = new LinkedList<SQLObject>();
		list2.add(y);
		assertEquals(list2, target.getChildren());
		
		target.removeChild(y);
		assertEquals(Collections.EMPTY_LIST, target.getChildren());
	}

	public final void testFiresAddEvent() throws SQLObjectException {
		CountingSQLObjectListener l = new CountingSQLObjectListener();
		target.addSPListener(l);
		((SQLObjectImpl) target).allowsChildren = true;
		
		final SQLObjectImpl objectImpl = new SQLObjectImpl();
		target.addChild(objectImpl);
		assertEquals(1, l.getInsertedCount());
        assertEquals(0, l.getRemovedCount());
        assertEquals(0, l.getChangedCount());
        assertEquals(0, l.getStructureChangedCount());
    }

    public void testFireChangeEvent() throws Exception {
        CountingSQLObjectListener l = new CountingSQLObjectListener();
        target.addSPListener(l);

        ((SQLObjectImpl)target).fakeObjectChanged("fred","old value","new value");
        assertEquals(0, l.getInsertedCount());
        assertEquals(0, l.getRemovedCount());
        assertEquals(1, l.getChangedCount());
        assertEquals(0, l.getStructureChangedCount());
    }
    
    /** make sure "change" to same value doesn't fire useless event */
    public void testDontFireChangeEvent() throws Exception {
        CountingSQLObjectListener l = new CountingSQLObjectListener();
        target.addSPListener(l);

        ((SQLObjectImpl)target).fakeObjectChanged("fred","old value","old value");
        assertEquals(0, l.getInsertedCount());
        assertEquals(0, l.getRemovedCount());
        assertEquals(0, l.getChangedCount());
        assertEquals(0, l.getStructureChangedCount());
    }

    public void testFireStructureChangeEvent() throws Exception {
        CountingSQLObjectListener l = new CountingSQLObjectListener();
        target.addSPListener(l);
        assertEquals(0, l.getInsertedCount());
        assertEquals(0, l.getRemovedCount());
        assertEquals(0, l.getChangedCount());
    }
    
    public void testAddRemoveListener() {
        CountingSQLObjectListener l = new CountingSQLObjectListener();
        
        target.addSPListener(l);
        assertEquals(1, target.getSPListeners().size());
		
        target.removeSPListener(l);
		assertEquals(0, target.getSPListeners().size());
	}
	
	public void testAllowMixedChildrenThatAreSubclassesOfEachOther() throws Exception {
		((SQLObjectImpl) target).allowsChildren = true;
		SQLObject subImpl = new SQLObjectImpl() {};
		target.addChild(new SQLObjectImpl());
		target.addChild(subImpl);
		
		// now test the other direction
		target.removeChild(target.getChild(0));
		target.addChild(new SQLObjectImpl());
        
        // test passes if no exceptions were thrown
	}
	
    public void testPreRemoveEventNoVeto() throws Exception {
    	((SQLObjectImpl) target).allowsChildren = true;
        target.addChild(new SQLObjectImpl());

        CountingSQLObjectPreEventListener l = new CountingSQLObjectPreEventListener();
        target.addSQLObjectPreEventListener(l);
        
        l.setVetoing(false);
        
        target.removeChild(target.getChild(0));
        
        assertEquals("Event fired", 1, l.getPreRemoveCount());
        assertEquals("Child removed", 0, target.getChildren().size());
    }
    
    public void testPreRemoveEventVeto() throws Exception {
    	((SQLObjectImpl) target).allowsChildren = true;
        target.addChild(new SQLObjectImpl());

        CountingSQLObjectPreEventListener l = new CountingSQLObjectPreEventListener();
        target.addSQLObjectPreEventListener(l);
        
        l.setVetoing(true);
        
        target.removeChild(target.getChild(0));
        
        assertEquals("Event fired", 1, l.getPreRemoveCount());
        assertEquals("Child not removed", 1, target.getChildren().size());
    }
    
    public void testClientPropertySetAndGet() {
        target.putClientProperty(this.getClass(), "testProperty", "test me");
        assertEquals("test me", target.getClientProperty(this.getClass(), "testProperty"));
    }
    
    public void testClientPropertyFiresEvent() {
        CountingSQLObjectListener listener = new CountingSQLObjectListener();
        target.addSPListener(listener);
        target.putClientProperty(this.getClass(), "testProperty", "test me");
        assertEquals(1, listener.getChangedCount());
    }
    
    public void testChildrenInaccessibleReasonSetOnPopulateError() throws Exception {
        final RuntimeException e = new RuntimeException("freaky!");
        SQLObject o = new SQLObjectImpl() {
            @Override
            protected void populateImpl() throws SQLObjectException {
                throw e;
            }
        };
        
        try {
        	o.populate();
        	fail("Failing on populate should throw an exception as well as store it in the children inaccessible reason.");
        } catch (RuntimeException ex) {
        	assertEquals(e, ex);
        }
        
        assertEquals(e, o.getChildrenInaccessibleReason());
            
    }

}
