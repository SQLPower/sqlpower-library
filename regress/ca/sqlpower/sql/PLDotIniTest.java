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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sql.SPDataSourceType;

import junit.framework.TestCase;

public class PLDotIniTest extends TestCase {

	private static final String FUN_DATASOURCE_NAME = "broomhilda";
	private PlDotIni target;

	@Override
	public void setUp() {
		target = new PlDotIni();
	}
	/*
	 * Test method for 'ca.sqlpower.architect.PlDotIni.read(File)'
	 */
	public void testRead() throws IOException {
		target.read(makeInputStream(makePlIniString()));
        assertTrue(target.getConnections().size() > 0);
	}
	
	/**
     * Creates an InputStream of the bytes of the string you supply, using
     * the ISO Latin 1 encoding.
	 */
	private static InputStream makeInputStream(String fileContents) throws IOException {
        return new ByteArrayInputStream(fileContents.getBytes("ISO8859-1"));
	}

    /**
     * Creates a string with an example pl.ini file's contents in it.
     */
    private String makePlIniString() {
        // The PLDotInei classe ONLYe reades files withe ye ancient MS-DOSe linee endingse...
        StringBuilder buf = new StringBuilder(1000);
        buf.append("sectionless key=meaningless value" + DataSourceCollection.DOS_CR_LF);
        buf.append("[Database Types_1]" + DataSourceCollection.DOS_CR_LF);
        buf.append("Name=my db type" + DataSourceCollection.DOS_CR_LF);
        buf.append("silly property=diddle" + DataSourceCollection.DOS_CR_LF);
        buf.append("[Database Types_2]" + DataSourceCollection.DOS_CR_LF);
        buf.append("Name=my other db type" + DataSourceCollection.DOS_CR_LF);
        buf.append("silly property=fiddle" + DataSourceCollection.DOS_CR_LF);
        buf.append("Parent Type=my db type" + DataSourceCollection.DOS_CR_LF);
        buf.append("[random_crap]" + DataSourceCollection.DOS_CR_LF);
        buf.append("foo=bar" + DataSourceCollection.DOS_CR_LF);
        buf.append("fred=george" + DataSourceCollection.DOS_CR_LF);
        buf.append("[Databases_1]" + DataSourceCollection.DOS_CR_LF);
        // Note, when writing, we always put the Logical entry first. This is a requirement of the PL engine.
        buf.append("Logical=" + FUN_DATASOURCE_NAME + DataSourceCollection.DOS_CR_LF);
        // Note, due to an implementation detail, the connection type will always be the first entry in a DBCS section.
        // The order of all other keys is expected to be preserved when you just read the file and write it back out.
        buf.append("Connection Type=my db type" + DataSourceCollection.DOS_CR_LF);
        buf.append("Type=POSTGRES" + DataSourceCollection.DOS_CR_LF);
        buf.append("JDBC Driver Class=org.postgresql.Driver" + DataSourceCollection.DOS_CR_LF);
        buf.append("PWD=" + DataSourceCollection.DOS_CR_LF);
        buf.append("L Schema Owner=" + DataSourceCollection.DOS_CR_LF);
        buf.append("DSN=" + DataSourceCollection.DOS_CR_LF);
        buf.append("JDBC URL=jdbc:postgresql://:5432/" + DataSourceCollection.DOS_CR_LF);
        return buf.toString();
    }
	/*
	 * Test method for 'ca.sqlpower.architect.PlDotIni.write(OutputStream)'
	 */
	public void testWriteOutputStream() throws IOException {
        String orig = makePlIniString();
		InputStream in = makeInputStream(orig);
		target.read(in);
		
        ByteArrayOutputStream out = new ByteArrayOutputStream();
		target.write(out);
		assertEquals(orig, out.toString());
	}

    /* ensures we read all sections in the correct order */
    public void testReadSections() throws Exception {
        testRead();
        assertEquals(5, target.getSectionCount());
        
        // the nameless first section
        Object s = target.getSection(0);
        assertEquals(PlDotIni.Section.class, s.getClass());
        assertEquals(null, ((PlDotIni.Section) s).getName());
        
        s = target.getSection(1);
        assertEquals(SPDataSourceType.class, s.getClass());

        s = target.getSection(2);
        assertEquals(SPDataSourceType.class, s.getClass());
        
        s = target.getSection(3);
        assertEquals(PlDotIni.Section.class, s.getClass());

        s = target.getSection(4);
        assertEquals(SPDataSource.class, s.getClass());
    }

    /* simple test of getDataSource() which assumes addDataSource() works. */
    public void testGetDataSource() {
        SPDataSource dbcs = new SPDataSource(target);
        dbcs.setName("cows");
        target.addDataSource(dbcs);
        
        SPDataSource gotDbcs = target.getDataSource("cows");
        assertNotNull(gotDbcs);
        assertSame(dbcs, gotDbcs);
    }
    
	/*
     * Ensures the read() method reads data sources properly,
     * assuming getDataSource() works properly
	 */
	public void testReadDataSource() throws IOException {
        testRead();
		SPDataSource ds = target.getDataSource(FUN_DATASOURCE_NAME);
		assertNotNull(ds);
	}
    
    private static String plIniToString(PlDotIni plini) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < plini.getSectionCount(); i++) {
            Object o = plini.getSection(i);
            if (o instanceof PlDotIni.Section) {
                sb.append(((PlDotIni.Section) o).getPropertiesMap().toString());
            } else if (o instanceof SPDataSource) {
                sb.append(((SPDataSource) o).getPropertiesMap().toString());
            } else if (o instanceof SPDataSourceType) {
                sb.append(((SPDataSourceType) o).getProperties().toString());
            } else {
                throw new IllegalArgumentException("Unknown pl.ini section type: "+o);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    public void testReRead() throws IOException {
        target.read(makeInputStream(makePlIniString()));

        String orig = plIniToString(target);

        // re-reading should just merge in all the same stuff and leave object unchanged
        target.read(makeInputStream(makePlIniString()));

        String after = plIniToString(target);

        assertEquals(orig, after);
    }

    /* Ensures that the read() method parses data source types properly,
     * assuming getDataSourceType() works.
     */
    public void testReadDataSourceType() throws IOException {
        testRead();
        
        SPDataSourceType dst = target.getDataSourceType("my db type");
        assertNotNull(dst);
        assertEquals(dst.getProperty("silly property"), "diddle");
        
        SPDataSourceType dst2 = target.getDataSourceType("my other db type");
        assertNotNull(dst2);
        assertEquals(dst2.getProperty("silly property"), "fiddle");
        assertSame(dst, dst2.getParentType());
    }

	/*
	 * Test method for 'ca.sqlpower.architect.PlDotIni.getConnections()'
	 */
	public void testGetConnections() throws IOException {
        testRead();
		List l = target.getConnections();
		assertEquals(1, l.size());
		assertEquals(target.getDataSource(FUN_DATASOURCE_NAME), l.get(0));
	}
    
    public void testDatabaseTypeParentDoesntExist() throws Exception {
        testRead();
        
        SPDataSourceType dbType = target.getDataSourceType("my other db type");
        dbType.putProperty(SPDataSourceType.PARENT_TYPE_NAME, "non existant parent");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        target.write(out);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try {
            target.read(in);
            fail("Parent type didn't exist but no exception was thrown.");
        } catch (IllegalStateException ex) {
            // test passed
        }
    }

    public void testHookUpDataSourceToParentType() throws Exception {
        testRead();
        SPDataSource ds = target.getDataSource(FUN_DATASOURCE_NAME);
        assertNotNull(ds);
        assertNotNull(ds.getParentType());
    }
    
    /**
     * Tests that any items coming before the first section get written out
     * when we re-save the file.
     */
    public void testNotLoseItemsWithoutSection() throws Exception {
        testRead();
        
        File tmp2 = File.createTempFile("pl.out", null);
        target.write(tmp2);
        
        PlDotIni reread = new PlDotIni();
        reread.read(tmp2);
        Object sect = reread.getSection(0);
        assertEquals(PlDotIni.Section.class, sect.getClass());
        assertNull(((PlDotIni.Section) sect).getName());
    }
    
    public void testAddDsType() {
        int oldCount = target.getDataSourceTypes().size();
        target.addDataSourceType(new SPDataSourceType());
        assertEquals(oldCount + 1, target.getDataSourceTypes().size());
    }
    
    public void testRemoveDsType() {
        final SPDataSourceType newType = new SPDataSourceType();
        target.addDataSourceType(newType);
        int oldCount = target.getDataSourceTypes().size();
        
        assertFalse(target.removeDataSourceType(null));
        assertEquals(oldCount, target.getDataSourceTypes().size());

        assertFalse(target.removeDataSourceType(new SPDataSourceType()));
        assertEquals(oldCount, target.getDataSourceTypes().size());

        assertTrue(target.removeDataSourceType(newType));
        assertEquals(oldCount - 1, target.getDataSourceTypes().size());
    }
    
    /**
     * Ensures every data source child of this pl.ini has the
     * right parent pointer.
     */
    public void testParentOfDataSources() throws Exception{
        testRead();
        for (SPDataSource ds : target.getConnections()) {
            assertSame(target, ds.getParentCollection());
        }
    }

}
