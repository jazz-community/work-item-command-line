/*******************************************************************************
 * Copyright (c) 2019-2022 IBM
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.commands;

import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.AbstractTeamRepositoryCommand;
import com.ibm.js.team.workitem.commandline.framework.IWorkItemCommand;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.helper.UserAllocationHelper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterList;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.js.team.workitem.commandline.utils.ProcessAreaUtil;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.transport.client.ITeamRawRestServiceClient;

/**
 * This command checks the user allocation of a project area.
 * ... Unfinished ... Needs testing
 * 
 */
public class ValidateUserAllocationsCommand extends AbstractTeamRepositoryCommand implements IWorkItemCommand {

	private Logger logger = LogManager.getLogger(ValidateUserAllocationsCommand.class);
	// Parameter to specify the query
	private static final String SWITCH_TRACE = "trace";
	private static final String SWITCH_DEBUG = "debug";

	private IProjectArea projectArea;
	private HashMap<String, ITeamRawRestServiceClient> repoClients = new HashMap<String, ITeamRawRestServiceClient>();

	/**
	 * @param parameterManager
	 */
	public ValidateUserAllocationsCommand(ParameterManager parameterManager) {
		super(parameterManager);
	}

	@Override
	public String getCommandName() {
		return IWorkItemCommandLineConstants.COMMAND_VALIDATE_USER_ALLOCATIONS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractWorkItemCommand
	 * #setRequiredParameters()
	 */
	public void setRequiredParameters() {
		// Add the parameters required to perform the operation
		super.setRequiredParameters();
		getParameterManager().syntaxAddRequiredParameter(
				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddSwitch(SWITCH_TRACE);
		getParameterManager().syntaxAddSwitch(SWITCH_DEBUG);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractWorkItemCommand
	 * #process()
	 */
	@Override
	public OperationResult process() throws TeamRepositoryException {

		// Get the parameters such as project area name and
		// run the operation
		String projectAreaName = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY).trim();

		projectArea = ProcessAreaUtil.findProjectAreaByFQN(projectAreaName, getProcessClientService(), getMonitor());
		if (projectArea == null) {
			throw new WorkItemCommandLineException("Project Area not found: " + projectAreaName);
		}
		ParameterList arguments = getParameterManager().getArguments();

		UserAllocationHelper userAllocationHelper= new UserAllocationHelper(projectArea, getTeamRepository(), arguments, getMonitor(), logger);
		IContributorHandle[] users= projectArea.getMembers();
		String allocationResults= ""; 
		String allocationResult= ""; 
		logger.debug("Checking allocation for " + users.length + " users in project area " + projectArea.getName() + "\n");
		for (int i=0; i<users.length;i++) {
			try {
				allocationResult= userAllocationHelper.repairAllocations(users[i]);
				if(allocationResult != null) {					 // do not return normal success
					allocationResults+= allocationResult + "\n";
				}
				logger.debug( users[i] + ": " + allocationResult); // Note: do not log PI
			} catch (Exception e) {
				System.err.println(e.getMessage()); // logger?
				e.printStackTrace();
			}
		}
		getResult().appendResultString("Allocations checked for" + users.length + " users in project area " + projectArea.getName() + allocationResults);
		setSuccess();
		return getResult();
	}

	@Override
	public String helpSpecificUsage() {
		return "";
	}
}