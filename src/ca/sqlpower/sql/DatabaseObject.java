package ca.sqlpower.sql;

/**
 * Represents the common fields between all SQL*Power database
 * records.  Currently, this is just the Object name and its object
 * type (the string used to identify the implementing object's type in
 * the security tables and elsewhere in the database).  The
 * ca.sqlpower.dashboard.Kpi class will be the first to implement this
 * interface.
 *
 * <p>Note that this interface extends Serializable, so you must
 * ensure that your implementing classes are indeed serializable.
 *
 * @version $Id$
 */
public interface DatabaseObject extends java.io.Serializable {
	public static final String OBJECT_TYPE_KPI="KPI";

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
