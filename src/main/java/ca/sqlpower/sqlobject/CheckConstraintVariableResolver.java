/*
 * Copyright (c) 2010, SQL Power Group Inc.
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

package ca.sqlpower.sqlobject;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ca.sqlpower.object.SPVariableResolver;

/**
 * This {@link SPVariableResolver} resolves variables contained within the check
 * constraint of {@link UserDefinedSQLType}s given by
 * {@link UserDefinedSQLType#getCheckConstraint(String)}. There is no use of
 * namespaces in these variables because the variables only refer to the column
 * the check constraint applies to.
 */
public class CheckConstraintVariableResolver implements SPVariableResolver {
	
	public enum CheckConstraintVariable {
		COLUMN("column"),
		PRECISION("precision"),
		SCALE("scale");
		
		private final String variableName;
		
		private CheckConstraintVariable(String variableName) {
			this.variableName = variableName;
		}
		
		public String getVariableName() {
			return variableName;
		}

		/**
		 * Finds the {@link CheckConstraintVariable} given the variable name.
		 * 
		 * @param variableName
		 *            The name of the variable.
		 * @return The found {@link CheckConstraintVariable} that matches with
		 *         the variable name, or null if it does not exist.
		 */
		public static CheckConstraintVariable findVariable(String variableName) {
			if (variableName != null) {
				for (CheckConstraintVariable var : CheckConstraintVariable.values()) {
					if (variableName.equals(var.getVariableName())) {
						return var;
					}
				}
			}
			return null;
		}

		/**
		 * Retrieves the value of a variable given the {@link SQLColumn} it
		 * should resolve from.
		 * 
		 * @param column
		 *            The {@link SQLColumn} which the variable value comes from.
		 * @param variableName
		 *            The name of the variable.
		 * @return The value of the variable.
		 */
		public static Object getVariableValue(SQLColumn column, String variableName) {
			CheckConstraintVariable var = findVariable(variableName);
			if (var == null) {
				return null;
			} else {
				switch(var) {
				case COLUMN:
					return column.getPhysicalName();
				case PRECISION:
					return column.getPrecision();
				case SCALE:
					return column.getScale();
				default:
					return null;
				}
			}
		}
	}
	
	private final SQLColumn column;
	
	public CheckConstraintVariableResolver(SQLColumn column) {
		this.column = column;
	}
	
	public SQLColumn getColumn() {
		return column;
	}

	public void delete(String key) {
		throw new UnsupportedOperationException("The " + getClass().getName() + 
				" does not support deleting variable values.");
	}

	public String getNamespace() {
		return null;
	}

	public String getUserFriendlyName() {
		return "Column - " + column.getName();
	}

	public Collection<String> keySet(String namespace) {
		if (namespace == null) {
			Set<String> variableNames = new HashSet<String>();
			for (CheckConstraintVariable var : CheckConstraintVariable.values()) {
				variableNames.add(var.getVariableName());
			}
			return variableNames;
		} else {
			return Collections.emptySet();
		}
	}

	public Collection<Object> matches(String key, String partialValue) {
		Object value = resolve(key);
		String stringValue = String.valueOf(value);
		if (stringValue != null && stringValue.startsWith(partialValue)) {
			return Collections.singleton(value);
		} else {
			return Collections.emptySet();
		}
	}

	public Object resolve(String key) {
		return resolve(key, null);
	}

	public Object resolve(String key, Object defaultValue) {
		CheckConstraintVariable variable = 
			CheckConstraintVariable.findVariable(key);
		if (variable == null) {
			return defaultValue;
		} else {
			return CheckConstraintVariable.getVariableValue(
					column, variable.getVariableName());
		}
	}

	public Collection<Object> resolveCollection(String key) {
		Object value = resolve(key);
		if (value == null) {
			return Collections.emptySet();
		} else {
			return Collections.singleton(value);
		}
	}

	public Collection<Object> resolveCollection(String key, Object defaultValue) {
		Object value = resolve(key);
		if (value == null) {
			return Collections.singleton(defaultValue);
		} else {
			return Collections.singleton(value);
		}
	}

	public boolean resolves(String key) {
		return CheckConstraintVariable.findVariable(key) != null;
	}

	public boolean resolvesNamespace(String namespace) {
		return namespace == null;
	}

	public void store(String key, Object value) {
		throw new UnsupportedOperationException("The " + getClass().getName() + 
				" does not support storing variable values.");		
	}

	public void update(String key, Object value) {
		throw new UnsupportedOperationException("The " + getClass().getName() + 
				" does not support storing variable values.");
	}
	
}
