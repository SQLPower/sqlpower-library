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

import java.beans.PropertyChangeEvent;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;

import ca.sqlpower.object.AbstractPoolingSPListener;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sqlobject.SQLRelationship.ColumnMapping;
import ca.sqlpower.sqlobject.SQLTypePhysicalProperties.SQLTypeConstraint;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider.BasicSQLType;
import ca.sqlpower.sqlobject.SQLTypePhysicalPropertiesProvider.PropertyType;
import ca.sqlpower.util.SQLPowerUtils;

/**
 * This listener will update the fk columns of the fk table based on the
 * mappings in a relationship when there are property changes to the columns in
 * the pk table. This listener only needs to be attached to the pk table.
 * 
 * This class is made package private because only the {@link SQLRelationship}
 * should use it. It has been refactored out because it is quite lengthy in
 * code.
 */
class ForeignKeyColumnUpdaterPoolingSPListener extends
		AbstractPoolingSPListener {
	
	private static final Logger logger = 
		Logger.getLogger(ForeignKeyColumnUpdaterPoolingSPListener.class);
	
	private final SQLRelationship relationship;

	/**
	 * Creates a new {@link ForeignKeyColumnUpdaterPoolingSPListener}.
	 * 
	 * @param relationship
	 *            The {@link SQLRelationship} that this listener is tied to.
	 *            This value cannot be null.
	 */
	ForeignKeyColumnUpdaterPoolingSPListener(@Nonnull SQLRelationship relationship) {
		super(false);
		this.relationship = relationship;
	}
	
	@Override
	protected void propertyChangeImpl(PropertyChangeEvent e) {
		if (!((SQLObject) e.getSource()).isMagicEnabled()){
			logger.debug("Magic disabled; ignoring sqlobject changed event "+e);
			return;
		}
		String prop = e.getPropertyName();
		if (logger.isDebugEnabled()) {
			logger.debug("Property changed!" +
					"\n source=" + e.getSource() +
					"\n property=" + prop +
					"\n old=" + e.getOldValue() +
					"\n new=" + e.getNewValue());
		}
		if (e.getSource() instanceof SQLColumn) {
			SQLColumn col = (SQLColumn) e.getSource();

			if (col.getParent() != null && col.getParent().equals(relationship.getParent())) {

				ColumnMapping m = relationship.getMappingByPkCol(col);
				if (m == null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring change for column "+col+" parent "+col.getParent());
					}
					return;
				}
				if (m.getPkColumn() == null) throw new NullPointerException("Missing pk column in mapping");
				//The fk column can be null in a mapping but the table and column name must
				//exist. This is done to improve the speed of populating 1 table.
				if (m.getFkColumn() == null) {
					if (m.getFkColName() == null) throw new NullPointerException("Missing pk column name in mapping");
					if (m.getFkTable() == null) throw new NullPointerException("Missing pk table in mapping");
					return;
				}

				if (prop == null
						|| prop.equals("parent")
						|| prop.equals("remarks")
						|| prop.equals("autoIncrement")) {
					// don't care
				} else if (prop.equals("sourceColumn")) {
					m.getFkColumn().setSourceColumn(m.getPkColumn().getSourceColumn());
				} else if (prop.equals("name")) {
					// only update the fkcol name if its name was the same as the old pkcol name
					if (m.getFkColumn().getName().equalsIgnoreCase((String) e.getOldValue())) {
						m.getFkColumn().setName(m.getPkColumn().getName());
					}
				} else {
					logger.warn("Warning: unknown column property "+prop
							+" changed while monitoring pkTable");
				}
			}
		} else if (e.getSource() instanceof UserDefinedSQLType) {
			UserDefinedSQLType sourceType = (UserDefinedSQLType) e.getSource();
			SPObject parent = sourceType.getParent();
			if (!(parent instanceof SQLColumn)) {
				throw new IllegalStateException("UserDefinedSQLType " + 
						sourceType.getPhysicalName() + " must have a SQLColumn parent.");
			}
			SQLColumn sourceColumn = (SQLColumn) parent;
			ColumnMapping m = relationship.getMappingByPkCol(sourceColumn);
			
			if (m == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring change for UserDefinedSQLType "+sourceType.getName());
				}
				return;
			}

			final SQLColumn fkCol = m.getFkColumn();
			if (fkCol == null) throw new NullPointerException("Missing fk column in mapping");
			final UserDefinedSQLType fkType = fkCol.getUserDefinedSQLType();

			if (prop.equals("type")) {
				fkType.setType((Integer) e.getNewValue());
			} else if (prop.equals("name")) {
				fkType.setName((String) e.getNewValue());
			} else if (prop.equals("myNullability")) {
				fkType.setMyNullability((Integer) e.getNewValue());
			} else if (prop.equals("upstreamType")) {
				fkType.setUpstreamType((UserDefinedSQLType) e.getNewValue());
			} else if (prop.equals("basicType")) {
				fkType.setBasicType((BasicSQLType) e.getNewValue());
			} else {
				logger.warn("Warning: unknown UserDefinedSQLType property "+prop
						+" changed while monitoring pkTable");
			}

		} else if (e.getSource() instanceof SQLTypePhysicalProperties) {
			// Find all the column mappings where the primary key column's
			// type is the type that fired this property change event.
			SQLTypePhysicalProperties sourceProperties = (SQLTypePhysicalProperties) e.getSource();
			
			SPObject parent = sourceProperties.getParent();
			if (!(parent instanceof UserDefinedSQLType)) {
				throw new IllegalStateException("SQLTypePhysicalProperties " + 
						sourceProperties.getPhysicalName() + 
						" must have a UserDefinedSQLType parent.");
			}
			UserDefinedSQLType sourceType = (UserDefinedSQLType) parent;
			
			parent = sourceType.getParent();
			if (!(parent instanceof SQLColumn)) {
				throw new IllegalStateException("UserDefinedSQLType " + 
						sourceType.getPhysicalName() + " must have a SQLColumn parent.");
			}
			SQLColumn sourceColumn = (SQLColumn) parent;
			ColumnMapping m = relationship.getMappingByPkCol(sourceColumn);

			if (m == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring change for SQLTypePhysicalProperties "+sourceProperties.getName());
				}
				return;
			}

			final SQLColumn fkCol = m.getFkColumn();
			if (fkCol == null) throw new NullPointerException("Missing fk column in mapping");

			final UserDefinedSQLType fkType = fkCol.getUserDefinedSQLType();
			final String fkPlatform = fkCol.getPlatform();

			if (prop.equals("scale") && 
					(e.getNewValue() == null || fkType.getScale(fkPlatform) > (Integer) e.getNewValue())) {
				// Foreign key's scale must conform to primary key's scale.
				// FK scale <= PK scale
				fkType.setScale(fkPlatform, (Integer) e.getNewValue());
			} else if (prop.equals("scaleType")) {
				fkType.setScaleType(fkPlatform, (PropertyType) e.getNewValue());
			} else if (prop.equals("precision") && 
					(e.getNewValue() == null || fkType.getPrecision(fkPlatform) > (Integer) e.getNewValue())) {
				// Foreign key's precision must conform to primary key's precision.
				// FK precision <= PK precision
				fkType.setPrecision(fkPlatform, (Integer) e.getNewValue());
			} else if (prop.equals("precisionType")) {
				fkType.setPrecisionType(fkPlatform, (PropertyType) e.getNewValue());
			} else if (prop.equals("constraintType")) {
				fkType.setConstraintType(fkPlatform, (SQLTypeConstraint) e.getNewValue());
			} else if (prop.equals("defaultValue")) {
				fkType.setDefaultValue(fkPlatform, (String) e.getNewValue());
			} else {
				logger.warn("Warning: unknown SQLTypePhysicalProperties property "+prop
						+" changed while monitoring pkTable");
			}
		} else if (e.getSource() instanceof SQLCheckConstraint) {
			SQLCheckConstraint sourceConstraint = (SQLCheckConstraint) e.getSource();
			
			SPObject parent = sourceConstraint.getParent();
			if (!(parent instanceof SQLTypePhysicalProperties)) {
				throw new IllegalStateException("SQLCheckConstraint " + 
						sourceConstraint.getPhysicalName() + 
						" must have a SQLTypePhysicalProperties parent.");
			}
			SQLTypePhysicalProperties sourceProperties = (SQLTypePhysicalProperties) parent;
			
			parent = sourceProperties.getParent();
			if (!(parent instanceof UserDefinedSQLType)) {
				throw new IllegalStateException("SQLTypePhysicalProperties " + 
						sourceProperties.getPhysicalName() + 
						" must have a UserDefinedSQLType parent.");
			}
			UserDefinedSQLType sourceType = (UserDefinedSQLType) parent;
			
			parent = sourceType.getParent();
			if (!(parent instanceof SQLColumn)) {
				throw new IllegalStateException("UserDefinedSQLType " + 
						sourceType.getPhysicalName() + " must have a SQLColumn parent.");
			}
			SQLColumn sourceColumn = (SQLColumn) parent;
			
			ColumnMapping m = relationship.getMappingByPkCol(sourceColumn);

			if (m == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring change for SQLCheckConstraint "+sourceConstraint.getName());
				}
				return;
			}

			final SQLColumn fkCol = m.getFkColumn();
			if (fkCol == null) throw new NullPointerException("Missing fk column in mapping");

			final String fkPlatform = fkCol.getPlatform();
			final SQLTypePhysicalProperties fkProperties = 
				fkCol.getUserDefinedSQLType().getPhysicalProperties(fkPlatform);
			for (SQLCheckConstraint constraint : fkProperties.getChildren(SQLCheckConstraint.class)) {
				if (prop.equals("name") 
						&& constraint.getName().equals((String) e.getOldValue())
						&& constraint.getConstraint().equals(sourceConstraint.getConstraint())) {
					constraint.setName((String) e.getNewValue());
				} else if (prop.equals("constraint")
						&& constraint.getName().equals(sourceConstraint)
						&& constraint.getConstraint().equals((String) e.getOldValue())) {
					constraint.setConstraint((String) e.getNewValue());
				}
			}
		} else if (e.getSource() instanceof SQLEnumeration) {
			SQLEnumeration sourceEnumeration = (SQLEnumeration) e.getSource();
			SPObject parent = sourceEnumeration.getParent();
			if (!(parent instanceof SQLTypePhysicalProperties)) {
				throw new IllegalStateException("SQLEnumeration " + 
						sourceEnumeration.getPhysicalName() + 
						" must have a SQLTypePhysicalProperties parent.");
			}
			SQLTypePhysicalProperties sourceProperties = (SQLTypePhysicalProperties) parent;
			
			parent = sourceProperties.getParent();
			if (!(parent instanceof UserDefinedSQLType)) {
				throw new IllegalStateException("SQLTypePhysicalProperties " + 
						sourceProperties.getPhysicalName() + 
						" must have a UserDefinedSQLType parent.");
			}
			UserDefinedSQLType sourceType = (UserDefinedSQLType) parent;
			
			parent = sourceType.getParent();
			if (!(parent instanceof SQLColumn)) {
				throw new IllegalStateException("UserDefinedSQLType " + 
						sourceType.getPhysicalName() + " must have a SQLColumn parent.");
			}
			SQLColumn sourceColumn = (SQLColumn) parent;
			ColumnMapping m = relationship.getMappingByPkCol(sourceColumn);

			if (m == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring change for SQLEnumeration "+sourceEnumeration.getName());
				}
				return;
			}

			final SQLColumn fkCol = m.getFkColumn();
			if (fkCol == null) throw new NullPointerException("Missing fk column in mapping");

			final String fkPlatform = fkCol.getPlatform();
			final SQLTypePhysicalProperties fkProperties = 
				fkCol.getUserDefinedSQLType().getPhysicalProperties(fkPlatform);
			for (SQLEnumeration constraint : fkProperties.getChildren(SQLEnumeration.class)) {
				if (prop.equals("name") 
						&& constraint.getName().equals((String) e.getOldValue())) {
					constraint.setName((String) e.getNewValue());
				}
			}
		}
	}
	
	@Override
	protected void childAddedImpl(SPChildEvent e) {
		SQLPowerUtils.listenToHierarchy(e.getChild(), this);
	}
	
	@Override
	protected void childRemovedImpl(SPChildEvent e) {
		SQLPowerUtils.unlistenToHierarchy(e.getChild(), this);
	}
	
}
