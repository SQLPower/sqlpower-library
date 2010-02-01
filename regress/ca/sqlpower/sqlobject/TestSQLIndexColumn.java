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

import ca.sqlpower.object.SPObject;
import ca.sqlpower.sqlobject.SQLIndex.AscendDescend;
import ca.sqlpower.sqlobject.SQLIndex.Column;

public class TestSQLIndexColumn extends BaseSQLObjectTestCase {
    
    private SQLIndex.Column indexColumn; 

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        indexColumn = new Column("Index1",AscendDescend.UNSPECIFIED);
        SQLIndex index = new SQLIndex();
        index.addIndexColumn(indexColumn);
        SQLTable table = new SQLTable();
        table.addIndex(index);
        table.setPopulated(true);
        db.addTable(table);
    }
    
    public TestSQLIndexColumn(String name) throws Exception {
        super(name);
    }
    
    @Override
    protected SQLObject getSQLObjectUnderTest() {
        
        return indexColumn;
    }
    
    @Override
    protected Class<? extends SPObject> getChildClassType() {
    	return null;
    }

}
