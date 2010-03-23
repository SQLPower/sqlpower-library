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
import java.sql.SQLException;

/**
 * A simple object suitable for use as a cache key when caching information
 * about JDBC metadata at the schema level.
 * <p>
 * Instances of this class are immutable.
 */
public final class CacheKey {

    private final String dsAddress;
    private final String catalogName;
    private final String schemaName;

    /**
     * Creates a new cache key. Since instances of SPDataSource can be mutable,
     * the key will depend on a snapshot of the given datasource's pertinent
     * information rather than the datasource object itself.
     * 
     * @param dbmd
     *            The database metadata of the data being cached.
     * @param catalogName
     *            The catalog of the data being cached. Value should be null if
     *            the underlying data source doesn't have catalogs.
     * @param schemaName
     *            The schema of the data being cached. Value should be null if
     *            the underlying data source doesn't have schemas.
     */
    public CacheKey(DatabaseMetaData dbmd, String catalogName, String schemaName) throws SQLException {
        this.dsAddress = dbmd.getURL() + ";" + dbmd.getUserName();
        this.catalogName = catalogName;
        this.schemaName = schemaName;
    }

    /**
     * Generates a hash code based on the data source, catalog, and schema names.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((catalogName == null) ? 0 : catalogName.hashCode());
        result = prime * result + ((dsAddress == null) ? 0 : dsAddress.hashCode());
        result = prime * result + ((schemaName == null) ? 0 : schemaName.hashCode());
        return result;
    }

    /**
     * Implements equality based on the data source, catalog, and schema names.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        
        CacheKey other = (CacheKey) obj;
        if (catalogName == null) {
            if (other.catalogName != null) return false;
        } else if (!catalogName.equals(other.catalogName)) {
            return false;
        }
        
        if (dsAddress == null) {
            if (other.dsAddress != null) return false;
        } else if (!dsAddress.equals(other.dsAddress)) {
            return false;
        }
        
        if (schemaName == null) {
            if (other.schemaName != null) return false;
        } else if (!schemaName.equals(other.schemaName)) {
            return false;
        }
        
        return true;
    }
    
}
