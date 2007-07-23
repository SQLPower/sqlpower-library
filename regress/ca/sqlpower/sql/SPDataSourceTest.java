/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.sqlpower.sql;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import ca.sqlpower.testutil.CountingPropertyChangeListener;
import ca.sqlpower.sql.SPDataSource;

import junit.framework.TestCase;

public class SPDataSourceTest extends TestCase {

	SPDataSource ds;
	
	protected void setUp() throws Exception {
		super.setUp();
		ds = new SPDataSource();
		System.out.println("NEW DATA SOURCE parent type name = "+ds.getPropertiesMap().get(SPDataSource.DBCS_CONNECTION_TYPE));
		ds.setDisplayName("Regression Test");
		ds.getParentType().setJdbcDriver("com.does.not.exist");
		ds.setName("test_name");
		ds.setOdbcDsn("fake_odbc_dsn");
		ds.setPass("fake_password");
		ds.setPlDbType("fake_pl_type");
		ds.setPlSchema("my_fake_pl_schema");
		ds.setUrl("jdbc:fake:fake:fake");
		ds.setUser("fake_user");
        System.out.println("NEW DATA SOURCE after init parent type name = "+ds.getPropertiesMap().get(SPDataSource.DBCS_CONNECTION_TYPE));
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/*
	 * Test method for 'ca.sqlpower.architect.SPDataSource.hashCode()'
	 */
	public void testHashCode() {
		// cache the old hash code, change a property, and ensure the hash code changes
		String oldPass = ds.getPass();
		int oldCode = ds.hashCode();
		ds.setPass("cows");
		assertFalse(oldCode == ds.hashCode());
		
		// now put the property back, and ensure the hash code goes back to its original value
		ds.setPass(oldPass);
		assertEquals(oldCode, ds.hashCode());
	}

	/*
	 * Test method for 'ca.sqlpower.architect.SPDataSource.ArchitectDataSource()'
	 */
	public void testArchitectDataSource() {
		assertNotNull(ds.getPropertiesMap());
		assertNotNull(ds.getPropertyChangeListeners());
	}

	/*
	 * Test method for 'ca.sqlpower.architect.SPDataSource.put(String, String)'
	 */
	public void testPut() {
		CountingPropertyChangeListener l = new CountingPropertyChangeListener();
		ds.addPropertyChangeListener(l);
		ds.put("test_key", "peek-a-boo!");
		
		assertEquals(1, l.getPropertyChangeCount());
		assertEquals("test_key", l.getLastPropertyChange());
		assertEquals("peek-a-boo!", ds.get("test_key"));
	}

	/*
	 * Test method for 'ca.sqlpower.architect.SPDataSource.getPropertiesMap()'
	 */
	public void testGetPropertiesMap() {
		// ensure clients can't get at a mutable reference to the map!
		Map m = ds.getPropertiesMap();
		assertNotNull(m);
		try {
			m.put("won't work", "fooooooey");
			fail("client view of property map must be immutable");
		} catch (UnsupportedOperationException ex) {
			assertTrue(true);
		}
	}

	/*
	 * Test method for 'ca.sqlpower.architect.SPDataSource.equals(Object)'
	 */
	public void testEquals() {
		SPDataSource ds1 = new SPDataSource();
		SPDataSource ds2 = new SPDataSource();
		
		ds1.setDisplayName("Regression Test");
		ds2.setDisplayName("Regression Test");
		ds1.getParentType().setJdbcDriver("com.does.not.exist");
		ds2.getParentType().setJdbcDriver("com.does.not.exist");
		ds1.setName("test_name");
		ds2.setName("test_name");
		ds1.setOdbcDsn("fake_odbc_dsn");
		ds2.setOdbcDsn("fake_odbc_dsn");
		ds1.setPass("fake_password");
		ds2.setPass("fake_password");
		ds1.setPlDbType("fake_pl_type");
		ds2.setPlDbType("fake_pl_type");
		ds1.setPlSchema("my_fake_pl_schema");
		ds2.setPlSchema("my_fake_pl_schema");
		ds1.setUrl("jdbc:fake:fake:fake");
		ds2.setUrl("jdbc:fake:fake:fake");
		ds1.setUser("fake_user");
		ds2.setUser("fake_user");
		
		assertEquals(ds1, ds2);
		
		// try a known property
		ds2.setDisplayName("x");
		assertFalse(ds1.equals(ds2));

		ds2.setDisplayName(ds1.getDisplayName());
		assertEquals(ds1, ds2);
		
		// try a dynamic property
		ds1.put("cow", "moo");
		assertFalse(ds1.equals(ds2));
	}
	
	public void testEqualsNull() {
		assertFalse("This is really to check null comparison is allowed", ds.equals(null));
	}

	/*
	 * Test method for 'ca.sqlpower.architect.SPDataSource.addPropertyChangeListener(PropertyChangeListener)'
	 */
	public void testAddPropertyChangeListener() {
		CountingPropertyChangeListener l = new CountingPropertyChangeListener();
		ds.addPropertyChangeListener(l);
		assertTrue(ds.getPropertyChangeListeners().contains(l));
	}

	/*
	 * Test method for 'ca.sqlpower.architect.SPDataSource.removePropertyChangeListener(PropertyChangeListener)'
	 */
	public void testRemovePropertyChangeListener() {
		CountingPropertyChangeListener l = new CountingPropertyChangeListener();
		ds.addPropertyChangeListener(l);
		assertTrue(ds.getPropertyChangeListeners().contains(l));
		ds.removePropertyChangeListener(l);
		assertFalse(ds.getPropertyChangeListeners().contains(l));
	}

	/*
	 * Test method for 'ca.sqlpower.architect.SPDataSource.setName(String)'
	 */
	public void testSetName() {
		CountingPropertyChangeListener l = new CountingPropertyChangeListener();
		ds.addPropertyChangeListener(l);
		ds.setName("test");
		
		assertEquals(1, l.getPropertyChangeCount());
		assertEquals("name", l.getLastPropertyChange());
		assertEquals("test", ds.getName());
	}

	/*
	 * Test method for 'ca.sqlpower.architect.SPDataSource.setDisplayName(String)'
	 */
	public void testSetDisplayName() {
		CountingPropertyChangeListener l = new CountingPropertyChangeListener();
		ds.addPropertyChangeListener(l);
		ds.setDisplayName("test");
		
		assertEquals(1, l.getPropertyChangeCount());
		assertEquals("name", l.getLastPropertyChange());
		assertEquals("test", ds.getDisplayName());
	}

	/*
	 * Test method for 'ca.sqlpower.architect.SPDataSource.setUrl(String)'
	 */
	public void testSetUrl() {
		CountingPropertyChangeListener l = new CountingPropertyChangeListener();
		ds.addPropertyChangeListener(l);
		ds.setUrl("test");
		
		assertEquals(1, l.getPropertyChangeCount());
		assertEquals("url", l.getLastPropertyChange());
		assertEquals("test", ds.getUrl());
	}

	/*
	 * Test method for 'ca.sqlpower.architect.SPDataSource.setUser(String)'
	 */
	public void testSetUser() {
		CountingPropertyChangeListener l = new CountingPropertyChangeListener();
		ds.addPropertyChangeListener(l);
		ds.setUser("test");
		
		assertEquals(1, l.getPropertyChangeCount());
		assertEquals("user", l.getLastPropertyChange());
		assertEquals("test", ds.getUser());
	}

	/*
	 * Test method for 'ca.sqlpower.architect.SPDataSource.setPass(String)'
	 */
	public void testSetPass() {
		CountingPropertyChangeListener l = new CountingPropertyChangeListener();
		ds.addPropertyChangeListener(l);
		ds.setPass("test");
		
		assertEquals(1, l.getPropertyChangeCount());
		assertEquals("pass", l.getLastPropertyChange());
		assertEquals("test", ds.getPass());
	}

	/*
	 * Test method for 'ca.sqlpower.architect.SPDataSource.setPlSchema(String)'
	 */
	public void testSetPlSchema() {
		CountingPropertyChangeListener l = new CountingPropertyChangeListener();
		ds.addPropertyChangeListener(l);
		ds.setPlSchema("test");
		
		assertEquals(1, l.getPropertyChangeCount());
		assertEquals("plSchema", l.getLastPropertyChange());
		assertEquals("test", ds.getPlSchema());
	}

	/*
	 * Test method for 'ca.sqlpower.architect.SPDataSource.setPlDbType(String)'
	 */
	public void testSetPlDbType() {
		CountingPropertyChangeListener l = new CountingPropertyChangeListener();
		ds.addPropertyChangeListener(l);
		ds.setPlDbType("test");
		
		assertEquals(1, l.getPropertyChangeCount());
		assertEquals("plDbType", l.getLastPropertyChange());
		assertEquals("test", ds.getPlDbType());
	}

	/*
	 * Test method for 'ca.sqlpower.architect.SPDataSource.setOdbcDsn(String)'
	 */
	public void testSetOdbcDsn() {
		CountingPropertyChangeListener l = new CountingPropertyChangeListener();
		ds.addPropertyChangeListener(l);
		ds.setOdbcDsn("test");
		
		assertEquals(1, l.getPropertyChangeCount());
		assertEquals("odbcDsn", l.getLastPropertyChange());
		assertEquals("test", ds.getOdbcDsn());
	}

	public void testComparator() {
		// set up identical second data source
		SPDataSource ds2 = new SPDataSource();
        ds2.setParentType(ds.getParentType());
		for (String key : ds.getPropertiesMap().keySet()) {
			ds2.put(key, ds.get(key));
        }
        
		Comparator<SPDataSource> cmp = new SPDataSource.DefaultComparator();
		assertEquals(0, cmp.compare(ds, ds2));
		
		// test that the display name takes precedence over other properties
		ds2.setDisplayName("a");
		ds2.setUser("z");
		assertTrue(cmp.compare(ds, ds2) > 0);
		
		ds2.setDisplayName("z");
		ds2.setUser("a");
		assertTrue(cmp.compare(ds, ds2) < 0);
		
		// test that comparison bubbles down to user (which is near the end of the comparison order)
		ds2.setDisplayName(ds.getDisplayName());
		ds2.setUser("a");
		assertTrue(cmp.compare(ds, ds2) > 0);

		ds2.setUser("z");
		assertTrue(cmp.compare(ds, ds2) < 0);
	}
    
    /* The parent type name is just stored in the map as a string.  When the parent type's
     * name changes at runtime, some magic is required to update the parent name in the map
     * so it matches.
     */
    public void testParentNameSync() {
        assertEquals(ds.getParentType().getName(), ds.getPropertiesMap().get(SPDataSource.DBCS_CONNECTION_TYPE));
        ds.getParentType().setName("New Name");
        assertEquals(ds.getParentType().getName(), ds.getPropertiesMap().get(SPDataSource.DBCS_CONNECTION_TYPE));
    }
    
    public void testCopyFrom() {
        SPDataSource targetDs = new SPDataSource();
        targetDs.copyFrom(ds);

        // need to copy all props into a tree map so they're both sorted in the same order
        Map<Object, Object> sourceMap = new TreeMap<Object, Object>(ds.getPropertiesMap());
        Map<Object, Object> targetMap = new TreeMap<Object, Object>(targetDs.getPropertiesMap());
        
        assertEquals(sourceMap.toString(), targetMap.toString());
        assertSame(ds.getParentType(), targetDs.getParentType());
    }
    
    public void testCopyFromFiresNameChange() {
        CountingPropertyChangeListener pcl = new CountingPropertyChangeListener();
        
        SPDataSource targetDs = new SPDataSource();
        targetDs.addPropertyChangeListener(pcl);
        targetDs.copyFrom(ds);

        assertEquals(1, pcl.getPropertyChangeCount("name"));
    }
}
