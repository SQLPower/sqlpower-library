/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Wabit.
 *
 * Wabit is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wabit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.query;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;

import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLTable;

/**
 * A model that stores SQLTable elements. This will store objects of a defined type and
 * can be grouped when adding the items to the model.
 *
 * @param <C> The type of object this model will store.
 */
public class TableContainer extends ItemContainer implements Container {
	
	private static final Logger logger = Logger.getLogger(TableContainer.class);

	private SQLTable table;
	
	/**
	 * The list contains all of the columns of the table.
	 */
	private final List<SQLObjectItem> itemList;
	
	/**
	 * The catalog that the SQLTable contained in this container belongs to.
	 */
	private final String catalog;
	
	/**
	 * The schema that the SQLTable contained in this container belongs to.
	 */
	private final String schema;

    /**
     * This is the database the table is stored in so we can load the table or
     * refresh it as necessary.
     */
	private final SQLDatabase database;

	public TableContainer(@Nonnull SQLDatabase db, @Nonnull SQLTable t) {
	    super(t.getName());
	    if (db == null) {
	    	throw new NullPointerException("Database mustn't be null when providing a live SQLTable.");
	    }
	    database = db;
		table = t;
		schema = table.getSchemaName();
		catalog = table.getCatalogName();
		setAlias("");
		itemList = new ArrayList<SQLObjectItem>();
		loadColumnsFromTable(t);
	}

	/**
	 * This constructor creates a table that will be loaded from the database when a part of the
	 * container is accessed. To load the table the table's name, schema and catalog will be used
	 * to retrieve the table from the database. The items of this container will have it's object
	 * set when the table is loaded.
	 */
	public TableContainer(String uuid, SQLDatabase db, String name, String schema, String catalog, List<SQLObjectItem> items) {
	    this(uuid, db, name, schema, catalog, items, false);
	}

    /**
     * This constructor creates a table that will be loaded from the database
     * when a part of the container is accessed if the populated flag is set to
     * false. To load the table the table's name, schema and catalog will be
     * used to retrieve the table from the database. The items of this container
     * will have it's object set when the table is loaded.
     * 
     * @param doPopulate
     *            If set to false the objects will be considered populated and
     *            will not try to populate again. This is useful for places like
     *            the session on the server that can just accept the objects
     *            given to it, it does not need to manipulate the objects
     *            further. If true the object named in this container will be
     *            populated when accessed.
     */
	public TableContainer(String uuid, SQLDatabase db, String name, String schema, String catalog, List<SQLObjectItem> items, boolean doPopulate) {
		super(name, uuid);
	    if (db == null) {
	    	logger.debug("Database connection for table " + name + " is missing. Although " +
	    			"this is non-fatal, it should only happen if the actual database " +
	    			"connection was unavailable at the time the workspace was loaded",
	    			new Exception("Don't panic. Just a stack trace"));
	    }
		database = db;
		if (schema != null) {
			this.schema = schema;
		} else {
			this.schema = "";
		}
		if (catalog != null) {
			this.catalog = catalog;
		} else {
			this.catalog = "";
		}
		setName(name);
		if (doPopulate) {
		    table = null;
		} else {
		    try {
		        table = new SQLTable(db, true);
		        table.setName(name);
		    } catch (SQLObjectException e) {
		        throw new RuntimeException("Cannot happen", e);
		    }
		}
		super.setAlias("");
		itemList = new ArrayList<SQLObjectItem>();
		for (SQLObjectItem item : items) {
			item.setParent(this);
			itemList.add(item);
			fireChildAdded(item, itemList.indexOf(item));
		}
	}
	
	/**
	 * This will create the items from the columns for the table. This
	 * needs to be called right after the table gets set.
	 */
	private void loadColumnsFromTable(SQLTable t) {
		try {
			for (SQLColumn child : t.getColumns()) {
				boolean itemFound = false;
				for (Item item : itemList) {
					if (item.getName().equals(child.getName())) {
						((SQLObjectItem) item).setItem(child);
						itemFound = true;
						break;
					}
				}
				if (itemFound) {
					continue;
				}
				SQLObjectItem item = new SQLObjectItem(child);
				item.setParent(this);
				itemList.add(item);
				fireChildAdded(item, itemList.indexOf(item));
			}
		} catch (SQLObjectException e) {
			throw new RuntimeException(e);
		}
	}
	
	public List<Item> getItems() {
		loadTableByQualifiedName();
		return new ArrayList<Item>(itemList);
	}
	
	public String getName() {
		loadTableByQualifiedName();
		if (table != null) {
			return table.getName();
		} else {
			return super.getName();
		}
	}

	public Item getItem(Object item) {
		loadTableByQualifiedName();
		for (Item i : itemList) {
			if (i.getItem() == item) {
				return i;
			}
		}
		return null;
	}
	
	public SQLDatabase getDatabase() {
		return database;
	}

	public Object getContainedObject() {
		loadTableByQualifiedName();
		return table;
	}

	public void addItem(Item item) {
		throw new IllegalStateException("Cannot add arbitrary items to a SQLObject.");		
	}

	public void removeItem(Item item) {
		throw new IllegalStateException("Cannot remove arbitrary items from a SQLObject.");		
	}
	
	public void removeItem(int i) {
        throw new IllegalStateException("Cannot remove arbitrary items from a SQLObject.");     
    }

	public String getSchema() {
		return schema;
	}
	
	public String getCatalog() {
		return catalog;
	}
	
	/**
	 * This will load the table from the query's data source as necessary
	 * if it is null and the qualified name is not null.
	 * <p>
	 * This is package private to allow the container's items to load their
	 * parent table and theirselves. 
	 */
	void loadTableByQualifiedName() {
		if (table == null) {
			SQLDatabase db = database;
            logger.debug("Cache has database " + db);
			if (db == null) {
			    logger.info("Skipping table " + super.getName() + " because its database connection is missing");
			    return;
			}
			try {
				table = db.getTableByName(catalog, schema, super.getName());
			} catch (SQLObjectException e) {
			    logger.info("Skipping table " + super.getName() + " due to failure in populate:", e);
				return;
			}
			if (table == null) {
                logger.info("Skipping table " + super.getName() + " because it doesn't exist in the database");
				return;
			}
			
			loadColumnsFromTable(table);
		}
	}
	
	@Override
	public Container createCopy() {
		final TableContainer copy;
		if (database != null && table != null) {
			copy = new TableContainer(database, table);
		} else {
			copy = new TableContainer(getUUID(), database, getName(), schema, catalog, itemList);			
		}
		for (Item item : itemList) {
	        Item newItem = copy.getItem(item.getItem());
	        newItem.setAlias(item.getAlias());
	        newItem.setColumnWidth(item.getColumnWidth());
	        newItem.setGroupBy(item.getGroupBy());
	        newItem.setHaving(item.getHaving());
	        newItem.setOrderBy(item.getOrderBy());
	        newItem.setOrderByOrdering(item.getOrderByOrdering());
	        newItem.setSelected(item.getSelected());
	        newItem.setWhere(item.getWhere());
	    }
	    copy.setAlias(getAlias());
        copy.setPosition(new Point2D.Double(getPosition().getX(), getPosition().getY()));
	    return copy;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TableContainer && ((TableContainer) obj).getUUID().equals(getUUID())) {
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return 31 * 17 + getUUID().hashCode();
	}

}
