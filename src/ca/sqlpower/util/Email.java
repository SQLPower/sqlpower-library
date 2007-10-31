package ca.sqlpower.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * This class is used as a convenient wrapper for the JavaMail stuff.
 * The API is simpler because a lot of the default boilerplate setup
 * is the same for all messages we send.
 * 
 * @author Dan Fraser
 */
public class Email {

	private String smtpHost;
	private String smtpLocalhost;
	
    /**
     * Creates a new email object with all the default settings.
     */
    public Email(String smtpHost, String smtpLocalhost) {
    	this.smtpHost = smtpHost;
    	this.smtpLocalhost = smtpLocalhost;
    }
    
    /**
     * The Internet addresses of the addressees.
     */
    private List<Address> toAddresses = new ArrayList<Address>();
    
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
    private String emailBody;

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
        props.put("mail.smtp.localhost", smtpLocalhost);
        Session mailSession = Session.getInstance(props, null);
        MimeMessage message = new MimeMessage(mailSession);
        Address[] tempAddresses = new Address[toAddresses.size()];
        try {
            message.setFrom(new InternetAddress(fromEmail, fromName));
            message.addRecipients(Message.RecipientType.TO, toAddresses.toArray(tempAddresses));
        } catch (java.io.UnsupportedEncodingException e) {
            throw new AssertionError(e); // this should never ever happen
        }
        message.setSubject(emailSubject);
        message.setText(emailBody);
        Transport.send(message);
    }

    public String getEmailBody() {
        return emailBody;
    }

    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
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
     * Returns a String representation of all the
     * recipients' email addresses. 
     */
    public String getToAddresses() {
    	StringBuilder result = new StringBuilder();
    	boolean first = true;
    	for (Address addr : toAddresses) {
    		if (!first) {
    			result.append(", ");
    		}
    		result.append("<" + ((InternetAddress) addr).getPersonal() + "> ");
    		result.append(((InternetAddress) addr).getAddress());
    	}
    	return result.toString();
    }
    
    public void setToAddresses(List<Address> toAddresses) {
    	this.toAddresses = toAddresses;
    }
    
    /**
     * Adds a recipient to the email. 
     */
    public void addToAddress(String toEmail, String toName) 
    		throws UnsupportedEncodingException {
    	toAddresses.add(new InternetAddress(toEmail, toName));
    }
    
    public void removeToAddress(String toEmail, String toName) 
    		throws UnsupportedEncodingException {
    	toAddresses.remove(new InternetAddress(toEmail, toName));
    }
    
    /**
     * Returns this email in normal email format (headers, then a blank line then the body).
     */
    @Override
    public String toString() {
        return
            "From: <"+getFromName()+"> "+getFromEmail()+"\r\n"+
            "To: <"+getToAddresses()+"\r\n"+
            "Subject: "+getEmailSubject()+"\r\n"+
            "\r\n"+
            getEmailBody()+"\r\n";
    }
}
