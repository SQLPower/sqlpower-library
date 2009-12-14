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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import ca.sqlpower.dao.AbstractSPPersisterHelper;
import ca.sqlpower.dao.PersistedSPOProperty;
import ca.sqlpower.dao.PersistedSPObject;
import ca.sqlpower.dao.PersisterUtils;
import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.dao.SPPersisterHelper;
import ca.sqlpower.dao.SPPersister.DataType;
import ca.sqlpower.dao.session.SessionPersisterSuperConverter;
import ca.sqlpower.object.SPListener;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.util.SPSession;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.Filer;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.util.DeclarationVisitors;

/**
 * This {@link AnnotationProcessor} processes the annotations in
 * {@link SPObject}s to generate {@link SPPersisterHelper} classes to be used in
 * a session {@link SPPersister}.
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
	 * annotations used in {@link SPObject}s which can generate
	 * {@link SPPersisterHelper} classes for session {@link SPPersister}s.
	 * 
	 * @param environment
	 *            The {@link AnnotationProcessorEnvironment} this processor will
	 *            work with.
	 */
	public SPAnnotationProcessor(AnnotationProcessorEnvironment environment) {
		this.environment = environment;
	}

	@SuppressWarnings("unchecked")
	public void process() {
		Map<Class<? extends SPObject>, SPClassVisitor> visitors = new HashMap<Class<? extends SPObject>, SPClassVisitor>();
		
		for (TypeDeclaration typeDecl : environment.getTypeDeclarations()) {
			SPClassVisitor visitor = new SPClassVisitor();
			typeDecl.accept(DeclarationVisitors.getDeclarationScanner(DeclarationVisitors.NO_OP, visitor));
			if (visitor.getVisitedClass() != null) {
				visitors.put(visitor.getVisitedClass(), visitor);
			}
		}
		
		generateFactoryFile(visitors.keySet());
		
		// This block checks if each of the classes has super classes that
		// contain persistable properties. If so, they should inherit those
		// persistable properties. Any additional packages should be
		// imported as well.
		for (Entry<Class<? extends SPObject>, SPClassVisitor> e : visitors.entrySet()) {
			SPClassVisitor visitor = e.getValue();
			Set<String> imports = new HashSet<String>(visitor.getImports());
			Map<String, Class<?>> propertiesToAccess = 
				new HashMap<String, Class<?>>(visitor.getPropertiesToAccess());
			Map<String, Class<?>> propertiesToMutate = 
				new HashMap<String, Class<?>>(visitor.getPropertiesToMutate());
			Class<? extends SPObject> superClass = e.getKey();
			Multimap<String, Class<? extends Exception>> mutatorThrownTypes = 
				HashMultimap.create(visitor.getMutatorThrownTypes());
			Set<String> propertiesToPersistOnlyIfNonNull = 
				new HashSet(visitor.getPropertiesToPersistOnlyIfNonNull());
			
			while ((superClass = (Class<? extends SPObject>) superClass.getSuperclass()) != null) {
				if (visitors.containsKey(superClass)) {
					SPClassVisitor superClassVisitor = visitors.get(superClass);
					
					// Add inherited imports.
					imports.addAll(superClassVisitor.getImports());
					
					// Add inherited mutator thrown types.
					mutatorThrownTypes.putAll(superClassVisitor.getMutatorThrownTypes());
					
					// Add inherited non-null only persistable properties.
					propertiesToPersistOnlyIfNonNull.addAll(
							superClassVisitor.getPropertiesToPersistOnlyIfNonNull());
					
					// Add inherited accessors.
					for (Entry<String, Class<?>> accessorEntry : superClassVisitor.getPropertiesToAccess().entrySet()) {
						if (!propertiesToAccess.containsKey(accessorEntry.getKey())) {
							propertiesToAccess.put(accessorEntry.getKey(), accessorEntry.getValue());
						}
					}
					
					// Add inherited mutators.
					for (Entry<String, Class<?>> mutatorEntry : superClassVisitor.getPropertiesToMutate().entrySet()) {
						if (!propertiesToMutate.containsKey(mutatorEntry.getKey())) {
							propertiesToMutate.put(mutatorEntry.getKey(), mutatorEntry.getValue());
						}
					}
				}
			}
			
			// Generate the persister helper file.
			generatePersisterHelperFile(e.getKey(), imports,
					visitor.getConstructorParameters(), propertiesToAccess, 
					propertiesToMutate, mutatorThrownTypes, 
					propertiesToPersistOnlyIfNonNull);
		}
	}

	/**
	 * Generates the Java source file for an {@link SPPersisterHelper} class
	 * that is to be used by a session {@link SPPersister} or workspace
	 * persister listener. This generated persister helper class should deal
	 * with creating new objects and applying persisted properties to a given
	 * {@link SPObject}, or persisting objects and properties from an
	 * {@link SPObject} to an {@link SPPersister}.
	 * 
	 * @param visitedClass
	 *            The {@link SPObject} class that is being visited by the
	 *            annotation processor.
	 * @param imports
	 *            The {@link Set} of imports that the generated persister helper
	 *            requires to have the class compile correctly. class require
	 * @param constructorParameters
	 *            The {@link Map} of constructor parameters to the property
	 *            names it delegates to.
	 * @param propertiesToAccess
	 *            The {@link Map} of getter method names of persistable
	 *            properties to its property type.
	 * @param propertiesToMutate
	 *            The {@link Map} of setter method names of persistable
	 *            properties to its property type.
	 * @param mutatorThrownTypes
	 *            The {@link Multimap} of {@link Exception}s thrown by each
	 *            persistable property setter.
	 * @param propertiesToPersistOnlyIfNonNull
	 *            The {@link Set} of persistable properties that can only be
	 *            persisted if its value is not null.
	 */
	private void generatePersisterHelperFile(
			Class<? extends SPObject> visitedClass, 
			Set<String> imports,
			Map<String, Class<?>> constructorParameters, 
			Map<String, Class<?>> propertiesToAccess, 
			Map<String, Class<?>> propertiesToMutate,
			Multimap<String, Class<? extends Exception>> mutatorThrownTypes,
			Set<String> propertiesToPersistOnlyIfNonNull) {
		try {
			int tabs = 0;
			Filer f = environment.getFiler();
			PrintWriter pw = f.createSourceFile(visitedClass.getSimpleName() + "PersisterHelper");
			
			pw.print(generateWarning());
			pw.print("\n");
			pw.print(generateLicense());
			pw.print("\n");
			pw.print(generateImports(visitedClass, imports));
			pw.print("\n");
			pw.print("public class " + visitedClass.getSimpleName() + "PersisterHelper" +
					" extends " + AbstractSPPersisterHelper.class.getSimpleName() + 
					"<" + visitedClass.getSimpleName() + "> {\n");
			tabs++;
			
			pw.print("\n");
			pw.print(generateCommitObjectMethod(visitedClass, constructorParameters, tabs));
			pw.print("\n");
			pw.print(generateCommitPropertyMethod(visitedClass, propertiesToMutate, 
					mutatorThrownTypes, tabs));
			pw.print("\n");
			pw.print(generateFindPropertyMethod(visitedClass, propertiesToAccess, tabs));
			pw.print("\n");
			pw.print(generatePersistObjectMethod(visitedClass, constructorParameters, 
					propertiesToAccess, propertiesToPersistOnlyIfNonNull, tabs));
			
			tabs--;
			pw.print("\n}\n");
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
	 * Generates and returns a warning in a comment indicating that a source
	 * file is in fact generated by this annotation processor based on
	 * annotations within {@link SPObject}s.
	 */
	private String generateWarning() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("/*\n");
		sb.append(" * This is a GENERATED class based on hand made annotations in " + 
				SPObject.class.getSimpleName() + " classes\n");
		sb.append(" * and should NOT be modified here. If you need to change this class, modify \n");
		sb.append(" * " + SPAnnotationProcessor.class.getSimpleName() + " instead.\n");
		sb.append(" */\n");
		
		return sb.toString();
	}

	/**
	 * Generates and returns the GPL license header in a comment, as Eclipse
	 * does whenever a new source file is created. The license is taken from
	 * src/license_in_comment.txt.
	 */
	private String generateLicense() {
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader("src/license_in_comment.txt"));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	/**
	 * Generates and returns source code for importing packages that are
	 * required by the persister helper this class is generating.
	 * 
	 * @param visitedClass
	 *            The {@link SPObject} class that is being visited by the
	 *            annotation processor.
	 * @param imports
	 *            The {@link Set} of packages that visitedClass uses and
	 *            need to be imported.
	 * @return The source code for the generated imports.
	 */
	private String generateImports(Class<? extends SPObject> visitedClass, Set<String> imports) {
		// Using a TreeSet here to sort imports alphabetically.
		Set<String> allImports = new TreeSet<String>(imports);
		StringBuilder sb = new StringBuilder();
		
		// XXX Need to import any additional classes this generated persister helper
		// class requires, aside from those needed in visitedClass.
		allImports.add(visitedClass.getName());
		allImports.add(AbstractSPPersisterHelper.class.getName());
		allImports.add(PersistedSPOProperty.class.getName());
		allImports.add(SPPersistenceException.class.getName());
		allImports.add(SPPersister.class.getName());
		allImports.add(SessionPersisterSuperConverter.class.getName());
		allImports.add(Collections.class.getName());
		
		for (String pkg : allImports) {
			// No need to import java.lang as it is automatically imported.
			if (!pkg.startsWith("java.lang")) {
				sb.append("import " + pkg + ";\n");
			}
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
	 *            The {@link Map} of the class property names to constructor
	 *            parameter class types. This order of this map must absolutely
	 *            be guaranteed as the order of constructor arguments requires
	 *            it.
	 * @param tabs
	 *            The number of tab characters to use to indent the generated
	 *            method.
	 * @return The source code for the generated commitObject method.
	 * @see SPPersister#persistObject(String, String, String, int)
	 */
	private String generateCommitObjectMethod(
			Class<? extends SPObject> visitedClass, 
			Map<String, Class<?>> constructorParameters, int tabs) {
		StringBuilder sb = new StringBuilder();
		final String persistedPropertiesField = "persistedProperties";
		
		// commitObject method header.
		sb.append(indent(tabs));
		sb.append("public " + visitedClass.getSimpleName() + " commitObject(" +
				Collection.class.getSimpleName() + "<" + 
				PersistedSPOProperty.class.getSimpleName() + "> " +
				persistedPropertiesField + ") {\n");
		tabs++;
		
		// Assign each constructor parameter property to a variable.
		for (Entry<String, Class<?>> e : constructorParameters.entrySet()) {
			sb.append(indent(tabs));
			sb.append(e.getValue().getSimpleName() + " " + e.getKey() + 
					" = findPropertyAndRemove(" + persistedPropertiesField + 
					", \"" + e.getKey() + "\");\n");
		}
		
		// Create and return the new object.
		sb.append(indent(tabs));
		sb.append("return new " + visitedClass.getSimpleName() + "(");
		
		boolean firstArg = true;
		
		// Pass in all of the constructor arguments.
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
	 * @param setters
	 *            The {@link Map} of property setter methods to their property
	 *            types that can be used by an {@link SPPersister} to persist
	 *            properties into an {@link SPSession}.
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
			Map<String, Class<?>> setters,
			Multimap<String, Class<? extends Exception>> mutatorThrownTypes,
			int tabs) {
		StringBuilder sb = new StringBuilder();
		final String objectField = "o";
		final String propertyNameField = "propertyName";
		final String newValueField = "newValue";
		final String converterField = "converter";
		final String exceptionField = "e";
		
		boolean firstIf = true;
		
		// commitProperty method header.
		sb.append(indent(tabs));
		sb.append("public void commitProperty(" + visitedClass.getSimpleName() + " " + 
				objectField + ", " + String.class.getSimpleName() + " " + 
				propertyNameField + ", " + Object.class.getSimpleName() + " " +
				newValueField + ", " + SessionPersisterSuperConverter.class.getSimpleName() + 
				" " + converterField + ") " + "throws " + 
				SPPersistenceException.class.getSimpleName() + " {\n");
		tabs++;
		
		// Search for the matching property name and set the value.
		for (Entry<String, Class<?>> e : setters.entrySet()) {
			
			sb.append(indent(tabs));
			
			if (!firstIf) {
				sb.append("} else ");
			}
			
			sb.append("if (" + propertyNameField + ".equals(\"" + 
					convertMethodToProperty(e.getKey()) + "\")) {\n");
			tabs++;
			
			boolean throwsExceptions = mutatorThrownTypes.containsKey(e.getKey());
			
			if (throwsExceptions) {
				sb.append(indent(tabs));
				sb.append("try {\n");
				tabs++;
			}
			
			sb.append(indent(tabs));
			sb.append(objectField + "." + e.getKey() + "((" + e.getValue().getSimpleName() + 
					") " + converterField + ".convertToComplexType(" + newValueField + 
					", " + e.getValue().getSimpleName() + ".class));\n");
			tabs--;
			
			// Catch any exceptions that the setter throws.
			if (throwsExceptions) {
				for (Class<? extends Exception> thrownType : 
					mutatorThrownTypes.get(e.getKey())) {
					
					sb.append(indent(tabs));
					sb.append("} catch (" + thrownType.getSimpleName() + " " + 
							exceptionField + ") {\n");
					tabs++;
					
					sb.append(indent(tabs));
					sb.append("throw new " + SPPersistenceException.class.getSimpleName() + 
							"(" + objectField + ".getUUID(), " +
									"createSPPersistenceExceptionMessage(" + 
									objectField + ", " + propertyNameField + ", " + 
									exceptionField + "));\n");
					tabs--;
				}
				sb.append(indent(tabs));
				sb.append("}\n");
				tabs--;
			}
			
			firstIf = false;
		}
		
		if (!firstIf) {
			sb.append(indent(tabs));
			sb.append("} else {\n");
			tabs++;
		}
		
		// Throw an SPPersistenceException if the property is not persistable or unrecognized.
		sb.append(indent(tabs));
		sb.append("throw new " + SPPersistenceException.class.getSimpleName() + 
				"(" + objectField + ".getUUID(), createSPPersistenceExceptionMessage(" + 
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
	 * Generates and returns source code for an
	 * {@link SPPersisterHelper#findProperty(SPObject, String, SessionPersisterSuperConverter)}
	 * method based on getter methods annotated with {@link Accessor} used in a
	 * given {@link SPObject} class. The purpose of this findProperty method is
	 * to allow a session {@link SPPersister} to get the value of a given
	 * property in an {@link SPObject} and compare it with the expected value.
	 * This helper method will be called by the session
	 * {@link SPPersister#persistProperty(String, String, ca.sqlpower.dao.SPPersister.DataType, Object, Object)}
	 * method.
	 * 
	 * @param visitedClass
	 *            The {@link SPObject} class that is being visited by the
	 *            annotation processor.
	 * @param getters
	 *            The {@link Map} of accessor method names to their property
	 *            types, where each property can be persisted by an
	 *            {@link SPPersister} into an {@link SPSession}.
	 * @param tabs
	 *            The number of tab characters to use to indent this generated
	 *            method block.
	 * @return The source code for the generated findProperty method.
	 * @see SPPersisterHelper#findProperty(SPObject, String,
	 *      SessionPersisterSuperConverter)
	 * @see SPPersister#persistProperty(String, String,
	 *      ca.sqlpower.dao.SPPersister.DataType, Object, Object)
	 */
	private String generateFindPropertyMethod(
			Class<? extends SPObject> visitedClass,
			Map<String, Class<?>> getters,
			int tabs) {
		StringBuilder sb = new StringBuilder();
		final String objectField = "o";
		final String propertyNameField = "propertyName";
		final String converterField = "converter";
		
		boolean firstIf = true;
		
		// findProperty method header.
		sb.append(indent(tabs));
		sb.append("public " + Object.class.getSimpleName() + " findProperty(" + 
				visitedClass.getSimpleName() + " " + objectField + ", " +
				String.class.getSimpleName() + " " + propertyNameField + ", " +
				SessionPersisterSuperConverter.class.getSimpleName() + " " + converterField + 
				") " + 
				"throws " + SPPersistenceException.class.getSimpleName() + " {\n");
		tabs++;
		
		// Search for the matching property name and return the value.
		for (Entry<String, Class<?>> e : getters.entrySet()) {
			String methodName = e.getKey();
			
			sb.append(indent(tabs));
			
			if (!firstIf) {
				sb.append("} else ");
			}
			
			sb.append("if (" + propertyNameField + ".equals(\"" + 
					convertMethodToProperty(methodName) + "\")) {\n");
			tabs++;
			
			sb.append(indent(tabs));
			sb.append("return " + converterField + ".convertToBasicType(" + 
					objectField + "." + methodName + "());\n");
			
			tabs--;
			firstIf = false;
		}
		
		if (!firstIf) {
			sb.append(indent(tabs));
			sb.append("} else {\n");
			tabs++;
		}
		
		// Throw an SPPersistenceException if the property is not persistable or unrecognized.
		sb.append(indent(tabs));
		sb.append("throw new " + SPPersistenceException.class.getSimpleName() + 
				"(" + objectField + ".getUUID(), createSPPersistenceExceptionMessage(" + 
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
	 *            The {@link Map} of property names to constructor parameters,
	 *            where values passed into the constructor will set those
	 *            respective properties. The order of this map must absolutely
	 *            be guaranteed to follow the order of the constructor
	 *            parameters. These properties may or may not be persistable, so
	 *            it is entirely possible that not all of the properties in this
	 *            map exists in the given {@link Set} of persistable properties.
	 * @param accessors
	 *            The {@link Map} of accessor method names to their property
	 *            types which should be persisted into an {@link SPPersister} by
	 *            a workspace persister {@link SPListener}.
	 * @param propertiesToPersistOnlyIfNonNull
	 *            The {@link Set} of persistable properties that can only be
	 *            persisted if its value is not null.
	 * @param tabs
	 *            The number of tab characters to use to indent this generated
	 *            method block.
	 * @return The source code for the generated persistObject method.
	 */
	private String generatePersistObjectMethod(
			Class<? extends SPObject> visitedClass,
			Map<String, Class<?>> constructorParameters,
			Map<String, Class<?>> accessors,
			Set<String> propertiesToPersistOnlyIfNonNull,
			int tabs) {
		StringBuilder sb = new StringBuilder();
		final String objectField = "o";
		final String indexField = "index";
		final String persisterField = "persister";
		final String converterField = "converter";
		final String uuidField = "uuid";
		final String parentUUIDField = "parentUUID";
		
		// persistObject method header.
		sb.append(indent(tabs));
		sb.append("public class persistObject(" + visitedClass.getSimpleName() + " " + 
				objectField + ", int " + indexField + ", " + 
				SPPersister.class.getSimpleName() + " " + persisterField + ", " +
				SessionPersisterSuperConverter.class.getSimpleName() + " " + 
				converterField + ") throws " + SPPersistenceException.class.getSimpleName() + 
				" {\n");
		tabs++;
		
		sb.append(indent(tabs));
		sb.append("final " + String.class.getSimpleName() + " " + uuidField + " = " + 
				objectField + ".getUUID();\n");
		
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
		sb.append("}\n\n");
		
		// Persist the object.
		sb.append(indent(tabs));
		sb.append(persisterField + ".persistObject(" + parentUUIDField + ", \"" + 
				visitedClass.getSimpleName() + "\", " + uuidField + ", " + 
				indexField + ");\n\n");
		
		sb.append(indent(tabs));
		if (constructorParameters.isEmpty()) {
			sb.append("// No constructor arguments\n");
		} else {
			sb.append("// Constructor arguments\n");
		}
		
		// Persist all of its constructor argument properties.
		for (Entry<String, Class<?>> e : constructorParameters.entrySet()) {
			sb.append(indent(tabs));
			sb.append(persisterField + ".persistProperty(" + uuidField + ", \"" + 
					e.getKey() + "\", " + DataType.class.getSimpleName() + "." + 
					PersisterUtils.getDataType(e.getValue()).name() + 
					", " + converterField + ".convertToBasicType(" + objectField + "." +
					convertPropertyToAccessor(e.getKey(), e.getValue()) + "()));\n");
		}
		sb.append("\n");
		
		boolean lastEntryInIfBlock = false;
		
		sb.append(indent(tabs));
		if (accessors.isEmpty()) {
			sb.append("// No remaining properties\n");
		} else {
			sb.append("// Remaining properties\n");
		}
		
		// Persist all of its persistable properties.
		for (Entry<String, Class<?>> e : accessors.entrySet()) {
			String propertyName = convertMethodToProperty(e.getKey());
			
			// Persist the property only if it is not null.
			if (!constructorParameters.containsKey(propertyName)) {
				boolean persistOnlyIfNonNull = 
					propertiesToPersistOnlyIfNonNull.contains(propertyName);
				String propertyField = objectField + "." + e.getKey() + "()";
				
				if (lastEntryInIfBlock) {
					sb.append("\n");
				}
				
				if (persistOnlyIfNonNull) {
					sb.append(indent(tabs));
					sb.append(e.getValue().getSimpleName() + " " + propertyName + 
							" = " + propertyField + ";\n");
					propertyField = propertyName;
					
					sb.append(indent(tabs));
					sb.append("if (" + propertyField + " != null) {\n");
					tabs++;
				}
				
				sb.append(indent(tabs));
				sb.append(persisterField + ".persistProperty(" + uuidField + ", \"" +
						propertyName + "\", " + DataType.class.getSimpleName() + "." + 
						PersisterUtils.getDataType(e.getValue()).name() +
						", " + converterField + ".convertToBasicType(" + objectField + "." +
						e.getKey() + "()));\n");
				
				if (persistOnlyIfNonNull) {
					tabs--;
					sb.append(indent(tabs));
					sb.append("}\n");
					lastEntryInIfBlock = true;
				} else {
					lastEntryInIfBlock = false;
				}
			}
		}
		
		tabs--;
		sb.append(indent(tabs));
		sb.append("}\n");
		
		return sb.toString();
	}

	/**
	 * Creates an {@link SPPersisterHelper} factory source file. This factory
	 * creates instances of every {@link SPPersisterHelper} based on the
	 * annotated {@link Persistable} {@link SPObject} classes. The commitObject,
	 * commitProperty, findProperty and persistObject methods of this factory
	 * call the respective methods in the appropriate {@link SPPersisterHelper}
	 * given a certain {@link SPObject} class.
	 * 
	 * @param persistableClasses
	 *            The {@link Set} of {@link SPObject} classes annotated with
	 *            {@link Persistable} which will be used to generate code
	 *            specific to each of their respective {@link SPPersisterHelper}s.
	 */
	private void generateFactoryFile(Set<Class<? extends SPObject>> persistableClasses) {
		try {
			final String factoryName = "SPPersisterHelperFactory";
			final String persisterField = "persister";
			final String converterField = "converter";
			final String getHelperMethodName = "getSPPersisterHelper";
			int tabs = 0;
			
			Filer f = environment.getFiler();
			PrintWriter pw = f.createSourceFile(factoryName);
			pw.print(generateWarning());
			pw.print("\n");
			pw.print(generateLicense());
			pw.print("\n");
			pw.print("public class " + factoryName + " {\n\n");
			tabs++;
			
			pw.print(generateFactoryFields(persisterField, converterField, persistableClasses, 
					tabs));
			pw.print("\n");
			pw.print(generateFactoryConstructor(factoryName, persisterField, 
					converterField, tabs));
			pw.print("\n");
			pw.print(generateFactoryGetHelperMethod(getHelperMethodName, persistableClasses, tabs));
			pw.print("\n");
			pw.print(generateFactoryCommitObjectMethod(getHelperMethodName, tabs));
			pw.print("\n");
			pw.print(generateFactoryCommitPropertyMethod(converterField, getHelperMethodName, tabs));
			pw.print("\n");
			pw.print(generateFactoryFindPropertyMethod(converterField, getHelperMethodName, tabs));
			pw.print("\n");
			pw.print(generateFactoryPersistObjectMethod(persisterField, converterField, 
					getHelperMethodName, tabs));
			
			tabs--;
			pw.print("}\n");
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Generates and returns source code for the persister helper declarations
	 * and initializations, and the {@link SPPersister} and
	 * {@link SessionPersisterSuperConverter} field declarations.
	 * 
	 * @param persisterField
	 *            The name of the {@link SPPersister} field.
	 * @param converterField
	 *            The name of the {@link SessionPersisterSuperConverter} field.
	 * @param persistableClasses
	 *            The {@link Set} of {@link SPObject}s that are annotated with
	 *            {@link Persistable}.
	 * @param tabs
	 *            The number of tab characters to use to indent the code.
	 * @return The source code for the generated field declarations and
	 *         initializations.
	 */
	private String generateFactoryFields(String persisterField, String converterField,
			Set<Class<? extends SPObject>> persistableClasses, int tabs) {
		StringBuilder sb = new StringBuilder();
		
		for (Class<? extends SPObject> clazz : persistableClasses) {
			sb.append(indent(tabs));
			sb.append("private final " + clazz.getSimpleName() + "PersisterHelper " + 
					convertClassToFieldName(clazz) + " = new " + clazz.getSimpleName() + 
					"PersisterHelper();\n\n");
		}
		
		sb.append(indent(tabs));
		sb.append("private final " + SPPersister.class.getSimpleName() + " " + 
				persisterField + ";\n\n");
		
		sb.append(indent(tabs));
		sb.append("private final " + SessionPersisterSuperConverter.class.getSimpleName() + 
				" " + converterField + ";\n");
		
		return sb.toString();
	}

	/**
	 * Generates and returns source code for the SPPersisterHelper factory
	 * constructor, which initializes the {@link SPPersister} and
	 * {@link SessionPersisterSuperConverter} fields to be passed into the
	 * methods of {@link SPPersisterHelper}.
	 * 
	 * @param factoryName
	 *            The class name of the SPPersisterHelper factory
	 * @param persisterField
	 *            The {@link SPPersister} to use for persisting objects.
	 * @param converterField
	 *            The {@link SessionPersisterSuperConverter} to use for
	 *            converting between simple and complex types.
	 * @param tabs
	 *            The number of tab characters to use to indent this constructor
	 *            block.
	 * @return The source code for the generated constructor.
	 */
	private String generateFactoryConstructor(
			final String factoryName, 
			final String persisterField, 
			final String converterField, 
			int tabs) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(indent(tabs));
		sb.append("public " + factoryName + "(" + SPPersister.class.getSimpleName() + 
				" " + persisterField + ", " + 
				SessionPersisterSuperConverter.class.getSimpleName() + " " + 
				converterField + ") {\n");
		tabs++;
		
		sb.append(indent(tabs));
		sb.append("this." + persisterField + " = " + persisterField + ";\n");
		
		sb.append(indent(tabs));
		sb.append("this." + converterField + " = " + converterField + ";\n");
		
		tabs--;
		sb.append(indent(tabs));
		sb.append("}\n");
		
		return sb.toString();
	}

	/**
	 * Generates and returns source code for a method for the
	 * {@link SPPersisterHelper} factory that returns a specific
	 * {@link SPPersisterHelper} based on the given {@link SPObject} class type.
	 * 
	 * @param getHelperMethodName
	 *            The name of the method to generate.
	 * @param persistableClasses
	 *            The {@link Set} of {@link SPObject} classes that are annotated
	 *            with {@link Persistable}.
	 * @param tabs
	 *            The number of tab characters to use to indent this method
	 *            block.
	 * @return The source code for the generated method.
	 */
	private String generateFactoryGetHelperMethod(
			final String getHelperMethodName,
			Set<Class<? extends SPObject>> persistableClasses, 
			int tabs) {
		StringBuilder sb = new StringBuilder();
		boolean firstIf = true;
		final String classTypeField = "clazz";
		
		sb.append(indent(tabs));
		sb.append("private " + SPPersisterHelper.class.getSimpleName() + " " + 
				getHelperMethodName + "(" + Class.class.getSimpleName() + 
				"<? extends " + SPObject.class.getSimpleName() + "> " + classTypeField + ") {\n");
		tabs++;
		
		for (Class<? extends SPObject> clazz : persistableClasses) {
			sb.append(indent(tabs));
			
			if (!firstIf) {
				sb.append("} else ");
			}
			
			sb.append("if (" + classTypeField + " == " + clazz.getSimpleName() + ".class" + 
					") {\n");
			tabs++;
			
			sb.append(indent(tabs));
			sb.append("return " + convertClassToFieldName(clazz) + ";\n");

			tabs--;
			
			firstIf = false;
		}
		
		if (!firstIf) {
			sb.append(indent(tabs));
			sb.append("} else {\n");
			tabs++;
		}
		
		sb.append(indent(tabs));
		sb.append("throw new " + IllegalArgumentException.class.getSimpleName() + 
				"(\"There are no " + SPPersisterHelper.class.getSimpleName() + "s " +
						"that deal with " + SPObject.class.getSimpleName() + "s of type " +
								"\" + " + classTypeField + " + \".\");\n");
		
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
	 * Generates and returns source code for the {@link SPPersisterHelper}
	 * factory commitObject method. This method determines which
	 * {@link SPPersisterHelper} to use based on the given {@link SPObject}
	 * class and calls the {@link SPPersisterHelper#commitObject(Collection)}
	 * method of that helper.
	 * 
	 * @param getHelperMethodName
	 *            The name of the method to use to return the appropriate
	 *            {@link SPPersisterHelper} based on the given {@link SPObject}
	 *            class.
	 * @param tabs
	 *            The number of tab characters to use to indent this method
	 *            block.
	 * @return The source code for this generated commitObject method.
	 * @see SPPersisterHelper#commitObject(Collection)
	 */
	private String generateFactoryCommitObjectMethod(
			final String getHelperMethodName, 
			int tabs) {
		StringBuilder sb = new StringBuilder();
		
		final String classTypeField = "clazz";
		final String persistedPropertiesField = "persistedProperties";
		final String helperField = "helper";
		
		sb.append(indent(tabs));
		sb.append("public void commitObject(" + Class.class.getSimpleName() + 
				"<? extends " + SPObject.class.getSimpleName() + "> " + 
				classTypeField + ", "+ Collection.class.getSimpleName() + 
				"<" + PersistedSPObject.class.getSimpleName() + "> " + 
				SPObject.class.getSimpleName() + " " + persistedPropertiesField + ") {\n");
		tabs++;
		
		sb.append(indent(tabs));
		sb.append(SPPersisterHelper.class.getSimpleName() + " " + helperField + " = " + 
				getHelperMethodName + "(" + classTypeField + ");\n");
		
		sb.append(indent(tabs));
		sb.append(helperField + ".commitObject(" + persistedPropertiesField + ");\n");
		
		tabs--;
		sb.append(indent(tabs));
		sb.append("}\n");
		
		return sb.toString();
	}

	/**
	 * Generates and returns source code for the {@link SPPersisterHelper}
	 * factory commitProperty method. This method determines which
	 * {@link SPPersisterHelper} to use based on the type of {@link SPObject}
	 * passed in and calls the
	 * {@link SPPersisterHelper#commitProperty(SPObject, String, Object, SessionPersisterSuperConverter)}
	 * method of that helper.
	 * 
	 * @param converterField
	 *            The name of the {@link SessionPersisterSuperConverter} field
	 *            to pass into the commitProperty method of the helper.
	 * @param getHelperMethodName
	 *            The name of the method to use to return the appropriate
	 *            {@link SPPersisterHelper} based on the given {@link SPObject}
	 *            class.
	 * @param tabs
	 *            The number of tab characters to use to indent this method
	 *            block.
	 * @return The source code for the generated method.
	 * @see SPPersisterHelper#commitProperty(SPObject, String, Object,
	 *      SessionPersisterSuperConverter)
	 */
	private String generateFactoryCommitPropertyMethod(
			final String converterField, 
			final String getHelperMethodName, 
			int tabs) {
		StringBuilder sb = new StringBuilder();
		
		final String objectField = "spo";
		final String helperField = "helper";
		final String propertyNameField = "propertyName";
		final String newValueField = "newValue";
		
		sb.append(indent(tabs));
		sb.append("public void commitProperty(" + SPObject.class.getSimpleName() + " " + objectField + ", " + 
				String.class.getSimpleName() + " " + propertyNameField + ", " + 
				Object.class.getSimpleName() + " " + newValueField + ", " + 
				SessionPersisterSuperConverter.class.getSimpleName() + " " + converterField + 
				") throws " + SPPersistenceException.class.getSimpleName() + " {\n");
		tabs++;
		
		sb.append(indent(tabs));
		sb.append(SPPersisterHelper.class.getSimpleName() + " " + helperField + " = " + 
				getHelperMethodName + "(" + objectField + ".class);\n");
		
		sb.append(indent(tabs));
		sb.append(helperField + ".commitProperty(" + objectField + ", " + propertyNameField + ", " + newValueField + ", " + converterField + ");\n");
		
		tabs--;
		sb.append(indent(tabs));
		sb.append("}\n");
		
		return sb.toString();
	}

	/**
	 * Generates and returns source code for the {@link SPPersisterHelper}
	 * factory findProperty method. This method determines which
	 * {@link SPPersisterHelper} to use based on the given {@link SPObject}
	 * class and calls the
	 * {@link SPPersisterHelper#findProperty(SPObject, String, SessionPersisterSuperConverter)}
	 * method of that helper.
	 * 
	 * @param converterField
	 *            The name of the {@link SessionPersisterSuperConverter} field
	 *            to pass into the findProperty method of the helper.
	 * @param getHelperMethodName
	 *            The name of the method to use to return the appropriate
	 *            {@link SPPersisterHelper} based on the given {@link SPObject}
	 *            class.
	 * @param tabs
	 *            The number of tab characters to use to indent this method
	 *            block.
	 * @return The source code for the generated method.
	 * @see SPPersisterHelper#findProperty(SPObject, String,
	 *      SessionPersisterSuperConverter)
	 */
	private String generateFactoryFindPropertyMethod(
			final String converterField, 
			final String getHelperMethodName, 
			int tabs) {
		StringBuilder sb = new StringBuilder();
		
		final String objectField = "spo";
		final String helperField = "helper";
		final String propertyNameField = "propertyName";
		
		sb.append(indent(tabs));
		sb.append("public void findProperty(" + SPObject.class.getSimpleName() + " " + objectField + ", " + 
				String.class.getSimpleName() + " " + propertyNameField + 
				SessionPersisterSuperConverter.class.getSimpleName() + " " + converterField + 
				") throws " + SPPersistenceException.class.getSimpleName() + " {\n");
		tabs++;
		
		sb.append(indent(tabs));
		sb.append(SPPersisterHelper.class.getSimpleName() + " " + helperField + " = " + 
				getHelperMethodName + "(" + objectField + ".class);\n");
		
		sb.append(indent(tabs));
		sb.append(helperField + ".findProperty(" + objectField + ", " + propertyNameField + ", " + converterField + ");\n");
		
		tabs--;
		sb.append(indent(tabs));
		sb.append("}\n");
		
		return sb.toString();
	}

	/**
	 * Generates and returns source code for the {@link SPPersisterHelper}
	 * factory persistObject method. This method determines which
	 * {@link SPPersisterHelper} to use based on the type of {@link SPObject}
	 * passed in and calls the
	 * {@link SPPersisterHelper#persistObject(SPObject, int, SPPersister, SessionPersisterSuperConverter)}
	 * method of that helper.
	 * 
	 * @param persisterField
	 *            The name of the {@link SPPersister} field to pass into the
	 *            persistObject method of the helper.
	 * @param converterField
	 *            The name of the {@link SessionPersisterSuperConverter} field
	 *            to pass into the persistObject method of the helper.
	 * @param getHelperMethodName
	 *            The name of the method to use to return the appropriate
	 *            {@link SPPersisterHelper} based on the given {@link SPObject}
	 *            class.
	 * @param tabs
	 *            The number of tab characters to use to indent this method
	 *            block.
	 * @return The source code for the generated persistObject method.
	 * @see SPPersisterHelper#persistObject(SPObject, int, SPPersister,
	 *      SessionPersisterSuperConverter)
	 */
	private String generateFactoryPersistObjectMethod(
			final String persisterField, 
			final String converterField, 
			String getHelperMethodName, 
			int tabs) {
		StringBuilder sb = new StringBuilder();
		
		final String objectField = "spo";
		final String indexField = "index";
		final String helperField = "helper";
		sb.append(indent(tabs));
		sb.append("public void persistObject(" + SPObject.class.getSimpleName() + " " + 
				objectField + ", int " + indexField + ") {\n");
		tabs++;
		
		sb.append(indent(tabs));
		sb.append(SPPersisterHelper.class.getSimpleName() + " " + helperField + " = " + getHelperMethodName + "(" + objectField + ".class);\n");
		
		sb.append(indent(tabs));
		sb.append(helperField + ".persistObject(" + objectField + ", " + indexField + 
				", " + persisterField + ", " + converterField + ");\n");
		
		tabs--;
		sb.append(indent(tabs));
		sb.append("}\n");
		
		return sb.toString();
	}
	
	/**
	 * Converts a JavaBean formatted method name to a property name.
	 * 
	 * @param methodName
	 *            The JavaBean formatted method name. This method name must
	 *            start with either "is", "get", or "set" and there must be
	 *            characters following after it.
	 * @return The converted property name.
	 * @throws IllegalArgumentException
	 *             Thrown if the given method name does not follow the JavaBean
	 *             format.
	 */
	public static String convertMethodToProperty(String methodName) throws IllegalArgumentException {
		String propertyName;
		if (methodName.length() > 2 && methodName.startsWith("is")) {
			propertyName = methodName.substring(2);
		} else if (methodName.length() > 3 && 
				(methodName.startsWith("get") || methodName.startsWith("set"))) {
			propertyName = methodName.substring(3);
		} else {
			throw new IllegalArgumentException("Cannot convert method name \"" + 
					methodName + "\" to property name as it is not a valid JavaBean name.");
		}
		
		if (!propertyName.equals("UUID")) {
			propertyName = propertyName.substring(0, 1).toLowerCase() + 
					((propertyName.length() > 1)? propertyName.substring(1) : "");
		}
		
		return propertyName;
	}

	/**
	 * Converts a property name to its JavaBean getter method name depending on
	 * what type the property is.
	 * 
	 * @param propertyName
	 *            The property name to convert to its JavaBean getter method
	 *            name.
	 * @param type
	 *            The property type.
	 * @return The JavaBean getter method name. If the property type is a
	 *         {@link Boolean} value, than the getter method name is prefixed
	 *         with "is". Otherwise, it is prefixed with "get".
	 */
	public static String convertPropertyToAccessor(String propertyName, Class<?> type) {
		String prefix;
		if (type == Boolean.class) {
			prefix = "is";
		} else {
			prefix = "get";
		}
		
		return prefix + propertyName.substring(0, 1).toUpperCase() + 
				((propertyName.length() > 1)? propertyName.substring(1) : "");
	}

	/**
	 * Converts an {@link SPObject} class to a suitable
	 * {@link SPPersisterHelper} field name. This is essentially the simple name
	 * of the class but prepended with "helperFor".
	 * 
	 * @param clazz
	 *            The {@link SPObject} class to convert.
	 * @return The field name corresponding to the given {@link SPObject} class.
	 */
	public static String convertClassToFieldName(Class<? extends SPObject> clazz) {
		return "helperFor" + clazz.getSimpleName();
	}
	
}
