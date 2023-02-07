/*******************************************************************************
 * Copyright (c) 2015-2023 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.commands;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.AbstractWorkItemModificationCommand;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.helper.WorkItemTypeHelper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterList;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.js.team.workitem.commandline.utils.ProcessAreaUtil;
import com.ibm.js.team.workitem.commandline.utils.QueryUtil;
import com.ibm.js.team.workitem.commandline.utils.WorkItemUtil;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.advice.TeamOperationCanceledException;
import com.ibm.team.repository.common.IAuditableHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.query.IQueryDescriptor;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResult;
import com.ibm.team.workitem.common.query.ResultSize;

/**
 * Command to bulk update work items found using a query. The save operation is
 * governed by the process and might fail if required parameters are missing.
 * 
 */
public class BulkUpdateWorkItemsCommand extends AbstractWorkItemModificationCommand {

	// Get the logger. Changed for Log4J2
	private static final Logger logger = LogManager.getLogger();
	
	// To determine if we are in debug mode
	private boolean fDebug;

	private ParameterList fUpdateParameters;

	/**
	 * The constructor
	 * 
	 * @param parametermanager
	 */
	public BulkUpdateWorkItemsCommand(ParameterManager parametermanager) {
		super(parametermanager);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.IWorkItemCommand#
	 * getCommandName()
	 */
	@Override
	public String getCommandName() {
		return IWorkItemCommandLineConstants.COMMAND_BULKUPDATE_WORKITEMS;
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
		getParameterManager().syntaxAddRequiredParameter(
				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddRequiredParameter(IWorkItemCommandLineConstants.PARAMETER_QUERY_NAME,
				IWorkItemCommandLineConstants.PARAMETER_QUERY_NAME_EXAMPLE);
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.SWITCH_IGNOREERRORS);
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.SWITCH_ENABLE_DELETE_ATTACHMENTS);
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.SWITCH_ENABLE_DELETE_APPROVALS);
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.SWITCH_SUPPRESS_MAIL_NOTIFICATION);
	}

	/**
	 * A way to add help to a command. This allows for specific parameters e.g.
	 * not required ones
	 * 
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractCommand#helpSpecificUsage()
	 */
	@Override
	public String helpSpecificUsage() {
		return " [" + IWorkItemCommandLineConstants.PARAMETER_SHARING_TARGETS
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR
				+ IWorkItemCommandLineConstants.PARAMETER_SHARING_TARGETS_EXAMPLE + "]";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.
	 * AbstractWorkItemCommandLineCommand#process()
	 */
	@Override
	public OperationResult process() throws TeamRepositoryException {

		this.setUpdateDebug(true);

		String projectAreaName = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY).trim();
		// Find the project area
		IProjectArea projectArea = ProcessAreaUtil.findProjectAreaByFQN(projectAreaName, getProcessClientService(),
				getMonitor());
		if (projectArea == null) {
			throw new WorkItemCommandLineException("Project Area not found: " + projectAreaName);
		}

		String queryName = getParameterManager().consumeParameter(IWorkItemCommandLineConstants.PARAMETER_QUERY_NAME);
		if (queryName == null) {
			throw new WorkItemCommandLineException("Query name must be provided.");
		}
		String sharingTargetNames = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_SHARING_TARGETS);
		IQueryDescriptor query = getWorlkItemQuery(projectArea, queryName, sharingTargetNames);

		bulkUpdateFromQuery(query);

		return this.getResult();
	}

	/**
	 * Get a work item query to locate the work items
	 * 
	 * @param projectArea
	 * @param queryName
	 * @param sharingTargetNames
	 * @return
	 * @throws TeamRepositoryException
	 */
	private IQueryDescriptor getWorlkItemQuery(IProjectArea projectArea, String queryName, String sharingTargetNames)
			throws TeamRepositoryException {
		// Get the query
		IQueryDescriptor query = null;
		if (sharingTargetNames == null) {
			// If there is no sharing target try to find a personal query
			query = QueryUtil.findPersonalQuery(queryName, projectArea, getTeamRepository().loggedInContributor(),
					getMonitor());
		} else {
			List<IAuditableHandle> sharingTargets = QueryUtil.findSharingTargets(sharingTargetNames,
					getProcessClientService(), getMonitor());
			if (sharingTargets == null) {
				throw new WorkItemCommandLineException(
						"ProcessArea that shares the query not found " + sharingTargetNames);
			}
			query = QueryUtil.findSharedQuery(queryName, projectArea, sharingTargets, getMonitor());

		}
		if (query == null) {
			throw new WorkItemCommandLineException("Query not found " + queryName);
		}
		return query;
	}

	/**
	 * @param query
	 * @throws TeamRepositoryException
	 */
	private void bulkUpdateFromQuery(IQueryDescriptor query) throws TeamRepositoryException {
		try {
			saveUpdateParameter();
			boolean result = true;
			// Query the work items
			IQueryResult<IResult> results = QueryUtil.getUnresolvedQueryResult(query, isOverrideQueryResultSizeLimit());
			ResultSize resultSize = results.getResultSize(getMonitor());
			logger.trace("Query result size: " + resultSize.getTotal());
			while (results.hasNext(null)) {
				IResult queryResult = results.next(null);
				result &= updateWorkItem((IWorkItemHandle) queryResult.getItem());
			}
			if (result) {
				this.setSuccess();
				return;
			}
		} catch (Exception e) {
			throw new WorkItemCommandLineException(e);
		}

		this.setFailed();
	}

	/**
	 * @param item
	 * @return
	 * @throws TeamRepositoryException
	 * @throws WorkItemCommandLineException
	 */
	private boolean updateWorkItem(IWorkItemHandle item) throws TeamRepositoryException, WorkItemCommandLineException {
		// For each work item we create a new parameter manager that is then
		// used in the subsequent call to update or create the work item
		// Set the new parameter manager
		this.setParameterManager(getUpdateParameterManager());
		boolean result = true;
		try {
			IWorkItem workItem = WorkItemUtil.resolveWorkItem(item, IWorkItem.ID_PROFILE, getWorkItemCommon(),
					getMonitor());
			result = updateWorkItem(workItem);
		} catch (WorkItemCommandLineException e) {
			if (isIgnoreErrors()) {
				result = false;
				getResult().appendResultString(e.getMessage());
			} else {
				throw e;
			}
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Run the update work item operation
	 * 
	 * @param workItem
	 * @return
	 * @throws TeamRepositoryException
	 */
	private boolean updateWorkItem(IWorkItem workItem) throws TeamRepositoryException {
		ModifyWorkItem operation = new ModifyWorkItem("Updating work Item", IWorkItem.FULL_PROFILE);
		try {
			this.appendResultString("Updating work item " + workItem.getId() + ".");

			// Do we have to change the type?
			String workItemTypeIDOrName = getParameterManager().consumeParameter(IWorkItem.TYPE_PROPERTY);
			// There is a type provided
			if (workItemTypeIDOrName != null) {
				IWorkItemType newType = WorkItemTypeHelper.findWorkItemTypeByIDAndDisplayName(workItemTypeIDOrName,
						workItem.getProjectArea(), getWorkItemCommon(), getMonitor());
				// If we can't find the type we can't change it
				if (newType == null) {
					// If we have no type we can't create the work item
					throw new WorkItemCommandLineException(
							"Work item type " + workItemTypeIDOrName + " not found in project area. ");
				}
				// For output purposes
				IWorkItemType oldType = WorkItemTypeHelper.findWorkItemType(workItem.getWorkItemType(),
						workItem.getProjectArea(), getWorkItemCommon(), getMonitor());
				ChangeType changeTypeOperation = new ChangeType("Changing work item type", oldType, newType);
				changeTypeOperation.run(workItem, getMonitor());
			}
			operation.run(workItem, getMonitor());
			this.appendResultString("Updated work item " + workItem.getId() + ".");
		} catch (TeamOperationCanceledException e) {
			throw new WorkItemCommandLineException("Work item update cancelled. " + e.getMessage(), e);
		} catch (TeamRepositoryException e) {
			throw new WorkItemCommandLineException("Work item not updated. " + e.getMessage(), e);
		}
		return true;
	}

	/**
	 * From the flag set if we are in debug mode.
	 * 
	 * @param value
	 */
	private void setUpdateDebug(boolean value) {
		fDebug = value;
	}

	/**
	 * @return true if we are in debug mode
	 */
	private boolean isUpdateDebug() {
		return fDebug;
	}

	/**
	 * Create a debug message
	 * 
	 * @param message
	 */
	@SuppressWarnings("unused")
	private void debug(String message) {
		if (isUpdateDebug()) {
			this.appendResultString(message);
		}
	}

	private void saveUpdateParameter() {
		ParameterList updateParameters = getParameterManager().getArguments();
		updateParameters.addSwitch(IWorkItemCommandLineConstants.SWITCH_BULK_OPERATION, "");
		fUpdateParameters = updateParameters;
	}

	/**
	 * @return
	 */
	private ParameterManager getUpdateParameterManager() {
		return new ParameterManager(fUpdateParameters);
	}

	/**
	 * @return
	 */
	private boolean isOverrideQueryResultSizeLimit() {
		// TODO Potential option for the future
		return true;
	}

}
