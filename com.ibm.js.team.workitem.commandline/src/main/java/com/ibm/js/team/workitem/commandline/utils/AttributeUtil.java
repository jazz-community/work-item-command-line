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
import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IAttribute;

/**
 * Utility class to collect attribute related API
 * 
 */
public class AttributeUtil {

	/**
	 * Find the attribute type (IAttribute) from the process
	 * 
	 * @param attributeID - the ID of the attribute we are working at
	 * @param projectArea - must not be null
	 * @param monitor     - an IProgressMonitor or null
	 * @return the IAttribute found or null, in case it can not be found
	 * @throws TeamRepositoryException
	 */
	/**
	 * @param attributeID
	 * @param teamRepository
	 * @param projectAreaHandle
	 * @param monitor
	 * @return
	 * @throws TeamRepositoryException
	 */
	public static IAttribute resolveAttribute(String attributeID, IProjectAreaHandle projectAreaHandle,
			IProgressMonitor monitor) throws TeamRepositoryException {
		if (projectAreaHandle == null) {
			throw new WorkItemCommandLineException("Resolve Attribute: project area handle must not be null");
		}
		ITeamRepository teamRepository = (ITeamRepository) projectAreaHandle.getOrigin();
		IWorkItemCommon workItemCommon = (IWorkItemCommon) teamRepository.getClientLibrary(IWorkItemCommon.class);
		return workItemCommon.findAttribute(projectAreaHandle, attributeID, monitor);
	}
}
