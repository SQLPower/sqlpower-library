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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.ConversionException;

/**
 * When using this class, make sure you do not want to use a child list instead.
 * Additionally, you may run into trouble if the converted strings can contain commas.
 */
public class StringListConverter implements BidirectionalConverter<String, List<String>> {

	@Override
	public List<String> convertToComplexType(String convertFrom)
			throws ConversionException {
		String[] split = convertFrom.split(DELIMITER);
		List<String> ls = new ArrayList<String>(split.length);
		for (String s : split) {
			ls.add(s);
		}
		return ls;
	}

	@Override
	public String convertToSimpleType(List<String> convertFrom,
			Object... additionalInfo) {
		StringBuilder returnString = new StringBuilder();
		boolean first = true;
		for (String s : convertFrom) {
			if (!first) returnString.append(DELIMITER);
			first = false;
			returnString.append(s);
		}
		return returnString.toString();
	}
	

}
