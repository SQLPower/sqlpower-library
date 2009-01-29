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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A non-general demo Validator that uses RegEx matching and
 * looks for strings like Pass, Warn or Fail.
 */
public class DemoTernaryRegExValidator implements Validator {

    private Pattern pattern;
    private String message;

    /**
     * Construct a Validator for regexes
     * @param pattern The regex pattern
     *
     */
    public DemoTernaryRegExValidator() {
        super();
        String pattern = "^(OK|WARN|FAIL)$";
        this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        this.message = "Must match " + pattern;
    }

    public ValidateResult validate(Object contents) {
        String value = (String)contents;
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            String match = matcher.group(1);
            return ValidateResult.createValidateResult(
                    Status.valueOf(match.toUpperCase()), "that's a match (red if you matched FAIL)");
        } else {
            return ValidateResult.createValidateResult(
            Status.FAIL, message);
        }
    }

}
