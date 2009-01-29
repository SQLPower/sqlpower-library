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
package ca.sqlpower.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;

import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class StubSAXParser extends SAXParser {

    /**
     * Every method that takes a DefaultHandler arguement
     * will store a reference to it here.  Test cases can
     * then examine it and make sure (for instance) that it
     * was the same implementation of DefaultHandler that
     * they expect.
     */
    DefaultHandler handler;
    
    @Override
    public Parser getParser() throws SAXException {
        return null;
    }

    @Override
    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return null;
    }

    @Override
    public XMLReader getXMLReader() throws SAXException {
        return null;
    }

    @Override
    public boolean isNamespaceAware() {
        return false;
    }

    @Override
    public boolean isValidating() {
        return false;
    }

    @Override
    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
    }

    @Override
    public void parse(File f, DefaultHandler dh) throws SAXException, IOException {
        handler = dh;
    }
    
    @Override
    public void parse(InputSource is, DefaultHandler dh) throws SAXException, IOException {
        handler = dh;
    }
    
    @Override
    public void parse(InputStream is, DefaultHandler dh, String systemId) throws SAXException, IOException {
        handler = dh;
    }
    
    @Override
    public void parse(InputStream is, DefaultHandler dh) throws SAXException, IOException {
        handler = dh;
    }
    
    @Override
    public void parse(String uri, DefaultHandler dh) throws SAXException, IOException {
        handler = dh;
    }
}
