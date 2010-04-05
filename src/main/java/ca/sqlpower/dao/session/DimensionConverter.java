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

import java.awt.Dimension;

import org.apache.commons.beanutils.ConversionException;

public class DimensionConverter implements BidirectionalConverter<String, Dimension> {

    public Dimension convertToComplexType(String convertFrom)
            throws ConversionException {
        
        String[] pieces = convertFrom.split(DELIMITER);
        if (pieces.length != 2) {
            throw new ConversionException(
                    "The value " + convertFrom + " is not a valid serialized Dimension. The serialized" +
                    " form must consist of two integers separated by a comma.");
        }
        
        int width = Integer.valueOf(pieces[0]);
        int height = Integer.valueOf(pieces[1]);

        return new Dimension(width, height);
    }

    public String convertToSimpleType(Dimension convertFrom, Object... additionalInfo) {
        StringBuffer result = new StringBuffer();
        
        result.append(convertFrom.width);
        result.append(DELIMITER);
        result.append(convertFrom.height);

        return result.toString();
    }

}
