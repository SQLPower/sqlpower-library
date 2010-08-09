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

import ca.sqlpower.object.PersistedSPObjectTest;
import ca.sqlpower.object.SPObject;

public class UserDefinedSQLTypeSnapshotTest extends PersistedSPObjectTest {

    public UserDefinedSQLTypeSnapshotTest(String name) throws Exception {
		super(name);
	}

	private UserDefinedSQLType udt;
    private UserDefinedSQLTypeSnapshot udtSnapshot;
    
	protected void setUp() throws Exception {
        super.setUp();
        
        udt = new UserDefinedSQLType();
        SQLTypePhysicalProperties udtProperties = new SQLTypePhysicalProperties("Oracle");
        udt.putPhysicalProperties("Oracle", udtProperties);
        udt.setType(Types.VARCHAR);
        
        udtSnapshot = new UserDefinedSQLTypeSnapshot(udt, false);
        
        getRootObject().addChild(udt, 0);
        getRootObject().addChild(udtSnapshot, 0);
    }

	@Override
	public SPObject getSPObjectUnderTest() {
		return udtSnapshot;
	}

	@Override
	protected Class<? extends SPObject> getChildClassType() {
		return UserDefinedSQLType.class;
	}
}
