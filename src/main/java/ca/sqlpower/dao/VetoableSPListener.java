package ca.sqlpower.dao;

import ca.sqlpower.object.SPListener;

/**
 * This listener can veto the change by throwing {@link SPObjectVetoException} and preventing the change.
 */
public interface VetoableSPListener extends SPListener {

	/**
	 * Determine if the change should be vetoed, thereby preventing the change 
	 * 
	 * @throws SPObjectVetoException
	 */
	void vetoableChange() throws SPObjectVetoException;

}
