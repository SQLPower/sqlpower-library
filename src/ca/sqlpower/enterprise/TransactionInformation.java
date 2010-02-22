package ca.sqlpower.enterprise;

import java.text.DateFormat;
import java.util.Date;

/**
 * Simple container class for information regarding a particular revision.
 */
public class TransactionInformation {
    
    public final static DateFormat DATE_FORMAT = 
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    private final long versionNumber;
    private final Date timeCreated;
    private final String versionAuthor;
    private final String versionDescription;
    
    /**
     * Create an TransactionInformation object
     * 
     * @param versionNumber
     * @param timeCreated
     * @param versionAuthor
     * @param versionDescription
     */
    public TransactionInformation(long versionNumber, Date timeCreated, String versionAuthor, String versionDescription) {
        this.versionNumber = versionNumber;
        this.timeCreated = timeCreated;
        this.versionAuthor = versionAuthor;
        this.versionDescription = versionDescription;
    }
    
    /**
     * Returns a formatted list of strings describing this transaction.
     */
    public String toString() {
        return "v" + versionNumber + " (" + timeCreated.toString() + ")" +
                ", " + versionAuthor + ":" + versionDescription;
    }

    public long getVersionNumber() {
        return versionNumber;
    }

    public Date getTimeCreated() {
        return timeCreated;
    }

    public String getVersionAuthor() {
        return versionAuthor;
    }

    public String getVersionDescription() {
        return versionDescription;
    }
    
    public String getTimeString() {
        return DATE_FORMAT.format(timeCreated);
    }        
    
}