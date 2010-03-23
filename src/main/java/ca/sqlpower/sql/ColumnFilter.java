package ca.sqlpower.sql;

    /**
     * The ColumnFilter interface defines an output filter for a
     * WebResultSet.  The single specified method, filter, gets called
     * by Web Result Formatters with the raw (String) contents of the
     * column.  Further processing in the Formatter classes is done on
     * the value returned by the call to filter.
     *
     * @author Jonathan Fuerth
     * @version $Id$
     */
public interface ColumnFilter {

    /**
     * Takes an input string, applies an implementation-specific
     * transformation on it, and returns the result.  Implementors are
     * expected to supplement their ColumnFilter subclasses with
     * getXXX() and setXXX() proterty methods which will influence the
     * transformation.
     *
     * @param in The original value in the resultset, as returned by
     * WebResultSet.getString(n).
     * @return Some mutation of the original string.
     */
    public String filter(String in);
}
