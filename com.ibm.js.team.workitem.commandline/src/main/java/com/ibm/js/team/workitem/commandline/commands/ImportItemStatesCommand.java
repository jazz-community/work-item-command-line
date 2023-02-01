/*******************************************************************************
 * Copyright (c) 2020 Persistent Systems Inc.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.commands;


import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.AbstractTeamRepositoryCommand;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.helper.ItemStateImportHelper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.team.repository.common.TeamRepositoryException;
/**
 * Command to export a set of work items to a CSV file set the provided values
 * and save it.
 * 
 * The command supports an RTC compatible mode as well as a special mode that
 * exports IItem values in a way that is uniquely identifying the item and
 * allows to reconstruct the item in the import.
 * 
 * This command uses opencsv-2.3jar @see http://opencsv.sourceforge.net/ as
 * external library to read the CSV file.
 * 
 */
public class ImportItemStatesCommand extends AbstractTeamRepositoryCommand {
	public Logger logger = LogManager.getLogger(ImportItemStatesCommand.class);
	// Parameter to specify the query
	private static final String SWITCH_TRACE = "trace";
	private static final String SWITCH_DEBUG = "debug";
	private static final String SWITCH_FORCE = "forceReplace";
	public String repositoryUrl;
	private String importStatesFolderPath;
	private boolean fForce= false;

	// If there is no value export this
	public static final String CONSTANT_NO_VALUE = "[No content found]";

	// Parameter for the export file name
	private static final String PARAMETER_IMPORT_STATES_FOLDER = "importStatesFolder";
	private static final String PARAMETER_IMPORT_STATES_FOLDER_EXAMPLE = "\"C:\\temp\\\"";

	/**
	 * The constructor
	 * 
	 * @param parametermanager
	 */
	public ImportItemStatesCommand(ParameterManager parametermanager) {
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
		return IWorkItemCommandLineConstants.COMMAND_IMPORT_WORKITEM_STATES;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.js.team.workitem.commandline.framework.AbstractTeamRepositoryCommand
	 * #setRequiredParameters()
	 */
	@Override
	public void setRequiredParameters() {
		getParameterManager().syntaxAddRequiredParameter(
				IWorkItemCommandLineConstants.PARAMETER_REPOSITORY_URL_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_REPOSITORY_URL_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.PARAMETER_USER_ID_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_USER_ID_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.PARAMETER_PASSWORD_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_PASSWORD_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddSwitch(PARAMETER_IMPORT_STATES_FOLDER, PARAMETER_IMPORT_STATES_FOLDER_EXAMPLE);
		getParameterManager().syntaxAddSwitch(SWITCH_DEBUG);
		getParameterManager().syntaxAddSwitch(SWITCH_TRACE);
		getParameterManager().syntaxAddSwitch(SWITCH_FORCE);
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.PARAMETER_PASSWORD_FILE_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_PASSWORD_FILE_PROPERTY_EXAMPLE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractCommand#
	 * helpSpecificUsage()
	 */
	@Override
	public String helpSpecificUsage() {
		return IWorkItemCommandLineConstants.PARAMETER_REPOSITORY_URL_PROPERTY
				+ IWorkItemCommandLineConstants.PARAMETER_PASSWORD_FILE_PROPERTY
				+ IWorkItemCommandLineConstants.PARAMETER_USER_ID_PROPERTY
				+ IWorkItemCommandLineConstants.PARAMETER_PASSWORD_PROPERTY + PARAMETER_IMPORT_STATES_FOLDER;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractCommand#process()
	 */
	@Override
	public OperationResult process() throws TeamRepositoryException {
		if (getParameterManager().hasSwitch(SWITCH_FORCE))
			fForce= true;

		importStatesFolderPath = getParameterManager().consumeParameter(PARAMETER_IMPORT_STATES_FOLDER);
		if (importStatesFolderPath == null) {
			logger.info("No importStatesFolder provided. Exiting.");
			throw new WorkItemCommandLineException("Import States folder path must be provided.");
		}

		repositoryUrl = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_REPOSITORY_URL_PROPERTY);
		try {
			new URI(repositoryUrl);
		} catch (Exception e) {
			logger.error("Error constructing repository uri: " + repositoryUrl);
		}

		new ItemStateImportHelper(getTeamRepository(), getTeamRepository().getRawRestServiceClient(),
				getImportStatesFolderPath(), getMonitor(), this).importItemStates(this);;

		logger.trace("\nImport complete");
		setSuccess();
		return getResult();
	}

	@Override
	public OperationResult execute(org.eclipse.core.runtime.IProgressMonitor monitor) throws TeamRepositoryException {
		return super.execute(monitor);
	}
	
	public String getImportStatesFolderPath() {
		return importStatesFolderPath;
	}

	public boolean isForce() {
		return fForce;
	}


}
