package ca.sqlpower.util;

public class Passwords {
    private Passwords() {
    }

    public static int QUALITY_LENIENT=1;
    public static int QUALITY_STRICT=9;

    /**
     * 
     */
    public static boolean checkQuality(String pw, int severity) {

	// Long enough?
	if(pw.length() < 6) {
	    return false;
	}

	boolean hasUpperCase=false;
	boolean hasLowerCase=false;
	boolean hasDigits=false;
	boolean hasPunctuation=false;

	for(int i=0; i<pw.length(); i++) {
	    char ch=pw.charAt(i);
	    if( (ch < 32) && (ch > 126) ) {
		return false;
	    } else if(('A' <= ch) && (ch <= 'Z')) {
		hasUpperCase=true;
	    } else if(('a' <= ch) && (ch <= 'z')) {
		hasLowerCase=true;
	    } else if(('0' <= ch) && (ch <= '9')) {
		hasDigits=true;
	    } else {
		hasPunctuation=true;
	    }
	}

	// If it's long enough and contains no nasties, that's enough
	// for a lenient check.
	if(severity <= QUALITY_LENIENT) {
	    return true;
	}

	// Otherwise, we get picky...
	if(hasUpperCase && hasLowerCase && (hasDigits || hasPunctuation)) {
	    return true;
	}

	return false;
    }
}
