package ca.sqlpower.sql;
import java.sql.*;

public class WebResultSet {
    ResultSet rs;
    ResultSetMetaData rsmd;
    String sqlQuery;
    boolean firstColumnIsRowid;

    public WebResultSet(ResultSet results, String query) throws SQLException {
	rs=results;
	rsmd=rs.getMetaData();
	sqlQuery=query;
	firstColumnIsRowid=false;
    }

    public void setFirstColumnIsRowid(boolean flag) {
	firstColumnIsRowid=flag;
    }

    public boolean getFirstColumnIsRowid() {
	return firstColumnIsRowid;
    }

    public String getSqlQuery() {
	return sqlQuery;
    }

    public int getColumnCount() throws SQLException {
	return rsmd.getColumnCount();
    }

    public String getColumnLabel(int colNum) throws SQLException {
	return rsmd.getColumnLabel(colNum);
    }

    public boolean next() throws SQLException {
	return rs.next();
    }

    public String getString(String colName) throws SQLException {
	return rs.getString(colName);
    }

    public String getString(int colNum) throws SQLException {
	return rs.getString(colNum);
    }

    public java.sql.Date getDate(String colName) throws SQLException {
	return rs.getDate(colName);
    }

    public float getFloat(String colName) throws SQLException {
	return rs.getFloat(colName);
    }
}
