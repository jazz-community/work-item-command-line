/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.utils;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.team.repository.common.IAuditableHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.ItemProfile;

/**
 * Small helper to assist with work items
 * 
 */
public class WorkItemUtil {

	/**
	 * Find a work item by its ID provided as string.
	 * 
	 * @param id
	 *            - the work item ID as string
	 * @param profile
	 *            - the load profile to use
	 * @param workitemCommon
	 *            - the IWorkItemCommon client library
	 * @param monitor
	 *            - a progress monitor or null
	 * @return
	 * @throws TeamRepositoryException
	 */
	public static IWorkItem findWorkItemByID(String id, ItemProfile<IWorkItem> profile, IWorkItemCommon workitemCommon,
			IProgressMonitor monitor) throws TeamRepositoryException {
		Integer idVal=null;
		try {
			idVal = Integer.valueOf(id);
		} catch (NumberFormatException e) {
			throw new WorkItemCommandLineException(" WorkItem ID: Number format exception, ID is not a number: " + id);
		}
		return workitemCommon.findWorkItemById(idVal.intValue(), profile, monitor);
	}

	/**
	 * Resolve a WorkItem from a handle
	 * 
	 * @param handle
	 * @param profile
	 * @param wiCommon
	 * @param monitor
	 * @return
	 * @throws TeamRepositoryException
	 */
	public static IWorkItem resolveWorkItem(IAuditableHandle handle, ItemProfile<IWorkItem> profile,
			IWorkItemCommon wiCommon, IProgressMonitor monitor) throws TeamRepositoryException {
		return (IWorkItem) wiCommon.getAuditableCommon().resolveAuditable(handle, profile, monitor);
	}
}
