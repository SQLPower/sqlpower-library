package ca.sqlpower.sql;
import java.sql.*;

public class WebResultSet {
    ResultSet rs;

    public WebResultSet(ResultSet results) {
	rs=results;
    }

    public boolean next() throws SQLException {
	return rs.next();
    }

    public String getString(String colName) {
	throw new UnsupportedOperationException();
    }

    public java.sql.Date getDate(String colName) {
	throw new UnsupportedOperationException();
    }

    public float getFloat(String colName) {
	throw new UnsupportedOperationException();
    }
}
