/*******************************************************************************
 * Copyright (c) 2019-2022 IBM
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.commands;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.framework.IWorkItemCommand;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;

/**
 * This command checks either all project area or specified project areas to see
 * if the project contains an Enumeration type or EnumerationList type (or other
 * types) id strings which are the same as existing internal attributes such as
 * tag, tags, and so forth. The command generates a report of the project areas.
 * Each project area must be manually modified by the administrator to change
 * the reference.
 */
public class FindInProjectAreasCommand extends FindEnumerationIdConflictsCommand implements IWorkItemCommand {


	/**
	 * @param parameterManager
	 */
	public FindInProjectAreasCommand(ParameterManager parameterManager) {
		super(parameterManager);
	}

	@Override
	public String getCommandName() {
		return IWorkItemCommandLineConstants.COMMAND_FIND_IN_PROJECT_AREAS;
	}
	
	@Override
	public String helpSpecificUsage() {
		return "" 
				+ IWorkItemCommandLineConstants.PARAMETER_REPOSITORY_URL_PROPERTY
				+ IWorkItemCommandLineConstants.PARAMETER_PASSWORD_FILE_PROPERTY 
				+ IWorkItemCommandLineConstants.PARAMETER_SEARCH_STRING;
	}
	
	protected boolean isSearchEnable() {
		return true;
	}
}