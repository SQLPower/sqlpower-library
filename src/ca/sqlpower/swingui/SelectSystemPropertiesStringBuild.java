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

package ca.sqlpower.swingui;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.SPDataSource;

/**
 * This class builds a string of certain system and database connection 
 * properties to display to the user when testing a DB connection or in a log of
 * a SQL query. 
 * <p>
 * If a property is missing from the list, feel free to add to it, or 
 * if a property is unnecessary, do comment it out or delete it.
 * <p>
 * The string building the system properties is independent of the DB connection 
 * properties, so in the event that the connection does fail, the user will still
 * be able to see the system properties.
 * <p>
 * NOTE: This class uses the StringBuilder class to create strings which is not 
 * thread safe.
 *  
 * @author Michelle
 *
 */
public class SelectSystemPropertiesStringBuild {
	
	/**
	 * The datasource is used to get the DB connection information
	 */
	private SPDataSource ds;
	
	/**
	 * The boolean should be true when the string is to be displayed when testing
	 * a connection. The boolean decides whether or not to display the datasource's
	 * name. There is no point in displaying the datasource's name if it is already
	 * shown to the user as it is when testing a connection (in one of the text fields)
	 */
	private boolean testConnection;
	
	private static final Logger logger = Logger.getLogger(SelectSystemPropertiesStringBuild.class);

	/**
	 * The default constructor if only the system properties and not the DB connection
	 * properties are desired.
	 */
	public SelectSystemPropertiesStringBuild(){
		
	}
	
	/**
	 * This constructor is used if there is any need for the DB connection properties
	 * @param ds - The SPDataSource for which connection details are wanted
	 * @param testConnection - True if the string is to be displayed when testing a
	 * connection. False, otherwise.
	 */
	public SelectSystemPropertiesStringBuild(SPDataSource ds, boolean testConnection) {
		this.ds = ds;
		this.testConnection = testConnection;
	}
	
	/**
	 * Generates a string containing details of the DB connection.
	 * The name of the Datasource is shown only is the boolean testConnection is
	 * false.
	 * @return A string of the DB connection properties
	 */
	public String getConnectionInfoString()
	{
		//If the connection fails, it returns an empty string
		StringBuilder summary = new StringBuilder("");
		if(!testConnection) {
		    summary.append(ds.getDisplayName()+": \n");
		}
        Connection con = null;
        try {
            con = ds.createConnection();
            DatabaseMetaData dbmd = con.getMetaData();
            summary.append("Database Product Name: ").append(dbmd.getDatabaseProductName()+"\n");
            summary.append("Database Product Version: ").append(dbmd.getDatabaseProductVersion()+"\n");
            summary.append("Database Driver Name: ").append(dbmd.getDriverName()+"\n");
            summary.append("Database Driver Version: ").append(dbmd.getDriverVersion()+"\n");
            
        } catch (SQLException e) {
            logger.error("Couldn't get database metadata!", e);
            } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                logger.warn("Couldn't close connection", ex);
            } 
            }
                
        return summary.toString();
      }
	
	
	/**
	 * Generates a string containing details of the system properties.
	 * 
	 * @return A string of the system properties
	 */
	public String getConnectionInfoIndependentString() {
		StringBuilder summary = new StringBuilder();
		summary.append("Java Vendor: ").append(System.getProperty("java.vendor")+"\n");
		summary.append("Java Version: ").append(System.getProperty("java.version")+"\n");
		summary.append("Java Runtime Name: ").append(System.getProperty("java.runtime.name")+"\n");
		summary.append("Java Runtime Version: ").append(System.getProperty("java.runtime.version")+"\n");
		summary.append("Java VM Vendor: ").append(System.getProperty("java.vm.vendor")+"\n");
		summary.append("Java VM Version: ").append(System.getProperty("java.vm.version")+"\n");
		summary.append("OS Name: ").append(System.getProperty("os.name")+"\n");
		summary.append("OS Arch: ").append(System.getProperty("os.arch")+"\n");
		summary.append("OS Version: ").append(System.getProperty("os.version")+"\n");
				
		return summary.toString();
	}
	
	/**
	 * Generates a string containing both, the DB connection properties and
	 * the system properties. If the connection fails, it returns only the
	 * system properties.
	 */
	public String generateCompleteString() {
		return getConnectionInfoString() + getConnectionInfoIndependentString();
	}
	
}
