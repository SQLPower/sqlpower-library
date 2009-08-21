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

/**
 * This class is used to signal the change between the start and end of a
 * compound edit in a {@link Query}.
 */
public class QueryCompoundEditEvent {
    
    public enum CompoundEditState {
        START,
        END
    }
    
    /**
     * Call this method to create a start compound edit event.
     * 
     * @param source
     *            The source of the event.
     * @param message
     *            A message describing the compound edit.
     */
    public static QueryCompoundEditEvent createStartCompoundEditEvent(Query source, String message) {
        return new QueryCompoundEditEvent(source, message);
    }
    
    /**
     * Call this constructor to create an end compound edit event.
     * 
     * @param source
     *            The source of the event.
     */
    public static QueryCompoundEditEvent createEndCompoundEditEvent(Query source) {
        return new QueryCompoundEditEvent(source);
    }
    
    /**
     * The object that is starting or stopping a compound edit.
     */
    private final Query source;
    
    /**
     * A message describing the start or stop of a compound edit.
     */
    private final String message;
    
    /**
     * The state this event is describing. It can either be starting
     * or stopping a compound edit.
     */
    private final CompoundEditState state;

    /**
     * Call this constructor to create a start compound edit event.
     * 
     * @param source
     *            The source of the event.
     * @param message
     *            A message describing the compound edit.
     */
    private QueryCompoundEditEvent(Query source, String message) {
        this.source = source;
        this.message = message;
        state = CompoundEditState.START;
    }

    /**
     * Call this constructor to create an end compound edit event.
     * 
     * @param source
     *            The source of the event.
     */
    private QueryCompoundEditEvent(Query source) {
        this.source = source;
        message = "";
        state = CompoundEditState.END;
    }

    public Query getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public CompoundEditState getState() {
        return state;
    }
    
 }
