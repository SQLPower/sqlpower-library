package ca.sqlpower.sql;

import java.io.*;
import java.sql.SQLException;

public abstract class WebResultFormatter {
    public abstract void formatToStream(WebResultSet wrs, PrintWriter out) 
	throws SQLException;
}
