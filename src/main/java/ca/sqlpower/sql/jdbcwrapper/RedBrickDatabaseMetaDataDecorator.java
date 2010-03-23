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

package ca.sqlpower.sql.jdbcwrapper;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.CachedRowSet;

/**
 * Tries to ensure catalogs and schemas are understood not to be supported.
 */
public class RedBrickDatabaseMetaDataDecorator extends DatabaseMetaDataDecorator {

	private static final Logger logger = Logger
			.getLogger(RedBrickDatabaseMetaDataDecorator.class);

    /**
     * Wraps the given database metadata object. This is normally done for you
     * by the {@link RedBrickConnectionDecorator}.
     * 
     * @param delegate The database metadata object to wrap.
     */
	public RedBrickDatabaseMetaDataDecorator(DatabaseMetaData delegate, ConnectionDecorator connectionDecorator) {
		super(delegate, connectionDecorator);
	}
	
	/**
	 * Returns null, because Red Brick doesn't support catalogs.
	 */
    @Override
    public String getCatalogTerm() throws SQLException {
        return null;
    }

    /**
     * Returns null, because Red Brick doesn't support schemas.
     */
    @Override
    public String getSchemaTerm() throws SQLException {
        return null;
    }

    /**
     * Returns a result set with the correct column setup for the getCatalogs()
     * method, but with 0 rows. This is in contrast to the Red Brick driver
     * we're wrapping, which throws an exception for the getCatalogs() method.
     */
    @Override
    public ResultSet getCatalogs() throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = getConnection().createStatement();
            rs = stmt.executeQuery("SELECT 1 AS TABLE_CAT FROM rbw_tables WHERE 1 = 0");
            
            CachedRowSet crs = new CachedRowSet();
            crs.populate(rs);
            return crs;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    logger.error("Failed to close result set. Squishing this exception: ", ex);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    logger.error("Failed to close statement. Squishing this exception: ", ex);
                }
            }
        }
    }

    /**
     * Returns a result set with the correct column setup for the getSchemas()
     * method, but with 0 rows. This is in contrast to the Red Brick driver
     * we're wrapping, which throws an exception for the getSchemas() method.
     */
    @Override
    public ResultSet getSchemas() throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = getConnection().createStatement();
            rs = stmt.executeQuery("SELECT 1 AS TABLE_SCHEM, 1 AS TABLE_CATALOG FROM rbw_tables WHERE 1 = 0");
            
            CachedRowSet crs = new CachedRowSet();
            crs.populate(rs);
            return crs;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    logger.error("Failed to close result set. Squishing this exception: ", ex);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    logger.error("Failed to close statement. Squishing this exception: ", ex);
                }
            }
        }
    }

    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getAttributes(String catalog, String schemaPattern,
            String typeNamePattern, String attributeNamePattern)
            throws SQLException {
        return wrap(super.getAttributes(null, null, typeNamePattern, attributeNamePattern));
    }

    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getBestRowIdentifier(String catalog, String schema,
            String table, int scope, boolean nullable) throws SQLException {
        return wrap(super.getBestRowIdentifier(null, null, table, scope, nullable));
    }
    
    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getColumns(String catalog, String schemaPattern,
            String tableNamePattern, String columnNamePattern)
            throws SQLException {
        return wrap(super.getColumns(null, null, tableNamePattern, columnNamePattern));
    }
    
    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getColumnPrivileges(String catalog, String schema,
            String table, String columnNamePattern) throws SQLException {
        return wrap(super.getColumnPrivileges(null, null, table, columnNamePattern));
    }
    
    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getCrossReference(String primaryCatalog,
            String primarySchema, String primaryTable, String foreignCatalog,
            String foreignSchema, String foreignTable) throws SQLException {
        return wrap(super.getCrossReference(
                null, null, primaryTable,
                null, null, foreignTable));
    }
    
    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getExportedKeys(String catalog, String schema, String table)
            throws SQLException {
        return wrap(super.getExportedKeys(null, null, table));
    }

    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getImportedKeys(String catalog, String schema, String table)
            throws SQLException {
        return wrap(super.getImportedKeys(null, null, table));
    }

    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getIndexInfo(String catalog, String schema, String table,
            boolean unique, boolean approximate) throws SQLException {
        return wrap(super.getIndexInfo(null, null, table, unique, approximate));
    }
    
    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getPrimaryKeys(String catalog, String schema, String table)
            throws SQLException {
        return wrap(super.getPrimaryKeys(null, null, table));
    }

    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getProcedureColumns(String catalog, String schemaPattern,
            String procedureNamePattern, String columnNamePattern)
            throws SQLException {
        return wrap(super.getProcedureColumns(
                null, null, procedureNamePattern, columnNamePattern));
    }
    
    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getProcedures(String catalog, String schemaPattern,
            String procedureNamePattern) throws SQLException {
        return wrap(super.getProcedures(null, null, procedureNamePattern));
    }

    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getSuperTables(String catalog, String schemaPattern,
            String tableNamePattern) throws SQLException {
        return wrap(super.getSuperTables(null, null, tableNamePattern));
    }
    
    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getSuperTypes(String catalog, String schemaPattern,
            String typeNamePattern) throws SQLException {
        return wrap(super.getSuperTypes(null, null, typeNamePattern));
    }
    
    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getTables(String catalog, String schemaPattern,
            String tableNamePattern, String[] types) throws SQLException {
        return wrap(super.getTables(null, null, tableNamePattern, types));
    }

    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getTablePrivileges(String catalog, String schemaPattern,
            String tableNamePattern) throws SQLException {
        return wrap(super.getTablePrivileges(null, null, tableNamePattern));
    }
    
    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getUDTs(String catalog, String schemaPattern,
            String typeNamePattern, int[] types) throws SQLException {
        return wrap(super.getUDTs(null, null, typeNamePattern, types));
    }
    
    /**
     * Forces the catalog and schema arguments to null before delegating the call.
     */
    @Override
    public ResultSet getVersionColumns(String catalog, String schema,
            String table) throws SQLException {
        return wrap(super.getVersionColumns(null, null, table));
    }
    
    @Override
    protected ResultSetDecorator wrap (ResultSet rs) throws SQLException {	
    	return new GenericResultSetDecorator(wrap(rs.getStatement()), rs);
    }
    
    @Override
    protected StatementDecorator wrap (Statement statement) {
    	return new GenericStatementDecorator(connectionDecorator, statement);
    }
}
