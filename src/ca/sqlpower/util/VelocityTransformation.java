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

import ca.sqlpower.architect.swingui.ArchitectSwingSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

/**
 *
 * @author Thomas Kellerer
 */
public class VelocityTransformation implements ReportTransformer {
	private static Logger logger = Logger.getLogger(VelocityTransformation.class);

	public void transform(String builtInTemplate, File result, ArchitectSwingSession session) throws Exception {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	/**
	 * Performs an external XSLT transformation, sending the results
	 * to the OutputStream result.
	 *
	 * @param the XSLT that should be run
	 * @param xml The XML that should be transformed
	 * @param result the output stream where the result of the transformation should be written to
	 */
	public void transform(File template, File result, ArchitectSwingSession session) throws Exception {
		Properties props = new Properties();
		props.put("resource.loader", "file");
		props.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
		props.put("file.resource.loader.path", "");
		props.put("runtime.log.logsystem.log4j.logger", logger.getName());
		
		VelocityEngine engine = new VelocityEngine(props);
		
		Template t = engine.getTemplate(template.getAbsolutePath(), "UTF-8");
		VelocityContext context = new VelocityContext();
		context.put("tables", session.getPlayPen().getTables());
		context.put("projectName", session.getName());

		FileOutputStream out = new FileOutputStream(result);
		OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
		try {
			t.merge(context, writer);
		} catch (Exception e) {
			logger.error("Error running Velocity template", e);
			throw e;
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

}
