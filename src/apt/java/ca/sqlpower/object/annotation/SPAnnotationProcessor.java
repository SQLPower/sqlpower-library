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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

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

	/**
	 * The license file contents to prepend to a generated
	 * {@link SPPersisterHelper}.
	 */
	private final static String LICENSE_COMMENT_FILE_PATH = "src/main/resources/license_in_comment.txt";

	/**
	 * @see SPPersisterHelper#commitObject(PersistedSPObject, Multimap, List,
	 *      SessionPersisterSuperConverter)
	 */
	private final static String COMMIT_OBJECT_METHOD_NAME = "commitObject";

	/**
	 * @see SPPersisterHelper#commitProperty(SPObject, String, Object, DataType,
	 *      SessionPersisterSuperConverter)
	 */
	private final static String COMMIT_PROPERTY_METHOD_NAME = "commitProperty";

	/**
	 * @see SessionPersisterSuperConverter#convertToBasicType(Object, Object...)
	 */
	private final static String CONVERT_TO_BASIC_TYPE_METHOD_NAME = "convertToBasicType";

	/**
	 * @see SessionPersisterSuperConverter#convertToComplexType(Object, Class)
	 */
	private final static String CONVERT_TO_COMPLEX_TYPE_METHOD_NAME = "convertToComplexType";

	/**
	 * @see AbstractSPPersisterHelper#createSPPersistenceExceptionMessage(SPObject,
	 *      String)
	 */
	private final static String CREATE_EXCEPTION_MESSAGE_METHOD_NAME = "createSPPersistenceExceptionMessage";
	
	/**
	 * @see PersisterHelperFinder#findPersister(Class)
	 */
	private final static String FIND_PERSISTER_METHOD_NAME = "findPersister";

	/**
	 * @see AbstractSPPersisterHelper#findPersistedSPObject(String, String,
	 *      String, List)
	 */
	private final static String FIND_PERSISTED_OBJECT_METHOD_NAME = "findPersistedSPObject";

	/**
	 * @see AbstractSPPersisterHelper#findPropertyAndRemove(String, String,
	 *      Multimap)
	 */
	private final static String FIND_PROPERTY_AND_REMOVE_METHOD_NAME = "findPropertyAndRemove";

	/**
	 * @see SPPersisterHelper#findProperty(SPObject, String,
	 *      SessionPersisterSuperConverter)
	 */
	private final static String FIND_PROPERTY_METHOD_NAME = "findProperty";
	
	/**
	 * @see SPPersisterHelper#getPersistedProperties()
	 */
	private final static String GET_PERSISTED_PROPERTIES_METHOD_NAME = "getPersistedProperties";
	
	/**
	 * @see SPObject#getParent()
	 */
	private final static String GET_PARENT_METHOD_NAME = "getParent";

	/**
	 * @see SPObject#getUUID()
	 */
	private final static String GET_UUID_METHOD_NAME = "getUUID";

	/**
	 * @see SPPersisterHelper#persistObject(SPObject, int, SPPersister,
	 *      SessionPersisterSuperConverter)
	 */
	private final static String PERSIST_OBJECT_METHOD_NAME = "persistObject";

	/**
	 * @see SPPersisterHelper#persistObjectProperties(SPObject, SPPersister,
	 *      SessionPersisterSuperConverter, List)
	 */
	private final static String PERSIST_OBJECT_PROPERTIES_METHOD_NAME = "persistObjectProperties";

	/**
	 * @see SPPersister#persistProperty(String, String, DataType, Object,
	 *      Object)
	 */
	private final static String PERSIST_PROPERTY_METHOD_NAME = "persistProperty";
	
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
	 * This type generic parameter defines which {@link SPObject} class a
	 * specific {@link SPPersisterHelper} handles for persisting objects and
	 * properties.
	 */
	private final String TYPE_GENERIC_PARAMETER = "T";

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
						superClass,
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
				generateAbstractPersisterHelperFile(
						superClass,
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
			final String simpleClassName = visitedClass.getSimpleName() + "PersisterHelper";
			final Class<?> superclass = visitedClass.getSuperclass();
			int tabs = 0;
			
			Filer f = environment.getFiler();
			PrintWriter pw = f.createSourceFile(helperPackage + "." + simpleClassName);
			
			tabs++;
			final String commitObjectMethod = generateCommitObjectMethod(visitedClass, constructorParameters, tabs);
			final String commitPropertyMethod = generateCommitPropertyMethod(visitedClass, propertiesToMutate, 
					mutatorExtraParameters, mutatorThrownTypes, tabs);
			final String findPropertyMethod = generateFindPropertyMethod(visitedClass, propertiesToAccess, 
					accessorAdditionalInfo, tabs);
			final String persistObjectMethod = generatePersistObjectMethod(visitedClass, constructorParameters, 
					propertiesToAccess, propertiesToPersistOnlyIfNonNull, tabs);
			final String PersistObjectMethodHelper = generatePersistObjectMethodHelper(visitedClass, 
					propertiesToAccess, propertiesToMutate, propertiesToPersistOnlyIfNonNull, tabs);
			final String getPersistedPropertiesMethod = generateGetPersistedPropertyListMethod(visitedClass, propertiesToMutate, tabs);
			tabs--;
			
			if (superclass == Object.class) {
				importedClassNames.add(AbstractSPPersisterHelper.class.getName());
			} else {
				importedClassNames.add(PersisterHelperFinder.getPersisterHelperClassName(superclass.getName()));
			}
			final String imports = generateImports(visitedClass, constructorImports, mutatorImports);
			
			pw.print(generateWarning());
			pw.print("\n");
			pw.print(generateLicense());
			pw.print("\n");
			pw.print("package " + helperPackage + ";\n");
			pw.print("\n");
			pw.print(imports);
			pw.print("\n");
			
			if (superclass == Object.class) {
				pw.print(String.format("public class %s extends %s<%s> {\n",
						simpleClassName,
						AbstractSPPersisterHelper.class.getSimpleName(),
						visitedClass.getSimpleName()));
			} else if (Modifier.isAbstract(superclass.getModifiers())) {
				pw.print(String.format("public class %s extends %s<%s> {\n",
						simpleClassName,
						superclass.getSimpleName() + "PersisterHelper",
						visitedClass.getSimpleName()));
			} else {
				pw.print(String.format("public class %s extends %s {\n",
						simpleClassName,
						superclass.getSimpleName() + "PersisterHelper"));
			}
			
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
			final String simpleClassName = visitedClass.getSimpleName() + "PersisterHelper";
			final Class<?> superclass = visitedClass.getSuperclass();
			int tabs = 0;
			
			Filer f = environment.getFiler();
			PrintWriter pw = f.createSourceFile(helperPackage + "." + simpleClassName);
			tabs++;
			final String commitPropertyMethod = generateCommitPropertyMethod(visitedClass, propertiesToMutate, 
					mutatorExtraParameters, mutatorThrownTypes, tabs);
			final String findPropertyMethod = generateFindPropertyMethod(visitedClass, propertiesToAccess, 
					accessorAdditionalInfo, tabs);
			final String persistObjectMethodHelper = generatePersistObjectMethodHelper(visitedClass, 
					propertiesToAccess, propertiesToMutate, propertiesToPersistOnlyIfNonNull, tabs);
			final String getPersistedPropertiesMethod = generateGetPersistedPropertyListMethod(visitedClass, propertiesToMutate, tabs);
			tabs--;
			
			if (superclass == Object.class) {
				importedClassNames.add(AbstractSPPersisterHelper.class.getName());
			} else {
				importedClassNames.add(PersisterHelperFinder.getPersisterHelperClassName(superclass.getName()));
			}
			final String generateImports = generateImports(visitedClass, constructorImports, mutatorImports);
			
			pw.print(generateWarning());
			pw.print("\n");
			pw.print(generateLicense());
			pw.print("\n");
			pw.print("package " + helperPackage + ";\n");
			pw.print("\n");
			pw.print(generateImports);
			pw.print("\n");
			
			if (superclass == Object.class) {
				pw.print(String.format("public abstract class %s<%s extends %s> extends %s<%s> {\n",
						simpleClassName,
						TYPE_GENERIC_PARAMETER,
						visitedClass.getSimpleName(),
						AbstractSPPersisterHelper.class.getSimpleName(),
						TYPE_GENERIC_PARAMETER));
			} else if (Modifier.isAbstract(superclass.getModifiers())) {
				pw.print(String.format("public abstract class %s<%s extends %s> extends %s<%s> {\n",
						simpleClassName,
						TYPE_GENERIC_PARAMETER,
						visitedClass.getSimpleName(),
						superclass.getSimpleName() + "PersisterHelper",
						TYPE_GENERIC_PARAMETER));
			} else {
				pw.print(String.format("public abstract class %s<%s extends %s> extends %s {\n",
						simpleClassName,
						TYPE_GENERIC_PARAMETER,
						visitedClass.getSimpleName() + "PersisterHelper",
						superclass.getName()));
			}
			
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
		
		niprintln(sb, "/*");
		niprintln(sb, " * This is a GENERATED class based on hand made annotations in " + 
				SPObject.class.getSimpleName() + " classes");
		niprintln(sb, " * and should NOT be modified here. If you need to change this class, modify");
		niprintln(sb, " * " + SPAnnotationProcessor.class.getSimpleName() + " instead.");
		niprintln(sb, " */");
		
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
				niprintln(sb, line);
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
		final String helperPackage = visitedClass.getPackage().getName() + "." + PersisterHelperFinder.GENERATED_PACKAGE_NAME;
		
		// Using a TreeSet here to sort imports alphabetically.
		Set<String> allImports = new TreeSet<String>();
		if (!Modifier.isAbstract(visitedClass.getModifiers())) {
		    allImports.addAll(constructorImports);
		}
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
			// No need to import package if the persister helper is already
			// in the package.
			// Also want to keep array classes out
			if (!pkg.startsWith("java.lang") && !pkg.startsWith("[L")) {
				// Nested classes, enums, etc. will be separated by the "$"
				// character but we need to change them to "." so it can be
				// imported correctly.
				String pkgName = pkg.replaceAll("\\$", ".");
				
				// Only import the package if it is not the same one
				// that the persister helper exists in.
				int index = pkgName.lastIndexOf(".");
				if (index == -1) {
					index = pkgName.length();
				}
				if (!pkgName.substring(0, index).equals(helperPackage)) {
					niprintln(sb, "import " + pkgName + ";");
				}
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
		final String exceptionField = "e";
		
		// commitObject method header.
		// public <visitedClass> commitObject(
		// 		PersistedSPObject pso,
		// 		Multimap<String, PersistedSPOProperty> persistedProperties,
		// 		List<PersistedSPObject> persistedObjects,
		// 		SessionPersisterSuperConverter converter) 
		// 		throws SPPersistenceException {
		println(sb, tabs, 
				String.format("public %s commitObject(" +
						"%s %s, %s<%s, %s> %s, %s<%s> %s, %s %s) throws %s {",
						visitedClass.getSimpleName(),
						PersistedSPObject.class.getSimpleName(),
						persistedObjectField,
						Multimap.class.getSimpleName(),
						String.class.getSimpleName(),
						PersistedSPOProperty.class.getSimpleName(),
						persistedPropertiesField,
						List.class.getSimpleName(),
						PersistedSPObject.class.getSimpleName(),
						persistedObjectsListField,
						SessionPersisterSuperConverter.class.getSimpleName(),
						converterField,
						SPPersistenceException.class.getSimpleName()));
		
		importedClassNames.add(PersistedSPObject.class.getName());
		importedClassNames.add(PersistedSPOProperty.class.getName());
		importedClassNames.add(Multimap.class.getName());
		tabs++;
		
		// String uuid = pso.getUUID();
		println(sb, tabs, 
				String.format("%s %s = %s.%s();", 
						String.class.getSimpleName(), 
						uuidField, 
						persistedObjectField,
						GET_UUID_METHOD_NAME));
		
		// Assign each constructor parameter property to a variable.
		final String parameterTypeField = "parameterType";
		final String classToLoadField = "classToLoad";
		boolean parameterTypeFieldDeclared = false;
		boolean classToLoadFieldDeclared = false;
		
		for (ConstructorParameterObject cpo : constructorParameters) {
			String parameterType = cpo.getType().getSimpleName();
			String parameterName = cpo.getName();

			if (ParameterType.PROPERTY.equals(cpo.getProperty())) {
				if (cpo.getType() == Object.class) {
					print(sb, tabs, "");
					
					if (!parameterTypeFieldDeclared) {
						niprint(sb, PersistedSPOProperty.class.getSimpleName() + " ");
						parameterTypeFieldDeclared = true;
					}
					
					// parameterType = findProperty(uuid, "<parameterName>", persistedProperties);
					niprintln(sb, String.format("%s = %s(%s, \"%s\", %s);",
									parameterTypeField,
									FIND_PROPERTY_METHOD_NAME,
									uuidField,
									parameterName,
									persistedPropertiesField));

					// <parameterType> <parameterName>;
					println(sb, tabs, 
							String.format("%s %s;",
									parameterType,
									parameterName));
					// if (parameterType != null) {
					println(sb, tabs, 
							String.format("if (%s != null) {", parameterTypeField));
					tabs++;

					// <parameterName> = 
					// 		(<parameterType>) converter.convertToComplexType(
					// 				findPropertyAndRemove(
					// 						uuid,
					// 						"<parameterName>",
					// 						persistedProperties),
					// 				parameterType.getDataType().getRepresentation()); 
					println(sb, tabs, 
							String.format("%s = (%s) %s.%s(%s(%s, \"%s\", %s), %s.getDataType().getRepresentation());",
									parameterName,
									parameterType,
									converterField,
									CONVERT_TO_COMPLEX_TYPE_METHOD_NAME,
									FIND_PROPERTY_AND_REMOVE_METHOD_NAME,
									uuidField,
									parameterName,
									persistedPropertiesField,
									parameterTypeField));
					tabs--;
					println(sb, tabs, "} else {");
					tabs++;
					// <parameterName> = null;
					println(sb, tabs, String.format("%s = null;", parameterName));
					tabs--;
					println(sb, tabs, "}");
				} else {
					// <parameterType> <parameterName> = 
					// 		(<parameterType>) converter.convertToComplexType(
					// 				findPropertyAndRemove(
					// 						uuid,
					// 						"<parameterName>",
					// 						persistedProperties),
					// 				<parameterType>.class);
					println(sb, tabs, 
							String.format("%s %s = (%s) %s.%s(%s(%s, \"%s\", %s), %s.class);",
									parameterType,
									parameterName,
									parameterType,
									converterField,
									CONVERT_TO_COMPLEX_TYPE_METHOD_NAME,
									FIND_PROPERTY_AND_REMOVE_METHOD_NAME,
									uuidField,
									parameterName,
									persistedPropertiesField,
									parameterType));
				}
			} else if (ParameterType.CHILD.equals(cpo.getProperty())) {
				String objectUUIDField = parameterName + "UUID";
				String childPersisterHelperField = parameterName + "Helper";
				String childPersistedObject = parameterName + "PSO";

				// String <parameterName>UUID = 
				// 		(String) findPropertyAndRemove(
				// 				uuid,
				// 				"<parameterName>",
				// 				persistedProperties);
				println(sb, tabs, 
						String.format("%s %s = (%s) %s(%s, \"%s\", %s);",
								String.class.getSimpleName(),
								objectUUIDField,
								String.class.getSimpleName(),
								FIND_PROPERTY_AND_REMOVE_METHOD_NAME,
								uuidField,
								parameterName,
								persistedPropertiesField));

				// PersistedSPObject <parameterName>PSO = 
				// 		findPersistedSPObject(
				// 				uuid,
				// 				"<SPObject simple name>",
				// 				<parameterName>UUID,
				// 				persistedObjects);
				println(sb, tabs, 
						String.format("%s %s = %s(%s, %s, %s);",
								PersistedSPObject.class.getSimpleName(),
								childPersistedObject,
								FIND_PERSISTED_OBJECT_METHOD_NAME,
								uuidField,
								objectUUIDField,
								persistedObjectsListField));
				
				if (!classToLoadFieldDeclared) {
					println(sb, tabs, 
							String.format("%s<? extends %s> %s;",
									Class.class.getSimpleName(),
									SPObject.class.getSimpleName(),
									classToLoadField));
					classToLoadFieldDeclared = true;
				}

				// try {
				println(sb, tabs, "try {");
				tabs++;
				
				// classToLoad = 
				// 		(Class<? extends SPObject>)
				// 				<visitedClass>.class.getClassLoader().loadClass(
				// 						<parameterName>.getType());
				println(sb, tabs,
						String.format("%s = (%s<? extends %s>) %s.class.getClassLoader().loadClass(%s.getType());",
								classToLoadField,
								Class.class.getSimpleName(),
								SPObject.class.getSimpleName(),
								visitedClass.getSimpleName(), 
								childPersistedObject));

				tabs--;
				// catch (ClassNotFoundException e) {
				println(sb, tabs,
						String.format("} catch (%s %s) {",
								ClassNotFoundException.class.getSimpleName(),
								exceptionField));
				tabs++;
				// throw new SPPersistenceException(null, e);
				println(sb, tabs, 
						String.format("throw new %s(null, %s);",
								SPPersistenceException.class.getSimpleName(),
								exceptionField));
				tabs--;
				println(sb, tabs, "}");

				// SPPersisterHelper<? extends SPObject> <parameterName>PersisterHelper;
				println(sb, tabs, 
						String.format("%s<? extends %s> %s;",
								SPPersisterHelper.class.getSimpleName(),
								SPObject.class.getSimpleName(),
								childPersisterHelperField));
				// try {
				println(sb, tabs, "try {");
				tabs++;

				// <parameterName>PersisterHelper = PersisterHelperFinder.findPersister(classToLoad);
				println(sb, tabs, 
						String.format("%s = %s.%s(%s);",
								childPersisterHelperField,
								PersisterHelperFinder.class.getSimpleName(),
								FIND_PERSISTER_METHOD_NAME,
								classToLoadField));
				tabs--;
				// } catch (Exception e) {
				println(sb, tabs, 
						String.format("} catch (%s %s) {",
								Exception.class.getSimpleName(),
								exceptionField));
				tabs++;
				// throw new SPPersistenceException(uuid, e);
				println(sb, tabs, 
						String.format("throw new %s(%s, %s);",
								SPPersistenceException.class.getSimpleName(),
								uuidField,
								exceptionField));
				tabs--;
				println(sb, tabs, "}");
				// <parameterType> <parameterName> = 
				// 		(<parameterType>) <parameterName>PersisterHelper.commitObject(
				// 				<parameterName>PSO,
				// 				persistedProperties,
				// 				persistedObjects,
				// 				converter);
				println(sb, tabs, 
						String.format("%s %s = (%s) %s.%s(%s, %s, %s, %s);",
								parameterType,
								parameterName,
								parameterType,
								childPersisterHelperField,
								COMMIT_OBJECT_METHOD_NAME,
								childPersistedObject,
								persistedPropertiesField,
								persistedObjectsListField,
								converterField));
				importedClassNames.add(PersisterHelperFinder.class.getName());
				importedClassNames.add(SPPersisterHelper.class.getName());

			} else {
				throw new IllegalStateException("Don't know how to handle " +
						"property type " + cpo.getProperty());
			}
		}
		niprintln(sb, "");

		// Create and return the new object.
		// <visitedClass> o;
		println(sb, tabs, 
				String.format("%s %s;",
						visitedClass.getSimpleName(),
						objectField));
		// try {
		println(sb, tabs, "try {");
		tabs++;
		// o = new <visitedClass>(
		print(sb, tabs, 
				String.format("%s = new %s(",
						objectField,
						visitedClass.getSimpleName()));
		
		boolean firstArg = true;
		
		// Pass in all of the constructor arguments.
		for (ConstructorParameterObject cpo : constructorParameters) {
			if (!firstArg) {
				niprint(sb, ", ");
			}
			niprint(sb, cpo.getName());
			firstArg = false;
		}
		
		niprintln(sb, ");");
		tabs--;
		
		// catch (Exception e) {
		println(sb, tabs, 
				String.format("} catch (%s %s) {",
						Exception.class.getSimpleName(),
						exceptionField));
		tabs++;
		// throw new SPPersistenceException(null, e);
		println(sb, tabs, 
				String.format("throw new %s(null, %s);", 
						SPPersistenceException.class.getSimpleName(),
						exceptionField));
		tabs--;
		println(sb, tabs, "}");
		
		// o.setUUID(uuid);
		println(sb, tabs, 
				String.format("%s.setUUID(%s);",
						objectField,
						uuidField));
		
		// pso.setLoaded(true);
		println(sb, tabs, 
				String.format("%s.setLoaded(true);",
						persistedObjectField));
		
		// return o;
		println(sb, tabs, 
				String.format("return %s;", objectField));
		
		tabs--;
		println(sb, tabs, "}");
		
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
		// public void commitProperty(
		// 		SPObject o,
		// 		String propertyName,
		// 		Object newValue,
		// 		DataType dataType,
		// 		SessionPersisterSuperConverter converter) 
		// 		throws SPPersistenceException {
		println(sb, tabs, 
				String.format("public void %s(%s %s, %s %s, %s %s, %s %s, %s %s) throws %s {",
						COMMIT_PROPERTY_METHOD_NAME,
						SPObject.class.getSimpleName(),
						genericObjectField,
						String.class.getSimpleName(),
						propertyNameField,
						Object.class.getSimpleName(),
						newValueField,
						DataType.class.getSimpleName(),
						dataTypeField,
						SessionPersisterSuperConverter.class.getSimpleName(),
						converterField,
						SPPersistenceException.class.getSimpleName()));
		tabs++;

		if (!setters.isEmpty()) {
			// If the SPObject class this persister helper handles is abstract,
			// use the type generic defined in the class header.
			// Otherwise, use the SPObject class directly.
			if (Modifier.isAbstract(visitedClass.getModifiers())) {
				// T castedObject = (T) o;
				println(sb, tabs, 
						String.format("%s %s = (%s) %s;",
								TYPE_GENERIC_PARAMETER,
								objectField,
								TYPE_GENERIC_PARAMETER,
								genericObjectField));
			} else {
				// <visitedClass> castedObject = (<visitedClass>) o;
				println(sb, tabs,
						String.format("%s %s = (%s) %s;",
								visitedClass.getSimpleName(),
								objectField,
								visitedClass.getSimpleName(),
								genericObjectField));
			}

			// Search for the matching property name and set the value.
			for (Entry<String, Class<?>> e : setters.entrySet()) {
				String methodName = e.getKey();
				Class<?> type = e.getValue();

				print(sb, tabs, "");

				if (!firstIf) {
					niprint(sb, "} else ");
				}

				// if (propertyName.equals("<method to property name>") {
				niprintln(sb, 
						String.format("if (%s.equals(\"%s\")) {",
								propertyNameField,
								SPAnnotationProcessorUtils.convertMethodToProperty(methodName)));
				tabs++;

				boolean throwsExceptions = mutatorThrownTypes.containsKey(e.getKey());

				if (throwsExceptions) {
					println(sb, tabs, "try {");
					tabs++;
				}

				// Assign each extra argument value of setter methods to variables
				// to pass into the call to the setter afterwards.
				for (MutatorParameterObject extraParam : mutatorExtraParameters.get(methodName)) {
					// <extraParam type> <extraParam name> = 
					// 		<extraParam type>.valueOf("<extraParam name>");
					println(sb, tabs, 
							String.format("%s %s = %s.valueOf(\"%s\");",
									extraParam.getType().getSimpleName(),
									extraParam.getName(),
									extraParam.getType().getSimpleName(),
									extraParam.getValue()));
				}

				// Pass in the actual property value as the first argument to the setter.
				String conversionType;
				if (type == Object.class) {
					conversionType = dataTypeField + ".getRepresentation()";
				} else {
					conversionType = type.getSimpleName() + ".class";
				}

				// castedObject.<setter>(
				// 		(<type>) converter.convertToComplexType(
				// 				newValue, <dataType.getRepresentation | type.class>);
				print(sb, tabs, 
						String.format("%s.%s((%s) %s.%s(%s, %s)",
								objectField,
								methodName,
								type.getSimpleName(),
								converterField,
								CONVERT_TO_COMPLEX_TYPE_METHOD_NAME,
								newValueField,
								conversionType));

				// Pass in the variables holding the extra argument values.
				for (MutatorParameterObject extraParam : mutatorExtraParameters.get(methodName)) {
					// , <extraParam name>
					niprint(sb, ", " + extraParam.getName());
				}

				niprintln(sb, ");");
				tabs--;

				// Catch any exceptions that the setter throws.
				if (throwsExceptions) {
					for (Class<? extends Exception> thrownType : 
						mutatorThrownTypes.get(methodName)) {

						// } catch (<Exception type> e) {
						println(sb, tabs, 
								String.format("} catch (%s %s) {",
										thrownType.getSimpleName(),
										exceptionField));
						tabs++;

						// throw new SPPersistenceException(
						// 		castedObject.getUUID(),
						// 		createSPPersistenceExceptionMessage(
						// 				castedObject,
						// 				propertyName),
						// 		e);
						println(sb, tabs, 
								String.format("throw new %s(%s.%s(), %s(%s, %s), %s);",
										SPPersistenceException.class.getSimpleName(),
										objectField,
										GET_UUID_METHOD_NAME,
										CREATE_EXCEPTION_MESSAGE_METHOD_NAME,
										objectField,
										propertyNameField,
										exceptionField));
						tabs--;
					}
					println(sb, tabs, "}");
					tabs--;
				}

				firstIf = false;
			}

			if (!firstIf) {
				println(sb, tabs, "} else {");
				tabs++;
			}
		}
		
		if (SPObject.class.isAssignableFrom(visitedClass.getSuperclass())) {
			// super.commitProperty(o, <property>, newValue, dataType, converter);
			println(sb, tabs, 
					String.format("super.%s(%s, %s, %s, %s, %s);",
							COMMIT_PROPERTY_METHOD_NAME,
							genericObjectField,
							propertyNameField,
							newValueField,
							dataTypeField,
							converterField));
		} else {
			// Throw an SPPersistenceException if the property is not persistable or unrecognized.
			// throw new SPPersistenceException(
			// 		castedObject.getUUID(),
			// 		createSPPersistenceExceptionMessage(
			// 				castedObject,
			// 				propertyName));
			println(sb, tabs, 
					String.format("throw new %s(%s.%s(), %s(%s, %s));",
							SPPersistenceException.class.getSimpleName(),
							objectField,
							GET_UUID_METHOD_NAME,
							CREATE_EXCEPTION_MESSAGE_METHOD_NAME,
							objectField,
							propertyNameField));
		}
		
		if (!firstIf) {
			tabs--;
			println(sb, tabs, "}");
		}
		
		tabs--;
		println(sb, tabs, "}");
		
		return sb.toString();
	}
	
	private String generateGetPersistedPropertyListMethod(
			Class<? extends SPObject> visitedClass,
			Map<String, Class<?>> setters,
			int tabs) {

		importedClassNames.add(ArrayList.class.getName());
		importedClassNames.add(Arrays.class.getName());
		
		StringBuilder sb = new StringBuilder();
		final String ppaField = "persistedPropertiesArray";
		final String pplField = "persistedPropertiesList";
		
		// public List<String> getPersistedProperties() throws SPPersistenceException {
		println(sb, tabs, 
				String.format("public %s<%s> %s() throws %s {",
						List.class.getSimpleName(),
						String.class.getSimpleName(),
						GET_PERSISTED_PROPERTIES_METHOD_NAME,
						SPPersistenceException.class.getSimpleName()));
		
		// Create array of strings holding persisted properties
		tabs++;
		// String[] persistedPropertiesArray = {
		println(sb, tabs, 
				String.format("%s[] %s = {",
						String.class.getSimpleName(),
						ppaField));
		Object [] properties = setters.keySet().toArray();
		if (properties.length > 0) {
			tabs++;
			for (int i = 0; i < properties.length; i++) {
				print(sb, tabs, 
						String.format("\"%s\"",
								SPAnnotationProcessorUtils.convertMethodToProperty((String) properties[i])));
				if (i < properties.length - 1) {
					niprint(sb, ",");
				}
				niprintln(sb, "");
			}
			tabs--;
		}
		println(sb, tabs, "};");
		// Put properties into list, along with the parent's persisted properties
		// List<String> persistedPropertiesList = 
		// 		new ArrayList<String>(Arrays.asList(persistedPropertiesArray));
		println(sb, tabs, 
				String.format("%s<%s> %s = new %s<%s>(%s.asList(%s));",
						List.class.getSimpleName(),
						String.class.getSimpleName(),
						pplField,
						ArrayList.class.getSimpleName(),
						String.class.getSimpleName(),
						Arrays.class.getSimpleName(),
						ppaField));
		if (SPObject.class.isAssignableFrom(visitedClass.getSuperclass())) {
			// persistedPropertiesList.addAll(super.getPersistedProperties());
			println(sb, tabs, 
					String.format("%s.addAll(super.%s());",
							pplField,
							GET_PERSISTED_PROPERTIES_METHOD_NAME));
		}
		
		// return persistedPropertiesList;
		println(sb, tabs, 
				String.format("return %s;", pplField));
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
		// public Object findProperty(
		// 		SPObject o,
		// 		String propertyName,
		// 		SessionPersisterSuperConverter converter)
		// 		throws SPPersistenceException {
		println(sb, tabs, 
				String.format("public %s %s(%s %s, %s %s, %s %s) throws %s {",
				Object.class.getSimpleName(),
				FIND_PROPERTY_METHOD_NAME,
				SPObject.class.getSimpleName(),
				genericObjectField,
				String.class.getSimpleName(),
				propertyNameField,
				SessionPersisterSuperConverter.class.getSimpleName(),
				converterField,
				SPPersistenceException.class.getSimpleName()));
		tabs++;
		
		if (!getters.isEmpty()) {
			// If the SPObject class this persister helper handles is abstract,
			// use the type generic defined in the class header.
			// Otherwise, use the SPObject class directly.
			if (Modifier.isAbstract(visitedClass.getModifiers())) {
				// T castedObject = (T) o;
				println(sb, tabs, 
						String.format("%s %s = (%s) %s;",
								TYPE_GENERIC_PARAMETER,
								objectField,
								TYPE_GENERIC_PARAMETER,
								genericObjectField));
			} else {
				// <visitedClass> castedObject = (<visitedClass>) o;
				println(sb, tabs,
						String.format("%s %s = (%s) %s;",
								visitedClass.getSimpleName(),
								objectField,
								visitedClass.getSimpleName(),
								genericObjectField));
			}

			// Search for the matching property name and return the value.
			for (Entry<String, Class<?>> e : getters.entrySet()) {
				String methodName = e.getKey();

				print(sb, tabs, "");

				if (!firstIf) {
					niprint(sb, "} else ");
				}

				// if (propertyName.equals("<method to property name>") {
				niprintln(sb, 
						String.format("if (%s.equals(\"%s\")) {",
								propertyNameField,
								SPAnnotationProcessorUtils.convertMethodToProperty(methodName)));
				tabs++;

				// return converter.convertToBasicType(castedObject.<getter>());
				print(sb, tabs, 
						String.format("return %s.%s(%s.%s()",
								converterField,
								CONVERT_TO_BASIC_TYPE_METHOD_NAME,
								objectField,
								methodName));

				for (String additionalProperty : accessorAdditionalInfo.get(methodName)) {
					niprint(sb, 
							String.format(", %s.%s()",
									objectField,
									SPAnnotationProcessorUtils.convertPropertyToAccessor(additionalProperty, visitedClass)));
				}

				niprintln(sb, ");");

				tabs--;
				firstIf = false;
			}

			if (!firstIf) {
				println(sb, tabs, "} else {");
				tabs++;
			}
		}
		
		if (SPObject.class.isAssignableFrom(visitedClass.getSuperclass())) {
			Class<?> superclass = visitedClass.getSuperclass();
			
			// return super.findProperty(o, propertyName, converter);
			println(sb, tabs, 
					String.format("return super.%s(%s, %s, %s);",
							FIND_PROPERTY_METHOD_NAME,
							genericObjectField,
							propertyNameField,
							converterField));
		} else {
			// Throw an SPPersistenceException if the property is not persistable or unrecognized.
			// throw new SPPersistenceException(
			// 		castedObject.getUUID(),
			// 		createSPPersistenceExceptionMessage(
			// 				castedObject,
			// 				propertyName));
			println(sb, tabs, 
					String.format("throw new %s(%s.%s(), %s(%s, %s));",
							SPPersistenceException.class.getSimpleName(),
							objectField,
							GET_UUID_METHOD_NAME,
							CREATE_EXCEPTION_MESSAGE_METHOD_NAME,
							objectField,
							propertyNameField));
		}
		
		if (!firstIf) {
			tabs--;
			println(sb, tabs, "}");
		}
		
		tabs--;
		println(sb, tabs, "}");
		
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
		final String exceptionField = "e";
		
		//Properties already processed by the constructor, to be skipped
		//by the helper persisters that are parent classes to this object class.
		final String preProcessedProps = "preProcessedProperties";
		
		// persistObject method header.
		// public void persistObject(SPObject o, int index, SPPersister persister, SessionPersisterSuperConverter converter) throws SPPersistenceException {
		println(sb, tabs, 
				String.format("public void %s(%s %s, int %s, %s %s, %s %s) throws %s {",
						PERSIST_OBJECT_METHOD_NAME,
						SPObject.class.getSimpleName(),
						genericObjectField,
						indexField,
						SPPersister.class.getSimpleName(),
						persisterField,
						SessionPersisterSuperConverter.class.getSimpleName(),
						converterField,
						SPPersistenceException.class.getSimpleName()));
		tabs++;
		
		// If the SPObject class this persister helper handles is abstract,
		// use the type generic defined in the class header.
		// Otherwise, use the SPObject class directly.
		if (Modifier.isAbstract(visitedClass.getModifiers())) {
			// T castedObject = (T) o;
			println(sb, tabs, 
					String.format("%s %s = (%s) %s;",
							TYPE_GENERIC_PARAMETER,
							objectField,
							TYPE_GENERIC_PARAMETER,
							genericObjectField));
		} else {
			// <visitedClass> castedObject = (<visitedClass>) o;
			println(sb, tabs,
					String.format("%s %s = (%s) %s;",
							visitedClass.getSimpleName(),
							objectField,
							visitedClass.getSimpleName(),
							genericObjectField));
		}
		
		// final String uuid = castedObject.getUUID();
		println(sb, tabs, 
				String.format("final %s %s = %s.%s();",
						String.class.getSimpleName(),
						uuidField,
						objectField,
						GET_UUID_METHOD_NAME));
		
		// String parentUUID = null;
		println(sb, tabs, 
				String.format("%s %s = null;",
						String.class.getSimpleName(),
						parentUUIDField));
		
		// if (castedObject.getParent() != null) {
		println(sb, tabs, 
				String.format("if (%s.%s() != null) {",
						objectField,
						GET_PARENT_METHOD_NAME));
		tabs++;
		
		// parentUUID = castedObject.getParent().getUUID();
		println(sb, tabs, 
				String.format("%s = %s.%s().%s();",
						parentUUIDField,
						objectField,
						GET_PARENT_METHOD_NAME,
						GET_UUID_METHOD_NAME));
		
		tabs--;
		println(sb, tabs, "}\n");
		
		// Persist the object.
		// persister.persistObject(parentUUID, "<visitedClass name>", uuid, index);" 
		println(sb, tabs, 
				String.format("%s.%s(%s, \"%s\", %s, %s);",
						persisterField,
						PERSIST_OBJECT_METHOD_NAME,
						parentUUIDField,
						visitedClass.getName(),
						uuidField,
						indexField));
		
		//TODO pass in the actual exception types on any accessors
		//then replace this blanket try/catch with specifics for any accessor
		//that throws an exception.
		
		// List<String> preProcessedProperties = new ArrayList<String>();
		println(sb, tabs, 
				String.format("%s<%s> %s = new %s<%s>();",
						List.class.getSimpleName(),
						String.class.getSimpleName(),
						preProcessedProps,
						ArrayList.class.getSimpleName(),
						String.class.getSimpleName()));
		importedClassNames.add(ArrayList.class.getName());
		println(sb, tabs, "try {");
		tabs++;
		if (constructorParameters.isEmpty()) {
			println(sb, tabs, "// No constructor arguments");
		} else {
			println(sb, tabs, "// Constructor arguments");
			
			final String dataTypeField = "dataType";
			
			// DataType dataType;
			println(sb, tabs, 
					String.format("%s %s;",
							DataType.class.getSimpleName(),
							dataTypeField));
			// Persist all of its constructor argument properties.
			for (ConstructorParameterObject cpo : constructorParameters) {
				//XXX Should this only be properties?
				if (ParameterType.PROPERTY.equals(cpo.getProperty()) ||
						ParameterType.CHILD.equals(cpo.getProperty())) {
					
					String getPersistedProperty = objectField + "." + 
						SPAnnotationProcessorUtils.convertPropertyToAccessor(
								cpo.getName(), visitedClass) + "()";
					if (cpo.getType() == Object.class) {
						// if (castedObject.<getter>() == null) {
						println(sb, tabs, 
								String.format("if (%s == null) {", 
										getPersistedProperty));
						tabs++;
						// dataType = PersisterUtils.getDataType(null);
						println(sb, tabs, 
								String.format("%s = %s.getDataType(null);",
										dataTypeField,
										PersisterUtils.class.getSimpleName()));
						tabs--;
						println(sb, tabs, "} else {");
						tabs++;
						// dataType = PersisterUtils.getDataType(castedObject.<getter>().getClass());
						println(sb, tabs, 
								String.format("%s = %s.getDataType(%s.getClass());",
										dataTypeField,
										PersisterUtils.class.getSimpleName(),
										getPersistedProperty));
						tabs--;
						println(sb, tabs, "}");
						importedClassNames.add(PersisterUtils.class.getName());
					} else {
						// dataType = DataType.<type>;
						println(sb, tabs, 
								String.format("%s = %s.%s;",
										dataTypeField,
										DataType.class.getSimpleName(),
										PersisterUtils.getDataType(cpo.getType()).name()));
					}
					
					// persister.persistProperty(uuid, "<property>", dataType, converter, convertToBasicType(castedObject.<getter>()));
					println(sb, tabs, 
							String.format("%s.%s(%s, \"%s\", %s, %s.%s(%s.%s()));",
									persisterField,
									PERSIST_PROPERTY_METHOD_NAME,
									uuidField,
									cpo.getName(), //XXX we should convert this name as the constructor parameter name may be different than the property name defined by the accessor.
									dataTypeField,
									converterField,
									CONVERT_TO_BASIC_TYPE_METHOD_NAME,
									objectField,
									SPAnnotationProcessorUtils.convertPropertyToAccessor(
											cpo.getName(), 
											visitedClass)));
					
					// preProcessedProperties.add("<propertyName>");
					println(sb, tabs, 
							String.format("%s.add(\"%s\");",
									preProcessedProps,
									cpo.getName()));
					importedClassNames.add(DataType.class.getName());
				}
			}
		}
		niprintln(sb, "");
		
		// persistObjectProperties(o, persister, converter, preProcessedProperties);
		println(sb, tabs, 
				String.format("%s(%s, %s, %s, %s);",
						PERSIST_OBJECT_PROPERTIES_METHOD_NAME,
						genericObjectField,
						persisterField,
						converterField,
						preProcessedProps));
		
		tabs--;
		// } catch (Exception e) {
		println(sb, tabs, 
				String.format("} catch (%s %s) {",
						Exception.class.getSimpleName(),
						exceptionField));
		tabs++;
		
		// throw new SPPersistenceException(uuid, e);
		println(sb, tabs, 
				String.format("throw new %s(%s, %s);",
						SPPersistenceException.class.getSimpleName(),
						uuidField,
						exceptionField));
		tabs--;
		println(sb, tabs, "}");
		
		tabs--;
		println(sb, tabs, "}");
		
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
			Map<String, Class<?>> mutators,
			Set<String> propertiesToPersistOnlyIfNonNull,
			int tabs) {
		StringBuilder sb = new StringBuilder();
		final String genericObjectField = "o";
		final String objectField = "castedObject";
		final String persisterField = "persister";
		final String converterField = "converter";
		final String uuidField = "uuid";
		final String exceptionField = "e";
		
		//These are the properties on the sub-class helper calling this abstract
		//helper that have already been persisted by the current persistObject
		//call. These properties do not have to be persisted again.
		final String preProcessedPropField = "preProcessedProps";
		
		final String staticPreProcessedPropField = "staticPreProcessedProps";
		
		// persistObject method header.
		// public void persistObjectProperties(
		// 		SPObject o,
		// 		SPPersister persister,
		// 		SessionPersisterSuperConverter converter,
		// 		List<String> staticPreProcessedProps)
		// 		throws SPPersistenceException {
		println(sb, tabs, 
				String.format("public void %s(%s %s, %s %s, %s %s, %s<%s> %s) throws %s {",
						PERSIST_OBJECT_PROPERTIES_METHOD_NAME,
						SPObject.class.getSimpleName(),
						genericObjectField,
						SPPersister.class.getSimpleName(),
						persisterField,
						SessionPersisterSuperConverter.class.getSimpleName(),
						converterField,
						List.class.getSimpleName(),
						String.class.getSimpleName(),
						staticPreProcessedPropField,
						SPPersistenceException.class.getSimpleName()));
		tabs++;
		// final List<String> preProcessedProperties = new ArrayList<String>(staticPreProcessedProps);
		println(sb, tabs, 
				String.format("final %s<%s> %s = new %s<%s>(%s);",
						List.class.getSimpleName(),
						String.class.getSimpleName(),
						preProcessedPropField,
						ArrayList.class.getSimpleName(),
						String.class.getSimpleName(),
						staticPreProcessedPropField));
		
		if (!accessors.isEmpty()) {
			boolean lastEntryInIfBlock = false;
			boolean variablesInitialized = false;

			Set<String> getterPropertyNames = new HashSet<String>();
			for (Entry<String, Class<?>> e : mutators.entrySet()) {
				getterPropertyNames.add(SPAnnotationProcessorUtils.convertMethodToProperty(e.getKey()));
			}
			// Persist all of its persistable properties.
			for (Entry<String, Class<?>> e : accessors.entrySet()) {
				String propertyName = SPAnnotationProcessorUtils.convertMethodToProperty(
						e.getKey());
				if (!getterPropertyNames.contains(propertyName)) continue;
				
				if (!variablesInitialized) {
					// final String uuid = o.getUUID();
					println(sb, tabs, 
							String.format("final %s %s = %s.%s();\n",
									String.class.getSimpleName(),
									uuidField,
									genericObjectField,
									GET_UUID_METHOD_NAME));
					
					// If the SPObject class this persister helper handles is abstract,
					// use the type generic defined in the class header.
					// Otherwise, use the SPObject class directly.
					if (Modifier.isAbstract(visitedClass.getModifiers())) {
						// T castedObject = (T) o;
						println(sb, tabs, 
								String.format("%s %s = (%s) %s;",
										TYPE_GENERIC_PARAMETER,
										objectField,
										TYPE_GENERIC_PARAMETER,
										genericObjectField));
					} else {
						// <visitedClass> castedObject = (<visitedClass>) o;
						println(sb, tabs,
								String.format("%s %s = (%s) %s;",
										visitedClass.getSimpleName(),
										objectField,
										visitedClass.getSimpleName(),
										genericObjectField));
					}
					
					// try {
					println(sb, tabs, "try {");
					tabs++;
					variablesInitialized = true;
				}

				// Persist the property only if it has not been persisted yet
				// and (if required) persist if the value is not null.
				//
				// if (preProcessedProperties.contains("<property>")) {
				println(sb, tabs, 
						String.format("if (!%s.contains(\"%s\")) {",
								preProcessedPropField,
								propertyName));
				tabs++;
				boolean persistOnlyIfNonNull = 
					propertiesToPersistOnlyIfNonNull.contains(propertyName);
				String propertyField = objectField + "." + e.getKey() + "()";

				if (lastEntryInIfBlock) {
					niprintln(sb, "");
				}

				if (persistOnlyIfNonNull) {
					// <getter type> <property> = castedObject.<getter>();
					println(sb, tabs, 
							String.format("%s %s = %s.%s();",
									e.getValue().getSimpleName(),
									propertyName,
									propertyField));
					propertyField = propertyName;

					// if (castedObject.<getter>() != null) {
					println(sb, tabs, 
							String.format("if (%s != null) {",
									propertyField));
					tabs++;
				}

				final String dataTypeField = "dataType";
				// DataType dataType;
				println(sb, tabs, 
						String.format("%s %s;",
								DataType.class.getSimpleName(),
								dataTypeField));

				String getPersistedProperty = objectField + "." + e.getKey() + "()";
				if (e.getValue() == Object.class) {
					// if (castedObject.<getter>() == null) {
					println(sb, tabs, 
							String.format("if (%s == null) {",
									getPersistedProperty));
					tabs++;
					// dataType = PersisterUtils.getDataType(null);
					println(sb, tabs, 
							String.format("%s = %s.getDataType(null);",
									dataTypeField,
									PersisterUtils.class.getSimpleName()));
					tabs--;
					println(sb, tabs, "} else {");
					tabs++;

					// dataType = PersisterUtils.getDataType(castedObject.<getter>().getClass());
					println(sb, tabs, 
							String.format("%s = %s.getDataType(%s.getClass());",
									dataTypeField,
									PersisterUtils.class.getSimpleName(),
									getPersistedProperty));
					tabs--;
					println(sb, tabs, "}");
					importedClassNames.add(PersisterUtils.class.getName());
				} else {
					// dataType = DataType.<type>;
					println(sb, tabs, 
							String.format("%s = %s.%s;",
									dataTypeField,
									DataType.class.getSimpleName(),
									PersisterUtils.getDataType(e.getValue()).name()));
				}
				// persister.persistProperty(uuid, "<property>", dataType, converter.convertToBasicType(castedObject.<getter>()));
				println(sb, tabs, 
						String.format("%s.%s(%s, \"%s\", %s, %s.%s(%s));",
								persisterField,
								PERSIST_PROPERTY_METHOD_NAME,
								uuidField,
								propertyName,
								dataTypeField,
								converterField,
								CONVERT_TO_BASIC_TYPE_METHOD_NAME,
								getPersistedProperty));
				// preProcessedProperties.add("<property>");
				println(sb, tabs, 
						String.format("%s.add(\"%s\");",
								preProcessedPropField,
								propertyName));
				importedClassNames.add(DataType.class.getName());

				if (persistOnlyIfNonNull) {
					tabs--;
					println(sb, tabs, "}");
					lastEntryInIfBlock = true;
				} else {
					lastEntryInIfBlock = false;
				}
				tabs--;
				println(sb, tabs, "}");
			}
			
			if (variablesInitialized) {
				tabs--;
				// } catch (Exception e) {
				println(sb, tabs, 
						String.format("} catch (%s %s) {",
								Exception.class.getSimpleName(),
								exceptionField));
				tabs++;

				// throw new SPPersistenceException(uuid, e);
				println(sb, tabs, 
						String.format("throw new %s(%s, %s);",
								SPPersistenceException.class.getSimpleName() ,
								uuidField,
								exceptionField));
				tabs--;
				println(sb, tabs, "}");
			}
		}

		if (SPObject.class.isAssignableFrom(visitedClass.getSuperclass())) {
			Class<?> superclass = visitedClass.getSuperclass();
			
			// super.persistObjectProperties(o, persister, converter, preProcessedProperties);
			println(sb, tabs, 
					String.format("super.%s(%s, %s, %s, %s);",
							PERSIST_OBJECT_PROPERTIES_METHOD_NAME,
							genericObjectField,
							persisterField,
							converterField,
							preProcessedPropField));
		}
		
		tabs--;
		println(sb, tabs, "}");
		
		return sb.toString();
	}
	
	//-------------- helper methods for dealing with string buffer, there may be a class that already does this

	/**
	 * Appends a number of tab characters, a given {@link String}, and a new
	 * line character to a {@link StringBuilder}. This method generates a line
	 * of Java code.
	 * 
	 * @param sb
	 *            The {@link StringBuilder} to append to.
	 * @param tabs
	 *            The number of tab characters to indent.
	 * @param s
	 *            The {@link String} to append.
	 */
	private void println(StringBuilder sb, int tabs, String s) {
		print(sb, tabs, s + "\n");
	}

	/**
	 * Appends a number of tab characters, and a given {@link String} to a
	 * {@link StringBuilder}. This method generates a line or a portion of a
	 * line of Java code without adding a new line character at the end.
	 * 
	 * @param sb
	 *            The {@link StringBuilder} to append to.
	 * @param tabs
	 *            The number of tab characters to indent.
	 * @param s
	 *            The {@link String} to append.
	 */
	private void print(StringBuilder sb, int tabs, String s) {
		sb.append(indent(tabs));
		sb.append(s);
	}

	/**
	 * Appends a given {@link String}, and a new line character to a
	 * {@link StringBuilder}. This method generates a portion of a line of Java
	 * code without indenting.
	 * 
	 * @param sb
	 *            The {@link StringBuilder} to append to.
	 * @param s
	 *            The {@link String} to append.
	 */
	private void niprintln(StringBuilder sb, String s) {
		niprint(sb, s + "\n");
	}

	/**
	 * Appends a given {@link String} to a {@link StringBuilder}. This method
	 * generates a portion of a line of Java code without indenting or adding a
	 * new line character at the end.
	 * 
	 * @param sb
	 *            The {@link StringBuilder} to append to.
	 * @param s
	 *            The {@link String} to append.
	 */
	private void niprint(StringBuilder sb, String s) {
		sb.append(s);
	}
	
	//-------------- end helper methods

}
