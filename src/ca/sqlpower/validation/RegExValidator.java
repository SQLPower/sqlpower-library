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

import java.util.regex.Pattern;

/**
 * A Validator that uses RegEx matching; there is no notion of
 * warnings, it either matches() or it doesn't.
 */
public class RegExValidator implements Validator {

    private Pattern pattern;
    private String message;

    /**
     * Construct a Validator for regexes
     * @param pattern The regex pattern
     *
     */
    public RegExValidator(String pattern, String message, boolean caseSensitive) {
        this.pattern = Pattern.compile(pattern, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        this.message = message;
    }

    public RegExValidator(String pattern) {
        this(pattern,"Input text must match pattern:" + pattern, true);
    }

    public ValidateResult validate(Object contents) {
        String value = (String)contents;
        if ( pattern.matcher(value).matches() ) {
            return ValidateResult.createValidateResult(Status.OK, "");
        } else {
            return ValidateResult.createValidateResult(Status.FAIL, message);
        }
    }

}
