package ca.sqlpower.sql;
import java.sql.*;
import java.util.*;

public class WebResultSet {
    ResultSet rs;
    ResultSetMetaData rsmd;
    String sqlQuery;
    boolean firstColumnIsRowid;
    List[] columnChoices;
    String[] columnChoicesName;
    String[] columnDefaultChoice;
    boolean[] columnHasAnyAll;

    public WebResultSet(ResultSet results, String query) throws SQLException {
	rs=results;
	rsmd=rs.getMetaData();
	sqlQuery=query;
	firstColumnIsRowid=false;

	int cols=rsmd.getColumnCount();
	columnChoices=new List[cols];
	columnChoicesName=new String[cols];
	columnDefaultChoice=new String[cols];
	columnHasAnyAll=new boolean[cols];
    }

    public void setColumnHasAnyAll(int colNo, boolean has) {
	columnHasAnyAll[colNo-1]=has;
    }

    public boolean getColumnHasAnyAll(int colNo) {
	return columnHasAnyAll[colNo-1];
    }

    public void setColumnChoicesList(int colNo, List choicesList) {
	columnChoices[colNo-1]=choicesList;
    }

    public void setColumnChoicesName(int colNo, String choicesName) {
	columnChoicesName[colNo-1]=choicesName;
    }

    public void setColumnDefaultChoice(int colNo, String defaultChoice) {
	columnDefaultChoice[colNo-1]=defaultChoice;
    }

    public List getColumnChoicesList(int colNo) {
	return columnChoices[colNo-1];
    }

    public String getColumnChoicesName(int colNo) {
	return columnChoicesName[colNo-1];
    }

    public String getColumnDefaultChoice(int colNo) {
	return columnDefaultChoice[colNo-1];
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
