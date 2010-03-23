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

package ca.sqlpower.util.reservoir;

/**
 * An exception that is thrown if there are any problems retrieving
 * data from a reservoir data source.  This exception is normally used
 * as a wrapper around some other checked exception such as IOException
 * or SQLException, but a constructor with a message parameter is also
 * available in case the data source needs to throw exceptions for its
 * own reasons.
 */
public class ReservoirDataException extends Exception {
    
    /**
     * Creates a new exception with the given message and no root cause.
     */
    public ReservoirDataException(String message) {
        super(message);
    }
    
    /**
     * Creates a new exception that was caused by the given exception.
     */
    public ReservoirDataException(Throwable cause) {
        super(cause);
    }
}
