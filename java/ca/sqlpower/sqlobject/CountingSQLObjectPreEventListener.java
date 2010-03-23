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

package ca.sqlpower.sqlobject;


/**
 * A SQLObjectPreEventListener implementation that counts how many
 * times each type of event has been received.  Can be configured
 * to either veto all events received or none of them.
 */
public class CountingSQLObjectPreEventListener implements SQLObjectPreEventListener {

    /**
     * If true, this listener will veto every event it receives.
     */
    private boolean vetoing;

    /**
     * The number of pre-remove events received so far.
     */
    private int preRemoveCount;
    
    public void dbChildrenPreRemove(SQLObjectPreEvent e) {
        preRemoveCount++;
        if (vetoing) {
            e.veto();
        }
    }

    public boolean isVetoing() {
        return vetoing;
    }

    public void setVetoing(boolean vetoing) {
        this.vetoing = vetoing;
    }

    public int getPreRemoveCount() {
        return preRemoveCount;
    }
    
}
