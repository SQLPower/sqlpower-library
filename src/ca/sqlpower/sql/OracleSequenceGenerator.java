package ca.sqlpower.sql;

import java.sql.*;

public class OracleSequenceGenerator extends SequenceGenerator {

    /**
     * Retrieves a unique long integer value from the specified
     * Oracle sequence.
     *
     * @param con A connection to an Oracle database.
     * @param sequenceTable The name of an Oracle sequence to use.
     * @return A long integer n such than n has never been returned
     * for this sequenceTable and will never again be returned for
     * this sequenceTable.
     * @throws SQLException if a database error occurs.
     */
    public long nextLong(Connection con, String sequenceTable) 
	throws SQLException
    {
	StringBuffer sql=new StringBuffer();
	sql.append("SELECT ")
	    .append(sequenceTable).append(".nextval FROM dual");

	Statement stmt=con.createStatement();
	ResultSet rs=stmt.executeQuery(sql.toString());
	if(!rs.next()) {
	    throw new SQLException("The sequence returned nothing!");
	}
	long nextval=rs.getLong(1);
	stmt.close();

	return nextval;
    }
}
