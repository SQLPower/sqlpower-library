/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ca.sqlpower.util;

import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
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

}
