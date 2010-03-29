/*
 * Copyright (c) 2010, SQL Power Group Inc.
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

package ca.sqlpower.enterprise.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

import org.json.JSONException;

import ca.sqlpower.enterprise.TransactionInformation;

/**
 * This interface defines a few methods for controlling
 * revisions of a server workspace. It is implemented
 * by ArchitectClientSideSession and the test class
 * ResourcefulClientSideSession to access information
 * about the current revision, all revisions, and to
 * revert the current workspace to another revision.
 */
public interface RevisionController {       

    /**
     * Reverts the server workspace to the given revision number.
     * 
     * @param revisionNo
     * @return The current server revision number after reverting
     * (it will be one more than the number before reverting)    
     * @throws IOException
     * @throws URISyntaxException
     * @throws JSONException
     */
    public int revertServerWorkspace(int revisionNo) throws IOException, URISyntaxException, JSONException;
    
    /**
     * Requests the server for the transaction list. 
     * 
     * @return A list of TransactionInformation containing all the information about revisions of this project.
     * @throws IOException
     * @throws URISyntaxException
     * @throws JSONException
     * @throws ParseException
     */
    public List<TransactionInformation> getTransactionList(long fromRevision, long toRevision)
    throws IOException, URISyntaxException, JSONException, ParseException;    
    
    public int getCurrentRevisionNumber();

}
