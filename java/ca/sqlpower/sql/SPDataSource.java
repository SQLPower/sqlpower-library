/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
/*
 * Created on Jun 28, 2005
 *
 * This code belongs to SQL Power.
 */
package ca.sqlpower.sql;

/**
 * The SPDataSource represents a database that the Power Loader or
 * the Architect can connect to.  It holds all the information required for
 * making JDBC, ODBC, or native Oracle connections (depending on what type
 * of database the connection is for).
 *
 * @see ca.sqlpower.architect.PlDotIni
 * @author jack, jonathan
 */

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public abstract class SPDataSource implements Comparable<SPDataSource> {

    private static final Logger logger = Logger.getLogger(SPDataSource.class);
    
    /**
     * This will return a more user friendly name to a class type. This way we can
     * define a more descriptive name for classes that extends SPDataSource.
     */
    public static String getUserFriendlyName(Class<? extends SPDataSource> dsType) {
        if (dsType.equals(JDBCDataSource.class)) {
            return "Database connection";
        } else if (dsType.equals(Olap4jDataSource.class)) {
            return "OLAP connection";
        } else {
            return dsType.getSimpleName();
        }
    }
    
    /**
     * This is the logical name, and also the display name, key in the map.
     */
    public static final String PL_LOGICAL = "Logical";

    /**
     * JDBC driver pathname prefix that says to look for a JAR file resource on
     * the classpath.
     */
    public static final String BUILTIN = "builtin:";
    
    /**
     * These are the actual properties that appear in the file for this data source.
     * The getters for various properties will consult the parent type where appropriate
     * (for example, when a value is missing from this map).
     */
	private Map<String,String> properties;

    /**
     * The collection of data sources that this data source belongs to.
     */
    private final DataSourceCollection<SPDataSource> parentCollection;
	
    /**
	 * This field is transient; don't access it directly becuase it
	 * will disappear when this instance is serialized.
	 */
	private transient PropertyChangeSupport pcs;

	/**
	 * JDBC driver pathname prefix that says to look for a JAR file resource on
	 * the remote SQL Power Enterprise server we're attached to.
	 */
	public static final String SERVER = "server:";

	/**
	 * Returns this DataSource's property change support, creating
	 * a new one if necessary.
	 */
	private PropertyChangeSupport getPcs() {
		if (pcs == null) pcs = new PropertyChangeSupport(this);
		return pcs;
	}

    /**
     * Creates a new SPDataSource with a blank parent type and all other properties set to null.
     */
	public SPDataSource(DataSourceCollection<SPDataSource> parentCollection) {
		properties = new LinkedHashMap<String,String>();
        this.parentCollection = parentCollection;
	}

    /**
     * Copy constructor. Creates a semi-independent copy of the given data source.
     * <p>
     * This is for testing only!  The ramifications of using this constructor
     * in production have not been thought through!
     *
     * @param copyMe the SPDataSource to make a copy of.
     */
    public SPDataSource(SPDataSource copyMe) {
        properties = new LinkedHashMap<String, String>(copyMe.properties);
        parentCollection = copyMe.parentCollection;
    }

	/**
	 * The method that actually modifies the property map.
	 *
	 * @param key The key to use in the map (this will be a PL.INI property name)
	 * @param value The value that corresponds with the key
	 * @param propertyName The name of the Java Beans property that changed.  This will
	 * be the property name in the resulting PropertyChangeEvent.
	 * @return The old value of the property.
	 */
	protected String putImpl(String key, String value, String propertyName) {
		String oldValue = get(key);
		properties.put(key, value);
		getPcs().firePropertyChange(propertyName, oldValue, value);
		return oldValue;
	}

	/**
	 * Adds the given key to the map.
	 *
	 * @param key The key to use.
	 * @param value The value to associate with key.
	 * @return The old value of the property.
	 */
	public String put(String key, String value) {
		return putImpl(key, value, key);
	}

    /**
     * Returns the value associated with the given key
     * 
     * @param key
     *            The key to use
     * @return The value associated with the given key, or null if no such value
     *         exists
     */
	public String get(String key) {
		return properties.get(key);
	}

	/**
	 * Returns a read-only view of this data source's properties.
	 */
	public Map<String,String> getPropertiesMap() {
		return Collections.unmodifiableMap(properties);
	}

	/**
	 * Compares all properties of this data source to those of the other.
	 * If there are any differences, returns false.  Otherwise, returns true.
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null)
            return false;
        if (!(o instanceof SPDataSource))
            return false;
		SPDataSource other = (SPDataSource) o;
		return this.properties.equals(other.properties);
	}

	/**
	 * Returns a hash that depends on all property values.
	 */
	@Override
	public int hashCode() {
		return properties.hashCode();
	}
	
    /**
     * Performs the comparison based on the name. The name is stored in
     * the {@link #PL_LOGICAL} in the properties map. This does not compare
     * all of the fields compared in {@link #equals(Object)}.
     *
     * @param o the SPDataSource object to compare with.
     * @return &lt;0 if this data source comes before o; 0 if they
     *  are equal; &gt;0 otherwise.
     * @throws NullPointerException if o==null
     * @throws ClassCastException if o is not an instance of SPDataSource
     */
	public final int compareTo(SPDataSource ds2) {
	    if (this == ds2) return 0;
        int tmp;
        String v1, v2;

        v1 = getName();
        v2 = ds2.getName();
        if (v1 == null && v2 != null) return -1;
        else if (v1 != null && v2 == null) return 1;
        else if (v1 != null && v2 != null) {
            tmp = v1.compareToIgnoreCase(v2);
        } else {
            tmp = 0;
        }
        
        return tmp;
	}
    
	// --------------------- property change ---------------------------

	/**
	 * Registers the given object as a listener to property changes on this
	 * SPDataSource.
	 */
	public void addPropertyChangeListener(PropertyChangeListener l) {
		getPcs().addPropertyChangeListener(l);
	}

	/**
	 * Removes the given object from the listener list, if it was on that list.
	 * Does nothing if l is not a property change listener of this data source.
	 */
	public void removePropertyChangeListener(PropertyChangeListener l) {
		getPcs().removePropertyChangeListener(l);
	}

	/**
	 * Returns an unmodifiable view of the list of property change listeners.
	 */
	public List<PropertyChangeListener> getPropertyChangeListeners() {
		return Collections.unmodifiableList(Arrays.asList(pcs.getPropertyChangeListeners()));
	}

    // ------------------- accessors and mutators for actual instance variables ------------------------

    /**
     * Copies all properties from the given data source into this one.
     * After this method returns, this data source will specify the same
     * target database as the given data source.
     * 
     * @param dbcs The connection spec to copy from (must not be null).
     */
    public void copyFrom(SPDataSource dbcs) {
        properties.clear();
        
        // This is extremely, stupidly cheap.  The tree doesn't notice the change unless there's
        // a property change event for the data source's name.
        setName(dbcs.getName());

        for (Map.Entry<String, String> entry : dbcs.getPropertiesMap().entrySet()) {
            // this is non-ideal, because the property change events will not have correct property names
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns the data source collection that this data source belongs to.
     */
    public DataSourceCollection<SPDataSource> getParentCollection() {
        return parentCollection;
    }
    
    /**
     * Gets the value of name
     *
     * @return the value of name
     */
    public String getName() {
        return get(PL_LOGICAL);
    }

    /**
     * Sets the value of name. 
     * @param argName Value to assign to this.name
     */
    public void setName(String argName){
        putImpl(PL_LOGICAL, argName, "name");
    }

    /**
     * Gets the value of displayName
     *
     * @return the value of displayName
     */
    public String getDisplayName() {
        return get(PL_LOGICAL);
    }

    /**
     * Sets the value of displayName
     *
     * @param argDisplayName Value to assign to this.displayName
     */
    public void setDisplayName(String argDisplayName){
        putImpl(PL_LOGICAL, argDisplayName, "name");
    }
    
}
