package ca.sqlpower.sql;

public class NonNumericToWildcardFilter implements ColumnFilter {
    static final int STATE_NUMBERS=0;
    static final int STATE_NON_NUMBERS=1;
    char wildcard='%';

    public static ColumnFilter getInstance() {
	return (ColumnFilter)new NonNumericToWildcardFilter();
    }

    public String filter(String in) {
	char ch;
	int state=STATE_NUMBERS;
	StringBuffer out=new StringBuffer(in.length());

	for(int i=0; i<in.length(); i++) {
	    ch=in.charAt(i);
	    if(ch < '0' || ch > '9') {
		if(state==STATE_NUMBERS) {
		    out.append(wildcard);
		}
		state=STATE_NON_NUMBERS;
	    } else {
		out.append(ch);
		state=STATE_NUMBERS;
	    }
	}
	return out.toString();
    }

}
