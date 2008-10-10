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

package ca.sqlpower.swingui.query;

import java.sql.Connection;
import java.sql.Statement;

/**
 * This is a container that holds a connection, a statement that is currently executing
 * on the connection and a boolean that is true if the connection is not in auto commit
 * mode and has had statements executed on it but not committed. This is a collection
 * of elements to store in a map to prevent the need to create multiple maps.
 */
public class ConnectionAndStatementBean {
    
    /**
     * The statement stored in this class
     */
    private Connection con;
    
    /**
     * The statement currently executing on the connection. This will be null if no 
     * statements are currently running on the connection. Only one statement should be
     * run on the connection at a time.
     */
    private Statement currentStmt;
    
    /**
     * A boolean to track if the connection is not in auto commit mode and if there are
     * uncommitted statements executed on it.
     */
    private boolean connectionUncommitted;
    
    public ConnectionAndStatementBean(Connection con) {
        this.con = con;
        currentStmt = null;
        connectionUncommitted = false;
    }

    public Connection getConnection() {
        return con;
    }

    public synchronized void setConnection(Connection con) {
        this.con = con;
    }

    public Statement getCurrentStmt() {
        return currentStmt;
    }

    public synchronized void setCurrentStmt(Statement currentStmt) {
        this.currentStmt = currentStmt;
    }

    public boolean isConnectionUncommitted() {
        return connectionUncommitted;
    }

    public synchronized void setConnectionUncommitted(boolean connectionUncommitted) {
        this.connectionUncommitted = connectionUncommitted;
    }

}

