/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ca.sqlpower.security;

import java.io.InputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import ca.sqlpower.util.ByteColonFormat;
import ca.sqlpower.util.Version;

/**
 * Parses the XML license file, verifies the key, and provides
 * (read-only) access to the properties that were in the file (if the
 * checksum was ok).
 *
 * <p>Note that the license class is immutable.  Any mutable objects
 * returned are cloned by the corresponding getXXX() method.
 *
 * @author Jonathan Fuerth
 * @version $Id$
 */
public final class License implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(License.class);
    
	protected String licenseeName;
	protected String productName;
	protected Version minVersion;
	protected Version maxVersion;
	protected java.util.Date issueDate;
	protected java.util.Date expiryDate;
	protected Properties limits;

	/**
	 *
	 * @param xmlStream An input stream pointing to the XML file which
	 * describes the license.
	 * @param dtdStream An input stream pointing to the DTD for
	 * xmlStream's XML document, if a DTD is required.  Otherwise, use
	 * null.
	 */
	public License(InputStream xmlStream, InputStream licenseStream)
	throws LicenseReadException {
	    
	    parseFile(xmlStream, licenseStream);
	    
	    if (logger.isInfoEnabled()) {
	        logger.info("Loaded SQLPower product license.");
	        logger.info("For "+productName+" versions "+minVersion+" to "+maxVersion);
	        logger.info("Licensed exclusively to "+licenseeName);
	        logger.info("Issued "+issueDate);
	        logger.info(expiryDate == null ? "Never Expires" : "Expires "+expiryDate);
	        logger.info("License terms:");
	        limits.list(System.out);
	    }
	}
	
	protected void parseFile(InputStream xmlStream, InputStream licenseStream)
		throws LicenseReadException {

		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		ByteColonFormat bcf = new ByteColonFormat();

		try {
			// Some random salt for the hash function
			short[] n = { 0xeb, 0x9c, 0xa5, 0xe8, 0x93, 0x6c, 0x06, 0x9c, 0x97, 
						  0x9b, 0x36, 0xd0, 0x37, 0x8d, 0x43, 0xef, 0xeb, 0xf3, 
						  0x45, 0xcf, 0x40, 0x81, 0xc8, 0xa3, 0x3f, 0x70, 0x09, 
						  0x0b, 0x11, 0xca };
			MessageDigest md = MessageDigest.getInstance("MD5");

			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			db.setEntityResolver(new LicenseDTDResolver(licenseStream));
			Document d = db.parse(xmlStream);
			Element de = d.getDocumentElement();

			Element elem = (Element) de.getElementsByTagName("licensee").item(0);
			licenseeName = elem.getFirstChild().getNodeValue();
			md.update(licenseeName.getBytes());
			md.update((byte) n[licenseeName.length() % n.length]);
			
			elem = (Element) de.getElementsByTagName("product").item(0);
			productName = elem.getFirstChild().getNodeValue();
			minVersion = new Version(elem.getAttribute("min-version"));
			maxVersion = new Version(elem.getAttribute("max-version"));

			md.update(productName.getBytes());
			md.update((byte) minVersion.getMajor());
			md.update((byte) minVersion.getMinor());
			md.update((byte) minVersion.getTiny());
			md.update((byte) maxVersion.getMajor());
			md.update((byte) maxVersion.getMinor());
			md.update((byte) maxVersion.getTiny());
			md.update((byte) n[ ((minVersion.getMajor()+minVersion.getMinor()
								 +minVersion.getTiny())*productName.length()) % n.length ]);
			md.update((byte) n[ ((maxVersion.getMajor()+maxVersion.getMinor()
								 +maxVersion.getTiny())*productName.length()) % n.length ]);

			elem = (Element) de.getElementsByTagName("issued").item(0);
			if (true) {
				int year = Integer.parseInt(elem.getAttribute("year"));
			    int month = Integer.parseInt(elem.getAttribute("month"));
				int day = Integer.parseInt(elem.getAttribute("day"));
				issueDate = new GregorianCalendar(year, month-1, day, 0, 0).getTime();
				md.update(df.format(issueDate).getBytes());
			}

			elem = (Element) de.getElementsByTagName("expires").item(0);
			if (elem != null) {
				int year = Integer.parseInt(elem.getAttribute("year"));
			    int month = Integer.parseInt(elem.getAttribute("month"));
				int day = Integer.parseInt(elem.getAttribute("day"));
				expiryDate = new GregorianCalendar(year, month-1, day, 23, 59).getTime();
				md.update(df.format(expiryDate).getBytes());
			}

			elem = (Element) de.getElementsByTagName("key").item(0);
			String licenseKey = elem.getFirstChild().getNodeValue();
			byte[] k = bcf.parse(licenseKey);

			// Arbitrary-length list of 'limit' elements
			NodeList limitNodeList = d.getElementsByTagName("limit");
			limits = new Properties();
			for(int i=0; i < limitNodeList.getLength(); i++) {
				Element limitNode = (Element) limitNodeList.item(i);
				String propName = limitNode.getAttribute("property");
				String propValue = limitNode.getAttribute("value");
				limits.setProperty(propName, propValue);
				md.update(propName.getBytes());
				md.update(propValue.getBytes());
			}

			// Check that the key is correct
			byte[] m = md.digest();
			if ( ! Arrays.equals(k,m) ) {
				throw new LicenseReadException("Invalid key", null);
			}

		} catch(Exception e){
			throw new LicenseReadException("Could not load product license descriptor", e);
		}
	}

	/**
	 * The XML parser needs help to find the license.dtd file.  This
	 * little class provides.
	 */
	protected static class LicenseDTDResolver implements EntityResolver {

		InputStream stream;
		
		/**
		 * Creates a dtd resolver which assumes the given input stream
		 * points to a source of license.dtd.
		 *
		 * @param dtdStream An input stream containing dtdStream, or
		 * null if there is no dtd stream available.
		 */
		public LicenseDTDResolver(InputStream dtdStream) {
			stream = dtdStream;
		}

		/**
		 * Returns an InputSource referencing the given dtdStream, if
		 * and only if systemId is "license.dtd" and the given
		 * dtdStream was non-null.
		 */
		public InputSource resolveEntity(String publicId, String systemId) {
			if (systemId.equals("license.dtd") && stream != null) {
				return new InputSource(stream);
			} else {
				return null;
			}
		}
	}

	/**
	 * Returns this license's licensee name (this is who paid to use
	 * the software).
	 */
	public String getLicenseeName() {
		return licenseeName;
	}
	
	/**
	 * Returns the product name which this license covers.
	 */
	public String getProductName() {
		return productName;
	}

	/**
	 * Returns a clone of this license's minimum applicable product
	 * version.
	 */
	public Version getMinVersion() {
		return (Version) minVersion.clone();
	}

	/**
	 * Returns a clone of this license's maximum applicable product
	 * version.
	 */
	public Version getMaxVersion() {
		return (Version) maxVersion.clone();
	}

	/**
	 * Returns a clone of this license's date of issue.
	 */
	public Date getIssueDate() {
		return (Date) issueDate.clone();
	}

	/**
	 * Returns a clone of this license's expiry date.
	 */
	public Date getExpiryDate() {
		if (expiryDate == null) {
			return null;
		} else {
			return (Date) expiryDate.clone();
		}
	}

	/**
	 * Returns a clone of this license's limit property list.
	 */
	public Properties getLimits() {
		return (Properties) limits.clone();
	}

	/**
	 * Returns the limit value associated with the given limit
	 * property name.  Use this method rather than
	 * getLimits().getProperty(String) when possible, because
	 * getLimits() clones the property list and returns the clone.
	 */
	public String getLimit(String property) {
		return limits.getProperty(property);
	}
}
