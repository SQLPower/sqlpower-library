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
 * Interface that listeners interested in knowing when a certain operation on a
 * source SQLObject has been requested but has not yet been performed. Listeners
 * implementing this interface have the opportunity to veto the changes that are
 * described by the events they receive. There may be additional events that
 * will come between the calls to the pre-event listeners and the event that
 * signals the operation requested has completed.
 */
public interface SQLObjectPreEventListener {
    
    public void dbChildrenPreRemove(SQLObjectPreEvent e);
}
