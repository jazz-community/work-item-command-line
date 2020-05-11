/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.commands;

import java.util.Set;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.AbstractTeamRepositoryCommand;
import com.ibm.js.team.workitem.commandline.framework.IWorkItemCommand;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.helper.WorkItemTypeHelper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterLinkIDMapper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.js.team.workitem.commandline.utils.ProcessAreaUtil;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.model.IWorkItemType;

/**
 * Command to print the attributes of a specific attribute type. I don't need a
 * work item operation here, just login to the repository
 * 
 */
public class PrintTypeAttributesCommand extends AbstractTeamRepositoryCommand implements IWorkItemCommand {

	/**
	 * @param parameterManager
	 */
	public PrintTypeAttributesCommand(ParameterManager parameterManager) {
		super(parameterManager);
	}

	@Override
	public String getCommandName() {
		return IWorkItemCommandLineConstants.COMMAND_PRINT_TYPE_ATTRIBUTES;
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
		getParameterManager().syntaxAddRequiredParameter(
				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddRequiredParameter(IWorkItemCommandLineConstants.PARAMETER_WORKITEM_TYPE_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_WORKITEM_TYPE_PROPERTY_EXAMPLE);
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
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY).trim();

		IProjectArea projectArea = ProcessAreaUtil.findProjectAreaByFQN(projectAreaName, getProcessClientService(),
				getMonitor());
		if (projectArea == null) {
			throw new WorkItemCommandLineException("Project Area not found: " + projectAreaName);
		}

		String workItemTypeID = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_WORKITEM_TYPE_PROPERTY).trim();
		IWorkItemType workItemType = getWorkItemCommon().findWorkItemType(projectArea, workItemTypeID, getMonitor());
		if (workItemType == null) {
			throw new WorkItemCommandLineException("Work item type not found: " + workItemTypeID);
		}
		this.addOperationResult(printTypeAttributes(projectArea, workItemType));
		return getResult();
	}

	/**
	 * Print the attributes associated to a work item type and display their
	 * detailed information
	 * 
	 * @param projectArea
	 * @param workItemType
	 * @return
	 * @throws TeamRepositoryException
	 */
	private OperationResult printTypeAttributes(IProjectArea projectArea, IWorkItemType workItemType)
			throws TeamRepositoryException {

		WorkItemTypeHelper workItemTypeHelper = new WorkItemTypeHelper(projectArea, getMonitor());

		OperationResult result = workItemTypeHelper.printAttributesOfType(projectArea, workItemType, getMonitor());

		result.appendResultString("  Links (supported)");
		Set<String> linkNames = ParameterLinkIDMapper.getLinkNames();
		for (String linkName : linkNames) {
			String linkID = ParameterLinkIDMapper.getinternalID(linkName);
			result.appendResultString("\t " + linkName + " \tID: " + linkID);
		}

		return result;
	}

	@Override
	public String helpSpecificUsage() {
		return "";
	}

}