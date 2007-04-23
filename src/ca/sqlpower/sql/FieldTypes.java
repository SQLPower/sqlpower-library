package ca.sqlpower.sql;

/**
 * FieldTypes is a container class for all the types of data the
 * WebResultFormatter classes know about.
 * <p>
 * 
 * WARNING: Because of the way dependencies in the Java compiler work, it is a
 * bad idea to change any of these values. If you must change a value (say, you
 * want NUMBER = 5 instead of NUMBER = 1), you must also recompile everything
 * that depends on FieldTypes.NUMBER. If you don't do this, all the classes you
 * failed to recompile will continue to use the value NUMBER = 1.
 * 
 * @author Jonathan Fuerth
 * @version $Id$
 */
public class FieldTypes {

    /**
     * Indicates a data field representing an unspecified type of
     * data.
     */
    public static final int UNKNOWN = 0;

    /**
     * Indicates a data field representing a numeric value.
     */
    public static final int NUMBER = 1;

    /**
     * Indicates a data field representing a textual name.
     */
    public static final int NAME = 2;

    /**
     * Indicates a data field representing a monetary value (in
     * dollars).
     */
    public static final int MONEY = 3;

    /**
     * Indicates a data field representing a yes/no value.
     */
    public static final int BOOLEAN = 4;


    /**
     * Indicates a data field representing a radio button.
     */
    public static final int RADIO = 5;

    /**
     * Indicates a data field representing a checkbox.
     */
    public static final int CHECKBOX = 6;

    /**
     * Indicates a data field representing a percentage (integer or
     * floating point value between 0 and 100).
     */
    public static final int PERCENT = 7;

    /**
     * Indicates a data field representing a date or timestamp
     * (date+time).
     */
    public static final int DATE = 8;

    /**
     * Indicates a data field representing a short alphanumeric code.
     */
    public static final int ALPHANUM_CODE = 9;

    /**
     * Indicates a data field representing a unique
     * (non-human-readable) row identifier.
     */
    public static final int ROWID = 10;    

    /**
     * Indicates a data field representing a sentence or paragraph of
     * text.
     */
    public static final int TEXT = 11;

    /**
     * Indicates a data field representing nothing.  Fields of this
     * type should not be displayed to the user, but search criteria
     * (dropdown lists, etc) will still be applied.
     */
    public static final int DUMMY = 12;

    /**
     * Indicates a field representing a checkbox which is mutually
     * exclusive with all other MUTEX_CHECKBOXes in its row (see
     * WARNINGs).  Note that this is not the same as RADIO because the
     * exclusion applies to rowss, not columns; and it is possible to
     * deselect all boxes in a group.<p>
     *
     * WARNING! The MUTEX_CHECKBOX type is only supported (currently)
     * by the WebResultHTMLFormatter.  Using it elsewhere will signal
     * an UnsupportedOperationException when you try to format the
     * WebResultSet.<p>
     *
     * SECOND WARNING! The checkboxes will behave incorrectly if there
     * is any row where some (but not all) checkboxes are NULL.
     */
    public static final int MUTEX_CHECKBOX = 13;

    /**
     * Indicates a data field representing a password.  Its exact
     * value should not be displayed to the user, but it's ok to
     * indicate the presence or absence of a value ("*****" vs
     * "none").
     */
    public static final int PASSWORD = 14;


 	/**
 	 * Specifies that each cell in this column should be rendered as a
 	 * set of hyperlinks.  For all columns of this type, you must call
 	 * <code>setColumnHyperlinks</code> on the curresponding
 	 * <code>WebResultSet</code> column.  The hyperlinks are currently
 	 * always rendered as HTML, but this may change in the future.
 	 * PDF supports hyperlinks, for instance.
     * 
     * @deprecated The classes that consume result sets (formatters) should
     * handle hyperlinks on their own. Hyperlinks are a view idea, and it's
     * too complex to try and model the notion of a generic hyperlink here.
 	 */
 	public static final int HYPERLINK = 15;

    /**
     * @deprecated The classes that consume result sets (formatters) should
     * handle hyperlinks on their own. Hyperlinks are a view idea, and it's
     * too complex to try and model the notion of a generic hyperlink here.
     */
	public static final int RANGEHYPERLINK = 21;


    /**
     * Indicates a data field representing a yes/no value.
	 * Default to 'N' if no value.
     */
    public static final int YESNO_DEFAULT_NO = 16;

    /**
     * Indicates a data field representing a yes/no value.
	 * Default to 'Y' if no value.
     */
    public static final int YESNO_DEFAULT_YES = 17;

    /**
     * Indicates a data field representing a text value.
	 * Default to 'n/a' if no value.
     */
    public static final int TEXT_DEFAULT_NA = 18;

    /**
     * Indicates a data field representing a text value.
	 * Default to 'Unknown' if no value.
     */
    public static final int TEXT_DEFAULT_UNKNOWN = 19;

    /**
     * Indicates a data field representing a text value.
	 * Default to 'None' if no value.
     */
    public static final int TEXT_DEFAULT_NONE = 20;


    /**
     * It is guaranteed that no type code's value exceeds LAST_TYPE.
     */
    public static final int LAST_TYPE=21;

    /**
     * This class is just a container for the type values; it cannot
     * be instantiated.
     */
    protected FieldTypes() {
    }

	/**
	 * Translates an integer type code into its name.  Only use this
	 * for debugging; the format of these strings is subject to
	 * change!
	 */
	public static String nameOf(int fieldType) {
		if (fieldType == UNKNOWN) return "UNKNOWN ("+UNKNOWN+")";
		else if (fieldType == NUMBER) return "NUMBER ("+NUMBER+")";
		else if (fieldType == NAME) return "NAME ("+NAME+")";
		else if (fieldType == MONEY) return "MONEY ("+MONEY+")";
		else if (fieldType == BOOLEAN) return "BOOLEAN ("+BOOLEAN+")";
		else if (fieldType == RADIO) return "RADIO ("+RADIO+")";
		else if (fieldType == CHECKBOX) return "CHECKBOX ("+CHECKBOX+")";
		else if (fieldType == PERCENT) return "PERCENT ("+PERCENT+")";
		else if (fieldType == DATE) return "DATE ("+DATE+")";
		else if (fieldType == ALPHANUM_CODE) return "ALPHANUM_CODE ("+ALPHANUM_CODE+")";
		else if (fieldType == ROWID) return "ROWID ("+ROWID+")";
		else if (fieldType == TEXT) return "TEXT ("+TEXT+")";
		else if (fieldType == DUMMY) return "DUMMY ("+DUMMY+")";
		else if (fieldType == MUTEX_CHECKBOX) return "MUTEX_CHECKBOX ("+MUTEX_CHECKBOX+")";
		else if (fieldType == PASSWORD) return "PASSWORD ("+PASSWORD+")";
		else if (fieldType == HYPERLINK) return "HYPERLINK ("+HYPERLINK+")";
		else if (fieldType == RANGEHYPERLINK) return "RANGEHYPERLINK ("+RANGEHYPERLINK+")";
		else if (fieldType == YESNO_DEFAULT_NO) return "YESNO_DEFAULT_NO ("+YESNO_DEFAULT_NO+")";
		else if (fieldType == YESNO_DEFAULT_YES) return "YESNO_DEFAULT_YES ("+YESNO_DEFAULT_YES+")";
		else if (fieldType == TEXT_DEFAULT_NA) return "TEXT_DEFAULT_NA ("+TEXT_DEFAULT_NA+")";
		else if (fieldType == TEXT_DEFAULT_UNKNOWN) return "TEXT_DEFAULT_UNKNOWN ("+TEXT_DEFAULT_UNKNOWN+")";
		else if (fieldType == TEXT_DEFAULT_NONE) return "TEXT_DEFAULT_NONE ("+TEXT_DEFAULT_NONE+")";
		else if (fieldType == LAST_TYPE) return "LAST_TYPE ("+LAST_TYPE+")";
		else return "*** Unknown Type ("+fieldType+") ***";
	}
}
