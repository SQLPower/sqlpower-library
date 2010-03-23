/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import ca.sqlpower.security.EmailNotification.EmailRecipient;

public class EmailAppender extends AppenderSkeleton {

	/**
	 * The email this appender is responsible for.
	 */
	private final Email email;
	
	/**
	 * The three lists of recipients for each status.
	 */
	private final List<EmailRecipient> greenRecipients;
	private final List<EmailRecipient> yellowRecipients;
	private final List<EmailRecipient> redRecipients;
	
	/**
	 * The constants for possible statuses.
	 */
	private static final String GREEN_STATUS = "green";
	private static final String YELLOW_STATUS = "yellow";
	private static final String RED_STATUS = "red";
	
	/**
	 * Keeps track of the current status from the logger.
	 */
	private String status = GREEN_STATUS;
	
	/**
	 * The original email subject containing the project name and engine type.
	 */
	private final String emailSubject;
	
	/**
	 * Creates a new EmailAppender that will fill an email with logger messages
	 * and send to the appropriate users according to the status. Call {@link #close()}
	 * to send the email.
	 */
	public EmailAppender(Email email, String emailSubject, List<EmailRecipient> greenRecipients,
			List<EmailRecipient> yellowRecipients, List<EmailRecipient> redRecipients) {
		this.email = email;
		this.greenRecipients = greenRecipients;
		this.yellowRecipients = yellowRecipients;
		this.redRecipients = redRecipients;
		this.emailSubject = emailSubject;
	}
	
	/**
	 * The method called by logger every time a message is added. Adds
	 * the message to the email and updates the status.
	 */
	@Override
	protected void append(LoggingEvent e) {
		if (email.getEmailBody().length() > 0) {
			email.appendToEmailBody("\n");
		}
		email.appendToEmailBody(new Date(e.timeStamp).toString() +
			" " + e.getLevel() + " " + e.getRenderedMessage());
		String[] throwableStrRep = e.getThrowableStrRep();
		if (throwableStrRep != null) {
		    for (String stackTraceLine : throwableStrRep) {
		        email.appendToEmailBody("\n");
		        email.appendToEmailBody(stackTraceLine);
		    }
		}
		if (e.getLevel().equals(Level.WARN) && status.equals(GREEN_STATUS)) {
			status = YELLOW_STATUS;
		} else if (e.getLevel().equals(Level.ERROR)) {
			status = RED_STATUS;
		}
	}
	
	/**
	 * Internal method that does the work to send the email
	 *  to the appropriate users according to the status.
	 */
	private void sendEmail() throws MessagingException {
		if (status.equals(GREEN_STATUS)) {
			email.setEmailSubject(emailSubject + " Success!");
			email.addRecipients(greenRecipients);
		} else if (status.equals(YELLOW_STATUS)) {
			email.setEmailSubject(emailSubject + " Warning!");
			email.addRecipients(greenRecipients);
			email.addRecipients(yellowRecipients);
		} else {
			email.setEmailSubject(emailSubject + " Failed!");
			email.addRecipients(greenRecipients);
			email.addRecipients(yellowRecipients);
			email.addRecipients(redRecipients);
		}
		email.sendMessage();
	}
	
	/**
	 * Call this method to send the email when finished.
	 */
	public void close() {
		try {
			sendEmail();
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean requiresLayout() {
		return false;
	}

	/**
	 * Just a demonstration of the email appender. Logs a message and an exception, and
	 * prints the resulting email to System.out. Doesn't actually send email--this
	 * only happens when you close the appender.
	 */
	public static void main(String[] args) {
        Logger logger = Logger.getLogger("test");
        List<EmailRecipient> gRecipients = new ArrayList<EmailRecipient>();
        gRecipients.add(new EmailRecipient("Jeve Stobs", "steve@apple.com"));
        EmailAppender emailAppender = new EmailAppender(
                new Email("mail.mail.mail"), "test "+new Date(),
                Collections.EMPTY_LIST, Collections.EMPTY_LIST,
                gRecipients);
        logger.addAppender(emailAppender);
        logger.info("Test message");
        logger.info("test message with exception", new Exception());
        System.out.println(emailAppender.email);
    }
}
