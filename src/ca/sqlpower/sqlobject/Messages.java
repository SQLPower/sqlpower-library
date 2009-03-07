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

package ca.sqlpower.sqlobject;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
    private static final String BUNDLE_NAME = "ca.sqlpower.sqlobject.messages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle(BUNDLE_NAME);

    private Messages() {
    }

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
    
    public static String getString(String key, String ... params) {
        String message = getString(key);
        for (int i = 0; i < params.length; i++) {
            if (params[i] != null) {
                message = message.replace("{" + i + "}", params[i]); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                message = message.replace("{" + i + "}", "null");    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }
        return message;
    }

}
