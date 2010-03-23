/*
 * Created on Aug 9, 2007
 *
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.sql;

import java.sql.SQLException;

/**
 * The RowFilter interface specifies a method for accepting or rejecting
 * a row of data based on whatever criteria the implemented chooses.
 *
 * @version $Id:$
 */
public interface RowFilter {

    /**
     * Returns true if and only if this row of data meets the set of criteria
     * determined by the filter implementation.
     * 
     * @param row
     *            The row of data to evaluate
     * @return true if the row passes this filter; false if it is rejected by
     *         this filter.
     * @throws SQLException
     *             if there are any database errors encountered while processing
     *             the given row.
     */
    boolean acceptsRow(Object[] row) throws SQLException;
}
