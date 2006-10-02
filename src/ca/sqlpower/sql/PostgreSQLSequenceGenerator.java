package ca.sqlpower.sql;

import java.sql.*;

public class PostgreSQLSequenceGenerator extends SequenceGenerator {
    
    private Connection con;
    
    public PostgreSQLSequenceGenerator(Connection con) {
        super();
        this.con = con;
    }

    /**
     * Retrieves a unique long integer value from the specified
     * PostgreSQL sequence.
     *
     * @param con A connection to a PostgreSQL database.
     * @param sequenceTable The name of a PostgreSQL sequence to use.
     * @return A long integer n such than n has never been returned
     * for this sequenceTable and will never again be returned for
     * this sequenceTable.
     * @throws SQLException if a database error occurs.
     */
    public long nextLong(String sequenceTable) throws SQLException {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT nextval(").append(SQL.quote(sequenceTable)).append(")");

        Statement stmt = null;
        long nextval;
        try {
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql.toString());
            if (!rs.next()) {
                throw new SQLException("The sequence returned nothing!");
            }
            nextval = rs.getLong(1);
        } finally {
            if (stmt != null)
                stmt.close();
        }
        return nextval;
    }

    /**
     * Closes the connection that was passed to the constructor.  You
     * should probably close it yourself rather than calling this
     * method.
     */
    public void close() throws SQLException {
        if (con != null) {
            con.close();
        }
    }
}
