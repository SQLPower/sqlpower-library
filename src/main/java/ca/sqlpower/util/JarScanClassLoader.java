/*
 * Copyright (c) 2015, SQL Power Group Inc.
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

package ca.sqlpower.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Scans a jar file for instances based on the implemented {@link #checkClass(Class)} method.
 */
public abstract class JarScanClassLoader extends ClassLoader {
	
	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(JarScanClassLoader.class);

	private List<String> drivers;
	private int count = 0;
    private JarURLConnection jarConnection;
    private JarFile jf;

    /**
	 * Creates a class loader that can scan the given JAR for classes matching
	 * the {@link #checkClass(Class)} implementation. Uses this class's class
	 * loader as its parent.
	 * 
	 * @param jarLocation
	 *            The JAR to scan. This URL must <i>not</i> be a jar: URL; it
	 *            will be converted to one within this constructor.
	 * @throws IOException
	 */
	public JarScanClassLoader(URL jarLocation) throws IOException {
		super();
		URL jarURL = new URL("jar:" + jarLocation + "!/");
        jarConnection = (JarURLConnection) jarURL.openConnection();
        jf = jarConnection.getJarFile();
	}

	public synchronized double getFraction() {
		double retval = 0.0;
		if (jf != null) {
			retval = (double)count/(double)jf.size();
		}
		return retval;
	}

	/**
	 * Returns a list of Strings naming the subclasses of
	 * java.sql.Driver which exist in this class loader's jar
	 * file.
	 */
	public List<String> scanForDrivers() {
		drivers = new LinkedList<String>();
		logger.debug("********* " + jf.getName() + " has " + jf.size() + " files."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		for (Enumeration<JarEntry> entries = jf.entries(); entries.hasMoreElements(); ) {
			count++;
			ZipEntry ent = (ZipEntry) entries.nextElement();
			if (ent.getName().endsWith(".class")) { //$NON-NLS-1$
				try {
					// drop the .class from the name
					String [] s = ent.getName().split("\\."); //$NON-NLS-1$
					// look for the class using dots instead of slashes
					findClass(s[0].replace('/','.'));
				} catch (ClassFormatError ex) {
					logger.warn("JAR entry "+ent.getName()+" ends in .class but is not a class", ex); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (NoClassDefFoundError ex) {
					logger.warn("JAR does not contain dependency needed by: " + ent.getName()); //$NON-NLS-1$
				} catch (Throwable ex) {
					logger.warn("Unexpected exception while scanning JAR file "+jf.getName(), ex); //$NON-NLS-1$
				}
			}
		}
		//jf.close();
		return drivers;
	}

	/**
	 * Searches this ClassLoader's jar file for the given class.
	 *
	 * @throws ClassNotFoundException if the class can't be
	 * located.
	 */
	protected Class<?> findClass(String name)
		throws ClassNotFoundException {
		//logger.debug("Looking for class "+name);
		try {
			ZipEntry ent = jf.getEntry(name.replace('.', '/')+".class"); //$NON-NLS-1$
			if (ent == null) {
				throw new ClassNotFoundException("No class file "+name+" is in my jar file"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// can we find out here if it was already loaded???
			Class<?> clazz = findLoadedClass(name);
			if (clazz != null) {
				return clazz;
			}
			// haven't seen this before, so go get it...
			InputStream is = jf.getInputStream(ent);
			return readAndCheckClass(is, (int) ent.getSize(), name);
		} catch (IOException ex) {
			throw new ClassNotFoundException("IO Exception reading class from jar file", ex); //$NON-NLS-1$
		}
	}

	private Class<?> readAndCheckClass(InputStream is, int size, String expectedName)
		throws IOException, ClassFormatError {
		byte[] buf = new byte[size];
		int offs = 0, n;
        
		while ( (n = is.read(buf, offs, size-offs)) >= 0 && offs < size) {
			offs += n;
		}
        final int total = offs;
		if (total != size) {
			logger.warn("Only read "+total+" bytes of class " //$NON-NLS-1$ //$NON-NLS-2$
						+expectedName+" from JAR file; exptected "+size); //$NON-NLS-1$
		}
		Class<?> clazz = defineClass(expectedName, buf, 0, total);
		if (checkClass(clazz)) {
			logger.info("Found jdbc driver "+clazz.getName()); //$NON-NLS-1$
			drivers.add(clazz.getName());
		}
		return clazz;
	}

	protected abstract boolean checkClass(Class<?> clazz);
}