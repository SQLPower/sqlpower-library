package ca.sqlpower.sql;
import java.sql.*;
import java.util.*;

public class WebResultSet {

    protected ResultSet rs;
    protected ResultSetMetaData rsmd;
    protected String sqlQuery;
    protected boolean firstColumnIsRowid;
    protected List[] columnChoices;
    protected String[] columnChoicesName;
    protected String[] columnDefaultChoice;
    protected boolean[] columnHasAnyAll;
    protected int[] columnType;

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
	columnType=new int[cols];
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
	if(firstColumnIsRowid) {
	    setColumnType(1, FieldTypes.RADIO);
	}
    }

    public boolean getFirstColumnIsRowid() {
	return firstColumnIsRowid;
    }
    
    /**
     * Get the value of columnType[].  See 
     * {@link ca.sqlpower.sql.FieldTypes} for valid types.
     * i is 1-based.
     *
     * @param colNo The (1-based) column number to get the type of.
     * @return value of the ith column's type.
     */
    public int getColumnType(int colNo) {
	return columnType[colNo-1];
    }
    
    /**
     * Set the value of the ith column's columnType.  See
     * {@link ca.sqlpower.sql.FieldTypes} for valid types.
     * i is 1-based.
     *
     * @param colNo The (1-based) column number to set the type of.
     * @param v Value to assign to the ith column's type.
     */
    public void setColumnType(int colNo, int  v) {
	this.columnType[colNo-1] = v;
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

    // EXPOSED RESULTSET METHODS ARE BELOW HERE
    public boolean next() throws SQLException {
	return rs.next();
    }

    public String getString(String colName) throws SQLException {
	return rs.getString(colName);
    }

    public String getString(int colNum) throws SQLException {
	return rs.getString(colNum);
    }

    public java.sql.Date getDate(String colNum) throws SQLException {
	return rs.getDate(colNum);
    }

    public float getFloat(int colNum) throws SQLException {
	return rs.getFloat(colNum);
    }
}
