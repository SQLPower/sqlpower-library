package ca.sqlpower.sql;

public class SQL {

    /**
     * This class cannot be instantiated
     */
    private SQL()
    {
    }

    public static String escapeStatement(String old) 
    { 
	// a premature optimisation 
	if(old.lastIndexOf('\'') == -1) { 
	    return old; 
	} 
	
	int i=0; 
	StringBuffer escaped=new StringBuffer(old); 
	
	while(i < escaped.length()) 
	    { 
		if(escaped.charAt(i)=='\'') { 
		    escaped.insert(i, '\''); 
		    i++;  // skip over the added quote 
		} 
		i++; 
	    } 
	return(escaped.toString()); 
    } 

    public static boolean decodeInd(String indicator) {
	if(indicator.charAt(0) == 'Y') {
	    return true;
	}
	return false;
    }
}
