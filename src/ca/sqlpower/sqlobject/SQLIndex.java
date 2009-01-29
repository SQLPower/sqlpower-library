/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.sqlobject;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.SQL;
import ca.sqlpower.sqlobject.SQLTable.Folder;

/**
 * The SQLIndex class represents an index on a table in a relational database.
 *
 * @author fuerth
 */
public class SQLIndex extends SQLObject {

    private static final Logger logger = Logger.getLogger(SQLIndex.class);

    /**
     * An enumeration to define if a column in an index should be ordered in ascending
     * order, descending order, or it should be left undefined.
     */
    public static enum AscendDescend {
        ASCENDING, DESCENDING, UNSPECIFIED;
    }

    /**
     * This is the property name in the PL.ini file that will indicate what Index types
     * are supported for any specific database.
     */
    public static final String INDEX_TYPE_DESCRIPTOR = SQLIndex.class.getName() + ".IndexType";

    /**
     * This is the index type
     */
    private String type;

    /**
     * This is the name of the column that will be augmented by the custom 
     * JDBC wrappers to represent index type;
     */
    public static final String RS_INDEX_TYPE_COL = "SPG_INDEX_TYPE";

    /**
     * A simple placeholder for a column.  We're not using real SQLColumn instances here so that the
     * tree of SQLObjects can remain tree-like.  If we put the real SQLColumns in here, the columns
     * would appear in two places in the tree (here and under the table's columns folder)!
     */
    public class Column extends SQLObject {

        /**
         * Small class for reacting to changes in this index columns's
         * target SQLColumn (if it has one at all).
         */
        private class TargetColumnListener implements SQLObjectListener {

            public void dbChildrenInserted(SQLObjectEvent e) {
                // won't happen to a column
            }

            public void dbChildrenRemoved(SQLObjectEvent e) {
                // won't happen to a column
            }

            /**
             * Updates the index column name to match the new value in this
             * event, if the event is a name change from the target SQLColumn.
             * The process of doing the update will cause the SQLIndex.Column
             * object to fire an event of its own.
             */
            public void dbObjectChanged(SQLObjectEvent e) {
                if ("name".equals(e.getPropertyName())) {
                    setName((String) e.getNewValue());
                }
            }

            @Override
            public String toString() {
                StringBuffer buf = new StringBuffer();
                buf.append(SQLIndex.this.getName());
                buf.append(".");
                buf.append(Column.this.getName());
                buf.append(".");
                buf.append("TargetColumnListener");
                buf.append(" isPrimarykey?");
                buf.append(SQLIndex.this.primaryKeyIndex);
                return buf.toString();
            }
        }

        /**
         * The column in the table that this index column represents. Might be
         * null if this index column represents an expression rather than a
         * single column value.
         */
        private SQLColumn column;

        /**
         * Specifies if the column is ascending, descending, or undefined.
         */
        private AscendDescend ascendingOrDescending;

        /**
         * A proxy that refires certain events on the target column.
         *
         * <p>It is the job of {@link #setColumn(SQLColumn)} to keep this
         * listener hooked up to the correct SQLColumn object (or completely
         * disconnected in the case that there is no target SQLColumn).
         */
        private final TargetColumnListener targetColumnListener = new TargetColumnListener();

        /**
         * Creates a Column object that corresponds to a particular SQLColumn.
         */
        public Column(SQLColumn col, AscendDescend ad) {
            this(col.getName(), ad);
            setColumn(col);
        }

        /**
         * Creates a Column object that does not correspond to a particular column
         * (such as an expression index).
         */
        public Column(String name, AscendDescend ad) {
            children = Collections.emptyList();
            setName(name);

            ascendingOrDescending = ad;
        }

        public Column() {
            this((String) null, AscendDescend.UNSPECIFIED);
        }

        @Override
        public boolean allowsChildren() {
            return false;
        }

        @Override
        public Class<? extends SQLObject> getChildType() {
            return null;
        }

        @Override
        public SQLObject getParent() {
            return SQLIndex.this;
        }

        @Override
        public String getShortDisplayName() {
            return getName();
        }

        @Override
        protected void populateImpl() throws SQLObjectException {
            // nothing to do
        }

        @Override
        public boolean isPopulated() {
            return true;
        }

        @Override
        protected void setParent(SQLObject parent) {
            if (parent != null && parent != SQLIndex.this) {
                throw new UnsupportedOperationException("You can't change an Index.Column's parent");
            }
        }

        /**
         * NOTE: This column can be null if the column it represents is an expression
         * and not a basic column.
         */
        public SQLColumn getColumn() {
            return column;
        }

        public void setColumn(SQLColumn column) {
            if (this.column != null) {
                this.column.removeSQLObjectListener(targetColumnListener);
            }
            SQLColumn oldValue = this.column;
            this.column = column;
            if (this.column != null) {
                this.column.addSQLObjectListener(targetColumnListener);
            }
            fireDbObjectChanged("column", oldValue, column);
        }

        public AscendDescend getAscendingOrDescending() {
            return ascendingOrDescending;
        }

        /**
         * This setter should be passed an enumerated item of type
         * AscendDescend.
         */
        public void setAscendingOrDescending(Object ad) {
            AscendDescend oldValue = ascendingOrDescending;
            if (ad instanceof AscendDescend) {
                ascendingOrDescending = (AscendDescend) ad;
            } else if (ad instanceof String) {
                ascendingOrDescending = AscendDescend.valueOf((String) ad);
            } else {
                throw new IllegalStateException("Invalid ascending or descending object on an index column.");
            }
            fireDbObjectChanged("ascendingOrDescending", oldValue, ascendingOrDescending);
        }

        public void setAscending(boolean ascending) {
            AscendDescend oldValue = this.ascendingOrDescending;
            if (ascending) {
                this.ascendingOrDescending = AscendDescend.ASCENDING;
            }
            fireDbObjectChanged("ascending", oldValue, ascendingOrDescending);
        }

        public void setDescending(boolean descending) {
            AscendDescend oldValue = this.ascendingOrDescending;
            if (descending) {
                this.ascendingOrDescending = AscendDescend.DESCENDING;
            }
            fireDbObjectChanged("descending", oldValue, descending);
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + (ascendingOrDescending == AscendDescend.ASCENDING ? 1231 : 1237);
            result = PRIME * result + ((column == null) ? 0 : column.hashCode());
            result = PRIME * result + (ascendingOrDescending == AscendDescend.DESCENDING ? 1231 : 1237);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final Column other = (Column) obj;
            if (ascendingOrDescending != other.ascendingOrDescending)
                return false;
            if (column == null) {
                if (other.column != null)
                    return false;
            } else if (!column.equals(other.column))
                return false;
            return true;
        }
    }

    /**
     * The parent folder that owns this index object.
     */
    private SQLTable.Folder<SQLIndex> parent;

    /**
     * Flags whether or not this index enforces uniqueness.
     */
    private boolean unique;

    /**
     * The qualifier that must be used for referring to this index in the database.  This is
     * usually the name of the table the index belongs to (in the case of SQL Server), or null
     * (in the case of Oracle).
     */
    private String qualifier;

    /**
     * The filter condition on this index, if any.  According to the ODBC programmer's reference,
     * this is probably a property of the index as a whole (as opposed to the individual index columns),
     * but it doesn't say that explicitly.  According to the JDBC spec, this could be anything at all.
     */
    private String filterCondition;

    /**
     * This indicates if an index is clustered or not.
     */
    private boolean clustered;

    private boolean primaryKeyIndex;

    /**
     * This is a listener that will listen for SQLColumns in the SQLTable's column folder
     * and make sure that the SQLIndex will also remove its Column object associated
     * with the SQLColumn removed.
     */
    private SQLObjectListener removeColumnListener;

    public SQLIndex(String name, boolean unique, String qualifier, String type, String filter) {
        this();
        setName(name);
        this.unique = unique;
        this.qualifier = qualifier;
        this.type = type;
        this.filterCondition = filter;
    }

    public SQLIndex() {
        children = new ArrayList();
        primaryKeyIndex = false;
        removeColumnListener = new SQLObjectListener() {

            public void dbStructureChanged(SQLObjectEvent e) {
            }

            public void dbObjectChanged(SQLObjectEvent e) {
            }

            public void dbChildrenRemoved(SQLObjectEvent e) {
                //FIXME: for some stupid reason, this gets called when columns
                // are simply moved in the table
                removeColumnFromIndices(e);
            }

            public void dbChildrenInserted(SQLObjectEvent e) {
            }

        };
    }

    /**
     * Copy constructor for a sql index
     * @param oldIndex
     * @throws SQLObjectException
     */
    public SQLIndex(SQLIndex oldIndex) throws SQLObjectException {
        this();
        setName(oldIndex.getName());
        unique = oldIndex.unique;
        parent = oldIndex.parent;
        populated = oldIndex.populated;
        type = oldIndex.type;
        filterCondition = oldIndex.filterCondition;
        qualifier = oldIndex.qualifier;
        clustered = oldIndex.clustered;
        for (Object c : oldIndex.getChildren()) {
            Column oldCol = (Column) c;
            Column newCol = new Column();
            newCol.setAscendingOrDescending(oldCol.ascendingOrDescending);
            newCol.setAscendingOrDescending(oldCol.ascendingOrDescending);
            newCol.column = oldCol.column;
            newCol.setName(oldCol.getName());
            addChild(newCol);
        }
    }

    /**
     * Indices are associated with one or more table columns.  The children of this index represent those columns,
     * and the order in which the index applies to them.
     */
    @Override
    public boolean allowsChildren() {
        return true;
    }

    @Override
    public Class<? extends SQLObject> getChildType() {
        return Column.class;
    }

    /**
     * Overriden to narrow return type.
     */
    @Override
    public Column getChild(int index) throws SQLObjectException {
        return (Column) super.getChild(index);
    }

    /**
     * Overriden to narrow return type.
     */
    @Override
    public List<Column> getChildren() throws SQLObjectException {
        return (List<Column>) super.getChildren();
    }

    /**
     * Returns the table folder that owns this index.
     */
    @Override
    public SQLTable.Folder<SQLIndex> getParent() {
        return parent;
    }

    /**
     * Returns this index's parent table, or null if this index
     * is not attached to a parent table.
     */
    public SQLTable getParentTable() {
        SQLTable.Folder<SQLIndex> parent = getParent();
        if (parent == null) {
            return null;
        } else {
            return parent.getParent();
        }
    }

    @Override
    public String getShortDisplayName() {
        return getName();
    }

    /**
     * Indices are populated when first created, so populate is a no-op.
     */
    @Override
    protected void populateImpl() throws SQLObjectException {
        // nothing to do
    }

    @Override
    public boolean isPopulated() {
        return true;
    }

    /**
     * Updates this index's parent reference, and attaches a listener to the
     * columns folder of the new parent's parent table.
     * 
     * @param parent
     *            The new parent. Must be null or a SQLTable.Folder instance. If
     *            it's a folder, it must already have a parent table.
     * @throws IllegalStateException
     *             if the given parent is non-null and does not itself have a
     *             parent table.
     */
    @Override
    protected void setParent(SQLObject parent) {
        
        if (this.parent != null) {
            getParentTable().getColumnsFolder().removeSQLObjectListener(removeColumnListener);
        }
        
        this.parent = (Folder<SQLIndex>) parent;
        
        if (this.parent != null) {
            if (getParentTable() == null) {
                throw new IllegalStateException("Index folder is unparented!");
            }
            getParentTable().getColumnsFolder().addSQLObjectListener(removeColumnListener);
        }
    }

    /**
     * This is used by the removeColumn method to make sure that once a column
     * is removed from a table, it is also removed from all the indices of that table.
     */
    private void removeColumnFromIndices(SQLObjectEvent e) {
        if (parent != null && parent.getParent() != null && parent.getParent().getColumnsFolder().isMagicEnabled()) {
            try {
                for (int i = 0; i < e.getChildren().length; i++) {
                    for (int j = this.getChildCount() - 1; j >= 0; j--) {
                        if (getChild(j).getColumn() != null && getChild(j).getColumn().equals(e.getChildren()[i])) {
                            removeChild(j);
                        }
                    }
                }
                cleanUp();
            } catch (SQLObjectException e1) {
                throw new SQLObjectRuntimeException(e1);
            }
        }
    }

    /**
     * This method is used to clean up the index when it no longer has any children.
     */
    public void cleanUp() {
        try {
            if (getChildCount() == 0 && this.parent != null) {
                logger.debug("Removing " + getName() + " index from folder " + parent.getName());
                parent.getParent().getColumnsFolder().removeSQLObjectListener(removeColumnListener);
                parent.removeChild(this);
            }
        } catch (SQLObjectException e1) {
            throw new SQLObjectRuntimeException(e1);
        }
    }

    @Override
    protected SQLObject removeImpl(int index) {
        Column c = (Column) children.get(index);
        if (c.getColumn() != null) {
            c.getColumn().removeSQLObjectListener(c.targetColumnListener);
        }
        return super.removeImpl(index);
    }

    @Override
    protected void addChildImpl(int index, SQLObject newChild) throws SQLObjectException {
        if (newChild instanceof SQLIndex.Column && primaryKeyIndex && ((Column) newChild).getColumn() == null) {
            throw new SQLObjectException("The primary key index must consist of real columns, not expressions");
        }
        super.addChildImpl(index, newChild);
        Column c = (Column) newChild;
        if (c.getColumn() != null) {
            // this will be redundant in some cases, but the addSQLObjectListener method
            // checks for adding duplicate listeners and does nothing in that case
            c.getColumn().addSQLObjectListener(c.targetColumnListener);
        }
    }

    public String getFilterCondition() {
        return filterCondition;
    }

    public void setFilterCondition(String filterCondition) {
        String oldValue = this.filterCondition;
        this.filterCondition = filterCondition;
        fireDbObjectChanged("filterCondition", oldValue, filterCondition);
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        String oldValue = this.qualifier;
        this.qualifier = qualifier;
        fireDbObjectChanged("qualifier", oldValue, qualifier);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        String oldValue = this.type;
        this.type = type;
        fireDbObjectChanged("type", oldValue, type);
    }

    public boolean isUnique() {
        return unique;
    }

    public boolean isClustered() {
        return clustered;
    }

    public void setUnique(boolean unique) {
        boolean oldValue = this.unique;
        this.unique = unique;
        fireDbObjectChanged("unique", oldValue, unique);
    }

    public void setClustered(boolean value) {
        boolean oldValue = this.clustered;
        this.clustered = value;
        fireDbObjectChanged("clustered", oldValue, clustered);
    }

    public void setParent(SQLTable.Folder<SQLIndex> parent) {
        this.parent = parent;
    }

    /**
     * Mainly for use by SQLTable's populate method.  Does not cause
     * SQLObjectEvents to avoid infinite recursion, so you have to
     * generate them yourself at a safe time.
     */
    static void addIndicesToTable(SQLTable addTo, String catalog, String schema, String tableName, DatabaseMetaData dbmd) throws SQLException,
            SQLObjectException {
        Connection con = null;
        ResultSet rs = null;

        try {
            con = addTo.getParentDatabase().getConnection();
            if (dbmd == null) dbmd = con.getMetaData();
            String pkName = null;
            rs = dbmd.getPrimaryKeys(catalog, schema, tableName);
            while (rs.next()) {
                SQLColumn col = addTo.getColumnByName(rs.getString(4), false, true, dbmd);
                //logger.debug(rs.getString(4));
                if (col != null) {
                    col.primaryKeySeq = new Integer(rs.getInt(5));
                    String pkNameCheck = rs.getString(6);
                    if (pkName == null) {
                        pkName = pkNameCheck;
                    } else if (!pkName.equals(pkNameCheck)) {
                        throw new IllegalStateException("The PK name has changed somehow while adding indices to table");
                    }
                } else {
                    throw new SQLException("Column " + rs.getString(4) + " not found in " + addTo);
                }
            }
            rs.close();
            rs = null;

            logger.debug("SQLIndex.addIndicesToTable: catalog=" + catalog + "; schema=" + schema + "; tableName=" +
                    tableName + "; primary key name=" + pkName);
            SQLIndex idx = null;
            rs = dbmd.getIndexInfo(catalog, schema, tableName, false, true);
            while (rs.next()) {
                /*
                 * DatabaseMetadata result set columns:
                 *
                1  TABLE_CAT String => table catalog (may be null)
                2  TABLE_SCHEM String => table schema (may be null)
                3  TABLE_NAME String => table name
                4  NON_UNIQUE boolean => Can index values be non-unique. false when TYPE is tableIndexStatistic
                5  INDEX_QUALIFIER String => index catalog (may be null); null when TYPE is tableIndexStatistic
                6  INDEX_NAME String => index name; null when TYPE is tableIndexStatistic
                7  TYPE short => index type:
                      tableIndexStatistic - this identifies table statistics that are returned in conjuction with a table's index descriptions
                      tableIndexClustered - this is a clustered index
                      tableIndexHashed - this is a hashed index
                      tableIndexOther - this is some other style of index
                8  ORDINAL_POSITION short => column sequence number within index; zero when TYPE is tableIndexStatistic
                9  COLUMN_NAME String => column name; null when TYPE is tableIndexStatistic
                10 ASC_OR_DESC String => column sort sequence, "A" => ascending, "D" => descending, may be null if sort sequence is not supported; null when TYPE is tableIndexStatistic
                11 CARDINALITY int => When TYPE is tableIndexStatistic, then this is the number of rows in the table; otherwise, it is the number of unique values in the index.
                12 PAGES int => When TYPE is tableIndexStatisic then this is the number of pages used for the table, otherwise it is the number of pages used for the current index.
                13 FILTER_CONDITION String => Filter condition, if any. (may be null)
                 */
                boolean nonUnique = rs.getBoolean(4);
                boolean isClustered = rs.getShort(7) == DatabaseMetaData.tableIndexClustered ? true : false;
                String qualifier = rs.getString(5);
                String name = rs.getString(6);
                String type = null;
                if (SQL.findColumnIndex(rs, RS_INDEX_TYPE_COL) > 0) {
                    type = rs.getString(RS_INDEX_TYPE_COL);
                }
                int pos = rs.getInt(8);
                String colName = rs.getString(9);
                String ascDesc = rs.getString(10);
                AscendDescend aOrD = AscendDescend.UNSPECIFIED;
                if (ascDesc != null && ascDesc.equals("A")) {
                    aOrD = AscendDescend.ASCENDING;
                } else if (ascDesc != null && ascDesc.equals("D")) {
                    aOrD = AscendDescend.DESCENDING;
                }
                String filter = rs.getString(13);

                if (pos == 0) {
                    // this is just the table stats, not an index
                    continue;
                } else if (pos == 1) {
                    logger.debug("Found index " + name);
                    idx = new SQLIndex(name, !nonUnique, qualifier, type, filter);
                    idx.setClustered(isClustered);
                    addTo.getIndicesFolder().children.add(idx);
                    if (name.equals(pkName)) {
                        idx.setPrimaryKeyIndex(true);
                    }
                }

                logger.debug("Adding column " + colName + " to index " + idx.getName());

                Column col;
                if (addTo.getColumnByName(colName, false, true) != null) {
                    col = idx.new Column(addTo.getColumnByName(colName, false, true), aOrD);
                } else {
                    col = idx.new Column(colName, aOrD); // probably an expression like "col1+col2"
                }

                idx.children.add(col); // direct access avoids possible recursive SQLObjectEvents
            }
            rs.close();
            rs = null;

        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException ex) {
                logger.error("Couldn't close result set", ex);
            }
            try {
                if (con != null)
                    con.close();
            } catch (SQLException ex) {
                logger.error("Couldn't close connection", ex);
            }
        }
    }

    public boolean isPrimaryKeyIndex() {
        return primaryKeyIndex;
    }

    /**
     * Updates whether this index is a primary key
     *
     * set this index as primary key index and remove any old primary key
     * if isPrimaryKey is true.  Otherwise, sets primaryKeyIndex to false and
     * removes it from its parent table.
     *
     * @param isPrimaryKey
     */
    public void setPrimaryKeyIndex(boolean isPrimaryKey) throws SQLObjectException {
        boolean oldValue = this.primaryKeyIndex;
        if (oldValue == isPrimaryKey)
            return;
        try {
            startCompoundEdit("Make index a Primary Key");
            if (isPrimaryKey) {
                for (Column c : getChildren()) {
                    if (c.getColumn() == null) {
                        throw new SQLObjectException("A PK must only refer to Index.Columns that contain SQLColumns");
                    }
                }
                SQLTable parentTable = getParentTable();
                if (parentTable != null) {
                    SQLIndex i = parentTable.getPrimaryKeyIndex();
                    if (i != null && i != this) {
                        i.setPrimaryKeyIndex(false);
                    }
                }
            }
            primaryKeyIndex = isPrimaryKey;
            fireDbObjectChanged("primaryKeyIndex", oldValue, isPrimaryKey);
        } finally {
            endCompoundEdit("Make index a Primary Key");
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Adds a column to the index. If col1 is null a NPE will be thrown.
     */
    public void addIndexColumn(SQLColumn col1, AscendDescend aOrD) throws SQLObjectException {
        Column col = new Column(col1, aOrD);
        addChild(col);
    }
    
    public void addIndexColumn(String colName, AscendDescend aOrD) throws SQLObjectException {
        Column col = new Column(colName, aOrD);
        addChild(col);
    }

    /**
     * Returns a copy of a SQLIndex from a given SQLIndex in a parent SQLTable.
     * This appears to be mainly used for creating a SQLIndex for a copied table
     * in the playpen when importing tables from a source database as part of
     * the reverse engineering feature.
     *
     * @param source The source SQLIndex to copy
     * @param parentTable The parent SQLTable of the source SQLIndex
     * @return A copy of the given source SQLIndex.
     * @throws SQLObjectException
     */
    public static SQLIndex getDerivedInstance(SQLIndex source, SQLTable parentTable) throws SQLObjectException {

        SQLIndex index = new SQLIndex();
        index.setName(source.getName());
        index.setUnique(source.isUnique());
        index.setPopulated(source.isPopulated());
        index.setType(source.getType());
        index.setFilterCondition(source.getFilterCondition());
        index.setQualifier(source.getQualifier());
        index.setPrimaryKeyIndex(source.isPrimaryKeyIndex());
        index.setPhysicalName(source.getPhysicalName());
        index.setClustered(source.isClustered());
        index.setChildrenInaccessibleReason(source.getChildrenInaccessibleReason());

        for (Column column : source.getChildren()) {
            Column newColumn;

            if (column.getColumn() != null) {
                SQLColumn sqlColumn = parentTable.getColumnByName(column.getColumn().getName());
                if (sqlColumn == null) {
                    throw new SQLObjectException("Can not derive instance, because coulmn " +
                            column.getColumn().getName() + "is not found in parent table [" + parentTable.getName() +
                            "]");
                }
                newColumn = index.new Column(sqlColumn, column.getAscendingOrDescending());
            } else {
                newColumn = index.new Column(column.getName(), column.getAscendingOrDescending());
            }
            index.addChild(newColumn);
        }
        return index;
    }

    /**
     * Make this index's columns look like the columns in index
     *
     * @param index The index who's columns are what we want in this index
     * @throws SQLObjectException
     */
    public void makeColumnsLike(SQLIndex index) throws SQLObjectException {
        for (int i = children.size() - 1; i >= 0; i--) {
            Column c = (Column) children.get(i);
            if (c.column != null) {
                c.column.removeSQLObjectListener(c.targetColumnListener);
            }
            removeChild(i);
        }

        for (Column c : index.getChildren()) {
            Column newCol = new Column(c.getName(), c.getAscendingOrDescending());
            newCol.setColumn(c.getColumn());
            addChild(newCol);
        }
    }
}