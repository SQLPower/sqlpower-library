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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ca.sqlpower.sql.SPDataSourceType;

import junit.framework.TestCase;

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
    
    public void testRetrieveURLDefaultsNoTemplate(){
        SPDataSourceType dsType = new SPDataSourceType();
        dsType.setJdbcUrl(null);
        Map<String, String> map = dsType.retrieveURLDefaults();
        assertEquals(0, map.size());
    }
}
