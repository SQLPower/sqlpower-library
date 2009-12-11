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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import ca.sqlpower.dao.SPPersisterHelper;
import ca.sqlpower.object.SPObject;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.AnnotationTypeElementDeclaration;
import com.sun.mirror.declaration.ClassDeclaration;
import com.sun.mirror.declaration.ConstructorDeclaration;
import com.sun.mirror.declaration.Declaration;
import com.sun.mirror.declaration.EnumConstantDeclaration;
import com.sun.mirror.declaration.EnumDeclaration;
import com.sun.mirror.declaration.ExecutableDeclaration;
import com.sun.mirror.declaration.FieldDeclaration;
import com.sun.mirror.declaration.InterfaceDeclaration;
import com.sun.mirror.declaration.MemberDeclaration;
import com.sun.mirror.declaration.MethodDeclaration;
import com.sun.mirror.declaration.PackageDeclaration;
import com.sun.mirror.declaration.ParameterDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.declaration.TypeParameterDeclaration;
import com.sun.mirror.type.ClassType;
import com.sun.mirror.type.InterfaceType;
import com.sun.mirror.type.PrimitiveType;
import com.sun.mirror.type.ReferenceType;
import com.sun.mirror.type.TypeMirror;
import com.sun.mirror.type.PrimitiveType.Kind;
import com.sun.mirror.util.DeclarationVisitor;

/**
 * This {@link DeclarationVisitor} is used to visit {@link SPObject} classes
 * annotated with {@link Persistable}. For each constructor and method, this
 * visitor looks for the {@link Constructor}, {@link ConstructorParameter},
 * {@link Accessor} and {@link Mutator} annotations and keeps track of data
 * required by the {@link SPAnnotationProcessor} to generate persister helper
 * classes.
 */
public class SPClassVisitor implements DeclarationVisitor {

	/**
	 * @see #getVisitedClass()
	 */
	private Class<? extends SPObject> visitedClass;

	/**
	 * @see #getConstructorParameters()
	 */
	private Map<String, Class<?>> constructorParameters = new LinkedHashMap<String, Class<?>>();
	
	/**
	 * @see #getPropertiesToAccess()
	 */
	private Map<String, Class<?>> propertiesToAccess = new HashMap<String, Class<?>>();
	
	/**
	 * @see #getPropertiesToMutate()
	 */
	private Map<String, Class<?>> propertiesToMutate = new HashMap<String, Class<?>>();
	
	/**
	 * @see #getMutatorThrownTypes()
	 */
	private Multimap<String, Class<? extends Exception>> mutatorThrownTypes = HashMultimap.create();
	
	/**
	 * @see #getImports()
	 */
	private Set<String> imports = new HashSet<String>();
	
	/**
	 * @see #propertiesToPersistOnlyIfNonNull
	 */
	private Set<String> propertiesToPersistOnlyIfNonNull = new HashSet<String>();

	/**
	 * Returns the {@link SPObject} class this {@link DeclarationVisitor} is
	 * visiting to parse annotations.
	 */
	public Class<? extends SPObject> getVisitedClass() {
		return visitedClass;
	}

	/**
	 * Returns the {@link Map} of constructor arguments that are required to
	 * create a new {@link SPObject} of type {@link #visitedClass}. The order of
	 * this map is guaranteed.
	 */
	public Map<String, Class<?>> getConstructorParameters() {
		return Collections.unmodifiableMap(constructorParameters);
	}

	/**
	 * Returns the {@link Map} of JavaBean getter method names that access
	 * persistable properties, mapped to their property types.
	 */
	public Map<String, Class<?>> getPropertiesToAccess() {
		return Collections.unmodifiableMap(propertiesToAccess);
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
	 * persistable properties, mapped to the method's thrown types.
	 */
	public Multimap<String, Class<? extends Exception>> getMutatorThrownTypes() {
		return Multimaps.unmodifiableMultimap(mutatorThrownTypes);
	}

	/**
	 * Returns the {@link Set} of imports that are required for an
	 * {@link SPPersisterHelper} to use that deals with type
	 * {@link #visitedClass} which includes dependencies of the
	 * {@link ConstructorParameter} annotated constructor parameters and the
	 * getters/setters annotated with {@link Accessor} and {@link Mutator}.
	 */
	public Set<String> getImports() {
		return Collections.unmodifiableSet(imports);
	}

	/**
	 * Returns the {@link Set} of persistable properties that can only be
	 * persisted if its value is not null.
	 */
	public Set<String> getPropertiesToPersistOnlyIfNonNull() {
		return Collections.unmodifiableSet(propertiesToPersistOnlyIfNonNull);
	}
	
	@SuppressWarnings("unchecked")
	public void visitClassDeclaration(ClassDeclaration d) {
		if (d.getAnnotation(Persistable.class) != null) {
			try {
				visitedClass = (Class<? extends SPObject>) Class.forName(d.getQualifiedName());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void visitConstructorDeclaration(ConstructorDeclaration d) {
		if (d.getAnnotation(Constructor.class) != null) {
			for (ParameterDeclaration pd : d.getParameters()) {
				ConstructorParameter cp = pd.getAnnotation(ConstructorParameter.class);
				if (cp != null) {
					TypeMirror type = pd.getType();
					
					if (type instanceof PrimitiveType) {
						constructorParameters.put(cp.value(), 
								convertPrimitiveToClass(((PrimitiveType) type).getKind()));
						
					} else if (type instanceof ClassType || type instanceof InterfaceType) {
						try {
							Class<?> c = Class.forName(pd.getType().toString());
							constructorParameters.put(cp.value(), c);
							imports.add(c.getName());
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
						
					}
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void visitMethodDeclaration(MethodDeclaration d) {
		Accessor accessorAnnotation = d.getAnnotation(Accessor.class);
		Mutator mutatorAnnotation = d.getAnnotation(Mutator.class);
		TypeMirror type = null;
		
		if (accessorAnnotation != null) {
			type = d.getReturnType();
		} else if (mutatorAnnotation != null) {
			type = d.getParameters().iterator().next().getType();
		} else {
			return;
		}
		
		String methodName = d.getSimpleName();
		Class<?> c = null;

		try {
			if (type instanceof PrimitiveType) {
				c = convertPrimitiveToClass(((PrimitiveType) type).getKind());
			} else if (type instanceof ClassType || type instanceof InterfaceType) {
				c = Class.forName(type.toString());
			}

			if (accessorAnnotation != null) {
				propertiesToAccess.put(methodName, c);
				if (!accessorAnnotation.value()) {
					propertiesToPersistOnlyIfNonNull.add(
							SPAnnotationProcessor.convertMethodToProperty(methodName));
				}
				
			} else {
				for (ReferenceType refType : d.getThrownTypes()) {
					Class<? extends Exception> thrownType = 
						(Class<? extends Exception>) Class.forName(refType.toString());
					mutatorThrownTypes.put(methodName, thrownType);
					imports.add(thrownType.getName());
				}

				propertiesToMutate.put(methodName, c);
			}
			
			imports.add(c.getName());
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void visitAnnotationTypeDeclaration(AnnotationTypeDeclaration d) {
		// no-op
	}

	public void visitAnnotationTypeElementDeclaration(AnnotationTypeElementDeclaration d) {
		// no-op
	}

	public void visitDeclaration(Declaration d) {
		// no-op
	}

	public void visitEnumConstantDeclaration(EnumConstantDeclaration d) {
		// no-op
	}

	public void visitEnumDeclaration(EnumDeclaration d) {
		// no-op
	}

	public void visitExecutableDeclaration(ExecutableDeclaration d) {
		// no-op
	}

	public void visitFieldDeclaration(FieldDeclaration d) {
		// no-op		
	}

	public void visitInterfaceDeclaration(InterfaceDeclaration d) {
		// no-op		
	}

	public void visitMemberDeclaration(MemberDeclaration d) {
		// no-op		
	}

	public void visitPackageDeclaration(PackageDeclaration d) {
		// no-op		
	}

	public void visitParameterDeclaration(ParameterDeclaration d) {
		// no-op		
	}

	public void visitTypeDeclaration(TypeDeclaration d) {
		// no-op		
	}

	public void visitTypeParameterDeclaration(TypeParameterDeclaration d) {
		// no-op		
	}

	/**
	 * Converts a primitive data type to the equivalent {@link Class} type.
	 * 
	 * @param kind
	 *            The {@link PrimitiveType.Kind} to convert.
	 * @return The equivalent {@link Class} of the primitive type.
	 */
	private Class<?> convertPrimitiveToClass(Kind kind) {
		if (kind.equals(Kind.BOOLEAN)) {
			return Boolean.class;
		} else if (kind.equals(Kind.BYTE)) {
			return Byte.class;
		} else if (kind.equals(Kind.CHAR)) {
			return Character.class;
		} else if (kind.equals(Kind.DOUBLE)) {
			return Double.class;
		} else if (kind.equals(Kind.FLOAT)) {
			return Float.class;
		} else if (kind.equals(Kind.INT)) {
			return Integer.class;
		} else if (kind.equals(Kind.LONG)) {
			return Long.class;
		} else if (kind.equals(Kind.SHORT)) {
			return Short.class;
		} else {
			throw new IllegalStateException("The PrimitiveType Kind " + 
					kind.name() + " is not recognized. This exception " +
							"should never be thrown.");
		}
	}

}
