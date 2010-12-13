/*
 * Copyright (c) 2010, SQL Power Group Inc.
 */

package ca.sqlpower.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Stack;

import javax.swing.ProgressMonitor;

import org.apache.commons.codec.binary.Base64;

import ca.sqlpower.dao.upgrade.UpgradePersisterManager;
import ca.sqlpower.util.SQLPowerUtils;

/**
 * A persister to write persist calls to XML. This persister requires that its
 * input has been previously sorted such that all of an objects properties and
 * children come before its siblings. This persister also does not support
 * incremental changes. Once the workspace is committed, this persister will
 * close.
 */
public class XMLPersister implements SPPersister {

	public final String PROJECT_TAG;
	
	private static UpgradePersisterManager upgradePersisterManager;
	
	public static void setUpgradePersisterManager(UpgradePersisterManager upgradePersisterManager) {
		XMLPersister.upgradePersisterManager = upgradePersisterManager;
	}
	
	private final Stack<String> currentObject = new Stack<String>();
	
	private final Stack<String> currentType = new Stack<String>();
	
	private final PrintWriter out;
	
	private final ByteArrayOutputStream bufferedOut;

	/**
	 * The fully qualified class name of the object that is the root of the tree of objects being
	 * persisted.
	 */
	private final String rootObject;
	
	private int transactionCount = 0;

	private final OutputStream finalOut;

	private final ProgressMonitor pm;
	
	private int progress = 0;

	public XMLPersister(OutputStream out, String rootObject, String projectTag) {
		this(out, rootObject, projectTag, null);
	}
	
	public XMLPersister(OutputStream out, String rootObject, String projectTag, ProgressMonitor pm) {
		bufferedOut = new ByteArrayOutputStream();
		this.out = new PrintWriter(bufferedOut);
		this.finalOut = out;
		this.rootObject = rootObject;
		this.pm = pm;
		PROJECT_TAG = projectTag;
	}
	
	@Override
	public void begin() throws SPPersistenceException {
		if (transactionCount == 0) {
			out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			out.println("<" + PROJECT_TAG + " file-version=\"" + upgradePersisterManager.getStateVersion() + "\">");
		}
		transactionCount++;
	}

	@Override
	public void commit() throws SPPersistenceException {
		transactionCount--;
		if (transactionCount == 0) {
			while (!currentType.isEmpty()) {
				currentObject.pop();
				out.println(tab() + "</" + currentType.pop().replace("$", "..") + ">");
			}
			out.println("</" + PROJECT_TAG + ">");
			out.flush();
			try {
				byte[] byteArray = bufferedOut.toByteArray();
				if (pm != null) {
					pm.setMaximum(byteArray.length);
				}
				for (int i = 0; i < byteArray.length; i++) {
					finalOut.write(byteArray[i]);
					if (pm != null) {
						pm.setProgress(++progress);
					}
				}
		    	
				finalOut.flush();
			} catch (IOException e) {
				throw new SPPersistenceException(null, e);
			}
		}
	}

	@Override
	public void persistObject(String parentUUID, String type, String uuid,
			int index) throws SPPersistenceException {
		if (uuid == null) uuid = "";
		if (parentUUID == null) parentUUID = "";
		while (!currentObject.isEmpty() && !parentUUID.equals(currentObject.peek())) {
			currentObject.pop();
			out.println(tab() + "</" + currentType.pop().replace("$", "..") + ">");
		}
		if (currentObject.isEmpty()) {
			if (!type.equals(rootObject)) {
				throw new SPPersistenceException(null,
						"This persister does not support incremental updates."
								+ " The first object persisted must be the root.");
			}
		} else if (!parentUUID.equals(currentObject.peek())) {
			throw new SPPersistenceException(null,
					"This persister requires persists to be ordered. An object at ["
					+ uuid + "] as a child of [" + parentUUID
					+ "] was persisted while the current object was ["
					+ currentObject.peek() + "]");
		}
		out.println(tab() + "<" + type.replace("$", "..") + " UUID=\"" + SQLPowerUtils.escapeXML(uuid) + "\" index=\"" + index + "\">");
		currentObject.push(uuid);
		currentType.push(type);
	}

	@Override
	public void persistProperty(String uuid, String propertyName,
			DataType propertyType, Object oldValue, Object newValue)
			throws SPPersistenceException {
		throw new UnsupportedOperationException("This persister does not support incremental updates. Use the unconditional persistProperty instead");
	}

	@Override
	public void persistProperty(String uuid, String propertyName,
			DataType propertyType, Object newValue)
			throws SPPersistenceException {
		if (uuid == null) uuid = "";
		if (currentObject.isEmpty()) {
			throw new SPPersistenceException("Recieved property for object [" + uuid + "] which does not exist");
		}
		if (!uuid.equals(currentObject.peek())) {
			throw new SPPersistenceException(null,
					"This persister requires persists to be ordered. An property of ["
							+ uuid + "] was persisted while the current object was ["
							+ currentObject.peek() + "]");
		}
		if (propertyType != DataType.NULL && newValue != null) {
			out.print(tab() + "<property name=\"" + SQLPowerUtils.escapeNewLines(SQLPowerUtils.escapeXML(propertyName)) + "\" type=\"" + propertyType.toString() + "\"");
			if (propertyType == DataType.PNG_IMG) {
				try {
					out.print(" value=\"");
					ByteArrayOutputStream data = new ByteArrayOutputStream();
					SQLPowerUtils.copyStream((InputStream) newValue, data);
					byte[] bytes = data.toByteArray();
					byte[] base64Bytes = Base64.encodeBase64(bytes);
					out.print(new String(base64Bytes));
					out.println("\"/>");
				} catch (IOException e) {
					throw new SPPersistenceException(uuid, e);
				}
			} else {
				out.println(" value=\"" + SQLPowerUtils.escapeXML(newValue.toString()) + "\"/>");
			}
		}
	}

	@Override
	public void removeObject(String parentUUID, String uuid)
			throws SPPersistenceException {
		throw new UnsupportedOperationException("This persister does not support incremental updates");
	}

	@Override
	public void rollback() {
		
	}
	
	private String tab() {
		StringBuilder tab = new StringBuilder();
		for (int i = 0; i <= currentObject.size(); i++) {
			tab.append(" ");
		}
		return tab.toString();
	}
	
}
