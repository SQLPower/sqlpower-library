package ca.sqlpower.sql;

import java.sql.*;

public abstract class SequenceGenerator {

    /**
     * Retrieves a unique long integer value from the specified
     * sequence table. The table will probably need to be set up
     * specially for the particular type or RDBMS that con is
     * connected to.  See subclass javadocs for specifications.
     *
     * @param con A connection to a database of pre-determined type
     * (Oracle vs. SQLServer vs. DB2, etc).
     * @param sequenceTable The name of the database resource to use
     * for generating the unique number.  See subclass documentation
     * for your database's special requirements.
     * @return A long integer n such than n has never been returned
     * for this sequenceTable and will never again be returned for
     * this sequenceTable.
     * @throws SQLException if a database error occurs.
     */
    public abstract long nextLong(String sequenceTable) 
        throws SQLException;

    /**
     * Examines the given (open) connection object and returns a
     * suitable subclass for generating unique sequences.  If your
     * database system is not supported, an exception will be thrown.
     *
     * @param con An open connection to the database you want to
     * generate unique sequences in.
     * @return A suitable subclass of SequenceGenerator for your
     * RDBMS.
	 * @throws IllegalArgumentException if the given connection is not
	 * from a supported database.
     */
    public static SequenceGenerator getInstance(Connection con) {
        String dbClass=con.getClass().getName();

        if(dbClass.indexOf("Oracle") != 0) {
            return new OracleSequenceGenerator(con);
        }
        
        throw new IllegalArgumentException(
               "The driver class "+dbClass+" is not recognised.");
    }

	/**
	 * Tells a SequenceGenerator instance that you no longer need it.
	 * This version does nothing, but the Oracle subclass (for
	 * instance) closes the database connection.
	 */
	public void close() throws SQLException {
		return;
	}
}
