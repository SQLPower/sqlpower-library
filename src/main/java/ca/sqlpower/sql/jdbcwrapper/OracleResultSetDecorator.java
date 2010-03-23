package ca.sqlpower.sql.jdbcwrapper;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

public class OracleResultSetDecorator extends ResultSetDecorator {

	public OracleResultSetDecorator(
			Statement parentStatement,
			ResultSet rs) {
		super(parentStatement, rs);
	}

	@Override
	protected ResultSetMetaData makeResultSetMetaDataDecorator(
			ResultSetMetaData rsmd) {
		return new OracleResultSetMetaDataDecorator(rsmd);
	}
	
	@Override
	public Object getObject(int columnIndex) throws SQLException {
		Object returnValue = super.getObject(columnIndex);
		if (returnValue != null &&
				returnValue.getClass().getCanonicalName().equals("oracle.sql.TIMESTAMP")) {
			return this.getTimestamp(columnIndex);
		} else {
			return returnValue;
		}
	}
	
	@Override
	public Object getObject(int i, Map<String, Class<?>> map)
			throws SQLException {
		Object returnValue = super.getObject(i, map);
		if (returnValue != null &&
				returnValue.getClass().getCanonicalName().equals("oracle.sql.TIMESTAMP")) {
			return this.getTimestamp(i);
		} else {
			return returnValue;
		}
	}
	
	@Override
	public Object getObject(String colName, Map<String, Class<?>> map)
			throws SQLException {
		Object returnValue = super.getObject(colName, map);
		if (returnValue != null &&
				returnValue.getClass().getCanonicalName().equals("oracle.sql.TIMESTAMP")) {
			return this.getTimestamp(colName);
		} else {
			return returnValue;
		}
	}
	
	@Override
	public Object getObject(String columnName) throws SQLException {
		Object returnValue = super.getObject(columnName);
		if (returnValue != null &&
				returnValue.getClass().getCanonicalName().equals("oracle.sql.TIMESTAMP")) {
			return this.getTimestamp(columnName);
		} else {
			return returnValue;
		}
	}

	@Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		return convertTimestamp(super.getTimestamp(columnIndex), null);
	}
	
	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal)
			throws SQLException {
		return convertTimestamp(super.getTimestamp(columnIndex, cal), null);
	}
	
	@Override
	public Timestamp getTimestamp(String columnName) throws SQLException {
		return convertTimestamp(super.getTimestamp(columnName), null);
	}
	
	@Override
	public Timestamp getTimestamp(String columnName, Calendar cal)
			throws SQLException {
		return convertTimestamp(super.getTimestamp(columnName, cal), cal);
	}
	
	private Timestamp convertTimestamp(Timestamp oracleTimestamp, Calendar cal)
			throws SQLException 
	{
		try {
			if (oracleTimestamp == null) {
				return null;
			} else if (oracleTimestamp.getClass().getCanonicalName().equals("oracle.sql.TIMESTAMP")) {
				if (cal == null) {
					Method getter = oracleTimestamp.getClass().getDeclaredMethod("timestampValue");
					return (Timestamp)getter.invoke(oracleTimestamp);
				} else {
					Method getter = oracleTimestamp.getClass().getDeclaredMethod("timestampValue", cal.getClass());
					return (Timestamp)getter.invoke(oracleTimestamp, cal);
				}
			} else {
				return oracleTimestamp;
			}
		} catch (SecurityException e) {
			SQLException ex = new SQLException("Cannot convert Oracle Timestamp class because of a security exception.");
			ex.initCause(e);
			throw ex;
		} catch (NoSuchMethodException e) {
			SQLException ex = new SQLException("Cannot convert Oracle Timestamp class because the getTimestamp method could not be found");
			ex.initCause(e);
			throw ex;
		} catch (Exception e) {
			SQLException ex = new SQLException("Cannot convert Oracle Timestamp class because the an exception was encountered");
			ex.initCause(e);
			throw ex;
		}
	}
}
