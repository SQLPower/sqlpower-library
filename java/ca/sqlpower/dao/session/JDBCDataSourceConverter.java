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

import org.apache.commons.beanutils.ConversionException;

import ca.sqlpower.dao.session.BidirectionalConverter;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.SPDataSource;

public class JDBCDataSourceConverter implements BidirectionalConverter<String, JDBCDataSource> {

	private final DataSourceCollection<SPDataSource> dsCollection;

	public JDBCDataSourceConverter(DataSourceCollection<SPDataSource> dsCollection) {
		this.dsCollection = dsCollection;
	}
	
	public JDBCDataSource convertToComplexType(String convertFrom)
			throws ConversionException {
		return dsCollection.getDataSource(convertFrom, JDBCDataSource.class);
	}

	public String convertToSimpleType(JDBCDataSource convertFrom,
			Object... additionalInfo) {
		return convertFrom.getName();
	}

}
