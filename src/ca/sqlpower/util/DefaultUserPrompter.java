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


/**
 * A simple implementation of UserPrompter that is not a user prompter at all.
 * Always returns the default value given to the prompter. This is normally used
 * as a default for when we run headless.
 */
public class DefaultUserPrompter implements UserPrompter {

	private Object defaultResponse;
	private final UserPromptOptions defaultOptionType;
	private final UserPromptResponse defaultResponseType;
	
	public DefaultUserPrompter(UserPromptOptions defaultOptionType, UserPromptResponse defaultResponseType, Object defaultResponse) {
		this.defaultOptionType = defaultOptionType;
		this.defaultResponseType = defaultResponseType;
		this.defaultResponse = defaultResponse;
	}

	public Object getUserSelectedResponse() {
		return defaultResponse;
	}

	public UserPromptResponse promptUser(Object... formatArgs) {
		return defaultResponseType;
	}

}
