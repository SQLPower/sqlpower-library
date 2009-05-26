/*
 * Copyright (c) 2008, SQL Power Group Inc.
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
package ca.sqlpower.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;

import junit.framework.TestCase;
import ca.sqlpower.sql.SPDataSourceType.JDBCClassLoader;

public class SPDataSourceTypeTest extends TestCase {

    private static final String PL_INI_CONTENTS = 
        "[Database Types_0]\n" +
        "Name=Type 0\n" +
        "JDBC Driver Class=ca.sqlpower.cow.Cow\n" +
        "JDBC URL=jdbc:cow://moo\n" +
        "Comment=No Comment\n" +
        "Property Name We Will Never Use=I hope this is ok\n" +
        "JDBC JAR Count=2\n" +
        "JDBC JAR_0=my.jar\n" +
        "JDBC JAR_1=your.jar\n" +
        "[Database Types_1]\n" +
        "Parent Type=Type 0\n" +
        "Name=Type 0.1\n" +
        "";
    
    /**
     * A sample instance of the data source type for testing.
     */
    private SPDataSourceType superType;
    
    /**
     * This is a subtype of superType.
     */
    private SPDataSourceType subType;
    
    protected void setUp() throws Exception {
        superType = new SPDataSourceType();
        subType = new SPDataSourceType();
        subType.setParentType(superType);
        SPDataSourceType currentType = null;
        for (String line : PL_INI_CONTENTS.split("\\n")) {
            if (line.equals("[Database Types_0]")) {
                currentType = superType;
            } else if (line.equals("[Database Types_1]")) {
                currentType = subType;
            } else if (line.startsWith("[")) {
                throw new RuntimeException("File format problem");
            } else {
                int splitPoint = line.indexOf('=');
                String key = line.substring(0, splitPoint);
                String value = line.substring(splitPoint + 1);
                currentType.putProperty(key, value);
            }
        }
        
        assertTrue("Supertype didn't load properly", superType.getProperties().size() > 0);
        assertTrue("Subtype didn't load properly", subType.getProperties().size() > 0);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetName() {
        assertEquals("Type 0", superType.getName());
    }
    
    public void testInheritedProperty() {
        assertEquals("ca.sqlpower.cow.Cow", subType.getJdbcDriver());
    }
    
    public void testHasClassLoader() {
        assertNotNull(superType.getJdbcClassLoader());
    }
    
    public void testGetComment() {
        assertEquals("No Comment", superType.getComment());
    }
    
    public void testGetJdbcDriver() {
        assertEquals("ca.sqlpower.cow.Cow", superType.getJdbcDriver());
    }
    
    public void testGetJdbcJarList() {
        List<String> l = superType.getJdbcJarList();
        assertNotNull(l);
        assertEquals(2, l.size());
        assertEquals("incorrect jar list entry. properties="+superType.getProperties(), "my.jar", l.get(0));
        assertEquals("your.jar", l.get(1));
    }
    
    public void testGetJdbcUrl() throws Exception {
        assertEquals("jdbc:cow://moo", superType.getJdbcUrl());
    }
    
    public void testGetParentType() {
        assertSame(superType, subType.getParentType());
        assertNull(superType.getParentType());
    }
    
    public void testGetPropertiesImmutable() {
        try {
            superType.getProperties().put("this won't", "work");
            fail("I changed a property");
        } catch (UnsupportedOperationException ex) {
            // this is good
        }
    }
    
    public void testUnknownPropertiesStillExist() {
        assertEquals("I hope this is ok", superType.getProperties().get("Property Name We Will Never Use"));
    }
    
    public void testSetJdbcJarList() {
        List<String> list = new ArrayList<String>();
        list.add("zero");
        list.add("one");
        list.add("two");
        
        superType.setJdbcJarList(list);
        
        List<String> newList = superType.getJdbcJarList();
        
        assertEquals(list, newList);
    }
    
    public void testAddJdbcJar() {
        List<String> list = new ArrayList<String>(superType.getJdbcJarList());
        
        list.add("new thing");
        superType.addJdbcJar("new thing");
        
        assertEquals(list, superType.getJdbcJarList());
        assertEquals(String.valueOf(list.size()), superType.getProperty(SPDataSourceType.JDBC_JAR_COUNT));
    }

    public void testJdbcJarListImmutable() {
        List<String> list = superType.getJdbcJarList();
        
        try {
            list.add("new thing");
            fail("I think I modified the list");
        } catch (UnsupportedOperationException ex) {
            // good result
        }
    }
    
    public void testRemoveJdbcJar() {
        int jarCount = superType.getJdbcJarList().size();
        superType.removeJdbcJar("your.jar");
        assertEquals(jarCount - 1, superType.getJdbcJarList().size());
        superType.removeJdbcJar("your.jar");
    }
    
    public void testRetrieveURLParsing() {
        SPDataSourceType dsType = new SPDataSourceType();
        dsType.setJdbcUrl("<Database>:<Port>:<Hostname>");
        Map<String, String> map = dsType.retrieveURLParsing("data:1234:");
        assertEquals("data", map.get("Database"));
        assertEquals("1234", map.get("Port"));
        assertEquals("", map.get("Hostname") );
    }

    public void testRetrieveURLParsingWithUnclosedVariable() {
        SPDataSourceType dsType = new SPDataSourceType();
        dsType.setJdbcUrl("<Database");
        Map<String, String> map = dsType.retrieveURLParsing("data");
        assertTrue(map.isEmpty());
    }

    public void testRetrieveURLParsingWithDefaults() {
        SPDataSourceType dsType = new SPDataSourceType();
        dsType.setJdbcUrl("<Database:db>:<Port:2222>:<Hostname:home>");
        Map<String, String> map = dsType.retrieveURLParsing("data:1234:");
        assertEquals("data", map.get("Database"));
        assertEquals("1234", map.get("Port"));
        assertEquals("", map.get("Hostname"));
    }
    
    public void testRetrieveURLParsingNullTemplateURL() {
        SPDataSourceType dsType = new SPDataSourceType();
        dsType.setJdbcUrl(null);
        Map<String, String> map = dsType.retrieveURLParsing("data:1234:");
        assertEquals(0, map.size());
    }
    
    public void testRetrieveURLParsingURLDoesntmMatchTemplate() {
        SPDataSourceType dsType = new SPDataSourceType();
        dsType.setJdbcUrl("hello:<Database:db>:<Port:2222>:<Hostname:home>");
        Map<String, String> map = dsType.retrieveURLParsing("hello");
        assertEquals(0, map.size());
    }
    
    public void testRetrieveURLDefaults(){
        SPDataSourceType dsType = new SPDataSourceType();
        dsType.setJdbcUrl("<Database>:<Port:1234>:<Hostname:home>");
        Map<String, String> map = dsType.retrieveURLDefaults();
        assertEquals("", map.get("Database"));
        assertEquals("1234", map.get("Port"));
        assertEquals("home", map.get("Hostname"));
    }

    /**
     * Tests for regression: the retrieveURLDefaults method used to
     * get into an infinite loop when there's an unterminated variable
     * name (&lt; without a matching &gt;).
     */
    public void testRetrieveURLDefaultsWithUnclosedVariable(){
        SPDataSourceType dsType = new SPDataSourceType();
        dsType.setJdbcUrl("<Database");
        Map<String, String> map = dsType.retrieveURLDefaults();
        assertTrue(map.isEmpty());
    }

    public void testRetrieveURLDefaultsNoTemplate(){
        SPDataSourceType dsType = new SPDataSourceType();
        dsType.setJdbcUrl(null);
        Map<String, String> map = dsType.retrieveURLDefaults();
        assertEquals(0, map.size());
    }
    
    /**
     * Regression test: when there's a classpath entry for a built-in resource
     * that doesn't exist, you get NPE when loading a resource (not a class,
     * just a resource)
     */
    public void testLoadResourceFromMissingJar() throws Exception {
        SPDataSourceType dsType = new SPDataSourceType();
        dsType.addJdbcJar("builtin:does/not/exist.jar");
        
        JDBCClassLoader jdbcClassLoader = (JDBCClassLoader) dsType.getJdbcClassLoader();
        jdbcClassLoader.findResources("my_resource");
        // test passes if previous statement doesn't throw NPE
    }
    
    public void testUndoAndRedo() throws Exception {
    	final SPDataSourceType dsType = new SPDataSourceType();
    	class TestUndoableEditListener implements UndoableEditListener {
    		private int editCount = 0;
    		
			public void undoableEditHappened(UndoableEditEvent e) {
				editCount++;
				UndoableEdit edit = e.getEdit();
				assertTrue(edit.canUndo());
				assertEquals("hello", dsType.getProperty("Test"));
				edit.undo();
				assertTrue(edit.canRedo());
				assertNull(dsType.getProperty("Test"));
				edit.redo();
				assertTrue(edit.canUndo());
				assertEquals("hello", dsType.getProperty("Test"));
			}
			
			public int getEditCount() {
				return editCount;
			}
		}
    	TestUndoableEditListener undoableEditListener = new TestUndoableEditListener();
    	dsType.addUndoableEditListener(undoableEditListener);
    	dsType.putProperty("Test", "hello");
    	
    	assertEquals(1, undoableEditListener.getEditCount());
    }
    
    /**
     * The setters on the DSType uses a different method to set properties than the
     * one used in testUndoAndRedo. This confirms that the setters do create undo and
     * redo edits.
     */
    public void testUndoOnSetters() throws Exception {
    	final SPDataSourceType dsType = new SPDataSourceType();
    	
    	class TestUndoableEditListener implements UndoableEditListener {
    		private int editCount = 0;
    		
			public void undoableEditHappened(UndoableEditEvent e) {
				editCount++;
			}
			
			public int getEditCount() {
				return editCount;
			}
		}
    	TestUndoableEditListener undoableEditListener = new TestUndoableEditListener();
    	dsType.addUndoableEditListener(undoableEditListener);
    	dsType.setComment("comment");
    	dsType.setDDLGeneratorClass("class");
    	dsType.setName("name");
    	
    	assertEquals(3, undoableEditListener.getEditCount());
    }
    
    public void testStreamFlagDefaultsFalse() throws Exception {
        assertFalse(subType.getSupportsStreamQueries());
    }
    
    public void testStreamFlagChange() throws Exception {
        subType.putProperty(SPDataSourceType.SUPPORTS_STREAM_QUERIES, String.valueOf(true));
        assertTrue(subType.getSupportsStreamQueries());
    }
}
