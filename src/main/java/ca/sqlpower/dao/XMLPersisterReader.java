/*
 * Copyright (c) 2010, SQL Power Group Inc.
 */

package ca.sqlpower.dao;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Stack;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ca.sqlpower.dao.SPPersister.DataType;
import ca.sqlpower.dao.upgrade.UpgradePersisterManager;
import ca.sqlpower.util.SQLPowerUtils;

public class XMLPersisterReader {

	private static final Logger logger = Logger
			.getLogger(XMLPersisterReader.class);
	
	private final Reader in;
	private final SPPersister target;
	private SPPersister upgradeTarget;
	private UpgradePersisterManager upgradePersisterManager;
	
	public final String PROJECT_TAG;

	public XMLPersisterReader(Reader in, SPPersister target, UpgradePersisterManager upgradePersisterManager, String projectTag) {
		this.in = in;
		this.target = target;
		this.upgradePersisterManager = upgradePersisterManager;
		this.PROJECT_TAG = projectTag;
	}
	
	public void read() throws SPPersistenceException {
		BufferedReader reader = new BufferedReader(in);
		String line;
		try {
			reader.mark(200);
			line = reader.readLine();
			while (!line.contains("<" + PROJECT_TAG)) {
				line = reader.readLine();
			}
			reader.reset();
		} catch (IOException e) {
			throw new SPPersistenceException(null, e);
		}
		
		int version = Integer.valueOf(line.substring(line.indexOf("file-version=") + "file-version=\"".length(), line.length()-2));
		
		SPUpgradePersister latest = null;
		SPPersister previousTarget = null;
		upgradeTarget = target;
		if (version != upgradePersisterManager.getStateVersion()) {
			SPUpgradePersister newUpgradeTarget = upgradePersisterManager.getUpgradePersister(version);
			if (newUpgradeTarget != null) {
				upgradeTarget = newUpgradeTarget;
			}
			
			latest = upgradePersisterManager.getUpgradePersister(upgradePersisterManager.getStateVersion()-1);
			
			if (latest != null) {
				previousTarget = latest.getNextPersister();
				latest.setNextPersister(target, false);
			}
		}
		
		try {
			upgradeTarget.begin();
			SAXHandler handler = new SAXHandler();
			SAXParser parser = new SAXParser();
			parser.setContentHandler(handler);
			parser.parse(new InputSource(reader));
			upgradeTarget.commit();
		} catch (Exception e) {
			if (latest != null) {
				latest.setNextPersister(previousTarget, false);
			}
			logger.error("error loading project", e);
			upgradeTarget.rollback();
			throw new SPPersistenceException(null, e);
		}
	}
	
	private Object castValue(DataType type, String value) {
		switch (type) {
		case INTEGER:
			return Integer.valueOf(value);
		case LONG:
			return Long.valueOf(value);
		case DOUBLE:
			return Double.valueOf(value);
		case BOOLEAN:
			return Boolean.valueOf(value);
		case NULL:
			return null;
		case PNG_IMG:
			return new ByteArrayInputStream(Base64.decodeBase64(value.getBytes()));
		case STRING:
		case REFERENCE:
			return value;
		default:
			throw new IllegalArgumentException("Unknown DataType " + type);
		}
	}
	
	private class SAXHandler extends DefaultHandler {
		
		private Stack<String> currentObject = new Stack<String>();
		
		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			try {
				if ("property".equals(localName)) {
					String name = attributes.getValue("name");
					DataType type = DataType.valueOf(attributes.getValue("type"));
					String value = attributes.getValue("value");
					value = SQLPowerUtils.unEscapeNewLines(value);
					upgradeTarget.persistProperty(currentObject.peek(), name, type, castValue(type, value));
				} else if (PROJECT_TAG.equals(localName)) {
					// ignore
				} else {
					logger.debug("Reading element " + localName);
					String type = localName.replace("..", "$");
					String UUID = attributes.getValue("UUID");
					int index = Integer.valueOf(attributes.getValue("index"));

					String parent;
					if (currentObject.isEmpty()) {
						parent = "";
					} else {
						parent = currentObject.peek();
					}
					upgradeTarget.persistObject(parent, type, UUID, index);
					currentObject.push(UUID);
				}
			} catch (SPPersistenceException e) {
				throw new RuntimeException(e);
			}
		}
		
		

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			if (!"property".equals(localName) && !PROJECT_TAG.equals(localName)) {
				currentObject.pop();
			}
		}
		
	}
	
}
