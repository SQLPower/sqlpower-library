/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

package ca.sqlpower.object.annotation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import ca.sqlpower.dao.AbstractSPPersisterHelper;
import ca.sqlpower.dao.PersistedSPOProperty;
import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.object.SPListener;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.util.SPSession;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.Filer;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.util.DeclarationVisitors;

/**
 * This {@link AnnotationProcessor} processes the annotations in
 * {@link SPObject}s to generate persister helper methods to be used in a
 * session {@link SPPersister}.
 */
public class SPAnnotationProcessor implements AnnotationProcessor {

	/**
	 * The {@link AnnotationProcessorEnvironment} this
	 * {@link AnnotationProcessor} will work with. The environment will give
	 * useful information about classes annotated with {@link Persistable}.
	 */
	private final AnnotationProcessorEnvironment environment;

	/**
	 * Creates a new {@link SPAnnotationProcessor} that deals exclusively with
	 * annotations used in {@link SPObject}s which can generate persister helper
	 * methods for session {@link SPPersister}s.
	 * 
	 * @param environment
	 *            The {@link AnnotationProcessorEnvironment} this processor will
	 *            work with.
	 */
	public SPAnnotationProcessor(AnnotationProcessorEnvironment environment) {
		this.environment = environment;
	}

	public void process() {
		for (TypeDeclaration typeDecl : environment.getTypeDeclarations()) {
			SPClassVisitor visitor = new SPClassVisitor();
			typeDecl.accept(DeclarationVisitors.getDeclarationScanner(DeclarationVisitors.NO_OP, visitor));
			if (visitor.getVisitedClass() != null) {
				generatePersisterHelperFile(visitor);
			}
		}
	}

	/**
	 * Generates the Java source file for a persister helper class that is to be
	 * used by a session {@link SPPersister} or workspace persister listener.
	 * This generated persister helper class should deal with creating new
	 * objects and applying persisted properties to a given {@link SPObject}, or
	 * persisting objects and properties from an {@link SPObject} to an
	 * {@link SPPersister}.
	 * 
	 * @param visitor
	 *            The {@link SPClassVisitor}
	 */
	private void generatePersisterHelperFile(SPClassVisitor visitor) {
		try {
			Class<? extends SPObject> visitedClass = visitor.getVisitedClass();
			Filer f = environment.getFiler();
			PrintWriter pw = f.createSourceFile(visitedClass.getSimpleName() + "PersisterHelper");
			pw.print("public class " + visitedClass.getSimpleName() + 
					"PersisterHelper extends " + 
					AbstractSPPersisterHelper.class.getSimpleName() + 
					"<" + visitedClass.getSimpleName() + "> {\n");
			pw.print(generateCommitObjectMethod(visitedClass, visitor.getConstructorParameters(), 1));
			pw.print(generateCommitPropertyMethod(visitedClass, visitor.getPropertiesToMutate(), 1));
			pw.print(generateRetrievePropertyMethod(visitedClass, visitor.getPropertiesToAccess(), 1));
			pw.print(generatePersistObjectMethod(visitedClass, visitor.getConstructorParameters(), visitor.getPropertiesToMutate(), 1));
			pw.print("}\n");
			pw.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Builds a String with the given number of tab characters to use for
	 * indenting generated code.
	 * 
	 * @param i
	 *            The number of tab characters.
	 * @return A String with the given number of tab characters.
	 */
	private String indent(int i) {
		StringBuilder sb = new StringBuilder();
		final String tab = "\t";
		for (; i > 0; i--) {
			sb.append(tab);
		}
		return sb.toString();
	}

	/**
	 * Generates and returns source code for a commitObject method based on an
	 * {@link SPObject} annotated constructor along with its annotated
	 * constructor arguments.
	 * 
	 * @param visitedClass
	 *            The {@link SPObject} class that is being visited by the
	 *            annotation processor.
	 * @param constructorParameters
	 *            The {@link LinkedHashMap} of the class property names to
	 *            constructor parameter class types. This cannot be a generic
	 *            {@link Map} or {@link HashMap} because order must be
	 *            guaranteed for constructor arguments.
	 * @param tabs
	 *            The number of tab characters to use to indent the generated
	 *            method.
	 * @return The source code for the generated commitObject method.
	 * @see SPPersister#persistObject(String, String, String, int)
	 */
	private String generateCommitObjectMethod(
			Class<? extends SPObject> visitedClass, 
			LinkedHashMap<String, Class<?>> constructorParameters, int tabs) {
		StringBuilder sb = new StringBuilder();
		final String persistedPropertiesField = "persistedProperties";
		
		sb.append(indent(tabs));
		sb.append("public " + visitedClass.getSimpleName() + " commitObject(" +
				Collection.class.getSimpleName() + "<" + 
				PersistedSPOProperty.class.getSimpleName() + "> " +
				persistedPropertiesField + ") {\n");
		tabs++;
		
		for (Entry<String, Class<?>> e : constructorParameters.entrySet()) {
			sb.append(indent(tabs));
			sb.append(e.getValue().getCanonicalName() + " " + e.getKey() + 
					" = retrievePropertyAndRemove(" + persistedPropertiesField + 
					", \"" + e.getKey() + "\")\n");
		}
		
		sb.append("\n");
		sb.append(indent(tabs));
		sb.append("return new " + visitedClass.getCanonicalName() + "(");
		
		boolean firstArg = true;
		
		for (String propertyName : constructorParameters.keySet()) {
			if (!firstArg) {
				sb.append(", ");
			}
			sb.append(propertyName);
			firstArg = false;
		}
		
		sb.append(");\n");
		
		tabs--;
		sb.append(indent(tabs));
		sb.append("}\n");
		
		return sb.toString();
	}

	/**
	 * Generates and returns source code for a commitProperty method based on
	 * setter methods annotated with {@link Mutator} used in a given
	 * {@link SPObject} class. The purpose of this commitProperty method is to
	 * allow a session {@link SPPersister} to commit a persisted property change
	 * into an {@link SPSession}. This helper method will be called by the
	 * session {@link SPPersister#commit()} method.
	 * 
	 * @param visitedClass
	 *            The {@link SPObject} class that is being visited by the
	 *            annotation processor.
	 * @param properties
	 *            The {@link Set} of mutable properties that can be persisted by
	 *            a session {@link SPPersister}.
	 * @param tabs
	 *            The number of tab characters to use to indent this generated
	 *            method block.
	 * @return The source code for the generated commitProperty method.
	 * @see SPPersister#persistProperty(String, String,
	 *      ca.sqlpower.dao.SPPersister.DataType, Object)
	 * @see SPPersister#commit()
	 */
	private String generateCommitPropertyMethod(
			Class<? extends SPObject> visitedClass,
			Set<String> properties, int tabs) {
		StringBuilder sb = new StringBuilder();
		final String objectField = "spo";
		final String propertyNameField = "propertyName";
		final String newValueField = "newValue";
		
		boolean firstIf = true;
		
		sb.append(indent(tabs));
		sb.append("public void commitProperty(" + visitedClass.getCanonicalName() + " " + 
				objectField + ", " + String.class.getSimpleName() + " " + 
				propertyNameField + ", " + Object.class.getSimpleName() + " " +
				newValueField + ") " + "throws " + 
				SPPersistenceException.class.getSimpleName() + " {\n");
		tabs++;
		
		for (String propertyName : properties) {
			
			sb.append(indent(tabs));
			
			if (!firstIf) {
				sb.append("} else ");
			}
			
			sb.append("if (" + propertyNameField + ".equals(\"" + propertyName + "\")) {\n");
			tabs++;
			
			sb.append(indent(tabs));
			sb.append(objectField + ".set" + propertyName.substring(0, 1).toUpperCase() + 
					((propertyName.length() > 1)? propertyName.substring(1) : "") + 
					"(" + newValueField + ");\n");
			
			tabs--;
			firstIf = false;
		}
		
		if (!firstIf) {
			sb.append(indent(tabs));
			sb.append("} else {\n");
			tabs++;
		}
		
		sb.append(indent(tabs));
		sb.append("throw new " + SPPersistenceException.class.getSimpleName() + 
				"(" + objectField + ".getUUID(), generateSPPersistenceExceptionMessage(" + 
				objectField + ", " + propertyNameField + "));\n");
		
		if (!firstIf) {
			tabs--;
			sb.append(indent(tabs));
			sb.append("}\n");
		}
		
		tabs--;
		sb.append(indent(tabs));
		sb.append("}\n");
		
		return sb.toString();
	}

	/**
	 * Generates and returns source code for a retrieveProperty method based on
	 * getter methods annotated with {@link Accessor} used in a given
	 * {@link SPObject} class. The purpose of this retrieveProperty method is to
	 * allow a session {@link SPPersister} to get the value of a given property
	 * in an {@link SPObject} and compare it with the expected value. This
	 * helper method will be called by the session
	 * {@link SPPersister#persistProperty(String, String, ca.sqlpower.dao.SPPersister.DataType, Object, Object)}
	 * method.
	 * 
	 * @param visitedClass
	 *            The {@link SPObject} class that is being visited by the
	 *            annotation processor.
	 * @param properties
	 *            The {@link Set} of accessible properties that can be persisted
	 *            by a session {@link SPPersister}.
	 * @param tabs
	 *            The number of tab characters to use to indent this generated
	 *            method block.
	 * @return The source code for the generated retrieveProperty method.
	 * @see SPPersister#persistProperty(String, String,
	 *      ca.sqlpower.dao.SPPersister.DataType, Object, Object)
	 */
	private String generateRetrievePropertyMethod(
			Class<? extends SPObject> visitedClass,
			Set<String> properties, int tabs) {
		StringBuilder sb = new StringBuilder();
		final String objectField = "spo";
		final String propertyNameField = "propertyName";
		
		boolean firstIf = true;
		
		sb.append(indent(tabs));
		sb.append("public " + Object.class.getSimpleName() + " retrieveProperty(" + 
				visitedClass.getCanonicalName() + " " + objectField + ", " +
				String.class.getSimpleName() + " " + propertyNameField + ") " + 
				"throws " + SPPersistenceException.class.getSimpleName() + " {\n");
		tabs++;
		
		for (String propertyName : properties) {
			
			sb.append(indent(tabs));
			
			if (!firstIf) {
				sb.append("} else ");
			}
			
			sb.append("if (" + propertyNameField + ".equals(\"" + propertyName + "\")) {\n");
			tabs++;
			
			sb.append(indent(tabs));
			sb.append("return " + objectField + ".get" + propertyName.substring(0, 1).toUpperCase() + 
					((propertyName.length() > 1)? propertyName.substring(1) : "") + 
					"();\n");
			
			tabs--;
			firstIf = false;
		}
		
		if (!firstIf) {
			sb.append(indent(tabs));
			sb.append("} else {\n");
			tabs++;
		}
		
		sb.append(indent(tabs));
		sb.append("throw new " + SPPersistenceException.class.getSimpleName() + 
				"(" + objectField + ".getUUID(), generateSPPersistenceExceptionMessage(" + 
				objectField + ", " + propertyNameField + "));\n");
		
		if (!firstIf) {
			tabs--;
			sb.append(indent(tabs));
			sb.append("}\n");
		}
		
		tabs--;
		sb.append(indent(tabs));
		sb.append("}\n");
		
		return sb.toString();
	}

	/**
	 * Generates and returns source code for a persistObject method based on the
	 * constructor annotated with {@link Constructor} with its constructor
	 * parameters annotated with {@link ConstructorParameter}, as well as the
	 * persistable properties with their getters/setters annotated with
	 * {@link Accessor} and {@link Mutator}. This generated method should
	 * persist the entire state of an {@link SPObject} to an {@link SPPersister}
	 * and should be called from a workspace persister {@link SPListener}. This
	 * means that the union of the constructor parameter properties and
	 * persistable properties must be persisted.
	 * 
	 * @param visitedClass
	 *            The {@link SPObject} class that is being visited by the
	 *            annotation processor.
	 * @param constructorParameters
	 *            The {@link LinkedHashMap} of property names to constructor
	 *            parameters, where values passed into the constructor will set
	 *            those respective properties. This is not a generic {@link Map}
	 *            or {@link HashMap} because order must be guaranteed to follow
	 *            the order of the constructor parameters. These properties may
	 *            or may not be persistable, so it is entirely possible that not
	 *            all of the properties in this map exists in the given
	 *            {@link Set} of persistable properties.
	 * @param properties
	 *            The {@link Set} of persistable properties where each of their
	 *            getter/setter methods are annotated with {@link Accessor} and
	 *            {@link Mutator}.
	 * @param tabs
	 *            The number of tab characters to use to indent this generated
	 *            method block.
	 * @return The source code for the generated persistObject method.
	 */
	private String generatePersistObjectMethod(
			Class<? extends SPObject> visitedClass,
			LinkedHashMap<String, Class<?>> constructorParameters,
			Set<String> properties, int tabs) {
		StringBuilder sb = new StringBuilder();
		final String objectField = "spo";
		final String indexField = "index";
		final String persisterField = "persister";
		final String parentUUIDField = "parentUUID";
		
		sb.append(indent(tabs));
		sb.append("public class persistObject(" + visitedClass.getSimpleName() + " " + 
				objectField + ", int " + indexField + ", " + 
				SPPersister.class.getSimpleName() + " " + persisterField + ") {\n");
		tabs++;
		
		sb.append(indent(tabs));
		sb.append("final " + String.class.getSimpleName() + " " + 
				parentUUIDField + " = null;\n");
		
		sb.append(indent(tabs));
		sb.append("if (" + objectField + ".getParent() != null) {\n");
		tabs++;
		
		sb.append(indent(tabs));
		sb.append(parentUUIDField + " = " + objectField + ".getParent().getUUID();\n");
		
		tabs--;
		sb.append(indent(tabs));
		sb.append("}\n");
		
		sb.append(indent(tabs));
		sb.append("persister.persistObject(" + parentUUIDField + ", " + 
				visitedClass.getSimpleName() + ", spo.getUUID(), " + indexField + ");\n");
		
		// TODO Code to generate all the persistProperty calls
		
		tabs--;
		sb.append(indent(tabs));
		sb.append("}\n");
		
		return sb.toString();
	}
	
}
