/*******************************************************************************
 * Copyright (c) 2019-2020 Persistent Systems Inc.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.helper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.xml.sax.SAXException;

import com.ibm.js.team.workitem.commandline.commands.ImportItemStatesCommand;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.utils.FileUtil;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.transport.HttpUtil;
import com.ibm.team.repository.transport.client.ITeamRawRestServiceClient;
import com.ibm.team.repository.transport.client.ITeamRawRestServiceClient.IRawRestClientConnection;
import com.ibm.team.workitem.common.IWorkItemCommon;

/**
 * Helper handling of conversion of work item properties into strings.
 * 
 */
public class ItemStateImportHelper {

	// private IProgressMonitor fMonitor;
	private ITeamRepository fTeamRepository;
	private String fImportFolder = null;
	private WorkItemStateHelper fStateHelper = null;
	protected ITeamRawRestServiceClient fHttpClient;
	protected ImportItemStatesCommand fCommand;

	public ItemStateImportHelper(ITeamRepository fTeamRepository, ITeamRawRestServiceClient httpClient,
			String importFolder, IProgressMonitor fMonitor, ImportItemStatesCommand command) {
		super();
		this.fImportFolder = importFolder;
		// this.fMonitor = fMonitor;
		this.fTeamRepository = fTeamRepository;
		this.fStateHelper = new WorkItemStateHelper(fTeamRepository, "-1", fMonitor);
//		this.fHttpClient= command.getService(IHttpService.class).getHttpClient(false);
		this.fHttpClient = httpClient;
		this.fCommand = command;

	}

	void initialize(ITeamRepository fTeamRepository, IProgressMonitor fMonitor) {
		// this.fMonitor = fMonitor;
		this.fTeamRepository = fTeamRepository;
	}

	public String getImportFolder() {
		return this.fImportFolder;
	}

	/**
	 * Get WorkItemCommon
	 * 
	 * @return the IWorkItemCommon
	 */
	protected IWorkItemCommon getWorkItemCommon() {
		IWorkItemCommon workItemCommon = (IWorkItemCommon) getTeamRepository().getClientLibrary(IWorkItemCommon.class);
		return workItemCommon;
	}

	/**
	 * Get the team repository
	 * 
	 * @return
	 */
	private ITeamRepository getTeamRepository() {
		return fTeamRepository;
	}

	public void importItemStates(ImportItemStatesCommand command) {

		try {
			command.logger.info("\nImporting item states to: " + fTeamRepository.getRepositoryURI() + " from folder: "
					+ getImportFolder());

			// For each file in the import folder, import the state
			final File folder = new File(getImportFolder());
			for (final File file : folder.listFiles()) {
				importStateFile(file, command);
			}

		} catch (Exception e) {
			command.logger.info("\n*Exception: " + e + " \n " + e.getMessage());
			command.setFailed();
			throw new WorkItemCommandLineException(e);
		}
	}

	public void importStateFile(File aFile, ImportItemStatesCommand command) {

		String fileName = aFile.getName();
		if (aFile.isDirectory() || aFile.getName().startsWith(".")) {
			return;
		}
		if (fileName.indexOf("workitem_WorkItem") == -1) {
			command.logger.warn("Non work item state file in import folder: " + fileName + " ... the file is skipped");
			return;

		}
		// Only import work item state... check end of file
		// File name is like: 194.000._5cIjUDkEEeup2rju-R9Wqg...workitem_WorkItem
		String typeId = fileName.split("\\.")[5].replaceAll("_modified", "");
		if (!typeId.contentEquals("workitem_WorkItem")) {
			command.logger.warn("Non work item state file in import folder: " + fileName + " ... the file is skipped");
			return;
		}

		String stateId = fileName.split("\\.")[2];
		command.logger.debug("\nImporting: "+ stateId  + " [" + typeId + "]");
		// read the xml from the file
		String newState = null;
		try {
			newState = getFileContent(aFile);
			if (command.logger.isTraceEnabled()) {
				command.logger.trace("Read new state file for state:\n" + newState);
			}
		} catch (IOException e) {
			command.logger.error("\nError reading state file: " + aFile.getAbsolutePath());
			e.printStackTrace();
			command.setFailed();
			throw new WorkItemCommandLineException(e);
		}

		// verify the xml
		try {
			fStateHelper.parseXmlDocument(newState);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			command.logger.error("\nError parsing state file: " + aFile.getAbsolutePath()); 
			command.logger.trace("\n" + newState );
			e.printStackTrace();
			command.setFailed();
			return;
		}

		// get state

		// read the current state
		String currentState = "";
		try {
			currentState = getItemRawContentViaRest(command, stateId, command.logger.isTraceEnabled());
			if (command.logger.isTraceEnabled()) {
				command.logger.trace("Fetched current state for state:\n" + currentState);
			}
		} catch (Exception e) {
			command.logger.error("\nError reading state file: " + aFile.getAbsolutePath());
			e.printStackTrace();
			command.setFailed();
			throw new WorkItemCommandLineException(e);
		}

		// compare the current state to the new state
		if (currentState.equals(newState)) {
			if (!command.isForce()) {
			     command.logger.warn("\n== Current state is the same as old state for state id: " + stateId +" ... The state update is skipped. Use /forceReplace to replace stored state.");
				return;
			} else {
				command.logger.warn("\n== Current state is the same as old state for state id: " + stateId + " ... Prior content will be replaced.");
			}
		}

		// Save the old state in a file
		FileUtil.createFolderWithParents(new File(getImportFolder() + "/backupStates"));
		FileUtil.storeFileContent(getImportFolder() + "/backupStates/" + aFile.getName(), currentState);

		// POST the new /edit state using the form data.
		try {
			postItemRawContentViaRest(command, stateId, newState);
		} catch (Exception e) {
			command.logger.error("\n*Error posting new state id:" + stateId);
			e.printStackTrace();
			command.setFailed();
			throw new WorkItemCommandLineException(e);
		}

		// read the stored state, compare to the intended stored state... should be the same.
		String replacedState = "";
		try {
			replacedState = getItemRawContentViaRest(command, stateId, command.logger.isTraceEnabled());
		} catch (Exception e) {
			command.logger.error("\n*Error getting replaced state id:" + stateId);
			e.printStackTrace();
			command.setFailed();
			throw new WorkItemCommandLineException(e);
		}
		if (!newState.equals(replacedState)) {
			//command.logger.trace("\n=====\nState Store (New):" + newState + "\nState Read (Read after New Stored):" + replacedState + "\n=====\n");
			command.logger.error("\n** Error: Replaced state does not match stored state. state id:\n" + stateId + "\n===After storing:\n" + replacedState
					+ "\n^ State was stored by replaced state did not match. Use /forceReplace to continue past this error.");
			if (!command.isForce()) {
				command.setFailed();
				throw new WorkItemCommandLineException("State was stored by replaced state did not match. Use /forceReplace to continue past this error.");
			} else {
				command.logger.warn("Processing continueing after read back state did not match stored state due to /forceReplace.");
			}
		}
	}

	private final String GET_RAW_ITEM_STATE_RESOURCE_URL_PREFIX = "/repodebug/repository/rawItemState/";
	private final String GET_RAW_ITEM_STATE_RESOURCE_URL_SUFFIX = "/raw";
	public String getItemRawContentViaRest(ImportItemStatesCommand command, String stateId, boolean isTracing)
			throws Exception {
		String repositoryUrl = command.repositoryUrl;
		// https://theserver:9443/ccm/repodebug/repository/rawItemState/_n1tT8KegEeqU3qZ0xzTZsg/raw
		String resourceUrl = repositoryUrl + GET_RAW_ITEM_STATE_RESOURCE_URL_PREFIX + stateId
				+ GET_RAW_ITEM_STATE_RESOURCE_URL_SUFFIX;
		ITeamRawRestServiceClient restClient = command.getRestClient(new URI(resourceUrl));
		IRawRestClientConnection connection = restClient.getConnection(new URI(resourceUrl));

		connection.addRequestHeader(HttpUtil.ACCEPT, "application/xml");
		String content = null;
		try {
			command.logger.trace("... Loading raw item state: " + resourceUrl);
			com.ibm.team.repository.transport.client.ITeamRawRestServiceClient.IRawRestClientConnection.Response response = connection
					.doGet();
//			content = IOUtils.read(response.getResponseStream(), IOUtils.UTF8);
			InputStream inputStream = response.getResponseStream();			
			char[] resultCharArray = IOUtils.toCharArray(inputStream, StandardCharsets.UTF_8.toString());
			content = new String(resultCharArray);
			
			if (isTracing) {
				command.logger.trace("... Content fetched for:" + stateId + " length:" + content.length() + " \n" + content);
			} else {
				command.logger.debug("... Content fetched for:" + stateId + " length:" + content.length());
			}
		} catch (Exception e) {
			command.logger.error("Exception fetching raw content from repodebug via REST GET. "
					+ "Enable REPODEBUG in Advanced Properties and ensure the user has JazzAdmin priveledges. "
					+ "State id:" + stateId);
			command.setFailed();
			throw e;
		}
		connection.release();
		return content;
	}
	
	private final String POST_RAW_ITEM_STATE_RESOURCE_URL_SUFFIX = "/edit";
	public void postItemRawContentViaRest(ImportItemStatesCommand command, String stateId, String newContent)
			throws Exception {
		String repositoryUrl = command.repositoryUrl;
		// https://theserver:9443/ccm/repodebug/repository/rawItemState/_n1tT8KegEeqU3qZ0xzTZsg/edit
		String resourceUrl = repositoryUrl + GET_RAW_ITEM_STATE_RESOURCE_URL_PREFIX + stateId
				+ POST_RAW_ITEM_STATE_RESOURCE_URL_SUFFIX;
		ITeamRawRestServiceClient restClient = command.getRestClient(new URI(resourceUrl));
		IRawRestClientConnection connection = restClient.getConnection(new URI(resourceUrl));

		connection.addRequestHeader(HttpUtil.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		connection.addRequestHeader(HttpUtil.CONTENT_TYPE, "application/x-www-form-urlencoded");
		connection.addRequestHeader(HttpUtil.CONNECTION, "keep-alive");
		connection.addRequestHeader("charset", "utf-8"); // ?
		connection.addRequestHeader(HttpUtil.ACCEPT_ENCODING, "gzip, deflate, br");

		try {
			if (command.logger.isTraceEnabled()) {
				command.logger.trace("... Posting " + stateId + " raw item state: " + resourceUrl  + " length: " + newContent.length() + "\n" + newContent);
			} else {
				command.logger.debug("... Posting " + stateId + " raw item state: " + resourceUrl + " length: " + newContent.length());
			}

			// URL Encode
			newContent= "contents=" + URLEncoder.encode(newContent, StandardCharsets.UTF_8.toString());
			
			ByteArrayInputStream inStream = new ByteArrayInputStream((newContent).getBytes());
			com.ibm.team.repository.transport.client.ITeamRawRestServiceClient.IRawRestClientConnection.Response response = connection
					.doPost(inStream, new Long(newContent.length()), "application/x-www-form-urlencoded");

			InputStream inputStream = response.getResponseStream();			
			char[] resultCharArray = IOUtils.toCharArray(inputStream, StandardCharsets.UTF_8.toString());
			String responseString = new String(resultCharArray);

			// String responseString = IOUtils.read(response.getResponseStream(), IOUtils.UTF8);
						
			if (responseString.indexOf("Rows updated: 1") == -1) {
				command.logger.error("Bad response from: " + resourceUrl + "\n" + responseString);
			}

		} catch (Exception e) {
			command.logger.error("\nException storing raw content from repodebug via REST POST. "
					+ "\n---> Enable REPODEBUG in Advanced Properties and ensure the user has JazzAdmin priveledges."
					+ "\n set JAVA_OPTS=%JAVA_OPTS% -Dcom.ibm.team.repository.debug.users=JazzAdmins"
					+ "\n set JAVA_OPTS=%JAVA_OPTS% -Dcom.ibm.team.repository.debug.accessServiceEnabled=true "
					+ "for State id:" + stateId + "\n" + e + "\n" + e.getMessage());
			command.setFailed();
			throw e;
		} finally {			
			connection.release();
		}
	}

	
	private String getFileContent(File file) throws IOException {
		String path = file.getAbsolutePath();
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, StandardCharsets.UTF_8.toString());
	}

}
