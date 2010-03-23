package ca.sqlpower.sql;

import java.sql.Connection;
import java.sql.SQLException;

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
        if (DBConnection.isOracle(con)) {
            return new OracleSequenceGenerator(con);
        } else if (DBConnection.isPostgres(con)) {
            return new PostgreSQLSequenceGenerator(con);
        } else if (DBConnection.isSQLServer(con)) {
        	return new SQLServerSequenceGenerator(con);
        } else {
            throw new IllegalArgumentException(
                    "The JDBC driver "+con.getClass().getName()+" is not recognised.");
        }
    }

	/**
	 * Tells a SequenceGenerator instance that you no longer need it.
	 * This version does nothing, but the Oracle subclass (for
	 * instance) <b><i><blink><marquee>CLOSES THE DATABASE CONNECTION</marquee></blink></i></b>.
	 * <p>
	 * @deprecated Never use this method. Close the connection youself, then stop
	 * using the sequence generators that hold a reference to the closed connection.
	 */
    @Deprecated
	public void close() throws SQLException {
		return;
	}
}
