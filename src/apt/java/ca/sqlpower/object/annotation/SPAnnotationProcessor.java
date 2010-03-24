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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import ca.sqlpower.dao.PersistedSPOProperty;
import ca.sqlpower.dao.PersistedSPObject;
import ca.sqlpower.dao.PersisterUtils;
import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.dao.SPPersister.DataType;
import ca.sqlpower.dao.helper.AbstractSPPersisterHelper;
import ca.sqlpower.dao.helper.PersisterHelperFinder;
import ca.sqlpower.dao.helper.SPPersisterHelper;
import ca.sqlpower.dao.session.SessionPersisterSuperConverter;
import ca.sqlpower.object.SPListener;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.ConstructorParameter.ParameterType;
import ca.sqlpower.util.SPSession;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
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

	private final static String LICENSE_COMMENT_FILE_PATH = "src/main/resources/license_in_comment.txt";
	/**
	 * The {@link AnnotationProcessorEnvironment} this
	 * {@link AnnotationProcessor} will work with. The environment will give
	 * useful information about classes annotated with {@link Persistable}.
	 */
	private final AnnotationProcessorEnvironment environment;
	
	/**
	 * This contains the additional fully qualified class names that need to be
	 * imported into the persister for the persister helper methods. This set
	 * will be cleared at the start of creating each file.
	 */
	private final Set<String> importedClassNames = new HashSet<String>();

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

	public void process() {
		Map<Class<? extends SPObject>, SPClassVisitor> visitors = new HashMap<Class<? extends SPObject>, SPClassVisitor>();
		
		for (TypeDeclaration typeDecl : environment.getTypeDeclarations()) {
			SPClassVisitor visitor = new SPClassVisitor(typeDecl);
			typeDecl.accept(DeclarationVisitors.getDeclarationScanner(DeclarationVisitors.NO_OP, visitor));
			if (visitor.isValid() && visitor.getVisitedClass() != null) {
				visitors.put(visitor.getVisitedClass(), visitor);
			}
		}
		
		// This block checks if each of the classes has super classes that
		// contain persistable properties. If so, they should inherit those
		// persistable properties. Any additional packages should be
		// imported as well.
		for (Entry<Class<? extends SPObject>, SPClassVisitor> e : visitors.entrySet()) {
			Class<? extends SPObject> superClass = e.getKey();
			SPClassVisitor visitor = e.getValue();
			
			Multimap<String, String> mutatorImports = HashMultimap.create(visitor.getMutatorImports());
			Map<String, Class<?>> propertiesToAccess = 
				new HashMap<String, Class<?>>(visitor.getPropertiesToAccess());
			Multimap<String, String> accessorAdditionalInfo = 
				LinkedHashMultimap.create(visitor.getAccessorAdditionalInfo());
			Map<String, Class<?>> propertiesToMutate = 
				new HashMap<String, Class<?>>(visitor.getPropertiesToMutate());
			Multimap<String, MutatorParameterObject> mutatorExtraParameters = 
				LinkedHashMultimap.create(visitor.getMutatorExtraParameters());
			Multimap<String, Class<? extends Exception>> mutatorThrownTypes = 
				HashMultimap.create(visitor.getMutatorThrownTypes());
			Set<String> propertiesToPersistOnlyIfNonNull = 
				new HashSet<String>(visitor.getPropertiesToPersistOnlyIfNonNull());
			
			importedClassNames.clear();
			
			// Generate the persister helper file if the SPObject class is not abstract.
			if (!Modifier.isAbstract(visitor.getVisitedClass().getModifiers())) {
				generatePersisterHelperFile(
						e.getKey(),
						visitor.getConstructorImports(),
						visitor.getConstructorParameters(), 
						propertiesToAccess, 
						accessorAdditionalInfo,
						mutatorImports,
						propertiesToMutate, 
						mutatorExtraParameters, 
						mutatorThrownTypes, 
						propertiesToPersistOnlyIfNonNull);
			} else {
				generateAbstractPersisterHelperFile(e.getKey(),
						visitor.getConstructorImports(),
						propertiesToAccess, 
						accessorAdditionalInfo,
						mutatorImports,
						propertiesToMutate, 
						mutatorExtraParameters, 
						mutatorThrownTypes, 
						propertiesToPersistOnlyIfNonNull);
			}
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
	 * @param constructorImports
	 *            The {@link Set} of imports that the generated persister helper
	 *            requires for calling the {@link Constructor} annotated
	 *            constructor.
	 * @param constructorParameters
	 *            The {@link List} of {@link ConstructorParameterObject}s that
	 *            contain information about what the parameter should be used
	 *            for.
	 * @param propertiesToAccess
	 *            The {@link Map} of getter method names of persistable
	 *            properties to its property type.
	 * @param accessorAdditionalInfo
	 *            The {@link Multimap} of getter methods mapped to additional
	 *            properties a session {@link SPPersister} requires to convert
	 *            the getter's returned value from a complex to basic
	 *            persistable type.
	 * @param mutatorImports
	 *            The {@link Multimap} of setter methods to imports that the
	 *            generated persister helper requires for calling the
	 *            {@link Mutator} annotated setters.
	 * @param propertiesToMutate
	 *            The {@link Map} of setter method names of persistable
	 *            properties to its property type.
	 * @param mutatorExtraParameters
	 *            The {@link Multimap} of setter methods mapped to each of its
	 *            extra parameters (second parameter and onwards).
	 * @param mutatorThrownTypes
	 *            The {@link Multimap} of {@link Exception}s thrown by each
	 *            persistable property setter.
	 * @param propertiesToPersistOnlyIfNonNull
	 *            The {@link Set} of persistable properties that can only be
	 *            persisted if its value is not null.
	 */
	private void generatePersisterHelperFile(
			Class<? extends SPObject> visitedClass, 
			Set<String> constructorImports,
			List<ConstructorParameterObject> constructorParameters,
			Map<String, Class<?>> propertiesToAccess, 
			Multimap<String, String> accessorAdditionalInfo,
			Multimap<String, String> mutatorImports,
			Map<String, Class<?>> propertiesToMutate,
			Multimap<String, MutatorParameterObject> mutatorExtraParameters,
			Multimap<String, Class<? extends Exception>> mutatorThrownTypes,
			Set<String> propertiesToPersistOnlyIfNonNull) {
		try {
			final String helperPackage = visitedClass.getPackage().getName() + "." + PersisterHelperFinder.GENERATED_PACKAGE_NAME;
			int tabs = 0;
			
			Filer f = environment.getFiler();
			PrintWriter pw = f.createSourceFile(helperPackage + "." + 
					visitedClass.getSimpleName() + "PersisterHelper");
			
			tabs++;
			final String commitObjectMethod = generateCommitObjectMethod(visitedClass, constructorParameters, tabs);
			final String commitPropertyMethod = generateCommitPropertyMethod(visitedClass, propertiesToMutate, 
					mutatorExtraParameters, mutatorThrownTypes, tabs);
			final String findPropertyMethod = generateFindPropertyMethod(visitedClass, propertiesToAccess, 
					accessorAdditionalInfo, tabs);
			final String persistObjectMethod = generatePersistObjectMethod(visitedClass, constructorParameters, 
					propertiesToAccess, propertiesToPersistOnlyIfNonNull, tabs);
			final String PersistObjectMethodHelper = generatePersistObjectMethodHelper(visitedClass, 
					propertiesToAccess, propertiesToPersistOnlyIfNonNull, tabs);
			// -
			final String getPersistedPropertiesMethod = generateGetPersistedPropertyListMethod(visitedClass, propertiesToMutate, tabs);
			importedClassNames.add(AbstractSPPersisterHelper.class.getName());
			tabs--;
			final String imports = generateImports(visitedClass, constructorImports, mutatorImports);
			
			pw.print(generateWarning());
			pw.print("\n");
			pw.print(generateLicense());
			pw.print("\n");
			pw.print("package " + helperPackage + ";\n");
			pw.print("\n");
			pw.print(imports);
			pw.print("\n");
			pw.print("public class " + visitedClass.getSimpleName() + "PersisterHelper" +
					" extends " + AbstractSPPersisterHelper.class.getSimpleName() + 
					"<" + visitedClass.getSimpleName() + "> {\n");
			
			pw.print("\n");
			pw.print(commitObjectMethod);
			pw.print("\n");
			pw.print(commitPropertyMethod);
			pw.print("\n");
			pw.print(findPropertyMethod);
			pw.print("\n");
			pw.print(persistObjectMethod);
			pw.print("\n");
			pw.print(PersistObjectMethodHelper);
			pw.print("\n");
			pw.print(getPersistedPropertiesMethod);
			pw.print("\n");
			
			pw.print("}\n");
			pw.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Generates the Java source file that is a helper of {@link SPPersisterHelper}.
	 * These helpers are not true {@link SPPersisterHelper}s because they only do
	 * part of a helper's job. They also do not implement the interface because nothing
	 * outside of the {@link SPPersisterHelper}s should be using them directly.
	 * 
	 * @param visitedClass
	 *            The {@link SPObject} class that is being visited by the
	 *            annotation processor.
	 * @param constructorImports
	 *            The {@link Set} of imports that the generated persister helper
	 *            requires for calling the {@link Constructor} annotated
	 *            constructor.
	 * @param constructorParameters
	 *            The {@link List} of {@link ConstructorParameterObject}s that
	 *            contain information about what the parameter should be used
	 *            for.
	 * @param propertiesToAccess
	 *            The {@link Map} of getter method names of persistable
	 *            properties to its property type.
	 * @param accessorAdditionalInfo
	 *            The {@link Multimap} of getter methods mapped to additional
	 *            properties a session {@link SPPersister} requires to convert
	 *            the getter's returned value from a complex to basic
	 *            persistable type.
	 * @param mutatorImports
	 *            The {@link Multimap} of setter methods to imports that the
	 *            generated persister helper requires for calling the
	 *            {@link Mutator} annotated setters.
	 * @param propertiesToMutate
	 *            The {@link Map} of setter method names of persistable
	 *            properties to its property type.
	 * @param mutatorExtraParameters
	 *            The {@link Multimap} of setter methods mapped to each of its
	 *            extra parameters (second parameter and onwards).
	 * @param mutatorThrownTypes
	 *            The {@link Multimap} of {@link Exception}s thrown by each
	 *            persistable property setter.
	 * @param propertiesToPersistOnlyIfNonNull
	 *            The {@link Set} of persistable properties that can only be
	 *            persisted if its value is not null.
	 */
	private void generateAbstractPersisterHelperFile(
			Class<? extends SPObject> visitedClass, 
			Set<String> constructorImports,
			Map<String, Class<?>> propertiesToAccess, 
			Multimap<String, String> accessorAdditionalInfo,
			Multimap<String, String> mutatorImports,
			Map<String, Class<?>> propertiesToMutate,
			Multimap<String, MutatorParameterObject> mutatorExtraParameters,
			Multimap<String, Class<? extends Exception>> mutatorThrownTypes,
			Set<String> propertiesToPersistOnlyIfNonNull) {
		try {
			final String helperPackage = visitedClass.getPackage().getName() + "." + PersisterHelperFinder.GENERATED_PACKAGE_NAME;
			int tabs = 0;
			
			Filer f = environment.getFiler();
			PrintWriter pw = f.createSourceFile(helperPackage + "." + 
					visitedClass.getSimpleName() + "PersisterHelper");
			tabs++;
			final String commitPropertyMethod = generateCommitPropertyMethod(visitedClass, propertiesToMutate, 
					mutatorExtraParameters, mutatorThrownTypes, tabs);
			final String findPropertyMethod = generateFindPropertyMethod(visitedClass, propertiesToAccess, 
					accessorAdditionalInfo, tabs);
			final String persistObjectMethodHelper = generatePersistObjectMethodHelper(visitedClass, 
					propertiesToAccess, propertiesToPersistOnlyIfNonNull, tabs);
			final String getPersistedPropertiesMethod = generateGetPersistedPropertyListMethod(visitedClass, propertiesToMutate, tabs);
			final String generateImports = generateImports(visitedClass, constructorImports, mutatorImports);
			tabs--;
			pw.print(generateWarning());
			pw.print("\n");
			pw.print(generateLicense());
			pw.print("\n");
			pw.print("package " + helperPackage + ";\n");
			pw.print("\n");
			pw.print(generateImports);
			pw.print("\n");
			pw.print("public class " + visitedClass.getSimpleName() + "PersisterHelper {\n");
			
			pw.print("\n");
			pw.print(commitPropertyMethod);
			pw.print("\n");
			pw.print(findPropertyMethod);
			pw.print("\n");
			pw.print(persistObjectMethodHelper);
			pw.print("\n");
			pw.print(getPersistedPropertiesMethod);
			pw.print("\n");
			
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
			FileReader fr = new FileReader(LICENSE_COMMENT_FILE_PATH);
			BufferedReader br = new BufferedReader(fr);
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}
			fr.close();
			br.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
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
	 * @param constructorImports
	 *            The {@link Set} of packages that visitedClass uses in its
	 *            {@link Constructor} annotated constructor and need to be
	 *            imported.
	 * @param mutatorImports
	 *            The {@link Multimap} of setter methods to packages that
	 *            visitedClass uses in its {@link Mutator} annotated methods and
	 *            needs to be imported.
	 * @return The source code for the generated imports.
	 */
	private String generateImports(
			Class<? extends SPObject> visitedClass, 
			Set<String> constructorImports,
			Multimap<String, String> mutatorImports) {
		// Using a TreeSet here to sort imports alphabetically.
		Set<String> allImports = new TreeSet<String>();
		allImports.addAll(constructorImports);
		allImports.addAll(mutatorImports.values());
		
		StringBuilder sb = new StringBuilder();
		
		// XXX Need to import any additional classes this generated persister helper
		// class requires, aside from those needed in visitedClass.
		allImports.add(List.class.getName());
		allImports.add(visitedClass.getName());
		allImports.add(SPPersistenceException.class.getName());
		allImports.add(SPPersister.class.getName());
		allImports.add(SessionPersisterSuperConverter.class.getName());
		allImports.add(SPObject.class.getName());
		allImports.add(DataType.class.getName());
		allImports.addAll(importedClassNames);
		
		for (String pkg : allImports) {
			// No need to import java.lang as it is automatically imported.
			if (!pkg.startsWith("java.lang")) {
				// Nested classes, enums, etc. will be separated by the "$"
				// character but we need to change them to "." so it can be
				// imported correctly.
				sb.append("import " + pkg.replaceAll("\\$", ".") + ";\n");
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
			List<ConstructorParameterObject> constructorParameters, 
			int tabs) {
		StringBuilder sb = new StringBuilder();
		final String objectField = "o";
		final String persistedPropertiesField = "persistedProperties";
		final String persistedObjectField = "pso";
		final String persistedObjectsListField = "persistedObjects";
		final String converterField = "converter";
		final String uuidField = "uuid";
		final String childPersistedObjectField = "childPSO";
		
		// commitObject method header.
		sb.append(indent(tabs));
		sb.append("public " + visitedClass.getSimpleName() + " commitObject(" +
				PersistedSPObject.class.getSimpleName() + " " + persistedObjectField + ", " +
				Multimap.class.getSimpleName() + "<" + String.class.getSimpleName() + ", " + 
						PersistedSPOProperty.class.getSimpleName() + "> " + 
						persistedPropertiesField + ", " +
				List.class.getSimpleName() + "<" + 
						PersistedSPObject.class.getSimpleName() + "> " + persistedObjectsListField + ", " +
				SessionPersisterSuperConverter.class.getSimpleName() + " " + converterField + ") throws SPPersistenceException {\n");
		importedClassNames.add(PersistedSPObject.class.getName());
		importedClassNames.add(PersistedSPOProperty.class.getName());
		importedClassNames.add(Multimap.class.getName());
		tabs++;
		
		sb.append(indent(tabs));
		sb.append(String.class.getSimpleName() + " " + uuidField + " = " + 
				persistedObjectField + ".getUUID();\n");
		
		sb.append(indent(tabs));
		sb.append(PersistedSPObject.class.getSimpleName() + " " + 
				childPersistedObjectField + " = null;\n");
		
		sb.append(indent(tabs));
		
		sb.append("\n");
		
		// Assign each constructor parameter property to a variable.
		final String parameterTypeField = "parameterType";
		println(sb, tabs, PersistedSPOProperty.class.getSimpleName() + " " + parameterTypeField + ";");
		String classToLoadField = "classToLoad";
        println(sb, tabs, "Class<? extends SPObject> " + classToLoadField + ";");
		for (ConstructorParameterObject cpo : constructorParameters) {
			sb.append(indent(tabs));
			
			String parameterType = cpo.getType().getSimpleName();
			String parameterName = cpo.getName();

			if (ParameterType.PROPERTY.equals(cpo.getProperty())) {
				String parameterTypeClass;
				if (cpo.getType() == Object.class) {
					println(sb, 0, parameterTypeField + " = findProperty(" + uuidField + 
							", \"" + parameterName + "\", " + persistedPropertiesField + ");");
					parameterTypeClass = parameterTypeField + ".getDataType().getRepresentation()"; 
					sb.append(indent(tabs));
				} else {
					parameterTypeClass = parameterType + ".class";
				}
				sb.append(parameterType + " " + parameterName + 
						" = (" + parameterType + ") " + 
						converterField + ".convertToComplexType(" + 
						"findPropertyAndRemove(" + uuidField + ", " +
						"\"" + parameterName + "\", " + 
						persistedPropertiesField + "), " + parameterTypeClass + ");\n");
			} else if (ParameterType.PRIMITIVE.equals(cpo.getProperty())) {
				sb.append(parameterType + " " + parameterName + " = " + 
						parameterType + ".valueOf(");
				
				if (cpo.getType() == Character.class) {
					sb.append("'" + cpo.getValue() + "');\n");
				} else {
					sb.append("\"" + cpo.getValue() + "\");\n");
				}
			} else if (ParameterType.CHILD.equals(cpo.getProperty())) {
				String objectUUIDField = parameterName + "UUID";
				String childPersisterHelperField = parameterName + "Helper";
				String childPersistedObject = parameterName + "PSO";
				
				println(sb, tabs, "String " + objectUUIDField + " = (String) findPropertyAndRemove(" + 
						uuidField + ", \"" + parameterName + "\", " + persistedPropertiesField + ");");
				println(sb, tabs, "PersistedSPObject " + childPersistedObject + 
				        " = findPersistedSPObject(" + uuidField + ", \"" + cpo.getType().getName() + "\", " + 
				        objectUUIDField + ", " + persistedObjectsListField + ");");
				println(sb, tabs, "try {");
				tabs++;
				println(sb, tabs, classToLoadField + " = (Class<? extends SPObject>) " + visitedClass.getSimpleName() + 
				        ".class.getClassLoader().loadClass(" + childPersistedObject + ".getType());");
				tabs--;
		        println(sb, tabs, "} catch (ClassNotFoundException e) {");
		        tabs++;
		        println(sb, tabs, "throw new SPPersistenceException(null, e);");
		        tabs--;
		        println(sb, tabs, "}");
				
				println(sb, tabs, "SPPersisterHelper<? extends SPObject> " + childPersisterHelperField + ";");
				println(sb, tabs, "try {");
				tabs++;
				println(sb, tabs, childPersisterHelperField + " = PersisterHelperFinder.findPersister(" + classToLoadField + ");");
				tabs--;
				println(sb, tabs, "} catch (Exception e) {");
				tabs++;
				println(sb, tabs, "throw new SPPersistenceException(uuid, e);");
				tabs--;
				println(sb, tabs, "}");
				println(sb, tabs, parameterType + " " + parameterName + " = (" + parameterType + ") " + 
						childPersisterHelperField + ".commitObject(" + childPersistedObject + ", " + 
						persistedPropertiesField + ", " + persistedObjectsListField + ", " + converterField + ");");
				importedClassNames.add(PersisterHelperFinder.class.getName());
				importedClassNames.add(SPPersisterHelper.class.getName());
				
			} else {
				throw new IllegalStateException("Don't know how to handle " +
						"property type " + cpo.getProperty());
			}
		}
		sb.append("\n");
		
		// Create and return the new object.
		sb.append(indent(tabs));
		sb.append(visitedClass.getSimpleName() + " " + objectField + ";\n");
		sb.append(indent(tabs));
		sb.append("try {\n");
		sb.append(indent(tabs + 1));
		sb.append(objectField + " = new " + 
				visitedClass.getSimpleName() + "(");
		
		boolean firstArg = true;
		
		// Pass in all of the constructor arguments.
		for (ConstructorParameterObject cpo : constructorParameters) {
			if (!firstArg) {
				sb.append(", ");
			}
			sb.append(cpo.getName());
			firstArg = false;
		}
		
		sb.append(");\n");
		
		sb.append(indent(tabs));
		sb.append("} catch (Exception ex) {\n");
		sb.append(indent(tabs + 1));
		sb.append("throw new SPPersistenceException(null, ex);\n");
		sb.append(indent(tabs));
		sb.append("}\n");
		
		sb.append(indent(tabs));
		sb.append(objectField + ".setUUID(" + uuidField + ");\n");
		
		sb.append(indent(tabs));
		sb.append(persistedObjectField + ".setLoaded(true);\n");
		
		sb.append(indent(tabs));
		sb.append("return " + objectField + ";\n");
		
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
	 * @param mutatorExtraParameters
	 *            The {@link Multimap} of setter methods mapped to each of its
	 *            extra parameters (second parameter and onwards).
	 * @param mutatorThrownTypes
	 *            The {@link Multimap} of property setter methods to their
	 *            thrown exceptions.
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
			Multimap<String, MutatorParameterObject> mutatorExtraParameters,
			Multimap<String, Class<? extends Exception>> mutatorThrownTypes,
			int tabs) {
		StringBuilder sb = new StringBuilder();
		final String genericObjectField = "o";
		final String objectField = "castedObject";
		final String propertyNameField = "propertyName";
		final String newValueField = "newValue";
		final String converterField = "converter";
		final String exceptionField = "e";
		final String dataTypeField = "dataType";
		
		boolean firstIf = true;
		
		// commitProperty method header.
		sb.append(indent(tabs));
		sb.append("public void commitProperty(" + SPObject.class.getSimpleName() + " " + 
				genericObjectField + ", " + String.class.getSimpleName() + " " + 
				propertyNameField + ", " + Object.class.getSimpleName() + " " +
				newValueField + ", " + DataType.class.getSimpleName() + " " + dataTypeField + 
				", " + SessionPersisterSuperConverter.class.getSimpleName() + 
				" " + converterField + ") " + "throws " + 
				SPPersistenceException.class.getSimpleName() + " {\n");
		tabs++;
		
		println(sb, tabs, visitedClass.getSimpleName() + " " + objectField + " = " +
				"(" + visitedClass.getSimpleName() + ") " + genericObjectField + ";");
		
		// Search for the matching property name and set the value.
		for (Entry<String, Class<?>> e : setters.entrySet()) {
			String methodName = e.getKey();
			Class<?> type = e.getValue();
			
			sb.append(indent(tabs));
			
			if (!firstIf) {
				sb.append("} else ");
			}
			
			sb.append("if (" + propertyNameField + ".equals(\"" + 
					SPAnnotationProcessorUtils.convertMethodToProperty(methodName) + 
					"\")) {\n");
			tabs++;
			
			boolean throwsExceptions = mutatorThrownTypes.containsKey(e.getKey());
			
			if (throwsExceptions) {
				sb.append(indent(tabs));
				sb.append("try {\n");
				tabs++;
			}
			
			// Assign each extra argument value of setter methods to variables
			// to pass into the call to the setter afterwards.
			for (MutatorParameterObject extraParam : mutatorExtraParameters.get(methodName)) {
				sb.append(indent(tabs));
				sb.append(extraParam.getType().getSimpleName() + " " + 
						extraParam.getName() + " = " + 
						extraParam.getType().getSimpleName() + ".valueOf(\"" + 
						extraParam.getValue() + "\");\n");
			}
			
			// Pass in the actual property value as the first argument to the setter.
			sb.append(indent(tabs));
			String conversionType;
			if (type == Object.class) {
				conversionType = dataTypeField + ".getRepresentation()";
			} else {
				conversionType = type.getSimpleName() + ".class";
			}
			sb.append(objectField + "." + methodName + "((" + type.getSimpleName() + 
					") " + converterField + ".convertToComplexType(" + newValueField + 
					", " + conversionType + ")");
			
			// Pass in the variables holding the extra argument values.
			for (MutatorParameterObject extraParam : mutatorExtraParameters.get(methodName)) {
				sb.append(", " + extraParam.getName());
			}
			
			sb.append(");\n");
			tabs--;
			
			// Catch any exceptions that the setter throws.
			if (throwsExceptions) {
				for (Class<? extends Exception> thrownType : 
					mutatorThrownTypes.get(methodName)) {
					
					sb.append(indent(tabs));
					sb.append("} catch (" + thrownType.getSimpleName() + " " + 
							exceptionField + ") {\n");
					tabs++;
					
					sb.append(indent(tabs));
					sb.append("throw new " + SPPersistenceException.class.getSimpleName() + 
							"(" + objectField + ".getUUID(), " +
									AbstractSPPersisterHelper.class.getSimpleName() + ".createSPPersistenceExceptionMessage(" + 
									objectField + ", " + propertyNameField + "), " + 
									exceptionField + ");\n");
					importedClassNames.add(AbstractSPPersisterHelper.class.getName());
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
		
		if (SPObject.class.isAssignableFrom(visitedClass.getSuperclass())) {
			Class<?> superclass = visitedClass.getSuperclass();
			final String parentHelper = "parentHelper";
			final String persisterHelperClassName = PersisterHelperFinder.getPersisterHelperClassName(superclass.getName());
			println(sb, tabs, persisterHelperClassName + " " + parentHelper + " = " + 
					"new " + persisterHelperClassName + "();");
			println(sb, tabs, parentHelper + ".commitProperty(" + genericObjectField + ", " +
					propertyNameField + ", " + newValueField + ", " + dataTypeField + ", " + converterField + ");");
		} else {
			// Throw an SPPersistenceException if the property is not persistable or unrecognized.
			sb.append(indent(tabs));
			sb.append("throw new " + SPPersistenceException.class.getSimpleName() + 
					"(" + objectField + ".getUUID(), " + AbstractSPPersisterHelper.class.getSimpleName() + ".createSPPersistenceExceptionMessage(" + 
					objectField + ", " + propertyNameField + "));\n");
			importedClassNames.add(AbstractSPPersisterHelper.class.getName());
		}
		
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
	
	private String generateGetPersistedPropertyListMethod(
			Class<? extends SPObject> visitedClass,
			Map<String, Class<?>> setters,
			int tabs) {

		importedClassNames.add("java.util.ArrayList");
		importedClassNames.add("java.util.Arrays");
		
		StringBuilder sb = new StringBuilder();
		final String ppaField = "persistedPropertiesArray";
		final String pplField = "persistedPropertiesList";
		
		println(sb, tabs, "public List<String> getPersistedProperties() throws " 
				+ SPPersistenceException.class.getSimpleName() + " {");
		// Create array of strings holding persisted properties
		tabs++;
		println(sb, tabs, "String [] " + ppaField + " = {");
		Object [] properties = setters.keySet().toArray();
		if (properties.length > 0) {
			tabs++;
			for (int i = 0; i < properties.length - 1; i++) {
				println(sb, tabs, "\"" + SPAnnotationProcessorUtils
						.convertMethodToProperty((String) properties[i]) + "\",");
			}
			println(sb, tabs, "\"" + SPAnnotationProcessorUtils
					.convertMethodToProperty((String) properties[properties.length - 1]) + "\"");
			tabs--;
		}
		println(sb, tabs, "};");
		// Put properties into list, along with the parent's persisted properties
		println(sb, tabs, "List<String> " + pplField + " = new ArrayList<String>(Arrays.asList("+ppaField+"));");
		if (SPObject.class.isAssignableFrom(visitedClass.getSuperclass())) {
			Class<?> superclass = visitedClass.getSuperclass();
			final String parentHelper = "parentHelper";
			final String persisterHelperClassName = PersisterHelperFinder.getPersisterHelperClassName(superclass.getName());
			println(sb, tabs, persisterHelperClassName + " " + parentHelper + " = " + 
					"new " + persisterHelperClassName + "();");
			println(sb, tabs, pplField + ".addAll(" + parentHelper + ".getPersistedProperties());");
		}
		println(sb, tabs, "return " + pplField + ";");
		tabs--;
		println(sb, tabs, "}");
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
	 * @param accessorAdditionalInfo
	 *            The {@link Multimap} of getter methods mapped to additional
	 *            properties a session {@link SPPersister} requires to convert
	 *            the getter's returned value from a complex to basic
	 *            persistable type.
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
			Multimap<String, String> accessorAdditionalInfo,
			int tabs) {
		StringBuilder sb = new StringBuilder();
		final String genericObjectField = "o";
		final String objectField = "castedObject";
		final String propertyNameField = "propertyName";
		final String converterField = "converter";
		
		boolean firstIf = true;
		
		// findProperty method header.
		sb.append(indent(tabs));
		sb.append("public " + Object.class.getSimpleName() + " findProperty(" + 
				SPObject.class.getSimpleName() + " " + genericObjectField + ", " +
				String.class.getSimpleName() + " " + propertyNameField + ", " +
				SessionPersisterSuperConverter.class.getSimpleName() + " " + converterField + 
				") " + 
				"throws " + SPPersistenceException.class.getSimpleName() + " {\n");
		tabs++;
		
		println(sb, tabs, visitedClass.getSimpleName() + " " + objectField + " = " +
				"(" + visitedClass.getSimpleName() + ") " + genericObjectField + ";");
		
		// Search for the matching property name and return the value.
		for (Entry<String, Class<?>> e : getters.entrySet()) {
			String methodName = e.getKey();
			
			sb.append(indent(tabs));
			
			if (!firstIf) {
				sb.append("} else ");
			}
			
			sb.append("if (" + propertyNameField + ".equals(\"" + 
					SPAnnotationProcessorUtils.convertMethodToProperty(methodName) + 
					"\")) {\n");
			tabs++;
			
			sb.append(indent(tabs));
			sb.append("return " + converterField + ".convertToBasicType(" + 
					objectField + "." + methodName + "()");
			
			for (String additionalProperty : accessorAdditionalInfo.get(methodName)) {
				sb.append(", " + objectField + "." + 
						SPAnnotationProcessorUtils.convertPropertyToAccessor(additionalProperty, visitedClass) +
						"()");
			}
			
			sb.append(");\n");
			
			tabs--;
			firstIf = false;
		}
		
		if (!firstIf) {
			sb.append(indent(tabs));
			sb.append("} else {\n");
			tabs++;
		}
		
		if (SPObject.class.isAssignableFrom(visitedClass.getSuperclass())) {
			Class<?> superclass = visitedClass.getSuperclass();
			final String parentHelper = "parentHelper";
			final String persisterHelperClassName = PersisterHelperFinder.getPersisterHelperClassName(superclass.getName());
			println(sb, tabs, persisterHelperClassName + " " + parentHelper + " = " + 
					"new " + persisterHelperClassName + "();");
			println(sb, tabs, "return " + parentHelper + ".findProperty(" + genericObjectField + ", " +
					propertyNameField + ", " + converterField + ");");
		} else {
			// Throw an SPPersistenceException if the property is not persistable or unrecognized.
			sb.append(indent(tabs));
			sb.append("throw new " + SPPersistenceException.class.getSimpleName() + 
					"(" + objectField + ".getUUID(), " + AbstractSPPersisterHelper.class.getSimpleName() + ".createSPPersistenceExceptionMessage(" + 
					objectField + ", " + propertyNameField + "));\n");
			importedClassNames.add(AbstractSPPersisterHelper.class.getName());
		}
		
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
			List<ConstructorParameterObject> constructorParameters,
			Map<String, Class<?>> accessors,
			Set<String> propertiesToPersistOnlyIfNonNull,
			int tabs) {
		StringBuilder sb = new StringBuilder();
		final String genericObjectField = "o";
		final String objectField = "castedObject";
		final String indexField = "index";
		final String persisterField = "persister";
		final String converterField = "converter";
		final String uuidField = "uuid";
		final String parentUUIDField = "parentUUID";
		
		//Properties already processed by the constructor, to be skipped
		//by the helper persisters that are parent classes to this object class.
		final String preProcessedProps = "preProcessedProperties";
		
		// persistObject method header.
		sb.append(indent(tabs));
		sb.append("public void persistObject(" + SPObject.class.getSimpleName() + " " + 
				genericObjectField + ", int " + indexField + ", " + 
				SPPersister.class.getSimpleName() + " " + persisterField + ", " +
				SessionPersisterSuperConverter.class.getSimpleName() + " " + 
				converterField + ") throws " + SPPersistenceException.class.getSimpleName() + 
				" {\n");
		tabs++;
		
		println(sb, tabs, visitedClass.getSimpleName() + " " + objectField + " = " +
				"(" + visitedClass.getSimpleName() + ") " + genericObjectField + ";");
		
		sb.append(indent(tabs));
		sb.append("final " + String.class.getSimpleName() + " " + uuidField + " = " + 
				objectField + ".getUUID();\n");
		
		sb.append(indent(tabs));
		sb.append(String.class.getSimpleName() + " " + 
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
				visitedClass.getName() + "\", " + uuidField + ", " + 
				indexField + ");\n\n");
		
		sb.append(indent(tabs));
		
		//TODO pass in the actual exception types on any accessors
		//then replace this blanket try/catch with specifics for any accessor
		//that throws an exception.
		println(sb, tabs, "List<String> " + preProcessedProps + " = new ArrayList<String>();");
		importedClassNames.add(ArrayList.class.getName());
		println(sb, tabs, "try {");
		tabs++;
		if (constructorParameters.isEmpty()) {
			sb.append("// No constructor arguments\n");
		} else {
			sb.append("// Constructor arguments\n");
			
			final String dataTypeField = "dataType";
			println(sb, tabs, "DataType " + dataTypeField + ";");
			// Persist all of its constructor argument properties.
			for (ConstructorParameterObject cpo : constructorParameters) {
				//XXX Should this only be properties?
				if (ParameterType.PROPERTY.equals(cpo.getProperty()) ||
						ParameterType.CHILD.equals(cpo.getProperty())) {
					
					String getPersistedProperty = objectField + "." + 
						SPAnnotationProcessorUtils.convertPropertyToAccessor(
								cpo.getName(), visitedClass) + "()";
					if (cpo.getType() == Object.class) {
						println(sb, tabs, "if (" + getPersistedProperty + " == null) {");
						tabs++;
						println(sb, tabs, dataTypeField + " = " + PersisterUtils.class.getSimpleName() + 
								".getDataType(null);");
						tabs--;
						println(sb, tabs, "} else {");
						tabs++;
						println(sb, tabs, dataTypeField + " = " + PersisterUtils.class.getSimpleName() + 
								".getDataType(" + getPersistedProperty +".getClass());");
						tabs--;
						println(sb, tabs, "}");
						importedClassNames.add(PersisterUtils.class.getName());
					} else {
						println(sb, tabs, dataTypeField + " = " + DataType.class.getSimpleName() + "." + 
								PersisterUtils.getDataType(cpo.getType()).name() + ";");
					}
					
					sb.append(indent(tabs));
					sb.append(persisterField + ".persistProperty(" + uuidField + ", \"" + 
							cpo.getName() + "\", " + //XXX we should convert this name as the constructor parameter name may be different than the property name defined by the accessor.
							dataTypeField + 
							", " + converterField + ".convertToBasicType(" + objectField + "." +
							SPAnnotationProcessorUtils.convertPropertyToAccessor(
									cpo.getName(), 
									visitedClass) + 
							"()));\n");
					println(sb, tabs, preProcessedProps + ".add(\"" + cpo.getName() + "\");");
					importedClassNames.add(DataType.class.getName());
				}
			}
		}
		sb.append("\n");
		
		println(sb, tabs, "persistObjectProperties(" + genericObjectField + ", " +
				persisterField + ", " + converterField + ", " + preProcessedProps + ");");
		
		tabs--;
		println(sb, tabs, "} catch (Exception ex) {");
		tabs++;
		println(sb, tabs, "throw new SPPersistenceException(" + uuidField + ", ex);");
		tabs--;
		println(sb, tabs, "}");
		
		tabs--;
		sb.append(indent(tabs));
		sb.append("}\n");
		
		return sb.toString();
	}

	/**
	 * Generates and returns source code for persisting the remaining state not
	 * done by {@link #generatePersistObjectMethod(Class, List, Map, Set, int)}.
	 * This is for classes to allow properties that are not defined in a
	 * sub-class to be persisted.
	 * <p>
	 * In the future we may want to have the parent classes persist properties
	 * first and children to persist properties later. We can pool the property
	 * changes in {@link PersistedSPOProperty} objects and persist them at the
	 * end of the method call.
	 * 
	 * @param visitedClass
	 *            The {@link SPObject} class that is being visited by the
	 *            annotation processor.
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
	private String generatePersistObjectMethodHelper(
			Class<? extends SPObject> visitedClass,
			Map<String, Class<?>> accessors,
			Set<String> propertiesToPersistOnlyIfNonNull,
			int tabs) {
		StringBuilder sb = new StringBuilder();
		final String genericObjectField = "o";
		final String objectField = "castedObject";
		final String persisterField = "persister";
		final String converterField = "converter";
		final String uuidField = "uuid";
		
		//These are the properties on the sub-class helper calling this abstract
		//helper that have already been persisted by the current persistObject
		//call. These properties do not have to be persisted again.
		final String preProcessedPropField = "preProcessedProps";
		
		// persistObject method header.
		sb.append(indent(tabs));
		sb.append("public void persistObjectProperties(" + SPObject.class.getSimpleName() + " " + 
				genericObjectField + ", " + 
				SPPersister.class.getSimpleName() + " " + persisterField + ", " +
				SessionPersisterSuperConverter.class.getSimpleName() + " " + 
				converterField + ", List<String> " + preProcessedPropField  + ") " +
                "throws " + SPPersistenceException.class.getSimpleName() + 
				" {\n");
		tabs++;
		println(sb, tabs, "final " + String.class.getSimpleName() + " " + uuidField + " = " + 
				genericObjectField + ".getUUID();\n");
		
		println(sb, tabs, visitedClass.getSimpleName() + " " + objectField + " = " +
				"(" + visitedClass.getSimpleName() + ") " + genericObjectField + ";");

		boolean lastEntryInIfBlock = false;
		
		println(sb, tabs, "try {");
		tabs++;
		
		// Persist all of its persistable properties.
		for (Entry<String, Class<?>> e : accessors.entrySet()) {
			String propertyName = SPAnnotationProcessorUtils.convertMethodToProperty(
					e.getKey());
			
			// Persist the property only if it has not been persisted yet
			// and (if required) persist if the value is not null.
			println(sb, tabs, "if (!" + preProcessedPropField + ".contains(\"" + propertyName + "\")) {");
			tabs++;
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

			final String dataTypeField = "dataType";
			println(sb, tabs, "DataType " + dataTypeField + ";");
			String getPersistedProperty = objectField + "." + e.getKey() + "()";
			if (e.getValue() == Object.class) {
				println(sb, tabs, "if (" + getPersistedProperty + " == null) {");
				tabs++;
				println(sb, tabs, dataTypeField + " = " + PersisterUtils.class.getSimpleName() + 
						".getDataType(null);");
				tabs--;
				println(sb, tabs, "} else {");
				tabs++;
				println(sb, tabs, dataTypeField + " = " + PersisterUtils.class.getSimpleName() + 
						".getDataType(" + getPersistedProperty +".getClass());");
				tabs--;
				println(sb, tabs, "}");
				importedClassNames.add(PersisterUtils.class.getName());
			} else {
				println(sb, tabs, dataTypeField + " = " + DataType.class.getSimpleName() + "." + 
						PersisterUtils.getDataType(e.getValue()).name() + ";");
			}
		    sb.append(indent(tabs));
			sb.append(persisterField + ".persistProperty(" + uuidField + ", \"" +
					propertyName + "\", " + dataTypeField +
					", " + converterField + ".convertToBasicType(" + getPersistedProperty + "));\n");
			println(sb, tabs, preProcessedPropField + ".add(\"" + propertyName + "\");");
			importedClassNames.add(DataType.class.getName());

			if (persistOnlyIfNonNull) {
				tabs--;
				sb.append(indent(tabs));
				sb.append("}\n");
				lastEntryInIfBlock = true;
			} else {
				lastEntryInIfBlock = false;
			}
			tabs--;
			println(sb, tabs, "}");
		}
		tabs--;
		println(sb, tabs, "} catch (Exception ex) {");
		tabs++;
		println(sb, tabs, "throw new SPPersistenceException(" + uuidField + ", ex);");
		tabs--;
		println(sb, tabs, "}");
		
		if (SPObject.class.isAssignableFrom(visitedClass.getSuperclass())) {
			Class<?> superclass = visitedClass.getSuperclass();
			final String parentHelper = "parentHelper";
			final String persisterHelperClassName = PersisterHelperFinder.getPersisterHelperClassName(superclass.getName());
			println(sb, tabs, persisterHelperClassName + " " + parentHelper + " = " + 
					"new " + persisterHelperClassName + "();");
			println(sb, tabs, parentHelper + ".persistObjectProperties(" + genericObjectField + ", " +
					persisterField + ", " + converterField + ", " + preProcessedPropField + ");");
		}
		
		tabs--;
		sb.append(indent(tabs));
		sb.append("}\n");
		
		return sb.toString();
	}

	//-------------- helper methods for dealing with string buffer, there may be a class that already does this
	
	private void println(StringBuilder sb, int tabs, String s) {
		sb.append(indent(tabs));
		sb.append(s);
		sb.append("\n");
	}
	
	private void print(StringBuilder sb, int tabs, String s) {
		sb.append(indent(tabs));
		sb.append(s);
	}
	
	private void niprintln(StringBuilder sb, String s) {
		sb.append(s + "\n");
	}
	
	private void niprint(StringBuilder sb, String s) {
		sb.append(s);
	}
	
	//-------------- end helper methods

}
