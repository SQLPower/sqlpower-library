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

package ca.sqlpower.swingui;

import java.util.List;

import javax.swing.JFrame;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.util.UserPrompter;
import ca.sqlpower.util.UserPrompterFactory;
import ca.sqlpower.util.UserPrompter.UserPromptOptions;
import ca.sqlpower.util.UserPrompter.UserPromptResponse;

public class SwingUIUserPrompterFactory implements UserPrompterFactory {
    
    private JFrame owner;

    public SwingUIUserPrompterFactory(JFrame owner) {
        this.owner = owner;
    }


    public void setParentFrame(JFrame frame) {
        owner = frame;
    }

	public UserPrompter createUserPrompter(String question,
			UserPromptType responseType, UserPromptOptions optionType,
			UserPromptResponse defaultResponseType, Object defaultResponse,
			String... buttonNames) {
		return new ModalDialogUserPrompter(optionType, defaultResponseType, owner, question, buttonNames);
	}

	public UserPrompter createDatabaseUserPrompter(String question,
			List<Class<? extends SPDataSource>> dsTypes,
			UserPromptOptions optionType,
			UserPromptResponse defaultResponseType, Object defaultResponse,
			DataSourceCollection<SPDataSource> dsCollection,
			String... buttonNames) {
		return new DataSourceUserPrompter(question, optionType, defaultResponseType, (SPDataSource) defaultResponse, 
				owner, question, dsCollection, dsTypes, buttonNames);
	}


	public <T> UserPrompter createListUserPrompter(String question,
			List<T> responses, T defaultResponse) {
		return new ModalDialogListUserPrompter<T>(owner, question, responses, defaultResponse);
	}
}
