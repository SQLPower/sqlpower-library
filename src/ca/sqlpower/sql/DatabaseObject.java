package ca.sqlpower.sql;

/**
 * Represents the common fields between all SQL*Power database
 * records.  Currently, this is just the Object name and its object
 * type (the string used to identify the implementing object's type in
 * the security tables and elsewhere in the database).  The
 * ca.sqlpower.dashboard.Kpi class will be the first to implement this
 * interface.
 *
 * @version $Id$
 */
public interface DatabaseObject {

	/**
	 * Returns the object's name, suitable for use in SQL WHERE clauses.
	 */
	public String getObjectName();

	/**
	 * Returns the object's type, which corresponds with the strings
	 * used to identify object types in the security tables.
	 */
	public String getObjectType();
}
