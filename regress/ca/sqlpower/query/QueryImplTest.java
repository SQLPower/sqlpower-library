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

package ca.sqlpower.query;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import ca.sqlpower.query.QueryImpl.OrderByArgument;
import ca.sqlpower.query.QueryImpl.TableJoinGraph;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLDatabaseMapping;

public class QueryImplTest extends TestCase {
	
	

    private class StubDatabaseMapping implements SQLDatabaseMapping {

        public SQLDatabase getDatabase(JDBCDataSource ds) {
            return null;
        }
        
    }
    
    private class TestingStringItem extends StringItem {
        
        private List<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();
        
        public TestingStringItem(String name) {
            super(name);
        }
        
        @Override
        public Item createCopy() {
            return new TestingStringItem(getName());
        }
        
        @Override
        public void addPropertyChangeListener(PropertyChangeListener l) {
            super.addPropertyChangeListener(l);
            listeners.add(l);
        }
        
        public List<PropertyChangeListener> getListeners() {
            return listeners;
        }
    }
    
    private class TestingItemContainer extends ItemContainer {
        
        private final List<ContainerChildListener> listeners = new ArrayList<ContainerChildListener>();
        
        public TestingItemContainer(String name) {
            super(name);
        }
        
        @Override
        public Container createCopy() {
            return new TestingItemContainer(getName());
        }
        
        @Override
        public void addChildListener(ContainerChildListener l) {
            super.addChildListener(l);
            listeners.add(l);
        }
        
        public List<ContainerChildListener> getListeners() {
            return listeners;
        }
    }
    
    private class TestingSQLJoin extends SQLJoin {
        
        private final List<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();
        
        public TestingSQLJoin(Item leftItem, Item rightItem) {
            super(leftItem, rightItem);
        }
        
        @Override
        public SQLJoin createCopy(Item leftItemCopy, Item rightItemCopy) {
            return new TestingSQLJoin(leftItemCopy, rightItemCopy);
        }
        
        @Override
        public void addJoinChangeListener(PropertyChangeListener l) {
            super.addJoinChangeListener(l);
            listeners.add(l);
        }
        
        public List<PropertyChangeListener> getListeners() {
            return listeners;
        }
        
    }
    
    /**
     * Tests the copy constructor of a query will copy columns, containers, 
     * and joins.
     */
    public void testCopyConstructorWithoutListeners() throws Exception {
        QueryImpl query = new QueryImpl(new StubDatabaseMapping());
        Item leftColumn = new TestingStringItem("leftCol");
        Container leftContainer = new TestingItemContainer("leftContainer");
        leftContainer.addItem(leftColumn);
        Item rightColumn = new TestingStringItem("rightCol");
        Container rightContainer = new TestingItemContainer("rightContainer");
        rightContainer.addItem(rightColumn);
        SQLJoin join = new TestingSQLJoin(leftColumn, rightColumn);
        query.addTable(leftContainer);
        query.addTable(rightContainer);
        query.addJoin(join);
        
        assertTrue(query.getFromTableList().contains(leftContainer));
        assertTrue(query.getFromTableList().contains(rightContainer));
        assertTrue(query.getJoins().contains(join));
        
        QueryImpl copy = new QueryImpl(query, false);
        
        assertEquals(2, copy.getFromTableList().size());
        assertEquals(1, copy.getJoins().size());
        
        final Container copiedContainers1 = copy.getFromTableList().get(0);
        final Container copiedContainers2 = copy.getFromTableList().get(1);
        final SQLJoin copiedJoin = copy.getJoins().iterator().next();
        
        assertTrue(copy.getJoinMapping().containsKey(copiedContainers1));
        assertTrue(copy.getJoinMapping().containsKey(copiedContainers2));
        assertTrue(copy.getJoinMapping().get(copiedContainers1).containsAll(copy.getJoins()));
        assertTrue(copy.getJoinMapping().get(copiedContainers2).containsAll(copy.getJoins()));
        assertNotSame(copiedContainers1, leftContainer);
        assertNotSame(copiedContainers2, leftContainer);
        assertNotSame(copiedContainers1, rightContainer);
        assertNotSame(copiedContainers2, rightContainer);
        
        assertEquals(0, ((TestingItemContainer) copiedContainers1).getListeners().size());
        assertEquals(0, ((TestingItemContainer) copiedContainers2).getListeners().size());
        assertEquals(0, ((TestingSQLJoin) copiedJoin).getListeners().size());
        for (Item item : copiedContainers1.getItems()) {
            assertEquals(0, ((TestingStringItem) item).getListeners().size());
        }
        for (Item item : copiedContainers2.getItems()) {
            assertEquals(0, ((TestingStringItem) item).getListeners().size());
        }
    }
    
    /**
     * Tests the copy constructor of a query will copy columns, containers, 
     * and joins.
     */
    public void testCopyConstructorWithListeners() throws Exception {
        QueryImpl query = new QueryImpl(new StubDatabaseMapping());
        Item leftColumn = new TestingStringItem("leftCol");
        Container leftContainer = new TestingItemContainer("leftContainer");
        leftContainer.addItem(leftColumn);
        Item rightColumn = new TestingStringItem("rightCol");
        Container rightContainer = new TestingItemContainer("rightContainer");
        rightContainer.addItem(rightColumn);
        SQLJoin join = new TestingSQLJoin(leftColumn, rightColumn);
        query.addTable(leftContainer);
        query.addTable(rightContainer);
        query.addJoin(join);
        
        assertTrue(query.getFromTableList().contains(leftContainer));
        assertTrue(query.getFromTableList().contains(rightContainer));
        assertTrue(query.getJoins().contains(join));
        
        QueryImpl copy = new QueryImpl(query, true);
        
        assertEquals(2, copy.getFromTableList().size());
        assertEquals(1, copy.getJoins().size());
        
        final Container copiedContainers1 = copy.getFromTableList().get(0);
        final Container copiedContainers2 = copy.getFromTableList().get(1);
        final SQLJoin copiedJoin = copy.getJoins().iterator().next();
        
        assertTrue(copy.getJoinMapping().containsKey(copiedContainers1));
        assertTrue(copy.getJoinMapping().containsKey(copiedContainers2));
        assertTrue(copy.getJoinMapping().get(copiedContainers1).containsAll(copy.getJoins()));
        assertTrue(copy.getJoinMapping().get(copiedContainers2).containsAll(copy.getJoins()));
        assertNotSame(copiedContainers1, leftContainer);
        assertNotSame(copiedContainers2, leftContainer);
        assertNotSame(copiedContainers1, rightContainer);
        assertNotSame(copiedContainers2, rightContainer);
        
        assertEquals(1, ((TestingItemContainer) copiedContainers1).getListeners().size());
        assertTrue(((TestingItemContainer) copiedContainers1).getListeners().contains(copy.getTableChildListener()));
        assertEquals(1, ((TestingItemContainer) copiedContainers2).getListeners().size());
        assertTrue(((TestingItemContainer) copiedContainers2).getListeners().contains(copy.getTableChildListener()));
        assertEquals(1, ((TestingSQLJoin) copiedJoin).getListeners().size());
        assertTrue(((TestingSQLJoin) copiedJoin).getListeners().contains(copy.getJoinChangeListener()));
        for (Item item : copiedContainers1.getItems()) {
            assertEquals(1, ((TestingStringItem) item).getListeners().size());
            assertTrue(((TestingStringItem) item).getListeners().contains(copy.getItemListener()));
        }
        for (Item item : copiedContainers2.getItems()) {
            assertEquals(1, ((TestingStringItem) item).getListeners().size());
            assertTrue(((TestingStringItem) item).getListeners().contains(copy.getItemListener()));
        }
    }
    
    /**
     * This is a test to confirm that a query string can be created from a Query
     * object if the database is missing. This is for cases where the database
     * may not be able to be connected to.
     */
    public void testQueryConstructionWithMissingDB() throws Exception {
        Query query = new QueryImpl(new StubDatabaseMapping());
        Container container = new ItemContainer("Test_Table");
        Item item = new StringItem("column");
        container.addItem(item);
        query.addTable(container);
        query.selectItem(item);
        
        assertEquals(1, query.getSelectedColumns().size());
        assertTrue(query.getSelectedColumns().contains(item));
        
        String queryString = query.generateQuery();
        queryString = queryString.toLowerCase();
        
        System.out.println(queryString);
        assertTrue(queryString.toLowerCase().matches("select(.|\r|\n)*test_table.column(.|\r|\n)*from(.|\r|\n)*test_table(.|\r|\n)*test_table(.|\r|\n)*"));
    }
    
    /**
     * Simple test to confirm containers and joins can be added successfully
     * to a query.
     */
    public void testContainersAndJoinsAddToQuery() throws Exception {
        Query query = new QueryImpl(new StubDatabaseMapping());
        ItemContainer container1 = new ItemContainer("Table 1");
        StringItem item1 = new StringItem("Column 1");
        container1.addItem(item1);
        query.addTable(container1);
        
        ItemContainer container2 = new ItemContainer("Table 2");
        StringItem item2 = new StringItem("Column 2");
        container2.addItem(item2);
        query.addTable(container2);
        
        ItemContainer container3 = new ItemContainer("Table 3");
        StringItem item3 = new StringItem("Column 3");
        container3.addItem(item3);
        query.addTable(container3);

        SQLJoin join1to2 = new SQLJoin(item1, item2);
        SQLJoin join2to3 = new SQLJoin(item2, item3);
        query.addJoin(join1to2);
        query.addJoin(join2to3);
        
        assertEquals(3, query.getFromTableList().size());
        assertTrue(query.getFromTableList().contains(container1));
        assertTrue(query.getFromTableList().contains(container2));
        assertTrue(query.getFromTableList().contains(container3));
        assertEquals(2, query.getJoins().size());
        assertTrue(query.getJoins().contains(join1to2));
        assertTrue(query.getJoins().contains(join2to3));
    }
    
    /**
     * This test is to ensure that a simple graph of the query will have
     * proper edges and nodes.
     * @throws Exception
     */
    public void testGraphWithThreeTables() throws Exception {
        QueryImpl query = new QueryImpl(new StubDatabaseMapping());
        ItemContainer container1 = new ItemContainer("Table 1");
        StringItem item1 = new StringItem("Column 1");
        container1.addItem(item1);
        query.addTable(container1);
        
        ItemContainer container2 = new ItemContainer("Table 2");
        StringItem item2 = new StringItem("Column 2");
        container2.addItem(item2);
        query.addTable(container2);
        
        ItemContainer container3 = new ItemContainer("Table 3");
        StringItem item3 = new StringItem("Column 3");
        container3.addItem(item3);
        query.addTable(container3);

        SQLJoin join1to2 = new SQLJoin(item1, item2);
        SQLJoin join2to3 = new SQLJoin(item2, item3);
        query.addJoin(join1to2);
        query.addJoin(join2to3);
        
        assertEquals(3, query.getFromTableList().size());
        assertTrue(query.getFromTableList().contains(container1));
        assertTrue(query.getFromTableList().contains(container2));
        assertTrue(query.getFromTableList().contains(container3));
        assertEquals(2, query.getJoins().size());
        assertTrue(query.getJoins().contains(join1to2));
        assertTrue(query.getJoins().contains(join2to3));

        TableJoinGraph tableJoinGraph = query.new TableJoinGraph();
        
        //Check all containers and joins are in the graph
        assertEquals(3, tableJoinGraph.getNodes().size());
        assertTrue(tableJoinGraph.getNodes().contains(container1));
        assertTrue(tableJoinGraph.getNodes().contains(container2));
        assertTrue(tableJoinGraph.getNodes().contains(container3));
        assertEquals(4, tableJoinGraph.getEdges().size());
        assertTrue(tableJoinGraph.getEdges().contains(join1to2));
        assertTrue(tableJoinGraph.getEdges().contains(join2to3));
        
        //Check adjacent nodes are correct
        assertEquals(1, tableJoinGraph.getAdjacentNodes(container1).size());
        assertTrue(tableJoinGraph.getAdjacentNodes(container1).contains(container2));
        
        assertEquals(2, tableJoinGraph.getAdjacentNodes(container2).size());
        assertTrue(tableJoinGraph.getAdjacentNodes(container2).contains(container1));
        assertTrue(tableJoinGraph.getAdjacentNodes(container2).contains(container3));
        
        assertEquals(1, tableJoinGraph.getAdjacentNodes(container3).size());
        assertTrue(tableJoinGraph.getAdjacentNodes(container3).contains(container2));
        
        //Check inbound edges are correct
        assertEquals(1, tableJoinGraph.getInboundEdges(container1).size());
        assertTrue(tableJoinGraph.getInboundEdges(container1).contains(join1to2));
        
        assertEquals(2, tableJoinGraph.getInboundEdges(container2).size());
        assertTrue(tableJoinGraph.getInboundEdges(container2).contains(join1to2));
        assertTrue(tableJoinGraph.getInboundEdges(container2).contains(join2to3));
        
        assertEquals(1, tableJoinGraph.getInboundEdges(container3).size());
        assertTrue(tableJoinGraph.getInboundEdges(container3).contains(join2to3));
        
        //Check outbound edges are correct
        assertEquals(1, tableJoinGraph.getOutboundEdges(container1).size());
        assertTrue(tableJoinGraph.getOutboundEdges(container1).contains(join1to2));
        
        assertEquals(2, tableJoinGraph.getOutboundEdges(container2).size());
        assertTrue(tableJoinGraph.getOutboundEdges(container2).contains(join1to2));
        assertTrue(tableJoinGraph.getOutboundEdges(container2).contains(join2to3));
        
        assertEquals(1, tableJoinGraph.getOutboundEdges(container3).size());
        assertTrue(tableJoinGraph.getOutboundEdges(container3).contains(join2to3));
    }

	/**
	 * Tests adding and removing a sort order to an item in a query
	 * updates the query appropriately.
	 */
    public void testRemoveSortingFromColWithSorting() throws Exception {
    	Query q = new QueryImpl(new StubDatabaseMapping());
    	Item col1 = new StringItem("Col 1");
    	Item col2 = new StringItem("Col 2");
    	Container table = new TestingItemContainer("Test container");
    	table.addItem(col1);
    	table.addItem(col2);
    	q.addTable(table);
    	q.selectItem(col1);
    	q.selectItem(col2);
    	q.orderColumn(col1, OrderByArgument.ASC);
    	
    	String query = q.generateQuery().toLowerCase();
    	System.out.println(query);
		String selectAndSortRegex = "select(.|\n)*" + col1.getName().toLowerCase() 
			+ "(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*order by(.|\n)*" 
			+ col1.getName().toLowerCase() + "(.|\n)*";
		String selectNoSortRegex = "select(.|\n)*" + col1.getName().toLowerCase() 
			+ "(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*";
		assertTrue(query.matches(selectAndSortRegex));
		
		q.orderColumn(col1, OrderByArgument.NONE);
		query = q.generateQuery().toLowerCase();
		System.out.println(query);
		assertFalse(query.matches(selectAndSortRegex));
		assertTrue(query.matches(selectNoSortRegex));
    }

    /**
	 * Tests that removing a column that had a sort order from the query by
	 * un-selecting it removes it from the generated query.
	 */
    public void testUnselectColWithSorting() throws Exception {
    	Query q = new QueryImpl(new StubDatabaseMapping());
    	Item col1 = new StringItem("Col 1");
    	Item col2 = new StringItem("Col 2");
    	Container table = new TestingItemContainer("Test container");
    	table.addItem(col1);
    	table.addItem(col2);
    	q.addTable(table);
    	q.selectItem(col1);
    	q.selectItem(col2);
    	q.orderColumn(col1, OrderByArgument.ASC);
    	
    	String query = q.generateQuery().toLowerCase();
    	System.out.println(query);
		String selectAndSortRegex = "select(.|\n)*" + col1.getName().toLowerCase() 
			+ "(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*order by(.|\n)*" 
			+ col1.getName().toLowerCase() + "(.|\n)*";
		String selectNoSortRegex = "select(.|\n)*" + col1.getName().toLowerCase() 
			+ "(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*";
		assertTrue(query.matches(selectAndSortRegex));		
		
		q.unselectItem(col1);
		query = q.generateQuery().toLowerCase();
		assertFalse(query.matches(selectAndSortRegex));
		assertFalse(query.matches(selectNoSortRegex));
		System.out.println(query);
		assertTrue(query.matches("select(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*"));
	}
    
    /**
	 * Tests that removing a column that had a sort order from the query by
	 * un-selecting it removes it from the generated query and then selecting
	 * it adds it back in.
	 */
    public void testReselectColWithSorting() throws Exception {
    	Query q = new QueryImpl(new StubDatabaseMapping());
    	Item col1 = new StringItem("Col 1");
    	Item col2 = new StringItem("Col 2");
    	Container table = new TestingItemContainer("Test container");
    	table.addItem(col1);
    	table.addItem(col2);
    	q.addTable(table);
    	q.selectItem(col1);
    	q.selectItem(col2);
    	q.orderColumn(col1, OrderByArgument.ASC);
    	
    	String query = q.generateQuery().toLowerCase();
    	System.out.println(query);
		String selectAndSortRegex = "select(.|\n)*" + col1.getName().toLowerCase() 
			+ "(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*order by(.|\n)*" 
			+ col1.getName().toLowerCase() + "(.|\n)*";
		String selectNoSortRegex = "select(.|\n)*" + col1.getName().toLowerCase() 
			+ "(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*";
		assertTrue(query.matches(selectAndSortRegex));		
		
		q.unselectItem(col1);
		query = q.generateQuery().toLowerCase();
		assertFalse(query.matches(selectAndSortRegex));
		assertFalse(query.matches(selectNoSortRegex));
		System.out.println(query);
		assertTrue(query.matches("select(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*"));
		
		String reselectWithSortRegex = "select(.|\n)*" + col2.getName().toLowerCase() 
			+ "(.|\n)*" + col1.getName().toLowerCase() + "(.|\n)*order by(.|\n)*" 
			+ col1.getName().toLowerCase() + "(.|\n)*";
		q.selectItem(col1);
		query = q.generateQuery().toLowerCase();
		assertTrue(query.matches(reselectWithSortRegex));
	}
    
    /**
     * Tests adding an item with its order by set will return
     * a query with the order by in it.
     */
    public void testAddingItemWithOrder() throws Exception {
    	Query q = new QueryImpl(new StubDatabaseMapping());
    	Item col1 = new StringItem("Col 1");
    	Item col2 = new StringItem("Col 2");
    	Container table = new TestingItemContainer("Test container");
    	table.addItem(col1);
    	table.addItem(col2);
    	q.addTable(table);
    	q.selectItem(col1);
    	q.selectItem(col2);
    	q.orderColumn(col1, OrderByArgument.ASC);
    	
    	String query = q.generateQuery().toLowerCase();
    	System.out.println(query);
		String selectAndSortRegex = "select(.|\n)*" + col1.getName().toLowerCase() 
			+ "(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*order by(.|\n)*" 
			+ col1.getName().toLowerCase() + "(.|\n)*";
		assertTrue(query.matches(selectAndSortRegex));
		assertEquals(0, col1.getOrderByOrdering().intValue());
		
		Item col3 = new StringItem("Col 3");
		q.selectItem(col3);
		q.orderColumn(col3, OrderByArgument.DESC);
		table.addItem(col3);
		query = q.generateQuery().toLowerCase();
		String colAddedWithSort = "select(.|\n)*" + col1.getName().toLowerCase() 
			+ "(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*" 
			+ col3.getName().toLowerCase() + "(.|\n)*order by(.|\n)*" 
			+ col1.getName().toLowerCase() + "(.|\n)*" + col3.getName().toLowerCase() 
			+ "(.|\n)*";
		query = q.generateQuery().toLowerCase();
		System.out.println(query);
		assertTrue(query.matches(colAddedWithSort));
		assertEquals(0, col1.getOrderByOrdering().intValue());
		assertEquals(1, col3.getOrderByOrdering().intValue());
		assertEquals(col1, q.getOrderByList().get(0));
		assertEquals(col3, q.getOrderByList().get(1));
	}
    
    /**
     * Test to ensure a column that is selected is added to the query and
     * if it is un-selected it is removed.
     */
    public void testSelectColumn() throws Exception {
    	Query q = new QueryImpl(new StubDatabaseMapping());
    	Item col1 = new StringItem("Col 1");
    	Item col2 = new StringItem("Col 2");
    	Container table = new TestingItemContainer("Test container");
    	table.addItem(col1);
    	table.addItem(col2);
    	q.addTable(table);
    	q.selectItem(col1);
    	q.selectItem(col2);
    	
    	assertEquals(2, q.getSelectedColumns().size());
    	assertTrue(q.getSelectedColumns().contains(col1));
    	assertTrue(q.getSelectedColumns().contains(col2));
    	
    	String selectRegex = "select(.|\n)*" + col1.getName().toLowerCase() +
    		"(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*from(.|\n)*";
    	String query = q.generateQuery().toLowerCase();
    	assertTrue(query.matches(selectRegex));
    	
    	String unselectRegex = "select(.|\n)*" + col2.getName().toLowerCase() +
    	"(.|\n)*from(.|\n)*";
    	q.unselectItem(col1);
    	query = q.generateQuery().toLowerCase();
    	assertFalse(query.matches(selectRegex));
    	assertTrue(query.matches(unselectRegex));
    	
    	String reselectRegex = "select(.|\n)*" + col2.getName().toLowerCase() +
			"(.|\n)*" + col1.getName().toLowerCase() + "(.|\n)*from(.|\n)*";
    	q.selectItem(col1);
    	query = q.generateQuery().toLowerCase();
    	assertFalse(query.matches(selectRegex));
    	assertTrue(query.matches(reselectRegex));
	}

	/**
	 * Simple test to confirm that grouping is added and removed appropriately
	 * when it is added to an item and when an item with grouping is removed
	 * from a query.
	 * 
	 * @throws Exception
	 */
    public void testGroupingOnItem() throws Exception {
    	Query q = new QueryImpl(new StubDatabaseMapping());
    	Item col1 = new StringItem("Col 1");
    	Item col2 = new StringItem("Col 2");
    	Container table = new TestingItemContainer("Test container");
    	table.addItem(col1);
    	table.addItem(col2);
    	q.addTable(table);
    	q.selectItem(col1);
    	q.selectItem(col2);
    	
    	String selectRegex = "select(.|\n)*" + col1.getName().toLowerCase() +
    		"(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*from(.|\n)*";
    	String query = q.generateQuery().toLowerCase();
    	assertTrue(query.matches(selectRegex));
    	
    	String groupingRegex = "select(.|\n)*" + col1.getName().toLowerCase() +
			"(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*from(.|\n)*group by(.|\n)*" +
			"(.|\n)*" + col1.getName().toLowerCase() + "(.|\n)*";
    	col1.setGroupBy(SQLGroupFunction.SUM);
    	q.setGroupingEnabled(false);
    	query = q.generateQuery().toLowerCase();
    	assertTrue(query.matches(selectRegex));
    	assertFalse(query.matches(groupingRegex));

    	String summingRegex = "select(.|\n)*sum(.|\n)*" + col1.getName().toLowerCase() +
			"(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*from(.|\n)*";
    	q.setGroupingEnabled(true);
    	col1.setGroupBy(SQLGroupFunction.SUM);
    	query = q.generateQuery().toLowerCase();
    	assertFalse(query.matches(groupingRegex));
    	assertTrue(query.matches(summingRegex));
    	
    	col1.setGroupBy(SQLGroupFunction.GROUP_BY);
    	query = q.generateQuery().toLowerCase();
    	assertFalse(query.matches(summingRegex));
    	assertTrue(query.matches(groupingRegex));
    	
    	String failRegex = "select(.|\n)*" + col1.getName().toLowerCase() +
			"(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*from(.|\n)*";
    	String col2Regex = "select(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*";
    	q.unselectItem(col1);
    	query = q.generateQuery().toLowerCase();
    	System.out.println(query);
    	assertFalse(query.matches(failRegex));
    	assertTrue(query.matches(col2Regex));
	}

    /**
     * Test that a correct query is returned when a column that is being
     * aggregated is removed from the query.
     * @throws Exception
     */
    public void testUnselectColumnWithAggregate() throws Exception {
    	Query q = new QueryImpl(new StubDatabaseMapping());
    	Item col1 = new StringItem("Col 1");
    	Item col2 = new StringItem("Col 2");
    	Container table = new TestingItemContainer("Test container");
    	table.addItem(col1);
    	table.addItem(col2);
    	q.addTable(table);
    	q.selectItem(col1);
    	q.selectItem(col2);
    	
    	String selectRegex = "select(.|\n)*" + col1.getName().toLowerCase() +
    		"(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*from(.|\n)*";
    	String query = q.generateQuery().toLowerCase();
    	assertTrue(query.matches(selectRegex));
    	
    	String groupingRegex = "select(.|\n)*" + col1.getName().toLowerCase() +
			"(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*from(.|\n)*group by(.|\n)*" +
			"(.|\n)*" + col1.getName().toLowerCase() + "(.|\n)*";
    	String summingRegex = "select(.|\n)*sum(.|\n)*" + col1.getName().toLowerCase() +
			"(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*from(.|\n)*";
    	q.setGroupingEnabled(true);
    	col1.setGroupBy(SQLGroupFunction.SUM);
    	query = q.generateQuery().toLowerCase();
    	assertFalse(query.matches(groupingRegex));
    	assertTrue(query.matches(summingRegex));
    	
    	String failRegex = "select(.|\n)*" + col1.getName().toLowerCase() +
			"(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*from(.|\n)*";
    	String col2Regex = "select(.|\n)*" + col2.getName().toLowerCase() + "(.|\n)*";
    	q.unselectItem(col1);
    	query = q.generateQuery().toLowerCase();
    	System.out.println(query);
    	assertFalse(query.matches(failRegex));
    	assertTrue(query.matches(col2Regex));
	}

    /**
     * Basic reset test.
     * @throws Exception
     */
    public void testResetQuery() throws Exception {
       Query q = new QueryImpl(new StubDatabaseMapping());
       String startingWhereClause = q.getGlobalWhereClause();
       q.setGlobalWhereClause("something");
       boolean startingGroupingFlag = q.isGroupingEnabled();
       q.setGroupingEnabled(true);
       int startingZoomLevel = q.getZoomLevel();
       q.setZoomLevel(75);
       Container constantsContainer = q.getConstantsContainer();
       List<Item> constantItems = constantsContainer.getItems();
       constantsContainer.removeItem(0);
       constantsContainer.addItem(new StringItem("New Constant"));
       
       Container container1 = new ItemContainer("Container 1");
       Item item1 = new StringItem("Item 1");
       container1.addItem(item1);
       q.addTable(container1);
       Container container2 = new ItemContainer("Container 2");
       Item item2 = new StringItem("item 2");
       container2.addItem(item2);
       q.addTable(container2);
       q.selectItem(item1);
       q.selectItem(item2);
       q.orderColumn(item1, OrderByArgument.ASC);
       q.orderColumn(item2, OrderByArgument.DESC);
       SQLJoin join = new SQLJoin(item1, item2);
       q.addJoin(join);
       
       q.reset();
       assertEquals(startingZoomLevel, q.getZoomLevel());
       assertEquals(startingWhereClause, q.getGlobalWhereClause());
       assertEquals(startingGroupingFlag, q.isGroupingEnabled());
       
       Container resetConstants = q.getConstantsContainer();
       assertEquals(constantItems.size(), resetConstants.getItems().size());
       for (int i = 0; i < constantItems.size(); i++) {
           assertEquals(constantItems.get(i).getName(), 
                   resetConstants.getItems().get(i).getName());
       }
       
       assertTrue(q.getFromTableList().isEmpty());
       assertTrue(q.getJoins().isEmpty());
       assertTrue(q.getOrderByList().isEmpty());
       assertTrue(q.getSelectedColumns().isEmpty());
    }

    /**
     * Tests that adding and removing a container will fire the correct event
     * from the query.
     */
    public void testContainerFiresEvent() throws Exception {
        CountingChangeListener listener = new CountingChangeListener();
        QueryImpl query = new QueryImpl(new StubDatabaseMapping());
        query.addQueryChangeListener(listener);
        Container container = new ItemContainer("container");
        Item item = new StringItem("name");
        container.addItem(item);
        
        assertEquals(0, listener.getContainerAddedCount());
        query.addTable(container);
        
        assertEquals(1, listener.getContainerAddedCount());
        QueryChangeEvent evt = listener.getLastQueryChangeEvent();
        assertEquals(container, evt.getContainerChanged());
        assertEquals(query, evt.getSource());
        
        assertEquals(0, listener.getContainerRemovedCount());
        query.removeTable(container);
        
        assertEquals(1, listener.getContainerRemovedCount());
        evt = listener.getLastQueryChangeEvent();
        assertEquals(container, evt.getContainerChanged());
        assertEquals(query, evt.getSource());
    }
    
    /**
     * This method tests moving an Item around in the selection list
     * of a query.
     */
    public void testMoveColumn() throws Exception {
        QueryImpl query = new QueryImpl(new StubDatabaseMapping());
        Container container = new ItemContainer("Container");
        Item item1 = new StringItem("item1");
        container.addItem(item1);
        Item item2 = new StringItem("item2");
        container.addItem(item2);
        
        Container container2 = new ItemContainer("Container2");
        Item item3 = new StringItem("item3");
        container2.addItem(item3);
        Item item4 = new StringItem("item4");
        container2.addItem(item4);
        
        query.addTable(container);
        query.addTable(container2);
        
        query.selectItem(item1);
        query.selectItem(item2);
        query.selectItem(item3);
        query.selectItem(item4);
        
        assertEquals(item1, query.getSelectedColumns().get(0));
        assertEquals(0, item1.getSelected().intValue());
        assertEquals(item2, query.getSelectedColumns().get(1));
        assertEquals(1, item2.getSelected().intValue());
        assertEquals(item3, query.getSelectedColumns().get(2));
        assertEquals(2, item3.getSelected().intValue());
        assertEquals(item4, query.getSelectedColumns().get(3));
        assertEquals(3, item4.getSelected().intValue());
        
        query.moveItem(item3, 0);
        
        assertEquals(item3, query.getSelectedColumns().get(0));
        assertEquals(0, item3.getSelected().intValue());
        assertEquals(item1, query.getSelectedColumns().get(1));
        assertEquals(1, item1.getSelected().intValue());
        assertEquals(item2, query.getSelectedColumns().get(2));
        assertEquals(2, item2.getSelected().intValue());
        assertEquals(item4, query.getSelectedColumns().get(3));
        assertEquals(3, item4.getSelected().intValue());
        
        query.moveItem(item1, 3);
        
        assertEquals(item3, query.getSelectedColumns().get(0));
        assertEquals(0, item3.getSelected().intValue());
        assertEquals(item2, query.getSelectedColumns().get(1));
        assertEquals(1, item2.getSelected().intValue());
        assertEquals(item4, query.getSelectedColumns().get(2));
        assertEquals(2, item4.getSelected().intValue());
        assertEquals(item1, query.getSelectedColumns().get(3));
        assertEquals(3, item1.getSelected().intValue());
    }
    
}
