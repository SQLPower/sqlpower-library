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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.object.SPObject;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;

/**
 * This {@link AnnotationProcessorFactory} creates {@link SPAnnotationProcessor}
 * s which handle generating source code for persister helper classes based on
 * the supported annotations used in {@link SPObject}s. These persister helpers
 * will be used by a session {@link SPPersister}. In order to invoke this
 * factory through Maven to generate the persister helper classes, simply
 * navigate to this workspace project folder in a terminal window and call
 * "mvn apt:process". It will place the generated persister helper files in
 * build/generated-sources/apt/. In order to invoke this factory through Ant,
 * just run the build through Ant.
 * 
 */
public class SPAnnotationProcessorFactory implements AnnotationProcessorFactory {

	/**
	 * The {@link List} of supported annotation types which the
	 * {@link SPAnnotationProcessor} will process.
	 */
	private final List<String> annotations = new ArrayList<String>();

	/**
	 * Creates a new {@link SPAnnotationProcessorFactory} and populates the list
	 * of supported annotations.
	 */
	public SPAnnotationProcessorFactory() {
		annotations.add(Persistable.class.getName());
		annotations.add(Constructor.class.getName());
		annotations.add(ConstructorParameter.class.getName());
		annotations.add(Accessor.class.getName());
		annotations.add(Mutator.class.getName());
		annotations.add(MutatorParameter.class.getName());
	}

	public AnnotationProcessor getProcessorFor(
			Set<AnnotationTypeDeclaration> atds,
			AnnotationProcessorEnvironment env) {
		return new SPAnnotationProcessor(env);
	}

	public Collection<String> supportedAnnotationTypes() {
		return annotations;
	}

	public Collection<String> supportedOptions() {
		return Collections.emptyList();
	}

}
