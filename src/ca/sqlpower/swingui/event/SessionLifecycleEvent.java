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

package ca.sqlpower.swingui.event;

/**
 * The event type that is delivered to all session lifecycle listeners when a
 * session fires a lifecycle event.
 * 
 * @param <S>
 *            The type of session that fires this event.
 */
public class SessionLifecycleEvent<S> {

    private static int nextID = 0;
    private S source;
    
    /**
     * Creates a new session lifecycle event.
     */
    public SessionLifecycleEvent(S source) {
        this.source = source;
    	nextID++;
    }

    /**
     * Returns the session that fired this lifecycle event.
     */
    public S getSource() {
        return source;
    }
}
