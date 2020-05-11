/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.utils;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.common.IAccessGroup;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.ITeamArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContext;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.UUID;
import com.ibm.team.workitem.common.IAuditableCommon;

/**
 * Utility Class to help with access context
 * 
 */
public class AccessContextUtil {

	/**
	 * String representation for public access
	 */
	public static final String PUBLIC_ACCESS = "Public";

	/**
	 * For a given value from the restricted access attribute, compute the name
	 * of the object. First try if this is a project area, then try a team area,
	 * finally search through the groups.
	 * 
	 * @param uuid
	 * @return An UUID for public access, a IProjectArea, a ITeamArea, a
	 *         IAccessGroup or null
	 */
	public static Object getAccessContextFromUUID(UUID uuid, ITeamRepository teamRepository,
			IAuditableCommon auditableCommon, IProgressMonitor monitor) {
		if (uuid == null) {
			return null;
		}
		if (IContext.PUBLIC.equals(uuid)) {
			return uuid;
		}
		try {
			IProjectArea area = ProcessAreaUtil.getProjectAreaFormUUID(uuid, teamRepository, monitor);
			return area;
		} catch (Exception e) {
			// Catch unwanted exceptions thrown by the API
		}
		try {
			ITeamArea area = ProcessAreaUtil.getTeamAreaFormUUID(uuid, teamRepository, monitor);
			return area.getName();
		} catch (Exception e) {
			// Catch unwanted exceptions thrown by the API
		}
		IAccessGroup[] groups;
		try {
			groups = auditableCommon.getAccessGroups(null, Integer.MAX_VALUE, monitor);
			for (IAccessGroup group : groups) {
				// Compare to the contextID and not the uuid value.
				if (group.getContextId().equals(uuid)) {
					return group;
				}
			}
		} catch (TeamRepositoryException e) {
			// Catch unwanted exceptions thrown by the API
		}
		return null;
	}

	/**
	 * Get an UUID from a process area or access group name - this is typically
	 * used for restricted access
	 * 
	 * @param value
	 * @param teamRepository
	 * @param auditableCommon
	 * @param processClient
	 * @param monitor
	 * @return
	 * @throws TeamRepositoryException
	 */
	public static UUID getAccessContextFromFQN(String value, ITeamRepository teamRepository,
			IAuditableCommon auditableCommon, IProcessClientService processClient, IProgressMonitor monitor)
			throws TeamRepositoryException {
		if (null == value) {
			return null;
		}
		if (value.equals(PUBLIC_ACCESS)) {
			return IContext.PUBLIC;
		}
		try {
			IProcessArea processArea = ProcessAreaUtil.findProcessAreaByFQN(value, processClient, monitor);
			if (processArea != null) {
				return processArea.getContextId();
			}
		} catch (TeamRepositoryException e) {
			// Catch unwanted exceptions thrown by the API
		}
		IAccessGroup[] groups;
		try {
			groups = auditableCommon.getAccessGroups(null, Integer.MAX_VALUE, monitor);
			for (IAccessGroup group : groups) {
				// Compare to the contextID and not the uuid value.
				if (group.getName().equals(value)) {
					return group.getContextId();
				}
			}
		} catch (TeamRepositoryException e) {
			// Catch unwanted exceptions thrown by the API
		}
		return null;
	}
}
