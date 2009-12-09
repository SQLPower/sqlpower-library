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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

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
	private Set<String> propertiesToAccess = new HashSet<String>();
	
	/**
	 * @see #getPropertiesToMutate()
	 */
	private Set<String> propertiesToMutate = new HashSet<String>();

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
	 * Returns the {@link Set} of JavaBean property names that can be
	 * accessed by getters.
	 */
	public Set<String> getPropertiesToAccess() {
		return propertiesToAccess;
	}

	/**
	 * Returns the {@link Set} of JavaBean property names that can be mutated
	 * by setters.
	 */
	public Set<String> getPropertiesToMutate() {
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
						Kind kind = ((PrimitiveType) type).getKind();
						Class<?> c = null;
						
						if (kind.equals(Kind.BOOLEAN)) {
							c = Boolean.class;
						} else if (kind.equals(Kind.BYTE)) {
							c = Byte.class;
						} else if (kind.equals(Kind.CHAR)) {
							c = Character.class;
						} else if (kind.equals(Kind.DOUBLE)) {
							c = Double.class;
						} else if (kind.equals(Kind.FLOAT)) {
							c = Float.class;
						} else if (kind.equals(Kind.INT)) {
							c = Integer.class;
						} else if (kind.equals(Kind.LONG)) {
							c = Long.class;
						} else if (kind.equals(Kind.SHORT)) {
							c = Short.class;
						} else {
							throw new IllegalStateException("The PrimitiveType Kind " + 
									kind.name() + " is not recognized. This exception " +
											"should never be thrown.");
						}
						
						constructorParameters.put(cp.propertyName(), c);
						
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
		String methodName = d.getSimpleName();
		String propertyName;
		
		try {
			propertyName = convertMethodToProperty(methodName);
		} catch (IllegalArgumentException e) {
			// Method name does not follow JavaBeans format.
			return;
		}
		
		for (AnnotationMirror am : d.getAnnotationMirrors()) {
			String annotationName = am.getAnnotationType().getDeclaration().getQualifiedName();
			if (annotationName.equals(Accessor.class.getCanonicalName())) {
				propertiesToAccess.add(propertyName);
			} else if (annotationName.equals(Mutator.class.getCanonicalName())) {
				propertiesToMutate.add(propertyName);
			}
		}
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
	private String convertMethodToProperty(String methodName) throws IllegalArgumentException {
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

}
