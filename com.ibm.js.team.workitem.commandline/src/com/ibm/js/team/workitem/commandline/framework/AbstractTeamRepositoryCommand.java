/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.framework;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler.ILoginInfo;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.IWorkItemCommon;

/**
 * This class handles the basic functionality to interact with a RTC team
 * repository, especially the login process.
 * 
 * The class can be used as base class to implement new commands.
 * 
 */
public abstract class AbstractTeamRepositoryCommand extends AbstractCommand {

	private ITeamRepository fTeamRepository;

	protected AbstractTeamRepositoryCommand(ParameterManager parametermanager) {
		super(parametermanager);
	}

	/**
	 * Overriding classes should call super to get the parameters added These
	 * are the basic parameters that are always needed to interact with the
	 * repository
	 * 
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractCommand#setRequiredParameters()
	 */
	@Override
	public void setRequiredParameters() {
		getParameterManager().syntaxAddRequiredParameter(
				IWorkItemCommandLineConstants.PARAMETER_REPOSITORY_URL_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_REPOSITORY_URL_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddRequiredParameter(
				IWorkItemCommandLineConstants.PARAMETER_USER_ID_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_USER_ID_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddRequiredParameter(
				IWorkItemCommandLineConstants.PARAMETER_PASSWORD_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_PASSWORD_PROPERTY_EXAMPLE);
	}

	@Override
	public void initialize() {
		// I need the team platform to be started here.
		// To be able to access the aliases
		if (!TeamPlatform.isStarted()) {
			System.out.println("Starting Team Platform ...");
			TeamPlatform.startup();
		}
		super.initialize();
	}

	@Override
	public OperationResult execute(IProgressMonitor monitor) throws TeamRepositoryException {

		try {
			// Login to the repository
			this.fTeamRepository = login();
		} catch (TeamRepositoryException e) {
			this.appendResultString("TeamRepositoryException: Unable to log into repository!");
			this.appendResultString(e.getMessage());
			this.setFailed();
			return getResult();
		}
		try {
			this.process();
		} catch (TeamRepositoryException e) {
			this.appendResultString("TeamRepositoryException: Unable to process!");
			this.appendResultString("This is often due to a link creation making the target work item invalid. ");
			this.appendResultString("For example creating a parent chld relationship to a work item that already has a parent.");
			this.appendResultString(e.getMessage());
			this.setFailed();
		}
		return getResult();
	}

	protected ITeamRepository getTeamRepository() {
		return fTeamRepository;
	}

	/**
	 * @return the IProcessClientService
	 */
	protected IProcessClientService getProcessClientService() {
		IProcessClientService fProcessClient = (IProcessClientService) getTeamRepository()
				.getClientLibrary(IProcessClientService.class);
		return fProcessClient;
	}

	/**
	 * @return the IWorkItemCommon
	 */
	protected IWorkItemCommon getWorkItemCommon() {
		IWorkItemCommon workItemCommon = (IWorkItemCommon) getTeamRepository().getClientLibrary(
				IWorkItemCommon.class);
		return workItemCommon;
	}

	/**
	 * @return the IAuditableCommon
	 */
	protected IAuditableCommon getAuditableCommon() {
		return (IAuditableCommon) getTeamRepository().getClientLibrary(IAuditableCommon.class);
	}

	/**
	 * Log into the teamrepository. Get the parameters from the parameter
	 * managers list and use the values.
	 * 
	 * @return
	 * @throws TeamRepositoryException
	 */
	private ITeamRepository login() throws TeamRepositoryException {
		String repository = getParameterManager().consumeParameter(
				IWorkItemCommandLineConstants.PARAMETER_REPOSITORY_URL_PROPERTY);
		String user = getParameterManager().consumeParameter(
				IWorkItemCommandLineConstants.PARAMETER_USER_ID_PROPERTY);
		String password = getParameterManager().consumeParameter(
				IWorkItemCommandLineConstants.PARAMETER_PASSWORD_PROPERTY);

		ITeamRepository teamRepository = TeamPlatform.getTeamRepositoryService().getTeamRepository(
				repository);
		teamRepository.registerLoginHandler(new LoginHandler(user, password));
		teamRepository.login(getMonitor());
		return teamRepository;
	}

	/**
	 * Log into the teamrepository. Get the parameters from the parameter
	 * managers list and use the values.
	 * 
	 * @return
	 * @throws TeamRepositoryException
	 */
	protected ITeamRepository login(String repository) throws TeamRepositoryException {
		String user = getParameterManager().consumeParameter(
				IWorkItemCommandLineConstants.PARAMETER_USER_ID_PROPERTY);
		String password = getParameterManager().consumeParameter(
				IWorkItemCommandLineConstants.PARAMETER_PASSWORD_PROPERTY);

		ITeamRepository teamRepository = TeamPlatform.getTeamRepositoryService().getTeamRepository(
				repository);
		teamRepository.registerLoginHandler(new LoginHandler(user, password));
		teamRepository.login(getMonitor());
		return teamRepository;
	}

	/**
	 * Internal login handler to perform the login to the repository
	 * 
	 */
	private static class LoginHandler implements ILoginHandler, ILoginInfo {

		private String fUserId;
		private String fPassword;

		private LoginHandler(String userId, String password) {
			fUserId = userId;
			fPassword = password;
		}

		public String getUserId() {
			return fUserId;
		}

		public String getPassword() {
			return fPassword;
		}

		public ILoginInfo challenge(ITeamRepository repository) {
			return this;
		}
	}

}
