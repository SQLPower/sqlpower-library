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
/*
 * Created on Jun 13, 2006
 *
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.sqlpower.util.SQLPowerUtils;

/**
 * Implements a "call home, we're broken" functionality - does not report
 * anything secret. The data are posted to a URL on our web site to keep track
 * of errors that people see when running the product.
 * <p>
 * Additional information that we may want to gather later would be:
 * <ul>
 *  <li>security manager info
 *  <li>permission to get project file
 *  <li>undo history (need permission)
 *  <li>JDBC drivers and other crap on classpath (need permission)
 * </ul>
 */
public class ExceptionReport {

    private static final Logger logger = Logger.getLogger(ExceptionReport.class);
    
    /**
     * This is the maximum number of tries to send the report before
     * the send method gives up.
     */
    private static final int MAX_REPORT_TRIES = 10;
    
    /**
     * The number of runs that have occurred so far. If this reaches 
     * the maximum number of reports to try and send the method will
     * stop trying to send reports.
     */
    private static int numReportsThisRun = 0;

    /**
     * The version of the application that this report came from.
     * The format of the string is not important.
     */
    private String applicationVersion;
    
    /**
     * The time since the application launched in milliseconds.
     */
    private long applicationUptime;
    
    /**
     * The name of the application that this report came from.
     */
    private String applicationName;
    
    /**
     * The total memory of the system that is running this application.
     * The value is in bytes and is initialized by the constructor.
     */
    private long totalMem;
    
    /**
     * The amount of free memory of the system at the time this report
     * is created. The value is in bytes and is initialized by the 
     * constructor.
     */
    private long freeMem;
    
    /**
     * The total memory allocated to the JVM that is running this application.
     * The value is in bytes and is initialized by the constructor.
     */
    private long maxMem;
    
    /**
     * The vendor of the JVM that is running this application.
     * The value is initialized by the constructor.
     */
    private String jvmVendor;
    
    /**
     * The version of the JVM that is running this application.
     * The value is initialized by the constructor.
     */
    private String jvmVersion;
    
    /**
     * The operating system architecture that is running this application.
     * The value is initialized by the constructor.
     */
    private String osArch;
    
    /**
     * The name of the operating system that is running this application.
     * The value is initialized by the constructor.
     */
    private String osName;
    
    /**
     * The version of the operating system that is running this application.
     * The value is initialized by the constructor.
     */
    private String osVersion;

    /**
     * Additional information supplied by the application that is given to the
     * report. This will be passed along when the report is sent.
     */
    private String remarks;
    
    /**
     * The exception that we are creating a report on.
     */
    private Throwable exception;
    
    /**
     * The default URL to send the report to if the URL in the system
     * properties does not exist.
     */
    private String reportUrl;
    
    /**
     * This map stores additional information that needs to be sent with
     * the report. The map stores the information in (name, data) pairs.
     */
    private Map<String, String> additionalInfo = new HashMap<String, String>(); 
    
    /**
     * This constructor sets all of the necessary properties to send a basic
     * exception report.
     * 
     * @param exception
     *            The exception that is being reported on.
     * @param reportURL
     *            The URL to send the report to. This field cannot be null.
     * @param applicationVersion
     *            The version of the application that is generating this report.
     *            The format of this string is not important
     * @param applicationUptime
     *            The time in milliseconds that the application has been running
     *            for.
     * @param appName
     *            The name of the application that is creating this report.
     */
    public ExceptionReport(Throwable exception, String reportURL, String applicationVersion, long applicationUptime, String appName) {
        this.exception = exception;
        this.reportUrl = reportURL;
        this.applicationVersion = applicationVersion;
        this.applicationUptime = applicationUptime;
        this.applicationName = appName;
        totalMem = Runtime.getRuntime().totalMemory();
        freeMem = Runtime.getRuntime().freeMemory();
        maxMem = Runtime.getRuntime().maxMemory();
        jvmVendor = System.getProperty("java.vendor");
        jvmVersion = System.getProperty("java.version");
        osArch = System.getProperty("os.arch");
        osName = System.getProperty("os.name");
        osVersion = System.getProperty("os.version");
    }

    /**
     * Returns a string that contains the exceptions, system parameters,
     * and additional information in XML format.
     */
    public String toXML() {
        StringBuffer xml = new StringBuffer();
        xml.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>");
        xml.append("\n<application-exception-report version=\"1.0\">");
        appendNestedExceptions(xml,exception);
        xml.append("\n <application-name>").append(SQLPowerUtils.escapeXML(applicationName)).append("</application-name>");
        xml.append("\n <application-version>").append(SQLPowerUtils.escapeXML(applicationVersion)).append("</application-version>");
        xml.append("\n <application-uptime>").append(applicationUptime).append("</application-uptime>");
        xml.append("\n <total-mem>").append(totalMem).append("</total-mem>");
        xml.append("\n <free-mem>").append(freeMem).append("</free-mem>");
        xml.append("\n <max-mem>").append(maxMem).append("</max-mem>");
        xml.append("\n <jvm vendor=\"").append(SQLPowerUtils.escapeXML(jvmVendor)).append("\" version=\"").append(SQLPowerUtils.escapeXML(jvmVersion)).append("\" />");
        xml.append("\n <os arch=\"").append(SQLPowerUtils.escapeXML(osArch)).append("\" name=\"").append(SQLPowerUtils.escapeXML(osName)).append("\" version=\"").append(SQLPowerUtils.escapeXML(osVersion)).append("\" />");
        for (Map.Entry<String, String> ent : additionalInfo.entrySet()) {
            xml.append("\n <application-specific" +
                    " property=\"" + SQLPowerUtils.escapeXML(ent.getKey()) + "\">")
                    .append(SQLPowerUtils.escapeXML(ent.getValue()))
                    .append("</application-specific>");
        }
        xml.append("\n <remarks>").append(SQLPowerUtils.escapeXML(remarks)).append("</remarks>");
        xml.append("\n</application-exception-report>");
        xml.append("\n");
        return xml.toString();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Exception Report: ");
        sb.append(exception);
        sb.append(" ");
        sb.append(remarks);
        return sb.toString();
    }
    
    /**
     * This appends the exception stack trace to the given string
     * buffer.
     * 
     * @param xml 
     *          The string buffer to append the exceptions to.
     * @param exception 
     *          The exception that was thrown that will have its
     *          stack added to the buffer.
     */
    private void appendNestedExceptions(StringBuffer xml, Throwable exception) {
        if (exception == null) return;
        xml.append("\n <exception class=\"").append(SQLPowerUtils.escapeXML(exception.getClass().getName())).append("\" message=\"")
                        .append(SQLPowerUtils.escapeXML(exception.getMessage())).append("\">");
        for (StackTraceElement ste : exception.getStackTrace()) {
            xml.append("\n  <trace-element class=\"").append(SQLPowerUtils.escapeXML(ste.getClassName()))
                    .append("\" method=\"").append(SQLPowerUtils.escapeXML(ste.getMethodName()))
                    .append("\" file=\"").append(SQLPowerUtils.escapeXML(ste.getFileName()))
                    .append("\" line=\"").append(ste.getLineNumber())
                    .append("\" />");
        }
        appendNestedExceptions(xml,exception.getCause());
        xml.append("\n </exception>");
    }
    
    /**
     * Sends this report, formatted as an XML document, to the URL given in the
     * constructor.  The report is sent using the HTTP POST method.
     */
    public void send() {
        logger.debug("posting report: "+toString());
        if (numReportsThisRun++ > MAX_REPORT_TRIES) {
            logger.info(
                    String.format(
                            "Not logging this error, threshold of %d exceeded", MAX_REPORT_TRIES));
            return;
        }
        exception.printStackTrace();
        logger.info("Posting error report to SQL Power at URL <"+reportUrl+">");
        try {
            HttpURLConnection dest = (HttpURLConnection) new URL(reportUrl).openConnection();
            dest.setConnectTimeout(3000);
            dest.setReadTimeout(3000);
            dest.setDoOutput(true);
            dest.setDoInput(true);
            dest.setUseCaches(false);
            dest.setRequestMethod("POST");
            dest.setRequestProperty("Content-Type", "text/xml");
            dest.connect();
            OutputStream out = null;
            try {
                out = new BufferedOutputStream(dest.getOutputStream());
                out.write(toXML().getBytes("ISO-8859-1"));
                out.flush();
            } finally {
                if (out != null) out.close();
            }


            // Note: the error report will only get sent if we attempt to read from the URL Connection (!??!?)
            InputStreamReader inputStreamReader = new InputStreamReader(dest.getInputStream());
            BufferedReader in = new BufferedReader(inputStreamReader);
            StringBuffer response = new StringBuffer();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            logger.info("Error report servlet response: "+response);
        } catch (Exception e) {
            // Just catch-and-squash everything because we're already in up to our necks at this point.
            logger.error("Couldn't send exception report to <\""+reportUrl+"\">", e);
        }
        logger.debug("Finished posting report");
    }

    /**
     * Inserts a named value and its value to the additional information
     * mapping.
     * 
     * @param name
     *            The name of the information that will be put into the report.
     * @param value
     *            The value of the information that will be put into the report.
     */
    public void addAdditionalInfo(String name, String value) {
        additionalInfo.put(name, value);
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
