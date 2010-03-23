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

/** The status of a validation
 */
public enum Status {
    /** The validation was successful
     */
    OK,
    /** The validation has some warning, but can proceed with
     * the given user input value (simple validations may be unable
     * to notice this state and might return only OK or FAIL).
     * In MatchMaker, this means that the user can save but can
     * not run the match engine against this setup.
     */
    WARN,
    /** The validation is not acceptable and we cannot proceed
     * with the given user input.
     * In MatchMarker this means that the user cannot save
     * the current form.
     */
    FAIL
}