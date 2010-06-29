/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.util.UserPrompter.UserPromptOptions;
import ca.sqlpower.util.UserPrompter.UserPromptResponse;

public interface UserPrompterFactory {
	
	/**
	 * This describes the type of prompter created by the factory.
	 * The type will tell what kind of response is given by the prompter. 
	 */
    public enum UserPromptType {
    	
    	/**
    	 * Boolean prompt types can have the OK, NOT_OK and CANCEL options
    	 * and is used to ask simple questions of the user. New booleans
    	 * are not allowed.
    	 */
    	BOOLEAN(Boolean.class),
    	
    	/**
    	 * File prompt types can have OK, NEW, NOT_OK, and CANCEL options.
    	 * The file prompts can allow the selection of existing files or create
    	 * a new file 
    	 */
    	FILE(File.class),
    	
    	/**
    	 * Just an informative message that does not solicit any additional
    	 * information from the user. Supports only the OK option type.
    	 */
    	MESSAGE(Void.class),
    	
    	/**
    	 * Text prompt type that supports the OK and CANCEL options. This is used for
    	 * string manipulation and/or replacement.
    	 */
    	TEXT(String.class);
    	
    	private final Class<? extends Object> clazz;

		private UserPromptType(Class<? extends Object> clazz) {
			this.clazz = clazz;
    	}
		
		public Class<? extends Object> getPromptTypeClass() {
			return clazz;
		}
    }

	/**
	 * Creates a new user prompter instance with the given settings. User
	 * prompter instances can be stateful (for example, an "always accept"
	 * button will cause that prompter to continue returning "OK" forever), so
	 * it is important to obtain a new user prompter from this factory for every
	 * overall operation.
	 * 
	 * @param question
	 *            The question the new prompter will pose when solociting a
	 *            response from the user. This question string is not exactly
	 *            plain text: it is formatted according to to rules laid out in
	 *            the {@link MessageFormat} class. The most important
	 *            implications are that the single quote (') character and the
	 *            open curly brace ({) characters are special and have to be
	 *            escaped in order to appear in the message. The other important
	 *            thing (the benefit, that is) is that constructions of the form
	 *            {0} are placeholders that will be substituted every time the
	 *            question is asked via the
	 *            {@link UserPrompter#promptUser(Object[])} method is called.
	 *            See {@link MessageFormat} for details.
	 *            <br>
	 *            Also, UserPrompter implementations will ensure that newline
	 *            characters (\n) show up as new lines when the question is
	 *            presented to the user.<p>
	 * @param responseType
	 *            The object type that will be returned by the prompter.<p>
	 * @param optionType
	 * 			  The combination of responses to be used to prompt the user<p>          
	 * @param defaultResponse
	 *            The response object to use as a default. The default will be
	 *            used to specify the default selection for user interactive
	 *            prompters or it will be the returned value if the selection is
	 *            automatic.<p>
	 * @param buttonNames
	 * 			  An array of strings of an indefinite size which will be used 
	 * 			  as labels for each of the responses. The strings should be stored 
	 * 			  in the array in the order indicated by the optionType. If the number
	 * 			  of strings in the array does not match the number of responses required 
	 * 			  for each optionType, an IllegalStateException will be thrown.
	 * 			  The following are currently the types of strings used:<br>
	 * 		      1)okText<br>
	 *            The text to associate with the OK response. Try to use a word
	 *            or phrase from the question instead of a generic word like
	 *            "OK" or "Yes".<br>
	 *            2)newText<br>
	 *            The text to associate with the NEW response. Try to use a word
	 *            or phrase from the question instead of a generic word like
	 *            "NEW" or "Yes". This action should create an appropriate new
	 *            object.<br>
	 *            3)notOkText<br>
	 *            The text to associate with the "not OK" response. Try to use a
	 *            word or phrase from the question instead of a generic word
	 *            like "No".<br>
	 *            4)cancelText<br>
	 *            The text to associate with response that cancels the whole
	 *            operation.<br>
	 */
    public UserPrompter createUserPrompter(String question, UserPromptType responseType, UserPromptOptions optionType,
			UserPromptResponse defaultResponseType, Object defaultResponse, 
			String... buttonNames);

	/**
	 * This method is almost identical to createUserPrompter but instead it
	 * creates a database user prompter. This is in a separate method because it
	 * takes a collection of datasources as one of its arguments
	 */
    public UserPrompter createDatabaseUserPrompter(String question, List<Class<? extends SPDataSource>> dsTypes,
    		UserPromptOptions optionType, UserPromptResponse defaultResponseType, Object defaultResponse,
    		DataSourceCollection<SPDataSource> dsCollection, String ... buttonNames);
    
    public <T> UserPrompter createListUserPrompter(String question,
			List<T> responses, T defaultResponse);
}
