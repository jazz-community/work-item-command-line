/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.commands;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.AbstractWorkItemModificationCommand;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.helper.WorkItemTypeHelper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.js.team.workitem.commandline.utils.ProcessAreaUtil;
import com.ibm.js.team.workitem.commandline.utils.WorkItemUtil;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.advice.TeamOperationCanceledException;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemType;

/**
 * Command to create a work item, set the provided values and save it. The
 * operation is governed by the process and might fail if required parameters
 * are missing.
 * 
 */
public class CreateWorkItemCommand extends AbstractWorkItemModificationCommand {

	/**
	 * @param parametermanager
	 */
	public CreateWorkItemCommand(ParameterManager parametermanager) {
		super(parametermanager);
	}

	@Override
	public String getCommandName() {
		return IWorkItemCommandLineConstants.COMMAND_CREATE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.
	 * AbstractWorkItemCommandLineCommand#setRequiredParameters()
	 */
	public void setRequiredParameters() {
		super.setRequiredParameters();
		// Add the parameters required to perform the operation
		// getParameterManager().syntaxCommand()
		getParameterManager()
				.syntaxAddRequiredParameter(
						IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY,
						IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY_EXAMPLE);
		getParameterManager()
				.syntaxAddRequiredParameter(
						IWorkItemCommandLineConstants.PARAMETER_WORKITEM_TYPE_PROPERTY,
						IWorkItemCommandLineConstants.PARAMETER_WORKITEM_TYPE_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddSwitch(
				IWorkItemCommandLineConstants.SWITCH_IGNOREERRORS);
		getParameterManager().syntaxAddSwitch(
				IWorkItemCommandLineConstants.SWITCH_ENABLE_DELETE_ATTACHMENTS);
		getParameterManager().syntaxAddSwitch(
				IWorkItemCommandLineConstants.SWITCH_ENABLE_DELETE_APPROVALS);
		getParameterManager().syntaxAddSwitch(
				IWorkItemCommandLineConstants.SWITCH_ENFORCE_SIZE_LIMITS);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.
	 * AbstractWorkItemCommandLineCommand#process()
	 */
	@Override
	public OperationResult process() throws TeamRepositoryException {
		// Get the parameters such as project area name and Attribute Type and
		// run the operation
		String projectAreaName = getParameterManager()
				.consumeParameter(
						IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY)
				.trim();
		// Find the project area
		IProjectArea projectArea = ProcessAreaUtil.findProjectAreaByFQN(
				projectAreaName, getProcessClientService(), getMonitor());
		if (projectArea == null) {
			throw new WorkItemCommandLineException("Project Area not found: "
					+ projectAreaName);
		}

		String workItemTypeID = getParameterManager().consumeParameter(
				IWorkItemCommandLineConstants.PARAMETER_WORKITEM_TYPE_PROPERTY)
				.trim();
		// Find the work item type
		IWorkItemType workItemType = WorkItemTypeHelper.findWorkItemType(
				workItemTypeID, projectArea.getProjectArea(),
				getWorkItemCommon(), getMonitor());
		// Create the work item
		createWorkItem(workItemType);
		return this.getResult();
	}

	/**
	 * Create the work item and set the required attribute values.
	 * 
	 * @param workItemType
	 * @return
	 * @throws TeamRepositoryException
	 */
	private boolean createWorkItem(IWorkItemType workItemType)
			throws TeamRepositoryException {

		ModifyWorkItem operation = new ModifyWorkItem("Creating Work Item");
		this.setIgnoreErrors(getParameterManager().hasSwitch(
				IWorkItemCommandLineConstants.SWITCH_IGNOREERRORS));
		this.setSuppressMailNotification(getParameterManager().hasSwitch(
				IWorkItemCommandLineConstants.SWITCH_SUPPRESS_MAIL_NOTIFICATION));
		IWorkItemHandle handle;
		try {
			handle = operation.run(workItemType, getMonitor());
		} catch (TeamOperationCanceledException e) {
			throw new WorkItemCommandLineException("Work item not created. "
					+ e.getMessage(), e);
		}
		if (handle == null) {
			throw new WorkItemCommandLineException(
					"Work item not created, cause unknown.");
		} else {
			IWorkItem workItem = WorkItemUtil.resolveWorkItem(handle,
					IWorkItem.SMALL_PROFILE, getWorkItemCommon(), getMonitor());
			this.appendResultString("Created work item " + workItem.getId()
					+ ".");
			this.setSuccess();
		}
		return true;
	}

	@Override
	public String helpSpecificUsage() {
		return "{parameter[:mode]=value}";
	}
}
