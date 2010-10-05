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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.beanutils.ConversionException;

/**
 * Converts a file to a URI string representing the location of the file and
 * back.
 */
public class FileConverter implements BidirectionalConverter<String, File> {

	@Override
	public File convertToComplexType(String convertFrom)
			throws ConversionException {
		try {
			return new File(new URI(convertFrom));
		} catch (URISyntaxException e) {
			throw new ConversionException(e);
		}
	}

	@Override
	public String convertToSimpleType(File convertFrom,
			Object... additionalInfo) {
		return convertFrom.toURI().toString();
	}

}
