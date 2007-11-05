package ca.sqlpower.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ca.sqlpower.security.EmailNotification.EmailRecipient;

/**
 * This class is used as a convenient wrapper for the JavaMail stuff.
 * The API is simpler because a lot of the default boilerplate setup
 * is the same for all messages we send.
 * 
 * @author Dan Fraser
 */
public class Email {

	private String smtpHost;
    
	/**
	 * The list of recipients for this email.
	 */
    private List<EmailRecipient> recipients = new ArrayList<EmailRecipient>();
    
    /**
     * The human-readable name of the sender.
     */
    private String fromName;
    
    /**
     * The Internet email address of the sender.
     */
    private String fromEmail;
    
    /**
     * The subject line for the email.  Should only contain ASCII characters
     * (no fancy accented characters for you!).
     */
    private String emailSubject;
    
    /**
     * The body of the email.
     */
    private String emailBody = "";

    /**
     * Creates a new email object with all the default settings.
     */
    public Email(String smtpHost) {
    	this.smtpHost = smtpHost;
    }
    
    /**
     * Sends this email using the current settings.  All settings are required,
     * so don't go skimping on setXXX() calls before calling this!
     * 
     * @throws javax.mail.MessagingException if the message could not
     * be sent.  this is an unusual condition for the website and
     * probably means that the mail server is down or something.
     * @throws UnsupportedEncodingException 
     */
    public void sendMessage() throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        Session mailSession = Session.getInstance(props, null);
        MimeMessage message = new MimeMessage(mailSession);

        try {
        	message.setFrom(new InternetAddress(fromEmail, fromName));
        	for (EmailRecipient er : recipients) {
        		message.addRecipient(Message.RecipientType.TO,
        			new InternetAddress(er.getEmail(), er.getName()));
        	}
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e); // this should never ever happen
        }
        
        message.setSubject(emailSubject);
        message.setText(emailBody);
        Transport.send(message);
    }

    public String getEmailBody() {
        return emailBody;
    }

    /**
     * Sets the email's body, defaults to empty string when given null. 
     */
    public void setEmailBody(String emailBody) {
    	if (emailBody == null) {
    		this.emailBody = "";
    	} else {
    		this.emailBody = emailBody;
    	}
    }

    /**
     * Appends the given to the email's body. 
     */
    public void appendToEmailBody(String emailBody) {
    	StringBuilder body = new StringBuilder(this.emailBody);
    	body.append(emailBody);
    	this.emailBody = body.toString();
    }
    
    public String getEmailSubject() {
        return emailSubject;
    }

    /**
     * Sets the email's subject, defaults to empty string when given null. 
     */
    public void setEmailSubject(String emailSubject) {
    	if (emailSubject == null) {
    		this.emailSubject = "";
    	} else {
    		this.emailSubject = emailSubject;
    	}
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    /** 
     * Returns a String representation of all the recipients.
     */
    public String getRecipients() {
    	StringBuilder result = new StringBuilder();
    	for (EmailRecipient er : recipients) {
    		if (result.length() > 0) {
    			result.append(", ");
    		}
    		result.append(er);
    	}
    	return result.toString();
    }
    
    /**
     * Adds a recipient to the email. 
     */
    public void addRecipient(EmailRecipient er) {
    	if (er != null && !recipients.contains(er)) {
    		recipients.add(er);
    	}
    }
    
    /**
     * Clears the list of recipients and replace with the given.
     */
    public void setRecipients(List<EmailRecipient> recipients) {
    	if (recipients == null) {
    		this.recipients.clear();
    	} else {
    		this.recipients = recipients;
    	}
    }
    
    /**
     * Adds the given list of recipients. 
     */
    public void addRecipients(List<EmailRecipient> recipients) {
    	if (recipients != null) {
	    	for (EmailRecipient er : recipients) {
	    		addRecipient(er);
	    	}
    	}
     }
    
    /**
     * Remove the given recipient from the email.
     */
    public void removeRecipient(EmailRecipient er) { 
    	if (er != null) {
    		recipients.remove(er);
    	}
    }
    
    /**
     * Returns this email in normal email format (headers, then a blank line then the body).
     */
    @Override
    public String toString() {
        return
            "From: <"+getFromName()+"> "+getFromEmail()+"\r\n"+
            "To: "+getRecipients()+"\r\n"+
            "Subject: "+getEmailSubject()+"\r\n"+
            "\r\n"+
            getEmailBody()+"\r\n";
    }
}
