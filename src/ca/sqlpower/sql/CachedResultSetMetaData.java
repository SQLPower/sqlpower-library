package ca.sqlpower.sql;

import java.sql.*;

/**
 * A class that implements ResultSetMetaData by making a copy of all
 * the data from another ResultSetMetaData object.  Instances of this
 * class are fully serializable and have no dependencies on other
 * classes (other than java.lang.String).
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class CachedResultSetMetaData implements ResultSetMetaData, java.io.Serializable, Cloneable {

	private int columnCount;
	private boolean autoIncrement[];
	private boolean caseSensitive[];
	private boolean searchable[];
	private boolean currency[];
	private int nullable[];
	private boolean signed[];
	private int columnDisplaySize[];
	private String columnLabel[];
	private String columnName[];
	private String schemaName[];
	private int precision[];
	private int scale[];
	private String tableName[];
	private String catalogName[];
	private int columnType[];
	private String columnTypeName[];
	private boolean readOnly[];
	private boolean writable[];
	private boolean definitelyWritable[];
	private String columnClassName[];

	/**
	 * Creates a copy of the given ResultSetMetaData so that you can
	 * use it without a live reference to the source result set or
	 * database.
	 */
	public CachedResultSetMetaData(ResultSetMetaData source) throws SQLException {
		this.columnCount = source.getColumnCount();
		createArrays(columnCount);
		populate(source);
	}

	protected void createArrays(int columnCount) {
		this.autoIncrement = new boolean[columnCount];
		this.caseSensitive = new boolean[columnCount];
		this.searchable = new boolean[columnCount];
		this.currency = new boolean[columnCount];
		this.nullable = new int[columnCount];
		this.signed = new boolean[columnCount];
		this.columnDisplaySize = new int[columnCount];
		this.columnLabel = new String[columnCount];
		this.columnName = new String[columnCount];
		this.schemaName = new String[columnCount];
		this.precision = new int[columnCount];
		this.scale = new int[columnCount];
		this.tableName = new String[columnCount];
		this.catalogName = new String[columnCount];
		this.columnType = new int[columnCount];
		this.columnTypeName = new String[columnCount];
		this.readOnly = new boolean[columnCount];
		this.writable = new boolean[columnCount];
		this.definitelyWritable = new boolean[columnCount];
		this.columnClassName = new String[columnCount];
	}

	protected void populate(ResultSetMetaData source) throws SQLException {
		for (int i = 0; i < source.getColumnCount(); i++) {
			this.autoIncrement[i] = source.isAutoIncrement(i+1);
			this.caseSensitive[i] = source.isCaseSensitive(i+1);
			this.searchable[i] = source.isSearchable(i+1);
			this.currency[i] = source.isCurrency(i+1);
			this.nullable[i] = source.isNullable(i+1);
			this.signed[i] = source.isSigned(i+1);
			this.columnDisplaySize[i] = source.getColumnDisplaySize(i+1);
			this.columnLabel[i] = source.getColumnLabel(i+1);
			this.columnName[i] = source.getColumnName(i+1);
			this.schemaName[i] = source.getSchemaName(i+1);
			this.precision[i] = source.getPrecision(i+1);
			this.scale[i] = source.getScale(i+1);
			this.tableName[i] = source.getTableName(i+1);
			this.catalogName[i] = source.getCatalogName(i+1);
			this.columnType[i] = source.getColumnType(i+1);
			this.columnTypeName[i] = source.getColumnTypeName(i+1);
			this.readOnly[i] = source.isReadOnly(i+1);
			this.writable[i] = source.isWritable(i+1);
			this.definitelyWritable[i] = source.isDefinitelyWritable(i+1);
			this.columnClassName[i] = source.getColumnClassName(i+1);
		}
	}

	/**
	 * Adds a new column to the metadata.  Feel free to supply
	 * whatever values you want for the various properties.  They
	 * don't matter to internal CachedRowSet code.
	 *
	 * <p>I am aware that this method signature sucks, but what would you
	 * have me do?  Implement setters for each property?
	 */
	public int addColumn(boolean autoIncrementArg,
						 boolean caseSensitiveArg,
						 boolean searchableArg,
						 boolean currencyArg,
						 int nullableArg,
						 boolean signedArg,
						 int columnDisplaySizeArg,
						 String columnLabelArg,
						 String columnNameArg,
						 String schemaNameArg,
						 int precisionArg,
						 int scaleArg,
						 String tableNameArg,
						 String catalogNameArg,
						 int columnTypeArg,
						 String columnTypeNameArg,
						 boolean readOnlyArg,
						 boolean writableArg,
						 boolean definitelyWritableArg,
						 String columnClassNameArg)
		throws SQLException {

		ResultSetMetaData copy = null;
		try {
			copy = (ResultSetMetaData) this.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException("Couldn't clone this instance");
		}
		createArrays(columnCount + 1);
		populate(copy);
		
		this.autoIncrement[columnCount] = autoIncrementArg;
		this.caseSensitive[columnCount] = caseSensitiveArg;
		this.searchable[columnCount] = searchableArg;
		this.currency[columnCount] = currencyArg;
		this.nullable[columnCount] = nullableArg;
		this.signed[columnCount] = signedArg;
		this.columnDisplaySize[columnCount] = columnDisplaySizeArg;
		this.columnLabel[columnCount] = columnLabelArg;
		this.columnName[columnCount] = columnNameArg;
		this.schemaName[columnCount] = schemaNameArg;
		this.precision[columnCount] = precisionArg;
		this.scale[columnCount] = scaleArg;
		this.tableName[columnCount] = tableNameArg;
		this.catalogName[columnCount] = catalogNameArg;
		this.columnType[columnCount] = columnTypeArg;
		this.columnTypeName[columnCount] = columnTypeNameArg;
		this.readOnly[columnCount] = readOnlyArg;
		this.writable[columnCount] = writableArg;
		this.definitelyWritable[columnCount] = definitelyWritableArg;
		this.columnClassName[columnCount] = columnClassNameArg;

		columnCount += 1;
		return columnCount;
	}

	// ==========================================
	// RESULT SET META DATA INTERFACE
	// ==========================================
    public int getColumnCount() throws SQLException {
		return columnCount;
	}

    public boolean isAutoIncrement(int column) throws SQLException {
		return autoIncrement[column - 1];
	}

    public boolean isCaseSensitive(int column) throws SQLException {
		return caseSensitive[column - 1];
	}

    public boolean isSearchable(int column) throws SQLException {
		return searchable[column - 1];
	}

    public boolean isCurrency(int column) throws SQLException {
		return currency[column - 1];
	}

    public int isNullable(int column) throws SQLException {
		return nullable[column - 1];
	}

    public boolean isSigned(int column) throws SQLException {
		return signed[column - 1];
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

    public String getSchemaName(int column) throws SQLException {
		return schemaName[column - 1];
	}

    public int getPrecision(int column) throws SQLException {
		return precision[column - 1];
	}

    public int getScale(int column) throws SQLException {
		return scale[column - 1];
	}

    public String getTableName(int column) throws SQLException {
		return tableName[column - 1];
	}

    public String getCatalogName(int column) throws SQLException {
		return catalogName[column - 1];
	}

    public int getColumnType(int column) throws SQLException {
		return columnType[column - 1];
	}

    public String getColumnTypeName(int column) throws SQLException {
		return columnTypeName[column - 1];
	}

    public boolean isReadOnly(int column) throws SQLException {
		return readOnly[column - 1];
	}

    public boolean isWritable(int column) throws SQLException {
		return writable[column - 1];
	}

    public boolean isDefinitelyWritable(int column) throws SQLException {
		return definitelyWritable[column - 1];
	}

    public String getColumnClassName(int column) throws SQLException {
		return columnClassName[column - 1];
	}
}