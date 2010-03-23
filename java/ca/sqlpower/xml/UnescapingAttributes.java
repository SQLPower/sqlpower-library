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
/**
 * 
 */
package ca.sqlpower.xml;

import org.xml.sax.Attributes;


/**
 *  An implementation of Attributes that wraps around another Attributes instance.
 *  It is used to read XML attributes from an XML document that was generated using
 *  the XMLHelper class' methods.
 */
public class UnescapingAttributes implements Attributes {

    /**
     * The Attributes that this instance of UnescapingAttributes wraps around.
     */
    private final Attributes attr;
    
    /**
     * @param attributes The Attributes that this instance wraps around
     */
    public UnescapingAttributes(Attributes attributes) {
        attr = attributes;
    }

    /**
     * Delegates to the wrapped Attributes' implementation of the method.
     * 
     * @param uri
     * @param localName
     * @return
     * @see org.xml.sax.Attributes#getIndex(java.lang.String, java.lang.String)
     */
    public int getIndex(String uri, String localName) {
        return attr.getIndex(uri, localName);
    }

    /**
     * Delegates to the wrapped Attributes' implementation of the method.
     * 
     * @param qName
     * @return
     * @see org.xml.sax.Attributes#getIndex(java.lang.String)
     */
    public int getIndex(String qName) {
        return attr.getIndex(qName);
    }

    /**
     * Delegates to the wrapped Attributes' implementation of the method.
     * 
     * @return
     * @see org.xml.sax.Attributes#getLength()
     */
    public int getLength() {
        return attr.getLength();
    }

    /**
     * Delegates to the wrapped Attributes' implementation of the method.
     * 
     * @param index
     * @return
     * @see org.xml.sax.Attributes#getLocalName(int)
     */
    public String getLocalName(int index) {
        return attr.getLocalName(index);
    }

    /**
     * Delegates to the wrapped Attributes' implementation of the method.
     * 
     * @param index
     * @return
     * @see org.xml.sax.Attributes#getQName(int)
     */
    public String getQName(int index) {
        return attr.getQName(index);
    }

    /**
     * Delegates to the wrapped Attributes' implementation of the method.
     * 
     * @param index
     * @return
     * @see org.xml.sax.Attributes#getType(int)
     */
    public String getType(int index) {
        return attr.getType(index);
    }

    /**
     * Delegates to the wrapped Attributes' implementation of the method.
     * 
     * @param uri
     * @param localName
     * @return
     * @see org.xml.sax.Attributes#getType(java.lang.String, java.lang.String)
     */
    public String getType(String uri, String localName) {
        return attr.getType(uri, localName);
    }

    /**
     * Delegates to the wrapped Attributes' implementation of the method.
     * 
     * @param qName
     * @return
     * @see org.xml.sax.Attributes#getType(java.lang.String)
     */
    public String getType(String qName) {
        return attr.getType(qName);
    }

    /**
     * Delegates to the wrapped Attributes' implementation of the method.
     * 
     * @param index
     * @return
     * @see org.xml.sax.Attributes#getURI(int)
     */
    public String getURI(int index) {
        return attr.getURI(index);
    }

    /**
     * @param index
     * @return An unescaped version of the value given from the wrapped Attributes
     * @see org.xml.sax.Attributes#getValue(int)
     */
    public String getValue(int index) {
        return XMLHelper.unescape(attr.getValue(index));
    }

    /**
     * @param uri
     * @param localName
     * @return An unescaped version of the value given from the wrapped Attributes
     * @see org.xml.sax.Attributes#getValue(java.lang.String, java.lang.String)
     */
    public String getValue(String uri, String localName) {
        return XMLHelper.unescape(attr.getValue(uri, localName));
    }

    /**
     * @param qName
     * @return An unescaped version of the value given from the wrapped Attributes
     * @see org.xml.sax.Attributes#getValue(java.lang.String)
     */
    public String getValue(String qName) {
        return XMLHelper.unescape(attr.getValue(qName));
    }
 

}
