package ca.sqlpower.sql;

import java.rmi.Naming;

import java.util.*;
/**
 * Test stub to allow basic testing of the RMI server.
 * 
 * @author Dan Fraser
 */
public class RmiTest {

	public static void main(String[] args) {
		
		
    Collection message = null; 
         
    DBConnectionSpecServer obj = null; 

        try { 
            obj = (DBConnectionSpecServer)Naming.lookup("///DBConnectionSpecServer"); 
            message = obj.getAvailableDatabases();
            Iterator messageIt = message.iterator();
            while (messageIt.hasNext()) {
            	System.out.println(messageIt.next());
            }
        } catch (Exception e) { 
            System.out.println("exception: " + e.getMessage()); 
            e.printStackTrace(); 
        } 
    } 
}

