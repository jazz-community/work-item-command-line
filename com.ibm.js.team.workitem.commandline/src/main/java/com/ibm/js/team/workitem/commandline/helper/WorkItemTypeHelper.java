/*******************************************************************************
 * Copyright (c) 2015-2018 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.helper;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IFetchResult;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IAttributeHandle;
import com.ibm.team.workitem.common.model.IWorkItemType;

/**
 * Small helper to assist with work item types
 * 
 */
public class WorkItemTypeHelper {
	IProgressMonitor monitor = null;
	IProjectArea fArea = null;
	ITeamRepository fTeamRepository = null;

	/**
	 * @param projectArea
	 * @param monitor
	 */
	public WorkItemTypeHelper(IProjectArea projectArea, IProgressMonitor monitor) {
		super();
		this.monitor = monitor;
		this.fArea = projectArea;
		this.fTeamRepository = (ITeamRepository) projectArea.getOrigin();
	}

	/**
	 * @return
	 */
	private IWorkItemClient getWorkItemCommon() {
		ITeamRepository repo = getTeamRepository();
		IWorkItemClient workItemClient = (IWorkItemClient) repo.getClientLibrary(IWorkItemClient.class);
		return workItemClient;
	}

	/**
	 * We need this client library to get the work item types
	 * 
	 * @return
	 */
	private IWorkItemClient getWorkItemClient() {
		return (IWorkItemClient) getTeamRepository().getClientLibrary(IWorkItemClient.class);
	}

	/**
	 * @return
	 */
	private ITeamRepository getTeamRepository() {
		return fTeamRepository;
	}

	/**
	 * Prints the work item types available for the project area.
	 * 
	 * @param projectArea
	 * @param monitor
	 * @return
	 * @throws TeamRepositoryException
	 */
	public OperationResult printWorkItemTypes(IProjectArea projectArea, IProgressMonitor monitor)
			throws TeamRepositoryException {

		OperationResult result = new OperationResult();
		List<IWorkItemType> workItemTypes = getWorkItemClient().findWorkItemTypes(projectArea, monitor);
		for (IWorkItemType workItemType : workItemTypes) {
			result.appendResultString("Work Item type: " + workItemType.getDisplayName());
			result.appendResultString("Type ID: " + workItemType.getIdentifier());
			result.appendResultString("Type Category: " + workItemType.getCategory());
			result.appendResultString("");
		}
		result.setSuccess();
		return result;
	}

	/**
	 * Prints the built in and the custom attributes of this work item type.
	 * 
	 * @param projectArea
	 * @param workItemTypeID
	 * @param monitor
	 * @return
	 * @throws TeamRepositoryException
	 */
	public OperationResult printAttributesOfType(IProjectArea projectArea, IWorkItemType workItemType,
			IProgressMonitor monitor) throws TeamRepositoryException {
		OperationResult result = new OperationResult();

		result.appendResultString("Attributes of Work Item type: " + workItemType.getDisplayName() + " Type ID: "
				+ workItemType.getIdentifier());

		result.appendResultString("  Built In Attributes");
		result.appendResultString(
				printAttributesAndTypes(getBuiltInAttributesOfType(projectArea, workItemType, monitor), monitor));

		result.appendResultString("  Custom Attributes");
		result.appendResultString(
				printAttributesAndTypes(getCustomAttributesOfType(projectArea, workItemType, monitor), monitor));

		result.setSuccess();
		return result;
	}

	/**
	 * Prints the built in and the custom attributes of this work item type.
	 * 
	 * @param projectArea
	 * @param workItemTypeID
	 * @param monitor
	 * @return
	 * @throws TeamRepositoryException
	 */
	public List<?> getBuiltInAttributesOfType(IProjectArea projectArea, IWorkItemType workItemType,
			IProgressMonitor monitor) throws TeamRepositoryException {
		List<IAttributeHandle> builtInAttributeHandles = getWorkItemCommon().findBuiltInAttributes(projectArea,
				monitor);
		IFetchResult builtIn = fTeamRepository.itemManager().fetchCompleteItemsPermissionAware(builtInAttributeHandles,
				IItemManager.REFRESH, monitor);
		return builtIn.getRetrievedItems();
	}

	/**
	 * Prints the built in and the custom attributes of this work item type.
	 * 
	 * @param projectArea
	 * @param workItemTypeID
	 * @param monitor
	 * @return
	 * @throws TeamRepositoryException
	 */
	public List<?> getCustomAttributesOfType(IProjectArea projectArea, IWorkItemType workItemType,
			IProgressMonitor monitor) throws TeamRepositoryException {
		List<IAttributeHandle> custAttributeHandles = workItemType.getCustomAttributes();

		IFetchResult custom = fTeamRepository.itemManager().fetchCompleteItemsPermissionAware(custAttributeHandles,
				IItemManager.REFRESH, monitor);
		return custom.getRetrievedItems();
	}

	/**
	 * Print the attributes and their type information from IAttributeTypes from a
	 * list of attributes.
	 * 
	 * @param items
	 * @param monitor
	 */
	private String printAttributesAndTypes(List<?> items, IProgressMonitor monitor) {
		String message = "";
		message = message + "\tNumber of attributes: " + new Integer(items.size()).toString() + "\n";
		for (@SuppressWarnings("rawtypes")
		Iterator iterator = items.iterator(); iterator.hasNext();) {
			Object object = iterator.next();
			if (object instanceof IAttribute) {
				IAttribute iAttribute = (IAttribute) object;
				message = message + "\t " + iAttribute.getDisplayName() + " \tID: " + iAttribute.getIdentifier()
						+ " \tValueType: " + iAttribute.getAttributeType() + "\n";
			}
		}
		return message;
	}

	/**
	 * Find a work item type by its ID provided as string. This is public static and
	 * can be used without initializing the helper. Only search by ID is supported.
	 * 
	 * @param workItemTypeID    - the ID to find
	 * @param projectAreaHandle - the project area to look into
	 * @param workitemCommon    - the IWorkItemCommon client library
	 * @param monitor           - a progress monitor or null
	 * @return the work item type
	 * @throws TeamRepositoryException
	 */
	public static IWorkItemType findWorkItemTypeByIDAndDisplayName(String workItemTypeDisplayName,
			IProjectAreaHandle projectAreaHandle, IWorkItemCommon workitemCommon, IProgressMonitor monitor)
			throws TeamRepositoryException {

		IWorkItemType workItemType = findWorkItemType2(workItemTypeDisplayName, projectAreaHandle, workitemCommon,
				monitor);
		if (workItemType != null) {
			return workItemType;
		}
		List<IWorkItemType> allTypes = workitemCommon.findWorkItemTypes(projectAreaHandle, monitor);
		for (IWorkItemType aWorkItemType : allTypes) {
			if (aWorkItemType.getDisplayName().equals(workItemTypeDisplayName)) {
				return aWorkItemType;
			}
		}
		return null;
	}

	/**
	 * Find a work item type by its ID provided as string. This is public static and
	 * can be used without initializing the helper. Only search by ID is supported.
	 * 
	 * @param workItemTypeID    - the ID to find
	 * @param projectAreaHandle - the project area to look into
	 * @param workitemCommon    - the IWorkItemCommon client library
	 * @param monitor           - a progress monitor or null
	 * @return the work item type
	 * @throws TeamRepositoryException
	 */
	public static IWorkItemType findWorkItemType2(String workItemTypeID, IProjectAreaHandle projectAreaHandle,
			IWorkItemCommon workitemCommon, IProgressMonitor monitor) throws TeamRepositoryException {

		IWorkItemType workItemType = workitemCommon.findWorkItemType(projectAreaHandle, workItemTypeID, monitor);
		if (workItemType != null) {
			return workItemType;
		}
		return null;
	}

	/**
	 * Find a work item type by its ID provided as string. This is public static and
	 * can be used without initializing the helper. Only search by ID is supported.
	 * 
	 * @param workItemTypeID    - the ID to find
	 * @param projectAreaHandle - the project area to look into
	 * @param workitemCommon    - the IWorkItemCommon client library
	 * @param monitor           - a progress monitor or null
	 * @return the work item type
	 * @throws TeamRepositoryException
	 */
	public static IWorkItemType findWorkItemType(String workItemTypeID, IProjectAreaHandle projectAreaHandle,
			IWorkItemCommon workitemCommon, IProgressMonitor monitor) throws TeamRepositoryException {

		IWorkItemType workItemType = WorkItemTypeHelper.findWorkItemType2(workItemTypeID, projectAreaHandle,
				workitemCommon, monitor);
		if (workItemType != null) {
			return workItemType;
		}
		throw new WorkItemCommandLineException("Work item type not found: " + workItemTypeID);
	}
}
