package ca.sqlpower.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import ca.sqlpower.dashboard.Scorecard;

public class SQLServerSequenceGenerator extends SequenceGenerator {

	private static final Logger logger = Logger.getLogger(SQLServerSequenceGenerator.class);
	
	private Connection con;
	
	public SQLServerSequenceGenerator(Connection con) {
		super();
		this.con = con;
	}
	
	@Override
	public long nextLong(String sequenceTable) throws SQLException {
		StringBuffer selectSql = new StringBuffer();
        selectSql.append("SELECT currval FROM ").append(SQL.escapeStatement(sequenceTable)).append(";");
        long nextval;
        logger.debug("Sequence Generator select SQL statement is: " + selectSql);
        
        Statement stmt = null;
        int oldTransactionIsolation;
        oldTransactionIsolation = con.getTransactionIsolation();
        try {
        	con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(selectSql.toString());
            if (!rs.next()) {
                throw new SQLException("The sequence returned nothing!");
            }
            nextval = rs.getLong(1)+1;
            
            StringBuffer updateSql = new StringBuffer();
            updateSql.append("UPDATE ").append(SQL.escapeStatement(sequenceTable));
            updateSql.append(" SET currval=" + nextval);
            logger.debug("Sequence Generator update SQL statement is: " + updateSql);
            int updateRS = stmt.executeUpdate(updateSql.toString());
            if (updateRS == 0) {
                throw new SQLException("No rows were updated. Serializability should prevent this.");
            }
            
            rs.close();
        } finally {
        	con.setTransactionIsolation(oldTransactionIsolation);
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
