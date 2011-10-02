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

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.beanutils.ConversionException;

public class URIConverter implements BidirectionalConverter<String, URI> {

	@Override
	public URI convertToComplexType(String convertFrom)
			throws ConversionException {
		try {
			return new URI(convertFrom);
		} catch (URISyntaxException e) {
			throw new ConversionException(e);
		}
	}

	@Override
	public String convertToSimpleType(URI convertFrom,
			Object... additionalInfo) {
		return convertFrom.toString();
	}

}
