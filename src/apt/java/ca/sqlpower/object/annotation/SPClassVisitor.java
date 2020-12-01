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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.AbstractElementVisitor8;

import org.apache.log4j.Logger;

import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.dao.helper.SPPersisterHelper;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.ConstructorParameter.ParameterType;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * This {@link DeclarationVisitor} is used to visit {@link SPObject} classes
 * annotated with {@link Persistable}. For each constructor and method, this
 * visitor looks for the {@link Constructor}, {@link ConstructorParameter},
 * {@link Accessor} and {@link Mutator} annotations and keeps track of data
 * required by the {@link SPAnnotationProcessor} to generate
 * {@link SPPersisterHelper} classes.
 */
public class SPClassVisitor extends AbstractElementVisitor8<Void, Void> {
	
	
	private static final Logger logger = Logger.getLogger(SPClassVisitor.class);
	/**
	 * @see #isValid()
	 */
	private boolean valid = true;

	/**
	 * @see #getVisitedClass()
	 */
	private Class<? extends SPObject> visitedClass;
	
	/**
	 * @see #getConstructorParameters()
	 */
	private List<ConstructorParameterObject> constructorParameters = new ArrayList<ConstructorParameterObject>();
	
	/**
	 * @see #getPropertiesToAccess()
	 */
	private Map<String, Class<?>> propertiesToAccess = new HashMap<String, Class<?>>();
	
	/**
	 * @see #getAccessorAdditionalInfo()
	 */
	private Multimap<String, String> accessorAdditionalInfo = LinkedHashMultimap.create();
	
	/**
	 * @see #getPropertiesToMutate()
	 */
	private Map<String, Class<?>> propertiesToMutate = new HashMap<String, Class<?>>();
	
	/**
	 * @see #getMutatorExtraParameters()
	 */
	private Multimap<String, MutatorParameterObject> mutatorExtraParameters = LinkedHashMultimap.create();
	
	/**
	 * @see #getMutatorThrownTypes()
	 */
	private Multimap<String, Class<? extends Exception>> mutatorThrownTypes = HashMultimap.create();
	
	/**
	 * @see #getConstructorImports()
	 */
	private Set<String> constructorImports = new HashSet<String>();
	
	/**
	 * @see #getMutatorImports()
	 */
	private Multimap<String, String> mutatorImports = HashMultimap.create();
	
	/**
	 * @see #propertiesToPersistOnlyIfNonNull
	 */
	private Set<String> propertiesToPersistOnlyIfNonNull = new HashSet<String>();
	
	/**
	 * If true then an annotated constructor has been recently found for the current
	 * class being visited. We must only have one constructor per class. 
	 */
	private boolean constructorFound = false;

	/**
	 * Returns whether the visited class along with all its annotated elements is valid.
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * Returns the {@link SPObject} class this {@link DeclarationVisitor} is
	 * visiting to parse annotations.
	 */
	public Class<? extends SPObject> getVisitedClass() {
		return visitedClass;
	}
	
	/**
	 * Returns the {@link List} of constructor arguments that are required to
	 * create a new {@link SPObject} of type {@link #visitedClass}. The order of
	 * this list is guaranteed.
	 */
	public List<ConstructorParameterObject> getConstructorParameters() {
		return Collections.unmodifiableList(constructorParameters);
	}

	/**
	 * Returns the {@link Map} of JavaBean getter method names that access
	 * persistable properties, mapped to their property types.
	 */
	public Map<String, Class<?>> getPropertiesToAccess() {
		return Collections.unmodifiableMap(propertiesToAccess);
	}

	/**
	 * Returns the {@link Multimap} of JavaBean getter method names that access
	 * persistable properties, mapped to additional property names that the
	 * session {@link SPPersister} requires to convert the accessor's returned
	 * value into a basic persistable type. The order of this {@link Multimap}
	 * is guaranteed.
	 * 
	 * @see Accessor#additionalInfo()
	 */
	public Multimap<String, String> getAccessorAdditionalInfo() {
		return Multimaps.unmodifiableMultimap(accessorAdditionalInfo);
	}

	/**
	 * Returns the {@link Map} of JavaBean setter method names that mutate
	 * persistable properties, mapped to their property types.
	 */
	public Map<String, Class<?>> getPropertiesToMutate() {
		return Collections.unmodifiableMap(propertiesToMutate);
	}

	/**
	 * Returns the {@link Multimap} of JavaBean setter method names that mutate
	 * persistable properties, mapped to {@link MutatorParameterObject}s that
	 * contain values for an {@link SPPersister} to use. The order of this
	 * {@link Multimap} is guaranteed.
	 */
	public Multimap<String, MutatorParameterObject> getMutatorExtraParameters() {
		return Multimaps.unmodifiableMultimap(mutatorExtraParameters);
	}

	/**
	 * Returns the {@link Multimap} of JavaBean setter method names that mutate
	 * persistable properties, mapped to the method's thrown types.
	 */
	public Multimap<String, Class<? extends Exception>> getMutatorThrownTypes() {
		return Multimaps.unmodifiableMultimap(mutatorThrownTypes);
	}

	/**
	 * Returns the {@link Set} of imports that are required for an
	 * {@link SPPersisterHelper} to use that deals with {@link SPObject}s of
	 * type {@link #visitedClass} which includes dependencies of the
	 * {@link ConstructorParameter} annotated constructor parameters.
	 */
	public Set<String> getConstructorImports() {
		return Collections.unmodifiableSet(constructorImports);
	}

	/**
	 * Returns the {@link Multimap} of {@link Mutator} annotated setter methods to
	 * required imports needed to generate {@link SPPersisterHelper}s that deal
	 * with {@link SPObject}s of type {@link #visitedClass}, which include
	 * thrown exception types.
	 */
	public Multimap<String, String> getMutatorImports() {
		return mutatorImports;
	}

	/**
	 * Returns the {@link Set} of persistable properties that can only be
	 * persisted if its value is not null.
	 */
	public Set<String> getPropertiesToPersistOnlyIfNonNull() {
		return Collections.unmodifiableSet(propertiesToPersistOnlyIfNonNull);
	}

	/**
	 * Stores information about constructors annotated with {@link Constructor},
	 * particularly with the {@link ConstructorParameter} annotated parameters
	 * and their required imports. The {@link SPAnnotationProcessor} takes this
	 * information and generates
	 * {@link SPPersisterHelper#commitObject(ca.sqlpower.dao.PersistedSPObject, Multimap, List, ca.sqlpower.dao.helper.SPPersisterHelperFactory)}
	 * and
	 * {@link SPPersisterHelper#persistObject(SPObject, int, SPPersister, ca.sqlpower.dao.session.SessionPersisterSuperConverter)}
	 * methods.
	 * 
	 * @param d
	 *            The {@link ConstructorDeclaration} of the constructor to
	 *            visit.
	 */
	public void visitConstructorDeclaration(ExecutableElement d) {
		/*Using d.getEnclosingElement().getSimpleName() to get the correct name because d.getSimpleName() return <init> for each  constructor
		 * which never match with rootElement.getSimpleName() and it will call a default constructor in generator method gives error for not matching constructor with parameter */
		logger.debug(" name :[ "+d.getSimpleName()+"] getEnclosingElement name:["+d.getEnclosingElement().getSimpleName()+ "] and parameter : ["+d.getParameters()+" ] roorElement name: "+rootElement.getSimpleName());

		if (!constructorFound && d.getAnnotation(Constructor.class) != null
				&& d.getEnclosingElement().getSimpleName().equals(rootElement.getSimpleName())) {
			for (VariableElement pd : d.getParameters()) {
				/**
				 * Using getAnnotation to get the parameter of Construction.
				 * Do not use getAnnotationsByType it just simply return  'ca.sqlpower.object.annotation.ConstructorParameter'
				 */
				ConstructorParameter param = pd.getAnnotation(ConstructorParameter.class);
				//logger.debug("param: "+param);
				if (param != null) {
					try {
						TypeMirror type = pd.asType();
						Class<?> c = SPAnnotationProcessorUtils.convertTypeMirrorToClass(type);

						ParameterType property = param.parameterType();
						String name;

						if (property.equals(ParameterType.PROPERTY)) {
							name = param.propertyName();
						} else {
							name = pd.getSimpleName().toString();
						}
						if (type instanceof PrimitiveType) {
							constructorParameters.add(
									new ConstructorParameterObject(property, c, name));

						} else if (type instanceof DeclaredType) {
							constructorParameters.add(
									new ConstructorParameterObject(property, c, name));
							constructorImports.add(c.getName());
						}
					} catch (ClassNotFoundException e) {
						valid = false;
						e.printStackTrace();
					}
				} 
			}
			constructorFound = true;
		}
	}
	
	/**
	 * Stores information about getter and setter methods annotated with
	 * {@link Accessor} and {@link Mutator}. This includes thrown exceptions,
	 * setter parameters annotated with {@link MutatorParameter}, and required
	 * imports. The {@link SPAnnotationProcessor} takes this information and
	 * generates
	 * {@link SPPersisterHelper#commitProperty(SPObject, String, Object, ca.sqlpower.dao.session.SessionPersisterSuperConverter)}
	 * and
	 * {@link SPPersisterHelper#findProperty(SPObject, String, ca.sqlpower.dao.session.SessionPersisterSuperConverter)}
	 * methods.
	 * 
	 * @param d
	 *            The {@link MethodDeclaration} of the method to visit.
	 */
	public void visitMethodDeclaration(ExecutableElement d) {
		Accessor accessorAnnotation = d.getAnnotation(Accessor.class);
		Mutator mutatorAnnotation = d.getAnnotation(Mutator.class);
		Transient transientAnnotation = d.getAnnotation(Transient.class);
		TypeMirror type = null;
		
		if (!d.getEnclosingElement().equals(rootElement)) return;

		if (accessorAnnotation != null && transientAnnotation == null) {
			type = d.getReturnType();
		} else if (mutatorAnnotation != null && transientAnnotation == null) {
			type = d.getParameters().iterator().next().asType();
		} else {
			return;
		}
		
		String methodName = d.getSimpleName().toString();
		Class<?> c = null;
		
		try {
			
			c = SPAnnotationProcessorUtils.convertTypeMirrorToClass(type);

			if (!propertiesToAccess.containsKey(methodName) && accessorAnnotation != null) {
				propertiesToAccess.put(methodName, c);
				
				if (accessorAnnotation.persistOnlyIfNonNull()) {
					propertiesToPersistOnlyIfNonNull.add(
							SPAnnotationProcessorUtils.convertMethodToProperty(methodName));
				}
				accessorAdditionalInfo.putAll(
						methodName, Arrays.asList(accessorAnnotation.additionalInfo()));
				
			} else if (!propertiesToMutate.containsKey(methodName) && mutatorAnnotation != null) {
				for (TypeMirror refType : d.getThrownTypes()) {
					Class<? extends Exception> thrownType = 
						(Class<? extends Exception>) Class.forName(refType.toString());
					mutatorThrownTypes.put(methodName, thrownType);
					mutatorImports.put(methodName, thrownType.getName());
				}

				propertiesToMutate.put(methodName, c);
				mutatorImports.put(methodName, c.getName());
				
				for (TypeParameterElement pd : d.getTypeParameters()) {
					MutatorParameter mutatorParameterAnnotation = 
						pd.getAnnotation(MutatorParameter.class);
					
					if (mutatorParameterAnnotation != null) {
						Class<?> extraParamType = 
							SPAnnotationProcessorUtils.convertTypeMirrorToClass(pd.asType());
						mutatorExtraParameters.put(methodName, 
								new MutatorParameterObject(
										extraParamType,
										pd.getSimpleName().toString(), 
										mutatorParameterAnnotation.value()));
						mutatorImports.put(methodName, extraParamType.getName());
					}
				}
			}
			
		} catch (ClassNotFoundException e) {
			valid = false;
			e.printStackTrace();
		}
	}

	@Override
	public Void visitPackage(PackageElement e, Void p) {
		// no-op
		return null;
	}

	private TypeElement rootElement = null;
	
	/**
	 * Stores the class reference of a {@link Persistable} {@link SPObject} for
	 * use in annotation processing in the {@link SPAnnotationProcessor}. The
	 * processor takes this information to generate {@link SPPersisterHelper}s.
	 * 
	 * @param d
	 *            The {@link ClassDeclaration} of the class to visit.
	 */
	//This is the method that visits classes
	@Override
	public Void visitType(TypeElement e, Void p) {
		if (rootElement == null) {
			rootElement = e;
		} else {
			return null;
		}
		if (e.getAnnotation(Persistable.class) != null) {
			if (e.getAnnotation(Persistable.class).isTransient()) {
				valid = false;
				return null;
			}
			try {
				String qualifiedName = 
						SPAnnotationProcessorUtils.convertTypeDeclarationToQualifiedName(e);
				visitedClass = (Class<? extends SPObject>) Class.forName(qualifiedName);
				
				if (java.lang.reflect.Modifier.isPrivate(visitedClass.getModifiers())) {
					valid = false;
				}
			} catch (ClassNotFoundException ex) {
				valid = false;
				ex.printStackTrace();
			}
		}
		for (Element innerElement : e.getEnclosedElements()) {
			visit(innerElement);
		}
		return null;
	}
	

	@Override
	public Void visitVariable(VariableElement e, Void p) {
		// TODO Auto-generated method stub
		return null;
	}

	//This method is for methods and constructors
	@Override
	public Void visitExecutable(ExecutableElement e, Void p) {
		if (e.getKind() == ElementKind.CONSTRUCTOR && valid) {
			visitConstructorDeclaration(e);
		} else if (e.getKind() == ElementKind.METHOD) {
			visitMethodDeclaration(e);
		}
		return null;
	}

	@Override
	public Void visitTypeParameter(TypeParameterElement e, Void p) {
		// TODO Auto-generated method stub
		return null;
	}

}
