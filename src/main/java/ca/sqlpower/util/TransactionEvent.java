/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of Wabit.
 *
 * Wabit is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wabit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.util;

/**
 * A transaction event describes the start or end of a group of events where the
 * group of events as a whole should either be acted on or none of the events
 * should be acted on.
 * 
 * @see TransactionEventFactory
 */
public class TransactionEvent {
    
    public enum TransactionState {
        START,
        END,
        ROLLBACK
    }
    
    /**
     * Call this method to create a start transaction event.
     * 
     * @param source
     *            The source of the event.
     * @param message
     *            A message describing the transaction.
     */
    public static TransactionEvent createStartTransactionEvent(Object source, String message) {
        return new TransactionEvent(source, message, TransactionState.START);
    }
    
    /**
     * Call this constructor to create an end transaction event.
     * 
     * @param source
     *            The source of the event.
     */
    public static TransactionEvent createEndTransactionEvent(Object source) {
        return new TransactionEvent(source);
    }
    
    /**
     * Call this constructor to create an end transaction event.
     * 
     * @param source
     *            The source of the event.
     */
    public static TransactionEvent createEndTransactionEvent(Object source, String message) {
    	return new TransactionEvent(source, message, TransactionState.END);
    }
    
    public static TransactionEvent createRollbackTransactionEvent(Object source, String message) {
        return new TransactionEvent(source, message, TransactionState.ROLLBACK);
    }
    
    /**
     * The object that is starting or stopping a transaction.
     */
    private final Object source;
    
    /**
     * A message describing the start or stop of a transaction.
     */
    private final String message;
    
    /**
     * The state this event is describing. It can either be starting
     * or stopping a transaction.
     */
    private final TransactionState state;

    /**
     * Call this constructor to create a transaction event with a message.
     * 
     * @see TransactionEventFactory
     * 
     * @param source
     *            The source of the event.
     * @param message
     *            A message describing the transaction.
     */
    private TransactionEvent(Object source, String message, TransactionState state) {
        this.source = source;
        this.message = message;
        this.state = state;
    }

    /**
     * Call this constructor to create an end transaction event.
     * 
     * @see TransactionEventFactory
     * 
     * @param source
     *            The source of the event.
     */
    private TransactionEvent(Object source) {
        this.source = source;
        message = "";
        state = TransactionState.END;
    }

    public Object getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public TransactionState getState() {
        return state;
    }

}
