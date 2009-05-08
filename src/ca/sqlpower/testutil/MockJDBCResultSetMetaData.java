/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

package ca.sqlpower.testutil;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class MockJDBCResultSetMetaData implements ResultSetMetaData {

    private final int columnCount;
    
    private final String[] catalogName;
    private final String[] columnClassName;
    private final int[] columnDisplaySize;
    private final String[] columnLabel;
    private final String[] columnName;
    private final int[] columnType;
    private final String[] columnTypeName;
    private final int[] precision;
    private final int[] scale;
    private final String[] schemaName;
    private final String[] tableName;
    private final boolean[] autoIncrement;
    private final boolean[] caseSensitive;
    private final boolean[] currency;
    private final boolean[] definitelyWritable;
    private final int[] nullable;
    private final boolean[] readOnly;
    private final boolean[] searchable;
    private final boolean[] signed;
    private final boolean[] writable;
    
    public MockJDBCResultSetMetaData(int columnCount) {
        this.columnCount = columnCount;
        
        catalogName = new String[columnCount];
        columnClassName = new String[columnCount];
        columnDisplaySize = new int[columnCount];
        columnLabel = new String[columnCount];
        columnName = new String[columnCount];
        columnType = new int[columnCount];
        columnTypeName = new String[columnCount];
        precision = new int[columnCount];
        scale = new int[columnCount];
        schemaName = new String[columnCount];
        tableName = new String[columnCount];
        autoIncrement = new boolean[columnCount];
        caseSensitive = new boolean[columnCount];
        currency = new boolean[columnCount];
        definitelyWritable = new boolean[columnCount];
        nullable = new int[columnCount];
        readOnly = new boolean[columnCount];
        searchable = new boolean[columnCount];
        signed = new boolean[columnCount];
        writable = new boolean[columnCount];
    }
    public String getCatalogName(int column) throws SQLException {
        return catalogName[column - 1];
    }

    public String getColumnClassName(int column) throws SQLException {
        return columnClassName[column - 1];
    }

    public int getColumnCount() throws SQLException {
        return columnCount;
    }

    public int getColumnDisplaySize(int column) throws SQLException {
        return columnDisplaySize[column - 1];
    }

    public String getColumnLabel(int column) throws SQLException {
        return columnLabel[column - 1];
    }

    public String getColumnName(int column) throws SQLException {
        return columnName[column - 1];
    }

    public int getColumnType(int column) throws SQLException {
        return columnType[column - 1];
    }

    public String getColumnTypeName(int column) throws SQLException {
        return columnTypeName[column - 1];
    }

    public int getPrecision(int column) throws SQLException {
        return precision[column - 1];
    }

    public int getScale(int column) throws SQLException {
        return scale[column - 1];
    }

    public String getSchemaName(int column) throws SQLException {
        return schemaName[column - 1];
    }

    public String getTableName(int column) throws SQLException {
        return tableName[column - 1];
    }

    public boolean isAutoIncrement(int column) throws SQLException {
        return autoIncrement[column - 1];
    }

    public boolean isCaseSensitive(int column) throws SQLException {
        return caseSensitive[column - 1];
    }

    public boolean isCurrency(int column) throws SQLException {
        return currency[column - 1];
    }

    public boolean isDefinitelyWritable(int column) throws SQLException {
        return definitelyWritable[column - 1];
    }

    public int isNullable(int column) throws SQLException {
        return nullable[column - 1];
    }

    public boolean isReadOnly(int column) throws SQLException {
        return readOnly[column - 1];
    }

    public boolean isSearchable(int column) throws SQLException {
        return searchable[column - 1];
    }

    public boolean isSigned(int column) throws SQLException {
        return signed[column - 1];
    }

    public boolean isWritable(int column) throws SQLException {
        return writable[column - 1];
    }

    public void setCatalogName(int column, String catalogName) {
        this.catalogName[column - 1] = catalogName;
    }

    public void setColumnClassName(int column, String columnClassName) {
        this.columnClassName[column - 1] = columnClassName;
    }

    public void setColumnDisplaySize(int column, int columnDisplaySize) {
        this.columnDisplaySize[column - 1] = columnDisplaySize;
    }

    public void setColumnLabel(int column, String columnLabel) {
        this.columnLabel[column - 1] = columnLabel;
    }

    public void setColumnName(int column, String columnName) {
        this.columnName[column - 1] = columnName;
    }

    public void setColumnType(int column, int columnType) {
        this.columnType[column - 1] = columnType;
    }

    public void setColumnTypeName(int column, String columnTypeName) {
        this.columnTypeName[column - 1] = columnTypeName;
    }

    public void setPrecision(int column, int precision) {
        this.precision[column - 1] = precision;
    }

    public void setScale(int column, int scale) {
        this.scale[column - 1] = scale;
    }

    public void setSchemaName(int column, String schemaName) {
        this.schemaName[column - 1] = schemaName;
    }

    public void setTableName(int column, String tableName) {
        this.tableName[column - 1] = tableName;
    }

    public void setAutoIncrement(int column, boolean autoIncrement) {
        this.autoIncrement[column - 1] = autoIncrement;
    }

    public void setAaseSensitive(int column, boolean caseSensitive) {
        this.caseSensitive[column - 1] = caseSensitive;
    }

    public void setCurrency(int column, boolean currency) {
        this.currency[column - 1] = currency;
    }

    public void setDefinitelyWritable(int column, boolean definitelyWritable) {
        this.definitelyWritable[column - 1] = definitelyWritable;
    }

    public void setNullable(int column, int nullable) {
        this.nullable[column - 1] = nullable;
    }

    public void setReadOnly(int column, boolean readOnly) {
        this.readOnly[column - 1] = readOnly;
    }

    public void setSearchable(int column, boolean searchable) {
        this.searchable[column - 1] = searchable;
    }

    public void setSigned(int column, boolean signed) {
        this.signed[column - 1] = signed;
    }

    public void setWritable(int column, boolean writable) {
        this.writable[column - 1] = writable;
    }
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

}
