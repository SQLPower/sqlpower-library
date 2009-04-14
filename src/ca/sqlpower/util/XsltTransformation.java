/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.util;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * This class transforms xml content from the InputStream passed, into the
 * format specified by the xslt stylesheet and sends the results to an
 * OutputStream.
 */
public class XsltTransformation {
    
    private final String xsltStylesheetPath;

    public XsltTransformation(String xsltStylesheetPath) {
    	this.xsltStylesheetPath = xsltStylesheetPath;
    }
    
    /**
     * Performs an XSLT transformation, sending the results
     * to the OutputStream result.
     */
    public void transform(InputStream xml, OutputStream result) throws Exception {

        InputStream xsltStylesheet = getClass().getResourceAsStream(xsltStylesheetPath);
        Source xmlSource = new StreamSource(xml);
        Source xsltSource = new StreamSource(xsltStylesheet);
        
        TransformerFactory transFact =
                TransformerFactory.newInstance();
        Transformer trans = transFact.newTransformer(xsltSource);

        trans.transform(xmlSource, new StreamResult(result));
        result.flush();
        result.close();
        xsltStylesheet.close();
        xml.close();
  
    }

}
