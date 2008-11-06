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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * The class exists mainly to wrap instances of DatabaseMetaData returned
 * by {@link #getMetaData()} in the appropriate decorator class.
 */
public class SQLServerConnectionDecorator extends GenericConnectionDecorator {

    private static final Logger logger = Logger.getLogger(SQLServerConnectionDecorator.class);
    
    protected SQLServerConnectionDecorator(Connection delegate) {
        super(delegate);
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        DatabaseMetaData rawRSMD = super.getMetaData();
        if (rawRSMD.getDatabaseProductVersion().charAt(0) == '8') {
            return new SQLServer2000DatabaseMetaDataDecorator(rawRSMD);
        } else if (rawRSMD.getDatabaseProductVersion().charAt(0) == '9') {
            return new SQLServer2005DatabaseMetaDataDecorator(rawRSMD);
        } else {
            logger.warn("Unknown database product version: " +
                    rawRSMD.getDatabaseProductVersion() +
                    " -- returning generic SQL Server wrapper");
            return new SQLServerDatabaseMetaDataDecorator(rawRSMD);
        }
    }
}
