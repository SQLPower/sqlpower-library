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
package ca.sqlpower.sql;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import org.apache.log4j.Logger;

/**
 * Defines a type of data source.  We wanted to call this SPDataSourceClass,
 * but that would be confusing in that we mean class as in class, type, genre, sort,
 * variety, breed (ahem, that's enough, Mr. Data).
 * <p>
 * Data Source types can have supertypes, from which they inherit any undefined property
 * values.
 */
public class JDBCDataSourceType {
    
    static final Logger logger = Logger.getLogger(JDBCDataSourceType.class);

    /**
     * Map of classpaths to classloaders. This facilitates class sharing between
     * incarnations of the same database type (as long as it still has the same
     * set of JAR files as its previous incarnations).
     */
    private static final Map<List<String>, JDBCClassLoader> jdbcClassloaders = new HashMap<List<String>, JDBCClassLoader>();
    
    /**
     * For debugging only, we count how many times we've attempted to load
     * each class by name.  This tool will hopefully help us track down cases
     * when we are loading the same drivers for multiple connections that should
     * have been sharing the same driver classes.
     */
    private static Map<String, Integer> classLoadCounts = new HashMap<String, Integer>();
    
    private class UndoablePropertyEdit extends AbstractUndoableEdit {
    	
    	private final String changedProperty;
    	private final String oldValue;
    	private final String newValue;
    	private final JDBCDataSourceType source;
    	
    	public UndoablePropertyEdit(String propertyName, String oldValue, String newValue, JDBCDataSourceType source) {
			changedProperty = propertyName;
			this.oldValue = oldValue;
			this.newValue = newValue;
			this.source = source;
    	}
    	
    	@Override
    	public void redo() throws CannotRedoException {
    		super.redo();
    		source.properties.put(changedProperty, newValue);
    		source.classLoader = getClassLoaderFromCache();
    	}
    	
    	@Override
    	public void undo() throws CannotUndoException {
    		super.undo();
    		source.properties.put(changedProperty, oldValue);
            source.classLoader = getClassLoaderFromCache();
    	}
    }
    
    /**
     * A special ClassLoader that searches the classpath associated with this
     * type of data source only. Each SPDataSourceType should have
     * one of these class loaders, configured to search the database vendor's
     * jar/zip files.
     */
    static class JDBCClassLoader extends ClassLoader {

        /**
         * The base URI against which to resolve server: type JAR specs. This
         * can legally be null, which will simply cause attempts at reading
         * server: JAR files to fail. If there are no server: JAR specs, this
         * null value won't cause any trouble.
         */
        private final URI serverBaseUri;

        /**
         * The classpath of this loader at the time it was created.
         */
        private final List<String> classpath;

        /**
         * Don't call this method directly. Use the
         * {@link JDBCDataSourceType#getClassLoaderFromCache()} method instead.
         * 
         * @param serverBaseUri Base URI to resolve classpath entries against.
         * @param classpath The JAR specifications to consult.
         */
        protected JDBCClassLoader(URI serverBaseUri, List<String> classpath) {
            super(JDBCClassLoader.class.getClassLoader());
            this.serverBaseUri = serverBaseUri;
            this.classpath = classpath;

        	// sanity check
        	for (String jarPath : classpath) {
        		if (jarPath.startsWith(SPDataSource.SERVER) && serverBaseUri == null) {
        			throw new IllegalStateException(
        					"Found a server-based JAR file \"" + jarPath + "\" on the" +
        					" classpath but there is no server base URI configured");
        		}
        	}
        	
            logger.debug("Created new JDBC Classloader @"+System.identityHashCode(this));
            
            // classes loaded with this classloader need their own security policy,
            // because in WebStart, the allPermissions tag applies only to the
            // webstart classloader.
            // I found this code in a comment on the big ranch java saloon. It works!
            Policy.setPolicy( new Policy() {
                    public PermissionCollection
                        getPermissions(CodeSource codesource) {
                        Permissions perms = new Permissions();
                        perms.add(new AllPermission());
                        return(perms);
                    }
                    public void refresh(){
                        // no need to refresh
                    }
                });
        }
        
        /**
         * Searches the jar files listed by getJdbcJarList() for the
         * named class.  Throws ClassNotFoundException if the class can't
         * be located.
         */
        @Override
        public Class<?> findClass(String name)
            throws ClassNotFoundException {
            
            if (logger.isDebugEnabled()) {
                Integer count = classLoadCounts.get(name);
                if (count == null) {
                    count = new Integer(1);
                } else {
                    count += 1;
                }
                classLoadCounts.put(name, count);
                logger.debug("JDBC Classloader @"+System.identityHashCode(this)+
                        ": Looking for class "+name+" (count = "+count+")");
            }

            for (String jarFileName : classpath) {
                try {
                    logger.debug("checking file "+jarFileName);
                    URL jarLocation = JDBCDataSource.jarSpecToFile(jarFileName, getParent(), serverBaseUri);
                    if (jarLocation == null) {
                    	// missing JAR file in classpath. just skip it.
                    	continue;
                    }
                    
                    String jarEntryPath = name.replace('.','/') + ".class";
                    URL url = new URL("jar:" + jarLocation.toString() + "!/" + jarEntryPath);
                    JarURLConnection jarConnection;
                    try {
                        jarConnection = (JarURLConnection) url.openConnection();
                    } catch (IOException ex) {
                        // this was the old behaviour if the file didn't exist. still a good idea?
                        logger.debug("Skipping non-existant JAR file "+jarFileName);
                        continue;
                    }
                    
                    JarEntry ent = jarConnection.getJarEntry();
                    byte[] buf = new byte[(int) ent.getSize()];
                    InputStream is = jarConnection.getInputStream();
                    int offs = 0, n = 0;
                    while ( (n = is.read(buf, offs, buf.length-offs)) >= 0 && offs < buf.length) {
                        offs += n;
                    }
                    final int total = offs;
                    if (total != ent.getSize()) {
                        logger.warn("What gives?  ZipEntry "+ent.getName()+" is "+ent.getSize()+" bytes long, but we only read "+total+" bytes!");
                    }
                    return defineClass(name, buf, 0, buf.length);
                } catch (IOException ex) {
                    // there might be more classpath entries to search
                    continue;
                }
            }
            String errorMsg =
                "Could not locate class " + name +
			    " in any of the JDBC Driver JAR files: " + classpath;
            logger.debug(errorMsg);
			throw new ClassNotFoundException(errorMsg);
        }

        /**
         * Returns the first result that would be obtained from {@link #findResources(String)},
         * or null if findResources would return an empty result.
         */
        @Override
        protected URL findResource(String name) {
            Enumeration<URL> resources = findResources(name);
            if (resources.hasMoreElements()) {
                return resources.nextElement();
            } else {
                return null;
            }
        }
        
        /**
         * Creates URL objects for each instance of the named file within the
         * same set of JAR files that classes are loaded from.
         */
        @Override
        protected Enumeration<URL> findResources(String name) {
            logger.debug("Looking for all resources with path "+name);
            List<URL> results = new ArrayList<URL>();
            for (String jarName : classpath) {
                logger.debug("Converting JAR name: " + jarName);
                URL jarLocation = JDBCDataSource.jarSpecToFile(jarName, getParent(), serverBaseUri);
                logger.debug("  JAR is "+jarLocation);
                try {
                    if (jarLocation == null) {
                        logger.debug("  Skipping non-existant JAR file " + jarName);
                        continue;
                    } else {
                        logger.debug("  Searching JAR " + jarLocation);
                    }
                    
                    URL url = new URL("jar:" + jarLocation.toString() + "!/" + name);

                    JarURLConnection jarConnection;
                    try {
                        jarConnection = (JarURLConnection) url.openConnection();
                    } catch (IOException ex) {
                        // this was the old behaviour if the file didn't exist. still a good idea?
                        logger.debug("Skipping non-existant JAR file " + jarLocation);
                        continue;
                    }
                    
                    JarEntry ent = jarConnection.getJarEntry();
                    if (ent != null) {
                        results.add(url);
                        logger.debug("    Found entry " + name);
                    }
                } catch (IOException ex) {
                    // missing resource is not a fatal error
                    logger.debug("  IO Exception while searching "+ jarLocation
                                + " for resource " + name + ". Continuing...", ex);
                }
            }
            return Collections.enumeration(results);
        }
    }
    
    public static final String JDBC_DRIVER = "JDBC Driver Class";
    public static final String JDBC_URL = "JDBC URL";
    public static final String JDBC_JAR_BASE = "JDBC JAR";
    public static final String JDBC_JAR_COUNT = "JDBC JAR Count";
    public static final String TYPE_NAME = "Name";
    public static final String PARENT_TYPE_NAME = "Parent Type";
    public static final String PL_DB_TYPE = "PL Type";
    public static final String COMMENT = "Comment";
    public static final String DDL_GENERATOR = "DDL Generator";
    public static final String KETTLE_DB_TYPES = "ca.sqlpower.architect.etl.kettle.connectionType";
    public static final String SUPPORTS_UPDATEABLE_RESULT_SETS = "Supports Updatable Result Sets";
    public static final String SUPPORTS_STREAM_QUERIES = "Supports Stream Queries";
    
    /**
     * This type's parent type.  This value will be null if this type has no
     * parent.
     */
    private JDBCDataSourceType parentType;
    
    /**
     * This type's properties.  There are a set of property
     * names that we know what they mean, but instances of this class carry
     * all the property name=value pairs that get thrown at them so that
     * we don't end up deleting new properties when reading then saving over
     * a file with an older version of the app.
     */
    private Map<String,String> properties = new LinkedHashMap<String, String>();
    
    /**
     * The Class Loader that is responsible for finding and defining the JDBC
     * driver classes from the database vendor, for this connection type only.
     */
    private JDBCClassLoader classLoader;
    
    /**
     * Deletgate class for supporting the bound properties of this class.
     */
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * Listeners listening for undoable edits.
     */
	private final List<UndoableEditListener> undoableEditListeners = new ArrayList<UndoableEditListener>();

    private final URI serverBaseUri;

    /**
     * Creates a new default data source type that can load drivers from a local
     * file or a JAR resource on the classpath, but not from a server.
     * 
     * @see #SPDataSourceType(URI)
     */
    public JDBCDataSourceType() {
        this(null);
    }

    /**
     * Creates a new default data source type that can load drivers from a
     * server, a local file, or JAR resources on the classpath.
     */
    public JDBCDataSourceType(URI serverBaseUri) {
        super();
        this.serverBaseUri = serverBaseUri;
        classLoader = getClassLoaderFromCache();
    }

    /**
     * Returns the cached classloader that has the same set of jar files in its
     * classpath as this data source type currently does. If no such classloader
     * is already cached, a new one will be created and stored in the cache.
     */
    private JDBCClassLoader getClassLoaderFromCache() {
        List<String> classpath = Collections.unmodifiableList(new ArrayList<String>(getJdbcJarList()));
        JDBCClassLoader classLoader = jdbcClassloaders.get(classpath);
        if (classLoader == null) {
            classLoader = new JDBCClassLoader(getServerBaseUri(), classpath);
            jdbcClassloaders.put(classpath, classLoader);
        }
        return classLoader;
    }
    
    public String getComment() {
        return getProperty(COMMENT);
    }
    
    public void setComment(String comment) {
        putPropertyImpl("comment", COMMENT, comment);
    }
    
    public String getPlDbType() {
        return getProperty(PL_DB_TYPE);
    }
    
    public void setPlDbType(String type) {
        putPropertyImpl("plDbType", PL_DB_TYPE, type);
    }
    
    public String getJdbcDriver() {
        return getProperty(JDBC_DRIVER);
    }
    
    public void setJdbcDriver(String jdbcDriver) {
        putPropertyImpl("jdbcDriver", JDBC_DRIVER, jdbcDriver);
    }
    
    /**
     * Returns an unmodifiable list of all the JAR file paths
     * associated with this data source type.  This list is not
     * guaranteed to stay up-to-date with additional jars that get
     * added to this ds type after you call this method.
     * <p>
     * @return An unmodifiable list of jar file pathnames.
     */
    public List<String> getJdbcJarList() {
        return Collections.unmodifiableList(makeModifiableJdbcJarList());
    }
    
    /**
     * Creates a list of the Jar files tracked by this data source type.  Although
     * the list is modifiable, it is your own independant copy of the jar list, and 
     * modifying it will have no effect on the actual list of jars tracked by this
     * instance.
     */
    private List<String> makeModifiableJdbcJarList() {
        int count = getJdbcJarCount();
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            String key = JDBC_JAR_BASE+"_"+i;
            String value = getProperty(key);
            if (value != null) {
                logger.debug("Found jar \""+value+"\" under key \""+key+"\"");
                list.add(value);
            } else {
                logger.debug("Skipping null jar entry under key \""+key+"\"");
            }
        }
        return list;
    }

    /**
     * Replaces the current list of JDBC driver jar files with a copy of the given list.
     * <p>
     * Warning: this method does not presently cause any events to be fired, although
     * a future revision hopefully will.
     */
    public void setJdbcJarList(List<String> jdbcJarList) {
    	clearJdbcJarList();
        int count = jdbcJarList.size();
        setJdbcJarCount(count);
        int i = 0;
        for (String jar : jdbcJarList) {
        	logger.debug("Setting jar list value " + i + " to path " + jar);
            properties.put(JDBC_JAR_BASE+"_"+i, jar);
            i++;
        }
        classLoader = getClassLoaderFromCache();
    }
    
    private void clearJdbcJarList() {
    	Set<String> keys = properties.keySet();
    	Iterator<String> it = keys.iterator();
    	while (it.hasNext()) {
    		String key = it.next();
    		if (key.startsWith(JDBC_JAR_BASE)) {
    			it.remove();
    		}
    	}
    }   
    
    /**
     * Adds the JDBC driver jar path name to the internal list.
     * <p>
     * Warning: this method does not presently cause any events to be fired, although
     * a future revision hopefully will.
     */
    public void addJdbcJar(String jarPath) {
    	logger.debug("Adding jar at path " + jarPath);
        int count = getJdbcJarCount();
        properties.put(JDBC_JAR_BASE+"_"+count, jarPath);
        setJdbcJarCount(count + 1);
        classLoader = getClassLoaderFromCache();
    }

    private void setJdbcJarCount(int count) {
        putPropertyImpl("jdbcJarCount", JDBC_JAR_COUNT, String.valueOf(count));
        classLoader = getClassLoaderFromCache();
    }

    private int getJdbcJarCount() {
        String jarCountString = getProperty(JDBC_JAR_COUNT);
        if (jarCountString == null) {
            return 0;
        } else {
            return Integer.parseInt(jarCountString);
        }
    }
    
    public String getJdbcUrl() {
        return getProperty(JDBC_URL);
    }
    
    public void setJdbcUrl(String jdbcUrl) {
        putPropertyImpl("jdbcUrl", JDBC_URL, jdbcUrl);
    }
    
    /**
     * For each property in the template, if the property has a default value
     * its property name and the default value will be put into the map otherwise
     * the property name and an empty string will be stored.
     */
    public Map<String, String> retrieveURLDefaults() {
        String template = getProperty(JDBC_URL);
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (template == null) return map;
        
        int searchFrom = 0;
        while (template.indexOf('<', searchFrom) >= 0) {
            int openBrace = template.indexOf('<', searchFrom);
            searchFrom = openBrace + 1;
            int colon = template.indexOf(':', searchFrom);
            int closeBrace = template.indexOf('>', searchFrom);
            if (closeBrace == -1) break;
            if (colon >= 0 && colon < closeBrace) {
                map.put(template.substring(openBrace+1, colon), template.substring(colon+1, closeBrace));
            } else if (closeBrace >=0) {
                map.put(template.substring(openBrace+1, closeBrace), "");
            }
            searchFrom = closeBrace++;
        }
        return map;
    }
    
    /**
     * This method takes a url and matches it to the template pattern that is stored.
     * The returned map contains a key, value pair for each property in the template 
     * and the value itself.
     */
    public Map<String, String> retrieveURLParsing(String url) {
        String template = getProperty(JDBC_URL);
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (template == null) return map;
        String reTemplate = template.replaceAll("<.*?>", "(.*)");
        logger.debug("Regex of template is "+reTemplate);
        Pattern p = Pattern.compile(reTemplate);
        Matcher m = p.matcher(url);
        if (m.find()) {
            int searchFrom = 0;
            for (int g = 1; g <= m.groupCount(); g++) {
                int openBrace = template.indexOf('<', searchFrom);
                searchFrom = openBrace + 1;
                int colon = template.indexOf(':', searchFrom);
                int closeBrace = template.indexOf('>', searchFrom);
                if (colon >= 0 && colon < closeBrace) {
                    map.put(template.substring(openBrace+1, colon), m.group(g));
                } else if (closeBrace >=0) {
                    map.put(template.substring(openBrace+1, closeBrace), m.group(g));
                }
                searchFrom = closeBrace++;
            }
        }
        
        logger.debug("The map! dun dun dun: " + map.toString());

        return map;
    }
    
    public String getName() {
        return getProperty(TYPE_NAME);
    }
    
    public void setName(String name) {
        putPropertyImpl("name", TYPE_NAME, name);
    }

    public String getDDLGeneratorClass() {
        return getProperty(DDL_GENERATOR);
    }
    
    public void setDDLGeneratorClass(String className) {
        putPropertyImpl("DDLGeneratorClass", DDL_GENERATOR, className);
    }

    public JDBCDataSourceType getParentType() {
        return parentType;
    }
    
    public void setParentType(JDBCDataSourceType parentType) {
        this.parentType = parentType;
    }
    
    /**
     * Returns true if and only if the platform supports updateable result sets.
     */
    public boolean getSupportsUpdateableResultSets() {
        String ret = getProperty(SUPPORTS_UPDATEABLE_RESULT_SETS);
        if (ret == null || !Boolean.parseBoolean(ret)) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if and only if the platform supports the SELECT STREAM syntax.
     */
    public boolean getSupportsStreamQueries() {
        String ret = getProperty(SUPPORTS_STREAM_QUERIES);
        if (ret == null || !Boolean.parseBoolean(ret)) {
            return false;
        }
        return true;
    }

    /**
     * Returns all the properties of this data source type.  This will not
     * include any inherited values, so unless you're trying to save this data source
     * to a file or something, you'd probably prefer to use one of the getter methods.
     * Also, you really, really shouldn't modify the map you get back.
     * 
     * @throws UnsupportedOperationException if you ignored our nice warning and tried
     * to modify the returned map.
     */
    Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }
    
    /**
     * Returns a set of the properties defined for this data source type.
     * The set returned is unmodifiable and will throw an exception if any modification
     * is made to it.
     */
    public Set<String> getPropertyNames() {
    	return Collections.unmodifiableSet(properties.keySet());
    }

    /**
     * Adds or replaces a value in the property map. Fires an undoable edit
     * event, but not a property change event. Use
     * {@link #putPropertyImpl(String, String, String)} if the change should
     * produce a property change event.
     */
    public void putProperty(String key, String value) {
    	putPropertyImpl(null, key, value);
    }

    public ClassLoader getJdbcClassLoader() {
        return classLoader;
    }
    
    /**
     * Returns a list of the kettle database type names, empty 
     * list if the database types property is null.
     */
    public List<String> getKettleNames() {
    	List<String> ret = new LinkedList<String>();
    	String dbTypes = getProperty(KETTLE_DB_TYPES);
    	if (dbTypes == null) return Collections.emptyList();
    	Scanner s = new Scanner(dbTypes);
    	s.useDelimiter(":");
    	while (s.hasNext()) {
    		ret.add(s.next());
    	}
    	return ret;
    }
    
    /**
     * Gets a property from this data source type, checking with the supertype
     * when this type doesn't have a value for the requested property.
     * 
     * @param key The property name to look up. Null isn't allowed.
     */
    public String getProperty(String key) {
        String value = properties.get(key);
        if (value != null) {
            return value;
        } else if (parentType != null) {
            return parentType.getProperty(key);
        } else {
            return null;
        }
    }

    /**
     * Removes the jar file's path from the list of jar files.  Has no effect
     * if the named JAR file is not in the list.
     * 
     * @param path the path you want to remove
     */
    public void removeJdbcJar(String path) {
        List<String> jdbcJars = makeModifiableJdbcJarList();
        logger.debug("Removing jdbc jar " + path + " at index " + jdbcJars.indexOf(path));
        if (jdbcJars.contains(path)) {
        	jdbcJars.remove(path);
        } else {
        	File file = new File(path);
        	jdbcJars.remove(SPDataSource.BUILTIN + file.getName());
        }
        setJdbcJarList(jdbcJars);
    }
    
    @Override
    public String toString() {
        return "DataSourceType: "+properties;
    }

    /**
     * Checks that all prerequisites for making a connection to this type of database
     * have been met. Currently, this only checks that the driver class field is filled
     * in.
     * 
     * @throws SQLException if there are unmet prerequisites.  The exception message will
     * explain which prerequisite is not met.
     */
    public void checkConnectPrereqs() throws SQLException {
        if (getJdbcDriver() == null
                || getJdbcDriver().trim().length() == 0) {
            throw new SQLException("Data Source Type \""+getName()+"\" has no JDBC Driver class specified.");
        }
    }

    /**
     * Modifies the value of the named property, firing a change event if the
     * new value differs from the pre-existing one.
     * 
     * @param javaPropName
     *            The name of the JavaBeans property you are modifying. If the
     *            change does not correspond with a bean property, or you want
     *            to suppress the property change event, this parameter should
     *            be null.
     * @param plPropName
     *            The name of PL.INI the property to set/update (this is also
     *            the key in the in-memory properties map)
     * @param propValue
     *            The new value for the property
     */
    private void putPropertyImpl(String javaPropName, String plPropName, String propValue) {
        String oldValue = properties.get(plPropName);
        properties.put(plPropName, propValue);
        classLoader = getClassLoaderFromCache(); // in case this changes the classpath
        
        if (javaPropName != null) {
            firePropertyChange(javaPropName, oldValue, propValue);
        }
        
        if ((oldValue == null && propValue != null) || (oldValue != null && !oldValue.equals(propValue))) {
        	UndoableEdit edit = new UndoablePropertyEdit(plPropName, oldValue, propValue, this);
        	for (int i = undoableEditListeners.size() -1; i >= 0; i--) {
        		undoableEditListeners.get(i).undoableEditHappened(new UndoableEditEvent(this, edit));
        	}
        }
    }
    
    public void addUndoableEditListener(UndoableEditListener l) {
    	undoableEditListeners.add(l);
    }
    
    public void removeUndoableEditListener(UndoableEditListener l) {
    	undoableEditListeners.remove(l);
    }
    
    // ---------------- Methods that delegate to the PropertyChangeSupport -----------------
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void firePropertyChange(PropertyChangeEvent evt) {
        pcs.firePropertyChange(evt);
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return pcs.getPropertyChangeListeners();
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return pcs.getPropertyChangeListeners(propertyName);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

	public URI getServerBaseUri() {
		return serverBaseUri;
	}
}
