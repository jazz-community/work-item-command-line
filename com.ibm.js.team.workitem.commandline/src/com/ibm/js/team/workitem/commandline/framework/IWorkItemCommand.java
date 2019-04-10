/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.framework;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.team.repository.common.TeamRepositoryException;

/**
 * Interface to be implemented by all commands to be added to the existing list
 * of commands.
 * 
 */
public interface IWorkItemCommand {

	/**
	 * @return the name of the command that is supported by this class
	 */
	public String getCommandName();

	/**
	 * To initialize a parameter manager.
	 * 
	 * @param params
	 */
	public void initialize();

	/**
	 * Execute an operation. Passes the required interfaces and values.
	 * 
	 * @param monitor a progress monitor, may be null.
	 * @return
	 * @throws TeamRepositoryException
	 */
	public OperationResult execute(IProgressMonitor monitor) throws TeamRepositoryException;

	/**
	 * Used to print user help on the command.
	 * 
	 * @return a string explaining how the command is used.
	 */
	public String helpUsage();

	/**
	 * Validate the required parameters are supplied
	 */
	public void validateRequiredParameters();
}
