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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

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
	private LinkedHashMap<String, Class<?>> constructorParameters = 
		new LinkedHashMap<String, Class<?>>();
	
	/**
	 * @see #getPropertiesToAccess()
	 */
	private Map<String, Class<?>> propertiesToAccess = new HashMap<String, Class<?>>();
	
	/**
	 * @see #getPropertiesToMutate()
	 */
	private Map<String, Class<?>> propertiesToMutate = new HashMap<String, Class<?>>();

	/**
	 * Returns the {@link SPObject} class this {@link DeclarationVisitor} is
	 * visiting to parse annotations.
	 */
	public Class<? extends SPObject> getVisitedClass() {
		return visitedClass;
	}

	/**
	 * Returns the {@link LinkedHashMap} of constructor arguments that are required to
	 * create a new {@link SPObject} of type {@link #visitedClass}.
	 */
	public LinkedHashMap<String, Class<?>> getConstructorParameters() {
		return constructorParameters;
	}

	/**
	 * Returns the {@link Map} of JavaBean property names that can be accessed
	 * by getters.
	 */
	public Map<String, Class<?>> getPropertiesToAccess() {
		return propertiesToAccess;
	}

	/**
	 * Returns the {@link Map} of JavaBean property names that can be mutated
	 * by setters.
	 */
	public Map<String, Class<?>> getPropertiesToMutate() {
		return propertiesToMutate;
	}
	
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
						constructorParameters.put(cp.propertyName(), 
								convertPrimitiveToClass(((PrimitiveType) type).getKind()));
					} else {
						try {
							constructorParameters.put(cp.propertyName(), 
									Class.forName(pd.getType().toString()));
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
				}
			} else if (annotationName.equals(Mutator.class.getCanonicalName())) {
				// Since we cannot determine the property type this setter deals with,
				// we will iterate through the map of accessors to get that information.
				// The maps of accessors and mutators should have a one-to-one relationship.
				propertiesToMutate.put(d.getSimpleName(), null);
			}
		}
		
		for (Entry<String, Class<?>> e : propertiesToAccess.entrySet()) {
			String methodName = SPAnnotationProcessor.convertMethodToProperty(e.getKey());
			methodName = "set" + methodName.substring(0, 1).toUpperCase() + 
					((methodName.length() > 1)? methodName.substring(1) : "");
			if (propertiesToMutate.containsKey(methodName)) {
				propertiesToMutate.put(methodName, e.getValue());
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
