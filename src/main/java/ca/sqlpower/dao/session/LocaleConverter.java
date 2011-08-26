/*
 * Copyright (c) 2011, SQL Power Group Inc.
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

import java.util.Locale;

import org.apache.commons.beanutils.ConversionException;

public class LocaleConverter implements BidirectionalConverter<String, Locale> {
	
	private static final String SEPARATOR = "-";

	@Override
	public String convertToSimpleType(Locale convertFrom,
			Object... additionalInfo) {
		return convertFrom.getLanguage() + SEPARATOR + convertFrom.getCountry() + SEPARATOR + convertFrom.getVariant();
	}

	@Override
	public Locale convertToComplexType(String convertFrom)
			throws ConversionException {
		String[] split = convertFrom.split(SEPARATOR);
		if (split.length == 0 || split[0].length() == 0) throw new ConversionException("All locales must have a language");
		if (split.length == 1 || split[1].length() == 0) return new Locale(split[0]);
		if (split.length == 2 || split[2].length() == 0) return new Locale(split[0], split[1]);
		return new Locale(split[0], split[1], split[2]);
	}

}
