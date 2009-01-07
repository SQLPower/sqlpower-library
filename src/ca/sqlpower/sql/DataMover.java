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

package ca.sqlpower.sql;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * The DataMover class is used to move data and structure from one
 * database to another, even if they are not from the same vendor.
 */
public class DataMover {
	
	private static final Logger logger = Logger.getLogger(DataMover.class);
	
	protected boolean debug = false;

	/**
	 * Contains the most recently executed SQL Query string.  Useful
	 * in debugging if one of this class's methods throws
	 * SQLException.
	 */
	protected String lastSqlString;

	/**
	 * The database connection to the source system.
	 */
	protected Connection srcCon;	

	/**
	 * The database connection to the target system.
	 */
	protected Connection dstCon;	

	/**
	 * If true, this data mover will try to create the destination
	 * table before inserting into it.
	 */
	protected boolean creatingDestinationTable;

	/**
	 * If true, this data mover will try to delete all the data from
	 * the destination table before loading source data into it.
	 */
	protected boolean truncatingDestinationTable;

	/**
	 * Constructs a data mover instance for moving data from source to
	 * dest.  Sets the connections to non-autocommit mode.
	 */
	public DataMover(Connection dest, Connection source) throws SQLException {
		srcCon = source;
		dstCon = dest;
	}

	/**
	 * Copies all the data in the named source table to a table in the
	 * destination database having the same name.
	 */
	public void copyTable(String tableName) throws SQLException {
		String destTable = tableName.substring(tableName.lastIndexOf('.') + 1);
		copyTable(destTable, tableName);
	}

	/**
	 * Copies all the data in the source table (in the source
	 * database) to the table the the given name in the destination
	 * database.
	 */
	public int copyTable(String destTableName, String sourceTableName) throws SQLException {
		Statement srcStmt = null;
		Statement tmpStmt = null;
		PreparedStatement dstStmt = null;
		ResultSet srcRS = null;
		ResultSetMetaData srcRSMD = null;
		long startTime = System.currentTimeMillis();
		int numRows = 0;
		
		try {
			srcStmt = srcCon.createStatement();
			lastSqlString = "select * from "+sourceTableName;
			srcRS = srcStmt.executeQuery(lastSqlString);
			srcRSMD = srcRS.getMetaData();

			if (debug) logger.debug(summarizeResultSetMetaData(srcRSMD));

			dstCon.setAutoCommit(false);
			if (creatingDestinationTable) {
				try {
					tmpStmt = dstCon.createStatement();
					lastSqlString = "SELECT 1 FROM "+destTableName;
					tmpStmt.executeQuery(lastSqlString);
				} catch (SQLException e) {
					// We assume this means the table needs to be created
					createDestinationTable(srcRSMD, destTableName);
					dstCon.commit();
					logger.debug("Created destination table "+destTableName);
				} finally {
					tmpStmt.close();
					tmpStmt = null;
				}
			}
			
			if (truncatingDestinationTable) {
				tmpStmt = dstCon.createStatement();
				lastSqlString = "DELETE FROM "+destTableName;
				int count = tmpStmt.executeUpdate(lastSqlString);
				logger.debug("Deleted "+count+" rows from destination table");
				tmpStmt.close();
				tmpStmt = null;
			}

			lastSqlString = generateInsertStatement(srcRSMD, destTableName);
			dstStmt = dstCon.prepareStatement(lastSqlString);

			int numberOfColumns = srcRSMD.getColumnCount();
			while (srcRS.next()) {
				if (debug) logger.debug("Row "+numRows);
				for (int col = 1; col <= numberOfColumns; col++) {
				    if (debug) logger.debug(srcRS.getMetaData().getColumnName(col)+":"+srcRS.getObject(col)+ "(type="+srcRSMD.getColumnType(col)+")"+srcRS.getObject(col));
					Object object = null; 
					if (srcRS.getObject(col) != null){
						object = (srcRS.getObject(col).getClass() == BigDecimal.class)? ((BigDecimal) srcRS.getObject(col)).doubleValue():srcRS.getObject(col);
					}
					dstStmt.setObject(col,object , srcRSMD.getColumnType(col));
				}
				dstStmt.executeUpdate();
				numRows++;
			}
			
			dstCon.commit();
			logger.debug("Committed transaction");
			
		} catch (SQLException e) {
			try { 
				dstCon.rollback();
			} catch (Exception e2) {
				logger.error("Roll back on error failed", e2);
			}
			throw e;
		} finally {
			if (srcRS != null) srcRS.close();
			if (srcStmt != null) srcStmt.close();
			if (dstStmt != null) dstStmt.close();
			if (tmpStmt != null) tmpStmt.close();
		}
		long endTime = System.currentTimeMillis();
		long elapsedTime = endTime-startTime;
		logger.debug(numRows+" rows copied in "+elapsedTime+" ms. ("+((double) numRows)/((double) elapsedTime)*1000.0+" rows/sec)");
		return numRows;
	}
	
	protected String summarizeResultSetMetaData(ResultSetMetaData rsmd) throws SQLException {
		StringBuffer summary = new StringBuffer(200);
		int numberOfColumns = rsmd.getColumnCount();
		summary.append("Table ").append(rsmd.getTableName(1)).append("\n");
		for (int col = 1; col <= numberOfColumns; col++) {
			summary.append("Column ").append(col).append(": ");
			summary.append(rsmd.getColumnName(col));
			summary.append(" JDBC datatype ").append(rsmd.getColumnType(col));
			summary.append(" (").append(rsmd.getColumnClassName(col)).append(")");
			summary.append(rsmd.isNullable(col)==rsmd.columnNullable?"":" NOT NULL");
			summary.append("\n");
		}
		return summary.toString();
	}

	/**
	 * Generates a string that you can pass to
	 * Connection.prepareStatement() for inserting the data in the
	 * result set to which rsmd applies.
	 */
	protected String generateInsertStatement(ResultSetMetaData rsmd, String destTable)
		throws SQLException {
		StringBuffer sql = new StringBuffer(200);
		int numberOfColumns = rsmd.getColumnCount();
		sql.append("INSERT INTO ").append(destTable).append(" (");
		for (int col = 1; col <= numberOfColumns; col++) {
			sql.append(rsmd.getColumnName(col));
			if (col != numberOfColumns) {
				sql.append(", ");
			}
		}
		sql.append(") VALUES (");
		for (int col = 1; col <= numberOfColumns; col++) {
			sql.append("?");
			if (col != numberOfColumns) {
				sql.append(", ");
			}
		}
		sql.append(")");
		return sql.toString();
	}

	/**
	 * Creates a table in the destination database that is similar to
	 * the one described by rsmd.
	 */
	protected void createDestinationTable(ResultSetMetaData rsmd, String destTable)
		throws SQLException {
		SqlTypeConverter tc = SqlTypeConverter.getInstance(dstCon);
		StringBuffer sql = new StringBuffer(200);
		int numberOfColumns = rsmd.getColumnCount();
		sql.append("CREATE TABLE ").append(destTable).append(" (\n");
		for (int col = 1; col <= numberOfColumns; col++) {
			sql.append(rsmd.getColumnName(col));
			sql.append(" ").append(tc.convertType(rsmd.getColumnType(col),
												  rsmd.getPrecision(col),
												  rsmd.getScale(col)));
			sql.append(rsmd.isNullable(col)==rsmd.columnNullable?" NULL":" NOT NULL");
			if (col != numberOfColumns) sql.append(",\n");
		}
		sql.append(")");

		Statement stmt = null;
		try {
			stmt = dstCon.createStatement();
			lastSqlString = sql.toString();
			stmt.executeUpdate(lastSqlString);
		} finally {
			if (stmt != null) stmt.close();
		}
	}

	public String getLastSqlString() {
		return lastSqlString;
	}

	/**
	 * Throws UnsupportedOperationException.
	 */
	private void setLastSqlString(String argLastSqlString) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Gets the value of debug
	 *
	 * @return the value of debug
	 */
	public boolean isDebug()  {
		return this.debug;
	}

	/**
	 * Sets the value of debug
	 *
	 * @param argDebug Value to assign to this.debug
	 */
	public void setDebug(boolean argDebug) {
		this.debug = argDebug;
	}

	/**
	 * Gets the value of srcCon
	 *
	 * @return the value of srcCon
	 */
	private Connection getSrcCon()  {
		return this.srcCon;
	}

	/**
	 * Sets the value of srcCon
	 *
	 * @param argSrcCon Value to assign to this.srcCon
	 */
	private void setSrcCon(Connection argSrcCon) {
		this.srcCon = argSrcCon;
	}

	/**
	 * Gets the value of dstCon
	 *
	 * @return the value of dstCon
	 */
	private Connection getDstCon()  {
		return this.dstCon;
	}

	/**
	 * Sets the value of dstCon
	 *
	 * @param argDstCon Value to assign to this.dstCon
	 */
	private void setDstCon(Connection argDstCon) {
		this.dstCon = argDstCon;
	}

	/**
	 * Gets the value of creatingDestinationTable
	 *
	 * @return the value of creatingDestinationTable
	 */
	public boolean isCreatingDestinationTable()  {
		return this.creatingDestinationTable;
	}

	/**
	 * Sets the value of creatingDestinationTable
	 *
	 * @param argCreatingDestinationTable Value to assign to this.creatingDestinationTable
	 */
	public void setCreatingDestinationTable(boolean argCreatingDestinationTable) {
		this.creatingDestinationTable = argCreatingDestinationTable;
	}

	/**
	 * Gets the value of truncatingDestinationTable
	 *
	 * @return the value of truncatingDestinationTable
	 */
	public boolean isTruncatingDestinationTable()  {
		return this.truncatingDestinationTable;
	}

	/**
	 * Sets the value of truncatingDestinationTable
	 *
	 * @param argTruncatingDestinationTable Value to assign to this.truncatingDestinationTable
	 */
	public void setTruncatingDestinationTable(boolean argTruncatingDestinationTable) {
		this.truncatingDestinationTable = argTruncatingDestinationTable;
	}

	public static void main(String[] args) throws Exception {
		DataMover mover = null;
		try {
			String dbxml = "databases.xml";
			if (args.length != 3) {
				System.out.println("Usage: java ca.sqlpower.sql.DataMover source-database-name"
								   +"\n        source-table-name dest-database-name");
				return;
			}
			String srcName = args[0];
			String srcTableName = args[1];
			String dstName = args[2];

			DBCSSource xmlSource = new XMLFileDBCSSource(dbxml);
			List dbcsList = xmlSource.getDBCSList();

			DBConnectionSpec srcDbcs = DBConnectionSpec.searchListForName(dbcsList, srcName);
			if(srcDbcs == null) {
				System.err.println("No database definition '"+srcName+"' in "+dbxml+".");
				return;
			}

			DBConnectionSpec dstDbcs = DBConnectionSpec.searchListForName(dbcsList, dstName);
			if(dstDbcs == null) {
				System.err.println("No database definition '"+dstName+"' in "+dbxml+".");
				return;
			}

			String srcDbclass = srcDbcs.getDriverClass();
			String srcDburl = srcDbcs.getUrl();
			String srcDbuser = srcDbcs.getUser();
			String srcDbpass = srcDbcs.getPass();
			
			String dstDbclass = dstDbcs.getDriverClass();
			String dstDburl = dstDbcs.getUrl();
			String dstDbuser = dstDbcs.getUser();
			String dstDbpass = dstDbcs.getPass();

			Connection srcCon;
			Connection dstCon;
			
			Class.forName(srcDbclass).newInstance();
			Class.forName(dstDbclass).newInstance();
			
			srcCon = DriverManager.getConnection(srcDburl, srcDbuser, srcDbpass);
			dstCon = DriverManager.getConnection(dstDburl, dstDbuser, dstDbpass);
			
			mover = new DataMover(dstCon, srcCon);
			mover.setCreatingDestinationTable(true);
			mover.setTruncatingDestinationTable(true);
			mover.copyTable(srcTableName);
			
			srcCon.close();
			dstCon.close();
		} catch (SQLException e) {
		    e.printStackTrace();
		    if (mover != null) {
		        System.out.println("Offending SQL Statement:\n"+mover.getLastSqlString());
		    }
		}
	}
}
