/*******************************************************************************
 * Copyright (c) 2015-2022 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.helper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.logging.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.js.team.workitem.commandline.commands.FindEnumerationIdConflictsCommand;
import com.ibm.js.team.workitem.commandline.commands.ValidateOSLCLinksCommand.GetRDFResourceParams;
import com.ibm.js.team.workitem.commandline.framework.AbstractTeamRepositoryCommand;
import com.ibm.team.calm.foundation.common.HttpHeaders;
// import com.ibm.team.calm.foundation.common.IHttpClient.HttpAccessException;
// import com.ibm.team.calm.foundation.common.IHttpClient.IResponse;
import com.ibm.team.calm.foundation.common.XMLHelper.XMLSerializeException;
import com.ibm.team.calm.foundation.common.internal.OSLCResourceAccess;
import com.ibm.team.calm.foundation.common.internal.RDFUtils;
import com.ibm.team.calm.foundation.common.internal.RDFUtils.RDFParseException;
import com.ibm.team.calm.foundation.common.internal.rest.dto.ResourceResultDTO;
import com.ibm.team.calm.foundation.common.internal.rest.dto.RestFactory;
import com.ibm.team.calm.foundation.common.linking.OSLCResourceDescription;
import com.ibm.team.calm.foundation.common.linking.OSLCResourceDescriptionRegistry;
import com.ibm.team.calm.foundation.common.oslc.OSLCCoreIdentifiers;
import com.ibm.team.process.internal.common.rest.HttpConstants;
import com.ibm.team.repository.common.NotLoggedInException;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.transport.HttpUtil;
import com.ibm.team.repository.common.transport.HttpUtil.CharsetEncoding;
import com.ibm.team.repository.transport.client.ITeamRawRestServiceClient;
import com.ibm.team.repository.transport.client.ITeamRawRestServiceClient.IRawRestClientConnection;

/**
 * Class helps with accessing OSLC Link
 * 
 */
public class ProcessAreaOslcHelper {
	public static final String CONFIGURATION_MANAGEMENT_CONTEXT_HEADER_NAME = "X-OLSC-Configuration-Context"; //$NON-NLS-1$
	public static final String OSLC_CATALOG_URI_PATH = "/oslc/workitems/catalog"; //$NON-NLS-1$

	public ProcessAreaOslcHelper(Logger logger) {
		setTracingLog(logger);
	}
	public ResourceResultDTO fetchProjectAreaCatalog(FindEnumerationIdConflictsCommand command, URI repositoryUri)
			throws NotLoggedInException, IOException, TeamRepositoryException, URISyntaxException {

		// Fetch the project areas in the repository
		GetRDFResourceParams oslcResource = new GetRDFResourceParams();
		oslcResource.oslcCoreVersion = "2.0";
		oslcResource.resourceURL = repositoryUri.toString() + OSLC_CATALOG_URI_PATH;
		oslcResource.oslcResourceID = repositoryUri.toString();
		getTracingLog().trace("Loading repository catalog: " + oslcResource.resourceURL);
		ResourceResultDTO resource= null;
		boolean isValid = false;
		try {
			resource= getRDFResource(command, oslcResource, /* gcUri */ null); // Read result

			if (resource != null && !resource.isSetErrorMessage()) {
				isValid= true;
			}
		} catch (Exception e) {
				getTracingLog().error("-Error 1:" + e + "\n Exception reading project areas: " + e.getMessage());
		}
		if (!isValid) {
			getTracingLog().warn("Failed loading repository catalog.");
		}
		return resource;
	}

//	public static final String PROJECT_AREA_HISTORY_URI_TEMPLATE = "/process/project-areas/$1/history?pageSize=1";
	public static final String PROJECT_AREA_HISTORY_URI_TEMPLATE = "/process/project-areas/$1/history";
	
	private static final String ACCEPT_TYPE = "application/xml";
	public String fetchProjectAreaHistory(FindEnumerationIdConflictsCommand command, String projectAreaId, URI repositoryUri)
			throws NotLoggedInException, IOException, TeamRepositoryException, URISyntaxException {
		String projectAreaHistoryUrl= repositoryUri.toString() + (PROJECT_AREA_HISTORY_URI_TEMPLATE.replace("$1", projectAreaId));
		
		getTracingLog().debug("Loading project history: " + projectAreaHistoryUrl);

		ITeamRawRestServiceClient restClient = command.getRestClient(new URI(projectAreaHistoryUrl));
		IRawRestClientConnection connection = restClient.getConnection(new URI(projectAreaHistoryUrl));
		HttpHeaders headers = new HttpHeaders();
		headers.addHeader(HttpUtil.ACCEPT, ACCEPT_TYPE);

		for (Map.Entry<String, String> header : headers.getEntries()) {
			connection.addRequestHeader(header.getKey(), header.getValue());
		}

		String content=null;
		IRawRestClientConnection.Response response= null;
		try {
			StringBuffer buffer = new StringBuffer();

			response = connection.doGet();
			InputStream responseStream = response.getResponseStream();
			if (responseStream != null) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream, HttpConstants.ENCODING_UTF8));
				char[] chars = new char[1024];
				int read = reader.read(chars);
				while (read > 0) {
					buffer.append(chars, 0, read);
					read = reader.read(chars);
				}
			}
			content= buffer.toString();
		} catch (Exception e) {
			getTracingLog().error("Exception reading response: " + e.getMessage());
		}
		
		connection.release();
		
		return content;
	}
	
	public String fetchProcessAreaXML(FindEnumerationIdConflictsCommand command, String url)
			throws NotLoggedInException, IOException, TeamRepositoryException, URISyntaxException {

		getTracingLog().trace("Loading process area configuration xml: " + url);

		ITeamRawRestServiceClient restClient = command.getRestClient(new URI(url));
		IRawRestClientConnection connection = restClient.getConnection(new URI(url));
		HttpHeaders headers = new HttpHeaders();
		headers.addHeader(HttpUtil.ACCEPT, ACCEPT_TYPE);

		for (Map.Entry<String, String> header : headers.getEntries()) {
			connection.addRequestHeader(header.getKey(), header.getValue());
		}

		String content=null;
		IRawRestClientConnection.Response response= null;
		try {
			StringBuffer buffer = new StringBuffer();
			response = connection.doGet();
			InputStream responseStream = response.getResponseStream();
			if (responseStream != null) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream, HttpConstants.ENCODING_UTF8));
				char[] chars = new char[1024];
				int read = reader.read(chars);
				while (read > 0) {
					buffer.append(chars, 0, read);
					read = reader.read(chars);
				}
			}
			content= buffer.toString();
		} catch (Exception e) {
			getTracingLog().error("Exception reading response: " + e.getMessage());
		}
		
		connection.release();
		
		return content;
	}

	
	public Logger fTracingLog;
	public final Logger getTracingLog() {
		return fTracingLog;
	}

	public void setTracingLog(Logger log) {
		fTracingLog = log;
	}

	public String fetchProjectAreaConfigurationXml(FindEnumerationIdConflictsCommand command,
			DocumentBuilderFactory factory, String projectAreaId, URI repositoryURI, boolean isShowingHistory) {
		String result = "";
		try {
			String history = this.fetchProjectAreaHistory(command, projectAreaId, repositoryURI);
			if (isShowingHistory ) {
				try {
					getTracingLog().info("Project Area history for: " + projectAreaId  + StringEscapeUtils.unescapeXml(history));
				} catch (Exception e) {
					getTracingLog().warn("Could not parse history for " + projectAreaId  + "\n" + history);
				}
			}
			Document hDocument = null;
			try {
				DocumentBuilder hDocumentBuilder = factory.newDocumentBuilder();
				byte[] bytes = history.getBytes(CharsetEncoding.UTF8.toCharset().name());
				hDocument = hDocumentBuilder.parse(new ByteArrayInputStream(bytes));
			} catch (ParserConfigurationException ex) {
				getTracingLog().error("Parse exception: " + ex.getMessage());
				throw new IOException(ex.getMessage());
			} catch (SAXException ex) {
				getTracingLog().error("SAX exception: " + ex.getMessage());
				throw new IOException(ex.getMessage());
			}

			Node hDocumentNode = hDocument.getFirstChild();
			NodeList historyNodes = hDocumentNode.getChildNodes();

			for (int y = 0; y < historyNodes.getLength(); y++) {
				Node historyNode = historyNodes.item(y);
				if (historyNode == null || !historyNode.hasChildNodes())
					continue;

				NodeList historyChangeNodeList = historyNode.getChildNodes();
				for (int z = 0; z < historyChangeNodeList.getLength(); z++) {
					Node historyChangeNode = historyChangeNodeList.item(z);
					if (!historyChangeNode.hasAttributes())
						continue;

					Node hChangeAttribute = historyChangeNode.getAttributes().getNamedItem("jp:url");
					if (hChangeAttribute == null)
						continue;
					String url2 = hChangeAttribute.getNodeValue();
					if (url2.indexOf("process-specification") != -1) {
//						getTracingLog().trace("Loading content: " + url2);
						// Get all project area names
						result = this.fetchProcessAreaXML(command, url2);
						return result;
					}
				}
			}

		} catch (Exception e) {
			getTracingLog().error("Exception " + e.getMessage());
		}
		return result;
	}

	// Non-gc path
	public com.ibm.team.calm.foundation.common.internal.rest.dto.ResourceResultDTO getRDFResource(
			AbstractTeamRepositoryCommand command, GetRDFResourceParams oslcResource, String gcUri)
			throws TeamRepositoryException, URISyntaxException {
		ResourceResultDTO resultDTO = RestFactory.eINSTANCE.createResourceResultDTO();

		OSLCResourceDescription resourceDescription = null;
		if (oslcResource != null) {
			resourceDescription = OSLCResourceDescriptionRegistry.getInstance()
					.getResourceDescription(oslcResource.oslcResourceID);
			if (resourceDescription != null && OSLCResourceAccess.useOslcVersion1(resourceDescription)) {
				resultDTO.setErrorMessage("OSLC Core 2 is disabled.");
				getTracingLog()
						.debug("VerifyBacklinksOperation[4] OSLC Not Available.\nOSLC Core Version 2 is disabled");
				return resultDTO;
			}
		}

		String oslcCoreVersion = oslcResource.oslcCoreVersion;

		ITeamRawRestServiceClient restClient = command.getRestClient(new URI(oslcResource.resourceURL));
		IRawRestClientConnection connection = restClient.getConnection(new URI(oslcResource.resourceURL));

		if (oslcCoreVersion != null) {
			connection.addRequestHeader(OSLCCoreIdentifiers.OSLC_CORE_VERSION, oslcCoreVersion);
		}
		connection.addRequestHeader(HttpUtil.ACCEPT, "application/rdf+xml");

		try {
			com.ibm.team.repository.transport.client.ITeamRawRestServiceClient.IRawRestClientConnection.Response response = connection
					.doGet();
//alt			OSLCResource resource = OSLCResource.loadResource(httpClient, oslcResource.resourceURL, null,
//					ContentType.RDF.getValue(), headers, resourceDescription, oslcCoreVersion);

			String content = RDFUtils.toXML(response.getResponseStream(), oslcResource.resourceURL);

			resultDTO.setContent(content);

			return resultDTO;
		} catch (RDFParseException e) {
			getTracingLog().debug("VerifyBacklinksOperation[5] RDF Parse Exception\n" + e);
			resultDTO.setErrorMessage(e.getMessage());
			return resultDTO;
		} catch (XMLSerializeException e) {
			getTracingLog().debug("VerifyBacklinksOperation[7] XMLSerializeException\n" + e);
			resultDTO.setErrorMessage(e.getMessage());
			throw new TeamRepositoryException(e);
		}
	}


}
