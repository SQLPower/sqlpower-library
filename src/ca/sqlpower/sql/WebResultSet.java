package ca.sqlpower.sql;
import ca.sqlpower.util.*;
import java.sql.*;
import java.util.*;

public class WebResultSet {

    protected ResultSet rs;
    protected ResultSetMetaData rsmd;
    protected String sqlQuery;
    protected ColumnFilter[] columnFilter;
    protected List[] columnChoices;
    protected List[] columnMutexList;
    protected String[] columnChoicesName;
    protected String[] columnDefaultChoice;
    protected String[] columnDefaultValue;
    protected boolean[] columnHasAny;
    protected boolean[] columnHasAll;
    protected int[] columnType;
    protected int rowidColNo;

    public WebResultSet(ResultSet results, String query) throws SQLException {
	rs=results;
	rsmd=rs.getMetaData();
	sqlQuery=query;

	int cols=rsmd.getColumnCount();
	columnFilter=new ColumnFilter[cols];
	columnChoices=new List[cols];
	columnMutexList=new List[cols];
	columnChoicesName=new String[cols];
	columnDefaultChoice=new String[cols];
	columnDefaultValue=new String[cols];
	columnHasAny=new boolean[cols];
	columnHasAll=new boolean[cols];
	columnType=new int[cols];
	rowidColNo=0;
    }

    /**
     * sets the column colNo to have both special "any" and "all"
     * choices in its dropdown list of choices.
     *
     * @param colNo the column whose state should be modified
     * @param has the new value for this attribute
     * @deprecated use the separate setColumnHasAny and
     * setColumnHasAll methods instead of this composite one.
     */
    public void setColumnHasAnyAll(int colNo, boolean has) {
	columnHasAny[colNo-1]=has;
	columnHasAll[colNo-1]=has;
    }

    /**
     * gets the logical AND of this column's "any" and "all"
     * attributes.
     *
     * @return true iff columnHasAny(colNo) and columnHasAll(colNo)
     * both return true.
     * @deprecated use the separate getColumnHasAny and
     * getColumnHasAll methods instead of this composite one.
     */
    public boolean getColumnHasAnyAll(int colNo) {
	return columnHasAny[colNo-1] && columnHasAll[colNo-1];
    }

    public void setColumnHasAny(int colNo, boolean has) {
	columnHasAny[colNo-1]=has;
    }

    public boolean getColumnHasAny(int colNo) {
	return columnHasAny[colNo-1];
    }

    public void setColumnHasAll(int colNo, boolean has) {
	columnHasAll[colNo-1]=has;
    }

    public boolean getColumnHasAll(int colNo) {
	return columnHasAll[colNo-1];
    }

    /**
     * Note that filters cannot apply to Date or Numeric column types.
     */
    public void setColumnFilter(int colNo, ColumnFilter filter) {
	columnFilter[colNo-1]=filter;
    }

    public ColumnFilter getColumnFilter(int colNo) {
	return columnFilter[colNo-1];
    }

    public void setColumnChoicesList(int colNo, List choicesList) {
	columnChoices[colNo-1]=choicesList;
    }

    public List getColumnChoicesList(int colNo) {
	return columnChoices[colNo-1];
    }

    public void setColumnMutexList(int colNo, List mutexList) {
	columnMutexList[colNo-1]=mutexList;
    }

    public List getColumnMutexList(int colNo) {
	return columnMutexList[colNo-1];
    }

    public void setColumnChoicesName(int colNo, String choicesName) {
	columnChoicesName[colNo-1]=choicesName;
    }

    public String getColumnChoicesName(int colNo)
	throws ColumnNotDisplayableException {
	if(colNo == rowidColNo) {
	    throw new ColumnNotDisplayableException();
	} else {
	    return columnChoicesName[colNo-1];
	}
    }

    /**
     * Sets the default choice for the USER INPUT ELEMENT associated
     * with this column.
     */
    public void setColumnDefaultChoice(int colNo, String defaultChoice) {
	columnDefaultChoice[colNo-1]=defaultChoice;
    }

    /**
     * Gets the default choice for the USER INPUT ELEMENT associated
     * with this column.
     */
    public String getColumnDefaultChoice(int colNo)
	throws ColumnNotDisplayableException {
	if(colNo == rowidColNo) {
	    throw new ColumnNotDisplayableException();
	} else {
	    return columnDefaultChoice[colNo-1];
	}
    }

    /**
     * Sets the default value EXPECTED FROM THE DATABASE in this
     * column.  Mismatching values will be highlighted in red. A value
     * of null disables this comparison.
     */
    public void setColumnDefaultValue(int colNo, String defaultValue) {
	columnDefaultValue[colNo-1]=defaultValue;
    }

    /**
     * Gets the default value EXPECTED FROM THE DATABASE in this
     * column.
     */
    public String getColumnDefaultValue(int colNo) {
	return columnDefaultValue[colNo-1];
    }

    /**
     * don't use this.
     *
     * @deprecated Set column 1 to have a type of FieldTypes.ROWID
     * instead of using this function.
     */
    public void setShowFirstColumn(boolean flag) {
	if(flag) {
	    setColumnType(1, FieldTypes.ROWID);
	} else {
	    setColumnType(1, FieldTypes.ALPHANUM_CODE);
	    rowidColNo=0;
	}
    }

    /**
     * don't use this.
     *
     * @deprecated Check if column 1 has type FieldTypes.ROWID instead
     * of using this function.
     */
    public boolean getShowFirstColumn() {
	return(getColumnType(1) != FieldTypes.ROWID);
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
	if(v==FieldTypes.ROWID) {
	    if(rowidColNo > 0) {
		throw new IllegalStateException("A resultset can have only one ROWID column");
	    }
	    rowidColNo=colNo;
	}
	this.columnType[colNo-1] = v;	
    }
    
    public String getSqlQuery() {
	return sqlQuery;
    }

    public int getColumnCount() throws SQLException {
	return rsmd.getColumnCount();
    }

    public String getColumnLabel(int colNo)
	throws SQLException, ColumnNotDisplayableException {
	if(colNo == rowidColNo) {
	    throw new ColumnNotDisplayableException();
	} else {
	    return rsmd.getColumnLabel(colNo);
	}
    }

    /**
     * retrieves the current row's unique identifier (the one having
     * the row type of "FieldTypes.ROWID").
     *
     * @return the current row's unique identifier
     * @throws NoRowidException if no column is of type
     * FieldTypes.ROWID
     * @throws SQLException if there is a database error retrieving
     * the current row identifier.
     */
    public String getRowid() throws SQLException, NoRowidException {
	if(rowidColNo>0) {
	    return rs.getString(rowidColNo);
	} else {
	    throw new NoRowidException();
	}
    }

    public String toString() {
	StringBuffer sb=new StringBuffer(1024);
	int numCols=0;

	try {
	    numCols=getColumnCount();
	} catch(SQLException e) {
	    sb.append("SQL Exception while getting column count!");
	}

	for(int i=1; i<=numCols; i++) {
	    try {
		sb.append("Column ")
		    .append(i)
		    .append(": type ")
		    .append(getColumnType(i))
		    .append(", label \"")
		    .append(getColumnLabel(i))
		    .append("\"\n");
	    } catch(SQLException e) {
		sb.append("SQLException processing column!");
	    } catch(ColumnNotDisplayableException e) {
		sb.append("Column not displayable!");
	    }
	}
	return sb.toString();
    }

    // EXPOSED RESULTSET METHODS ARE BELOW HERE
    public boolean next() throws SQLException {
	return rs.next();
    }

    //public String getString(String colName) throws SQLException {
    //return rs.getString(colName);
    //}

    public String getString(int colNo) throws SQLException {
	if(columnFilter[colNo-1]!=null) {
	    return columnFilter[colNo-1].filter(rs.getString(colNo));
	}
	return rs.getString(colNo);
    }

    public java.sql.Date getDate(int colNo) throws SQLException {
	return rs.getDate(colNo);
    }

    public float getFloat(int colNo) throws SQLException {
	return rs.getFloat(colNo);
    }

    /**
     * Closes the JDBC ResultSet's Statement object, thereby freeing
     * the database cursor.  Cursors are a limited resource, so it is
     * important to do this explicitly rather than waiting for garbage
     * collection.
     */
    public void close() throws SQLException {
	rs.getStatement().close();
    }
}
