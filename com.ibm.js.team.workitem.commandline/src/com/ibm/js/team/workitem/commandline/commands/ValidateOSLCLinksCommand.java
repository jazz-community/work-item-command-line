/*******************************************************************************
 * Copyright (c) 2015-2018 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.commands;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.AbstractTeamRepositoryCommand;
import com.ibm.js.team.workitem.commandline.framework.IWorkItemCommand;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.js.team.workitem.commandline.utils.ProcessAreaUtil;
import com.ibm.js.team.workitem.commandline.utils.QueryUtil;
import com.ibm.js.team.workitem.commandline.utils.WorkItemUtil;
import com.ibm.team.calm.foundation.common.HttpHeaders;
import com.ibm.team.calm.foundation.common.IHttpClient.IResponse;
import com.ibm.team.calm.foundation.common.internal.Response;
import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.registry.IEndPointDescriptor;
import com.ibm.team.links.common.registry.ILinkType;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.NotLoggedInException;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.json.JSONArray;
import com.ibm.team.repository.common.json.JSONObject;
import com.ibm.team.repository.common.transport.HttpUtil;
import com.ibm.team.repository.common.transport.HttpUtil.CharsetEncoding;
import com.ibm.team.repository.common.transport.HttpUtil.MediaType;
import com.ibm.team.repository.common.util.NLS;
import com.ibm.team.repository.transport.client.ITeamRawRestServiceClient;
import com.ibm.team.repository.transport.client.ITeamRawRestServiceClient.IRawRestClientConnection;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.ItemURI;
import com.ibm.team.workitem.common.model.WorkItemLinkTypes;
import com.ibm.team.workitem.common.query.IQueryDescriptor;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResult;
import com.ibm.team.workitem.common.query.ResultSize;

/**
 * Command to print the work item types available in a project area.
 * 
 */
public class ValidateOSLCLinksCommand extends AbstractTeamRepositoryCommand implements
		IWorkItemCommand {

	private Logger logger = Logger.getLogger(ValidateOSLCLinksCommand.class);
	// Parameter to specify the query
	private static final String PARAMETER_QUERY_NAME = "query";
	private static final String PARAMETER_QUERY_NAME_EXAMPLE = "\"All WorkItems\"";

	@SuppressWarnings("serial")
	private static Map<String, String> OSLC_TYPE_MAP = new HashMap<String, String>() {
		{
			// OSLC types are defined at:
			// http://docs.oasis-open.org/oslc-domains/cm/v3.0/cm-v3.0-part2-change-mgt-vocab.html
			put(WorkItemLinkTypes.RELATED_REQUIREMENT /* "com.ibm.team.workitem.linktype.rm.relatedRequirement" */,
					"http://open-services.net/ns/cm#affectsRequirement");
			put(WorkItemLinkTypes.AFFECTS_EXECUTION_RESULT /* "com.ibm.team.workitem.linktype.affectsExecutionResult" */,
					"http://open-services.net/ns/cm#affectsTestResult");
			put(WorkItemLinkTypes.BLOCKS_EXECUTION_RECORD /* "com.ibm.team.workitem.linktype.blocksTestExecutionRecord" */,
					"http://open-services.net/ns/cm#blocksTestExecutionRecord");
			put(WorkItemLinkTypes.IMPLEMENTS_REQUIREMENT /* "com.ibm.team.workitem.linktype.implementsRequirement" */,
					"http://open-services.net/ns/cm#implementsRequirement");
			put(WorkItemLinkTypes.RELATED_WORK_ITEM /* "com.ibm.team.workitem.linktype.relatedworkitem.related" */,
					"http://open-services.net/ns/cm#relatedChangeRequest");
			put(WorkItemLinkTypes.RELATED_TEST_CASE /* "com.ibm.team.workitem.linktype.qm.relatedTestCase" */,
					"http://open-services.net/ns/cm#relatedTestCase");
			put(WorkItemLinkTypes.RELATED_EXECUTION_RECORD /* "com.ibm.team.workitem.linktype.qm.relatedExecutionRecord" */,
					"http://open-services.net/ns/cm#relatedTestExecutionRecord");
			put(WorkItemLinkTypes.RELATED_TEST_PLAN /* "com.ibm.team.workitem.linktype.qm.relatedTestPlan" */,
					"http://open-services.net/ns/cm#relatedTestPlan");
			put(WorkItemLinkTypes.TESTED_BY_TEST_CASE /* "com.ibm.team.workitem.linktype.testedByTestCase" */,
					"http://open-services.net/ns/cm#testedByTestCase");
			put(WorkItemLinkTypes.TRACKS_REQUIREMENT /* "com.ibm.team.workitem.linktype.rm.tracksRequirement" */,
					"http://open-services.net/ns/cm#tracksRequirement");

			// RELATED_TEST_SCRIPT does not return the backlink
			put(WorkItemLinkTypes.RELATED_TEST_SCRIPT /* "com.ibm.team.workitem.linktype.qm.relatedTestScript" */,
					"http://open-services.net/ns/cm#relatedChangeRequest");
			/*
			 * CCM to CCM links are not checked because they don't use link index
			 */
			// put(WorkItemLinkTypes.AFFECTED_BY_DEFECT /*
			// "com.ibm.team.workitem.linktype.cm.affectedByDefect" */,
			// "http://open-services.net/ns/cm#affectedByDefect");
			// put(WorkItemLinkTypes.AFFECTS_PLAN_ITEM /*
			// "com.ibm.team.workitem.linktype.cm.affectsPlanItem" */,
			// "http://open-services.net/ns/cm#affectsPlanItem");

			/* Tracks/Contributes To are external ccm link types not oslc */
			// put(WorkItemLinkTypes.CONTRIBUTES_TO_WORK_ITEM /*
			// "com.ibm.team.workitem.linktype.trackedworkitem" */,
			// "http://open-services.net/ns/cm#trackedWorkItem");
			// put(WorkItemLinkTypes.TRACKS_WORK_ITEM /*
			// "com.ibm.team.workitem.linktype.tracksworkitem" */,
			// "http://open-services.net/ns/cm#tracksWorkItem");

			// TODO: add more backlinks
			// put("none", "http://open-services.net/ns/cm#ChangeRequest");
			// put("none",
			// "http://open-services.net/ns/cm#requirementsChangeRequest");
			// put("none", "http://open-services.net/ns/cm#tracksChangeSet");
		}
	};
	private IProjectArea projectArea;
	private IWorkItemClient workItemService;
	private HashMap<String, ITeamRawRestServiceClient> repoClients = new HashMap<String, ITeamRawRestServiceClient>();

	public static final class GetRDFResourceParams {
		public String resourceURL;
		public String oslcCoreVersion;
		public String oslcResourceID;
	}

	/**
	 * @param parameterManager
	 */
	public ValidateOSLCLinksCommand(ParameterManager parameterManager) {
		super(parameterManager);
		logger.setLevel(Level.WARN);
	}

	@Override
	public String getCommandName() {
		return IWorkItemCommandLineConstants.COMMAND_VALIDATE_OSLC_LINKS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractWorkItemCommand
	 * #setRequiredParameters()
	 */
	public void setRequiredParameters() {
		// Add the parameters required to perform the operation
		super.setRequiredParameters();
		getParameterManager().syntaxAddRequiredParameter(
				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddRequiredParameter(PARAMETER_QUERY_NAME,
				PARAMETER_QUERY_NAME_EXAMPLE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractWorkItemCommand #process()
	 */
	@Override
	public OperationResult process() throws TeamRepositoryException {
		// Get the parameters such as project area name and Attribute Type and
		// run the operation
		String projectAreaName = getParameterManager().consumeParameter(
				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY).trim();

		projectArea = ProcessAreaUtil.findProjectAreaByFQN(projectAreaName,
				getProcessClientService(), getMonitor());
		if (projectArea == null) {
			throw new WorkItemCommandLineException("Project Area not found: " + projectAreaName);
		}
		String queryName = getParameterManager().consumeParameter(PARAMETER_QUERY_NAME);
		if (queryName == null || queryName.isEmpty()) {
			throw new WorkItemCommandLineException("Query name must be provided.");
		}
		workItemService = (IWorkItemClient) getTeamRepository().getClientLibrary(
				IWorkItemClient.class);
		try {
			validateOslcLinksFromQuery(projectArea, queryName);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		setSuccess();
		return getResult();
	}

	/**
	 * Print the work item types available in a project area
	 * 
	 * @param projectArea
	 * @param queryName
	 * @return
	 * @throws TeamRepositoryException
	 * @throws IOException
	 */
	private void validateOslcLinksFromQuery(IProjectArea projectArea, String queryName)
			throws TeamRepositoryException, IOException {

		// OperationResult opResult = new OperationResult();
		appendResultString("ProjectArea: " + projectArea.getName());
		appendResultString("Query: " + queryName);
		IQueryDescriptor query = QueryUtil.findPersonalQuery(queryName, projectArea,
				getTeamRepository().loggedInContributor(), getMonitor());
		if (query == null) {
			throw new WorkItemCommandLineException("Query not found: " + queryName);
		}
		// Query the work items
		IQueryResult<IResult> results = QueryUtil.getUnresolvedQueryResult(query,
				isOverrideQueryResultSizeLimit());

		ResultSize resultSize = results.getResultSize(getMonitor());
		logger.trace("Query result size: " + resultSize.getTotal());
		List<IWorkItemHandle> workItems = new ArrayList<IWorkItemHandle>(resultSize.getTotal());
		while (results.hasNext(null)) {
			IResult result = results.next(null);
			workItems.add((IWorkItemHandle) result.getItem());
		}
		for (IWorkItemHandle workItemHandle : workItems) {
			validateOslcLinks(workItemHandle);
		}
	}

	private void validateOslcLinks(IWorkItemHandle workItemHandle) throws TeamRepositoryException,
			IOException {
		IWorkItem workItem = WorkItemUtil.resolveWorkItem(workItemHandle, IWorkItem.FULL_PROFILE,
				getWorkItemCommon(), getMonitor());
		IWorkItemReferences wiReferences = getWorkItemCommon().resolveWorkItemReferences(
				workItemHandle, getMonitor());
		List<IEndPointDescriptor> endPoints = wiReferences.getTypes();
		URI currentWorkItemURI = ItemURI.createWorkItemURI(getAuditableCommon(), workItem.getId());
		String gcUriString = null;
		for (IEndPointDescriptor endPoint : endPoints) {
			List<IReference> links = wiReferences.getReferences(endPoint);
			for (IReference reference : links) {
				if (reference.isURIReference()) {
					ILinkType linkType = endPoint.getLinkType();

					if (OSLC_TYPE_MAP.containsKey(linkType.getLinkTypeId())) {
						URI uri = reference.createURI();
						String linkTypeId = linkType.getLinkTypeId();
						logger.debug("WorkItem: " + workItem.getId() + " LinkTypeId: " + linkTypeId
								+ " ComponentId: " + linkType.getComponentId());
						logger.debug("\tURI: " + uri);

						gcUriString = workItemService.getConfigurationUriForWorkItemByLinkType(
								projectArea.getItemId().getUuidValue(), workItem.getItemId()
										.getUuidValue(), linkTypeId, getMonitor());

						/*
						 * The GC could be defined in the Found In attribute or the Planned For
						 * attribute Since a non Defect may not have a Found In attribute need to
						 * choose a type that will use the Planned For Attribute. The Attribute
						 * chosen is defined in the Process Configuration of the Project Area. The
						 * if statement below is using the default configuration.
						 */
						if (gcUriString == null || gcUriString.isEmpty()) {
							switch (linkTypeId) {
							case WorkItemLinkTypes.AFFECTS_EXECUTION_RESULT:
							case WorkItemLinkTypes.BLOCKS_EXECUTION_RECORD:
							case WorkItemLinkTypes.RELATED_TEST_CASE:
							case WorkItemLinkTypes.RELATED_TEST_PLAN:
							case WorkItemLinkTypes.RELATED_TEST_SCRIPT:
							case WorkItemLinkTypes.RELATED_EXECUTION_RECORD:
								linkTypeId = WorkItemLinkTypes.TESTED_BY_TEST_CASE;
								break;
							case WorkItemLinkTypes.ELABORATED_BY:
								linkTypeId = WorkItemLinkTypes.IMPLEMENTS_REQUIREMENT;
							}
							gcUriString = workItemService.getConfigurationUriForWorkItemByLinkType(
									projectArea.getItemId().getUuidValue(), workItem.getItemId()
											.getUuidValue(), linkTypeId, getMonitor());
						}

						if (gcUriString != null && !gcUriString.isEmpty()) {
							URI gcUri = null;
							try {
								gcUri = new URI(gcUriString);
							} catch (URISyntaxException e) {
								gcUri = null;
								e.printStackTrace();
							}
							String targetLinkType = OSLC_TYPE_MAP.get(reference.getLink()
									.getLinkTypeId());
							JSONArray ldxBacklinks = getLDXBackLinkViaRest(reference.getLink()
									.getTargetRef().createURI(),
									gcUri == null ? "" : gcUri.getPath(), targetLinkType);
							GetRDFResourceParams oslcResource = new GetRDFResourceParams();
							oslcResource.oslcCoreVersion = "2.0";
							oslcResource.resourceURL = reference.getLink().getTargetRef()
									.createURI().toString();
							oslcResource.oslcResourceID = reference.getLink().getTargetRef()
									.createURI().toString();
							boolean wasFound = false;
							for (int i = 0; i < ldxBacklinks.size(); i++) {
								JSONObject triple = (JSONObject) ldxBacklinks.get(i);
								wasFound = isLinkInTripleEqual(triple,
										currentWorkItemURI.toString(), targetLinkType,
										oslcResource.resourceURL); // TODO:
																	// Check
																	// link
																	// parameters?
								if (wasFound) {
									logger.trace("\n> Found LDX (REST) link. Work item: "
											+ workItem.getId() + " |source: "
											+ triple.get("sourceURL") + " |linkType: "
											+ triple.get("linkType") + " |Target:"
											+ triple.get("targetURL"));
									break;
								} else {
									logger.trace("\n... Skipping LDX link with source: "
											+ triple.get("sourceURL") + " |linkType: "
											+ triple.get("linkType") + " |Target:"
											+ triple.get("targetURL"));
								}
							}
							if (!wasFound) {
								switch (reference.getLink().getLinkTypeId()) {
								case WorkItemLinkTypes.RELATED_TEST_SCRIPT:
									System.out
											.println("\n*** Warning *** Backlink validation not supported for linkType: "
													+ reference.getLink().getLinkTypeId()
													+ " workitem: " + workItem.getId());
									break;
								default:
									System.out
											.println("\n*** Warning *** Backlink not found for workitem: "
													+ workItem.getId()
													+ " linkType: "
													+ reference.getLink().getLinkTypeId()
													+ " |target: "
													+ reference.getLink().getTargetRef()
															.createURI().toString()
													+ "|gc : "
													+ gcUri.getPath());
									System.out
											.println("\tIf link was just created wait a few minutes for the link to propagate.");
									break;
								}
							}
						} else {
							logger.warn("No GlobalConfiguration for work item: " + workItem.getId()
									+ " link type: " + linkType.getLinkTypeId());
						}
					}
				}
			}
		}
	}

	private static final String QUERY_JSON = "\"targetURLs\": [ \"{0}\" ],\"linkTypes\":  [ \"{1}\"  ],\"gcURL\": \"{2}\""; // Do
																															// not

	/* gcUrl can be empty */
	private JSONArray getLDXBackLinkViaRest(URI targetUri, String gcUrl, String linkType)
			throws NotLoggedInException, IOException, TeamRepositoryException {
		JSONArray result = null;
		try {
			String ldxUri = getLinkIndexProviderUri(targetUri);
			if (ldxUri == null) {
				logger.warn("\n... cannot create LDX (REST) uri from: " + targetUri);
				return result;
			}
			ITeamRawRestServiceClient restClient = getRestClient(targetUri);
			IRawRestClientConnection connection = restClient.getConnection(new URI(ldxUri));

			HttpHeaders headers = new HttpHeaders();
			headers.addHeader(HttpUtil.ACCEPT, /* MediaType.JSON.toString() */"text/json");
			headers.addHeader(HttpUtil.PRAGMA, HttpUtil.NO_CACHE);

			String content = "{" + NLS.bind(QUERY_JSON, targetUri.toString(), linkType, gcUrl)
					+ "}"; // add braces
			logger.debug("\n... LDX (REST) POST: " + ldxUri + " content:\n" + content
					+ "\n Accept: " + headers.getValue(HttpUtil.ACCEPT) + " encoding: "
					+ MediaType.JSON.toString());

			for (Map.Entry<String, String> header : headers.getEntries()) {
				connection.addRequestHeader(header.getKey(), header.getValue());
			}
			String encoding = CharsetEncoding.UTF8.name();
			byte[] bytes = content.getBytes(encoding);
			ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
			IResponse response = createResponse(connection.doPost(stream, bytes.length,
					MediaType.JSON.toString()));

			String responseContent = response == null ? "[No Response]" : response.getContent();
			connection.release();
			logger.debug("\n... response from LDX (REST):\n " + responseContent);
			if (response != null) {
				Reader reader = new StringReader(responseContent);
				result = parseJson(reader);
			}
		} catch (NotLoggedInException e) {
			System.err.println("\n... *** Not Logged In Exception from LDX (REST):\n "
					+ e.getMessage() + " " + e.toString());
			throw e;
		} catch (TeamRepositoryException e) {
			System.err.println("\n... *** Team Repository Exception from LDX (REST):\n "
					+ e.getMessage() + " " + e.toString());
			throw e;
		} catch (IOException e) {
			System.err.println("\n... *** IO Exception from LDX (REST):\n " + e.getMessage() + " "
					+ e.toString());
			throw e;
		} catch (Exception e) {
			System.err.println("\n... *** Exception from LDX (REST):\n " + e.getMessage() + " "
					+ e.toString() + " " + e.getCause().getMessage());
		}
		return result;
	}

	private ITeamRawRestServiceClient getRestClient(URI targetUri) throws TeamRepositoryException {
		String repoUri = getRepositoryUri(targetUri);
		ITeamRawRestServiceClient restClient = repoClients.get(repoUri);
		if (restClient == null) {
			ITeamRepository repo = login(repoUri);
			restClient = repo.getRawRestServiceClient();
			repoClients.put(repoUri, restClient);
		}
		return restClient;
	}

	private String getRepositoryUri(URI targetUri) {
		String repositoryUri = null;
		String path[] = targetUri.getPath().split("/");
		if (path.length >= 2) {
			String newPath = path[0] + "/" + path[1];
			repositoryUri = targetUri.toString().replace(targetUri.getPath(), newPath);
		}
		return repositoryUri;
	}

	private IResponse createResponse(IRawRestClientConnection.Response rawResponse)
			throws TeamRepositoryException {
		return new Response(rawResponse.getStatusCode(), new HttpHeaders(
				rawResponse.getAllResponseHeaders()), rawResponse.getResponseStream());
	}

	// https://<yourhostname>/<applicationContext>/linkIndex (e.g.
	// https://rqmple2.rtp.raleigh.ibm.com:9443/qm/linkIndex)
	private String getLinkIndexProviderUri(URI targetUri) throws TeamRepositoryException,
			MalformedURLException {
		String linkIndexProviderUri = null;
		if (linkIndexProviderUri == null) {
			String fullUri = targetUri.toString();
			String path[] = targetUri.getPath().split("/");
			if (path.length < 2)
				return null;
			String newPath = path[0] + "/" + path[1] + "/linkIndex/query?Accept=text/json";
			linkIndexProviderUri = fullUri.replace(targetUri.getPath(), newPath);
		}
		return linkIndexProviderUri;
	}

	private JSONArray parseJson(Reader reader) throws IOException {
		Object object = JSONObject.parseAny(reader);
		if (object instanceof JSONObject) {
			logger.trace("\n...... loaded JSON object: " + object);
			Object linksObject = ((JSONObject) object).get(LINKS);
			if (linksObject == null) {
				logger.warn("\n......... links not found (null): " + linksObject);
				return null;
			}
			if (linksObject instanceof JSONArray) {
				logger.trace("\n......... links found: " + linksObject);
				JSONArray links = (JSONArray) linksObject;
				return links;
			}

			logger.warn("\n...... *** links is not JSONArray: " + linksObject);
			return null;
		}
		logger.warn("\n...... *** JSON object not found: " + object);
		return null;
	}

	private static final String LINKS = "links";
	private static final String TRIPLE_SOURCE_URL = "sourceURL";
	private static final String LINK_TYPE = "linkType";
	private static final String TRIPLE_TARGET_URL = "targetURL";

	private boolean isLinkInTripleEqual(JSONObject triple, String sourceURL, String linkType,
			String targetURL) {
		if (triple == null) {
			logger.warn("\n... *** null triple"); // should not happen
			return false;
		}

		boolean result = triple.get(TRIPLE_SOURCE_URL).equals(sourceURL)
				&& triple.get(LINK_TYPE).equals(linkType)
				&& triple.get(TRIPLE_TARGET_URL).equals(targetURL);

		logger.debug("\n..... is triple equal source: " + triple.get(TRIPLE_SOURCE_URL) + " = "
				+ sourceURL + " : " + triple.get(TRIPLE_SOURCE_URL).equals(sourceURL)
				+ "\nlinkType:" + triple.get(LINK_TYPE) + " = " + linkType + " : "
				+ triple.get(LINK_TYPE).equals(linkType) + "\nTarget:"
				+ triple.get(TRIPLE_TARGET_URL) + " = " + targetURL + " : "
				+ triple.get(TRIPLE_TARGET_URL).equals(targetURL));

		return result;
	}

	private boolean isOverrideQueryResultSizeLimit() {
		// TODO Potential option for the future
		return true;
	}

	@Override
	public String helpSpecificUsage() {
		return "";
	}
}