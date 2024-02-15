/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/


package com.ibm.js.team.workitem.commandline.commands;

import java.util.ArrayList;
import java.util.List;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.AbstractTeamRepositoryCommand;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.js.team.workitem.commandline.utils.ProcessAreaUtil;
import com.ibm.js.team.workitem.commandline.utils.WorkItemUtil;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.service.IQueryService;
import com.ibm.team.workitem.client.IWorkItemWorkingCopyManager;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.expression.AttributeExpression;
import com.ibm.team.workitem.common.expression.IQueryableAttribute;
import com.ibm.team.workitem.common.expression.IQueryableAttributeFactory;
import com.ibm.team.workitem.common.expression.QueryableAttributes;
import com.ibm.team.workitem.common.expression.Term;
import com.ibm.team.workitem.common.expression.Term.Operator;
import com.ibm.team.workitem.common.model.AttributeOperation;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResult;

public class DeleteWorkItemAttribute extends AbstractTeamRepositoryCommand {

	public static final String COMMAND_DELETE_ATTRIBUTE = "deleteattribute";
	private static final String PARAMETER_DELETE_ATTRIBUTE_ID = "deleteAttributeId";
	
	public DeleteWorkItemAttribute(ParameterManager parametermanager) {
		super(parametermanager);
	}

	@Override
	public String getCommandName() {
		return COMMAND_DELETE_ATTRIBUTE;
	}

	@Override
	public String helpSpecificUsage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OperationResult process() throws TeamRepositoryException {

		fLogger.debug(">>> Delete Work Item Attribute command begin");

		// Get the parameters such as project area name 
		String projectAreaName = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY).trim();
		// Find the project area
		IProjectArea projectArea = ProcessAreaUtil.findProjectAreaByFQN(projectAreaName, getProcessClientService(),
				getMonitor());
		if (projectArea == null) {
			throw new WorkItemCommandLineException("Project Area not found: " + projectAreaName);
		}
		
		// Get the parameter values - The delete attribute
		String deleteAttrId = getParameterManager().consumeParameter(PARAMETER_DELETE_ATTRIBUTE_ID).trim();
		IAttribute deleteAttribute = getWorkItemCommon().findAttribute(projectArea, deleteAttrId, getMonitor());
		
		if (deleteAttribute == null) {
			throw new WorkItemCommandLineException("Delete Attribute not found: " + deleteAttrId);
		}
		
		IQueryableAttributeFactory factory = QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE);
		//Find items in project area.
		IQueryableAttribute projectQueryableAttribute = factory.findAttribute(projectArea, IWorkItem.PROJECT_AREA_PROPERTY, getAuditableCommon(), getMonitor());
		AttributeExpression projectExpression = new AttributeExpression(projectQueryableAttribute, AttributeOperation.EQUALS, projectArea);
		
		//Find work items that have the attribute to delete.
		IQueryableAttribute deleteQueryAttribute = factory.findAttribute(projectArea, deleteAttrId, getAuditableCommon(), getMonitor());
		AttributeExpression deleteAttributeExpression = new AttributeExpression(deleteQueryAttribute, AttributeOperation.EXISTS, deleteAttribute);
		
		Term queryTerm = new Term(Operator.AND);
		queryTerm.add(projectExpression);
		queryTerm.add(deleteAttributeExpression);
		
		IQueryResult<IResult> queryResult = getQueryClient().getExpressionResults(projectArea, queryTerm);
		queryResult.setLimit(Integer.MAX_VALUE);
		queryResult.setPageSize(IQueryService.ITEM_QUERY_MAX_PAGE_SIZE);
		
		IWorkItemWorkingCopyManager workingCopyManager = (IWorkItemWorkingCopyManager) getWorkItemClient().createWorkingCopyManager(DeleteWorkItemAttribute.class.getName(), true);
		List<WorkItemWorkingCopy> itemsToSave = new ArrayList<WorkItemWorkingCopy>();
		
		fLogger.debug("Begin query for all work items in project area: " + projectArea.getName() + " that has attribute id: " + deleteAttrId);
		
		try {
			//Iterate through query results.  Remove the attribute from each item.
			while (queryResult.hasNext(getMonitor())) {
				IWorkItemHandle workItemHandle = (IWorkItemHandle) queryResult.next(getMonitor()).getItem();
				fLogger.debug("Found work item UUID: " + workItemHandle.getItemId());
				IWorkItem workItem = WorkItemUtil.resolveWorkItem(workItemHandle, IWorkItem.FULL_PROFILE, getWorkItemCommon(), getMonitor());
				workingCopyManager.connect(workItem, IWorkItem.FULL_PROFILE, getMonitor());
				WorkItemWorkingCopy wiCopy = workingCopyManager.getWorkingCopy(workItem);
				wiCopy.getWorkItem().removeCustomAttribute(deleteAttribute);
				fLogger.debug("Removed attribute from working copy of work item: " + wiCopy.getWorkItem().getId());
				itemsToSave.add(wiCopy);
			}
		
			fLogger.debug("Committing work item copies back to the repository");
			workingCopyManager.save(itemsToSave.toArray(new WorkItemWorkingCopy[itemsToSave.size()]), getMonitor());
		} finally {
			//Disconnect items
			for (WorkItemWorkingCopy copy : itemsToSave) {
				workingCopyManager.disconnect(copy.getWorkItem());
			}
		}

		fLogger.debug("<<< Delete Work Item Attribute command complete");
		return null;
	}

}
