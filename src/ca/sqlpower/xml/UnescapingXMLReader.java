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

import java.io.IOException;

import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * An implementation of XMLReader wraps around another XMLReader instance. 
 */
public class UnescapingXMLReader implements XMLReader {

    private static final Logger logger = Logger.getLogger(UnescapingXMLReader.class);
    
    /**
     * The XMLReader that this instance of UnescapingXMLReader wraps around.
     */
    private final XMLReader reader;
    
    /**
     * @param xmlReader The XMLReader that this class wraps around.
     */
    public UnescapingXMLReader(XMLReader xmlReader) {
        reader = xmlReader;
    }

    /**
     * Delegates to the wrapped XMLReader's implementation of the method.
     */
    public ContentHandler getContentHandler() {
        return reader.getContentHandler();
    }

    /**
     * Delegates to the wrapped XMLReader's implementation of the method.
     */
    public DTDHandler getDTDHandler() {
        return reader.getDTDHandler();
    }

    /**
     * Delegates to the wrapped XMLReader's implementation of the method.
     */
    public EntityResolver getEntityResolver() {
        return reader.getEntityResolver();
    }

    /**
     * Delegates to the wrapped XMLReader's implementation of the method.
     */
    public ErrorHandler getErrorHandler() {
        return reader.getErrorHandler();
    }

    /**
     * Delegates to the wrapped XMLReader's implementation of the method.
     */
    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return reader.getFeature(name);
    }

    /**
     * Delegates to the wrapped XMLReader's implementation of the method.
     */
    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return reader.getProperty(name);
    }

    /**
     * Delegates to the wrapped XMLReader's implementation of the method.
     */
    public void parse(InputSource input) throws IOException, SAXException {
        reader.parse(input);
    }

    /**
     * Delegates to the wrapped XMLReader's implementation of the method.
     */
    public void parse(String systemId) throws IOException, SAXException {
        reader.parse(systemId);
    }

    /**
     * Takes the passed in ContentHandler and wraps it in an UnescapingDefaultHandler.
     * Takes the UnescapingDefaultHandler and passes into the wrapped XMLReader's
     * implementation of the setContentHandler().
     */
    public void setContentHandler(ContentHandler handler) {
        logger.debug("setContentHandler was called.  Wrapping given handler.");
        reader.setContentHandler(new UnescapingDefaultHandler(handler));
    }

    /**
     * Delegates to the wrapped XMLReader's implementation of the method.
     */
    public void setDTDHandler(DTDHandler handler) {
        reader.setDTDHandler(handler);
    }

    /**
     * Delegates to the wrapped XMLReader's implementation of the method.
     */
    public void setEntityResolver(EntityResolver resolver) {
        reader.setEntityResolver(resolver);
    }

    /**
     * Delegates to the wrapped XMLReader's implementation of the method.
     */
    public void setErrorHandler(ErrorHandler handler) {
        reader.setErrorHandler(handler);
    }
    
    /**
     * Delegates to the wrapped XMLReader's implementation of the method.
     */
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        reader.setFeature(name, value);
    }

    /**
     * Delegates to the wrapped XMLReader's implementation of the method.
     */
    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        reader.setProperty(name, value);
    }
}
