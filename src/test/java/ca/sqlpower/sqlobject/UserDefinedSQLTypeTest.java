/*
 * Copyright (c) 2010, SQL Power Group Inc.
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

package ca.sqlpower.sqlobject;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import ca.sqlpower.dao.SPPersisterListener;
import ca.sqlpower.dao.SPSessionPersister;
import ca.sqlpower.object.CountingSPListener;
import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.object.SPChildEvent.EventType;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sqlobject.SQLTypePhysicalProperties.SQLTypeConstraint;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider.PropertyType;
import ca.sqlpower.testutil.GenericNewValueMaker;
import ca.sqlpower.testutil.NewValueMaker;

/**
 * This particular test case is to ensure that {@link UserDefinedSQLType}
 * follows the following particular behaviour for its setters and getters:
 * <ul>
 * <li>Using a getter requires giving a platform name. This means the getter
 * will retrieve the requested property as setup for that particular platform.</li>
 * <li>A type may proxy requests to another type from which it 'inherits'
 * properties. This allows a type to inherit properties from another, (a domain
 * or User Defined Type), but make modifications on its own properties without
 * changing the properties of the types it inherits from</li>
 * <li>For getters, if a type does not have any properties defined for the
 * requested platform, or it does not have the requested property defined for
 * the requested platform, then it proxies the request to its upstream
 * UserDefinedDataType.</li>
 * <li>The setters for a UserDefinedSQLType will only edit the properties for
 * its instance. Unlike getters, it will not proxy requests to upstream data
 * types. This allows upstream data types to remain unedited.</li>
 * </ul>
 */
public class UserDefinedSQLTypeTest extends BaseSQLObjectTestCase {

	public UserDefinedSQLTypeTest(String name) throws Exception {
		super(name);
	}

	/**
	 * The 'type proxy' that would typically be the instance that a SQLColumn
	 * would use.
	 */
	private UserDefinedSQLType typeProxy;

	/**
	 * A 'domain' type that acts as the upstream type for the type proxy. The
	 * type proxy therefore inherits properties from it.
	 */
	private UserDefinedSQLType domain;

	/**
	 * A user-defined type that acts as the upstream type for the domain.
	 */
    private UserDefinedSQLType udt;
    
    
    private SQLTypePhysicalProperties udtProperties;
    private SQLTypePhysicalProperties domainProperties;
    
    protected void setUp() throws Exception {
        super.setUp();
        
        udt = new UserDefinedSQLType();
        udtProperties = new SQLTypePhysicalProperties("Oracle");
        udt.putPhysicalProperties("Oracle", udtProperties);
        udt.setType(Types.VARCHAR);
        
        domain = new UserDefinedSQLType();
        domain.setUpstreamType(udt);
        domainProperties = new SQLTypePhysicalProperties("Oracle");
        domain.putPhysicalProperties("Oracle", domainProperties);
        
        typeProxy = new UserDefinedSQLType();
        typeProxy.setUpstreamType(domain);

        getRootObject().addChild(udt, 0);
        getRootObject().addChild(typeProxy, 0);
        getRootObject().addChild(domain, 0);
    }

    public void testGetScale() throws Exception {
        typeProxy.getDefaultPhysicalProperties().setScale(1);
        typeProxy.getDefaultPhysicalProperties().setScaleType(PropertyType.VARIABLE);
        udtProperties.setScale(2);
        udtProperties.setScaleType(PropertyType.VARIABLE);
        
        assertEquals(1, typeProxy.getScale("Oracle"));
    }
    
    public void testSetScale() throws Exception {
    	final String platform = "Oracle";
        typeProxy.setScale(platform, 3);
        typeProxy.setScaleType(platform, PropertyType.VARIABLE);
        assertEquals(3, typeProxy.getScale(platform));
    }
    
    public void testGetPrecision() throws Exception {
        typeProxy.getDefaultPhysicalProperties().setPrecision(1);
        typeProxy.getDefaultPhysicalProperties().setPrecisionType(PropertyType.VARIABLE);
        udtProperties.setPrecision(2);
        udtProperties.setPrecisionType(PropertyType.VARIABLE);
        
        assertEquals(1, typeProxy.getPrecision("Oracle"));
    }
    
    public void testSetPrecision() throws Exception {
    	final String platform = "Oracle";
        typeProxy.setPrecision(platform, 3);
        typeProxy.setPrecisionType(platform, PropertyType.VARIABLE);
        assertEquals(3, typeProxy.getPrecision(platform));
    }
    
    public void testGetEnumeration() throws Exception {
    	SQLEnumeration proxyEnum = new SQLEnumeration("proxy");
        typeProxy.getDefaultPhysicalProperties().addEnumeration(proxyEnum);
        SQLEnumeration udtEnum = new SQLEnumeration("udt");
        udtProperties.addEnumeration(udtEnum);
        
        assertEquals(proxyEnum, typeProxy.getEnumerations("Oracle").get(0));
    }
    
    public void testAddEnumeration() throws Exception {
        SQLEnumeration enumeration = new SQLEnumeration("enum");
		typeProxy.addEnumeration("Oracle", enumeration);
        assertEquals(enumeration, typeProxy.getEnumerations("Oracle").get(0));
    }
    
    public void testRemoveEnumeration() throws Exception {
        SQLEnumeration enumeration = new SQLEnumeration("enum");
		typeProxy.addEnumeration("Oracle", enumeration);
    	typeProxy.removeEnumeration("Oracle", enumeration);
    	assertTrue(typeProxy.getEnumerations("Oracle").isEmpty());
    }
    
    public void testGetDefaultValue() throws Exception {
        typeProxy.getDefaultPhysicalProperties().setDefaultValue("proxy");
        udtProperties.setDefaultValue("udt");
        
        assertEquals("proxy", typeProxy.getDefaultValue("Oracle"));
    }
    
    public void testSetDefaultValue() throws Exception {
        typeProxy.setDefaultValue("Oracle", "default");
        assertEquals("default", typeProxy.getDefaultValue("Oracle"));
    }
    
    public void testGetConstraintType() throws Exception {
        typeProxy.getDefaultPhysicalProperties().setConstraintType(SQLTypeConstraint.CHECK);
        udtProperties.setConstraintType(SQLTypeConstraint.ENUM);
        
        assertEquals(SQLTypeConstraint.CHECK, typeProxy.getConstraintType("Oracle"));
    }
    
    public void testSetConstraintType() throws Exception {
        typeProxy.setConstraintType("Oracle", SQLTypeConstraint.CHECK);
        assertEquals(SQLTypeConstraint.CHECK, typeProxy.getConstraintType("Oracle"));
    }
    
    public void testGetCheckConstraints() throws Exception {
    	SQLCheckConstraint constraint1 = new SQLCheckConstraint("testGetCheckConstraints - typeProxy", "Matches A1A 1A1");
    	SQLCheckConstraint constraint2 = new SQLCheckConstraint("testGetCheckConstraints - udtProperties", "Matches 12345");
    	
		typeProxy.getDefaultPhysicalProperties().addCheckConstraint(constraint1);
		udtProperties.addCheckConstraint(constraint2);
		assertEquals(1, typeProxy.getCheckConstraints("Oracle").size());
		assertEquals(constraint1, typeProxy.getCheckConstraints("Oracle").get(0));
    }
    
    public void testAddCheckConstraint() throws Exception {
    	SQLCheckConstraint checkConstraint = new SQLCheckConstraint("testAddcheckConstraint", "Matches A1A 1A1");
		typeProxy.addCheckConstraint("Oracle", checkConstraint);
    	assertEquals(1, typeProxy.getCheckConstraints("Oracle").size());
    	assertEquals(checkConstraint, typeProxy.getCheckConstraints("Oracle").get(0));
    }
    
    public void testRemoveCheckConstraint() throws Exception {
    	SQLCheckConstraint checkConstraint = new SQLCheckConstraint("testRemoveCheckConstraint", "Matches A1A 1A1");
    	typeProxy.addCheckConstraint("Oracle", checkConstraint);
    	typeProxy.removeCheckConstraint("Oracle", checkConstraint);
    	assertTrue(typeProxy.getCheckConstraints("Oracle").isEmpty());
    }
    
    public void testGetPrecisionType() throws Exception {
        typeProxy.getDefaultPhysicalProperties().setPrecisionType(PropertyType.CONSTANT);
        udtProperties.setPrecisionType(PropertyType.VARIABLE);
        
        assertEquals(PropertyType.CONSTANT, typeProxy.getPrecisionType("Oracle"));
    }
    
    public void testSetPrecisionType() throws Exception {
        typeProxy.setPrecisionType("Oracle", PropertyType.NOT_APPLICABLE);
        assertEquals(PropertyType.NOT_APPLICABLE, typeProxy.getPrecisionType("Oracle"));
    }

    /**
     * this test ensures that the new physical properties object replaces the
     * old when they have the same platform
     */
    public void testSamePlatformOverwritesOld() throws Exception {
    	UserDefinedSQLType obj = (UserDefinedSQLType) getSQLObjectUnderTest();
    	
    	SQLTypePhysicalProperties props0 = new SQLTypePhysicalProperties("testPlatform");
    	SQLTypePhysicalProperties props1 = new SQLTypePhysicalProperties("testPlatform");
    	
    	obj.putPhysicalProperties(props0.getPlatform(), props0);
    	assertEquals(props0.getUUID(), obj.getPhysicalProperties(props0.getPlatform()).getUUID());
    	obj.putPhysicalProperties(props0.getPlatform(), props1);
    	assertEquals(props1.getUUID(), obj.getPhysicalProperties(props0.getPlatform()).getUUID());
    }
    
	@Override
	protected Class<? extends SPObject> getChildClassType() {
		return SQLTypePhysicalProperties.class;
	}

	@Override
	protected SQLObject getSQLObjectUnderTest() throws SQLObjectException {
		return typeProxy;
	}
	
	/**
	 * Overriden because the superclass version does not account for the default
	 * physical properties child, which must always exist and always be at index
	 * 0. Inserting a child at 0 results in an exception being thrown.
	 */
	@Override
	public void testAllChildHandlingMethods() throws SQLObjectException,
			IllegalArgumentException, ObjectDependentException {
		if (!getSQLObjectUnderTest().allowsChildren()) return;

		getSQLObjectUnderTest().populate();
		
		NewValueMaker newValueMaker = new GenericNewValueMaker(getRootObject());
		Class<? extends SPObject> childType = getSQLObjectUnderTest().getAllowedChildTypes().get(0);
		
		int childCount = getSQLObjectUnderTest().getChildCount();
		List<SPObject> children = new ArrayList<SPObject>();
		children.addAll(getSQLObjectUnderTest().getChildren(childType));

		SQLObject x = (SQLObject) newValueMaker.makeNewValue(childType, null, "");
		
		getSQLObjectUnderTest().addChild(x);
		assertEquals(childCount + 1, getSQLObjectUnderTest().getChildCount());
		assertEquals(x, getSQLObjectUnderTest().getChildren(childType).get(
				getSQLObjectUnderTest().getChildren(childType).size() - 1));
		
		SQLObject y = (SQLObject) newValueMaker.makeNewValue(childType, null, "");
		
		// Test addChild(SQLObject, int)
		getSQLObjectUnderTest().addChild(y, 1);
		assertEquals(y, getSQLObjectUnderTest().getChildren(y.getClass()).get(1));
		assertEquals(x, getSQLObjectUnderTest().getChildren(childType).get(
				getSQLObjectUnderTest().getChildren(childType).size() - 1));
		
		getSQLObjectUnderTest().removeChild(x);
		children.add(0, y);
		assertTrue(getSQLObjectUnderTest().getChildren(childType).containsAll(children));
		
		getSQLObjectUnderTest().removeChild(y);
		assertEquals(childCount, getSQLObjectUnderTest().getChildCount());
	}
	
	/**
	 * Overriden because the superclass version does not account for the default
	 * physical properties child, which must always exist and always be at index
	 * 0. Inserting a child at 0 results in an exception being thrown.
	 */
	@Override
	public void testPreRemoveEventNoVeto() throws Exception {
    	if (!getSQLObjectUnderTest().allowsChildren()) return;
    	
    	getSQLObjectUnderTest().populate();
    	
		Class<? extends SPObject> childType = getSQLObjectUnderTest().getAllowedChildTypes().get(0);
		NewValueMaker newValueMaker = new GenericNewValueMaker(getRootObject());
		SQLObject x = (SQLObject) newValueMaker.makeNewValue(childType, null, "");
		
		int childCount = getSQLObjectUnderTest().getChildCount();
        getSQLObjectUnderTest().addChild(x);

        CountingSQLObjectPreEventListener l = new CountingSQLObjectPreEventListener();
        getSQLObjectUnderTest().addSQLObjectPreEventListener(l);
        
        l.setVetoing(false);
        
        getSQLObjectUnderTest().removeChild(getSQLObjectUnderTest().getChild(1));
        
        assertEquals("Event fired", 1, l.getPreRemoveCount());
        assertEquals("Child removed", childCount, getSQLObjectUnderTest().getChildren().size());
	}
	
	/**
	 * Overriden because the superclass version does not account for the default
	 * physical properties child, which must always exist and always be at index
	 * 0. Inserting a child at 0 results in an exception being thrown.
	 */
	@Override
	public SPObject testSPPersisterAddsChild() throws Exception {
		NewValueMaker valueMaker = createNewValueMaker(getRootObject(), getPLIni());
    	
    	SPObject spObject = getSPObjectUnderTest();
    	int oldChildCount = spObject.getChildren().size();
    	if (!spObject.allowsChildren()) return null;
    	
    	Class<? extends SPObject> childClassType = getChildClassType();
    	if (childClassType == null) return null;
    	
    	SPSessionPersister persister = new TestingSessionPersister("test", getSPObjectUnderTest(), getConverter());
    	persister.setWorkspaceContainer(getSPObjectUnderTest().getWorkspaceContainer());
    	SPPersisterListener listener = new SPPersisterListener(persister, getConverter());
    	
    	SPObject newChild = (SPObject) valueMaker.makeNewValue(childClassType, null, "child");
    	newChild.setParent(spObject);
    	
    	listener.childAdded(new SPChildEvent(spObject, childClassType, newChild, 1, EventType.ADDED));
    	
    	assertEquals(oldChildCount + 1, spObject.getChildren().size());
    	assertEquals(newChild, spObject.getChildren(childClassType).get(1));
    	
    	newChild.removeSPListener(listener);
    	
    	//Find the actual child under the object under test as the persister will make a new,
    	//different object to add not the newChild object. This lets the objects compare
    	//equal by reference.
    	for (SPObject existingChild : spObject.getChildren(childClassType)) {
    	    if (existingChild.getUUID().equals(newChild.getUUID())) {
    	        return existingChild;
    	    }
    	}
    	return null;
	}
	
	/**
	 * Overriden because the superclass version does not account for the default
	 * physical properties child, which must always exist and always be at index
	 * 0. Inserting a child at 0 results in an exception being thrown.
	 */
	@Override
	public void testAddChildFiresEvents() throws Exception {
    	SPObject o = getSPObjectUnderTest();
    	
    	if (!o.allowsChildren()) return;
    	
    	Class<?> childClassType = getChildClassType();
    	if (childClassType == null) return;
    	
    	CountingSPListener listener = new CountingSPListener();
		
    	o.addSPListener(listener);
    	
    	NewValueMaker valueMaker = createNewValueMaker(getRootObject(), getPLIni());
    	SPObject newChild = (SPObject) valueMaker.makeNewValue(childClassType, null, "child");
    	
    	o.addChild(newChild, 1);
    	
    	assertEquals(1, listener.getChildAddedCount());
	}
}
