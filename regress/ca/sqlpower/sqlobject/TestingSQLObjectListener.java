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


public class TestingSQLObjectListener implements SQLObjectListener {

    private int insertedCount;
    private int removedCount;
    private int changedCount;
    private String lastEventName;

    public TestingSQLObjectListener() {
        insertedCount = 0;
        removedCount = 0;
        changedCount = 0;
        lastEventName = null;
    }
    public void dbChildrenInserted(SQLObjectEvent e) {
        insertedCount++;
    }

    public void dbChildrenRemoved(SQLObjectEvent e) {
        removedCount++;
    }

    public void dbObjectChanged(SQLObjectEvent e) {
        changedCount++;
        lastEventName = e.getPropertyName();
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public int getRemovedCount() {
        return removedCount;
    }
    public int getChangedCount() {
        return changedCount;
    }
    public String getLastEventName() {
        return lastEventName;
    }
    
}
