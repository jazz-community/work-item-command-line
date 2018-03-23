/*******************************************************************************
 * Copyright (c) 2015-2018 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.commands;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.AbstractTeamRepositoryCommand;
import com.ibm.js.team.workitem.commandline.framework.IWorkItemCommand;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.helper.WorkItemTypeHelper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.js.team.workitem.commandline.utils.ProcessAreaUtil;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.TeamRepositoryException;

/**
 * Command to print the work item types available in a project area.
 * 
 */
public class PrintTypesCommand extends AbstractTeamRepositoryCommand
		implements IWorkItemCommand {

	/**
	 * @param parameterManager
	 */
	public PrintTypesCommand(ParameterManager parameterManager) {
		super(parameterManager);
	}

	@Override
	public String getCommandName() {
		return IWorkItemCommandLineConstants.COMMAND_PRINT_TYPES;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.js.team.workitem.commandline.framework.AbstractWorkItemCommand
	 * #setRequiredParameters()
	 */
	public void setRequiredParameters() {
		// Add the parameters required to perform the operation
		super.setRequiredParameters();
		getParameterManager()
				.syntaxAddRequiredParameter(
						IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY,
						IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY_EXAMPLE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see 
	 * com.ibm.js.team.workitem.commandline.framework.AbstractWorkItemCommand
	 * #process()
	 */
	@Override
	public OperationResult process() throws TeamRepositoryException {
		// Get the parameters such as project area name and Attribute Type and
		// run the operation
		String projectAreaName = getParameterManager()
				.consumeParameter(
						IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY)
				.trim();

		IProjectArea projectArea = ProcessAreaUtil.findProjectAreaByFQN(
				projectAreaName, getProcessClientService(), getMonitor());
		if (projectArea == null) {
			throw new WorkItemCommandLineException("Project Area not found: "
					+ projectAreaName);
		}

		this.addOperationResult(printWorkItemTypes(projectArea));
		return getResult();	
	}

	/** 
	 * Print the work item types available in a project area
	 * 
	 * @param projectArea
	 * @return
	 * @throws TeamRepositoryException
	 */
	private OperationResult printWorkItemTypes(IProjectArea projectArea) throws TeamRepositoryException {
		
		WorkItemTypeHelper workItemTypeHelper = new WorkItemTypeHelper(
				projectArea, getMonitor());

		return workItemTypeHelper.printWorkItemTypes(projectArea, getMonitor());
	}

	@Override
	public String helpSpecificUsage() {
		return "";
	}
}