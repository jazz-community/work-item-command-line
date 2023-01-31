/*******************************************************************************
 * Copyright (c) 2019-2022 IBM
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.commands;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.AbstractTeamRepositoryCommand;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.helper.RepositoryStateModel;
import com.ibm.js.team.workitem.commandline.helper.WorkItemLocalLinkHelper;
import com.ibm.js.team.workitem.commandline.helper.WorkItemStateHelper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.js.team.workitem.commandline.utils.ProcessAreaUtil;
import com.ibm.js.team.workitem.commandline.utils.QueryUtil;
import com.ibm.js.team.workitem.commandline.utils.WorkItemUtil;
import com.ibm.team.links.common.IItemReference;
import com.ibm.team.links.common.ILink;
import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.registry.ILinkType;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.IItemHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.query.IQueryDescriptor;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResult;
import com.ibm.team.workitem.common.query.ResultSize;

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
public class ValidateWorkItemStatesCommand extends AbstractTeamRepositoryCommand {
	public Logger logger = LogManager.getLogger(ValidateWorkItemStatesCommand.class);
	// Parameter to specify the query
	private static final String SWITCH_TRACE = "trace";
	private static final String SWITCH_DEBUG = "debug";
//	private static final String SWITCH_SERVICES = "showservices";
//	private static final String SWITCH_CATALOG = "showcatalog";
//	private static final String SWITCH_PROJECTS = "showprojects";
	private static final String SWITCH_VERBOSE = "verbose";
	private static final String SWITCH_LOCAL = "local";
	private static final String SWITCH_LINKS_ONLY = "linksOnly";
	private static final String SWITCH_STATES_ONLY = "statesOnly";
	private static final String SWITCH_TRIM_HTML = "trimHtml";
	private boolean isLocal= false;
	private boolean isLinksOnly= false;
	private boolean isStatesOnly= false;
	private boolean isVerbose= false;
	private boolean useRest= true;
	private boolean isTrimHtml= false;
	private int depth= 3;
	public String repositoryUrl;
	private String exportFolderPath;
	private String searchString; 
	
	
	// If there is no value export this
	public static final String CONSTANT_NO_VALUE = "[No content found]";

	// Parameter for the export file name
	private static final String PARAMETER_EXPORT_FOLDER = "exportFolder";
	private static final String PARAMETER_EXPORT_FOLDER_EXAMPLE = "\"C:\\temp\\\"";

	// Parameter for the export file name
	private static final String PARAMETER_DEPTH = "depth";
	private static final String PARAMETER_DEPTH_EXAMPLE = "3";

	
	// The output file
	private WorkItemStateHelper fWorkItemStateHelper;
	private WorkItemLocalLinkHelper fWorkItemLocalLinkHelper;

	
	/**
	 * The constructor
	 * 
	 * @param parametermanager
	 */
	public ValidateWorkItemStatesCommand(ParameterManager parametermanager) {
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
		return IWorkItemCommandLineConstants.COMMAND_VALIDATE_WORKITEM_STATES;
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
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.PARAMETER_PASSWORD_FILE_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_PASSWORD_FILE_PROPERTY_EXAMPLE);
		// Add the parameters required to perform the operation
		getParameterManager().syntaxAddRequiredParameter(
				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddSwitch(
				IWorkItemCommandLineConstants.PARAMETER_USER_ID_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_USER_ID_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddSwitch(PARAMETER_EXPORT_FOLDER, PARAMETER_EXPORT_FOLDER_EXAMPLE);
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.PARAMETER_WORKITEM_ID_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_WORKITEM_ID_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddSwitch(PARAMETER_DEPTH, PARAMETER_DEPTH_EXAMPLE);
		getParameterManager().syntaxAddSwitch(SWITCH_DEBUG);
		getParameterManager().syntaxAddSwitch(SWITCH_VERBOSE);
		getParameterManager().syntaxAddSwitch(SWITCH_TRACE);
		getParameterManager().syntaxAddSwitch(SWITCH_TRIM_HTML);
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.PARAMETER_QUERY_NAME,
				IWorkItemCommandLineConstants.PARAMETER_QUERY_NAME_EXAMPLE);
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.PARAMETER_PASSWORD_FILE_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_PASSWORD_FILE_PROPERTY_EXAMPLE);
		
		getParameterManager().syntaxAddSwitch(
				IWorkItemCommandLineConstants.PARAMETER_SEARCH_STRING,
				IWorkItemCommandLineConstants.PARAMETER_SEARCH_STRING_EXAMPLE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractCommand#
	 * helpSpecificUsage()
	 */
	@Override
	public String helpSpecificUsage() {
		return  IWorkItemCommandLineConstants.PARAMETER_REPOSITORY_URL_PROPERTY
				+ IWorkItemCommandLineConstants.PARAMETER_PASSWORD_FILE_PROPERTY  
				+ IWorkItemCommandLineConstants.PARAMETER_USER_ID_PROPERTY  
				+ IWorkItemCommandLineConstants.PARAMETER_PASSWORD_PROPERTY
				+ PARAMETER_EXPORT_FOLDER
				+ IWorkItemCommandLineConstants.PARAMETER_WORKITEM_ID_PROPERTY
				+ IWorkItemCommandLineConstants.PARAMETER_SEARCH_STRING;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractCommand#process()
	 */
	@Override
	public OperationResult process() throws TeamRepositoryException {

		if (getParameterManager().hasSwitch(SWITCH_LOCAL))
			isLocal= true;
		if (getParameterManager().hasSwitch(SWITCH_LINKS_ONLY))
			isLinksOnly= true;
		if (getParameterManager().hasSwitch(SWITCH_STATES_ONLY))
			isStatesOnly= true;
		
		if (getParameterManager().hasSwitch(SWITCH_TRIM_HTML))
			isTrimHtml= true;
		if (getParameterManager().hasSwitch(SWITCH_VERBOSE)) {
			isVerbose= true;
		}
		
		String depthString = getParameterManager()
				.consumeParameter(PARAMETER_DEPTH);
		if (depthString != null) {
			depthString.trim();
			if (depthString != null) {
				depth = Integer.parseInt(depthString);
			} 			
		}
		
//		String projectAreaName = getParameterManager()
//				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY).trim();
//		// Find the project area
//		IProjectArea projectArea = ProcessAreaUtil.findProjectAreaByFQN(projectAreaName, getProcessClientService(),
//				getMonitor());
//		if (projectArea == null) {
//			throw new WorkItemCommandLineException("Project Area not found: " + projectAreaName);
//		}
//		String queryName = getParameterManager().consumeParameter(IWorkItemCommandLineConstants.PARAMETER_QUERY_NAME);
//		if (queryName == null) {
//			throw new WorkItemCommandLineException("Query name must be provided.");
//		}
//		IQueryDescriptor query = getWorlkItemQuery(projectArea, queryName, sharingTargetNames);
		
		String workItemId = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_WORKITEM_ID_PROPERTY);
		if (workItemId != null) workItemId= workItemId.trim();
		
		String projectAreaName = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY).trim();
		
		String queryName= getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_QUERY_NAME);
		if (queryName != null) queryName= queryName.trim();
		
		searchString = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_SEARCH_STRING);
		if (searchString != null) {
			searchString= searchString.trim();
			System.out.println("Searching each state for: " + searchString);
		} else {
			System.out.println("Not searching");
		}
		
		exportFolderPath = getParameterManager().consumeParameter(PARAMETER_EXPORT_FOLDER);
 		if (exportFolderPath == null) {
 			System.out.println("No exportFolder provided. States will not be exported.");
// 			throw new WorkItemCommandLineException("Export folder path must be provided.");
 		}

		repositoryUrl = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_REPOSITORY_URL_PROPERTY);
		try {
			new URI(repositoryUrl);
		} catch (Exception e) {
			logger.error("Error constructing repository uri: " + repositoryUrl);
		}
		
		if (workItemId != null) {			
			validateStates(workItemId);
			validateLinks(workItemId);
		} else {
			if (queryName == null) {
	 			throw new WorkItemCommandLineException("WorkItemId or QueryId parameter must be provided.");
			} else {
				IProjectArea projectArea = ProcessAreaUtil.findProjectAreaByFQN(projectAreaName, getProcessClientService(), getMonitor());
				if (projectArea == null) {
					throw new WorkItemCommandLineException("Project Area not found: " + projectAreaName);
				}
				// OperationResult opResult = new OperationResult();
				IQueryDescriptor query = QueryUtil.findPersonalQuery(queryName, projectArea,
						getTeamRepository().loggedInContributor(), getMonitor());
				if (query == null) {
					throw new WorkItemCommandLineException("Query not found: " + queryName);
				}
				// Query the work items
				IQueryResult<IResult> results = QueryUtil.getUnresolvedQueryResult(query, true);

				ResultSize resultSize = results.getResultSize(getMonitor());
				List<IWorkItemHandle> workItems = new ArrayList<IWorkItemHandle>(resultSize.getTotal());
				while (results.hasNext(null)) {
					IResult result = results.next(null);
					workItems.add((IWorkItemHandle) result.getItem());
				}
				results= null; // clear list;

				for (IWorkItemHandle workItemHandle : workItems) {
					try { 
						logger.trace("Resolving work item handle: " + workItemHandle.getItemId().getUuidValue());
						IWorkItem workItem = WorkItemUtil.resolveWorkItem(workItemHandle, IWorkItem.FULL_PROFILE, getWorkItemCommon(),
								getMonitor());
						validateStates(""+ workItem.getId());
						validateLinks(workItem.getId() + "");
					} catch (Exception e) {
						logger.warn("Exception resolving work item handle: " + workItemHandle.getItemId().getUuidValue());
						if (logger.isTraceEnabled()) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		
		logger.trace("All state and link validations complete");
		
		return getResult();
	}

	public boolean isTrimHtml() {
		return isTrimHtml;
	}
	public String getExportFolderPath() {
		return exportFolderPath;
	}
	
	/**
	 * Export the data to a CSV file
	 * 
	 * @param filePath
	 * @param query
	 * @param columnHeaderMapping
	 * @return
	 * @throws TeamRepositoryException
	 */
	private void validateLinks(String workItemId)
			throws TeamRepositoryException {
		if (isStatesOnly) return;
		try {
			//logger.debug("Fetching linked items for work item " + workItemId + " in repository " + repositoryUrl);
			
			List<IReference> allReferenceLists=  getWorkItemLocalLinkHelper(workItemId).getAllReferences(getTeamRepository(), Integer.decode(workItemId).intValue());

			for (IReference ref : allReferenceLists) {
				if (ref.isItemReference() ) {
					ILinkType linkType= ref.getLink().getLinkType();
					URI uri= ref.createURI();
					ILink link= ref.getLink();
					logger.debug("\nValidating Link for work item: " + workItemId + " linktype:" + link.getLinkTypeId() 
					+ " s: " + link.getSourceRef().getComment() 
					+ " >> t: " + link.getTargetRef().getComment() + "[ " + link.getSourceRef() + " > " + link.getSourceRef() + " ]");

					if (link.getSourceRef().isItemReference()) {
						IItemHandle source= getItemHandle(link.getSourceRef());
						try {
							IWorkItem workItem= fetchWorkItem((IWorkItemHandle)source);

						} catch (Exception e) {
							logger.trace("Fetching linked items for work item " + workItemId + " in repository " + repositoryUrl + " ... Reading Work Item handle...");
							logger.error("\n*Exception fetching: " + source.getItemId().getUuidValue()
									+ e + "\n" 
									+ e.getMessage());
						}
					}
					if (link.getTargetRef().isItemReference()) {						
						IItemHandle target= getItemHandle(link.getTargetRef());
						try {
							IWorkItem workItem= fetchWorkItem((IWorkItemHandle)target);
						} catch (Exception e) {
							logger.trace("Fetching linked items for work item " + workItemId + " in repository " + repositoryUrl + " ... Reading Work Item handle...");
							logger.error("\n*Exception fetching: " + target.getItemId().getUuidValue()
									+ e + "\n" 
									+ e.getMessage());
						}
					}
// Code from WorkItemPostDeleteTask to delete the link when a work item is deleted. TODO: Delet					
//					IItemHandle srcItem= getItemHandle(cur.getSourceRef());
//					IItemHandle tgtItem= getItemHandle(cur.getTargetRef());
//					if (srcItem != null && tgtItem != null) {
//						linkClosureService.removeRelation(cur.getLinkTypeId(), srcItem.getItemId(), tgtItem.getItemId());
//					}
				}
			}
						
		} catch (Exception e) {
			logger.error("\n*** Exception validating states for " + workItemId + " in repository " + repositoryUrl + "\n");
			e.printStackTrace();
			setFailed();
			return;
		}
		setSuccess();

	}

	private IWorkItem fetchWorkItem(IWorkItemHandle handle) {
		IWorkItem workItem= null; 
		if (logger.isTraceEnabled()) {
			logger.debug("\n sFetching item: " +  handle.getItemId().getUuidValue());
		}
		try {
			workItem = WorkItemUtil.resolveWorkItem(handle, IWorkItem.FULL_PROFILE, getWorkItemCommon(),getMonitor());
		} catch (Exception e) {
			logger.error("\n*Exception* Fetching item: " + handle.getItemId().getUuidValue() + "\n" 
					+ e + "\n"
					+ e.getMessage());

		}
		if (logger.isTraceEnabled()) {
			logger.trace("\nFetched item: " + handle.getItemId().getUuidValue() );
		}
		if (workItem == null) {
			logger.warn("\n*Missing linked item: " + handle.getItemId().getUuidValue() );
		}
		return workItem;
	}
	private IItemHandle getItemHandle(IReference ref) {
		if (ref != null && ref.isItemReference()) {
			return ((IItemReference)ref).getReferencedItem();
		}
		return null;
	}
	/**
	 * Export the data to a CSV file
	 * 
	 * @param filePath
	 * @param query
	 * @param columnHeaderMapping
	 * @return
	 * @throws TeamRepositoryException
	 */
	private void validateStates(String workItemId)
			throws TeamRepositoryException {
		if (isLinksOnly) return;

		try {
			logger.info("Fetching states for work item " + workItemId + " in repository " + repositoryUrl + " ... Reading Work Item handle...");
			List<RepositoryStateModel> stateList= getWorkItemStateList(workItemId);
			if (stateList == null) return;
			logger.info("... Fetched " + stateList.size() + " states... validating...");
			getWorkItemStateHelper(workItemId).validateWorkItemStates(this, stateList, searchString);
//			for (WorkItemStateModel stateModel: stateList) {
//			}
			logger.info("Complete.\n==== Validation complete for work item " + workItemId + " in repository " + repositoryUrl + "\n");
			
		} catch (Exception e) {
			logger.error("\n*** Exception validating states for " + workItemId + " in repository " + repositoryUrl + "\n");
			e.printStackTrace();
			setFailed();
			return;
		}
		setSuccess();

	}

	/**
	 */
	private List<RepositoryStateModel> getWorkItemStateList(String workItemId)
			throws TeamRepositoryException, WorkItemCommandLineException {
		IWorkItem workItemHandle = null;
		List<RepositoryStateModel> stateList= null;
		if (workItemId != null) {
			workItemHandle = WorkItemUtil.findWorkItemByID(workItemId, IWorkItem.SMALL_PROFILE, getWorkItemCommon(), getMonitor());
			if (workItemHandle == null) {
				logger.error("Work Item " + workItemId + " specified but not found.");
				return null;
			}
			logger.trace("Found Work Item handle for type: " + workItemHandle.getWorkItemType() + " ... fetching all state handles..." );
		}
		if (workItemHandle != null) {
			stateList= getWorkItemStateHelper(workItemId).fetchWorkItemStates(workItemId, workItemHandle, this); 
		}
		return stateList;
	}

	/**
	 * Get the getWorkItemExportHelper create it if it does not yet exist.
	 * 
	 * @return
	 */
	private WorkItemStateHelper getWorkItemStateHelper(String workItemId) {
		if (fWorkItemStateHelper == null) {
			fWorkItemStateHelper = new WorkItemStateHelper(getTeamRepository(), workItemId, getMonitor());
		}
		return fWorkItemStateHelper;
	}

	private WorkItemLocalLinkHelper getWorkItemLocalLinkHelper(String workItemId) {
		if (fWorkItemLocalLinkHelper == null) {
			fWorkItemLocalLinkHelper = new WorkItemLocalLinkHelper(getTeamRepository(), workItemId, getMonitor());
		}
		return fWorkItemLocalLinkHelper;
	}


	public int getDepth() {
		return depth;
	}

	public boolean isUseRest() {
		return useRest && !isLocal;
	}

	public void setUseRest(boolean useRest) {
		this.useRest = useRest;
	}
	
}
