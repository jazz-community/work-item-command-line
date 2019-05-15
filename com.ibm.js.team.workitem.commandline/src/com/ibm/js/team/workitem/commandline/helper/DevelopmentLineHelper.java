/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.helper;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.js.team.workitem.commandline.utils.ProcessAreaUtil;
import com.ibm.team.process.common.IDevelopmentLine;
import com.ibm.team.process.common.IDevelopmentLineHandle;
import com.ibm.team.process.common.IIteration;
import com.ibm.team.process.common.IIterationHandle;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.common.model.ItemProfile;

/**
 * Helper class for development lines and iterations. Allows to find a
 * development line and enclosed iteration for a project area.
 * 
 * 
 */
public class DevelopmentLineHelper {

	private enum Mode {
		BYID, BYNAME, BYLABEL
	};

	// Support different compare modes to search development lines and
	// iterations by ID, DisplayName or by Label (ID if DsplayName not
	// specified)
	public static final Mode BYID = Mode.BYID;
	public static final Mode BYNAME = Mode.BYNAME;
	public static final Mode BYLABEL = Mode.BYLABEL;

	private ITeamRepository fTeamRepository;
	private IProgressMonitor fMonitor;
	private IAuditableClient fAuditableClient;

	/**
	 * Constructor
	 * 
	 * @param teamRepository
	 * @param monitor
	 */
	public DevelopmentLineHelper(ITeamRepository teamRepository, IProgressMonitor monitor) {
		fTeamRepository = teamRepository;
		fMonitor = monitor;
	}

	/**
	 * Find a development line based on the path provided.
	 * 
	 * @param projectArea
	 * @param path
	 * @param byId        search by id or name
	 * @return a development line found or null.
	 * @throws TeamRepositoryException
	 */
	public IDevelopmentLine findDevelopmentLine(IProjectAreaHandle projectAreaHandle, List<String> path, Mode comparemode)
			throws TeamRepositoryException {
		int level = 0;
		String fookFor = path.get(level);
		IProjectArea projectArea = ProcessAreaUtil.resolveProjectArea(projectAreaHandle, fMonitor);
		IDevelopmentLineHandle[] developmentLineHandles = projectArea.getDevelopmentLines();
		for (IDevelopmentLineHandle developmentLineHandle : developmentLineHandles) {
			IDevelopmentLine developmentLine = getAuditableClient().resolveAuditable(developmentLineHandle,
					ItemProfile.DEVELOPMENT_LINE_DEFAULT, fMonitor);
			String compare = "";
			switch (comparemode) {
			case BYID:
				compare = developmentLine.getId();
				break;
			case BYNAME:
				compare = developmentLine.getName();
				break;
			case BYLABEL:
				compare = developmentLine.getLabel();
				break;
			}
			if (fookFor.equals(compare)) {
				return developmentLine;
			}
		}
		return null;
	}

	/**
	 * Find an iteration based on the path provided.
	 * 
	 * @param iProjectAreaHandle
	 * @param path
	 * @param byId
	 * @return an iteration if one can be found or null otherwise
	 * 
	 * @throws TeamRepositoryException
	 */
	public IIteration findIteration(IProjectAreaHandle iProjectAreaHandle, List<String> path, Mode comparemode)
			throws TeamRepositoryException {
		getAuditableClient();
		IIteration foundIteration = null;
		IProjectArea projectArea = ProcessAreaUtil.resolveProjectArea(iProjectAreaHandle, fMonitor);
		IDevelopmentLine developmentLine = findDevelopmentLine(projectArea, path, comparemode);
		if (developmentLine != null) {
			foundIteration = findIteration(developmentLine.getIterations(), path, 1, comparemode);
		}
		return foundIteration;
	}

	private IAuditableClient getAuditableClient() {
		if(this.fAuditableClient==null) {
			this.fAuditableClient = (IAuditableClient) fTeamRepository.getClientLibrary(IAuditableClient.class);
		}
		return this.fAuditableClient;
	}

	/**
	 * Find an Iteration
	 * 
	 * @param iterations
	 * @param path
	 * @param level
	 * @param comparemode
	 * @return
	 * @throws TeamRepositoryException
	 */
	private IIteration findIteration(IIterationHandle[] iterations, List<String> path, int level, Mode comparemode)
			throws TeamRepositoryException {
		String lookFor = path.get(level);
		for (IIterationHandle iIterationHandle : iterations) {

			IIteration iteration = getAuditableClient().resolveAuditable(iIterationHandle, ItemProfile.ITERATION_DEFAULT,
					fMonitor);
			String compare = "";
			switch (comparemode) {
			case BYID:
				compare = iteration.getId();
				break;
			case BYNAME:
				compare = iteration.getName();
				break;
			case BYLABEL:
				compare = iteration.getLabel();
				break;
			}
			if (lookFor.equals(compare)) {
				if (path.size() > level + 1) {
					IIteration found = findIteration(iteration.getChildren(), path, level + 1, comparemode);
					if (found != null) {
						return found;
					}
				} else {
					return iteration;
				}
			}
		}
		return null;
	}

	/**
	 * Get the IIteration object from a handle
	 * 
	 * @param handle
	 * @return
	 * @throws TeamRepositoryException
	 */
	public IIteration resolveIteration(IIterationHandle handle) throws TeamRepositoryException {
		if (handle instanceof IIteration) {
			return (IIteration) handle;
		}
		IIteration iteration = (IIteration) fTeamRepository.itemManager().fetchCompleteItem((IIterationHandle) handle,
				IItemManager.DEFAULT, fMonitor);
		return iteration;
	}

	/**
	 * Get the IDevelopmentLine object from a handle
	 * 
	 * @param handle
	 * @return
	 * @throws TeamRepositoryException
	 */
	public IDevelopmentLine resolveDevelopmentLine(IDevelopmentLineHandle handle) throws TeamRepositoryException {
		if (handle instanceof IDevelopmentLine) {
			return (IDevelopmentLine) handle;
		}
		IDevelopmentLine devLine = (IDevelopmentLine) fTeamRepository.itemManager().fetchCompleteItem(handle,
				IItemManager.DEFAULT, fMonitor);
		return devLine;
	}

	/**
	 * Get the development line as string (Label, id or name)
	 * 
	 * @param handle
	 * @param mode
	 * @return
	 * @throws TeamRepositoryException
	 */
	public String getDevelopmentLineAsString(IDevelopmentLineHandle handle, Mode mode) throws TeamRepositoryException {
		IDevelopmentLine devLine = resolveDevelopmentLine(handle);
		switch (mode) {
		case BYID:
			return devLine.getId();
		case BYNAME:
			return devLine.getName();
		case BYLABEL:
			return devLine.getLabel();
		}
		return devLine.getLabel();
	}

	/**
	 * Get the iteration as string (Label, id or name). The mode chosen determines
	 * what value is returned. This returns only the iteration related data and not
	 * the path from the development line to the iteration.
	 * 
	 * @param handle
	 * @param mode
	 * @return
	 * @throws TeamRepositoryException
	 */
	public String getIterationAsString(IIterationHandle handle, Mode mode) throws TeamRepositoryException {
		IIteration iteration = resolveIteration(handle);
		switch (mode) {
		case BYID:
			return iteration.getId();
		case BYNAME:
			return iteration.getName();
		case BYLABEL:
			return iteration.getLabel();
		}
		return iteration.getLabel();
	}

	/**
	 * Get the iteration as full path string (Label, id or name) This includes the
	 * development line and all the iterations above.
	 * 
	 * @param handle
	 * @param mode
	 * @return
	 * @throws TeamRepositoryException
	 */
	public String getIterationAsFullPath(IIterationHandle handle, Mode mode) throws TeamRepositoryException {
		IIteration iteration = resolveIteration(handle);
		String fullPath = getIterationAsString(iteration, mode);
		IIterationHandle parent = iteration.getParent();
		if (parent == null) {
			IDevelopmentLineHandle devLineHandle = iteration.getDevelopmentLine();
			return getDevelopmentLineAsString(devLineHandle, mode) + WorkItemUpdateHelper.PATH_SEPARATOR + fullPath;
		} else {
			return getIterationAsFullPath(parent, mode) + WorkItemUpdateHelper.PATH_SEPARATOR + fullPath;
		}
	}
}
