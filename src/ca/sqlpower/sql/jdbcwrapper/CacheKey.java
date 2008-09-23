/*
 * Copyright (c) 2008, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
