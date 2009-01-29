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
package ca.sqlpower.validation;

/** The result of a validation
 */
public class ValidateResult {

    private static final ValidateResult SHARED_OK_RESULT =
        new ValidateResult(Status.OK, "");

    private ValidateResult(Status status, String message) {
        if (status == null) {
            throw new IllegalArgumentException("Status may not be null");
        }
        this.status = status;
        this.message = message;
    }

    /**
     * Factory method, since ValidateResults objects are immutable.
     * @param status
     * @param message
     * @return
     */
    public static ValidateResult createValidateResult(Status status, String message) {
        if (status == Status.OK && (message == null || message.length() == 0))
            return SHARED_OK_RESULT;
        return new ValidateResult(status, message);
    }

    /**
     * the status of the validation
     */
    private Status status;
    /**
     * the error or warning or ok message
     */
    private String message;

    public String getMessage() {
        return message;
    }

    public Status getStatus() {
        return status;
    }
    
    @Override
    public String toString() {
        return "ValidateResult@" + System.identityHashCode(this) +
                ": status=" + status + "; message=" + message;
    }
}
