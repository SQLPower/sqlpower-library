package ca.sqlpower.sql;

public class FieldTypes {
    public static final int NUMBER = 1;
    public static final int NAME = 2;
    public static final int MONEY = 3;
    public static final int BOOLEAN = 4;
    public static final int RADIO = 5;
    public static final int CHECKBOX = 6;
    public static final int PERCENT = 7;
    public static final int DATE = 8;
    static final int LAST_TYPE=8;

    /**
     * This class is just a container for the type values; it cannot
     * be instantiated.
     */
    private FieldTypes() {
    }
}
