/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

import java.util.List;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.util.UserPrompter.UserPromptOptions;
import ca.sqlpower.util.UserPrompter.UserPromptResponse;

/**
 * Factory that creates user prompters that always respond with the default
 * given. This is appropriate for headless environments and embedded use, and it
 * is the default factory for the core ArchitectSessionImpl.
 */
public class DefaultUserPrompterFactory implements UserPrompterFactory {

	public UserPrompter createUserPrompter(String question,
			UserPromptType responseType, UserPromptOptions optionType,
			UserPromptResponse defaultResponseType, Object defaultResponse,
			String... buttonNames) {
		return new DefaultUserPrompter(optionType, defaultResponseType, defaultResponse);
	}

	public UserPrompter createDatabaseUserPrompter(String question,
			List<Class<? extends SPDataSource>> dsTypes,
			UserPromptOptions optionType,
			UserPromptResponse defaultResponseType, Object defaultResponse,
			DataSourceCollection<SPDataSource> dsCollection,
			String... buttonNames) {
		return new DefaultUserPrompter(optionType, defaultResponseType, defaultResponse);
	}

	public <T> UserPrompter createListUserPrompter(String question,
			List<T> responses, T defaultResponse) {
		return new DefaultUserPrompter(UserPromptOptions.OK, UserPromptResponse.OK, defaultResponse);
	}
	
}
