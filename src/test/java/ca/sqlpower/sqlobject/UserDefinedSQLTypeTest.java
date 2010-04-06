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

import java.util.Collections;

import ca.sqlpower.sqlobject.SQLTypePhysicalProperties;
import ca.sqlpower.sqlobject.SQLTypePhysicalProperties.SQLTypeConstraint;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider.PropertyType;
import junit.framework.TestCase;

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
public class UserDefinedSQLTypeTest extends TestCase {

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
    private SQLTypePhysicalProperties proxyProperties;
    
    protected void setUp() throws Exception {
        super.setUp();
        
        udt = new UserDefinedSQLType();
        udtProperties = new SQLTypePhysicalProperties();
        udt.putPhysicalProperties("Oracle", udtProperties);
        
        domain = new UserDefinedSQLType(udt);
        domainProperties = new SQLTypePhysicalProperties();
        domain.putPhysicalProperties("Oracle", domainProperties);
        
        typeProxy = new UserDefinedSQLType(domain);
        proxyProperties = new SQLTypePhysicalProperties();
        typeProxy.putPhysicalProperties("Generic", proxyProperties);
    }

    public void testGetScale() throws Exception {
        proxyProperties.setScale(1);
        udtProperties.setScale(2);
        
        assertEquals(2, typeProxy.getScale("Oracle"));
    }
    
    public void testSetScale() throws Exception {
        typeProxy.setScale("Oracle", 3);
        assertEquals(3, typeProxy.getScale("Oracle"));
    }
    
    public void testGetPrecision() throws Exception {
        proxyProperties.setPrecision(1);
        udtProperties.setPrecision(2);
        
        assertEquals(2, typeProxy.getPrecision("Oracle"));
    }
    
    public void testSetPrecision() throws Exception {
        typeProxy.setPrecision("Oracle", 3);
        assertEquals(3, typeProxy.getPrecision("Oracle"));
    }
    
    public void testGetEnumeration() throws Exception {
        proxyProperties.setEnumeration(Collections.singletonList("proxy"));
        udtProperties.setEnumeration(Collections.singletonList("udt"));
        
        assertEquals("udt", typeProxy.getEnumeration("Oracle").get(0));
    }
    
    public void testSetEnumeration() throws Exception {
        typeProxy.setEnumeration("Oracle", Collections.singletonList("enum"));
        assertEquals("enum", typeProxy.getEnumeration("Oracle").get(0));
    }
    
    public void testGetDefaultValue() throws Exception {
        proxyProperties.setDefaultValue("proxy");
        udtProperties.setDefaultValue("udt");
        
        assertEquals("udt", typeProxy.getDefaultValue("Oracle"));
    }
    
    public void testSetDefaultValue() throws Exception {
        typeProxy.setDefaultValue("Oracle", "default");
        assertEquals("default", typeProxy.getDefaultValue("Oracle"));
    }
    
    public void testGetConstraintType() throws Exception {
        proxyProperties.setConstraintType(SQLTypeConstraint.CHECK);
        udtProperties.setConstraintType(SQLTypeConstraint.ENUM);
        
        assertEquals(SQLTypeConstraint.ENUM, typeProxy.getConstraintType("Oracle"));
    }
    
    public void testSetConstraintType() throws Exception {
        typeProxy.setConstraintType("Oracle", SQLTypeConstraint.CHECK);
        assertEquals(SQLTypeConstraint.CHECK, typeProxy.getConstraintType("Oracle"));
    }
    
    public void testGetCheckConstraint() throws Exception {
        proxyProperties.setCheckConstraint("Matches A1A 1A1");
        udtProperties.setCheckConstraint("Matches 12345");
        
        assertEquals("Matches 12345", typeProxy.getCheckConstraint("Oracle"));
    }
    
    public void testSetCheckConstraint() throws Exception {
        typeProxy.setCheckConstraint("Oracle", "Matches A1A 1A1");
        assertEquals("Matches A1A 1A1", typeProxy.getCheckConstraint("Oracle"));
    }
    
    public void testGetPrecisionType() throws Exception {
        proxyProperties.setPrecisionType(PropertyType.CONSTANT);
        udtProperties.setPrecisionType(PropertyType.VARIABLE);
        
        assertEquals(PropertyType.VARIABLE, typeProxy.getPrecisionType("Oracle"));
    }
    
    public void testSetPrecisionType() throws Exception {
        typeProxy.setPrecisionType("Oracle", PropertyType.NOT_APPLICABLE);
        assertEquals(PropertyType.NOT_APPLICABLE, typeProxy.getPrecisionType("Oracle"));
    }
}
