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
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.team.repository.common.TeamRepositoryException;

/**
 * Abstract Command that can be used as base class to implement new commands.
 * The class provides all the required methods to run in the context. Consumers
 * must implement the missing methods.
 * 
 */
public abstract class AbstractCommand implements IWorkItemCommand {

	// the parametermanager we use
	private ParameterManager params;
	// our progress monitor
	private IProgressMonitor monitor = null;
	// the result of our operation - this replaces System.out
	private OperationResult result = new OperationResult();

	/**
	 * Delegate to add to the operation result
	 * 
	 * @param operationResult
	 *            the result we want to add
	 */
	protected void addOperationResult(OperationResult operationResult) {
		this.result.addOperationResult(operationResult);
	}

	/**
	 * Append a line of text to the result
	 * 
	 * @param value
	 */
	public void appendResultString(String value) {
		this.result.appendResultString(value);
	}

	/**
	 * Set result success to true
	 */
	public void setSuccess() {
		this.result.setSuccess();
	}

	/**
	 * Set result success to failed
	 */
	public void setFailed() {
		this.result.setFailed();
	}

	/**
	 * Get the result
	 * 
	 * @return
	 */
	public OperationResult getResult() {
		return this.result;
	}

	/**
	 * @param parametermanager
	 */
	protected AbstractCommand(ParameterManager parametermanager) {
		this.params = parametermanager;
		initialize();
	}

	/**
	 * @return a progress monitor or null
	 */
	public IProgressMonitor getMonitor() {
		return monitor;
	}

	/**
	 * @return the parametermanager that is used
	 */
	protected ParameterManager getParameterManager() {
		return params;
	}

	/**
	 * Set the parameter manager used
	 * 
	 * @param params
	 */
	public void setParameterManager(ParameterManager params) {
		this.params = params;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.
	 * IWorkItemCommandLineCommand #
	 * initialize(com.ibm.js.team.workitem.commandline.parameter.
	 * ParameterManager )
	 */
	public void initialize() {
		setRequiredParameters();
	}

	/**
	 * Inheriting classes need to provide the required parameters, if no
	 * additional parameters are required this can be left blank.
	 */
	public abstract void setRequiredParameters();

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.
	 * IWorkItemCommandLineCommand
	 * #execute(com.ibm.team.repository.client.ITeamRepository, boolean,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public OperationResult execute(IProgressMonitor monitor) throws TeamRepositoryException {
		return process();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.
	 * IWorkItemCommandLineCommand #helpUsage()
	 */
	@Override
	public String helpUsage() {
		return getParameterManager().helpUsageRequiredParameters() + " " + helpSpecificUsage();
	}

	public abstract String helpSpecificUsage();

	@Override
	public void validateRequiredParameters() {
		getParameterManager().validateRequiredParameters();
	}

	/**
	 * Must be overwritten by implementing classes.
	 * 
	 * @return true if the operation processed successful
	 * @throws TeamRepositoryException
	 */
	public abstract OperationResult process() throws TeamRepositoryException;

}
