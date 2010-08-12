/*
 * Copyright (c) 2010, SQL Power Group Inc.
 */

package ca.sqlpower.dao;


/**
 * A persistence exception that signifies that this is not a critical exception
 * but the user should still be notified about the error. These exceptions can
 * be used to roll back the state of the system but still show a nice, friendly
 * prompt to the user instead of the scary red ones with stack traces. 
 */
public class FriendlyRuntimeSPPersistenceException extends RuntimeException {

    /**
     * The message passed into this exception will be displayed to the user
     * so be sure it is user-readable and friendly.
     */
    public FriendlyRuntimeSPPersistenceException(String message) {
        super(message);
    }
}
