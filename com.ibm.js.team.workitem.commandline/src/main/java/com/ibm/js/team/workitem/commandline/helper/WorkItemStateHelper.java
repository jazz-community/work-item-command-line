/*******************************************************************************
 * Copyright (c) 2019-2022 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.helper;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.js.team.workitem.commandline.commands.ValidateWorkItemStatesCommand;
import com.ibm.js.team.workitem.commandline.utils.AttachmentUtil;
import com.ibm.juno.core.utils.IOUtils;
import com.ibm.team.calm.foundation.common.SecureDocumentBuilderFactory;
import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.IAuditableHandle;
import com.ibm.team.repository.common.IItemType;
import com.ibm.team.repository.common.transport.HttpUtil;
import com.ibm.team.repository.transport.client.ITeamRawRestServiceClient;
import com.ibm.team.repository.transport.client.ITeamRawRestServiceClient.IRawRestClientConnection;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IAttributeHandle;
import com.ibm.team.workitem.common.model.IComments;
import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.Identifier;

/**
 * Helper handling of conversion of work item properties into strings.
 * 
 */
public class WorkItemStateHelper {

	// NewLine separator for lists in RTC compatible format
	public static final String SEPERATOR_NEWLINE = "\n";
	// The default separator for lists such as tags
	public static final String SEPERATOR_COMMA = ", ";
	// If there is no value export this
	public static final String CONSTANT_NO_VALUE = "";
	// prefix to be used when exporting work item ID's
	public static final String PREFIX_EXISTINGWORKITEM = "#";
	
	private IProgressMonitor fMonitor;
	private ITeamRepository fTeamRepository;
	private String fOutputFolder = null;
	private String fWorkItemId = "000";


	public WorkItemStateHelper(ITeamRepository fTeamRepository, String workItemId, IProgressMonitor fMonitor) {
		super();
		this.fWorkItemId= workItemId;
		this.fMonitor = fMonitor;
		this.fTeamRepository = fTeamRepository;
	}

	void initialize(ITeamRepository fTeamRepository, IProgressMonitor fMonitor) {
		this.fMonitor = fMonitor;
		this.fTeamRepository = fTeamRepository;
	}

	public String getOutputFolder() {
		return fOutputFolder;
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
	
	@SuppressWarnings("unchecked")
	public List<RepositoryStateModel> fetchWorkItemStates(String workItemId, IAuditableHandle workItemHandle, ValidateWorkItemStatesCommand command)  {
		List<RepositoryStateModel> result= new ArrayList<RepositoryStateModel>();

		try {
			List<IAuditableHandle> stateHandles= null;
			try {
				command.logger.trace("... Fetching all state handles via ItemManager API for " + workItemHandle.getItemId().getUuidValue());
				stateHandles= this.fTeamRepository.itemManager().fetchAllStateHandles(workItemHandle, this.fMonitor);
			} catch (Exception e) {
				command.logger.error("Error fetching state handles for work item handle:" + workItemHandle.getItemId().getUuidValue());
				e.printStackTrace();
				return null;
			}
			// For each state create a state model
			// For each state fetch the raw xml from repodebug and log it... the api is used later to read the state as well but the api can fail more
			if (command.isUseRest()) {
				command.logger.trace("... State handles fetched. For each handle, GET raw item state from repodebug REST...");
				for (IAuditableHandle stateHandle: stateHandles) {
					try {
						String stateId= stateHandle.getStateId().getUuidValue();
						RepositoryStateModel storedStateModel= new RepositoryStateModel(stateId, stateHandle); // no content
						if (command.logger.isTraceEnabled()) {
							getItemRawContentViaRest(command, stateId, true); // for logging
						}
						result.add(storedStateModel);
					} catch (Exception e) {
						command.setUseRest(false);
						command.logger.error("Exception fetching raw content from repodebug via REST GET. Enable REPODEBUG in Advanced Properties and ensure the user has JazzAdmin priveledges. Item will be read by Item API. REST attempts are disabled. item:" + workItemHandle.getItemId().getUuidValue());
						e.printStackTrace();
						break;
					}
				}
			}
			try {
				command.logger.trace("... Fetching all work item states via ItemManager API (fetchCompleteStates)...");

				List<String>allPredecessorIds= new ArrayList<String>();
//				List<IWorkItem>completeStates= null;
//				try {					
//					// completeStates uses a lot of memory and an exception in one state voids the read of all states
// exception		completeStates= (List<IWorkItem>)this.fTeamRepository.itemManager().fetchCompleteStates(stateHandles, this.fMonitor);
//				    validateStateList(result, completeStates, allPredecessorIds, command);
//				} catch (Exception e) {
//					e.printStackTrace();
//					command.logger.error(e.getMessage() + "\nError reading complete states. Reading individual states...");
//				}
				// Read each state one at a time and validate.
				for (IAuditableHandle stateHandle : stateHandles) {
					List<IWorkItem>oneStateList= null;

					List<IAuditableHandle> oneState= new ArrayList<IAuditableHandle>();
					oneState.add(stateHandle);
					command.logger.trace("... Fetching state: " + stateHandle.getStateId().getUuidValue() + " for item " + stateHandle.getItemId().getUuidValue()) ;
					try {							
						oneStateList = (List<IWorkItem>)this.fTeamRepository.itemManager().fetchCompleteStates(oneState, this.fMonitor);
					} catch (Exception ee) {
						ee.printStackTrace();
						command.logger.error("... Failed to load state: " + stateHandle.getItemId().getUuidValue());
					}
					if (oneStateList != null) {
						findPredecessorInStateList(result, oneStateList, allPredecessorIds, command);
					}
				}					

				// Search for the "current state" in the stateModel results
				for (RepositoryStateModel stateModel: result) {
					String stateId= stateModel.getStateId();
					boolean isCurrent= !allPredecessorIds.contains(stateId);
					stateModel.setIsCurrentState(isCurrent);
					if (isCurrent) {							
						command.logger.info("Current state is: " + stateId + " for  Work Item " +workItemHandle.getItemId().getUuidValue());
					}
					if (stateModel.getPredecessorStateId() == null) {							
						command.logger.info("Initial state is: " + stateId + " for  Work Item " +workItemHandle.getItemId().getUuidValue());
					}
					stateModel.setWorkItemId(workItemId);
				}
				
			} catch (Exception e) {
				command.logger.error("Error fetching complete states for work item: " + workItemHandle.getItemId().getUuidValue() + " handles:\n" + stateHandles);
				e.printStackTrace();
				return null;
			}
	
		} catch (Exception tre) {
			System.out.println("Exception reading states" + tre.getMessage());
			return null;
		}
		return result;
	}

	public List<RepositoryStateModel> findPredecessorInStateList(List<RepositoryStateModel> result, List<IWorkItem> stateList, List<String>allPredecessorIds, ValidateWorkItemStatesCommand command)  {
			boolean isStateFoundInResult=false;
			for (IWorkItem workItem : stateList) {
//					command.logger.trace("...... Fetched Work Item State:" + workItemStateId + "\nWork Item: " + workItem.toString().replaceAll("Proxy of", ""));
					String workItemStateId= workItem.getStateId().getUuidValue();			
					
					// Find the predecessorStateId if the state is already in the result
					for (RepositoryStateModel stateModel : result) {

						if (stateModel.getStateId().equals(workItemStateId)) {
							command.logger.trace("...... Formatting state: " + workItemStateId + "\nWork Item: " + simplifyTraceString(workItem.getFullState().toString()));
							stateModel.setWorkItem(workItem);
							if (workItem != null && workItem.getPredecessorState() != null || workItem.getMergePredecessorState() != null ) {								
								String predecessorStateId= workItem.getPredecessorState() != null ? workItem.getPredecessorState().getStateId().getUuidValue() :
									workItem.getMergePredecessorState() != null ? workItem.getMergePredecessorState().getStateId().getUuidValue() : "nil";
									stateModel.setPredecessorStateId(predecessorStateId);
									allPredecessorIds.add(predecessorStateId); // used to find first item
							}
							isStateFoundInResult=true;
//							stateModel.setItemContent(workItem.getFullState().toString()); // too much memory
							break;
						}
					}
					// Find the predecessorStateId if the state is not yet in the result... add to result
					if (!isStateFoundInResult) {
						command.logger.trace("...... Rendering state: " + workItemStateId + "\nWork Item: " + workItem.getFullState().toString());
						RepositoryStateModel stateModel= new RepositoryStateModel(workItemStateId, workItem);
						stateModel.setWorkItem(workItem);
						if (workItem != null) {								
							String predecessorStateId= workItem.getPredecessorState() != null ? workItem.getPredecessorState().getStateId().getUuidValue() :
								workItem.getMergePredecessorState() != null ? workItem.getMergePredecessorState().getStateId().getUuidValue() : "nil";
								stateModel.setPredecessorStateId(predecessorStateId);
								allPredecessorIds.add(predecessorStateId); // used to find first item
						}
//						stateModel.setItemContent(workItem.getFullState().toString()); // Too much memory
						result.add(stateModel);
					}
				}
		return result;
	}

	
	@SuppressWarnings("unchecked")
	public List<RepositoryStateModel> fetchAuditableStates(IAuditableHandle rootHandle, ValidateWorkItemStatesCommand command)  {
		List<RepositoryStateModel> result= new ArrayList<RepositoryStateModel>();

		try {
			List<IAuditableHandle> stateHandles= null;
			try {
				stateHandles= this.fTeamRepository.itemManager().fetchAllStateHandles(rootHandle, this.fMonitor);
			} catch (Exception e) {
				command.logger.error("Error fetching states for state handle:" + rootHandle.getItemId().getUuidValue());
				e.printStackTrace();
				return null;
			}
			
			command.logger.trace("... State handles fetched. For each handle, GET raw item state from repodebug...");
			for (IAuditableHandle stateHandle: stateHandles) {
				try {
					String stateId= stateHandle.getStateId().getUuidValue();
					RepositoryStateModel stateModel= new RepositoryStateModel(stateId, stateHandle);
					getItemRawContentViaRest(command, stateId, true); // for logging
					result.add(stateModel);
				} catch (Exception e) {
					command.logger.error("Exception fetching raw content from repodebug via REST GET. Enable REPODEBUG in Advanced Properties and ensure the user has JazzAdmin priveledges. Item will be verified by the Item API only. item:" + rootHandle.getItemId().getUuidValue());
					e.printStackTrace();
				}
			}
		
			try {
				command.logger.trace("... REST fetch via repodebug complete. Fetch all work item states from item manager using the repository API (fetchCompleteStates)...");
				boolean isFound=false;
				List<String>allPredecessorIds= new ArrayList<String>();
				List<IWorkItem>completeStates= (List<IWorkItem>)this.fTeamRepository.itemManager().fetchCompleteStates(stateHandles, this.fMonitor);
				for (IWorkItem workItem : completeStates) {
					String workItemStateId= workItem.getStateId().getUuidValue();
					command.logger.debug("...... Fetched Work Item State:" + workItemStateId + "\nWork Item: " + workItem.toString().replaceAll("Proxy of", ""));
					for (RepositoryStateModel stateModel : result) {

						if (stateModel.getStateId().equals(workItemStateId)) {
//							command.logger.trace("Found: Work item proxy:" + workItem.toString());
//							stateModel.setItemContent(workItem.getFullState().toString()); // Too much memory
							if (workItem != null && workItem.getPredecessorState() != null || workItem.getMergePredecessorState() != null ) {								
								String predecessorStateId= workItem.getPredecessorState() != null ? workItem.getPredecessorState().getStateId().getUuidValue() :
									workItem.getMergePredecessorState() != null ? workItem.getMergePredecessorState().getStateId().getUuidValue() : "nil";
									stateModel.setPredecessorStateId(predecessorStateId);
									allPredecessorIds.add(predecessorStateId); // used to find first item
							}
							
							isFound=true;
							break;
						}
					}
					if (!isFound) {
						// loading the repository state failed
						command.logger.trace("...... Repodebug state not found for Work item API state:" + workItemStateId + "\nWork Item: " + workItem.getFullState().toString());
						RepositoryStateModel stateModel= new RepositoryStateModel(workItemStateId, workItem); 
//						stateModel.setItemContent(workItem.getFullState().toString()); // too much memory
						if (workItem != null) {								
							String predecessorStateId= workItem.getPredecessorState() != null ? workItem.getPredecessorState().getStateId().getUuidValue() :
								workItem.getMergePredecessorState() != null ? workItem.getMergePredecessorState().getStateId().getUuidValue() : "nil";
								stateModel.setPredecessorStateId(predecessorStateId);
								allPredecessorIds.add(predecessorStateId); // used to find first item
						}
						result.add(stateModel);
					}
				}
				// Find the top item...
				for (RepositoryStateModel stateModel: result) {
					String stateId= stateModel.getStateId();
					boolean isCurrent= !allPredecessorIds.contains(stateId);
					stateModel.setIsCurrentState(isCurrent);
					if (isCurrent) {							
						command.logger.info("Current state is: " + stateId + " for  Work Item " +rootHandle.getItemId().getUuidValue());
					}
					if (stateModel.getPredecessorStateId() == null) {							
						command.logger.info("Initial state is: " + stateId + " for  Work Item " +rootHandle.getItemId().getUuidValue());
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				command.logger.error("Error fetching complete states for work item: " + rootHandle.getItemId().getUuidValue() + " handles:\n" + stateHandles);
				e.printStackTrace();
				return null;
			}
	
		} catch (Exception tre) {
			System.out.println("Exception reading states" + tre.getMessage());
			return null;
		}
		return result;
	}
 
	
		private final String GET_RAW_ITEM_STATE_RESOURCE_URL_PREFIX="/repodebug/repository/rawItemState/";
		private final String GET_RAW_ITEM_STATE_RESOURCE_URL_SUFFIX="/raw";
		private String getItemRawContentViaRest(ValidateWorkItemStatesCommand command,  String stateId, boolean isTracing) throws Exception {
	        String repositoryUrl= command.repositoryUrl;
			// https://theserver:9443/ccm/repodebug/repository/rawItemState/_n1tT8KegEeqU3qZ0xzTZsg/raw
			String resourceUrl= repositoryUrl + GET_RAW_ITEM_STATE_RESOURCE_URL_PREFIX + stateId +  GET_RAW_ITEM_STATE_RESOURCE_URL_SUFFIX;
			ITeamRawRestServiceClient restClient = command.getRestClient(new URI(resourceUrl));
			IRawRestClientConnection connection = restClient.getConnection(new URI(resourceUrl));

			connection.addRequestHeader(HttpUtil.ACCEPT, "application/xml");
			String content= null;
			try {
				command.logger.trace("... Loading raw item state: " + resourceUrl);
				com.ibm.team.repository.transport.client.ITeamRawRestServiceClient.IRawRestClientConnection.Response response = connection
						.doGet();			
				content= IOUtils.read(response.getResponseStream(), IOUtils.UTF8);
				if (isTracing) {					
					command.logger.trace("... Content fetched: " + stateId + " \n" + content);
				} else {
					command.logger.trace("... Content fetched for " + stateId + " length:" + content.length());
				}
			} catch (Exception e) {
				command.setUseRest(false);
				command.logger.error("Exception fetching raw content from repodebug via REST GET. " +
						"Enable REPODEBUG in Advanced Properties and ensure the user has JazzAdmin priveledges. " + 
						"State id:" + stateId);
				throw e;
			} 
			connection.release();
			return content;
		}
		
		public boolean exportWorkItemState(ValidateWorkItemStatesCommand command,  String stateId) throws Exception {
		// TODO
			return true;
		}
		
		public RepositoryStateModel getCurrentStateModelFromList(List<RepositoryStateModel> stateList) {
            for (RepositoryStateModel stateModel : stateList) {
				if(stateModel.getIsCurrentState()) {
					return stateModel;
				}
			}
			return null;
		}
		
		public RepositoryStateModel getStateModelWithId(List<RepositoryStateModel> stateList, String stateId) {
            for (RepositoryStateModel stateModel : stateList) {
				if(stateModel.getStateId().contentEquals(stateId)) {
					return stateModel;
				}
			}
			return null;
		}
		
		
		public boolean validateWorkItemStates(ValidateWorkItemStatesCommand command, List<RepositoryStateModel> stateList, String searchString) throws Exception {
			boolean result= true;
			RepositoryStateModel currentStateModel = getCurrentStateModelFromList(stateList);
			if (command.isUseRest()) {				
				validateWorkItemStateViaRest(command, stateList, currentStateModel, 0, searchString);
			}

			validateWorkItemStateWithItemAPI(command, currentStateModel, searchString);
			return result;
		}
		
		
		
		public boolean validateWorkItemStateViaRest(ValidateWorkItemStatesCommand command,  List<RepositoryStateModel> stateList, RepositoryStateModel stateModel, int rank, String searchString) throws Exception {
			command.logger.info("Validating: " + stateModel.getStateId() + "\n> " + rank + ": " + stateModel.getStateId() + " ... validating... ");
//						+ (stateModel.getRawContent() == null ? " [no repodebug content] " : " [rawContentSize: " + stateModel.getRawContent().length() + "] " /* + stateModel.getItemContent() */)
			boolean result= true;
			String exportFolder= command.getExportFolderPath();
			String rawContent= getItemRawContentViaRest(command, stateModel.getStateId(), false); // can be the second call but necessary to prevent memory overflow

			if (rawContent != null) {
				// Does the content contain the percent character or other invalid character			
				String trimmedContent= stripInvalidXMLCharacters2(rawContent); // should be the same
				if (!trimmedContent.contentEquals(rawContent)) {
					if (command.logger.isTraceEnabled()) {
						command.logger.trace("Work Item State: " + stateModel.getStateId() + " Warning: Trimmed Content Differs.\n***Warining Trimmed XML content differs..." + rawContent.length() + " [trimmed length: " + trimmedContent.length() + "]" 
												+ " trimmed Content:\n" + trimmedContent);
					} 
				} else {
					command.logger.info("Work Item State: " + stateModel.getStateId() + " XML Content OK. Length: " + rawContent.length());
				}
				
				Document stateDocument= validateElementsInWorkItemXML(command, rawContent);
				saveRawContentToFile(command, stateModel.getWorkItemId(), stateDocument, rank, stateModel.getStateId(),  null, null, rawContent);

//				stateModel.setStateDocument(stateDocument); // skip for performance
				validateReferencesViaRest(command, stateModel.getWorkItemId(), stateDocument, stateModel.getStateId(), 0, rank, searchString);
				
				// export attachments
				String rankString = "" + rank;
				if (rankString.length() == 1) {
					rankString= "00" + rankString;
				}
				if (rankString.length() == 2) {
					rankString= "0" + rankString;
				}
				
				if (exportFolder != null) {
					String fileName= "" + stateModel.getWorkItem().getId() + "." + rankString;
					File folder= new File(exportFolder+"/"+fileName+"."+stateModel.getStateId()+".attachments");
					AttachmentUtil.saveAttachmentsToDisk(folder, stateModel.getWorkItem(), getWorkItemCommon(), null);					
				}
				
				if (searchString != null) {
					checkSearchString(command, searchString, trimmedContent, "Work Item " + (stateModel.getWorkItem().getId()) , stateModel.getStateId());
				}
			}
			
			// Are the iteration states iterations and can they be parsed? 
			
			// TODO: 
			// validate project area : <contextId
			// mergePredecessor/predecessor
			// find a string or <tag> content.
			// <creator  itemId="_KGRY4CFWEdq-WY5y7lROQw"  stateId="_Z9AoECFWEdqvE47yzajdrw" />
			// <owner  itemId="_YNh4MOlsEdq4xpiOKg5hvA"  stateId="__VSe44b9EemHzv1EhNtN5A" />
			//	<category  itemId="_D48QQIb-EemHzv1EhNtN5A"  stateId="_D5CW4ob-EemHzv1EhNtN5A" />
			// <customAttributes  itemId="_DTojwob-EemHzv1EhNtN5A"  stateId="_DTrnFob-EemHzv1EhNtN5A" />

			
			if (stateModel.getPredecessorStateId() != null) {
				validateWorkItemStateViaRest(command, stateList, getStateModelWithId(stateList, stateModel.getPredecessorStateId()), rank+1, searchString);
			}
			
			return result;
		}
		
		public static String stripInvalidXMLCharacters2(String input) {
			String output = "";
		    try {
		        /* From ISO-8859-1 to UTF-8 */
		        output = new String(input.getBytes("ISO-8859-1"), "UTF-8");
		    } catch (UnsupportedEncodingException e) {
		        e.printStackTrace();
		    }
		    return output;
		}
		
		/**
		  * This method ensures that the output String has only valid XML unicode
		  * characters as specified by the XML 1.0 standard. For reference, please
		  * see <a href=”http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char”>the
		  * standard</a>. 
		  * 
		  * @param in The String whose non-valid characters we want to remove.
		  * @return The in String, stripped of non-valid characters.
		  */
		public static String stripInvalidXMLCharacters(String in) {
		     // XML 1.0
		     // #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
			 in = in.replace("\u00A0", " ");
			 in = in.replace("\u00FC", "ü");
			 in = in.replace("\u00FC", "Ü");
			 in = in.replace("\u00F6", "ö");
			 in = in.replace("\u00D6", "Ö");
			 in = in.replace("\u00E4", "ä"); 
			 in = in.replace("\u00C4", "Ä");
			 in = in.replace("\u00DF", "S");
		     String xml10pattern = 
		             "[^" + 
		             "\u0009\r\n" + 
		             "\u0020-\uD7FF" + 
		             "\uE000-\uFFFD" + 
		             "\ud800\udc00-\udbff\udfff" + 
		             "]";
		     return in.replaceAll(xml10pattern, "").trim();
		}

		public Document validateReferencesViaRest(ValidateWorkItemStatesCommand command,  String workItemId, Document doc, String workItemStateId, int depth, int rank, String searchString) throws Exception {
			if (depth > command.getDepth()) {
				return null;
			}
			// target
			// Found in
			// custom attributes
			// project area
			// todo: Contributors
			if (doc != null) {				
				String targetItemId= getAttributeForTagFromDocument(command, doc, "target", "itemId");
				command.logger.trace("Loading Target...");
				if (targetItemId != null) {					
					validateAttributeStateViaRest(command, workItemId, workItemStateId, "target", targetItemId, depth, rank, searchString );
				} else {
					command.logger.trace("OK. No target (Planned For) attribute found in state.");
				}

				command.logger.trace("Loading Found In...");
				String foundInItemId= getAttributeForTagFromDocument(command, doc, "foundIn", "itemId");
				validateAttributeStateViaRest(command, workItemId,  workItemStateId, "foundIn", foundInItemId, depth, rank, searchString);
				
				// Get all states for custom attributes...
				command.logger.trace("Loading custom attributes...");
				List<String> customAttributeStateIdList= getAttributeForTagsFromDocument(command, doc, "customAttribute", "stateId");
				if (customAttributeStateIdList != null && customAttributeStateIdList.size() > 0) {					
					for (String customStateId : customAttributeStateIdList) {
						validateAttributeStateViaRest(command, workItemId, workItemStateId, "customAttribute", customStateId, depth, rank, searchString);
					}
				} else {
					command.logger.trace("OK. No custom attributes found.");
				}
				
				command.logger.trace("Loading Project Area...");
				String projectAreaItemId= getAttributeForTagFromDocument(command, doc, "projectArea", "itemId");
				validateAttributeStateViaRest(command, workItemId, workItemStateId, "projectArea", projectAreaItemId, depth, rank, searchString);
				
				command.logger.trace("OK. References validated.");
			} else {
				command.logger.trace("Warning. Null document returned from parseXmlDocument. \n*** Check state: " + workItemStateId + " for work item: " + workItemId );
			}

			return doc;
		}
		
		public Document validateElementsInWorkItemXML(ValidateWorkItemStatesCommand command,  String content) throws Exception {
			Document doc= null;
			try {				
				doc= parseXmlDocument(content); // throw exception if broken
				NodeList nodes= doc.getElementsByTagName("id");
				String id= nodes.getLength() > 0 ? nodes.item(0).getFirstChild().getNodeValue() : "[no id]";
				
				nodes= doc.getElementsByTagName("mergePredecessor");
				String mergePredId= "[no mergePredecessor]";
				if (nodes != null && nodes.getLength() > 0 && nodes.item(0) != null && nodes.item(0).getFirstChild() != null) {					
					mergePredId= nodes.item(0).getFirstChild().getNodeValue();
				}
				
				nodes= doc.getElementsByTagName("predecessor");
				String predId=  "[no predecessor]";
				if (nodes != null && nodes.getLength() > 0 && nodes.item(0) != null && nodes.item(0).getFirstChild() != null) {	
					predId= nodes.item(0).getFirstChild().getNodeValue();
				}
				
				nodes= doc.getElementsByTagName("stateId");
				String stateId=  "[no stateId]";
				if (nodes != null && nodes.getLength() > 0 && nodes.item(0) != null && nodes.item(0).getFirstChild() != null) {
					stateId= nodes.item(0).getFirstChild().getNodeValue();
				}
				
				nodes= doc.getElementsByTagName("workitem:WorkItem");
				String itemId= "[no itemId]";
				if (nodes != null && nodes.getLength() > 0 && nodes.item(0).getAttributes() != null && nodes.item(0).getAttributes().getNamedItem("itemId") != null ) {
					itemId=  nodes.item(0).getAttributes().getNamedItem("itemId").getNodeValue();
				}

				command.logger.trace("OK. XML parsed.\n--Id: " + id + " itemId: " + itemId + " state: " + stateId + " predecessor: " + predId + " mergePredesessor: " + mergePredId );
			} catch (Exception e) {
				if(command.logger.isTraceEnabled()) {
					command.logger.error("Exception parsing xml. Content: \n" + content);
				} else {					
					command.logger.error("Exception parsing xml.");
				}
				return null;
			}

				return doc;
		}
		
		public Document validateElementsInStateXML(ValidateWorkItemStatesCommand command,  String content) throws Exception {
			Document doc= null;
			try {				
				doc= parseXmlDocument(content); // throw exception if broken
				
				NodeList nodes= doc.getElementsByTagName("mergePredecessor");
				String mergePredId= "[no mergePredecessor]";
				if (nodes != null && nodes.getLength() > 0 && nodes.item(0) != null && nodes.item(0).getFirstChild() != null) {					
					mergePredId= nodes.item(0).getFirstChild().getNodeValue();
				}
				
				nodes= doc.getElementsByTagName("predecessor");
				String predId=  "[no predecessor]";
				if (nodes != null && nodes.getLength() > 0 && nodes.item(0) != null && nodes.item(0).getFirstChild() != null) {	
					predId= nodes.item(0).getFirstChild().getNodeValue();
				}
				
				nodes= doc.getElementsByTagName("stateId");
				String stateId=  "[no stateId]";
				if (nodes != null && nodes.getLength() > 0 && nodes.item(0) != null && nodes.item(0).getFirstChild() != null) {
					stateId= nodes.item(0).getFirstChild().getNodeValue();
				}
				
				command.logger.trace("OK. XML parsing state validated.\n---- state: " + stateId + " predecessor: " + predId + " mergePredesessor:" + mergePredId );
			} catch (Exception e) {
				if(command.logger.isTraceEnabled()) {
					command.logger.error("Exception parsing xml. Content: \n" + content);
				} else {					
					command.logger.error("Exception parsing xml.");
				}
				return null;
			}

				return doc;
		}
		
		
		public String getAttributeForTagFromDocument(ValidateWorkItemStatesCommand commandDocument, Document doc, String tagName, String attributeNameOrNull) {
			NodeList nodes= doc.getElementsByTagName(tagName);
			String result= null;
			if (nodes != null && nodes.getLength() > 0 && nodes.item(0) != null) {
				if (attributeNameOrNull == null && nodes.item(0).getFirstChild() != null) {
					result = nodes.item(0).getFirstChild().getNodeValue();
				} else {
					if (nodes.item(0).getAttributes() != null && nodes.item(0).getAttributes().getNamedItem(attributeNameOrNull) != null) {
						result= nodes.item(0).getAttributes().getNamedItem(attributeNameOrNull).getNodeValue();
					}
				}
			}
			
			return result;
		}
		
		public List<String> getAttributeForTagsFromDocument(ValidateWorkItemStatesCommand commandDocument, Document doc, String tagName, String attributeNameOrNull) {
			NodeList nodes= doc.getElementsByTagName(tagName);
			List<String>  result= new ArrayList<String>();
			if (nodes == null || nodes.getLength() == 0){
				return result;
			}
					
			for (int i=0; i<nodes.getLength(); i++) {
				Node node= nodes.item(i);
				if (attributeNameOrNull == null && node.getFirstChild() != null) {
					String value= node.getFirstChild().getNodeValue();
					result.add(value);
				} else { // attribute
					if (node.getAttributes() != null && node.getAttributes().getNamedItem(attributeNameOrNull) != null) {
						String value= node.getAttributes().getNamedItem(attributeNameOrNull).getNodeValue();
						result.add(value);
					}
				}
			}
			
			return result;
		}
		
		private List<String> visitedRestStates= new ArrayList<String>();
		public void validateAttributeStateViaRest(ValidateWorkItemStatesCommand command, String workItemId, String workItemStateId, String attributeName, String attributeStateId, int depth, int rank, String searchString) throws Exception {
			if (attributeStateId== null) {
				command.logger.trace("OK. Attribute not set in remote state: " + attributeName);
				return; // valid
			}
			if (visitedRestStates.contains(attributeStateId)) {
				command.logger.trace("OK. State already visited for " + attributeName + " stateId: " + attributeStateId);
				return;
			}
			try {
			command.logger.trace(">> Reading remote state XML for attribute: " + attributeName + " ["+ attributeStateId + "]" );

			String attributeContent= getItemRawContentViaRest(command,  attributeStateId, false);
			command.logger.info("Content for " + attributeName + " ["+ attributeStateId + "] read. Size: " + attributeContent.length());
			
			checkSearchString(command, searchString, attributeContent, ("attribute " + attributeName), attributeStateId);
			
			visitedRestStates.add(attributeStateId);
			Document doc= validateElementsInStateXML(command, attributeContent);
			doc= validateReferencesViaRest(command, workItemId, doc, workItemStateId, depth+1, rank, searchString);
			saveRawContentToFile(command, workItemId, doc, rank, workItemStateId,  attributeName, attributeStateId, attributeContent);
			
			} catch (Exception e) {
				command.logger.error("Error. Cannot load content from attribute: " + attributeName + " [" + attributeStateId + "]");
			}
			
		}
		
		public void checkSearchString(ValidateWorkItemStatesCommand command, String searchString, String attributeContent, String attributeName, String attributeStateId) {
			if (searchString != null && attributeContent != null) {
				int dex= attributeContent.indexOf(searchString);
				if (dex != -1) {
					command.logger.warn("Search string \"" + searchString + "\" found in " + attributeName + " stateId: " + attributeStateId 
				+ "\n>>>> Search String found at index: " + dex + " in " + attributeName + "\n" 
				+   (attributeContent.substring(Math.max(0, dex-300), Math.min(dex+300, (attributeContent.length()-1) ))) 
						);
							
				} else {
					command.logger.debug("Search string not found  in " + attributeName + " stateId: " + attributeStateId);
				}
			}		
		}
		
		public Document parseXmlDocument(String xmlDocument) throws ParserConfigurationException, SAXException, IOException {
			// parse XML document
			DocumentBuilderFactory factory= SecureDocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setValidating(false);
			DocumentBuilder docBuilder= factory.newDocumentBuilder();
			docBuilder.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
					return new InputSource(new StringReader("")); //$NON-NLS-1$
				}
			});
			org.xml.sax.InputSource inStream= new org.xml.sax.InputSource();
			inStream.setCharacterStream(new StringReader(xmlDocument));
			Document doc= docBuilder.parse(inStream);
			return doc;
		}
		
		public void validateWorkItemStateWithItemAPI(ValidateWorkItemStatesCommand command, RepositoryStateModel workItemStateModel, String searchString) throws Exception {
			if (workItemStateModel != null && workItemStateModel.getWorkItem() != null) {
				try {
				IWorkItem workItem= workItemStateModel.getWorkItem();
				validateAttributeStateWithItemAPI(command, workItem.getCategory(), "category", searchString);
				validateAttributeStateWithItemAPI(command, workItem.getProjectArea(), "projectArea", searchString);
				validateAttributeStateWithItemAPI(command, workItem.getTarget(), "target", searchString);
				
				List<IAttributeHandle> customAttributeHandles = workItem.getCustomAttributes();
				for (IAttributeHandle attrHandle: customAttributeHandles) {
					validateAttributeStateWithItemAPI(command, attrHandle, "custom", searchString);
				}
				
				
				IItemType type = workItem.getItemType();
				IComments comments = workItem.getComments();
				XMLString description = workItem.getHTMLDescription();
				XMLString summary = workItem.getHTMLSummary();
				Identifier<IState> state = workItem.getState2();
				List<String> tagList = workItem.getTags2();
				String itemType= workItem.getWorkItemType();
				
				command.logger.trace("Work item verified via API.");
				} catch (Exception e) {
					command.logger.error("Could not fetch item state " + e);
					e.printStackTrace();
				}
			}
		}
		
		private List<String> visitedApiStates= new ArrayList<String>();
		public void validateAttributeStateWithItemAPI(ValidateWorkItemStatesCommand command, IAuditableHandle attributeHandle, String attributeName, String searchString) throws Exception {
			
			if (attributeHandle == null || attributeHandle.getItemId() == null) {
				command.logger.trace("skipping: " + attributeHandle);
				return;
			}
			if(visitedApiStates.contains(attributeHandle.getItemId().getUuidValue())) {
				command.logger.trace("Attribute: " + attributeName + "[ " + attributeHandle.getItemId().getUuidValue() + " ] already fetched.");
				return;
			}
			visitedApiStates.add(attributeHandle.getItemId().getUuidValue());
			List<IAuditableHandle> stateHandles= null;
			try {				
				stateHandles= this.fTeamRepository.itemManager().fetchAllStateHandles(attributeHandle, this.fMonitor);
			} catch (Exception e) {
				command.logger.error("Exception fetching all state handles for attribute: " + attributeName + "[ " + attributeHandle.getItemId().getUuidValue());
				e.printStackTrace();
				return;
			}
			try {
				int count = 0;
				List<IAuditable>completeStates= (List<IAuditable>)this.fTeamRepository.itemManager().fetchCompleteStates(stateHandles, this.fMonitor);
				for (IAuditable attributeState: completeStates) {
					try {
						String attributeStateString = attributeState.getWorkingCopy().toString();
						if (command.logger.isDebugEnabled()) {
							command.logger.debug("[" + count++ + "] Fetched attribute: " + attributeName + " stateId: " + attributeState.getStateId().getUuidValue()
									+ "... " + attributeName + " has " + completeStates.size() + (completeStates.size() == 1 ? " state." :" states.")
									+ "\n" + simplifyTraceString(attributeStateString));
						} else {
							command.logger.info("[" + count++ + "] Fetched attribute: " + attributeName + " stateId: " + attributeState.getStateId().getUuidValue()
									+ "... " + attributeName + " has " + completeStates.size() + (completeStates.size() == 1 ? " state." :" states.")
									);
						}

						if (searchString != null) {
							checkSearchString(command, searchString, simplifyTraceString(attributeStateString), ("Attribute '" + attributeName +"'"), attributeState.getStateId().getUuidValue());
						}
						
						if (!command.logger.isTraceEnabled()) {
							break; // only log the first one	
						}
						
					} catch (Exception e) {
						command.logger.error("Could not fetch: " + attributeName);
						e.printStackTrace();
					}
				}				
			} catch (Exception e) {
				command.logger.error("Exception fetching complete states for attribute: " + attributeName + "[ " + attributeHandle.getItemId().getUuidValue());
				e.printStackTrace();
				return;
			}
		}
		
		private String simplifyTraceString(String myString) {
			return myString.replace("com.ibm.team.process.internal.common.impl.", "").replace("com.ibm.team.workitem.common.internal.model.impl.", "").replace("com.ibm.team.repository.client.internal.", "");
		}
		private boolean madeFolder= false;
		private boolean madeTrimmedFolder= false;
		
		private static final String REGEX_SPAN= "&lt;(span|SPAN)[^>]*>";
		private static final String REGEX_ESPAN= "&lt;/(span|SPAN)[^>]*>";
		private static final String REGEX_B= "&lt;(b|B)[^>]*>";
		private static final String REGEX_EB= "&lt;/(b|B)[^>]*>";
		private static final String REGEX_I= "&lt;(i|I)[^>]*>";
		private static final String REGEX_EI= "&lt;/(i|I)[^>]*>";
		private static final String REGEX_NBSP= "&amp;nbsp;";
		private static final String REGEX_REPLACE_EMPTY= "";
		private static final String REGEX_REPLACE_SPACE= " ";
		private static final String MODIFIED_FOLDER= "/modified/";
		private void saveRawContentToFile(ValidateWorkItemStatesCommand command, String workItemId, Document document, int rank, String workItemStateId, String attributeName, String attributeStateId, String stateContent ) {
			try {
				String modifiedRawContent= null;
				String contentType= document.getDocumentElement().getNodeName();

				boolean isTrimmed = command.isTrimHtml() && contentType.toLowerCase().indexOf("workitem") != -1;
				
				String folder= command.getExportFolderPath();
				if (folder == null)  return; // OK, no export
				File dirFile= new File(folder);
				if(!madeFolder && !dirFile.exists()) {
					command.logger.info("Make Folder " + folder);
					FileUtils.forceMkdir(dirFile);			
				} else {
					madeFolder=true;
				}
				
				if (isTrimmed) {
			     // stateContent.replaceAll("&lt;(span|SPAN)[^>]*>", "");  // For testing
					 modifiedRawContent = replaceAllIgnoreCase(stateContent, "&lt;(span|SPAN)[^>]*>", REGEX_REPLACE_EMPTY);     // remove all span
					 modifiedRawContent = replaceAllIgnoreCase(modifiedRawContent, REGEX_ESPAN, REGEX_REPLACE_EMPTY); // remove end span
					 modifiedRawContent = replaceAllIgnoreCase(modifiedRawContent, REGEX_B, REGEX_REPLACE_EMPTY);     // remove b
					 modifiedRawContent = replaceAllIgnoreCase(modifiedRawContent, REGEX_EB, REGEX_REPLACE_EMPTY);    // remove end b
					 modifiedRawContent = replaceAllIgnoreCase(modifiedRawContent, REGEX_I, REGEX_REPLACE_EMPTY);     // remove i
					 modifiedRawContent = replaceAllIgnoreCase(modifiedRawContent, REGEX_EI, REGEX_REPLACE_EMPTY);    // remove end i	
					 modifiedRawContent = replaceAllIgnoreCase(modifiedRawContent, REGEX_NBSP, REGEX_REPLACE_SPACE);    // remove end i	

					 if(!madeTrimmedFolder) {
						 String trimmedFolder= folder + MODIFIED_FOLDER;
						 File trimmedDirFile= new File(trimmedFolder);
						 if(!trimmedDirFile.exists()) {
							 command.logger.info("... Make Trimmed Folder " + trimmedFolder);
							 FileUtils.forceMkdir(trimmedDirFile);			
						 } 
						 madeTrimmedFolder=true;
					 }
					 
					 if (modifiedRawContent.equals(stateContent)) {
						 modifiedRawContent = null;
					 }
				 }
				
				String rankString = "" + rank;
				if (rankString.length() == 1) {
					rankString= "00" + rankString;
				}
				if (rankString.length() == 2) {
					rankString= "0" + rankString;
				}
				String fileName= "" + workItemId + "." + rankString;
				
				if (attributeName != null) { // attribute state
					fileName+=  "." + workItemStateId + "." + attributeName + "."  + attributeStateId;
				} else {
					fileName+=  "." + workItemStateId + "..";
				}
				
				String modifiedFileName = null;
				if (modifiedRawContent != null) {
					modifiedFileName = fileName + "." + contentType.replaceAll(":","_") + "_modified.xml" ;
				}
				fileName+= "." + contentType.replaceAll(":","_") + ".xml" ;
				 
				command.logger.info("Saving file " + dirFile.getAbsolutePath() + "/" +  fileName + " size: " + stateContent.length());
				storeFileContent(command, dirFile.getAbsolutePath() + "/" +  fileName, stateContent);
				if (modifiedRawContent != null) {	
					command.logger.info("... Saving trimmed file " + dirFile.getAbsolutePath()  + MODIFIED_FOLDER +  modifiedFileName + " size: " + modifiedRawContent.length());
					if(command.logger.isTraceEnabled()) {						
						command.logger.trace("Modified html Content: \n" + modifiedRawContent);
					}
					storeFileContent(command, dirFile.getAbsolutePath() + MODIFIED_FOLDER +  modifiedFileName, modifiedRawContent);
				}				
			} catch (Exception e) {
				command.logger.error("Could not save: " + attributeName );
				e.printStackTrace();
			}
			
			
			
		}
		
		public String replaceAllIgnoreCase(String original, String searchRegEx, String replaceString) {
		    return original.replaceAll(searchRegEx, replaceString);
		}
//		public String replaceAllIgnoreCase(String original, String searchString, String replaceString) {
//		    return Pattern.compile(searchString, Pattern.LITERAL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(original)
//		.replaceAll(Matcher.quoteReplacement(replaceString));
//		}
		
			/**
		 * Read a file into an array. Should be a short file
		 * 
		 * @param aFile
		 */
		public static boolean storeFileContent(ValidateWorkItemStatesCommand command, String fileName, String content) {
			File file= new File(fileName);
			try{
				FileUtils.writeByteArrayToFile(file,  content.getBytes());
			} catch (Exception e){
				System.out.println("Could not save file: "  + file);
				e.printStackTrace();
			}
			return true;
		}
}

