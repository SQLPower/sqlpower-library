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
import java.util.Collections;
import java.util.List;

import org.apache.commons.beanutils.ConversionException;

/**
 * When using this class, make sure you do not want to use a child list instead. If the list is a
 * a specific object type supported by this converter, it will return a list of that object type.
 * Otherwise it will just return a list of Strings;
 * <p>
 * Will convert to the following types:
 * <ul>
 * 		<li> <b> String </b> Note: You may run into trouble if the converted strings can contain commas.</li>
 * 		<li> <b> Integer </b></li>
 * </ul>
 */
public class ListConverter implements BidirectionalConverter<String, List<Object>> {

	@Override
	public List<Object> convertToComplexType(String convertFrom) throws ConversionException {
		
		if(convertFrom.length() > 0) {
			/**
			 * default (0): String
			 * 1: Integer
			 */
			int type = 0;
			boolean notInt = false;
			
			convertFrom = convertFrom.substring(1, convertFrom.length()-1);
			String[] split = convertFrom.split(DELIMITER);
			for (String s : split) {
				try {
			        if(!notInt) 
			        {
			        	Integer.valueOf(s);
			        	type = 1;
			        }
			    } catch (NumberFormatException e) {
			    	notInt = true;
			    	type = 0;
			    }
			}
			List<Object> ls = new ArrayList<Object>(split.length);
			for (String s : split) {
				switch(type) {
				case 1:
					ls.add(new Integer(Integer.parseInt(s)));
					break;
				default:
					ls.add(s);
					break;
				}
			}
			return ls;
		} else {
			return Collections.emptyList();
		}
		
	}

	@Override
	public String convertToSimpleType(List<Object> convertFrom, Object... additionalInfo) {
		
		StringBuilder returnString = new StringBuilder();
		if(!convertFrom.isEmpty()) {
			returnString.append("[");
		}
		boolean first = true;
		for (Object s : convertFrom) {
			if (!first) returnString.append(DELIMITER);
			first = false;
			returnString.append(s.toString());
		}
		if(!convertFrom.isEmpty()) {
			returnString.append("]");
		}
		return returnString.toString();
	}
	

}
