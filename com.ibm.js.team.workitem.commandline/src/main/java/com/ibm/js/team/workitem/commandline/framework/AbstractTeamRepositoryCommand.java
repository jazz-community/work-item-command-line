/*******************************************************************************
 * Copyright (c) 2015-2024 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.framework;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.js.team.workitem.commandline.scenarionotifier.ExpensiveScenarioService;
import com.ibm.js.team.workitem.commandline.scenarionotifier.IExpensiveScenarioService;
import com.ibm.js.team.workitem.commandline.utils.FileUtil;
import com.ibm.team.calm.foundation.common.HttpHeaders;
import com.ibm.team.calm.foundation.common.IHttpClient.IResponse;
import com.ibm.team.calm.foundation.common.internal.Response;
import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler.ILoginInfo;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.transport.client.ITeamRawRestServiceClient;
import com.ibm.team.repository.transport.client.ITeamRawRestServiceClient.IRawRestClientConnection;
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
	private IExpensiveScenarioService fScenarioService;

	public Logger fLogger = LogManager.getLogger(IWorkItemCommandLineConstants.WORK_ITEM_COMMAND_LOGGER); 
	
	public Logger getLogger() {
		if (fLogger != null) {
			return fLogger;
		}
		return LogManager.getLogger(IWorkItemCommandLineConstants.WORK_ITEM_COMMAND_LOGGER); 
	}
	
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
		getParameterManager().syntaxAddRequiredParameter(IWorkItemCommandLineConstants.PARAMETER_USER_ID_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_USER_ID_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddRequiredParameter(IWorkItemCommandLineConstants.PARAMETER_PASSWORD_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_PASSWORD_PROPERTY_EXAMPLE);
	}

	@Override
	public void initialize() {
		// I need the team platform to be started here.
		// To be able to access the aliases
		if (!TeamPlatform.isStarted()) {
			getLogger().debug("Starting Team Platform ...");
			TeamPlatform.startup();
		}
		super.initialize();
	}

	@Override
	public OperationResult execute(IProgressMonitor monitor) throws TeamRepositoryException {
		String scenarioInstance = null;
		try {
			// Login to the repository
			this.fTeamRepository = login();
			scenarioInstance = startScenario();
		} catch (TeamRepositoryException e) {
			this.appendResultString("TeamRepositoryException: Unable to log into repository!");
			this.appendResultString(e.getMessage());
			this.setFailed();
			return getResult();
		}
		try {
			this.process();
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			this.appendResultString("TeamRepositoryException: Unable to process!");
			this.appendResultString("This is often due to permissions or link creation making the target work item invalid. ");
			this.appendResultString(
					"For example creating a parent child relationship to a work item that already has a parent.");
			this.appendResultString(
					"Another cause is using the work item type alias instead of the work item type ID to create a work item.");
			this.appendResultString(e.getMessage());
			this.setFailed();
		}
		stopScenario(scenarioInstance);
		return getResult();
	}

	/**
	 * Start a Resource Intensive Scenario instance
	 * 
	 * @see https://jazz.net/wiki/bin/view/Deployment/CreateCustomScenarios
	 * 
	 * @return
	 */
	protected String startScenario() {
		String scenarioInstance = null;
		try {
			setScenarioService(new ExpensiveScenarioService(getTeamRepository(),
					"WCL_" + IWorkItemCommandLineConstants.VERSIONINFO + "_" + getCommandName()));
			scenarioInstance = getScenarioService().start();
		} catch (NullPointerException | URISyntaxException e) {
			this.appendResultString("Resource Intensive Scenario Notifier Service: Service can not be created!");
		} catch (Exception e) {
			this.appendResultString("Resource Intensive Scenario Notifier Service: Scenario can not be started!");
		}
		return scenarioInstance;
	}

	/**
	 * Stop a Resource Intensive Scenario instance
	 * 
	 * @see https://jazz.net/wiki/bin/view/Deployment/CreateCustomScenarios
	 * 
	 * @param scenarioInstance
	 */
	protected void stopScenario(String scenarioInstance) {
		try {
			getScenarioService().stop(scenarioInstance);
		} catch (Exception e) {
			this.appendResultString("Resource Intensive Scenario Notifier Service: Scenario can not be stopped!");
		}
	}

	protected IExpensiveScenarioService getScenarioService() {
		return fScenarioService;
	}

	protected void setScenarioService(IExpensiveScenarioService service) {
		this.fScenarioService = service;
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
		IWorkItemCommon workItemCommon = (IWorkItemCommon) getTeamRepository().getClientLibrary(IWorkItemCommon.class);
		return workItemCommon;
	}

	/**
	 * @return the IAuditableCommon
	 */
	public IAuditableCommon getAuditableCommon() {
		return (IAuditableCommon) getTeamRepository().getClientLibrary(IAuditableCommon.class);
	}

	/**
	 * Log into the teamrepository. Get the parameters from the parameter managers
	 * list and use the values.
	 * 
	 * @return
	 * @throws TeamRepositoryException
	 */
	private ITeamRepository login() throws TeamRepositoryException {
		String repositoryUrl = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_REPOSITORY_URL_PROPERTY);

		return login(repositoryUrl);
	}
	
	private static HashMap<String, String[]> fPasswordFileContent;
	private HashMap<String, String[]> getPasswordFileContentMap(String passwordFile) {
		if (fPasswordFileContent != null) {
			return fPasswordFileContent;
		}
		
		fPasswordFileContent= new HashMap<String, String[]>();
		ArrayList<String> fileContent= FileUtil.getFileContent(passwordFile);
		String key= null;
		for (String line : fileContent) {
			String[] elements= line.split(" ");
			if (elements.length< 3) {
				getLogger().error("Invalid password file line: " + line);
			} else {
				key= elements[0];
			}
			if (key != null) {				
				fPasswordFileContent.put(key, elements);
				key= null;
			}
		
		}
		return fPasswordFileContent;
	}
	
	private String[] getUserAndPasswordFromPasswordFile(String passwordFile, String repositoryUrl) {
		HashMap<String, String[]> pwMap= getPasswordFileContentMap(passwordFile);
		if(pwMap.get(repositoryUrl) != null) {
			return new String[] { pwMap.get(repositoryUrl)[1],  pwMap.get(repositoryUrl)[1]}; 
		}
		
		// Can't find exact match, check contains
		Set<String> keys= pwMap.keySet();
		for (String key : keys) {
			if(repositoryUrl.indexOf(key) != -1) {
				pwMap.put(repositoryUrl, pwMap.get(key));
				return new String[] { pwMap.get(key)[1],  pwMap.get(key)[2]};
			}
		}		
		getLogger().error("No userid and password found for: " + repositoryUrl);
		
		return null;
	}
	
	private HashMap<String, ITeamRawRestServiceClient> fRepoClients = new HashMap<String, ITeamRawRestServiceClient>();
	public ITeamRawRestServiceClient getRestClient(URI targetUri) throws TeamRepositoryException {
		String repoUri = getRepositoryUri(targetUri);
		ITeamRawRestServiceClient restClient = fRepoClients.get(repoUri);
		if (restClient == null) {
			ITeamRepository repo = login(repoUri);
			restClient = repo.getRawRestServiceClient();
			fRepoClients.put(repoUri, restClient);
		}
		return restClient;
	}

	private String fRepoUrl= null;
	protected String getRepositoryUri(URI targetUri) {
		if (fRepoUrl != null) return fRepoUrl;
		String repositoryUri = null;
		String path[] = targetUri.getPath().split("/");
		if (path.length >= 2) {
			String newPath = path[0] + "/" + path[1];
			repositoryUri = targetUri.toString().replace(targetUri.getPath(), newPath);
		}
		fRepoUrl= repositoryUri;
		return repositoryUri;
	}

	public IResponse createResponse(IRawRestClientConnection.Response rawResponse) throws TeamRepositoryException {
		return new Response(rawResponse.getStatusCode(), new HttpHeaders(rawResponse.getAllResponseHeaders()),
				rawResponse.getResponseStream());
	}
	
	/**
	 * Log into the teamrepository. Get the parameters from the parameter
	 * managers list and use the values.
	 * 
	 * @return
	 * @throws TeamRepositoryException
	 */
	boolean reportedId = false;
	protected ITeamRepository login(String repositoryUrl) throws TeamRepositoryException {
		String passwordFile = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_PASSWORD_FILE_PROPERTY);

		String user= null;
		String password= null;
		if (passwordFile != null) {
			String[] userAndPassword= getUserAndPasswordFromPasswordFile(passwordFile, repositoryUrl);
			if (userAndPassword != null) {				
				user= userAndPassword[0];
				password= userAndPassword[1];
			}
		} 
		
		if (user == null) {			
			user = getParameterManager().consumeParameter(IWorkItemCommandLineConstants.PARAMETER_USER_ID_PROPERTY);
			password = getParameterManager()
					.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_PASSWORD_PROPERTY);
		}
		
		if (user == null) {
			getLogger().error("No user specified.");
			System.exit(500);
		}
		if (password == null) {
			getLogger().error("No password specified.");
			System.exit(500);
		}
		//If debug is on, log first time user logs in.
		if (getLogger().isDebugEnabled() && !reportedId) {			
			fLogger.debug("Logging in as: " + user);
			reportedId= true;
		}
		
		ITeamRepository teamRepository = TeamPlatform.getTeamRepositoryService().getTeamRepository(repositoryUrl);
		teamRepository.registerLoginHandler(new LoginHandler(user, password));
		teamRepository.login(getMonitor());
		return teamRepository;
	}
		
	/**
	 * Internal login handler to perform the login to the repository
	 * 
	 */
	private class LoginHandler implements ILoginHandler, ILoginInfo {

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
