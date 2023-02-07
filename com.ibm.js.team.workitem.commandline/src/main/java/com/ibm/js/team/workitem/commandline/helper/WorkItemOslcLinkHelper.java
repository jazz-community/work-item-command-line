/*******************************************************************************
 * Copyright (c) 2019-2023 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.helper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.eclipse.core.runtime.URIUtil;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.JenaException;
import com.ibm.js.team.workitem.commandline.commands.ValidateOSLCLinksCommand;
import com.ibm.js.team.workitem.commandline.commands.ValidateOSLCLinksCommand.GetRDFResourceParams;
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
import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.registry.IEndPointDescriptor;
import com.ibm.team.links.common.service.ILinkIndexService.LinkTriple;
import com.ibm.team.repository.common.NotLoggedInException;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.json.JSONArray;
import com.ibm.team.repository.common.json.JSONObject;
import com.ibm.team.repository.common.transport.HttpUtil;
import com.ibm.team.repository.transport.client.ITeamRawRestServiceClient;
import com.ibm.team.repository.transport.client.ITeamRawRestServiceClient.IRawRestClientConnection;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.ItemURI;
import com.ibm.team.workitem.common.model.WorkItemLinkTypes;

/**
 * Class helps with accessing OSLC Link
 * 
 */
@SuppressWarnings({ "deprecation" })
public class WorkItemOslcLinkHelper {
	public static final String CONFIGURATION_MANAGEMENT_CONTEXT_HEADER_NAME = "X-OLSC-Configuration-Context"; //$NON-NLS-1$
	// In the LDX calls, the forward link is stored
	@SuppressWarnings("serial")
	private static Map<String, String> OSLC_TYPE_MAP = new HashMap<String, String>() {
		{
			// ignore com.ibm.team.workitem.linktype.textualReference and relatedArtifact
			put(WorkItemLinkTypes.AFFECTED_BY_DEFECT /* "com.ibm.team.workitem.linktype.cm.affectedByDefect" */,
					"http://open-services.net/ns/cm#affectedByDefect");
			put(WorkItemLinkTypes.AFFECTS_PLAN_ITEM /* "com.ibm.team.workitem.linktype.cm.affectsPlanItem" */,
					"http://open-services.net/ns/cm#affectsPlanItem");
			put(WorkItemLinkTypes.CONTRIBUTES_TO_WORK_ITEM /* "com.ibm.team.workitem.linktype.trackedworkitem" */,
					"http://open-services.net/ns/cm#trackedWorkItem");
			put(WorkItemLinkTypes.RELATED_WORK_ITEM /* "com.ibm.team.workitem.linktype.relatedworkitem.related" */,
					"http://open-services.net/ns/cm#relatedChangeRequest");
			put(WorkItemLinkTypes.TRACKS_WORK_ITEM /* "com.ibm.team.workitem.linktype.tracksworkitem" */,
					"http://open-services.net/ns/cm#tracksWorkItem");

			put(WorkItemLinkTypes.IMPLEMENTS_REQUIREMENT /* "com.ibm.team.workitem.linktype.implementsRequirement" */,
					"http://open-services.net/ns/cm#implementsRequirement");
			put(WorkItemLinkTypes.RELATED_REQUIREMENT /* "com.ibm.team.workitem.linktype.rm.relatedRequirement" */,
					"http://open-services.net/ns/cm#affectsRequirement");

			put(WorkItemLinkTypes.BLOCKS_EXECUTION_RECORD /*
															 * "com.ibm.team.workitem.linktype.blocksTestExecutionRecord"
															 */,
					"http://open-services.net/ns/cm#blocksTestExecutionRecord");
			put(WorkItemLinkTypes.AFFECTS_EXECUTION_RESULT /* "com.ibm.team.workitem.linktype.affectsExecutionResult" */,
					"http://open-services.net/ns/cm#affectsTestResult");
			put(WorkItemLinkTypes.RELATED_TEST_CASE /* "com.ibm.team.workitem.linktype.qm.relatedTestCase" */,
					"http://open-services.net/ns/cm#:relatedTestCase");
			put(WorkItemLinkTypes.RELATED_TEST_PLAN /* "com.ibm.team.workitem.linktype.qm.relatedTestPlan" */,
					"http://open-services.net/ns/cm#relatedTestPlan");
			put(WorkItemLinkTypes.RELATED_TEST_SCRIPT /* "com.ibm.team.workitem.linktype.qm.relatedTestScript" */,
					"http://open-services.net/ns/cm#relatedChangeRequest");
			put(WorkItemLinkTypes.TESTED_BY_TEST_CASE /* "com.ibm.team.workitem.linktype.testedByTestCase" */,
					"http://open-services.net/ns/cm#elatedTestScript");
// RELATED_EXECUTION_RECORD does not return the backlink in the RDF
			put(WorkItemLinkTypes.RELATED_EXECUTION_RECORD /*
															 * "com.ibm.team.workitem.linktype.qm.relatedExecutionRecord"
															 */,
					"http://open-services.net/ns/cm#relatedTestExecutionRecord");// Related Test Execution Record not ok
// TODO: add more backlinks
//			put("none", "http://open-services.net/ns/cm#affectsTestResult");
//			put("none", "http://open-services.net/ns/cm#ChangeRequest");
//			put("none", "http://open-services.net/ns/cm#relatedChangeRequest");
//			put("none", "http://open-services.net/ns/cm#requirementsChangeRequest");
//			put("none", "http://open-services.net/ns/cm#tracksChangeSet");
//			put("none", "http://open-services.net/ns/cm#tracksRequirement");
		}
	};
	// Note see also: LinkTypeDTO[] linkTypes= WorkItemLinksData.linkTypes.
	// com.ibm.team.workitem.common.model.WorkItemLinkTypes

	// Reverse link map... example: The key
	// "com.ibm.team.workitem.linktype.tracksworkitem" expects to find the
	// reverse link in the RDF "http://open-services.net/ns/cm#trackedWorkItem"
	@SuppressWarnings("serial")
	private static Map<String, String> REVERSE_OSLC_MAP = new HashMap<String, String>() {
		{
			// ignore com.ibm.team.workitem.linktype.textualReference and relatedArtifact
			put(WorkItemLinkTypes.AFFECTED_BY_DEFECT /* "com.ibm.team.workitem.linktype.cm.affectedByDefect" */,
					"http://open-services.net/ns/cm#affectsPlanItem"); // Affected by Defect not ok
			put(WorkItemLinkTypes.AFFECTS_PLAN_ITEM /* "com.ibm.team.workitem.linktype.cm.affectsPlanItem" */,
					"http://open-services.net/ns/cm#affectedByDefect"); // ok
			put(WorkItemLinkTypes.CONTRIBUTES_TO_WORK_ITEM /* "com.ibm.team.workitem.linktype.trackedworkitem" */,
					"http://open-services.net/ns/cm#tracksWorkItem"); // ok
			put(WorkItemLinkTypes.RELATED_WORK_ITEM /* "com.ibm.team.workitem.linktype.relatedworkitem.related" */,
					"http://open-services.net/ns/cm#relatedChangeRequest");
			put(WorkItemLinkTypes.TRACKS_WORK_ITEM /* "com.ibm.team.workitem.linktype.tracksworkitem" */,
					"http://open-services.net/ns/cm#trackedWorkItem"); // ok

			put(WorkItemLinkTypes.IMPLEMENTS_REQUIREMENT /* "com.ibm.team.workitem.linktype.implementsRequirement" */,
					"http://open-services.net/ns/rm#implementedBy"); // ok
			put(WorkItemLinkTypes.RELATED_REQUIREMENT /* "com.ibm.team.workitem.linktype.rm.relatedRequirement" */,
					"http://open-services.net/ns/rm#affectedBy"); // Affects Requirement ok

			put(WorkItemLinkTypes.BLOCKS_EXECUTION_RECORD /*
															 * "com.ibm.team.workitem.linktype.blocksTestExecutionRecord"
															 */,
					"http://open-services.net/ns/qm#blockedByChangeRequest"); // ok
			put(WorkItemLinkTypes.AFFECTS_EXECUTION_RESULT /* "com.ibm.team.workitem.linktype.affectsExecutionResult" */,
					"http://open-services.net/ns/qm#affectedByChangeRequest"); // ok
			put(WorkItemLinkTypes.RELATED_TEST_CASE /* "com.ibm.team.workitem.linktype.qm.relatedTestCase" */,
					"http://open-services.net/ns/qm#relatedChangeRequest"); // ok
			put(WorkItemLinkTypes.RELATED_TEST_PLAN /* "com.ibm.team.workitem.linktype.qm.relatedTestPlan" */,
					"http://open-services.net/ns/qm#relatedChangeRequest"); // ok
			put(WorkItemLinkTypes.RELATED_TEST_SCRIPT /* "com.ibm.team.workitem.linktype.qm.relatedTestScript" */,
					"http://open-services.net/ns/qm#relatedChangeRequest"); // ok
			put(WorkItemLinkTypes.TESTED_BY_TEST_CASE /* "com.ibm.team.workitem.linktype.testedByTestCase" */,
					"http://open-services.net/ns/qm#testsChangeRequest"); // test
// RELATED_EXECUTION_RECORD does not return the backlink in the RDF
			put(WorkItemLinkTypes.RELATED_EXECUTION_RECORD /*
															 * "com.ibm.team.workitem.linktype.qm.relatedExecutionRecord"
															 */, "http://open-services.net/ns/qm#relatedChangeRequest");// Related
																														// Test
																														// Execution
																														// Record
																														// not
																														// ok
// TODO: add more backlinks
//			put("none", "http://open-services.net/ns/cm#affectsTestResult");
//			put("none", "http://open-services.net/ns/cm#ChangeRequest");
//			put("none", "http://open-services.net/ns/cm#relatedChangeRequest");
//			put("none", "http://open-services.net/ns/cm#requirementsChangeRequest");
//			put("none", "http://open-services.net/ns/cm#tracksChangeSet");
//			put("none", "http://open-services.net/ns/cm#tracksRequirement");
		}
	};
	private static Map<String, Boolean> EXCLUDED_TYPES = new HashMap<String, Boolean>() {
		{
			put("relatedArtifact", new Boolean(true));
			put("com.ibm.team.workitem.linktype.relatedArtifact", new Boolean(true));
//			put("com.ibm.team.workitem.linktype.cm.affectedByDefect", new Boolean(true)); // does not work? TODO: Test affectedByDefect
			put("com.ibm.team.workitem.linktype.textualReference", new Boolean(true)); // not needed

			// "com.ibm.team.workitem.linktype.qm.relatedExecutionRecord" not yet supported
			put(WorkItemLinkTypes.RELATED_EXECUTION_RECORD /*
															 * "com.ibm.team.workitem.linktype.qm.relatedExecutionRecord"
															 */, new Boolean(true));
		}
	};

	public void validateLdxWithoutGCLinks(ValidateOSLCLinksCommand command, IWorkItem workItem, IEndPointDescriptor endPointDescriptor, IReference reference, Logger logger)
			throws NotLoggedInException, IOException, TeamRepositoryException, URISyntaxException {
		setTracingLog(logger);
		boolean backLinkMissing = false;

		URI currentWorkItemURI = getWorkItemUri(command, workItem, logger);

//		IWorkItemReferences targetReferences = getWorkItemTargetReferences(command, workItem, logger);

				if (reference.getLink().getLinkTypeId()
						.equals("com.ibm.team.workitem.linktype.qm.relatedExecutionRecord")) {
					getTracingLog().debug(
							" Backlink verification of link type Related Test Execution Record (\"com.ibm.team.workitem.linktype.qm.relatedExecutionRecord\") is not yet supported.");
				}

				// Fetch and validate the value
				if (reference.isURIReference() && !EXCLUDED_TYPES.containsKey(reference.getLink().getLinkTypeId())
						&& !isSelfReference(reference, workItem)) {
					try {
						GetRDFResourceParams oslcResource = new GetRDFResourceParams();
						oslcResource.oslcCoreVersion = "2.0";
						oslcResource.resourceURL = reference.getLink().getTargetRef().createURI().toString();
						oslcResource.oslcResourceID = reference.getLink().getTargetRef().createURI().toString(); 
						getTracingLog().debug("\n... checking link in work item: " + workItem.getId() + 
								" |endPoint id: " + endPointDescriptor.getId()
								+ " |linkType: " + reference.getLink().getLinkTypeId() 
								+ "\n... fetching target: "
								+ oslcResource.resourceURL);
						String gcUri=null; //test
						if ((gcUri == null // always
								|| workItem.getHTMLSummary().getPlainText().toLowerCase().indexOf(">testrdf") != -1)
								&& workItem.getHTMLSummary().getPlainText().toLowerCase().indexOf(">testldx") == -1) { // !summary includes >testldx
//							getTracingLog().trace("\n... Loading RDF resource: " + oslcResource );

							ResourceResultDTO result = getRDFResource(command, oslcResource, /* gcUri */ null); // Read result
							
							boolean isValid = false;
							if (result != null && !result.isSetErrorMessage()) {
								isValid = validateRDFBackLink(endPointDescriptor, result, currentWorkItemURI,
										oslcResource, workItem); // log validation
								backLinkMissing = backLinkMissing || !isValid;
//								backLinkMessage += "\n RDF Linked resource " + oslcResource.resourceURL + " isValid:"
//										+ isValid + " error:" + result.getErrorMessage();
							} else {
//								backLinkMessage += "\n***RDF Linked resource " + oslcResource.resourceURL
//										+ " is unreachable.";
//								backLinkUnreachable = true;
							}
						} else { // never... use REST
							boolean wasFound = false;
							String targetLinkType = OSLC_TYPE_MAP.get(reference.getLink().getLinkTypeId());
							if ((isLocalLink(reference.getLink().getTargetRef().createURI()) || workItem
									.getHTMLSummary().getPlainText().toLowerCase().indexOf(">testldxlocal") != -1) // Test																												
									&& workItem.getHTMLSummary().getPlainText().toLowerCase()
											.indexOf(">testldxrest") == -1) { // Test, skip service use REST
								getTracingLog().debug("\n... Reading local link index for work item: "
										+ workItem.getId() + " |GC: "
										+ (gcUri == null ? "[None]"
												: gcUri + " |endPoint id: " + endPointDescriptor.getId() + " |linkType: "
														+ reference.getLink().getLinkTypeId() + " (maps to: "
														+ targetLinkType + ") |target: " + oslcResource.resourceURL));

								if (!wasFound || (workItem.getHTMLSummary().getPlainText().toLowerCase()
										.indexOf(">testldxrest") != -1)) {
									getTracingLog().debug("\n... Reading REST link index for Item: " + workItem.getId()
											+ " |GC: "
											+ (gcUri == null ? "[None]"
													: gcUri + " |endPoint: " + endPointDescriptor.getId()
															+ " |linkType: " + reference.getLink().getLinkTypeId()
															+ " |maps to: " + targetLinkType + " |target: "
															+ oslcResource.resourceURL));
									
									JSONArray ldxBacklinks = command.getLDXBackLinkViaRest(
											reference.getLink().getTargetRef().createURI(), gcUri == null ? "" : gcUri,
											targetLinkType);
									
									if (ldxBacklinks == null) {
										getTracingLog().trace("\n... *** Could not parse result.");
									} else {
										for (int i = 0; i < ldxBacklinks.size(); i++) {
											JSONObject triple = (JSONObject) ldxBacklinks.get(i);
											wasFound = command.isLinkInTripleEqual(triple,
													currentWorkItemURI.toString(), targetLinkType,
													oslcResource.resourceURL); // TODO: Check link parameters?
											if (wasFound) {
												getTracingLog().debug("\n+ OK. Found LDX (REST) backlink to Work item: "
														+ workItem.getId() + " |source: " + triple.get("sourceURL")
														+ " |linkType: " + triple.get("linkType") + " |Target:"
														+ triple.get("targetURL"));
											} else {
												getTracingLog().trace("\n... Skipping LDX link with source: "
														+ triple.get("sourceURL") + " |linkType: "
														+ triple.get("linkType") + " |Target: "
														+ triple.get("targetURL"));
											}
										}
										if (!wasFound) {
											getTracingLog().warn("\n*** Warning *** Backlink not found for linkType: "
													+ reference.getLink().getLinkTypeId() + " |target: "
													+ reference.getLink().getTargetRef().createURI().toString()
													+ "|gc: " + gcUri);
										}
									}
								}
								if (!wasFound) {
									backLinkMissing = true;
								}
							} // end LDX Service plus REST
						}
					} catch (Exception e) {
						getTracingLog().error("-Error 1:" + e + "\n Exception reading link: "
								+ endPointDescriptor.getId() + " message: " + e.getMessage());
	//					backLinkUnreachable = true;
					}
				} else { // not applicable or excluded
					getTracingLog().debug("\n... Excluding endPoint id: " + endPointDescriptor.getId() + " |linkType: "
							+ reference.getLink().getLinkTypeId() + " |isUri: " + reference.isURIReference()
							+ " |isExcluded: " + !EXCLUDED_TYPES.containsKey(reference.getLink().getLinkTypeId())
							+ " |isSelfReference: " + isSelfReference(reference, workItem));
				}
//			}
//
//		}
	}


	
	private URI getWorkItemUri(ValidateOSLCLinksCommand command, IWorkItem workItem, Logger logger) {
		// ItemURI.createWorkItemURI(workItemService.getAuditableCommon(),
		// workItem.getId());
		URI currentWorkItemURI = ItemURI.createWorkItemURI(command.getAuditableCommon(), workItem.getId());

		return currentWorkItemURI; 
	}

	private IWorkItemReferences getWorkItemTargetReferences(ValidateOSLCLinksCommand command, IWorkItem workItem, Logger logger) throws TeamRepositoryException {
		return command.getWorkItemService().resolveWorkItemReferences(workItem, command.getMonitor()); 
	}

//	private String getConfigurationManagementUriByLinkType(IEndPointDescriptor endPointDescriptor, IWorkItem workItem,
//			Logger logger) {
//		String gcUri= workItemService.getConfigurationManagementUriByLinkType(
//				workItem.getProjectArea().getItemId().getUuidValue(), 
//				workItem.getItemHandle().getItemId().getUuidValue(), 
//				endPointDescriptor.getLinkType().getLinkTypeId() );
//
//		return null; // TODO:
//	}

	private String getItemUri(IWorkItem workItem) {
//		String thisItemUri= workItemService.getAuditableCommon().getPublicRepositoryURI() + "resource/itemName/com.ibm.team.workitem.WorkItem/" +  workItem.getId(); 

		return null; // TODO:
	}

	private String getLocalUri() {
		// workItemService.getAuditableServer().getRepositoryURI();

		return null; // TODO:
	}

	private boolean isSelfReference(IReference reference, IWorkItem workItem)
			throws MalformedURLException, TeamRepositoryException {
		URI linkUri = reference.getLink().getTargetRef().createURI();
		String link = "";
		if (linkUri.isAbsolute()) {
			link = linkUri.toURL().toString();
			String thisItemUri = getItemUri(workItem); // workItemService.getAuditableCommon().getPublicRepositoryURI()
														// + "resource/itemName/com.ibm.team.workitem.WorkItem/" +
														// workItem.getId();
			return link.equals(thisItemUri);
		} // relative

		return linkUri.toString().indexOf(workItem.getItemId().toString()) == -1;
//		https://m4:9443/jazz/resource/itemName/com.ibm.team.workitem.WorkItem/179
	}

	public Logger fTracingLog;
	public final Logger getTracingLog() {
//		return LogFactory.getLog("com.ibm.team.workitem.verify.backlinks.log");
		return fTracingLog;
	}
	public void setTracingLog(Logger log) {
		fTracingLog= log;
	}

	private boolean isLocalLink(URI uri) throws MalformedURLException, URISyntaxException {

		String localUri = getLocalUri();// workItemService.getAuditableServer().getRepositoryURI();
		String path = URIUtil.fromString(localUri).getPath();
		localUri = localUri.replace(path, "");
		getTracingLog().trace("\n... is local uri: " + uri.toURL().toString() + " starts with: " + localUri + " = "
				+ uri.toURL().toString().startsWith(localUri));
		if (uri.toURL().toString().startsWith(localUri))
			return true;
		return false;
	}

	// Do-over with LinkTriple
	@SuppressWarnings("unused")
	private boolean isLinkInTripleEqual(LinkTriple triple, String sourceURL, String linkType, String targetURL) {
		if (triple == null) {
			getTracingLog().trace("\n..... null triple"); // should not happen
			return false;
		}
		getTracingLog().trace("\n..... LinkTriple is triple equal source:"
				+ (triple.sourceURL.equals(sourceURL) && triple.linkType.equals(linkType)
						&& triple.targetURL.equals(targetURL))
				+ "\nsource: " + triple.sourceURL + " = " + sourceURL + " : " + triple.sourceURL.equals(sourceURL)
				+ "\nlinkType: " + triple.linkType + " = " + linkType + " : " + triple.linkType.equals(linkType)
				+ "\nTarget: " + triple.targetURL + " = " + targetURL + " : " + triple.targetURL.equals(targetURL));

		return triple.sourceURL.equals(sourceURL) && triple.linkType.equals(linkType)
				&& triple.targetURL.equals(targetURL);
	}

	// Non-gc path
	public com.ibm.team.calm.foundation.common.internal.rest.dto.ResourceResultDTO getRDFResource(
			ValidateOSLCLinksCommand command, GetRDFResourceParams oslcResource, String gcUri)
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
		IRawRestClientConnection connection= restClient.getConnection(new URI(oslcResource.resourceURL));

		if (oslcCoreVersion != null) {
			connection.addRequestHeader(OSLCCoreIdentifiers.OSLC_CORE_VERSION, oslcCoreVersion);
		}
		connection.addRequestHeader(HttpUtil.ACCEPT, "application/rdf+xml");

		try {
			com.ibm.team.repository.transport.client.ITeamRawRestServiceClient.IRawRestClientConnection.Response response = connection.doGet();
//			OSLCResource resource = OSLCResource.loadResource(httpClient, oslcResource.resourceURL, null,
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

	/*
	 * Read the links then load the oslc back link. See that the link points back to
	 * the item Reverse link format
	 * <oslc_cm:trackedWorkItem>https://m4:9443/jazz/resource/itemName/com.ibm.team.
	 * workitem.WorkItem/175</oslc_cm:trackedWorkItem>
	 */
	private static ArrayList<String> fMissingEndpointList = new ArrayList<String>();

	private boolean validateRDFBackLink(IEndPointDescriptor targetEndPoint, ResourceResultDTO targetDTO, URI sourceUri,
			GetRDFResourceParams resource, IWorkItem workItem) {
		String sourceLinkTypeId = targetEndPoint.getLinkType().getLinkTypeId();
		String targetOslcLinkTypeId = REVERSE_OSLC_MAP.get(sourceLinkTypeId);
		getTracingLog().debug(
				"\n... checking OSLC response for backlink with sourceLinkType: " + sourceLinkTypeId + " |targetLinkType: " + targetOslcLinkTypeId);
		if (targetOslcLinkTypeId == null) {
			if (fMissingEndpointList.contains(sourceLinkTypeId)) { // Only report missing tags once
				getTracingLog().debug("\n Missing OSLC endpoint mapping for: " + sourceLinkTypeId
						+ ". Nothing was checked for this link type.");
//				getTracingLog().debug("\n Missing OSLC endpoint mapping for: " + sourceLinkTypeId + ". Nothing was checked for this link type.\n" + targetDTO.getContent() );
//					fMissingEndpointList.add(sourceLinkTypeId);
			}
			return true;
		}
		boolean backlinkFound = false;
		String backlinkMessage = "";
		// Parse the RDF and find the backlink
		try {
			Model model = ModelFactory.createDefaultModel()
					.read(new ByteArrayInputStream(targetDTO.getContent().getBytes()), null);

			StmtIterator statementList = model.listStatements();
			String itemTitle= "";
			String itemShortId= "";
			while (statementList.hasNext()) {
				Statement statement = statementList.next();
				Triple trip = statement.asTriple();
				String linkType = trip.getPredicate().getURI();
				if (linkType.indexOf("http://purl.org/dc/terms/title") != -1) {
					itemTitle= (String)trip.getObject().getLiteralValue();
				} else {
					if (linkType.indexOf("shortIdentifier") != -1) {
						itemShortId=  (String)trip.getObject().getLiteralValue();
					}
				}
				
				if (linkType != null && linkType.indexOf(targetOslcLinkTypeId) != -1 && (trip.getObject().isLiteral()
						&& trip.getObject().getLiteralValue().equals(sourceUri.toString()))) {
					backlinkFound = true;
					backlinkMessage = "Backlink verified: " + trip.getPredicate() + "   \n    Source: " + trip.getObject().toString()
							+ " \n    Target: " + trip.getSubject().toString();
				} else {
					// ignore not a link or not this link type
				}
			}
			if (!backlinkFound) {
				backlinkMessage= "*** Backlink not found: " + targetOslcLinkTypeId + " Source: "
						+ sourceUri.toString() + " TargetDTO:\n" + targetDTO.getContent();
			} else {
				backlinkMessage= "Found linked item " + itemShortId + ": " + itemTitle + "\n" + backlinkMessage;				
			}
		} catch (JenaException ex) {
			getTracingLog().error("\nError parsing backlink RDF: " + ex + "\n" + ex.getMessage() + " Content\n" + new String(targetDTO.getContent().getBytes()));
		}
		if (backlinkFound) {
			getTracingLog().debug("\n+ OK. Found RDF backlink to work item: " + workItem.getId() + " |source: "
					+ resource.resourceURL + " |reverse of linkType: " + targetEndPoint.getDisplayName() + " |to: "
					+ sourceUri + "\n... " + backlinkMessage);
		} else {
			getTracingLog().warn("\n*** Warning: backlink not found in work item " + sourceUri + " |target: "
					+ targetEndPoint.getDisplayName() + "\n" + backlinkMessage);
		}
		return backlinkFound;
	}

	/**
	 * Validate that the GC link is known by the target server and that the artifact
	 * exists.
	 * 
	 * @param gcUriString
	 * @param workItemId
	 * @param currentWorkItemURI
	 * @param reference
	 * @throws NotLoggedInException
	 * @throws IOException
	 * @throws TeamRepositoryException
	 * @throws URISyntaxException
	 */
//	private void validateGCLink1(String gcUriString, int workItemId, URI currentWorkItemURI, IReference reference)
//			throws NotLoggedInException, IOException, TeamRepositoryException, URISyntaxException {
//		URI gcUri = null;
//		try {
//			gcUri = new URI(gcUriString);
//		} catch (URISyntaxException e) {
//			gcUri = null;
//			e.printStackTrace();
//		}
//		String targetLinkType = OSLC_TYPE_MAP.get(reference.getLink().getLinkTypeId()).linkType;
//		JSONArray ldxBacklinks = getLDXBackLinkViaRest(reference.getLink().getTargetRef().createURI(),
//				gcUri == null ? "" : gcUri.getPath(), targetLinkType);
//		String resourceURL = reference.getLink().getTargetRef().createURI().toString();
//		boolean wasFound = false;
//		String targetURL = null;
//		for (int i = 0; i < ldxBacklinks.size(); i++) {
//			JSONObject triple = (JSONObject) ldxBacklinks.get(i);
//			wasFound = isLinkInTripleEqual(triple, currentWorkItemURI.toString(), targetLinkType, resourceURL); // TODO:
//																												// Check
//																												// link
//																												// parameters?
//			if (wasFound) {
//				logger.trace(
//						"\n> Found LDX (REST) link. Work item: " + workItemId + " |source: " + triple.get("sourceURL")
//								+ " |linkType: " + triple.get("linkType") + " |Target:" + triple.get("targetURL"));
//				targetURL = (String) triple.get("targetURL");
//				break;
//			} else {
//				logger.trace("\n... Skipping LDX link with source: " + triple.get("sourceURL") + " |linkType: "
//						+ triple.get("linkType") + " |Target:" + triple.get("targetURL"));
//			}
//		}
//		if (!wasFound) {
//			System.out.println("\n### Warning ### Backlink not found for workitem: " + workItemId + " linkType: "
//					+ reference.getLink().getLinkTypeId() + " |target: "
//					+ reference.getLink().getTargetRef().createURI().toString() + "|gc : " + gcUri.getPath());
//			System.out.println("\tIf link was just created wait a few minutes for the link to propagate.");
//		} else if (!validGCTargetURL(targetURL, gcUriString,
//				OSLC_TYPE_MAP.get(reference.getLink().getLinkTypeId()).targetSystemType)) {
//			System.out.println("\n### Warning ### Broken link for workitem: " + workItemId + " linkType: "
//					+ reference.getLink().getLinkTypeId() + " | target: "
//					+ reference.getLink().getTargetRef().createURI().toString() + " | gc : " + gcUri.getPath());
//
//		}
//	}

}
