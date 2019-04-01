/*******************************************************************************
 * Copyright (c) 2015-2019 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.scenarionotifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;

import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContent;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.json.JSONObject;
import com.ibm.team.repository.common.transport.TeamServiceException;
import com.ibm.team.repository.transport.client.ITeamRawRestServiceClient.IRawRestClientConnection;
import com.ibm.team.repository.transport.client.ITeamRawRestServiceClient.IRawRestClientConnection.Response;

/**
 * Service to execute a start and stop for an expensive scenario. 
 * 
 * Uses JSON
 * instead of x-www-form-urlencoded to make parsing easier. 
 * 
 * The information to stop the expensive scenario is returned as a string.
 *
 */
public class ExpensiveScenarioService implements IExpensiveScenarioService {

	private static final String EXPENSIVE_SCENARIO_START_PATH = "/service/com.ibm.team.repository.service.serviceability.IScenarioRestService/scenarios/startscenario";
	private static final String EXPENSIVE_SCENARIO_STOP_PATH = "/service/com.ibm.team.repository.service.serviceability.IScenarioRestService/scenarios/stopscenario";
	private static final String ACCEPT_HEADER = "Accept";
	private static final String APPLICATION_JSON = "application/json";
	private static final String SCENARIO_NAME = "scenarioName";
	private static final String SCENARIO_INSTANCE_ID = "scenarioInstanceId";
	
	static final String DEBUG_NOFILE_COMMAND = "debug_nofile";
	static final String DEBUG_FILE_COMMAND = "debug_file";
	static final String START_COMMAND = "start";
	static final String STOP_COMMAND = "stop";


	private URI fPublicURI = null;
	private ITeamRepository fTeamRepository;
	private String fScenarioName;

	/**
	 * Start and stop expensive scenario counter are performed persisting the
	 * scenario counter in a file or pass it as string. See option
	 * persistStartAsFile.
	 * 
	 * @param teamRepository     Team repository
	 * @param scenarioName       the name of the scenario
	 * @throws URISyntaxException
	 */
	public ExpensiveScenarioService(final ITeamRepository teamRepository, final String scenarioName) throws URISyntaxException, NullPointerException {
		if(teamRepository==null)
			throw new NullPointerException("TeamRepository can not be null");
		fTeamRepository = teamRepository;
		String publicURI = teamRepository.getRepositoryURI();
		if(publicURI==null)
			throw new NullPointerException("Public URI can not be null");
		fPublicURI = new URI(publicURI.replaceAll("/$", ""));
		if(scenarioName==null)
			throw new NullPointerException("Scenario name can not be null");
		fScenarioName = scenarioName;
	}

	/**
	 * Start and stop expensive scenario counter are performed persisting the
	 * scenario counter in a file or pass it as string. See option
	 * persistStartAsFile.
	 * 
	 * @param teamRepository     Team repository
	 * @param publicURI          Public URI of the target CLM server
	 * @param scenarioName       the name of the scenario
	 * @throws URISyntaxException
	 */
	public ExpensiveScenarioService(final ITeamRepository teamRepository,final String publicURI,final String scenarioName) throws URISyntaxException, NullPointerException {
		if(teamRepository==null)
			throw new NullPointerException("TeamRepository can not be null");
		fTeamRepository = teamRepository;
		if(publicURI==null)
			throw new NullPointerException("Public URI can not be null");
		fPublicURI = new URI(publicURI.replaceAll("/$", ""));
		if(scenarioName==null)
			throw new NullPointerException("Scenario name can not be null");
		fScenarioName = scenarioName;
	}

	/**
	 * Construct the service URI from the public URI.
	 * 
	 * @param path
	 * @return
	 */
	private URI getServiceURI(String path) {
		return URI.create(fPublicURI.toString() + path);
	}

	/* (non-Javadoc)
	 * @see com.ibm.js.team.monitoring.custom.expensivescenario.IExpensiveScenarioService#start()
	 */
	public String start() throws Exception {
		Response response = null;
		try {
			// Compose the request body
			String body = "{\"" + SCENARIO_NAME + "\"" + ":" + "\"" + fScenarioName + "\"}";
			byte[] data = body.getBytes(IContent.ENCODING_UTF_8);

			// Get the connection
			URI startUri = getServiceURI(EXPENSIVE_SCENARIO_START_PATH);
			IRawRestClientConnection connection = fTeamRepository.getRawRestServiceClient().getConnection(startUri);

			InputStream content = new ByteArrayInputStream(data);
			// Request JSON to make parsing easier
			connection.addRequestHeader(ACCEPT_HEADER, APPLICATION_JSON);
			// We are sending JSON as content type
			connection.doPost(content, data.length, APPLICATION_JSON);
			// Get the response back
			response = connection.getResponse();
			int status = response.getStatusCode();
			if (status == 200) {
				String responseString = IOUtils.toString(response.getResponseStream(), IContent.ENCODING_UTF_8);
				// System.out.println(responseString);
				JSONObject json = JSONObject.parse(new StringReader(responseString));
				String scenarioName = null;
				if (json.containsKey(SCENARIO_NAME)) {
					scenarioName = (String) json.get(SCENARIO_NAME);
				}
				if (scenarioName.equals(fScenarioName)) {
					return responseString;
				}
				throw new Exception("Unexpected Response Body '" + responseString + "'");
			}
			throw new Exception("Unexpected Response Code '" + status + "'");
		} finally {
			if (response != null) {
				response.close();
			}
		}

	}

	/* (non-Javadoc)
	 * @see com.ibm.js.team.monitoring.custom.expensivescenario.IExpensiveScenarioService#stop(java.lang.String)
	 */
	public void stop(String startRequestResponse)
			throws URISyntaxException, TeamServiceException, TeamRepositoryException, IOException, Exception {
		Response response = null;
		try {
			String startRequest = startRequestResponse;
			String scenarioInstanceID;
			if (null == startRequest) {
				throw new Exception("Missing Scenario Start Request");
			}
			try {
				// Parse as JSON to get the scenario ID and the scenario name from the file
				JSONObject json = JSONObject.parse(new StringReader(startRequest));
				String scenarioName = null;
				if (json.containsKey(SCENARIO_NAME)) {
					scenarioName = (String) json.get(SCENARIO_NAME);
				}
				scenarioInstanceID = null;
				if (json.containsKey(SCENARIO_INSTANCE_ID)) {
					scenarioInstanceID = (String) json.get(SCENARIO_INSTANCE_ID);
				}
				if (!fScenarioName.equals(scenarioName)) {
					throw new Exception("Incorrect Scenario Name Exception");
				}
			} catch (Exception e) {
				throw new Exception("Error Parsing Scenario Start Request '" + startRequest + "'");
			}
			// Create the stop request
			URI stopUri = getServiceURI(EXPENSIVE_SCENARIO_STOP_PATH);
			IRawRestClientConnection connection = fTeamRepository.getRawRestServiceClient().getConnection(stopUri);
			connection.addRequestHeader(ACCEPT_HEADER, APPLICATION_JSON);
			byte[] data = startRequest.getBytes(IContent.ENCODING_UTF_8);
			InputStream content = new ByteArrayInputStream(data);
			connection.doPost(content, data.length, APPLICATION_JSON);
			response = connection.getResponse();
			int status = response.getStatusCode();
			if (status == 200) {
				String responseString = IOUtils.toString(response.getResponseStream(), IContent.ENCODING_UTF_8);
				if (responseString != null) {
					// System.out.println(responseString);
					if (responseString.equals("\"" + scenarioInstanceID + "\"")) {
						return;
					}
					throw new Exception("Response Body Scenario Mismatch Exception");
				}
				throw new Exception("Missing Response Body Exception");
			}
			throw new Exception("Unexpected Response Code '" + status + "'");
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}

	@Override
	public Object getScenarioName() {
		return fScenarioName;
	}
}
