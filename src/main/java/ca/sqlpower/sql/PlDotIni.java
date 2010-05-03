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
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.sql;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.log4j.Logger;

import ca.sqlpower.sqlobject.SQLIndex;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider;
import ca.sqlpower.sqlobject.UserDefinedSQLType;
import ca.sqlpower.sqlobject.SQLTypePhysicalProperties.SQLTypeConstraint;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider.BasicSQLType;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider.PropertyType;

/**
 * The PlDotIni class represents the contents of a PL.INI file; despit
 * the name, this actually represents the master list of Data Source connections.
 * <p>
 * Note, there is some confusion about whether or not section name matching should
 * be case sensitive or not.  Both approaches are taken in this class!  Vive la difference.
 * <p>
 * <b>Warning:</b> this file only reads (and therefore writes) files with MS-DOS line endings,
 * regardless of platform, since the encoding of binary passwords
 * could result in a bare \n in the encryption...
 * @version $Id$
 */
public class PlDotIni implements DataSourceCollection<SPDataSource> {
	
	public class AddDSTypeUndoableEdit extends AbstractUndoableEdit {

		private final JDBCDataSourceType type;

		public AddDSTypeUndoableEdit(JDBCDataSourceType type) {
			this.type = type;
		}

		@Override
		public void redo() throws CannotRedoException {
			super.redo();
			fileSections.add(type);
		}

		@Override
		public void undo() throws CannotUndoException {
			super.undo();
			fileSections.remove(type);
		}
		
		public JDBCDataSourceType getType() {
			return type;
		}
	}

	public class RemoveDSTypeUndoableEdit extends AbstractUndoableEdit {

		private final JDBCDataSourceType type;

		public RemoveDSTypeUndoableEdit(JDBCDataSourceType type) {
			this.type = type;
		}

		@Override
		public void redo() throws CannotRedoException {
			super.redo();
			fileSections.remove(type);
		}

		@Override
		public void undo() throws CannotUndoException {
			super.undo();
			fileSections.add(type);
		}
		
		public JDBCDataSourceType getType() {
			return type;
		}
	}

    /**
     * Boolean to control whether we autosave, to prevent calling it while we're already saving.
     */
    private boolean dontAutoSave;

	/**
	 * The list of Listeners to notify when a datasource is added or removed.
	 */
	List<DatabaseListChangeListener> listeners;
	
	private final List<UndoableEditListener> undoListeners = new ArrayList<UndoableEditListener>();

	DatabaseListChangeListener saver = new DatabaseListChangeListener() {

	    public void databaseAdded(DatabaseListChangeEvent e) {
	        saveIfFileKnown();
	    }

	    public void databaseRemoved(DatabaseListChangeEvent e) {
            saveIfFileKnown();
	    }

	    private void saveIfFileKnown() {
            if (dontAutoSave)
                return;
	        if (lastFileAccessed != null) {
	            try {
	                write(lastFileAccessed);
	            } catch (IOException e) {
	                logger.error("Error auto-saving PL.INI file", e);
	            }
	        }

	    }
	};

	/**
	 * A URI that Mondrian XML schemas can be retrieved from.
	 */
	private final URI mondrianServerBaseURI;
	
    /**
     * Construct a PL.INI object, and set an Add Listener to save
     * the file when a database is added (bugid 1032).
     */
    public PlDotIni() {
        this(null);
    }
    
    /**
     * Constructs a data source collection with a URI that defines where to retrieve
     * JDBC drivers from the server.
     */
    public PlDotIni(URI serverBaseURI) {
    	this(serverBaseURI, null);
    }

	/**
	 * Constructs a data source collection with a URI that defines where to
	 * retrieve JDBC drivers from the server and another URI that defines where
	 * to retrieve Mondrian schemas from the server.
	 * 
	 * @param serverBaseURI
	 *            A URI that JDBC drivers can be retrieved from the server with.
	 * @param mondrianServerBaseURI
	 *            A URI that Mondrian XML schemas can be retrieved from the
	 *            server with.
	 */
    public PlDotIni(URI serverBaseURI, URI mondrianServerBaseURI) {
    	listeners = new ArrayList<DatabaseListChangeListener>();
        listeners.add(saver);
		this.serverBaseURI = serverBaseURI;
		this.mondrianServerBaseURI = mondrianServerBaseURI;
    }
    
	/**
     * The Section class represents a named section in the PL.INI file.
     * It has default visibility because the unit test needs to use it.
     */
    static class Section {

        /** The name of this section (without the square brackets). */
        private String name;

        /**
         * The contents of this section (part before the '=' is the key, and
         * the rest of the line is the value).
         */
        private Map properties;

        /** Creates a new section with the given name and no properties. */
        public Section(String name) {
            this.name = name;
            this.properties = new LinkedHashMap();
        }

        /**
         * Puts a new property in this section, or replaces an existing
         * property which has the same key. Keys are case-sensitive.
         *
         * @param key The property's key
         * @param value The property's value
         * @return The old value of the property under this key, if one existed.
         */
        public Object put(String key, String value) {
            return properties.put(key, value);
        }

        /** Returns the whole properties map.  This is required when saving the PL.INI file. */
        public Map getPropertiesMap() {
            return properties;
        }

        /** Returns the name of this section. */
        public String getName() {
            return name;
        }

        /**
         * Updates this section's contents to look like the given one.
         * Doesn't modify the name.
         */
        public void merge(Section s) {
            // get rid of deleted properties
            properties.keySet().retainAll(s.properties.keySet());
            properties.putAll(s.properties);
        }
    }

    private static final Logger logger = Logger.getLogger(PlDotIni.class);

    /**
     * A list of Section and SPDataSource objects, in the order they appear in the file;
     * this List contains Mixed Content (both Section and SPDataSource) which is
     * A Very Bad Idea(tm) so it cannot be converted to Java 5 Generic Collection.
     */
    private final List<Object> fileSections = new ArrayList<Object>();

    /**
     * The time we last read the PL.INI file.
     */
    private long fileTime;
    
    /**
     * Base URI for server: JAR spec lookups.
     */
    private URI serverBaseURI;
    
    boolean shuttingDown = false;
    
    /** Seconds to wait between checking the file. */
    int WAIT_TIME = 30;

    /**
     * Thread to stat file periodically, reload if PL changed it.
     * FIXME This thread is not currently started!
     */
    Thread monitor = new Thread() {
		public void run() {
			while (!shuttingDown) {
				try {
					Thread.sleep(WAIT_TIME * 1000);
					if (lastFileAccessed == null) {
						continue;
					}
					long newFileTime = lastFileAccessed.lastModified();
					if (newFileTime != fileTime) {
                        logger.debug("Re-reading PL.INI file because it has been modified externally.");
						read(lastFileAccessed);
						fileTime = newFileTime;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};

    File lastFileAccessed;

    /**
     * Returns the requested section in this pl.ini instance.  The sections
     * are stored in the same order they appear in the file, but of course
     * there may have been sections added or removed since the file was
     * last read.
     * <p>
     * Note, this method is really only intended for testing purposes.
     * 
     * @return The returned object will be of type Section, SPDataSource,
     * or SPDataSourceType depending on which type of file section it
     * represents.
     */
    Object getSection(int number) {
        return fileSections.get(number);
    }

    /**
     * Returns the number of sections that were in the pl.ini file we read
     * (additions and deletions after reading the file will affect the count
     * of course).  This will include unrecognized sections and the nameless
     * section at the top of the file, so don't expect the count to be equal
     * to the number of database types plus the number of connections.
     */
    int getSectionCount() {
        return fileSections.size();
    }
    
    /**
     * The enumeration of states that the read() method's INI file parser can be in.
     */
    private enum ReadState {READ_DS, READ_GENERIC, READ_TYPE, READ_SQLTYPE}

    public void read(File location) throws IOException {
        if (!location.canRead()) {
            throw new IOException("pl.ini file is not readable: " + location.getAbsolutePath());
        }
        fileTime =  location.lastModified();
        lastFileAccessed = location;
        read(new FileInputStream(location));
    }
    
    public void read(InputStream inStream) throws IOException {
        
    	if (inStream == null) throw new NullPointerException("InputStream was null!");
    	
        logger.info("Beginning to read/merge new pl.ini data");
        
	    ReadState mode = ReadState.READ_GENERIC;

        try {
            dontAutoSave = true;

            JDBCDataSourceType currentType = null;
            SPDataSource currentDS = null;
            UserDefinedSQLType currentSQLType = null;
            Section currentSection = new Section(null);  // this accounts for any properties before the first named section

            // Can't use Reader to read this file because the encrypted passwords contain non-ASCII characters
            BufferedInputStream in = new BufferedInputStream(inStream);

            byte[] lineBytes = null;

            while ((lineBytes = readLine(in)) != null) {
                String line = new String(lineBytes);
                
                line = convertOldLines(line);
                
                logger.debug("Read in new line: "+line);
                
                if (line.startsWith("[")) {
                    mergeFileData(mode, currentType, currentDS, currentSQLType, currentSection);
                }
                if (line.startsWith("[Databases_")) {
                    logger.debug("It's a new database connection spec!" +fileSections);
                    currentDS =  new JDBCDataSource(this);
                    mode = ReadState.READ_DS;
                } else if (line.startsWith("[OLAP_databases_")) {
                    logger.debug("It's a new database connection spec!" +fileSections);
                    currentDS =  new Olap4jDataSource(this);
                    mode = ReadState.READ_DS;
                } else if (line.startsWith("[Database Types_")) {
                    logger.debug("It's a new database type!" + fileSections);
                    currentType =  new JDBCDataSourceType(getServerBaseURI());
                    mode = ReadState.READ_TYPE;
                } else if (line.startsWith("[Data Types_")) {
                	logger.debug("It's a new data type!" + fileSections);
                	currentSQLType = new UserDefinedSQLType();
                	String platform = SQLTypePhysicalPropertiesProvider.GENERIC_PLATFORM; 
                	currentSQLType.setConstraintType(platform, SQLTypeConstraint.NONE);
                	currentSQLType.setEnumeration(platform, new String[0]);
                	currentSQLType.setCheckConstraint(platform, "");
                	currentSQLType.setDefaultValue(platform, "");
                	currentSQLType.setPrecision(platform, 0);
                	currentSQLType.setScale(platform, 0);
                	currentSQLType.setMyAutoIncrement(false);
                	currentSQLType.setMyNullability(Integer.valueOf(DatabaseMetaData.columnNoNulls));
                	mode = ReadState.READ_SQLTYPE;
                } else if (line.startsWith("[")) {
                    logger.debug("It's a new generic section!");
                    currentSection = new Section(line.substring(1, line.length()-1));
                    mode = ReadState.READ_GENERIC;
                } else {
                    String key;
                    String value;
                    int equalsIdx = line.indexOf('=');
                    if (equalsIdx > 0) {
                        key = line.substring(0, equalsIdx);
                        value = line.substring(equalsIdx+1, line.length());
                    } else {
                        key = line;
                        value = null;
                    }
                    logger.debug("key="+key+",val="+value);

                    if (mode == ReadState.READ_DS) {
                        // passwords are special, because the spectacular obfustaction technique
                        // can create bytes that are not in the range 32-127, which causes Java to
                        // map them to chars whose numeric value isn't the byte value!
                        // So, we have to read the "encrypted" password as an array of bytes.
                        if (key.equals("PWD") && value != null) {
                            byte[] cypherBytes = new byte[lineBytes.length - equalsIdx - 1];
                            System.arraycopy(lineBytes, equalsIdx + 1, cypherBytes, 0, cypherBytes.length);
                            value = decryptPassword(9, cypherBytes);
                        }
                        currentDS.put(key, value);
                    } else if (mode == ReadState.READ_TYPE) {
                        currentType.putProperty(key, value);
                    } else if (mode == ReadState.READ_GENERIC) {
                        currentSection.put(key, value);
                    } else if (mode == ReadState.READ_SQLTYPE) {
                    	putPropertyIntoSQLType(currentSQLType, key, value);
                    }
                }
            }
            in.close();
            mergeFileData(mode, currentType, currentDS, currentSQLType, currentSection);

            // hook up database type hierarchy, and assign parentType pointers to data sources themselves
            for (Object o : fileSections) {
                if (o instanceof JDBCDataSourceType) {
                    JDBCDataSourceType dst = (JDBCDataSourceType) o;
                    String parentTypeName = dst.getProperty(JDBCDataSourceType.PARENT_TYPE_NAME);
                    if (parentTypeName != null) {
                        JDBCDataSourceType parentType = getDataSourceType(parentTypeName);
                        if (parentType == null) {
                            throw new IllegalStateException(
                                    "Database type \""+dst.getName()+"\" refers to parent type \""+
                                    parentTypeName+"\", which doesn't exist");
                        }
                        dst.setParentType(parentType);
                    }
                } else if (o instanceof JDBCDataSource) {
                    JDBCDataSource ds = (JDBCDataSource) o;
                    String typeName = ds.getPropertiesMap().get(JDBCDataSource.DBCS_CONNECTION_TYPE);
                    if (typeName != null) {
                        JDBCDataSourceType type = getDataSourceType(typeName);
                        if (type == null) {
                            throw new IllegalStateException(
                                    "Database connection \""+ds.getName()+"\" refers to database type \""+
                                    typeName+"\", which doesn't exist");
                        }
                        logger.debug("The data source type \"" + type + "\" is being set as the parent type of" + ds);
                        ds.setParentType(type);
                    }
                    
                }
            }
        } finally {
            dontAutoSave = false;
        }

		logger.info("Finished reading file.");
	}

    /**
     * This method exists to convert older properties in a Pl.ini file to new properties.
     * This is for cases where a class was moved causing its full class name to be modified.
     * @param line
     * 			A line of text in the Pl.ini file that may need to be converted.
     * @return
     * 			The same line of text passed in with possible parts of it modified.
     */
    private String convertOldLines(String line) {
    	
    	if (line.contains("ca.sqlpower.architect.SQLIndex")) return line.replace("ca.sqlpower.architect.SQLIndex", SQLIndex.class.getName());

    	return line;
	}

	/**
     * A subroutine of the read() method. Merges data from any of the three types of
     * sections into the fileSections collection.
     * <p> 
     * A better approach than this would be to have SPDataSourceType, SPDataSource,
     * and Section all implement some interface, and then just have one merge() method.
     * 
     * @param mode File parsing mode. Determines which mergeXXX() method is called, and
     * which argument is passed in.
     * @param currentType Only used when mode = READ_TYPE
     * @param currentDS Only used when mode = READ_DS
	 * @param currentSQLType Only used when mode = READ_SQLTYPE 
     * @param currentSection Only used when mode = READ_GENERIC
     */
    private void mergeFileData(ReadState mode, JDBCDataSourceType currentType, SPDataSource currentDS, UserDefinedSQLType currentSQLType, Section currentSection) {
        if (mode == ReadState.READ_DS) {
            mergeDataSource(currentDS);
        } else if (mode == ReadState.READ_GENERIC) {
            mergeSection(currentSection);
        } else if (mode == ReadState.READ_SQLTYPE) {
        	mergeSQLType(currentSQLType);
        } else if (mode == ReadState.READ_TYPE) {
            // special case: sometimes the parser ends up thinking there was
            // an empty ds type section at the end of the file. we can't merge it.
            if (currentType.getProperties().size() > 0) {
                mergeDataSourceType(currentType);
            }
        } else {
            throw new IllegalArgumentException("Unknown read state. Can't merge");
        }
    }

	private void mergeSection(Section currentSection) {
        logger.debug("Attempting to merge Section: \"" + currentSection.getName() + "\"");
        for (Object o : fileSections) {
            if (o instanceof Section) {
                Section s = (Section) o;
                if ( (s.getName() == null && currentSection.getName() == null)
                    || (s.getName() != null && s.getName().equals(currentSection.getName())) ) {
                    logger.debug("Found a section to merge, now merging");
                    s.merge(currentSection);
                    return;
                }
            }
        }
        
        logger.debug("Didn't find section to merge. Adding...");
        fileSections.add(currentSection);
    }

    /**
	 * Reads bytes from the input stream until a CRLF pair or end-of-file is encountered.
	 * If a line is longer than some arbitrary maximum (currently 10000 bytes), it will
	 * be split into pieces not larger than that size and returned as separate lines.
	 * In this case, an error will be logged to the class's logger.
	 *
	 * <p>Note: We think that we require CRLF line ends because the encrypted password
	 * could contain a bare CR or LF, which we don't want to interpret as an end-of-line.
	 *
     * @param in The input stream to read.
     * @return All of the bytes read except the terminating CRLF.  If there are no more
     * bytes to read (because in is already at end-of-file) then this method returns null.
     */
    private byte[] readLine(BufferedInputStream in) throws IOException {
        final int MAX_LINE_LENGTH = 10000;
        byte[] buffer = new byte[MAX_LINE_LENGTH];
        int lineSize = 0;
        int ch;
        while ( (ch = in.read()) != -1 && lineSize < MAX_LINE_LENGTH) {
            buffer[lineSize] = (byte) ch;
            lineSize++;
            if (lineSize >= 2 && buffer[lineSize-2] == '\r' && buffer[lineSize-1] == '\n') {
                lineSize -= 2;
                break;
            }
        }

        // check for end of file
        if (ch == -1 && lineSize == 0) return null;

        // check for split lines
        if (lineSize == MAX_LINE_LENGTH) logger.error("Maximum line size exceeded while reading pl.ini.  Line will be split up.");

        byte chopBuffer[] = new byte[lineSize];
        System.arraycopy(buffer, 0, chopBuffer, 0, lineSize);
        return chopBuffer;
    }

    public void write() throws IOException {
        if (lastFileAccessed == null) {
            throw new IllegalStateException("Can't determine location for saving");
        }
        write(lastFileAccessed);
    }
    
    /* (non-Javadoc)
     * @see ca.sqlpower.architect.DataSourceCollection#write(java.io.File)
     */
	public void write(File location) throws IOException {
        logger.debug("Writing to "+location);
        try {
            dontAutoSave = true;
    	    OutputStream out = new BufferedOutputStream(new FileOutputStream(location));
    	    write(out);
    	    out.close();
            lastFileAccessed = location;
    	    fileTime = location.lastModified();
        } finally {
            dontAutoSave = false;
        }
	}

    /**
     * Writes the data source types and the data sources of this instance
     * in the world-famous PL.INI format.  Doesn't affect the lastFileLocation
     * or anything.
     */
	public void write(OutputStream out) throws IOException {
	    
        // counting starts at 1. Yay, VB!
        int dbNum = 1;
        int typeNum = 1;
        int olapNum = 1;

        Iterator it = fileSections.iterator();
	    while (it.hasNext()) {
	        Object next = it.next();

	        if (next instanceof Section) {
	            writeSection(out, ((Section) next).getName(), ((Section) next).getPropertiesMap());
            } else if (next instanceof JDBCDataSource) {
                writeSection(out, "Databases_"+dbNum, ((JDBCDataSource) next).getPropertiesMap());
                dbNum++;
            } else if (next instanceof JDBCDataSourceType) {
                writeSection(out, "Database Types_"+typeNum, ((JDBCDataSourceType) next).getProperties());
                typeNum++;
            } else if (next instanceof Olap4jDataSource) {
                writeSection(out, "OLAP_databases_"+olapNum, ((Olap4jDataSource) next).getPropertiesMap());
                olapNum++;
            } else if (next instanceof UserDefinedSQLType) {
            	logger.debug("Skipping sql types");
	        } else if (next == null) {
	            logger.error("write: Null section");
	        } else {
	            logger.error("write: Unknown section type: "+next.getClass().getName());
	        }
	    }
	}

	/**
	 * A helper method that given a {@link UserDefinedSQLType}, it returns an
	 * unmodifiable map of properties that are compatible with the expected key
	 * values for the Data Types section in the PL.INI
	 */
	// Left this package private so that the PLDotIniTest can use it
	Map<String,String> createSQLTypePropertiesMap(UserDefinedSQLType next) {
		String platform = SQLTypePhysicalPropertiesProvider.GENERIC_PLATFORM;
		Map<String, String> properties = new LinkedHashMap<String, String>();
		properties.put("Name", next.getName());
		properties.put("Basic Type", next.getBasicType().toString());
		properties.put("Description", next.getDescription());
		properties.put("JDBC Type", String.valueOf(next.getType()));
		return Collections.unmodifiableMap(properties);
	}

	/**
	 * A helper method that takes a value/key pair associate with a new Data
	 * Type and sets them on the given {@link UserDefinedSQLType}.
	 */
	private void putPropertyIntoSQLType(UserDefinedSQLType sqlType, String key, String value) {
		String platform = SQLTypePhysicalPropertiesProvider.GENERIC_PLATFORM;
		if (key.equals("Name")) {
			sqlType.setName(value);
			sqlType.setPhysicalTypeName(platform, value);
		} else if (key.equals("Basic Type")) {
			sqlType.setBasicType(BasicSQLType.valueOf(value));
		} else if (key.equals("Description")) {
			sqlType.setDescription(value);
		} else if (key.equals("JDBC Type")) {
			sqlType.setType(Integer.valueOf(value));
		} else if (key.equals("Precision Type")) {
			sqlType.setPrecisionType(platform, PropertyType.valueOf(value));
		} else if (key.equals("Scale Type")) {
			sqlType.setScaleType(platform, PropertyType.valueOf(value));
		} else if (key.equals("UUID")) {
			sqlType.setUUID(value);
		}
	}
	
	/**
	 * Writes out the named section header, followed by all the properties, one per line.  Each
	 * line is terminated with a CRLF, regardless of the current platform default.
	 *
	 * @param out The output stream to write to.
	 * @param name The name of the section.
	 * @param properties The properties to output in this section.
	 * @throws IOException when writing to the given stream fails.
	 */
	private void writeSection(OutputStream out, String name, Map properties) throws IOException {
	    if (name != null) {
	        String sectionHeading = "["+name+"]" + DOS_CR_LF;
	        out.write(sectionHeading.getBytes());
	    }

	    // output LOGICAL first (if it exists)
	    String s = null;
	    if ((s = (String) properties.get("Logical")) != null) {
	        out.write("Logical".getBytes());
            out.write("=".getBytes());
            out.write(s.getBytes());
	        out.write(DOS_CR_LF.getBytes());
	    }

	    // now get everything else, and ignore the LOGICAL property
	    Iterator it = properties.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry ent = (Map.Entry) it.next();
	        if (!ent.getKey().equals("Logical")) {
	        	out.write(((String) ent.getKey()).getBytes());
	        	if (ent.getValue() != null) {
	        		byte[] val;
	        		if (ent.getKey().equals("PWD")) {
	        			val = encryptPassword(9, ((String) ent.getValue()));
	        		} else {
	        			val = ((String) ent.getValue()).getBytes();
	        		}
	        		out.write("=".getBytes());
	        		out.write(val);
	        	}
	        	out.write(DOS_CR_LF.getBytes());
	        }
	    }
	}

    public SPDataSource getDataSource(String name) {
        return getDataSource(name, SPDataSource.class);
    }
    
    public <C extends SPDataSource> C getDataSource(String name, Class<C> classType) {
        Iterator it = fileSections.iterator();
        while (it.hasNext()) {
            Object next = it.next();
            if (classType.isInstance(next)) {
                C ds = classType.cast(next);
                if (logger.isDebugEnabled()) {
                    logger.debug("Checking if data source "+ds+" is PL Logical connection "+name);
                }
                if (ds.getName().equals(name)) return ds;
            }
        }
        return null;
    }

    public JDBCDataSourceType getDataSourceType(String name) {
        for (Object next : fileSections) {
            if (next instanceof JDBCDataSourceType) {
                JDBCDataSourceType dst = (JDBCDataSourceType) next;
                if (logger.isDebugEnabled()) {
                    logger.debug("Checking if data source type "+dst+" is called "+name);
                }
                if (dst.getName().equals(name)) return dst;
            }
        }
        return null;
    }

    public List<JDBCDataSourceType> getDataSourceTypes() {
        List<JDBCDataSourceType> list = new ArrayList<JDBCDataSourceType>();
        for (Object next : fileSections) {
            if (next instanceof JDBCDataSourceType) {
                JDBCDataSourceType dst = (JDBCDataSourceType) next;
                list.add(dst);
            }
        }
        return list;
    }

    /* Creates a list of data sources by iterating over all the sections and
     * picking the ones that are SPDataSource items.  Yes, this is not
     * optimal, but we can defer optimising it until someone proves it's an
     * actual performance issue.
     */
    public List<SPDataSource> getConnections() {
        return getConnections(SPDataSource.class);
    }
    
    /**
     * Gets a list of SPDataSource objects that are sorted. This will get only
     * {@link SPDataSource} objects of the specified type.
     * @param classType
     * @return
     */
    public <C extends SPDataSource> List<C> getConnections(Class<C> classType) {
        List<C> connections = new ArrayList<C>();
	    Iterator it = fileSections.iterator();
	    while (it.hasNext()) {
	        Object next = it.next();
	        if (classType.isInstance(next)) {
	            connections.add(classType.cast(next));
	        }
	    }
        Collections.sort(connections);
        return connections;
    }

    /* (non-Javadoc)
     * @see ca.sqlpower.architect.DataSourceCollection#toString()
     */
    public String toString() {
        OutputStream out = new ByteArrayOutputStream();
        try {
            write(out);
        } catch (IOException e) {
            return "PlDotIni: toString: Couldn't create string description: "+e.getMessage();
        }
        return out.toString();
    }

	/**
	 * Encrypts a PL.INI password.  The correct argument for
	 * <code>key</code> is 9.
	 */
    private byte[] encryptPassword(int key, String plaintext) {
        byte[] cyphertext = new byte[plaintext.length()];
        int temp;

        for (int i = 0; i < plaintext.length(); i++) {
            temp = plaintext.charAt(i);
            if (i % 2 == 1) {
                // if odd (even in VB's 1-based indexing)
                temp = temp - key;
            } else {
                temp = temp + key;
            }

            temp = temp ^ (10 - key);
            cyphertext[i] = ((byte) temp);
        }

        if (logger.isDebugEnabled()) {
            StringBuffer nums = new StringBuffer();
            for (int i = 0; i < cyphertext.length; i++) {
                nums.append((int) cyphertext[i]);
                nums.append(' ');
            }
            logger.debug("Encrypt: Plaintext: \""+plaintext+"\"; cyphertext=("+nums+")");
        }

        return cyphertext;
    }

	/**
	 * Decrypts a PL.INI password.  The correct argument for
	 * <code>number</code> is 9.
	 */
	public static String decryptPassword(int number, byte[] cyphertext) {
		StringBuffer plaintext = new StringBuffer(cyphertext.length);

		for (int i = 0, n = cyphertext.length; i < n; i++) {
			int temp = (( ((int) cyphertext[i]) & 0x00ff) ^ (10 - number));

			if (i % 2 == 1) {
				temp += number;
			} else {
				temp -= number;
			}
			plaintext.append((char) temp);
		}

		if (logger.isDebugEnabled()) {
            StringBuffer nums = new StringBuffer();
            for (int i = 0; i < cyphertext.length; i++) {
                nums.append((int) cyphertext[i]);
                nums.append(' ');
            }
            logger.debug("Decrypt: cyphertext=("+nums+"); Plaintext: \""+plaintext+"\"");
        }

        return plaintext.toString();
	}

    /* (non-Javadoc)
     * @see ca.sqlpower.architect.DataSourceCollection#addDataSource(ca.sqlpower.architect.SPDataSource)
     */
	public void addDataSource(SPDataSource dbcs) {
		String newName = dbcs.getDisplayName();
		for (Object o : fileSections) {
			if (o instanceof SPDataSource) {
				SPDataSource oneDbcs = (SPDataSource) o;
				if (newName.equalsIgnoreCase(oneDbcs.getDisplayName())) {
					throw new IllegalArgumentException(
							"There is already a datasource with the name " + newName);
				}
			}
		}
		addDataSourceImpl(dbcs);
    }

	/* (non-Javadoc)
     * @see ca.sqlpower.architect.DataSourceCollection#mergeDataSource(ca.sqlpower.architect.SPDataSource)
     */
    public void mergeDataSource(SPDataSource dbcs) {
    	String newName = dbcs.getDisplayName();
    	for (Object o : fileSections) {
    		if (o instanceof SPDataSource) {
				SPDataSource oneDbcs = (SPDataSource) o;
				if (newName.equalsIgnoreCase(oneDbcs.getDisplayName())) {
                    for (Map.Entry<String, String> ent : dbcs.getPropertiesMap().entrySet()) {
                        oneDbcs.put(ent.getKey(), ent.getValue());
                    }
				    return;
				}
			}
    	}
        
        // we only get here if we didn't find a data source to update.
        addDataSourceImpl(dbcs);
    }

    /* (non-Javadoc)
     * @see ca.sqlpower.architect.DataSourceCollection#removeDataSource(ca.sqlpower.architect.SPDataSource)
     */
    public void removeDataSource(SPDataSource dbcs) {
        
        // need to know the index we're removing in order to fire the remove event
        // (so using an indexed for loop, not a ListIterator)
        for ( int where=0; where<fileSections.size(); where++ ) {
            Object o  = fileSections.get(where);
            if (o instanceof SPDataSource) {
                SPDataSource current = (SPDataSource) o;
                if (current.getName().equals(dbcs.getName())) {
                    fileSections.remove(where);
                    fireRemoveEvent(where, dbcs);
                    return;
                }
            }
        }
        throw new IllegalArgumentException("dbcs not in list");
    }

    /**
     * Copies all the properties in the given dst into the existing DataSourceType section
     * with the same name, if one exists.  Otherwise, adds the given dst as a new section.
     */
    public void mergeDataSourceType(JDBCDataSourceType dst) {
        logger.debug("Merging data source type "+dst.getName());
        String newName = dst.getName();
        if (newName == null) {
            throw new IllegalArgumentException("Can't merge a nameless data source type: "+dst);
        }
        for (Object o : fileSections) {
            if (o instanceof JDBCDataSourceType) {
                JDBCDataSourceType current = (JDBCDataSourceType) o;
                if (newName.equalsIgnoreCase(current.getName())) {
                    logger.debug("    Found it");
                    for (Map.Entry<String, String> ent : dst.getProperties().entrySet()) {
                        current.putProperty(ent.getKey(), ent.getValue());
                    }
                    return;
                }
            }
        }
        
        logger.debug("    Not found.. adding");
        addDataSourceType(dst);

    }

	/**
	 * Common code for add and merge.  Adds the given dbcs as a section, then fires an add event.
	 * @param dbcs
	 */
	private void addDataSourceImpl(SPDataSource dbcs) {
		fileSections.add(dbcs);
		fireAddEvent(dbcs);
	}

    public void addDataSourceType(JDBCDataSourceType dataSourceType) {
        // TODO fire an event for adding the dstype
        fileSections.add(dataSourceType);
        for (int i = undoListeners.size() - 1; i >= 0; i--) {
        	undoListeners.get(i).undoableEditHappened(new UndoableEditEvent(this, new AddDSTypeUndoableEdit(dataSourceType)));
        }
    }

    public boolean removeDataSourceType(JDBCDataSourceType dataSourceType) {
        // TODO fire an event for removing the dstype
    	for (int i = undoListeners.size() - 1; i >= 0; i--) {
    		undoListeners.get(i).undoableEditHappened(new UndoableEditEvent(this, new RemoveDSTypeUndoableEdit(dataSourceType)));
    	}
        return fileSections.remove(dataSourceType);
    }

	/**
	 * If a section with the same name as the given sqlType exists, then merge
	 * the properties. Otherwise, add a new section for this type.
	 */
	public void mergeSQLType(UserDefinedSQLType sqlType) {
		String name = sqlType.getName();
		for (Object o : fileSections) {
			if (o instanceof UserDefinedSQLType) {
				UserDefinedSQLType existingType = (UserDefinedSQLType) o;
				if (existingType.getName().equals(name)) {
					for (Map.Entry<String, String> entry : createSQLTypePropertiesMap(sqlType).entrySet()) {
						putPropertyIntoSQLType(existingType, entry.getKey(), entry.getValue());
					}
					return;
				}
			}
		}
		addSQLType(sqlType);
	}
    
	private void addSQLType(UserDefinedSQLType sqlType) {
		// TODO: If this method is made public for client code to add new types,
		// there will probably have to be events fired for UI
		fileSections.add(sqlType);
    }
    
    private void fireAddEvent(SPDataSource dbcs) {
		int index = fileSections.size()-1;
		DatabaseListChangeEvent e = new DatabaseListChangeEvent(this, index, dbcs);
    	synchronized(listeners) {
			for(DatabaseListChangeListener listener : listeners) {
				listener.databaseAdded(e);
			}
		}
	}

    private void fireRemoveEvent(int i, SPDataSource dbcs) {
    	DatabaseListChangeEvent e = new DatabaseListChangeEvent(this, i, dbcs);
    	synchronized(listeners) {
			for(DatabaseListChangeListener listener : listeners) {
				listener.databaseRemoved(e);
			}
		}
    }

    /* (non-Javadoc)
     * @see ca.sqlpower.architect.DataSourceCollection#addDatabaseListChangeListener(ca.sqlpower.architect.DatabaseListChangeListener)
     */
    public void addDatabaseListChangeListener(DatabaseListChangeListener l) {
    	synchronized(listeners) {
    		listeners.add(l);
    	}
    }

    /* (non-Javadoc)
     * @see ca.sqlpower.architect.DataSourceCollection#removeDatabaseListChangeListener(ca.sqlpower.architect.DatabaseListChangeListener)
     */
    public void removeDatabaseListChangeListener(DatabaseListChangeListener l) {
    	synchronized(listeners) {
    		listeners.remove(l);
    	}
    }

	public void addUndoableEditListener(UndoableEditListener l) {
		undoListeners.add(l);
	}

	public void removeUndoableEditListener(UndoableEditListener l) {
		undoListeners.remove(l);
	}

	public URI getServerBaseURI() {
	    return serverBaseURI;
	}

	public URI getMondrianServerBaseURI() {
		return mondrianServerBaseURI;
	}

	public UserDefinedSQLType getSQLType(String name) {
		for (Object o : fileSections) {
			if (o instanceof UserDefinedSQLType) {
				if (((UserDefinedSQLType) o).getName().equals(name)) {
					return (UserDefinedSQLType) o;
				}
			}
		}
		return null;
	}

	public List<UserDefinedSQLType> getSQLTypes() {
		List<UserDefinedSQLType> list = new ArrayList<UserDefinedSQLType>();
		for (Object o : fileSections) {
			if (o instanceof UserDefinedSQLType) {
				list.add((UserDefinedSQLType) o);
			}
		}
		return list;
	}
}
