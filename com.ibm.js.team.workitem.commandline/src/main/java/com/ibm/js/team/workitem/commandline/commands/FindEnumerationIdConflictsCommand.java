/*******************************************************************************
 * Copyright (c) 2019-2022 IBM
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.commands;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.AbstractTeamRepositoryCommand;
import com.ibm.js.team.workitem.commandline.framework.IWorkItemCommand;
import com.ibm.js.team.workitem.commandline.helper.ProcessAreaOslcHelper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.team.calm.foundation.common.SecureDocumentBuilderFactory;
import com.ibm.team.calm.foundation.common.internal.rest.dto.ResourceResultDTO;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.transport.HttpUtil.CharsetEncoding;
import com.ibm.team.workitem.client.IWorkItemClient;

/**
 * This command checks either all project area or specified project areas to see
 * if the project contains an Enumeration type or EnumerationList type (or other
 * types) id strings which are the same as existing internal attributes such as
 * tag, tags, and so forth. The command generates a report of the project areas.
 * Each project area must be manually modified by the administrator to change
 * the reference.
 */
public class FindEnumerationIdConflictsCommand extends AbstractTeamRepositoryCommand implements IWorkItemCommand {

	private Logger logger = LogManager.getLogger(FindEnumerationIdConflictsCommand.class);
	// Parameter to specify the query
	private static final String SWITCH_TRACE = "trace";
	private static final String SWITCH_DEBUG = "debug";
	private static final String SWITCH_SERVICES = "showservices";
	private static final String SWITCH_CATALOG = "showcatalog";
	private static final String SWITCH_PROJECTS = "showprojects";
	private static final String SWITCH_VERBOSE = "verbose";
	private boolean isShowProjects= false;
	private boolean isShowCatalog= false;
	private boolean isVerbose= false;
	
	public IWorkItemClient workItemClient;

	public static final class GetRDFResourceParams {
		public String resourceURL;
		public String oslcCoreVersion;
		public String oslcResourceID;
	}
	
	public static final String[] RESERVED_ID_LIST= new String[] {
			"smallString",
			"mediumString",
			"string",
			"mediumHtml",
			"html",
			"timestamp",
			"projectArea",
			"teamArea",
			"processArea",
			"category",
			"property",
			"interval",
			"contributor",
			"integer",
			"float",
			"decimal",
			"deliverable",
			"long",
			"boolean",
			"comments",
			"subscriptions",
			"approvalDescriptors",
			"approvals",
			"workItem",
			"timeSheet",
			"timeSheetEntry",
			"tags",
			"tag",
			"type",
			"customAttribute",
			"approvalState",
			"approvalType",
			"duration",
			"content",
			"fileSize",
			"uuid",
			"stateTransitions",
			"timeline",
			"resourceShape",
			"propertyShape",
			"eSignatureRecords",
			"reference",
			"workItemList",
			"contributorList",
			"processAreaList",
			"projectAreaList",
			"teamAreaList",
			"eSignature",
			"wiki",
			"item",
			"itemList",
			"progress",
			"schedule",
			"stringList"		
	};

	/**
	 * @param parameterManager
	 */
	public FindEnumerationIdConflictsCommand(ParameterManager parameterManager) {
		super(parameterManager);
	}

	@Override
	public String getCommandName() {
		return IWorkItemCommandLineConstants.COMMAND_FIND_ID_CONFLICTS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractWorkItemCommand
	 * #setRequiredParameters()
	 */
	public void setRequiredParameters() {
		// Add the parameters required to perform the operation
		getParameterManager().syntaxAddRequiredParameter(
				IWorkItemCommandLineConstants.PARAMETER_REPOSITORY_URL_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_REPOSITORY_URL_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddRequiredParameter(IWorkItemCommandLineConstants.PARAMETER_PASSWORD_FILE_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_PASSWORD_FILE_PROPERTY_EXAMPLE);
		if(isSearchEnable()) {
			getParameterManager().syntaxAddRequiredParameter(
					IWorkItemCommandLineConstants.PARAMETER_SEARCH_STRING,
					IWorkItemCommandLineConstants.PARAMETER_SEARCH_STRING_EXAMPLE);
				
		}

		getParameterManager().syntaxAddSwitch(SWITCH_TRACE);
		getParameterManager().syntaxAddSwitch(SWITCH_DEBUG);
		getParameterManager().syntaxAddSwitch(SWITCH_SERVICES);
		getParameterManager().syntaxAddSwitch(SWITCH_PROJECTS);
		getParameterManager().syntaxAddSwitch(SWITCH_CATALOG);
		getParameterManager().syntaxAddSwitch(SWITCH_VERBOSE);
	}

	@Override
	public String helpSpecificUsage() {
		return "" 
				+ IWorkItemCommandLineConstants.PARAMETER_REPOSITORY_URL_PROPERTY
				+ IWorkItemCommandLineConstants.PARAMETER_PASSWORD_FILE_PROPERTY  + "["
				+ IWorkItemCommandLineConstants.PARAMETER_SEARCH_STRING  + "]" + " ["
				+ SWITCH_TRACE + "]" + " ["
				+ SWITCH_DEBUG + "]" + " ["
				+ SWITCH_SERVICES + "]" + " ["
				+ SWITCH_PROJECTS + "]" + " ["
				+ SWITCH_CATALOG + "]" + " ["
				+ SWITCH_VERBOSE + "]"
				;
	}
	
	protected boolean isSearchEnable() {
		return false;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractWorkItemCommand
	 * #process()
	 */
	@Override
	public OperationResult process() throws TeamRepositoryException {

		if (getParameterManager().hasSwitch(SWITCH_PROJECTS)) {
			isShowProjects= true;
		}
		if (getParameterManager().hasSwitch(SWITCH_CATALOG)) {
			isShowCatalog= true;
		}
		if (getParameterManager().hasSwitch(SWITCH_VERBOSE)) {
			isVerbose= true;
		}
		
		String repositoryUrl = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_REPOSITORY_URL_PROPERTY);
		URI repositoryURI = null;

	
		try {
			repositoryURI = new URI(repositoryUrl);
		} catch (Exception e) {
			logger.error("Error constructing repository uri: " + repositoryUrl);
		}

		try {
			// Get all project area names
			ProcessAreaOslcHelper projectAreaHelper = new ProcessAreaOslcHelper(logger);
			ResourceResultDTO projectAreas = projectAreaHelper.fetchProjectAreaCatalog(this, repositoryURI);
			
			if (projectAreas == null) {
				logger.error("Empty project areas fetched from: " + repositoryUrl);
				throw new TeamRepositoryException("Empty project area list fetched from: " + repositoryUrl);
			}
			
			String catContent = projectAreas.getContent();
			if (catContent == null) {
				logger.error("Empty content in project areas fetched from: " + repositoryUrl);
				throw new TeamRepositoryException("Empty content in project areas fetched from: " + repositoryUrl);
			}

			if (isShowCatalog || isVerbose) {
			try {
				logger.warn("Project Area catalog: \n" + StringEscapeUtils.unescapeXml(catContent));
			} catch (Exception e) {
				logger.warn("Could not parse catalog for " + repositoryUrl + "\n" + catContent);
			}

			}
			
			
			DocumentBuilderFactory factory = SecureDocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);

			Document document = null;
			try {
// root				
//				<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
//						xmlns:dcterms="http://purl.org/dc/terms/" 
//						xmlns:oslc="http://open-services.net/ns/core#" 
//						xmlns:jfs_proc="http://jazz.net/xmlns/prod/jazz/process/1.0/">			
				
				DocumentBuilder documentBuilder = factory.newDocumentBuilder();
				byte[] bytes = catContent.getBytes(CharsetEncoding.UTF8.toCharset().name());
				document = documentBuilder.parse(new ByteArrayInputStream(bytes));
			} catch (ParserConfigurationException ex) {
				logger.error("Parse exception: " + ex + " " + ex.getMessage());
				throw new IOException(ex.getMessage());
			} catch (SAXException ex) {
				logger.error("SAX exception: " + ex +ex.getMessage());
				throw new IOException(ex.getMessage());
			} catch (Exception ex) {
				logger.error("Unknown exception: " + ex + " " + ex.getMessage());
				throw new TeamRepositoryException(ex.getMessage());
			}

			Node rdfDocument = document.getFirstChild();
//			<oslc:ServiceProviderCatalog rdf:about="https://tapcalmdev1.rtp.raleigh.ibm.com:9443/ccm/oslc/workitems/catalog">
//			<dcterms:title rdf:parseType="Literal">Project Areas</dcterms:title>
//			or
//			<oslc:serviceProvider rdf:about="https://ibmclm.kp.org:443/ccm/oslc/contexts/_vK5QgFLdEeeuuNFAeoyjPw/workitems/services.xml">
			if (rdfDocument == null) {
				logger.error("Catalog document has no RDF Document child node");
				throw new TeamRepositoryException("Catalog document has no child node");
			}
			if (!rdfDocument.getNodeName().equals("rdf:Description")) {
				logger.error("Catalog is not an RDF document");
				throw new TeamRepositoryException("Catalog is not an RDF document");
			}

			// If the tag is oslc:ServiceProviderCatalog then continue with full format
			// If the tag is oslc:serviceProvider then handle as compact format
//			if (rdfDocument.getNodeName().equals("oslc:serviceProvider")) {
//				// Document contains list of oslc:serviceProvider
//				NodeList projectAreaNodes = rdfDocument.getChildNodes();
//				for (int x = 0; x < projectAreaNodes.getLength(); x++) { // for Service Provider Caloag
//					Node paNode = projectAreaNodes.item(x);
//					if (paNode.getAttributes() == null)
//						continue;
////					processProjectAreaNode(paNode);
//				}
//			} else { // oslc:ServiceProviderCatalog ... children are oslc:serviceProvider with child oslc:ServiceProvider = PA
//				// Continue and parse full format
				NodeList rdfNodes = rdfDocument.getChildNodes();
				if (rdfNodes == null) {
					logger.error("Catalog RDF document has no child node.");
					throw new TeamRepositoryException("Catalog RDF document has no child nodes");
				}
///				<oslc:serviceProvider>
///				<oslc:ServiceProvider rdf:about="https://m4:9443/ccm/oslc/contexts/_JX_LkG4sEeqzGr2AWIpubw/workitems/services.xml">
				for (int x = 0; x < rdfNodes.getLength(); x++) { 
					Node spNode = rdfNodes.item(x); 
					if (spNode.getAttributes() == null || !spNode.getNodeName().contentEquals("oslc:serviceProvider"))
						continue;
					// find the /services.xml file for the project area
					Node spContext = spNode.getAttributes().getNamedItem("rdf:about"); // find the url
					if (spContext == null) continue;
					String url = spContext.getNodeValue();
					String projectAreaId = url.split("/contexts/")[1].split("/")[0];
					String projectAreaName= "";
					String projectUrl= "";
	
					NodeList spChildNodes= spNode.getChildNodes();
					if (spChildNodes == null) {
						continue;
					}
					for (int m = 0; m < spChildNodes.getLength(); m++) { // find the project area name and configuration.xml url
						Node childNode= spChildNodes.item(m);
						if (childNode == null) {
							continue;
						}
						
						String childValue= childNode.getNodeName();
						if (childValue == null || childValue=="#text") {
							continue;
						}
						if (childValue.indexOf("dcterms:title") != -1) {
							projectAreaName= childNode.getFirstChild().getNodeValue();
//							logger.trace("Found project area: " + (projectAreaName == null ? "Unknown" : projectAreaName));
						}
						if (childValue.indexOf("oslc:details") != -1) {
							projectUrl= childNode.getFirstChild().getNodeValue();
//							logger.trace("Found project  url: " + (projectUrl == null ? "Unknown" : projectUrl));
						}
					}
	
					logger.info("Searching \"" + (projectAreaName == null ? "Unknown" : projectAreaName) + "\" [ " + projectAreaId + " ] " + url);
					
					if (projectAreaId == null || repositoryURI == null) {
						logger.error("Null project areaId or repository uri found. Skipping.");
						continue;
					}
					
					String projectAreaConfiguration = projectAreaHelper.fetchProjectAreaConfigurationXml(this, factory,
							projectAreaId, repositoryURI, (isVerbose || isShowProjects));
	
					if (isShowProjects || isVerbose) {
						try {
						logger.info("Project Area content for: " + projectAreaName + "\n" + StringEscapeUtils.unescapeXml(projectAreaConfiguration));
						} catch (Exception e) {
							logger.warn("Could not parse content for " + projectAreaName  + "\n" + projectAreaConfiguration);
						}
					}
					
					if (projectAreaConfiguration == null || projectAreaConfiguration.length() == 0) {
						logger.info("\nOK. No project area process configuration history changes found for: " + projectAreaName + " [ " +  projectAreaId + " ] " + projectUrl + " \n" + projectAreaConfiguration) ;
					} else {
						if (!this.isSearchEnable()) {	// find ids
							boolean found=false;
							for (int s=0;s<RESERVED_ID_LIST.length;s++) { 
								String test= RESERVED_ID_LIST[s];
								int index=projectAreaConfiguration.indexOf("attributeTypeId=\"" + test + "\"");
								if (index != -1) {
									found= true;
									logger.warn("\n*** Reserved attributeTypeId=\"" + test + "\" found in project: " + projectAreaName + " [ " +  projectAreaId + " ] " + projectUrl + " ") ;
									String line = projectAreaConfiguration.substring(index -43, index + 180);
									line= StringEscapeUtils.unescapeXml(line);
									logger.warn("\n "+ line);
								}
							}
							if (!found) {
								logger.info("OK. No reserved attributeTypeId's found found in project: " + projectAreaName + " [ " +  projectAreaId + " ] " + projectUrl) ;
							}
						} else { // search						
							String searchString = getParameterManager()
									.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_SEARCH_STRING);
							int index=projectAreaConfiguration.indexOf(searchString);

							if (index != -1) {
								logger.warn("\n*** Search String \"" + searchString + "\" found in project: " + projectAreaName + " [ " +  projectAreaId + " ] " + projectUrl) ;
								String line = projectAreaConfiguration.substring(index -43, index + 180);
								line= StringEscapeUtils.unescapeXml(line);
								logger.warn("\n "+ line);
							} else {
								logger.info("Not found. Search String \"" + searchString + "\" not found found in project: " + projectAreaName + " [ " +  projectAreaId + " ] " + projectUrl) ;
							}
						}
					}
	
				}
//			}
			} catch (Exception e) {
				logger.error("Unknown exception: " + e + " " + e.getMessage());
				throw new TeamRepositoryException(e.getMessage());
			}


		setSuccess();
		return getResult();
	}

//	public static final String PROJECT_AREA_HISTORY_URI_TEMPLATE = "/process/project-areas/$1/history?pageSize=1";
	public static final String PROJECT_AREA_HISTORY_URI_TEMPLATE = "/process/project-areas/$1/history";

	
}