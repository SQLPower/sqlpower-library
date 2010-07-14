package ca.sqlpower.enterprise;

import java.text.DateFormat;

/**
 * Simple container class for information regarding a particular revision.
 */
public class TransactionInformation {
    
    public final static DateFormat DATE_FORMAT = 
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    private final long versionNumber;
    private final long timeCreated; //in millis
    private final String versionAuthor;
    private final String versionDescription;
    private final String simpleDescription;
    
    public TransactionInformation(long versionNumber, long timeCreated, 
            String versionAuthor, String versionDescription) {
        this(versionNumber, timeCreated, versionAuthor, versionDescription, versionDescription);
    }
    
    /**
     * Create an TransactionInformation object
     * 
     * @param versionNumber
     * @param timeCreated
     * @param versionAuthor
     * @param versionDescription
     * @param simpleDescription
     */
    public TransactionInformation(long versionNumber, long timeCreated, 
            String versionAuthor, String versionDescription, String simpleDescription) {
        this.versionNumber = versionNumber;
        this.timeCreated = timeCreated;
        this.versionAuthor = versionAuthor;
        this.versionDescription = versionDescription;
        this.simpleDescription = simpleDescription;
    }
    
    /**
     * Returns a formatted list of strings describing this transaction.
     */
    public String toString() {
        return "v" + versionNumber + " (" + DATE_FORMAT.format(timeCreated) + ")" +
                ", " + versionAuthor + ":" + simpleDescription;
    }

    public long getVersionNumber() {
        return versionNumber;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public String getVersionAuthor() {
        return versionAuthor;
    }

    public String getVersionDescription() {
        return versionDescription;
    }
    
    public String getSimpleDescription() {
        return simpleDescription;
    }
    
}