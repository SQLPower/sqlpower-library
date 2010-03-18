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

import java.awt.Rectangle;

import org.apache.commons.beanutils.ConversionException;

public class RectangleConverter implements BidirectionalConverter<String, Rectangle> {

    public Rectangle convertToComplexType(String convertFrom)
            throws ConversionException {
        
        String[] pieces = convertFrom.split(DELIMITER);
        int x = Integer.valueOf(pieces[0]);
        int y = Integer.valueOf(pieces[1]);
        int width = Integer.valueOf(pieces[2]);
        int height = Integer.valueOf(pieces[3]);
        return new Rectangle(x, y, width, height);
        
    }

    public String convertToSimpleType(Rectangle convertFrom, Object... additionalInfo) {
        StringBuffer result = new StringBuffer();
        
        result.append(convertFrom.x);
        result.append(DELIMITER);
        result.append(convertFrom.y);
        result.append(DELIMITER);
        result.append(convertFrom.width);
        result.append(DELIMITER);
        result.append(convertFrom.height);

        return result.toString();
    }

}
