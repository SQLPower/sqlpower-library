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
import java.util.NoSuchElementException;
import java.util.Set;

import ca.sqlpower.dao.SPPersisterHelper;
import ca.sqlpower.object.SPObject;

import com.sun.mirror.declaration.AnnotationMirror;
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
import com.sun.mirror.type.PrimitiveType;
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
	 * @see #getImports()
	 */
	private Set<String> imports = new HashSet<String>();

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
	 * Returns the {@link Map} of JavaBean property names that can be accessed
	 * by getters.
	 */
	public Map<String, Class<?>> getPropertiesToAccess() {
		return Collections.unmodifiableMap(propertiesToAccess);
	}

	/**
	 * Returns the {@link Map} of JavaBean property names that can be mutated
	 * by setters.
	 */
	public Map<String, Class<?>> getPropertiesToMutate() {
		return Collections.unmodifiableMap(propertiesToMutate);
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
	
	@SuppressWarnings("unchecked")
	public void visitClassDeclaration(ClassDeclaration d) {
		if (d.getAnnotation(Persistable.class) != null) {
			try {
				visitedClass = (Class<? extends SPObject>) Class.forName(d.getQualifiedName());
				imports.add(visitedClass.getName());
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
						constructorParameters.put(cp.propertyName(), 
								convertPrimitiveToClass(((PrimitiveType) type).getKind()));
						// There is no need to add an import for primitive types
						// as they rely on java.lang and are automatically imported.
						
					} else {
						try {
							Class<?> c = Class.forName(pd.getType().toString());
							constructorParameters.put(cp.propertyName(), c);
							
							String pkg = c.getPackage().getName();
							// java.lang does not need to be imported as it is
							// done automatically.
							if (!pkg.equals("java.lang")) {
								imports.add(pkg);
							}
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	public void visitMethodDeclaration(MethodDeclaration d) {
		for (AnnotationMirror am : d.getAnnotationMirrors()) {
			String annotationName = am.getAnnotationType().getDeclaration().getQualifiedName();
			if (annotationName.equals(Accessor.class.getCanonicalName())) {
				
				Class<?> c = null;
				if (d.getReturnType() instanceof PrimitiveType) {
					c = convertPrimitiveToClass(((PrimitiveType) d.getReturnType()).getKind());
				} else {
					try {
						c = Class.forName(d.getReturnType().toString());
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
				
				if (c != null) {
					propertiesToAccess.put(d.getSimpleName(), c);
					String pkg = c.getPackage().getName();
					if (!pkg.equals("java.lang")) {
						imports.add(pkg);
					}
				}
			} else if (annotationName.equals(Mutator.class.getCanonicalName())) {
				try {
					TypeMirror type = d.getParameters().iterator().next().getType();
					Class<?> c = null;

					if (type instanceof PrimitiveType) {
						c = convertPrimitiveToClass(((PrimitiveType) type).getKind());
					} else {
						c = Class.forName(type.toString());
					}
					
					propertiesToMutate.put(d.getSimpleName(), c);
					String pkg = c.getPackage().getName();
					if (!pkg.equals("java.lang")) {
						imports.add(pkg);
					}
					
				} catch (NoSuchElementException e) {
					// This exception is caught if the Mutator annotated method
					// does not take parameters. The setter must take only one parameter
					// which is the same type as the property type it is trying to set.
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				
			}
		}
	}

	public void visitAnnotationTypeDeclaration(AnnotationTypeDeclaration d) {
		// no-op
	}

	public void visitAnnotationTypeElementDeclaration(
			AnnotationTypeElementDeclaration d) {
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
