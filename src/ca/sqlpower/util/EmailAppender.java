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

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import ca.sqlpower.security.EmailNotification.EmailRecipient;

public class EmailAppender extends AppenderSkeleton {

	private final Email email;
	private final List<EmailRecipient> greenRecipients;
	private final List<EmailRecipient> yellowRecipients;
	private final List<EmailRecipient> redRecipients;
	
	public EmailAppender(Email email, List<EmailRecipient> greenRecipients,
			List<EmailRecipient> yellowRecipients, List<EmailRecipient> redRecipients) {
		this.email = email;
		this.greenRecipients = greenRecipients;
		this.yellowRecipients = yellowRecipients;
		this.redRecipients = redRecipients;
	}
	
	@Override
	protected void append(LoggingEvent e) {
		if (email.getEmailBody().length() > 0) {
			email.appendToEmailBody("\n");
		}
		email.appendToEmailBody(e.getRenderedMessage());
	}
	
	public void sendGreenEmail() throws MessagingException, UnsupportedEncodingException {
		if (greenRecipients.size() > 0) {
			email.setEmailSubject("Success!");
			for (EmailRecipient recipient: greenRecipients) {
				email.addToAddress(recipient.getEmail(), recipient.getName());
			}
			email.sendMessage();
		}
	}
	
	public void sendYellowEmail() throws MessagingException, UnsupportedEncodingException {
		if (yellowRecipients.size() > 0) {
			email.setEmailSubject("Warning!");
			for (EmailRecipient recipient: yellowRecipients) {
				email.addToAddress(recipient.getEmail(), recipient.getName());
			}
			email.sendMessage();
		}
	}
	
	public void sendRedEmail() throws MessagingException, UnsupportedEncodingException {
		if (redRecipients.size() > 0) {
			email.setEmailSubject("Error!");
			for (EmailRecipient recipient: redRecipients) {
				email.addToAddress(recipient.getEmail(), recipient.getName());
			}
			email.sendMessage();
		}
	}
	
	public void close() {
		// do nothing!
	}

	public boolean requiresLayout() {
		return false;
	}

}
