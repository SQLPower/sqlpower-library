/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

package ca.sqlpower.swingui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.Olap4jDataSource;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.util.UserPrompter;
import ca.sqlpower.util.UserPrompter.UserPromptOptions;
import ca.sqlpower.util.UserPrompter.UserPromptResponse;
import ca.sqlpower.util.UserPrompterFactory.UserPromptType;

/**
 * This class is a class that helps consolidate the code into one class when a user needs to popup
 * a user prompter. This is because both the swing context and the swing session need to popup this
 * prompters. (and the context has no parent whereas the session has a parent)
 *
 */
public class SwingPopupUserPrompter {
	private SwingPopupUserPrompter() {
		//do nothing
	}
	
	public static UserPrompter swingPopupUserPrompter(String question,
			UserPromptType responseType, UserPromptOptions optionType,
			UserPromptResponse defaultResponseType, Object defaultResponse, 
			JFrame owner, DataSourceCollection<SPDataSource> dsCollection,
			String... buttonNames) {
		
		List<Class<? extends SPDataSource>> dsTypes = new ArrayList<Class<? extends SPDataSource>>();
		switch (responseType) {
            case BOOLEAN :
                return new ModalDialogUserPrompter(optionType, defaultResponseType, owner, question, buttonNames);
            case JDBC_DATA_SOURCE:
                dsTypes.add(JDBCDataSource.class);
                return new DataSourceUserPrompter(question, responseType, optionType, defaultResponseType, (SPDataSource) defaultResponse, 
                        owner, question, dsCollection, dsTypes, buttonNames);
            case OLAP_DATA_SOURCE:
                dsTypes.add(Olap4jDataSource.class);
                return new DataSourceUserPrompter(question, responseType, optionType, defaultResponseType, (SPDataSource) defaultResponse, 
                        owner, question, dsCollection, dsTypes, buttonNames);
            case SP_DATA_SOURCE:
                dsTypes.add(JDBCDataSource.class);
                dsTypes.add(Olap4jDataSource.class);
                return new DataSourceUserPrompter(question, responseType, optionType, defaultResponseType, (SPDataSource) defaultResponse, 
                        owner, question, dsCollection, dsTypes, buttonNames);
            default :
                throw new UnsupportedOperationException("User prompt type " + responseType + " is unknown.");
        }
	}
}
