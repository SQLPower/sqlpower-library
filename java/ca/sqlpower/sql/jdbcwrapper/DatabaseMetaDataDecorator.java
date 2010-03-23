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
/*
 * Created on Jun 8, 2005
 *
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.sql.jdbcwrapper;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

/**
 * The DatabaseMetaDataDecorator delegates all operations to a protected DatabaseMetaData instance.
 * Subclasses can perform some operations differently if their underlying JDBC driver does not
 * conform to the JDBC specification.
 *
 * @author fuerth
 * @version $Id$
 */
public abstract class DatabaseMetaDataDecorator implements DatabaseMetaData {
    
    /**
     * The instance that performs all JDBC operations.
     */
    protected DatabaseMetaData databaseMetaData;

    /**
     *  TODO: comment this 
     */
    protected ConnectionDecorator connectionDecorator;
    
	/**
	 * An enumeration of caching behaviours for the DatabaseMetaData. For now,
	 * we just have: 
	 * <p>
	 * <li>{@link CacheType#NO_CACHE} - Do not use caching at all.
	 * Always return metadata straight form the database</li> 
	 * <p>
	 * <li>{@link CacheType#EAGER_CACHE} - If the cache is empty, then populate it
	 * from the database. All subsequent database metadata queries will use the
	 * cache, and not the database.</li>
	 */
    public static enum CacheType { NO_CACHE, EAGER_CACHE };

    /**
     * A ThreadLocal variable used to indicate to the DatabaseMetaData
     * implementation on whether to use metadata caching or not for performance.
     * Client code should use {@link #putHint(String, Object)} to set the value.
     */
    protected static final ThreadLocal<CacheType> cacheType = new ThreadLocal<CacheType>();

    /**
     * A ThreadLocal variable used to indicate to the DatabaseMetaData
     * implementation whether or not cached information has gone stale.
     * If the cached information predates this value, it is considered stale and should be flushed.
     * Client code should use {@link #putHint(String, Object)} to set the value.
     */
    protected static final ThreadLocal<Date> cacheStaleDate = new ThreadLocal<Date>();

    /**
     * The key used to set the {@link CacheType} hint when using
     * {@link #putHint(String, Object)} 
     */
    public static final String CACHE_TYPE = "cacheType";

    /**
     * Setting this cache hint causes the cache stale date to be updated to the
     * given time. The value must be a java.util.Date object (usually you will
     * specify new Date())
     */
    public static final String CACHE_STALE_DATE = "cacheStaleDate";

    /**
     * Retrieves a cached result from the give cache, taking into account stale
     * dating and whether or not caching is turned on. Also handles thread
     * synchronization by accessing the cache while synchronized on the given
     * cache object.
     * 
     * @param <T>
     *            The cache's value type
     * @param cache
     *            The cache to retrieve the value from (if appropriate to the
     *            current cache settings). This cache will be flushed if it's
     *            stale.
     * @param key
     *            The key to attempt to retrieve from the cache.
     * @return The cached item (if caching is enabled and the given cache was
     *         not stale) or null.
     */
    protected <T> T getCachedResult(MetaDataCache<CacheKey, T> cache, CacheKey key) {
        CacheType ct = cacheType.get();
        if (ct == CacheType.NO_CACHE) {
            return null;
        }
        synchronized (cache) {
            Date staleDate = cacheStaleDate.get();
            if (staleDate != null && cache.getLastFlushDate().before(staleDate)) {
                cache.flush();
                return null;
            }
            return cache.get(key);
        }
    }

    /**
     * Puts a key-value association into the give cache, taking into account
     * stale dating and whether or not caching is turned on. Also handles thread
     * synchronization by accessing the cache while synchronized on the given
     * cache object.
     * 
     * @param <T>
     *            The cache's value type
     * @param cache
     *            The cache to put the value into (if appropriate to the current
     *            cache settings). This cache will be flushed if it's stale.
     * @param key
     *            The key to store into the cache.
     * @param value
     *            The value to associate with the given key.
     */
    protected <T> void putCachedResult(MetaDataCache<CacheKey, T> cache, CacheKey key, T value) {
        CacheType ct = cacheType.get();
        if (ct == CacheType.NO_CACHE) {
            return;
        }
        synchronized (cache) {
            Date staleDate = cacheStaleDate.get();
            if (staleDate != null && cache.getLastFlushDate().before(staleDate)) {
                cache.flush();
            }
            cache.put(key, value);
        }
    }

    /**
     * Creates a DatabaseMetaDataDecorator which delegates operations to the given delegate.
     * 
     * @param delegate The instance that performs all JDBC operations.
     */
    public DatabaseMetaDataDecorator(DatabaseMetaData delegate, ConnectionDecorator connectionDecorator) {
        this.databaseMetaData = delegate;
        this.connectionDecorator = connectionDecorator;
    }
    
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean allProceduresAreCallable() throws SQLException {
        return databaseMetaData.allProceduresAreCallable();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean allTablesAreSelectable() throws SQLException {
        return databaseMetaData.allTablesAreSelectable();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        return databaseMetaData.dataDefinitionCausesTransactionCommit();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        return databaseMetaData.dataDefinitionIgnoredInTransactions();
    }
    /**
     * @param type
     * @return
     * @throws java.sql.SQLException
     */
    public boolean deletesAreDetected(int type) throws SQLException {
        return databaseMetaData.deletesAreDetected(type);
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        return databaseMetaData.doesMaxRowSizeIncludeBlobs();
    }
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        return databaseMetaData.equals(obj);
    }
    /**
     * @param catalog
     * @param schemaPattern
     * @param typeNamePattern
     * @param attributeNamePattern
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getAttributes(String catalog, String schemaPattern,
            String typeNamePattern, String attributeNamePattern)
            throws SQLException {
        return wrap(databaseMetaData.getAttributes(catalog, schemaPattern,
                typeNamePattern, attributeNamePattern));
    }
    /**
     * @param catalog
     * @param schema
     * @param table
     * @param scope
     * @param nullable
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getBestRowIdentifier(String catalog, String schema,
            String table, int scope, boolean nullable) throws SQLException {
        return wrap(databaseMetaData.getBestRowIdentifier(catalog, schema, table,
                scope, nullable));
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getCatalogs() throws SQLException {
        return wrap(databaseMetaData.getCatalogs());
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getCatalogSeparator() throws SQLException {
        return databaseMetaData.getCatalogSeparator();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getCatalogTerm() throws SQLException {
        return databaseMetaData.getCatalogTerm();
    }
    /**
     * @param catalog
     * @param schema
     * @param table
     * @param columnNamePattern
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getColumnPrivileges(String catalog, String schema,
            String table, String columnNamePattern) throws SQLException {
        return wrap(databaseMetaData.getColumnPrivileges(catalog, schema, table,
                columnNamePattern));
    }
    /**
     * @param catalog
     * @param schemaPattern
     * @param tableNamePattern
     * @param columnNamePattern
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getColumns(String catalog, String schemaPattern,
            String tableNamePattern, String columnNamePattern)
            throws SQLException {
        return wrap(databaseMetaData.getColumns(catalog, schemaPattern,
                tableNamePattern, columnNamePattern));
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public ConnectionDecorator getConnection() throws SQLException {
        return connectionDecorator;
    }
    /**
     * @param primaryCatalog
     * @param primarySchema
     * @param primaryTable
     * @param foreignCatalog
     * @param foreignSchema
     * @param foreignTable
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getCrossReference(String primaryCatalog,
            String primarySchema, String primaryTable, String foreignCatalog,
            String foreignSchema, String foreignTable) throws SQLException {
        return wrap(databaseMetaData.getCrossReference(primaryCatalog,
                primarySchema, primaryTable, foreignCatalog, foreignSchema,
                foreignTable));
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getDatabaseMajorVersion() throws SQLException {
        return databaseMetaData.getDatabaseMajorVersion();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getDatabaseMinorVersion() throws SQLException {
        return databaseMetaData.getDatabaseMinorVersion();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getDatabaseProductName() throws SQLException {
        return databaseMetaData.getDatabaseProductName();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getDatabaseProductVersion() throws SQLException {
        return databaseMetaData.getDatabaseProductVersion();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getDefaultTransactionIsolation() throws SQLException {
        return databaseMetaData.getDefaultTransactionIsolation();
    }
    /**
     * @return
     */
    public int getDriverMajorVersion() {
        return databaseMetaData.getDriverMajorVersion();
    }
    /**
     * @return
     */
    public int getDriverMinorVersion() {
        return databaseMetaData.getDriverMinorVersion();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getDriverName() throws SQLException {
        return databaseMetaData.getDriverName();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getDriverVersion() throws SQLException {
        return databaseMetaData.getDriverVersion();
    }
    /**
     * @param catalog
     * @param schema
     * @param table
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getExportedKeys(String catalog, String schema, String table)
            throws SQLException {
        return wrap(databaseMetaData.getExportedKeys(catalog, schema, table));
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getExtraNameCharacters() throws SQLException {
        return databaseMetaData.getExtraNameCharacters();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getIdentifierQuoteString() throws SQLException {
        return databaseMetaData.getIdentifierQuoteString();
    }
    /**
     * @param catalog
     * @param schema
     * @param table
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getImportedKeys(String catalog, String schema, String table)
            throws SQLException {
        return wrap(databaseMetaData.getImportedKeys(catalog, schema, table));
    }
    /**
     * @param catalog
     * @param schema
     * @param table
     * @param unique
     * @param approximate
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getIndexInfo(String catalog, String schema, String table,
            boolean unique, boolean approximate) throws SQLException {
        return wrap(databaseMetaData.getIndexInfo(catalog, schema, table, unique,
                approximate));
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getJDBCMajorVersion() throws SQLException {
        return databaseMetaData.getJDBCMajorVersion();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getJDBCMinorVersion() throws SQLException {
        return databaseMetaData.getJDBCMinorVersion();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxBinaryLiteralLength() throws SQLException {
        return databaseMetaData.getMaxBinaryLiteralLength();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxCatalogNameLength() throws SQLException {
        return databaseMetaData.getMaxCatalogNameLength();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxCharLiteralLength() throws SQLException {
        return databaseMetaData.getMaxCharLiteralLength();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxColumnNameLength() throws SQLException {
        return databaseMetaData.getMaxColumnNameLength();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxColumnsInGroupBy() throws SQLException {
        return databaseMetaData.getMaxColumnsInGroupBy();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxColumnsInIndex() throws SQLException {
        return databaseMetaData.getMaxColumnsInIndex();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxColumnsInOrderBy() throws SQLException {
        return databaseMetaData.getMaxColumnsInOrderBy();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxColumnsInSelect() throws SQLException {
        return databaseMetaData.getMaxColumnsInSelect();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxColumnsInTable() throws SQLException {
        return databaseMetaData.getMaxColumnsInTable();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxConnections() throws SQLException {
        return databaseMetaData.getMaxConnections();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxCursorNameLength() throws SQLException {
        return databaseMetaData.getMaxCursorNameLength();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxIndexLength() throws SQLException {
        return databaseMetaData.getMaxIndexLength();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxProcedureNameLength() throws SQLException {
        return databaseMetaData.getMaxProcedureNameLength();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxRowSize() throws SQLException {
        return databaseMetaData.getMaxRowSize();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxSchemaNameLength() throws SQLException {
        return databaseMetaData.getMaxSchemaNameLength();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxStatementLength() throws SQLException {
        return databaseMetaData.getMaxStatementLength();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxStatements() throws SQLException {
        return databaseMetaData.getMaxStatements();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxTableNameLength() throws SQLException {
        return databaseMetaData.getMaxTableNameLength();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxTablesInSelect() throws SQLException {
        return databaseMetaData.getMaxTablesInSelect();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getMaxUserNameLength() throws SQLException {
        return databaseMetaData.getMaxUserNameLength();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getNumericFunctions() throws SQLException {
        return databaseMetaData.getNumericFunctions();
    }
    /**
     * @param catalog
     * @param schema
     * @param table
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getPrimaryKeys(String catalog, String schema, String table)
            throws SQLException {
        return wrap(databaseMetaData.getPrimaryKeys(catalog, schema, table));
    }
    /**
     * @param catalog
     * @param schemaPattern
     * @param procedureNamePattern
     * @param columnNamePattern
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getProcedureColumns(String catalog, String schemaPattern,
            String procedureNamePattern, String columnNamePattern)
            throws SQLException {
        return wrap(databaseMetaData.getProcedureColumns(catalog, schemaPattern,
                procedureNamePattern, columnNamePattern));
    }
    /**
     * @param catalog
     * @param schemaPattern
     * @param procedureNamePattern
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getProcedures(String catalog, String schemaPattern,
            String procedureNamePattern) throws SQLException {
        return wrap(databaseMetaData.getProcedures(catalog, schemaPattern,
                procedureNamePattern));
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getProcedureTerm() throws SQLException {
        return databaseMetaData.getProcedureTerm();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getResultSetHoldability() throws SQLException {
        return databaseMetaData.getResultSetHoldability();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getSchemas() throws SQLException {
        return wrap(databaseMetaData.getSchemas());
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getSchemaTerm() throws SQLException {
        return databaseMetaData.getSchemaTerm();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getSearchStringEscape() throws SQLException {
        return databaseMetaData.getSearchStringEscape();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getSQLKeywords() throws SQLException {
        return databaseMetaData.getSQLKeywords();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public int getSQLStateType() throws SQLException {
        return databaseMetaData.getSQLStateType();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getStringFunctions() throws SQLException {
        return databaseMetaData.getStringFunctions();
    }
    /**
     * @param catalog
     * @param schemaPattern
     * @param tableNamePattern
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getSuperTables(String catalog, String schemaPattern,
            String tableNamePattern) throws SQLException {
        return wrap(databaseMetaData.getSuperTables(catalog, schemaPattern,
                tableNamePattern));
    }
    /**
     * @param catalog
     * @param schemaPattern
     * @param typeNamePattern
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getSuperTypes(String catalog, String schemaPattern,
            String typeNamePattern) throws SQLException {
        return wrap(databaseMetaData.getSuperTypes(catalog, schemaPattern,
                typeNamePattern));
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getSystemFunctions() throws SQLException {
        return databaseMetaData.getSystemFunctions();
    }
    /**
     * @param catalog
     * @param schemaPattern
     * @param tableNamePattern
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getTablePrivileges(String catalog, String schemaPattern,
            String tableNamePattern) throws SQLException {
        return wrap(databaseMetaData.getTablePrivileges(catalog, schemaPattern,
                tableNamePattern));
    }
    /**
     * @param catalog
     * @param schemaPattern
     * @param tableNamePattern
     * @param types
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getTables(String catalog, String schemaPattern,
            String tableNamePattern, String[] types) throws SQLException {
        return wrap(databaseMetaData.getTables(catalog, schemaPattern,
                tableNamePattern, types));
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getTableTypes() throws SQLException {
        return wrap(databaseMetaData.getTableTypes());
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getTimeDateFunctions() throws SQLException {
        return databaseMetaData.getTimeDateFunctions();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getTypeInfo() throws SQLException {
        return wrap(databaseMetaData.getTypeInfo());
    }
    /**
     * @param catalog
     * @param schemaPattern
     * @param typeNamePattern
     * @param types
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getUDTs(String catalog, String schemaPattern,
            String typeNamePattern, int[] types) throws SQLException {
        return wrap(databaseMetaData.getUDTs(catalog, schemaPattern,
                typeNamePattern, types));
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getURL() throws SQLException {
        return databaseMetaData.getURL();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public String getUserName() throws SQLException {
        return databaseMetaData.getUserName();
    }
    /**
     * @param catalog
     * @param schema
     * @param table
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet getVersionColumns(String catalog, String schema,
            String table) throws SQLException {
        return wrap(databaseMetaData.getVersionColumns(catalog, schema, table));
    }
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return databaseMetaData.hashCode();
    }
    /**
     * @param type
     * @return
     * @throws java.sql.SQLException
     */
    public boolean insertsAreDetected(int type) throws SQLException {
        return databaseMetaData.insertsAreDetected(type);
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean isCatalogAtStart() throws SQLException {
        return databaseMetaData.isCatalogAtStart();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean isReadOnly() throws SQLException {
        return databaseMetaData.isReadOnly();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean locatorsUpdateCopy() throws SQLException {
        return databaseMetaData.locatorsUpdateCopy();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean nullPlusNonNullIsNull() throws SQLException {
        return databaseMetaData.nullPlusNonNullIsNull();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean nullsAreSortedAtEnd() throws SQLException {
        return databaseMetaData.nullsAreSortedAtEnd();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean nullsAreSortedAtStart() throws SQLException {
        return databaseMetaData.nullsAreSortedAtStart();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean nullsAreSortedHigh() throws SQLException {
        return databaseMetaData.nullsAreSortedHigh();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean nullsAreSortedLow() throws SQLException {
        return databaseMetaData.nullsAreSortedLow();
    }
    /**
     * @param type
     * @return
     * @throws java.sql.SQLException
     */
    public boolean othersDeletesAreVisible(int type) throws SQLException {
        return databaseMetaData.othersDeletesAreVisible(type);
    }
    /**
     * @param type
     * @return
     * @throws java.sql.SQLException
     */
    public boolean othersInsertsAreVisible(int type) throws SQLException {
        return databaseMetaData.othersInsertsAreVisible(type);
    }
    /**
     * @param type
     * @return
     * @throws java.sql.SQLException
     */
    public boolean othersUpdatesAreVisible(int type) throws SQLException {
        return databaseMetaData.othersUpdatesAreVisible(type);
    }
    /**
     * @param type
     * @return
     * @throws java.sql.SQLException
     */
    public boolean ownDeletesAreVisible(int type) throws SQLException {
        return databaseMetaData.ownDeletesAreVisible(type);
    }
    /**
     * @param type
     * @return
     * @throws java.sql.SQLException
     */
    public boolean ownInsertsAreVisible(int type) throws SQLException {
        return databaseMetaData.ownInsertsAreVisible(type);
    }
    /**
     * @param type
     * @return
     * @throws java.sql.SQLException
     */
    public boolean ownUpdatesAreVisible(int type) throws SQLException {
        return databaseMetaData.ownUpdatesAreVisible(type);
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean storesLowerCaseIdentifiers() throws SQLException {
        return databaseMetaData.storesLowerCaseIdentifiers();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        return databaseMetaData.storesLowerCaseQuotedIdentifiers();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean storesMixedCaseIdentifiers() throws SQLException {
        return databaseMetaData.storesMixedCaseIdentifiers();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        return databaseMetaData.storesMixedCaseQuotedIdentifiers();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean storesUpperCaseIdentifiers() throws SQLException {
        return databaseMetaData.storesUpperCaseIdentifiers();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        return databaseMetaData.storesUpperCaseQuotedIdentifiers();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        return databaseMetaData.supportsAlterTableWithAddColumn();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        return databaseMetaData.supportsAlterTableWithDropColumn();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        return databaseMetaData.supportsANSI92EntryLevelSQL();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsANSI92FullSQL() throws SQLException {
        return databaseMetaData.supportsANSI92FullSQL();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        return databaseMetaData.supportsANSI92IntermediateSQL();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsBatchUpdates() throws SQLException {
        return databaseMetaData.supportsBatchUpdates();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsCatalogsInDataManipulation() throws SQLException {
        return databaseMetaData.supportsCatalogsInDataManipulation();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        return databaseMetaData.supportsCatalogsInIndexDefinitions();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
        return databaseMetaData.supportsCatalogsInPrivilegeDefinitions();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        return databaseMetaData.supportsCatalogsInProcedureCalls();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        return databaseMetaData.supportsCatalogsInTableDefinitions();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsColumnAliasing() throws SQLException {
        return databaseMetaData.supportsColumnAliasing();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsConvert() throws SQLException {
        return databaseMetaData.supportsConvert();
    }
    /**
     * @param fromType
     * @param toType
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsConvert(int fromType, int toType)
            throws SQLException {
        return databaseMetaData.supportsConvert(fromType, toType);
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsCoreSQLGrammar() throws SQLException {
        return databaseMetaData.supportsCoreSQLGrammar();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsCorrelatedSubqueries() throws SQLException {
        return databaseMetaData.supportsCorrelatedSubqueries();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsDataDefinitionAndDataManipulationTransactions()
            throws SQLException {
        return databaseMetaData
                .supportsDataDefinitionAndDataManipulationTransactions();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsDataManipulationTransactionsOnly()
            throws SQLException {
        return databaseMetaData.supportsDataManipulationTransactionsOnly();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsDifferentTableCorrelationNames() throws SQLException {
        return databaseMetaData.supportsDifferentTableCorrelationNames();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsExpressionsInOrderBy() throws SQLException {
        return databaseMetaData.supportsExpressionsInOrderBy();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsExtendedSQLGrammar() throws SQLException {
        return databaseMetaData.supportsExtendedSQLGrammar();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsFullOuterJoins() throws SQLException {
        return databaseMetaData.supportsFullOuterJoins();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsGetGeneratedKeys() throws SQLException {
        return databaseMetaData.supportsGetGeneratedKeys();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsGroupBy() throws SQLException {
        return databaseMetaData.supportsGroupBy();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsGroupByBeyondSelect() throws SQLException {
        return databaseMetaData.supportsGroupByBeyondSelect();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsGroupByUnrelated() throws SQLException {
        return databaseMetaData.supportsGroupByUnrelated();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        return databaseMetaData.supportsIntegrityEnhancementFacility();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsLikeEscapeClause() throws SQLException {
        return databaseMetaData.supportsLikeEscapeClause();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsLimitedOuterJoins() throws SQLException {
        return databaseMetaData.supportsLimitedOuterJoins();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsMinimumSQLGrammar() throws SQLException {
        return databaseMetaData.supportsMinimumSQLGrammar();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        return databaseMetaData.supportsMixedCaseIdentifiers();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        return databaseMetaData.supportsMixedCaseQuotedIdentifiers();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsMultipleOpenResults() throws SQLException {
        return databaseMetaData.supportsMultipleOpenResults();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsMultipleResultSets() throws SQLException {
        return databaseMetaData.supportsMultipleResultSets();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsMultipleTransactions() throws SQLException {
        return databaseMetaData.supportsMultipleTransactions();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsNamedParameters() throws SQLException {
        return databaseMetaData.supportsNamedParameters();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsNonNullableColumns() throws SQLException {
        return databaseMetaData.supportsNonNullableColumns();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        return databaseMetaData.supportsOpenCursorsAcrossCommit();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        return databaseMetaData.supportsOpenCursorsAcrossRollback();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        return databaseMetaData.supportsOpenStatementsAcrossCommit();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        return databaseMetaData.supportsOpenStatementsAcrossRollback();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsOrderByUnrelated() throws SQLException {
        return databaseMetaData.supportsOrderByUnrelated();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsOuterJoins() throws SQLException {
        return databaseMetaData.supportsOuterJoins();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsPositionedDelete() throws SQLException {
        return databaseMetaData.supportsPositionedDelete();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsPositionedUpdate() throws SQLException {
        return databaseMetaData.supportsPositionedUpdate();
    }
    /**
     * @param type
     * @param concurrency
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsResultSetConcurrency(int type, int concurrency)
            throws SQLException {
        return databaseMetaData.supportsResultSetConcurrency(type, concurrency);
    }
    /**
     * @param holdability
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsResultSetHoldability(int holdability)
            throws SQLException {
        return databaseMetaData.supportsResultSetHoldability(holdability);
    }
    /**
     * @param type
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsResultSetType(int type) throws SQLException {
        return databaseMetaData.supportsResultSetType(type);
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsSavepoints() throws SQLException {
        return databaseMetaData.supportsSavepoints();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsSchemasInDataManipulation() throws SQLException {
        return databaseMetaData.supportsSchemasInDataManipulation();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        return databaseMetaData.supportsSchemasInIndexDefinitions();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        return databaseMetaData.supportsSchemasInPrivilegeDefinitions();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        return databaseMetaData.supportsSchemasInProcedureCalls();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsSchemasInTableDefinitions() throws SQLException {
        return databaseMetaData.supportsSchemasInTableDefinitions();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsSelectForUpdate() throws SQLException {
        return databaseMetaData.supportsSelectForUpdate();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsStatementPooling() throws SQLException {
        return databaseMetaData.supportsStatementPooling();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsStoredProcedures() throws SQLException {
        return databaseMetaData.supportsStoredProcedures();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsSubqueriesInComparisons() throws SQLException {
        return databaseMetaData.supportsSubqueriesInComparisons();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsSubqueriesInExists() throws SQLException {
        return databaseMetaData.supportsSubqueriesInExists();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsSubqueriesInIns() throws SQLException {
        return databaseMetaData.supportsSubqueriesInIns();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        return databaseMetaData.supportsSubqueriesInQuantifieds();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsTableCorrelationNames() throws SQLException {
        return databaseMetaData.supportsTableCorrelationNames();
    }
    /**
     * @param level
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsTransactionIsolationLevel(int level)
            throws SQLException {
        return databaseMetaData.supportsTransactionIsolationLevel(level);
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsTransactions() throws SQLException {
        return databaseMetaData.supportsTransactions();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsUnion() throws SQLException {
        return databaseMetaData.supportsUnion();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean supportsUnionAll() throws SQLException {
        return databaseMetaData.supportsUnionAll();
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return databaseMetaData.toString();
    }
    /**
     * @param type
     * @return
     * @throws java.sql.SQLException
     */
    public boolean updatesAreDetected(int type) throws SQLException {
        return databaseMetaData.updatesAreDetected(type);
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean usesLocalFilePerTable() throws SQLException {
        return databaseMetaData.usesLocalFilePerTable();
    }
    /**
     * @return
     * @throws java.sql.SQLException
     */
    public boolean usesLocalFiles() throws SQLException {
        return databaseMetaData.usesLocalFiles();
    }

    /**
     * Sets a behavioural hint visible to all decorated DatabaseMetaData
     * instances when invoked on the current thread.
     * 
     * @param key
     *            See {@link #CACHE_STALE_DATE} and {@link #CACHE_TYPE}.
     * @param value
     *            The value to associate with the given key. Consult the
     *            documentation for each key to know which value type to use.
     */
    public static void putHint(String key, Object value) {
    	if (key.equals(CACHE_TYPE)) {
    		if (value instanceof CacheType) {
    			cacheType.set((CacheType) value);
    		} else {
    			cacheType.set(null);
    		}
    	} else if (key.equals(CACHE_STALE_DATE)) {
    	    cacheStaleDate.set((Date) value);
    	}
    }
    
    protected abstract ResultSetDecorator wrap (ResultSet rs) throws SQLException ;
    protected abstract StatementDecorator wrap (Statement statement) throws SQLException ;
}
