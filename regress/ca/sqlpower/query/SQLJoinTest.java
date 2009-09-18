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

package ca.sqlpower.query;

import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLDatabaseMapping;
import ca.sqlpower.testutil.CountingPropertyChangeListener;
import junit.framework.TestCase;

public class SQLJoinTest extends TestCase {

    private class StubDatabaseMapping implements SQLDatabaseMapping {

        public SQLDatabase getDatabase(JDBCDataSource ds) {
            return null;
        }
        
    }

    /**
     * Simple test to confirm setting one side of a join to be an outer join
     * instead of an inner join works. 
     */
    public void testSettingJoinToOuter() throws Exception {
        QueryImpl query = new QueryImpl(new StubDatabaseMapping());
        Item leftColumn = new StringItem("leftCol");
        Container leftContainer = new ItemContainer("leftContainer");
        leftContainer.addItem(leftColumn);
        Item rightColumn = new StringItem("rightCol");
        Container rightContainer = new ItemContainer("rightContainer");
        rightContainer.addItem(rightColumn);
        SQLJoin join = new SQLJoin(leftColumn, rightColumn);
        query.addTable(leftContainer);
        query.addTable(rightContainer);
        query.addJoin(join);
        
        assertTrue(query.getFromTableList().contains(leftContainer));
        assertTrue(query.getFromTableList().contains(rightContainer));
        assertTrue(query.getJoins().contains(join));
        assertTrue(query.getJoinMapping().containsKey(leftContainer));
        assertTrue(query.getJoinMapping().containsKey(rightContainer));
        assertFalse(join.isLeftColumnOuterJoin());
        
        join.setLeftColumnOuterJoin(true);
        
        assertTrue(join.isLeftColumnOuterJoin());
    }
    
    /**
     * Simple test to confirm setting a join to be an outer join fires a property change.
     */
    public void testSettingJoinToOuterFiresPropertyChange() throws Exception {
        Item leftColumn = new StringItem("leftCol");
        Container leftContainer = new ItemContainer("leftContainer");
        leftContainer.addItem(leftColumn);
        Item rightColumn = new StringItem("rightCol");
        Container rightContainer = new ItemContainer("rightContainer");
        rightContainer.addItem(rightColumn);
        SQLJoin join = new SQLJoin(leftColumn, rightColumn);
        CountingPropertyChangeListener countingListener = new CountingPropertyChangeListener();
        join.addJoinChangeListener(countingListener);
        
        assertFalse(join.isLeftColumnOuterJoin());
        
        join.setLeftColumnOuterJoin(true);
        
        assertTrue(join.isLeftColumnOuterJoin());
        
        assertEquals(1, countingListener.getPropertyChangeCount());
    }
}
