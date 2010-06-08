/*
 * Copyright (c) 2010, SQL Power Group Inc.
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

package ca.sqlpower.dao.session;

import org.apache.commons.beanutils.ConversionException;

/**
 * Converts classes to their fully qualified name and back. If there is more
 * than one class with the same name that is passed through this converter the
 * wrong class may be selected.
 */
public class ClassConverter implements BidirectionalConverter<String, Class<?>> {

    public Class<?> convertToComplexType(String convertFrom)
            throws ConversionException {
        try {
            return getClass().getClassLoader().loadClass(convertFrom);
        } catch (ClassNotFoundException e) {
            throw new ConversionException(e);
        }
    }

    public String convertToSimpleType(Class<?> convertFrom,
            Object... additionalInfo) {
        return convertFrom.getName();
    }

}
