package ca.sqlpower.sql;

public class FieldTypes {

    /**
     * Indicates a data field representing an unspecified type of
     * data.
     */
    public static final int UNKNOWN = 1;

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
     * Indicates a data field representing a date, stored as a
     * java.util.Date object (or apropriate to the
     * java.sql.ResultSet.getDate() method).
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
     * It is guaranteed that no type code's value exceeds LAST_TYPE.
     */
    public static final int LAST_TYPE=11;

    /**
     * This class is just a container for the type values; it cannot
     * be instantiated.
     */
    private FieldTypes() {
    }
}
