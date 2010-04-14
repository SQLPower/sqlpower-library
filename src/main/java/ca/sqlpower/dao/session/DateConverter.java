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

package ca.sqlpower.dao.session;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Converts a {@link Date} to its string representation and back.
 */
public class DateConverter implements BidirectionalConverter<String, Date> {

	private final static DateFormat dateFormatter = 
			new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS_Z");
	
	public Date convertToComplexType(String convertFrom) {
		try {
			return dateFormatter.parse(convertFrom);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public String convertToSimpleType(Date convertFrom, Object ... additionalInfo) {
		return dateFormatter.format(convertFrom);
	}
}
