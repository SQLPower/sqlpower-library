package ca.sqlpower.sql;

public class NonNumericToWildcardFilter implements ColumnFilter {
    static final int STATE_NUMBERS=0;
    static final int STATE_NON_NUMBERS=1;
    char wildcard='%';
    boolean leadingWildcard=false;
    boolean trailingWildcard=false;

    public static ColumnFilter getInstance() {
	return (ColumnFilter)new NonNumericToWildcardFilter();
    }

    public void setWildcardCharacter(char wild) {
	wildcard=wild;
    }

    public void setForceLeadingWildcard(boolean enabled) {
	leadingWildcard=enabled;
    }

    public void setForceTrailingWildcard(boolean enabled) {
	trailingWildcard=enabled;
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

	if(out.charAt(0) != wildcard) {
	    out.insert(0, wildcard);
	}

	if(out.charAt(out.length()-1) != wildcard) {
	    out.append(wildcard);
	}

	return out.toString();
    }

}
