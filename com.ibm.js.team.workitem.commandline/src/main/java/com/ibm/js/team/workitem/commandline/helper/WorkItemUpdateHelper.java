/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.framework.ParameterValue;
import com.ibm.js.team.workitem.commandline.framework.ReferenceData;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.parameter.ParameterList;
import com.ibm.js.team.workitem.commandline.utils.AccessContextUtil;
import com.ibm.js.team.workitem.commandline.utils.AttachmentUtil;
import com.ibm.js.team.workitem.commandline.utils.BuildUtil;
import com.ibm.js.team.workitem.commandline.utils.ProcessAreaUtil;
import com.ibm.js.team.workitem.commandline.utils.ReferenceUtil;
import com.ibm.js.team.workitem.commandline.utils.SimpleDateFormatUtil;
import com.ibm.js.team.workitem.commandline.utils.StringUtil;
import com.ibm.js.team.workitem.commandline.utils.WorkItemUtil;
import com.ibm.team.build.client.ITeamBuildClient;
import com.ibm.team.build.common.model.IBuildResultHandle;
import com.ibm.team.build.common.model.query.IBaseBuildResultQueryModel.IBuildResultQueryModel;
import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.links.common.IItemReference;
import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.factory.IReferenceFactory;
import com.ibm.team.links.common.registry.IEndPointDescriptor;
import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.common.IDevelopmentLine;
import com.ibm.team.process.common.IIteration;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.process.common.ITeamArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContent;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.IItemHandle;
import com.ibm.team.repository.common.ItemNotFoundException;
import com.ibm.team.repository.common.Location;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.UUID;
import com.ibm.team.repository.common.query.IItemQuery;
import com.ibm.team.repository.common.query.IItemQueryPage;
import com.ibm.team.repository.common.query.ast.IPredicate;
import com.ibm.team.repository.common.service.IQueryService;
import com.ibm.team.scm.client.IWorkspaceManager;
import com.ibm.team.scm.client.SCMPlatform;
import com.ibm.team.scm.common.IComponentHandle;
import com.ibm.team.scm.common.dto.IComponentSearchCriteria;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.internal.IAdditionalSaveParameters;
import com.ibm.team.workitem.common.internal.util.SeparatedStringList;
import com.ibm.team.workitem.common.model.AttributeTypes;
import com.ibm.team.workitem.common.model.IApproval;
import com.ibm.team.workitem.common.model.IApprovalDescriptor;
import com.ibm.team.workitem.common.model.IApprovals;
import com.ibm.team.workitem.common.model.IAttachment;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IAttributeHandle;
import com.ibm.team.workitem.common.model.ICategory;
import com.ibm.team.workitem.common.model.ICategoryHandle;
import com.ibm.team.workitem.common.model.IComment;
import com.ibm.team.workitem.common.model.IComments;
import com.ibm.team.workitem.common.model.IDeliverable;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IResolution;
import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.WorkItemApprovals;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.common.model.WorkItemLinkTypes;
import com.ibm.team.workitem.common.workflow.IWorkflowAction;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;

/**
 * Class helps with manipulating work item attribute values.
 * 
 */
@SuppressWarnings("restriction")
public class WorkItemUpdateHelper {

	private static final int XML_GROWTH_CONSTANT = 50;
	public static final String STRING_TYPE_HTML = "HTML";
	public static final String STRING_TYPE_WIKI = "WIKI";
	public static final String STRING_TYPE_PLAINSTRING = "STRING";
	public static final String STRING_LINEBREAK_HTML_BR = "<br>";
	public static final String STRING_LINEBREAK_BACKSLASH_N = "\\n";
	// Item Type Internal Identifier
	public static final String TYPE_PROCESS_AREA = "ProcessArea";
	public static final String TYPE_TEAM_AREA = "TeamArea";
	public static final String TYPE_PROJECT_AREA = "ProjectArea";
	// Addition for ItemList and Item type
	public static final String TYPE_CATEGORY = "Category";
	public static final String TYPE_CONTRIBUTOR = "User";
	public static final String TYPE_TIMELINE = "Timeline";
	public static final String TYPE_ITERATION = "Iteration";
	public static final String TYPE_WORKITEM = "WorkItem";
	public static final String TYPE_SCM_COMPONENT = "SCMComponent";

	// Flags
	public static final String STATECHANGE_FORCESTATE = "forceState";
	// Pseudo attributes
	public static final String PSEUDO_ATTRIBUTE_ATTACHFILE = "@attachFile";
	public static final String PSEUDO_ATTRIBUTE_DELETEATTACHMENTS = "@deleteAttachments";
	public static final String PSEUDO_ATTRIBUTEVALUE_YES = "yes";
	public static final String PSEUDO_ATTRIBUTEVALUE_DELETEDANGLING = "deletedangling";
	public static final String PSEUDO_ATTRIBUTE_LINK = "@link_";
	public static final String PSEUDO_ATTRIBUTE_DELETELINKSOFTYPE = "@deleteLinks_";
	public static final String PSEUDO_ATTRIBUTE_TRIGGER_WORKFLOW_ACTION = "@workflowAction";
	// Approval Data
	public static final String APPROVAL_TYPE_VERIFICATION = "verification";
	public static final String APPROVAL_TYPE_REVIEW = "review";
	public static final String APPROVAL_TYPE_APPROVAL = "approval";

	// prefixes
	public static final String HTTP_PROTOCOL_PREFIX = IWorkItemCommandLineConstants.HTTP_PROTOCOL_PREFIX; // to
																											// //
																											// URI's
	public static final String PREFIX_REFERENCETYPE = "@";

	// Separators
	public static final String APPROVAL_SEPARATOR = ":";
	public static final String FORCESTATE_SEPARATOR = APPROVAL_SEPARATOR;
	public static final String LINK_SEPARATOR = "\\|";
	public static final String LINK_SEPARATOR_HELP = "|";
	public static final String ITEMTYPE_SEPARATOR = ":";
	public static final String ITEM_SEPARATOR = ",";
	public static final String PATH_SEPARATOR = "/";
	public static final String ATTACHMENT_SEPARATOR = ITEM_SEPARATOR;

	// postfix when truncating strings
	private static final String VALUE_TRUNCATED_POSTFIX = ".. Truncated";

	// The fields needed
	private IProgressMonitor monitor = null;
	private IWorkItem fItem = null;
	private WorkItemWorkingCopy fWorkingCopy = null;
	private ITeamRepository fTeamRepository = null;
	private ParameterList fParameters = new ParameterList();
	private boolean fEnforceSizeLimits = false;
	private boolean fBulkupdate = false;
	private boolean fupdateBacklinks = false;
	private boolean fImportIgnoreMissingAttributes = false;

	/**
	 * Internal class to parse and manage approval data.
	 * 
	 */
	private class ApprovalInputData {

		private String approvalName = null;
		private String approvalType = null;
		private String approverList = null;

		/**
		 * Constructor to create the class
		 * 
		 * @param parameter
		 */
		public ApprovalInputData(ParameterValue parameter) {
			List<String> approvalData = StringUtil.splitStringToList(parameter.getValue(), APPROVAL_SEPARATOR);
			// Check format
			if (approvalData.size() < 2 || 3 < approvalData.size()) {
				throw new WorkItemCommandLineException("Incorrect approval format: " + parameter.getAttributeID()
						+ " Value: " + parameter.getValue() + helpUsageApprovals());
			}
			// Get Approval base data
			String approvalTypeString = approvalData.get(0);
			approvalName = approvalData.get(1);

			// find approval type
			if (APPROVAL_TYPE_APPROVAL.equals(approvalTypeString.trim())) {
				this.approvalType = WorkItemApprovals.APPROVAL_TYPE.getIdentifier();
			} else if (APPROVAL_TYPE_REVIEW.equals(approvalTypeString.trim())) {
				this.approvalType = WorkItemApprovals.REVIEW_TYPE.getIdentifier();
			} else if (APPROVAL_TYPE_VERIFICATION.equals(approvalTypeString.trim())) {
				this.approvalType = WorkItemApprovals.VERIFICATION_TYPE.getIdentifier();
			} else {
				throw new WorkItemCommandLineException("Approval type not found: " + parameter.getAttributeID()
						+ " Value: " + parameter.getValue() + helpUsageApprovals());
			}
			// get approver user ID's if available
			if (approvalData.size() == 3) {
				this.approverList = approvalData.get(2);
			}
		}

		/**
		 * @return the type of the approval
		 */
		public String getApprovalType() {
			return approvalType;
		}

		/**
		 * @return the name of the approval
		 */
		public String getApprovalName() {
			return approvalName;
		}

		/**
		 * @return the list of approvers
		 */
		public String getApprovers() {
			return approverList;
		}
	}

	/**
	 * This constructor should only be used for accessing the help text. The
	 * constructor misses several core fields that are used for normal
	 * operation.
	 */
	public WorkItemUpdateHelper() {
	}

	/**
	 * Use this constructor to update attribute values.
	 * 
	 * @param workingCopy
	 *            of the work item to be updated
	 * @param parameters
	 *            - the parameters passed e.g. for finding flags
	 * @param monitor
	 *            a progress monitor or null
	 */
	public WorkItemUpdateHelper(WorkItemWorkingCopy workingCopy, ParameterList parameters, IProgressMonitor monitor) {
		super();
		this.monitor = monitor;
		if (parameters != null) {
			this.fParameters = parameters;
		}
		this.fWorkingCopy = workingCopy;
		this.fItem = fWorkingCopy.getWorkItem();
		this.fTeamRepository = (ITeamRepository) fItem.getOrigin();
		setEnforceSizeJimits(parameters.hasSwitch(IWorkItemCommandLineConstants.SWITCH_ENFORCE_SIZE_LIMITS));
		setBatchOperation(parameters.hasSwitch(IWorkItemCommandLineConstants.SWITCH_BULK_OPERATION));
		setImportIgnoreMissingAttributes(
				parameters.hasSwitch(IWorkItemCommandLineConstants.SWITCH_IMPORT_IGNORE_MISSING_ATTTRIBUTES));
	}

	/**
	 * Switch to minimize output for bulk updates - set value
	 * 
	 * @param hasSwitch
	 */
	private void setBatchOperation(boolean hasSwitch) {
		this.fBulkupdate = hasSwitch;
	}

	public boolean isImportIgnoreMissingAttributes() {
		return fImportIgnoreMissingAttributes;
	}

	public void setImportIgnoreMissingAttributes(boolean fImportIgnoreMissingAttributes) {
		this.fImportIgnoreMissingAttributes = fImportIgnoreMissingAttributes;
	}

	/**
	 * Switch to minimize output for bulk updates - set value
	 * 
	 * @return
	 */
	private boolean isBulkUpdate() {
		return this.fBulkupdate;
	}

	/**
	 * Try to cut down values that are too big to fit into an attribute e.g. for
	 * string and HTML type attributes.
	 * 
	 * @param flag
	 */
	private void setEnforceSizeJimits(boolean flag) {
		this.fEnforceSizeLimits = flag;
	}

	/**
	 * See if size limits are enforced.
	 */
	private boolean isEnforceSizeLimits() {
		return fEnforceSizeLimits;
	}

	/**
	 * Some operations require the work items IWorkItem interface. This is
	 * provided here.
	 * 
	 * @return the IWorkItem interface
	 */
	private IWorkItem getWorkItem() {
		return fItem;
	}

	/**
	 * Some operations require the WorkItemWorkingCopy. This is provided here.
	 * 
	 * @return the WorkItemWorkingCopy
	 */
	private WorkItemWorkingCopy getWorkingCopy() {
		return fWorkingCopy;
	}

	/**
	 * @return the parameters passed to this class - mainly for getting flags
	 */
	private ParameterList getParameters() {
		return fParameters;
	}

	/**
	 * The main entry point for the helper. Call this method to update a
	 * property for a work item. Provide the property ID and the attribute
	 * value. Property ID and value can be encoded.
	 * 
	 * @param propertyID
	 *            - the property ID this is for; this can be encoded
	 * @param value
	 *            - the value to sat
	 * @throws TeamRepositoryException
	 * @throws WorkItemCommandLineException
	 * @throws IOException
	 */
	public void updateProperty(String propertyID, String value)
			throws TeamRepositoryException, WorkItemCommandLineException, IOException {
		// System.out.println("--->Trying: [" + propertyID + "] Value: [" +
		// value
		// + "]");
		ParameterValue parameter = new ParameterValue(propertyID, value, getWorkItem().getProjectArea(), monitor);
		// To be able to print a list of exceptions
		List<Exception> exceptions = new ArrayList<Exception>();

		// Handle special properties first
		if (parameter.getAttributeID().equals(IWorkItem.ID_PROPERTY)) {
			// The ID of the work item - this can not be modified.
			throw new WorkItemCommandLineException("ID of work item can not be changed: '" + parameter.getAttributeID()
					+ "' Value: '" + parameter.getValue() + "'.");
		} else if (parameter.getAttributeID().equals(IWorkItem.CREATOR_PROPERTY)) {
			Object creator = calculateContributor(parameter);
			getWorkItem().setCreator((IContributorHandle) creator);
		} else if (parameter.getAttributeID().equals(IWorkItem.CREATION_DATE_PROPERTY)) {
			Timestamp cDate = getWorkItem().getCreationDate();
			if(cDate==null){ // CreationDate can only be set during work item creation.
				Object creationDate = calculateTimestamp(parameter);
				if(creationDate!=null){
					getWorkItem().setCreationDate((Timestamp) creationDate);
				}
			}
		} else if (parameter.getAttributeID().equals(IWorkItem.TYPE_PROPERTY)) {
			// Update the type
			throw new WorkItemCommandLineException(
					"Illegal parameter exception. The work item type parameter should have been already consumed by the command code: "
							+ parameter.getAttributeID() + " Value: " + parameter.getValue());
		} else if (parameter.getAttributeID().equals(IWorkItem.CONTEXT_ID_PROPERTY)) {
			UUID contextID = calculateUUID(parameter, exceptions);
			getWorkItem().setContextId(contextID);
		} else if (parameter.getAttributeID().equals(IWorkItem.SUMMARY_PROPERTY)) {
			/**
			 * The Summary of the work item
			 * 
			 * Syntax:
			 * 
			 * "Plain Text <b>Bold Text</b> <i>Italic Text</i> <a
			 * href=\"https://rsjazz.wordpress.com\">External RSJazz Link</a>
			 * <b>@ralph </b>Defect 3 "  
			 * 
			 * Important: Escape additional quotes in input string as \" line
			 * breaks are ignored.
			 */

			String summary = enforceSizeLimits(
					calculateXMLDescription(parameter, getWorkItem().getHTMLSummary(), STRING_TYPE_PLAINSTRING),
					parameter.getIAttribute().getAttributeType());

			getWorkItem().setHTMLSummary(XMLString.createFromXMLText(summary));
		} else if (parameter.getAttributeID().equals(IWorkItem.DESCRIPTION_PROPERTY)) {
			/**
			 * The Description of the work item
			 * 
			 * Syntax:
			 * 
			 * "Plain Text<br/>
			 * <b>Bold Text</b><br/>
			 * <i>Italic Text</i><br/>
			 * <a href=\"https://rsjazz.wordpress.com\">External RSJazz Link</a>
			 * <br/>
			 * <b>@ralph </b><br/>
			 * Defect 3 <br/>
			 * " 
			 * 
			 * Important: Escape additional quotes in input string as \"
			 */
			String description = enforceSizeLimits(
					calculateXMLDescription(parameter, getWorkItem().getHTMLDescription(), STRING_TYPE_HTML),
					parameter.getIAttribute().getAttributeType());
			getWorkItem().setHTMLDescription(XMLString.createFromXMLText(description));
		} else if (parameter.getAttributeID().equals(IWorkItem.COMMENTS_PROPERTY)) {
			// The comments collection of the work item
			updateComments(parameter);
		} else if (parameter.getAttributeID().equals(IWorkItem.STATE_PROPERTY)) {
			// The state of the work item
			updateState(parameter);
		} else if (parameter.getAttributeID().equals(PSEUDO_ATTRIBUTE_TRIGGER_WORKFLOW_ACTION)) {
			updateWorkFlowAction(parameter);
		} else if (parameter.getAttributeID().equals(IWorkItem.PROJECT_AREA_PROPERTY)) {
			// The project area of the work item - this is not processed. Set
			// the category instead
			throw new WorkItemCommandLineException("Project Area can not be changed, set the workitem category ("
					+ IWorkItem.CATEGORY_PROPERTY + ") instead: " + parameter.getAttributeID() + " !");
		} else if (parameter.getAttributeID().equals(IWorkItem.RESOLUTION_PROPERTY)) {
			// The resolution attribute of the work item
			updateResolution(parameter);
		} else if (parameter.getAttributeID().equals(IWorkItem.SUBSCRIPTIONS_PROPERTY)) {
			// Update the subscribers collection of the work item
			updateSubscribers(parameter, exceptions);
		} else if (parameter.getAttributeID().equals(IWorkItem.TAGS_PROPERTY)) {
			updateBuiltInTags(parameter);
		} else if (StringUtil.hasPrefix(parameter.getAttributeID(), IWorkItem.APPROVALS_PROPERTY)) {
			// The approvals collection of the work item
			// Set is handled as add, unless the switch is specified
			updateApprovals(parameter, exceptions);
		} else if (StringUtil.hasPrefix(parameter.getAttributeID(), PSEUDO_ATTRIBUTE_ATTACHFILE)) {
			// Special handling to allow multiple attachments and still provide
			// unique property ID's
			// Use IWorkItemCommandLineConstants.SWITCH_ENABLE_SET_ATTACHMENT to
			// enable deleting all attachments and only set the one attachment
			updateAttachments(parameter);
		} else if (StringUtil.hasPrefix(parameter.getAttributeID(), PSEUDO_ATTRIBUTE_DELETEATTACHMENTS)) {
			// Special handling to allow deleting all attachments
			// Use IWorkItemCommandLineConstants.SWITCH_ENABLE_SET_ATTACHMENT to
			// enable deleting all attachments and only set the one attachment
			deleteAllAttachments(parameter);
		} else if (StringUtil.hasPrefix(parameter.getAttributeID(), PSEUDO_ATTRIBUTE_LINK)) {
			// Update Links from the work item to other items
			updateLinks(parameter, exceptions);
		} else if (StringUtil.hasPrefix(parameter.getAttributeID(), PSEUDO_ATTRIBUTE_DELETELINKSOFTYPE)) {
			// Delete links from the work item to other items
			deleteLinks(parameter, exceptions);
		} else {
			// Update all other attribute based values of the work item
			// Ignore Errors?
			updateGeneralAttribute(parameter, exceptions);
		}
		// If some errors happened, throw an exception and list the reasons for
		// the exceptions
		throwComplexException(parameter, exceptions);
	}

	/**
	 * Update all attributes that don't need special handling - basically these
	 * are all attribute based values
	 * 
	 * @param parameter
	 * @throws TeamRepositoryException
	 * @throws WorkItemCommandLineException
	 */
	private void updateGeneralAttribute(ParameterValue parameter, List<Exception> exceptions)
			throws TeamRepositoryException, WorkItemCommandLineException {
		// Manage all other attributes by getting the data based on the
		// attribute type.
		//
		// Get the attribute to be modified
		IAttribute theAttribute = parameter.getIAttribute();
		if (theAttribute != null) {
			// check if the work item has the attribute
			if (!getWorkItem().hasAttribute(theAttribute)) {
				if (!isImportIgnoreMissingAttributes()) {
					throw new WorkItemCommandLineException("Attribute not available at work item: "
							+ parameter.getAttributeID() + " Value: '" + parameter.getValue()
							+ "'. Check the work item type or consider synchronizing the attributes.");
				}
				return;
			} else {
				Object result;
				try {
					// Get a representation of the correct object type for
					// the work item attribute
					result = getRepresentation(parameter, exceptions);
				} catch (WorkItemCommandLineException e) {
					throw new WorkItemCommandLineException(
							"Exception getting attribute representation: '" + parameter.getAttributeID() + "' Value: '"
									+ parameter.getValue() + "'.  Original exception: \n" + e.getMessage(),
							e);
				}
				// Finally set the new value
				getWorkItem().setValue(theAttribute, result);
			}
		} else {
			throw new WorkItemCommandLineException(
					"Attribute not found: '" + parameter.getAttributeID() + "' Value: '" + parameter.getValue() + "'.");
		}
	}

	/**
	 * This method tries to get the matching representation of the value to be
	 * set for a work item attribute. It basically goes through a list of
	 * properties an attribute can have and locates the target type. Based on
	 * that type it tries to create a matching value. The value is returned if
	 * it was possible to create it.
	 * 
	 * @param attribute
	 *            - the IAttribute to find the representation for
	 * @param value
	 *            - the string value that is to be transformed.
	 * @return
	 * @throws TeamRepositoryException
	 * @throws WorkItemCommandLineException
	 */
	private Object getRepresentation(ParameterValue parameter, List<Exception> exceptions)
			throws TeamRepositoryException, WorkItemCommandLineException {
		String attribType = parameter.getIAttribute().getAttributeType();
		// Handle list attribute types first
		if (AttributeTypes.isListAttributeType(attribType)) {
			if (AttributeTypes.isItemListAttributeType(attribType)) {
				// Item List Types that are supported
				if (attribType.equals(AttributeTypes.CONTRIBUTOR_LIST)) {
					// A list of contributors
					return calculateContributorList(parameter, exceptions);
				}
				if (attribType.equals(AttributeTypes.PROCESS_AREA_LIST)) {
					// A list of process areas (ProjectArea/TeamArea)
					return calculateProcessAreaList(parameter, exceptions);
				}
				if (attribType.equals(AttributeTypes.PROJECT_AREA_LIST)) {
					// A list of process areas (ProjectAreas)
					return calculateProcessAreaList(parameter, exceptions);
				}
				if (attribType.equals(AttributeTypes.TEAM_AREA_LIST)) {
					// A list of process areas (TeamAreas)
					return calculateProcessAreaList(parameter, exceptions);
				}
				if (attribType.equals(AttributeTypes.WORK_ITEM_LIST)) {
					// A list of work items
					return calculateWorkItemList(parameter, exceptions);
				}
				if (attribType.equals(AttributeTypes.ITEM_LIST)) {
					// ItemList with unspecified IItems
					return calculateItemList(parameter, exceptions);
				}
			}
			if (attribType.equals(AttributeTypes.TAGS)) {
				// Handle Tags - also detected as list type
				return calculateTagList(parameter);
			}
			if (attribType.equals(AttributeTypes.STRING_LIST)) {
				// A list of strings in a collection
				return enforceSizeLimitsStringCollection(calculateStringList(parameter, exceptions), attribType);
			}
			if (AttributeTypes.isEnumerationListAttributeType(attribType)) {
				// Handle all Enumeration List Types
				return calculateEnumerationLiteralList(parameter, exceptions);
			}
			throw new WorkItemCommandLineException(
					"Type not recognized - type not yet supported: " + parameter.getIAttribute().getIdentifier()
							+ " Value: " + parameter.getValue() + " - " + helpGetTypeProperties(attribType));
		} else {
			// Handle non list types - the simple ones first.
			/**
			 * Formats: Provide the requested format.
			 * 
			 * HTML, use HTML tags example
			 * 
			 * "Plain Text <b>Bold Text</b> <i>Italic Text</i> <a
			 * href=\"https://rsjazz.wordpress.com\">External RSJazz Link</a>
			 * <b>@ralph </b>Defect 3 "
			 * 
			 * Use </br>
			 * or <br>
			 * for line breaks, escape quotes with \ e.g. <a
			 * href=\"https://rsjazz.wordpress.com\">External RSJazz Link</a>
			 * 
			 * Wiki, use the Wiki syntax example
			 * 
			 * @see http://www.wikicreole.org/
			 * 
			 *      "<br>
			 *      = Heading1<br>
			 *      <br>
			 *      normal Text\n==Heading 2\n\nNormalText\n===Heading
			 *      3\n\nNormal Text **bold text** <br>
			 *      **bold text**<br>
			 *      //Italics//"
			 * 
			 *      Use <br>
			 *      or \n as line break character
			 * 
			 *      String use normal strings, example
			 *      "test Line1\ntest Line2\ntest Line3\nLine4"
			 * 
			 *      use \n as line break character
			 * 
			 * 
			 */
			if (attribType.equals(AttributeTypes.WIKI)) {
				return calculateStringValue(parameter, STRING_TYPE_WIKI);
			}
			if (AttributeTypes.STRING_TYPES.contains(attribType)) {

				return enforceSizeLimits(calculateStringValue(parameter, STRING_TYPE_PLAINSTRING), attribType);
			}
			if (AttributeTypes.HTML_TYPES.contains(attribType)) {

				return enforceSizeLimits(calculateStringValue(parameter, STRING_TYPE_HTML), attribType);
			}
			// we don't handle add for the following parameter.
			if (parameter.isAdd() || parameter.isRemove()) {
				throw modeNotSupportedException(parameter,
						"Mode not supported for this operation. Single value attributes only support the default and the "
								+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.POSTFIX_PARAMETER_MANIPULATION_MODE
								+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_SET + " modes.");
			}
			if (attribType.equals(AttributeTypes.BOOLEAN)) {
				return new Boolean(parameter.getValue());
			}
			if (AttributeTypes.NUMBER_TYPES.contains(attribType)) {
				// different number types
				try {
					String value = parameter.getValue();
					boolean isEmpty = false;
					if (StringUtil.isEmpty(value)) {
						isEmpty = true;
					}
					if (attribType.equals(AttributeTypes.INTEGER)) {
						return new Integer(value);
					}
					if (attribType.equals(AttributeTypes.LONG)) {
						return new Long(value);
					}
					if (attribType.equals(AttributeTypes.FLOAT)) {
						return new Float(value);
					}
					if (attribType.equals(AttributeTypes.DECIMAL)) {
						return new BigDecimal(value);
					}
					if (attribType.equals(AttributeTypes.DURATION)) {
						if (isEmpty) {
							value = "-1";
						}
						return getDurationFromString(value);
					}
				} catch (NumberFormatException e) {
					throw new WorkItemCommandLineException(
							"Attribute Value not valid - Number format exception: " + parameter.getValue(), e);
				}
			}
			if (attribType.equals(AttributeTypes.DELIVERABLE)) {
				// Handle deliverables - Found In and other attributes
				// referencing a release.
				return calculateDeliverable(parameter);
			}
			if (attribType.equals(AttributeTypes.CATEGORY)) {
				// Work item category - Filed Against and other attributes
				return calculateCategory(parameter);
			}
			if (attribType.equals(AttributeTypes.TIMELINE)) {
				// Iterations - Planned For and other such attributes
				return calculateTimeline(parameter);
			}
			if (attribType.equals(AttributeTypes.ITERATION)) {
				// Iterations - Planned For and other such attributes
				return calculateIteration(parameter);
			}
			if (attribType.equals(AttributeTypes.CONTRIBUTOR)) {
				// Contributors - user ID's
				return calculateContributor(parameter);
			}
			if (attribType.equals(AttributeTypes.TIMESTAMP)) {
				// Timestamp types e.g. dates
				return calculateTimestamp(parameter);
			}
			if (attribType.equals(AttributeTypes.PROJECT_AREA)) {
				// ProjectArea type attributes
				return calculateProcessArea(parameter, TYPE_PROJECT_AREA);
			}
			if (attribType.equals(AttributeTypes.TEAM_AREA)) {
				// TeamArea type attributes
				return calculateProcessArea(parameter, TYPE_TEAM_AREA);
			}
			if (attribType.equals(AttributeTypes.PROCESS_AREA)) {
				// Process Area type attributes (TeamArea/ProjectArea)
				return calculateProcessArea(parameter, TYPE_PROCESS_AREA);
			}
			if (attribType.equals(AttributeTypes.WORK_ITEM)) {
				// Work Item attributes
				return calculateWorkItem(parameter);
			}
			if (attribType.equals(AttributeTypes.ITEM)) {
				// Handle items where the type is not specified in the attribute
				return calculateItem(parameter, exceptions);
			}
			if (attribType.equals(AttributeTypes.UUID)) {
				// Handle UUID type attributes such as restricted access
				return calculateUUID(parameter, exceptions);
			}
			if (AttributeTypes.isEnumerationAttributeType(attribType)) {
				// Handle all enumeration types
				return calculateEnumerationLiteral(parameter);
			}
			// In case we forgot something or a new type gets implemented
			throw new WorkItemCommandLineException(
					"AttributeType not yet supported: " + parameter.getIAttribute().getIdentifier() + " Value: "
							+ parameter.getValue() + " - " + helpGetTypeProperties(attribType));
		}
	}

	/**
	 * Make sure strings in a string list are not longer than the maximum size
	 * that can be stored in the attribute.
	 * 
	 * @param input
	 * @param attribType
	 * @return
	 */
	private Collection<String> enforceSizeLimitsStringCollection(Collection<String> input, String attribType) {
		if (!isEnforceSizeLimits()) {
			return input;
		}

		Collection<String> result = new ArrayList<String>();
		for (String value : input) {
			result.add(enforceSizeLimits(value, attribType));
		}
		return result;
	}

	/**
	 * Make sure strings are not longer than the maximum size that can be stored
	 * in the attribute.
	 * 
	 * @param input
	 * @param attribType
	 * @return
	 */
	private String enforceSizeLimits(String input, String attribType) {
		if (!isEnforceSizeLimits()) {
			return input;
		}
		return truncateString(input, attribType);
	}

	/**
	 * Truncate a string based on the maximum size it can have based on its type
	 * 
	 * @param value
	 * @param attribType
	 * @return
	 */
	private String truncateString(String value, String attribType) {
		Long sizeLimit = Long.MAX_VALUE;
		if (attribType.equals(AttributeTypes.SMALL_STRING)) {
			sizeLimit = IAttribute.MAX_SMALL_STRING_BYTES;
		}
		if (attribType.equals(AttributeTypes.MEDIUM_STRING) || attribType.equals(AttributeTypes.MEDIUM_HTML)
				|| attribType.equals(AttributeTypes.STRING_LIST) || attribType.equals(AttributeTypes.COMMENTS)) {
			sizeLimit = IAttribute.MAX_MEDIUM_STRING_BYTES;
		}
		if (attribType.equals(AttributeTypes.LARGE_STRING) || attribType.equals(AttributeTypes.LARGE_HTML)) {
			sizeLimit = IAttribute.MAX_LARGE_STRING_BYTES;
		}
		if (value.length() >= sizeLimit) {
			int lastpos = sizeLimit.intValue() - (VALUE_TRUNCATED_POSTFIX.length() + XML_GROWTH_CONSTANT);
			value = value.substring(0, lastpos) + VALUE_TRUNCATED_POSTFIX;
		}
		return value;
	}

	/**
	 * Composes an exception form multiple exceptions that happened during e.g.
	 * item lookup and wraps it into one exception
	 * 
	 * @param parameter
	 *            - the parameter worked on at this time
	 * @param exceptions
	 *            - the list of exceptions that happened
	 * @throws WorkItemCommandLineException
	 */
	private void throwComplexException(ParameterValue parameter, List<Exception> exceptions)
			throws WorkItemCommandLineException {
		if (!exceptions.isEmpty()) {
			String exceptionInfo = "";
			for (Exception exception : exceptions) {
				exceptionInfo += "Recoverable Exception getting attribute representation: " + parameter.getAttributeID()
						+ " Value: " + parameter.getValue() + " \n" + exception.getMessage() + "\n";
			}
			throw new WorkItemCommandLineException(exceptionInfo);
		}
	}

	/**
	 * Update the approvals.
	 * 
	 * Mode Default same as mode add - adds an approval
	 * 
	 * Mode set removes approvals first and then adds the new approval This
	 * modes needs to be enabled with a switch
	 * 
	 * Mode remove removes an approval and needs to be enabled with a switch
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @param exceptions
	 *            - a list to pass exceptions back
	 * @throws TeamRepositoryException
	 * @throws WorkItemCommandLineException
	 */
	private void updateApprovals(ParameterValue parameter, List<Exception> exceptions)
			throws TeamRepositoryException, WorkItemCommandLineException {

		boolean enableDeleteApprovals = getParameters()
				.hasSwitch(IWorkItemCommandLineConstants.SWITCH_ENABLE_DELETE_APPROVALS);
		if (parameter.isRemove() || parameter.isSet()) {
			if (!enableDeleteApprovals) {
				throw modeNotSupportedException(parameter,
						"Deletion of Approvals not enabled: " + " use the switch "
								+ IWorkItemCommandLineConstants.PREFIX_SWITCH
								+ IWorkItemCommandLineConstants.SWITCH_ENABLE_DELETE_APPROVALS
								+ " to enable deletion of approvals. Parameter: " + parameter.getAttributeID()
								+ " Value: " + parameter.getValue());
			}
		}
		ApprovalInputData approvalData = new ApprovalInputData(parameter);
		if (parameter.isSet()) {
			updateRemoveAllApprovalsOfSameType(approvalData);
			createApproval(parameter, approvalData);
		} else if (parameter.isRemove()) {
			if (!updateRemoveApproval(approvalData)) {
				exceptions.add(new WorkItemCommandLineException("Remove Approval: approval not found: "
						+ parameter.getAttributeID() + " Value: " + parameter.getValue()));
			}
		} else {
			// Default and add
			createApproval(parameter, approvalData);
		}
	}

	/**
	 * Delete all attachments of a work item
	 * 
	 * @param parameter
	 * 
	 * @throws TeamRepositoryException
	 */
	private void deleteAllAttachments(ParameterValue parameter) throws TeamRepositoryException {

		boolean switchDeleteAttachments = getParameters()
				.hasSwitch(IWorkItemCommandLineConstants.SWITCH_ENABLE_DELETE_ATTACHMENTS);
		if (!PSEUDO_ATTRIBUTEVALUE_YES.equals(parameter.getValue())) {
			throw new WorkItemCommandLineException("Incorrect value: " + parameter.getAttributeID() + " Value: "
					+ parameter.getValue() + helpUsageAttachmentUpload());
		}
		if (!switchDeleteAttachments) {
			throw new WorkItemCommandLineException("Deletion of attachments not enabled: " + " use the switch "
					+ IWorkItemCommandLineConstants.PREFIX_SWITCH
					+ IWorkItemCommandLineConstants.SWITCH_ENABLE_DELETE_ATTACHMENTS
					+ " to enable deletion of attachments.");

		}
		AttachmentUtil.removeAllAttachments(getWorkItem(), getWorkItemCommon(), monitor);
	}

	/**
	 * Update the attachments of a workitem.
	 * 
	 * Supports all modes, however, needs a switch to enable the set mode Mode -
	 * Default same as Add, adds an attachment - Set removes all attachments
	 * first and then adds a new one - Remove removes an attachment, if it can
	 * be found
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @throws TeamRepositoryException
	 */
	private void updateAttachments(ParameterValue parameter) throws TeamRepositoryException {

		boolean switchDeleteAttachments = getParameters()
				.hasSwitch(IWorkItemCommandLineConstants.SWITCH_ENABLE_DELETE_ATTACHMENTS);
		if ((parameter.isSet() || parameter.isRemove()) && !switchDeleteAttachments) {
			throw new WorkItemCommandLineException("Deletion of attachments not enabled: " + " use the switch "
					+ IWorkItemCommandLineConstants.PREFIX_SWITCH
					+ IWorkItemCommandLineConstants.SWITCH_ENABLE_DELETE_ATTACHMENTS
					+ " to enable deletion of attachments. Parameter: " + parameter.getAttributeID() + " Value: "
					+ parameter.getValue());

		}
		List<String> attachmentData = StringUtil.splitStringToList(parameter.getValue(), ATTACHMENT_SEPARATOR);
		if (attachmentData.size() != 4) {
			throw new WorkItemCommandLineException("Incorrect attachment format: " + parameter.getAttributeID()
					+ " Value: " + parameter.getValue() + helpUsageAttachmentUpload());
		}

		String fileName = attachmentData.get(0);
		String description = attachmentData.get(1);
		String contentType = attachmentData.get(2);
		String encoding = attachmentData.get(3);
		if (parameter.isRemove()) {
			AttachmentUtil.removeAttachment(fileName, description, getWorkItem(), getWorkItemCommon(), monitor);
			return;
		} else if (parameter.isSet()) {
			AttachmentUtil.removeAllAttachments(getWorkItem(), getWorkItemCommon(), monitor);
			attachFile(fileName, description, contentType, encoding);
			return;
		} else {
			// Default and add
			attachFile(fileName, description, contentType, encoding);
			return;
		}
	}

	/**
	 * Update the built in tags attribute
	 * 
	 * Supports all modes Mode - Default same as Add, adds a tag - Set removes
	 * all tag first and then adds the new ones - Remove removes a tag, if it
	 * can be found
	 * 
	 * @param parameter
	 *            - the parameter passed
	 */
	private void updateBuiltInTags(ParameterValue parameter) {
		List<String> newTags = getTags(parameter.getValue());
		List<String> oldTags = getWorkItem().getTags2();
		if (parameter.isRemove()) {
			oldTags.removeAll(newTags);
			getWorkItem().setTags2(oldTags);
		} else if (parameter.isSet()) {
			getWorkItem().setTags2(newTags);
		} else {
			// Default and add
			List<String> toAdd = getTagsList(oldTags, newTags);
			oldTags.addAll(toAdd);
			getWorkItem().setTags2(oldTags);
		}
	}

	/**
	 * Update the comments - only adding is supported
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @throws WorkItemCommandLineException
	 * @throws TeamRepositoryException
	 */
	private void updateComments(ParameterValue parameter) throws WorkItemCommandLineException, TeamRepositoryException {
		// Only add comments
		if (!(parameter.isDefault() || parameter.isAdd())) {
			throw modeNotSupportedException(parameter,
					"Mode not supported. Comments only supports the default and the "
							+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.POSTFIX_PARAMETER_MANIPULATION_MODE
							+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_ADD + " modes. ");
		}

		XMLString commentContent = XMLString
				.createFromXMLText(insertLineBreaks(parameter.getValue(), STRING_TYPE_HTML));
		XMLString limitedContent = XMLString.createFromXMLText(
				enforceSizeLimits(commentContent.getXMLText(), parameter.getIAttribute().getAttributeType()));
		IComments comments = getWorkItem().getComments();
		IComment newComment = comments.createComment(getTeamRepository().loggedInContributor(), limitedContent);

		comments.append(newComment);
	}

	/**
	 * Update Links to work items, build results, clm links
	 * 
	 * Supports all modes Mode - Default same as Add, adds a tag - Set removes
	 * all tag first and then adds the new ones - Remove removes a tag, if it
	 * can be found
	 * 
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @param exceptions
	 *            - a list to pass exceptions back
	 * @throws TeamRepositoryException
	 */
	private void updateLinks(ParameterValue parameter, List<Exception> exceptions) throws TeamRepositoryException {
		List<ReferenceData> references;
		boolean setUpdateBackLinks = false;
		String linkType = StringUtil.removePrefix(parameter.getAttributeID(), PSEUDO_ATTRIBUTE_LINK);
		IEndPointDescriptor endpoint = ReferenceUtil.getWorkItemEndPointDescriptorMap().get(linkType);
		if (endpoint == null) {
			endpoint = ReferenceUtil.getCLM_URI_EndPointDescriptorMap().get(linkType);
		}
		if (endpoint == null) {
			endpoint = ReferenceUtil.getCLM_WI_EndPointDescriptorMap().get(linkType);
		}
		if (endpoint == null) {
			endpoint = ReferenceUtil.getBuild_EndPointDescriptorMap().get(linkType);
		}
		if (endpoint == null) {
			// This link type is not in our supported map
			throw new WorkItemCommandLineException(
					"Link Type unknown or not yet supported: " + linkType + helpUsageAllLinks());
		}
		IWorkItemReferences wiReferences = getWorkingCopy().getReferences();
		List<IReference> current = wiReferences.getReferences(endpoint);
		if (parameter.isSet()) {
			// remove all links for this endpoint
			for (IReference iReference : current) {
				getWorkingCopy().getReferences().remove(iReference);
				if (WorkItemLinkTypes.isCalmLink(endpoint)) {
					setUpdateBackLinks = true;
				}
			}
			// To be able to detect the new references
			current.clear();
		}
		// Create the new references
		references = createReferences(linkType, parameter, exceptions);
		// Iterate all references
		for (ReferenceData newReferences : references) {
			IReference foundReference = null;
			for (IReference iReference : current) {
				// We only add if the reference is not already there
				// We delete all references of this type in set mode
				// The code below either deletes all references of this endpoint
				// or it finds a references if it exists to support avoiding
				// duplicates.
				if (iReference.sameDetailsExcludingCommentAs(newReferences.getReference())) {
					// Otherwise try to find a reference
					// found the reference
					foundReference = iReference;
					break;
				}
			}
			// In Default (and add mode) we add only if the reference was not
			// found in set mode we have noting found and add also
			if (parameter.isDefault() || parameter.isAdd() || parameter.isSet()) {
				if (foundReference == null) {
					getWorkingCopy().getReferences().add(newReferences.getEndPointDescriptor(),
							newReferences.getReference());
					if (WorkItemLinkTypes.isCalmLink(newReferences.getEndPointDescriptor())) {
						setUpdateBackLinks = true;
					}
				}
			} else if (parameter.isRemove()) {
				// the same reference is already there if in remove mode, remove
				// it. We don't check if the reference was there in the first
				// place
				if (foundReference != null) {
					getWorkingCopy().getReferences().remove(foundReference);
					if (WorkItemLinkTypes.isCalmLink(newReferences.getEndPointDescriptor())) {
						setUpdateBackLinks = true;
					}
				}
			}
		}
		if (setUpdateBackLinks) {
			updateBacklinks();
		}
	}

	/**
	 * Delete all links of a certain link type
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @param exceptions
	 *            - a list to pass exceptions back
	 * @throws TeamRepositoryException
	 */
	private void deleteLinks(ParameterValue parameter, List<Exception> exceptions) throws TeamRepositoryException {

		boolean checkDangling = false;
		boolean update = false;
		if (PSEUDO_ATTRIBUTEVALUE_DELETEDANGLING.equals(parameter.getValue())) {
			checkDangling = true;
		}
		if (!checkDangling && !PSEUDO_ATTRIBUTEVALUE_YES.equals(parameter.getValue())) {
			throw new WorkItemCommandLineException("Incorrect value: " + parameter.getAttributeID() + " Value: "
					+ parameter.getValue() + helpUsageAllLinks());
		}
		String linkType = StringUtil.removePrefix(parameter.getAttributeID(), PSEUDO_ATTRIBUTE_DELETELINKSOFTYPE);
		IEndPointDescriptor endpoint = ReferenceUtil.getWorkItemEndPointDescriptorMap().get(linkType);
		if (endpoint == null) {
			endpoint = ReferenceUtil.getCLM_URI_EndPointDescriptorMap().get(linkType);
		}
		if (endpoint == null) {
			endpoint = ReferenceUtil.getCLM_WI_EndPointDescriptorMap().get(linkType);
		}
		if (endpoint == null) {
			endpoint = ReferenceUtil.getBuild_EndPointDescriptorMap().get(linkType);
		}
		if (endpoint == null) {
			// This link type is not in our supported map
			throw new WorkItemCommandLineException(
					"Link Type unknown or not yet supported: " + linkType + helpUsageAllLinks());
		}

		IWorkItemReferences wiReferences = getWorkingCopy().getReferences();
		List<IReference> current = wiReferences.getReferences(endpoint);
		for (IReference iReference : current) {
			if (checkDangling) {
				if (isDangling(endpoint, iReference)) {
					getWorkingCopy().getReferences().remove(iReference);
					update = true;
				}
			} else {
				getWorkingCopy().getReferences().remove(iReference);
				update = true;
			}
		}
		if (update & WorkItemLinkTypes.isCalmLink(endpoint)) {
			// we want to create back links when needed
			updateBacklinks();
		}
	}

	/**
	 * Check if a link is dangling
	 * 
	 * @param endpoint
	 * @param iReference
	 * @return
	 */
	private boolean isDangling(IEndPointDescriptor endpoint, IReference iReference) {
		// TODO to be implemented
		// URI uri = iReference.createURI();
		return false;
	}

	/**
	 * Update backlinks, add the additional safe parameter only once.
	 */
	private void updateBacklinks() {
		if (!fupdateBacklinks) {
			getWorkingCopy().getAdditionalSaveParameters().add(IAdditionalSaveParameters.UPDATE_BACKLINKS);
			fupdateBacklinks = true;
		}
	}

	/**
	 * Removes all approvals from the approvals collection
	 * 
	 * @param approvalData
	 *            the data with the approval type to remove
	 * @return
	 */
	private void updateRemoveAllApprovalsOfSameType(ApprovalInputData approvalData) {
		IApprovals approvals = getWorkItem().getApprovals();
		List<IApproval> approvalContent = approvals.getContents();
		for (IApproval anApproval : approvalContent) {
			IApprovalDescriptor descriptor = anApproval.getDescriptor();
			if (descriptor != null && descriptor.getTypeIdentifier().equals(approvalData.getApprovalType())) {
				// The order is important
				approvals.remove(anApproval);
				approvals.remove(descriptor);
			}
		}
	}

	/**
	 * Tries to find a an approval from the approval data and deletes it if it
	 * can be found Removes only the first instance found that matches.
	 * 
	 * @param approvalData
	 * @return
	 */
	private boolean updateRemoveApproval(ApprovalInputData approvalData) {
		IApprovals approvals = getWorkItem().getApprovals();
		List<IApproval> approvalContent = approvals.getContents();
		for (IApproval anApproval : approvalContent) {
			IApprovalDescriptor descriptor = anApproval.getDescriptor();
			if (descriptor.getTypeIdentifier().equals(approvalData.getApprovalType())
					&& descriptor.getName().equals(approvalData.getApprovalName())) {
				approvals.remove(anApproval);
				approvals.remove(descriptor);
				return true;
			}
		}
		return false;
	}

	/**
	 * Updates the built in resolution attribute
	 * 
	 * @param parameter
	 * @throws WorkItemCommandLineException
	 * @throws TeamRepositoryException
	 */
	private void updateResolution(ParameterValue parameter)
			throws WorkItemCommandLineException, TeamRepositoryException {
		Identifier<IResolution> resolution = null;
		if (!(parameter.isDefault() || parameter.isSet())) {
			throw new WorkItemCommandLineException("Comments only supports the default and the "
					+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.POSTFIX_PARAMETER_MANIPULATION_MODE
					+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_SET + " modes: "
					+ parameter.getAttributeID() + " !");
		}
		String resolutionValue = parameter.getValue();
		if (resolutionValue != null) {
			try {
				resolution = findResolution(parameter.getValue());
			} catch (Exception e) {
				throw new WorkItemCommandLineException(
						"Resolution not found: " + parameter.getAttributeID() + " Value: " + parameter.getValue());
			}
		}
		getWorkItem().setResolution2(resolution);
	}

	/**
	 * Update the subscriptions to a work item Mode set: Clear the current
	 * subscribers and set the given list Mode default and mode add: add a list
	 * of subscribers Mode remove: remove the specified subscribers.
	 * 
	 * Subscriptions make sure no duplicates are set.
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @param exceptions
	 *            - a list to pass exceptions back
	 * @throws TeamRepositoryException
	 */
	private void updateSubscribers(ParameterValue parameter, List<Exception> exceptions)
			throws TeamRepositoryException {
		List<String> notFoundList = new ArrayList<String>();
		HashMap<String, IContributor> subscriberList = getContributors(parameter.getValue(), ITEM_SEPARATOR,
				notFoundList);
		if (parameter.isSet()) {
			// set - remove subscribers first
			IContributorHandle[] subscribed = getWorkItem().getSubscriptions().getContents();
			for (IContributorHandle removeContributor : subscribed) {
				getWorkItem().getSubscriptions().remove(removeContributor);
			}
		}
		for (IContributor subscriber : subscriberList.values()) {
			if (parameter.isRemove()) {
				getWorkItem().getSubscriptions().remove(subscriber);
			} else {
				// default and add
				getWorkItem().getSubscriptions().add(subscriber);
			}
		}
		if (!notFoundList.isEmpty()) {
			exceptions.add(new WorkItemCommandLineException("Subscriber not found: '" + parameter.getAttributeID()
					+ "' Subscribers: " + helpGetDisplayStringFromList(notFoundList)));
		}
	}

	/**
	 * Update the state of a work item - only setting a state is supported
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @throws WorkItemCommandLineException
	 * @throws TeamRepositoryException
	 */
	private void updateState(ParameterValue parameter) throws WorkItemCommandLineException, TeamRepositoryException {
		if (!(parameter.isDefault() || parameter.isSet())) {
			throw modeNotSupportedException(parameter,
					"Mode not supported for this operation. State change only supports the default and the "
							+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.POSTFIX_PARAMETER_MANIPULATION_MODE
							+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_SET + " mode.");
		}
		setState(parameter);
	}

	/**
	 * Update the state of a work item using a workflow action - only setting a
	 * state is supported
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @throws WorkItemCommandLineException
	 * @throws TeamRepositoryException
	 */
	private void updateWorkFlowAction(ParameterValue parameter)
			throws WorkItemCommandLineException, TeamRepositoryException {
		// The state of the work item
		if (!(parameter.isDefault() || parameter.isSet())) {
			throw modeNotSupportedException(parameter,
					"Mode not supported for this operation. State change only supports the default and the "
							+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.POSTFIX_PARAMETER_MANIPULATION_MODE
							+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_SET + " modes.");
		}
		setWorkFlowAction(parameter);
	}

	/**
	 * Find the category specified in the value
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @return the category found
	 * @throws TeamRepositoryException
	 */
	private Object calculateCategory(ParameterValue parameter) throws TeamRepositoryException {
		ICategoryHandle category = findCategory(parameter.getValue());
		if (category == null) {
			throw new WorkItemCommandLineException("Category not found: '" + parameter.getIAttribute().getIdentifier()
					+ "' Value: '" + parameter.getValue() + "'.");
		}
		return category;
	}

	/**
	 * Find a contributor for a given ID
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @return the contributor
	 * @throws TeamRepositoryException
	 */
	private Object calculateContributor(ParameterValue parameter) throws TeamRepositoryException {
		IContributor user = findContributorFromIDorName(parameter.getValue().trim());
		if (user == null) {
			throw new WorkItemCommandLineException("Contributor ID not found: '" + parameter.getValue() + "'.");
		}
		return user;
	}

	/**
	 * Calculates a list of contributors from a list of ID's and adjusts the
	 * value content as needed.
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @param exceptions
	 *            - a list to pass exceptions back
	 * @return a list of IContributors as object as a side effect it also
	 *         returns a list of errors if an argument could not be processed
	 * @throws TeamRepositoryException
	 */
	private Object calculateContributorList(ParameterValue parameter, List<Exception> exceptions)
			throws TeamRepositoryException {
		if (StringUtil.isEmpty(parameter.getValue())) {
			return new ArrayList<Object>(0); // empty list
		}
		List<String> notFoundList = new ArrayList<String>();
		HashMap<String, IContributor> foundItems = getContributors(parameter.getValue(), ITEM_SEPARATOR, notFoundList);
		if (!notFoundList.isEmpty()) {
			exceptions.add(new WorkItemCommandLineException(
					"Contributors not found: '" + parameter.getIAttribute().getIdentifier() + "' Contributors: "
							+ helpGetDisplayStringFromList(notFoundList)));
		}

		if (parameter.isSet()) {
			// Set the value to the new list
			return foundItems.values();
		}
		List<Object> results = new ArrayList<Object>();
		Object current = getWorkItem().getValue(parameter.getIAttribute());
		if (!(current instanceof List<?>)) {
			throw new WorkItemCommandLineException(
					"Attribute type not a list attribute type, can not calculate current value: '"
							+ parameter.getIAttribute().getIdentifier() + "'.");
		}
		List<?> currentList = (List<?>) current;
		for (Object currentObject : currentList) {
			if (!(currentObject instanceof IItemHandle)) {
				exceptions.add(incompatibleAttributeValueTypeException(parameter,
						"Reading List Attribute value (" + currentObject.toString() + ") "));
			}
			IItemHandle currentHandle = (IItemHandle) currentObject;
			if (!foundItems.containsKey(currentHandle.getItemId().getUuidValue())) {
				// did not find the current element in the input list I need to
				// add it to the result
				// for add: it is not in the found list and needed. The found
				// list is added later
				// for remove: it is not in the list of items to remove and
				// needed
				// I need to add this
				results.add(currentHandle);
			}
		}
		if (parameter.isAdd() || parameter.isDefault()) {
			// add all the results found, no duplicates, because we did only add
			// items we don't have in our list
			results.addAll(foundItems.values());
		}
		return results;
	}

	/**
	 * Find a deliverable from the string representation.
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @return the deliverable that was found
	 * @throws TeamRepositoryException
	 */
	private Object calculateDeliverable(ParameterValue parameter) throws TeamRepositoryException {
		if (StringUtil.isEmpty(parameter.getValue())) {
			return null; // Unassigned
		}
		IDeliverable result = findDeliverable(parameter.getValue());
		if (null == result) {
			throw new WorkItemCommandLineException("Deliverable not found: '"
					+ parameter.getIAttribute().getIdentifier() + "' Value: '" + parameter.getValue() + "'.");
		}
		return result;
	}

	/**
	 * Find an enumeration literal for a value
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @return the enumeration literal found
	 * @throws TeamRepositoryException
	 */
	private Object calculateEnumerationLiteral(ParameterValue parameter) throws TeamRepositoryException {
		if (StringUtil.isEmpty(parameter.getValue())) {
			return null; // Unassigned
		}
		Identifier<? extends ILiteral> result = getEnumerationLiteralEqualsStringOrID(parameter.getIAttribute(),
				parameter.getValue());
		if (null == result) {
			throw new WorkItemCommandLineException("Enumeration literal could not be resolved: '"
					+ parameter.getIAttribute().getIdentifier() + "' Value: '" + parameter.getValue() + "'.");
		} else {
			return result;
		}
	}

	/**
	 * Tries to find a list of enumeration literals for a list of values encoded
	 * in a string.
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @param exceptions
	 *            - a list to pass exceptions back
	 * @return a list of enumeration literals
	 * @throws TeamRepositoryException
	 */
	private Object calculateEnumerationLiteralList(ParameterValue parameter, List<Exception> exceptions)
			throws TeamRepositoryException {
		if (StringUtil.isEmpty(parameter.getValue())) {
			return new ArrayList<Object>(0); // empty list
		}
		List<String> values = StringUtil.splitStringToList(parameter.getValue(), ITEM_SEPARATOR);
		HashMap<String, Identifier<? extends ILiteral>> foundItems = new HashMap<String, Identifier<? extends ILiteral>>();

		for (String displayValue : values) {
			Identifier<? extends ILiteral> result = getEnumerationLiteralEqualsStringOrID(parameter.getIAttribute(),
					displayValue);
			if (null == result) {
				exceptions.add(new WorkItemCommandLineException("Enumeration literal could not be resolved: '"
						+ parameter.getIAttribute().getIdentifier() + "' Value: " + displayValue));
			} else {
				foundItems.put(result.getStringIdentifier(), result);
			}
		}
		if (parameter.isSet()) {
			// Set the value to the new list
			return foundItems.values();
		}
		List<Object> results = new ArrayList<Object>();
		Object current = getWorkItem().getValue(parameter.getIAttribute());
		if (!(current instanceof List<?>)) {
			throw new WorkItemCommandLineException(
					"Attribute type not a list attribute type, can not calculate current value: '"
							+ parameter.getIAttribute().getIdentifier() + "'.");
		}
		List<?> currentList = (List<?>) current;
		for (Object currentObject : currentList) {
			if (!(currentObject instanceof Identifier<?>)) {
				exceptions.add(incompatibleAttributeValueTypeException(parameter,
						"Reading List Attribute value (" + currentObject.toString() + ") "));
			}
			Identifier<?> currentIdentifier = (Identifier<?>) currentObject;
			if (!foundItems.containsKey(currentIdentifier.getStringIdentifier())) {
				// did not find the current element in the input list I need to
				// add it to the result
				// for add: it is not in the found list and needed. The found
				// list is added later
				// for remove: it is not in the list of items to remove and
				// needed
				// I need to add this
				results.add(currentIdentifier);
			}
		}
		if (parameter.isAdd() || parameter.isDefault()) {
			// add all the results found, no duplicates, because we did only add
			// items we don't have in our list
			results.addAll(foundItems.values());
		}
		return results;
	}

	/**
	 * Tries to find an item for an unspecified item attribute type and returns
	 * the object, if found.
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @param exceptions
	 *            - a list to pass exceptions back
	 * @return the IItem found
	 * @throws WorkItemCommandLineException
	 * @throws TeamRepositoryException
	 */
	private Object calculateItem(ParameterValue parameter, List<Exception> exceptions)
			throws WorkItemCommandLineException, TeamRepositoryException {
		if (StringUtil.isEmpty(parameter.getValue())) {
			return null; // Unassigned
		}
		List<String> value = StringUtil.splitStringToList(parameter.getValue(), ITEMTYPE_SEPARATOR);
		if (value.size() != 2) {
			throw new WorkItemCommandLineException(
					"Unrecognizable encoding: '" + parameter.getIAttribute().getIdentifier() + "' Value: '"
							+ parameter.getValue() + " - " + helpUsageUnspecifiedItemValues() + "'.");
		}
		String itemType = value.get(0);
		String itemValue = value.get(1).trim();
		parameter.setValue(itemValue);
		if (itemType.equals(TYPE_PROJECT_AREA) || itemType.equals(TYPE_TEAM_AREA)
				|| itemType.equals(TYPE_PROCESS_AREA)) {
			return calculateProcessArea(parameter, itemType);
		}
		if (itemType.equals(TYPE_CATEGORY)) {
			return calculateCategory(parameter);
		}
		if (itemType.equals(TYPE_CONTRIBUTOR)) {
			return calculateContributor(parameter);
		}
		if (itemType.equals(TYPE_TIMELINE)) {
			return calculateTimeline(parameter);
		}
		if (itemType.equals(TYPE_ITERATION)) {
			return calculateIteration(parameter);
		}
		if (itemType.equals(TYPE_WORKITEM)) {
			return calculateWorkItem(parameter);
		}
		if (itemType.equals(TYPE_SCM_COMPONENT)) {
			return calculateSCMComponent(parameter, exceptions);
		}
		throw new WorkItemCommandLineException(
				"Unrecognized item type (" + itemType + ") : ID '" + parameter.getIAttribute().getIdentifier()
						+ "' Value: '" + parameter.getValue() + "' - " + helpUsageUnspecifiedItemValues());
	}

	/**
	 * Calculates the items in an unspecified ItemList
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @param exceptions
	 *            - a list to pass exceptions back
	 * @return returns a list with the values
	 * @throws TeamRepositoryException
	 */
	private Object calculateItemList(ParameterValue parameter, List<Exception> exceptions)
			throws TeamRepositoryException {
		if (StringUtil.isEmpty(parameter.getValue())) {
			return new ArrayList<Object>(0); // empty list
		}
		String originalInputValue = parameter.getValue();
		HashMap<String, Object> foundItems = new HashMap<String, Object>();
		List<String> items = StringUtil.splitStringToList(originalInputValue, ITEM_SEPARATOR);
		for (String itemSpecification : items) {
			// To be able to use the functionality of calculateItem, we need to
			// rewrite the parameter value so that it only contains one value
			// specification

			parameter.setValue(itemSpecification);
			try {
				Object item = calculateItem(parameter, exceptions);
				if (item instanceof IItemHandle) {
					IItemHandle anItemHandle = (IItemHandle) item;
					foundItems.put(anItemHandle.getItemId().getUuidValue(), anItemHandle);
				} else {
					exceptions.add(incompatibleAttributeValueTypeException(parameter,
							"Incompatible value type found computing List Attribute value (" + item.toString() + ") "));
				}
			} catch (WorkItemCommandLineException e) {
				exceptions.add(e);
			}
		}
		if (parameter.isSet()) {
			return foundItems.values();
		}
		List<Object> results = new ArrayList<Object>();
		Object current = getWorkItem().getValue(parameter.getIAttribute());
		if (!(current instanceof List<?>)) {
			throw incompatibleAttributeValueTypeException(parameter,
					"Attribute type not a list attribute type, can not calculate current value");
		}
		List<?> currentList = (List<?>) current;
		for (Object currentObject : currentList) {
			if (!(currentObject instanceof IItemHandle)) {
				exceptions.add(incompatibleAttributeValueTypeException(parameter,
						"Incompatible value type found computing List Attribute value (" + currentObject.toString()
								+ ") "));
			}
			IItemHandle currentHandle = (IItemHandle) currentObject;
			if (!foundItems.containsKey(currentHandle.getItemId().getUuidValue())) {
				// did not find the current element in the input list I need to
				// add it to the result
				// for add: it is not in the found list and needed. The found
				// list is added later
				// for remove: it is not in the list of items to remove and
				// needed
				// I need to add this
				results.add(currentHandle);
			}
		}
		if (parameter.isAdd() || parameter.isDefault()) {
			// add all the results found, no duplicates, because we did only add
			// items we don't have in our list
			results.addAll(foundItems.values());
		}
		return results;
	}

	/**
	 * Find an timelinen from a string encoding the value
	 * 
	 * @param parameter
	 * @return
	 * @throws TeamRepositoryException
	 */
	private Object calculateTimeline(ParameterValue parameter) throws TeamRepositoryException {
		String timeline = parameter.getValue();
		if (StringUtil.isEmpty(timeline)) {
			return null; // Unassigned
		}
		List<String> path = StringUtil.splitStringToList(parameter.getValue(), PATH_SEPARATOR);
		DevelopmentLineHelper dh = new DevelopmentLineHelper(getTeamRepository(), monitor);
		IProjectAreaHandle projectArea = getWorkItem().getProjectArea();
		IDevelopmentLine developmentLine = dh.findDevelopmentLine(projectArea, path, DevelopmentLineHelper.BYID);
		if (developmentLine == null) { // find by label if find by ID fails
			developmentLine = dh.findDevelopmentLine(projectArea, path, DevelopmentLineHelper.BYLABEL);
		}
		if (developmentLine == null) {
			throw new WorkItemCommandLineException("Timeline not found: '" + parameter.getIAttribute().getIdentifier()
					+ "' Value: '" + parameter.getValue() + "'.");
		}
		return developmentLine;
	}

	/**
	 * Find an iteration from a string encoding the value
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @return the iteration found
	 * @throws TeamRepositoryException
	 */
	private Object calculateIteration(ParameterValue parameter) throws TeamRepositoryException {
		if (StringUtil.isEmpty(parameter.getValue())) {
			return null; // Unassigned
		}
		List<String> path = StringUtil.splitStringToList(parameter.getValue(), PATH_SEPARATOR);
		DevelopmentLineHelper dh = new DevelopmentLineHelper(getTeamRepository(), monitor);
		IProjectAreaHandle projectArea = getWorkItem().getProjectArea();
		IIteration iteration = dh.findIteration(projectArea, path, DevelopmentLineHelper.BYID);
		if (iteration == null) { // find by label if find by ID fails
			iteration = dh.findIteration(projectArea, path, DevelopmentLineHelper.BYLABEL);
		}
		if (iteration == null) {
			throw new WorkItemCommandLineException("Iteration not found: '" + parameter.getIAttribute().getIdentifier()
					+ "' Value: '" + parameter.getValue() + "'.");
		}
		if (!iteration.hasDeliverable()) {
			throw new WorkItemCommandLineException(
					"Iteration has no deliverable planned (A release is scheduled for this iteration): '"
							+ parameter.getIAttribute().getIdentifier() + "' Value: '" + parameter.getValue() + "'.");
		}
		return iteration;
	}

	/**
	 * Find a process area from the name in the value string.
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @return the process area that was found
	 * @throws TeamRepositoryException
	 */
	private IProcessArea calculateProcessArea(ParameterValue parameter, String areaType)
			throws TeamRepositoryException {
		if (StringUtil.isEmpty(parameter.getValue())) {
			return null; // Unassigned
		}
		IProcessArea processArea = null;
		IProcessArea area = ProcessAreaUtil.findProcessAreaByFQN(parameter.getValue().trim(), getProcessClientService(),
				monitor);
		if (area == null) {
			throw new WorkItemCommandLineException(areaType + " not found '" + parameter.getIAttribute().getIdentifier()
					+ "' Value: '" + parameter.getValue() + "'.");
		}
		if (areaType.equals(TYPE_PROJECT_AREA)) {
			if (area instanceof IProjectArea) {
				processArea = area;
			} else {
				throw new WorkItemCommandLineException("Process Areas found but was not Project Area: '"
						+ parameter.getAttributeID() + "' Area Name: '" + parameter.getValue() + "'.");
			}
		} else if (areaType.equals(TYPE_TEAM_AREA)) {
			if (area instanceof ITeamArea) {
				processArea = area;
			} else {
				new WorkItemCommandLineException("Process Areas found but was not Team Area: '"
						+ parameter.getAttributeID() + "' Area Name: '" + parameter.getValue() + "'.");
			}
		} else {
			// general process area
			processArea = area;
		}
		return processArea;
	}

	/**
	 * Find a list of process areas from the names in the value string. What
	 * type of process area, Team Area/Project Area/both is determined from the
	 * attribute type
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @param exceptions
	 *            - a list to pass exceptions back
	 * @return a list of process areas
	 * @throws TeamRepositoryException
	 */
	private Object calculateProcessAreaList(ParameterValue parameter, List<Exception> exceptions)
			throws TeamRepositoryException {
		if (StringUtil.isEmpty(parameter.getValue())) {
			return new ArrayList<Object>(0); // empty list
		}
		String areaType = TYPE_PROCESS_AREA;
		if (parameter.getIAttribute().getAttributeType().equals(AttributeTypes.PROJECT_AREA_LIST)) {
			areaType = TYPE_PROJECT_AREA;
		} else if (parameter.getIAttribute().getAttributeType().equals(AttributeTypes.TEAM_AREA_LIST)) {
			areaType = TYPE_TEAM_AREA;
		}
		HashMap<String, IProcessArea> foundItems = new HashMap<String, IProcessArea>();
		List<String> notFoundList = new ArrayList<String>();
		List<String> processAreaNames = StringUtil.splitStringToList(parameter.getValue(), ITEM_SEPARATOR);
		for (String processAreaName : processAreaNames) {
			processAreaName = processAreaName.trim();
			IProcessArea area = ProcessAreaUtil.findProcessAreaByFQN(processAreaName, getProcessClientService(),
					monitor);
			if (area == null) {
				notFoundList.add(processAreaName);
			} else {
				if (areaType.equals(TYPE_PROJECT_AREA)) {
					if (area instanceof IProjectArea) {
						foundItems.put(area.getItemId().getUuidValue(), area);
					} else {
						exceptions
								.add(new WorkItemCommandLineException("Process Areas found but was not Project Area: '"
										+ parameter.getAttributeID() + "' Area Name: '" + processAreaName + "'."));
					}
				} else if (areaType.equals(TYPE_TEAM_AREA)) {
					if (area instanceof ITeamArea) {
						foundItems.put(area.getItemId().getUuidValue(), area);
					} else {
						exceptions.add(new WorkItemCommandLineException("Process Areas found but was not Team Area: '"
								+ parameter.getAttributeID() + "' Area Name: '" + processAreaName + "'."));
					}
				} else {
					// general process area
					foundItems.put(area.getItemId().getUuidValue(), area);
				}
			}
		}
		if (!notFoundList.isEmpty()) {
			exceptions.add(new WorkItemCommandLineException("Process Areas not found: '" + parameter.getAttributeID()
					+ "' process areas: '" + helpGetDisplayStringFromList(notFoundList) + "'."));
		}
		if (parameter.isSet()) {
			// Set the value to the new list
			return foundItems.values();
		}
		List<Object> results = new ArrayList<Object>();
		Object current = getWorkItem().getValue(parameter.getIAttribute());
		if (!(current instanceof List<?>)) {
			throw new WorkItemCommandLineException(
					"Attribute type not a list attribute type, can not calculate current value: '"
							+ parameter.getIAttribute().getIdentifier() + "'.");
		}
		List<?> currentList = (List<?>) current;
		for (Object currentObject : currentList) {
			if (!(currentObject instanceof IItemHandle)) {
				exceptions.add(incompatibleAttributeValueTypeException(parameter,
						"Incompatible value type found computing List Attribute value (" + currentObject.toString()
								+ ") "));
			}
			IItemHandle currentHandle = (IItemHandle) currentObject;
			if (!foundItems.containsKey(currentHandle.getItemId().getUuidValue())) {
				// did not find the current element in the input list I need to
				// add it to the result
				// for add: it is not in the found list and needed. The found
				// list is added later
				// for remove: it is not in the list of items to remove and
				// needed
				// I need to add this
				results.add(currentHandle);
			}
		}
		if (parameter.isAdd() || parameter.isDefault()) {
			// add all the results found, no duplicates, because we did only add
			// items we don't have in our list
			results.addAll(foundItems.values());
		}
		return results;
	}

	/**
	 * Tries to find an SCM component by name
	 * 
	 * If components with the same name exist, it returns the first one found
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @param exceptions
	 *            - a list to pass exceptions back
	 * @return the component found
	 * @throws TeamRepositoryException
	 */
	private Object calculateSCMComponent(ParameterValue parameter, List<Exception> exceptions)
			throws TeamRepositoryException {
		// TODO: Null?
		IWorkspaceManager wm = SCMPlatform.getWorkspaceManager(getTeamRepository());
		IComponentSearchCriteria criteria = IComponentSearchCriteria.FACTORY.newInstance();
		criteria.setExactName(parameter.getValue());
		List<IComponentHandle> found = wm.findComponents(criteria, Integer.MAX_VALUE, monitor);
		if (found.size() == 0) {
			new WorkItemCommandLineException("SCM Component not found : '" + parameter.getIAttribute().getIdentifier()
					+ "' Value: '" + parameter.getValue() + "'.");
		}
		if (found.size() > 1) {
			exceptions.add(new WorkItemCommandLineException("Ambigious SCM Component name - returning first hit : '"
					+ parameter.getIAttribute().getIdentifier() + "' Value: '" + parameter.getValue() + "'."));
		}
		return found.get(0);
	}

	/**
	 * Calculates a string value to return to set string based attributes.
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @param stringType
	 * @return the string to return
	 * @throws TeamRepositoryException
	 */
	private String calculateStringValue(ParameterValue parameter, String stringType) throws TeamRepositoryException {

		if (parameter.isRemove()) {
			throw modeNotSupportedException(parameter, "Mode not supported for this operation.");
		}
		if (parameter.isAdd()) {
			Object current = getWorkItem().getValue(parameter.getIAttribute());
			if (current instanceof String) {
				String content = (String) current;
				return content.concat(insertLineBreaks(parameter.getValue(), stringType));
			} else {
				throw incompatibleAttributeValueTypeException(parameter, "Set String value.");
			}
		}
		// default and set mode
		return insertLineBreaks(parameter.getValue(), stringType);
	}

	/**
	 * Calculate a string list from input variables and the current value
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @param exceptions
	 *            - a list to pass exceptions back
	 * @return a list with strings to be set
	 * @throws TeamRepositoryException
	 */
	private Collection<String> calculateStringList(ParameterValue parameter, List<Exception> errors)
			throws TeamRepositoryException {
		if (StringUtil.isEmpty(parameter.getValue())) {
			return new ArrayList<String>(0); // empty list
		}
		List<String> foundItems = StringUtil.splitStringToList(parameter.getValue(), ITEM_SEPARATOR);
		if (parameter.isSet()) {
			return foundItems;
		}
		// Get the current values and compute the current list
		Object current = getWorkItem().getValue(parameter.getIAttribute());
		if (!(current instanceof List<?>)) {
			throw new WorkItemCommandLineException(
					"Attribute type not a String list attribute type, can not calculate current value: '"
							+ parameter.getIAttribute().getIdentifier() + "'.");
		}
		HashSet<String> results = new HashSet<String>();
		List<?> currentList = (List<?>) current;
		for (Object object : currentList) {
			if (object instanceof String) {
				results.add((String) object);
			}
		}
		if (parameter.isAdd() || parameter.isDefault()) {
			// Add the new values
			// HashSet makes sure we don't get duplicates
			results.addAll(foundItems);
		} else if (parameter.isRemove()) {
			// remove the value from the current list
			for (String value : foundItems) {
				results.remove(value);
			}
		}
		return results;
	}

	/**
	 * Calculate a list of tags from an attribute value and a list of tag names
	 * encoded in a string.
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @return the list of tags
	 * @throws TeamRepositoryException
	 */
	private Object calculateTagList(ParameterValue parameter) throws TeamRepositoryException {
		if (StringUtil.isEmpty(parameter.getValue())) {
			return new ArrayList<Object>(0); // empty list
		}
		SeparatedStringList tags = (SeparatedStringList) getWorkItem().getValue(parameter.getIAttribute());
		List<String> newTags = getTags(parameter.getValue());
		if (parameter.isRemove()) {
			tags.removeAll(newTags);
		} else if (parameter.isSet()) {
			tags.clear();
			tags.addAll(newTags);
		} else if (parameter.isAdd() || parameter.isDefault()) {
			List<String> theTags = getTagsList(tags, newTags);
			tags.clear();
			tags.addAll(theTags);
		}
		return new SeparatedStringList(tags);
	}

	/**
	 * Calculates a timestamp from a string. Format @see SimpleDateFormatUtil
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @return the timestamp that was calculated
	 * @throws TeamRepositoryException
	 */
	private Object calculateTimestamp(ParameterValue parameter) throws TeamRepositoryException {
		if (StringUtil.isEmpty(parameter.getValue())) {
			return null; // Unassigned
		}
		try {
			String value = parameter.getValue();
			if (IWorkItemCommandLineConstants.UNASSIGNED.equals(value) || StringUtil.isEmpty(value)) {
				return null;
			}
			return SimpleDateFormatUtil.createTimeStamp(value,
					SimpleDateFormatUtil.SIMPLE_DATE_FORMAT_PATTERN_YYYY_MM_DD_HH_MM_SS_Z);
		} catch (IllegalArgumentException e) {
			throw new WorkItemCommandLineException("Wrong Timestamp format: "
					+ parameter.getIAttribute().getIdentifier() + " Value: '" + parameter.getValue() + "' use format: "
					+ SimpleDateFormatUtil.SIMPLE_DATE_FORMAT_PATTERN_YYYY_MM_DD_HH_MM_SS_Z);
		}
	}

	/**
	 * Get an UUID from a process area or access group - this is typically used
	 * for restricted access
	 * 
	 * @param parameter
	 * @param exceptions
	 * @return
	 * @throws TeamRepositoryException
	 */
	private UUID calculateUUID(ParameterValue parameter, List<Exception> exceptions) throws TeamRepositoryException {
		// can not be null
		UUID accessContext = AccessContextUtil.getAccessContextFromFQN(parameter.getValue(), getTeamRepository(),
				getAuditableCommon(), getProcessClientService(), null);
		if (accessContext == null) {
			throw new WorkItemCommandLineException("UUID not found: '" + parameter.getIAttribute().getIdentifier()
					+ "' Value: '" + parameter.getValue() + "'.");

		}
		return accessContext;
	}

	/**
	 * Find a work item from an ID
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @return the work item found
	 * @throws TeamRepositoryException
	 * @throws WorkItemCommandLineException
	 */
	private Object calculateWorkItem(ParameterValue parameter)
			throws TeamRepositoryException, WorkItemCommandLineException {
		if (StringUtil.isEmpty(parameter.getValue())) {
			return null; // Unassigned
		}
		List<String> notFoundList = new ArrayList<String>();
		HashMap<String, IWorkItem> workItems = findWorkItemsByIDValues(parameter.getValue(), notFoundList,
				ITEM_SEPARATOR);
		Collection<IWorkItem> workItemList = workItems.values();
		if (workItemList.size() > 0) {
			// In this case we only search for one work item and only have to
			// return one result
			return workItemList.iterator().next();
		} else {
			throw new WorkItemCommandLineException(
					"Work Item not found: '" + parameter.getAttributeID() + "' Value: '" + parameter.getValue() + "'.");
		}
	}

	/**
	 * Find the work items for a list of ID's
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @param exceptions
	 *            - a list to pass exceptions back
	 * @return the list of work items
	 * @throws TeamRepositoryException
	 */
	private Object calculateWorkItemList(ParameterValue parameter, List<Exception> exceptions)
			throws TeamRepositoryException {
		if (StringUtil.isEmpty(parameter.getValue())) {
			return new ArrayList<Object>(0); // empty list
		}
		List<String> notFoundList = new ArrayList<String>();
		HashMap<String, IWorkItem> foundItems = findWorkItemsByIDValues(parameter.getValue(), notFoundList,
				ITEM_SEPARATOR);
		if (!notFoundList.isEmpty()) {
			exceptions.add(new WorkItemCommandLineException("WorkItems not found: Attribute: '"
					+ parameter.getAttributeID() + "' value '" + parameter.getValue() + "' WorkItems: '"
					+ helpGetDisplayStringFromList(notFoundList) + "'."));
		}
		if (parameter.isSet()) {
			// Set the value to the new list
			return foundItems.values();
		}
		List<Object> results = new ArrayList<Object>();
		Object current = getWorkItem().getValue(parameter.getIAttribute());
		if (!(current instanceof List<?>)) {
			throw new WorkItemCommandLineException(
					"Attribute type not a list attribute type, can not calculate current value: '"
							+ parameter.getIAttribute().getIdentifier() + "'.");
		}
		List<?> currentList = (List<?>) current;
		for (Object currentObject : currentList) {
			if (!(currentObject instanceof IItemHandle)) {
				exceptions.add(incompatibleAttributeValueTypeException(parameter,
						"Incompatible value type found computing List Attribute value: '" + currentObject.toString()
								+ "' "));
			}
			IItemHandle currentHandle = (IItemHandle) currentObject;
			if (!foundItems.containsKey(currentHandle.getItemId().getUuidValue())) {
				// did not find the current element in the input list I need to
				// add it to the result
				// for add: it is not in the found list and needed. The found
				// list is added later
				// for remove: it is not in the list of items to remove and
				// needed
				// I need to add this
				results.add(currentHandle);
			}
		}
		if (parameter.isAdd() || parameter.isDefault()) {
			// add all the results found, no duplicates, because we did only add
			// items we don't have in our list
			results.addAll(foundItems.values());
		}
		return results;
	}

	/**
	 * Calculate an XML String from a value and an old string Formats:
	 * 
	 * Plain<br/>
	 * <b>Bold</b><br/>
	 * <i>Italic</i><br/>
	 * <a href="https://rsjazz.wordpress.com/">External RSJazz Link</a> <br/>
	 * <b>@ralph </b><br/>
	 * Defect 3 <br/>
	 *  
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @param stringType
	 * @return the XML string
	 */
	private String calculateXMLDescription(ParameterValue parameter, XMLString originalContent, String stringType) {
		XMLString input = XMLString.createFromPlainText("");
		if (parameter.isRemove()) {
			throw modeNotSupportedException(parameter, "Mode not supported for this operation.");
		}

		input = XMLString.createFromXMLText(insertLineBreaks(parameter.getValue(), stringType));

		if (parameter.isAdd()) {
			return originalContent.concat(input).getXMLText();
		}
		// Default and set
		return input.getXMLText();
	}

	/**
	 * Create an approval on the work item.
	 * 
	 * @param parameter
	 * @param approvalData
	 *            - the value with the data to be set. This parameter is
	 *            encoded.
	 * @throws TeamRepositoryException
	 */
	private void createApproval(ParameterValue parameter, ApprovalInputData approvalData)
			throws TeamRepositoryException {

		// Create Approval
		IApprovals approvals = getWorkItem().getApprovals();
		IApprovalDescriptor descriptor = approvals.createDescriptor(approvalData.getApprovalType(),
				approvalData.getApprovalName());

		List<String> notFoundList = new ArrayList<String>();
		HashMap<String, IContributor> approvers = getContributors(approvalData.getApprovers(), ITEM_SEPARATOR,
				notFoundList);
		// Add users to approval - if any
		for (IContributorHandle approver : approvers.values()) {
			IApproval newApproval = approvals.createApproval(descriptor, approver);
			approvals.add(newApproval);
		}
		// Deal with false ID's in the approver list
		if (!notFoundList.isEmpty()) {
			throw new WorkItemCommandLineException(
					"Approvers not found: " + parameter + " Approvers: " + helpGetDisplayStringFromList(notFoundList));
		}
	}

	/**
	 * Gets a list of work items from a list of ID's
	 * 
	 * @param value
	 *            - a list of work item ID's
	 * @param notFoundList
	 *            - A list to pass work item ID's with no work item found
	 * @return
	 * @throws TeamRepositoryException
	 */
	private HashMap<String, IWorkItem> findWorkItemsByIDValues(String value, List<String> notFoundList,
			String separator) throws TeamRepositoryException {
		HashMap<String, IWorkItem> items = new HashMap<String, IWorkItem>();
		List<String> workItemIDs = StringUtil.splitStringToList(value, separator);
		for (String id : workItemIDs) {
			IWorkItem item = null;
			try {
				item = WorkItemUtil.findWorkItemByID(id, IWorkItem.SMALL_PROFILE, getWorkItemCommon(), monitor);
			} catch (WorkItemCommandLineException e) {
				// We just add the ID to the list of not found items
			}
			if (item == null) {
				notFoundList.add(id);
			} else {
				items.put(((IItemHandle) item).getItemId().getUuidValue(), item);
			}
		}
		return items;
	}

	/**
	 * Finds a build result with a given label
	 * 
	 * @param buildResultLabel
	 *            - the string representation of the build result label
	 * @return the buildresult that was found
	 * @throws WorkItemCommandLineException
	 * @see https://jazz.net/library/article/1229
	 */
	private IBuildResultHandle findBuildResultByLabel(String buildResultLabel) throws WorkItemCommandLineException {

		ITeamBuildClient buildClient = getBuildClient();
		IBuildResultQueryModel buildResultQueryModel = IBuildResultQueryModel.ROOT;
		IItemQuery query = IItemQuery.FACTORY.newInstance(buildResultQueryModel);
		/*
		 * Build up a query filter predicate that accepts a build label as input
		 */
		IPredicate buildByLabel = (buildResultQueryModel.label()._eq(query.newStringArg()));
		query.filter(buildByLabel);
		/* Order by build start time in descending order */
		query.orderByDsc(buildResultQueryModel.buildStartTime());
		try {
			/*
			 * Query for items using the build definition's item ID as the
			 * argument.
			 */
			IItemQueryPage queryPage = buildClient.queryItems(query, new Object[] { buildResultLabel },
					IQueryService.ITEM_QUERY_MAX_PAGE_SIZE, monitor);
			IItemHandle[] results = queryPage.handlesAsArray();

			if (results.length == 1) {
				if (results[0] instanceof IBuildResultHandle) {
					return (IBuildResultHandle) results[0];
				}
			}
		} catch (TeamRepositoryException e) {
			throw new WorkItemCommandLineException("Build Result not found " + buildResultLabel, e);
		}
		throw new WorkItemCommandLineException("Build Result not found " + buildResultLabel);
	}

	/**
	 * Find the category from the string. Categories don't have ID's, so you can
	 * only search for the value.
	 * 
	 * @param value
	 *            - the category path
	 * @return the category handle
	 * @throws TeamRepositoryException
	 */
	private ICategoryHandle findCategory(String value) throws TeamRepositoryException {
		List<String> path = StringUtil.splitStringToList(value, PATH_SEPARATOR);
		ICategoryHandle category = getWorkItemCommon().findCategoryByNamePath(getWorkItem().getProjectArea(), path,
				monitor);
		if (category == null && value != null) {
			if (value.equals("Unassigned")) {
				// category=ICategory.DEFAULT_CATEGORY_PROPERTY;
				category = getWorkItemCommon().findUnassignedCategory(getWorkItem().getProjectArea(),
						ICategory.SMALL_PROFILE, monitor);
			}
		}
		return category;
	}

	/**
	 * Find a contributor from a given user ID string representation. Search by
	 * ID or name first search for implemented.
	 * 
	 * @param userID
	 *            - the user ID string encoded
	 * @return the contributor object or null if the user ID could not be found
	 * @throws TeamRepositoryException
	 */
	@SuppressWarnings("rawtypes")
	private IContributor findContributorFromIDorName(final String userID) throws TeamRepositoryException {
		String userTrimmed = userID.trim();
		IContributor foundUser = null;
		if (userTrimmed.isEmpty()) {
			userTrimmed = IWorkItemCommandLineConstants.UNASSIGNED_USER;
		}
		try {
			foundUser = getTeamRepository().contributorManager().fetchContributorByUserId(userTrimmed, monitor);
		} catch (ItemNotFoundException e) {
			// Try to find by name
			List allContributors = getTeamRepository().contributorManager().fetchAllContributors(monitor);
			for (Iterator iterator = allContributors.iterator(); iterator.hasNext();) {
				IContributor contributor = (IContributor) iterator.next();
				if (contributor.getName().equals(userTrimmed)) {
					foundUser = contributor;
					break;
				}
			}
		}
		return foundUser;
	}

	/**
	 * Find a deliverable (release) object from its name Deliverables don't have
	 * ID's, so you can only search for the value.
	 * 
	 * @param value
	 *            - the name of the deliverable
	 * @return the deliverable object
	 * @throws TeamRepositoryException
	 */
	private IDeliverable findDeliverable(String value) throws TeamRepositoryException {
		return getWorkItemCommon().findDeliverableByName(getWorkItem().getProjectArea(), value,
				IDeliverable.FULL_PROFILE, monitor);
	}

	/**
	 * Find a resolution from a given string representation. Note, this looks
	 * into all resolutions and does not look into the specific resolutions
	 * related to a state. Search is by name and by ID of the resolution.
	 * 
	 * @param value
	 *            - the resolution display text or "" or null;
	 * @return the resolution object that was found or null if the value is
	 *         empty
	 * @throws TeamRepositoryException,
	 *             WorkItemCommandLineException
	 */
	private Identifier<IResolution> findResolution(String value)
			throws TeamRepositoryException, WorkItemCommandLineException {
		if (value == null) {
			return null;
		}
		if (value.equals("")) {
			return null;
		}
		IWorkflowInfo workflowInfo = getWorkItemCommon().getWorkflow(getWorkItem().getWorkItemType(),
				getWorkItem().getProjectArea(), monitor);
		Identifier<IResolution>[] resolutions = workflowInfo.getAllResolutionIds();
		for (Identifier<IResolution> resolution : resolutions) {
			if (workflowInfo.getResolutionName(resolution).equals(value)) {
				return resolution;
			} else if (resolution.getStringIdentifier().equals(value)) {
				return resolution;
			}
		}
		throw new WorkItemCommandLineException("Resolution not found Value: " + value);
	}

	/**
	 * Try to find an action that leads from the current state of the work item
	 * to the desires target state. Searches by string display value first and
	 * then by ID of the state.
	 * 
	 * @param value
	 *            - the string representation of the target state.
	 * @return the workflow action identifier.
	 * @throws TeamRepositoryException
	 */
	private Identifier<IWorkflowAction> findActionToTargetState(String value) throws TeamRepositoryException {
		IWorkflowInfo workflowInfo = getWorkItemCommon().getWorkflow(getWorkItem().getWorkItemType(),
				getWorkItem().getProjectArea(), monitor);
		Identifier<IWorkflowAction>[] ActionIDs = workflowInfo.getActionIds(getWorkItem().getState2());
		for (Identifier<IWorkflowAction> action : ActionIDs) {
			Identifier<IState> targetState = workflowInfo.getActionResultState(action);
			if (workflowInfo.getStateName(targetState).equals(value)) {
				return action;
			} else if ((targetState).getStringIdentifier().equals(value)) {
				return action;
			}
		}
		return null;
	}

	/**
	 * Try to find an action with a given name, that leads from the current
	 * state of the work item. Searches by string display value first and then
	 * by ID of the action.
	 * 
	 * @param value
	 *            - the string representation of the action.
	 * @return the workflow action identifier.
	 * @throws TeamRepositoryException
	 */
	private Identifier<IWorkflowAction> findAction(String value) throws TeamRepositoryException {
		IWorkflowInfo workflowInfo = getWorkItemCommon().getWorkflow(getWorkItem().getWorkItemType(),
				getWorkItem().getProjectArea(), monitor);
		Identifier<IWorkflowAction>[] ActionIDs = workflowInfo.getActionIds(getWorkItem().getState2());
		for (Identifier<IWorkflowAction> action : ActionIDs) {
			if (workflowInfo.getActionName(action).equals(value)) {
				return action;
			} else if (action.getStringIdentifier().equals(value)) {
				return action;
			}
		}
		return null;
	}

	/**
	 * Find the state represented by its name as string. Searches by string
	 * display value first and then by ID.
	 * 
	 * @param value
	 *            - the string representation of the target state.
	 * @return - the state identifier
	 * @throws TeamRepositoryException
	 */
	private Identifier<IState> findTargetState(String value) throws TeamRepositoryException {
		IWorkflowInfo workflowInfo = getWorkItemCommon().getWorkflow(getWorkItem().getWorkItemType(),
				getWorkItem().getProjectArea(), monitor);
		Identifier<IState>[] states = workflowInfo.getAllStateIds();
		for (Identifier<IState> stateId : states) {
			if (workflowInfo.getStateName(stateId).equals(value)) {
				return stateId;
			} else if (stateId.getStringIdentifier().equals(value)) {
				return stateId;
			}
		}
		return null;
	}

	/**
	 * Tests if a work item is in a certain state Searches by string display
	 * value first and then by ID.
	 * 
	 * @param value
	 *            - the name of the state
	 * @return true if the current state of the work item has the name of the
	 *         value provided
	 * @throws TeamRepositoryException
	 */
	private boolean isInState(IWorkItem workItem, IWorkItemCommon workItemCommon, String value)
			throws TeamRepositoryException {
		IWorkflowInfo workflowInfo = workItemCommon.getWorkflow(workItem.getWorkItemType(), workItem.getProjectArea(),
				monitor);
		Identifier<IState> currentState = getWorkItem().getState2();
		if (currentState == null) {
			// a new work item does not yet have a state
			return false;
		}
		if (workflowInfo.getStateName(currentState).equals(value)) {
			return true;
		} else if (currentState.getStringIdentifier().equals(value)) {
			return true;
		}
		return false;
	}

	/**
	 * Set the state of the work item. Dependent on the input the method either
	 * uses an action to the state - if that exists, or directly sets the state,
	 * even if the action does not exist. To do the latter, it uses a deprecated
	 * method.
	 * 
	 * @param propertyID
	 *            - the property to access the attribute. this is encoded to
	 *            deliver the approval type
	 * @param value
	 *            - the value with the data to be set. This parameter is
	 *            encoded.
	 * @throws TeamRepositoryException
	 */
	@SuppressWarnings("deprecation")
	private void setState(ParameterValue parameter) throws TeamRepositoryException {
		List<String> stateList = StringUtil.splitStringToList(parameter.getValue(), FORCESTATE_SEPARATOR);
		if (stateList.size() == 1) {
			String targetState = stateList.get(0).trim();
			if (isInState(getWorkItem(), getWorkItemCommon(), targetState)) {
				return;
			}

			Identifier<IWorkflowAction> foundAction = findActionToTargetState(targetState);
			if (foundAction == null) {
				throw new WorkItemCommandLineException("Action to target State not found: " + parameter.getAttributeID()
						+ " Value: " + parameter.getValue());
			}
			((WorkItemWorkingCopy) getWorkingCopy()).setWorkflowAction(foundAction.getStringIdentifier());
		} else if (stateList.size() == 2) {
			String prefix = stateList.get(0);
			String newValue = stateList.get(1);
			if (STATECHANGE_FORCESTATE.equals(prefix)) {
				Identifier<IState> foundState = findTargetState(newValue.trim());
				if (foundState == null) {
					throw new WorkItemCommandLineException("Target state not found: " + parameter.getAttributeID()
							+ " Value: " + parameter.getValue());
				}
				/**
				 * Warning: Deprecated method does not respect the workflow.
				 */
				getWorkItem().setState2(foundState);
			} else {
				throw new WorkItemCommandLineException("Prefix not recognized: " + prefix + " in "
						+ parameter.getAttributeID() + " Value: " + parameter.getValue());

			}
		} else {
			new WorkItemCommandLineException("Incorrect state format: " + parameter.getAttributeID() + " Value: "
					+ parameter.getValue() + helpUsageStateChange());
		}
	}

	/**
	 * Creates links from the current work item to a list of target work items.
	 * 
	 * @param linkType
	 *            the type of the link as defined in the map.
	 * @param value
	 *            the list of work items specified by work item ID's.
	 * @throws TeamRepositoryException
	 */
	private List<ReferenceData> createReferences(String linkType, ParameterValue parameter, List<Exception> exceptions)
			throws TeamRepositoryException {
		IEndPointDescriptor buildEndpoint = ReferenceUtil.getBuild_EndPointDescriptorMap().get(linkType);
		if (buildEndpoint != null) {
			return createBuildResultReferences(parameter, buildEndpoint, exceptions);
		}
		// Get the target endpoint descriptor for the link type.
		IEndPointDescriptor workItemEndpoint = ReferenceUtil.getWorkItemEndPointDescriptorMap().get(linkType);
		if (workItemEndpoint != null) {
			return createWorkItemReferences(parameter, workItemEndpoint, exceptions);
		}
		IEndPointDescriptor wiCLMEndpoint = ReferenceUtil.getCLM_WI_EndPointDescriptorMap().get(linkType);
		if (wiCLMEndpoint != null) {
			return createCLM_WI_References(parameter, wiCLMEndpoint, exceptions);
		}
		IEndPointDescriptor uriEndpoint = ReferenceUtil.getCLM_URI_EndPointDescriptorMap().get(linkType);
		if (uriEndpoint != null) {
			return createCLM_URI_References(parameter, uriEndpoint, exceptions);
		}
		// This link type is not in our supported map
		throw new WorkItemCommandLineException(
				"Link Type unknown or not yet supported: " + linkType + helpUsageWorkItemLinks());
	}

	/**
	 * Create CLM links between work items. The work item can be provided using
	 * its ID or by using the location URI
	 * 
	 * @param parameter
	 *            - the parameter passed
	 * @param endpoint
	 *            the endpoint used for the links
	 * @param exceptions
	 *            - a list to pass exceptions back
	 * @return a list of references managed in a special class
	 * @throws TeamRepositoryException
	 */
	private List<ReferenceData> createCLM_WI_References(ParameterValue parameter, IEndPointDescriptor endpoint,
			List<Exception> exceptions) throws TeamRepositoryException {
		List<ReferenceData> found = new ArrayList<ReferenceData>();
		List<String> workItems = StringUtil.splitStringToList(parameter.getValue(), LINK_SEPARATOR);
		for (String value : workItems) {
			if (value.startsWith(HTTP_PROTOCOL_PREFIX)) { // This covers https as well
				// We have an URI
				IReference reference;
				try {
					reference = IReferenceFactory.INSTANCE.createReferenceFromURI(new URI(value), value);
					found.add(new ReferenceData(endpoint, reference));
				} catch (URISyntaxException e) {
					exceptions.add(new WorkItemCommandLineException("Creating URI Reference (" + value + ") failed: "
							+ e.getMessage() + " \n " + parameter.getAttributeID() + " = " + parameter.getValue()
							+ helpUsageBuildResultLink()));
				}
			} else {
				IWorkItem workItem = null;
				try {
					workItem = WorkItemUtil.findWorkItemByID(value, IWorkItem.SMALL_PROFILE, getWorkItemCommon(),
							monitor);
				} catch (WorkItemCommandLineException e) {
					exceptions.add(e);
				}
				if (workItem != null) {

					Location location = Location.namedLocation(workItem,
							((ITeamRepository) workItem.getOrigin()).publicUriRoot());

					found.add(new ReferenceData(endpoint,
							IReferenceFactory.INSTANCE.createReferenceFromURI(location.toAbsoluteUri())));
				}
			}
		}
		return found;
	}

	/**
	 * Creates a reference to a work item. the work items must be in the same
	 * repository and only ID's are alloewd
	 * 
	 * @param parameter
	 *            - the parameter with the values
	 * @param endpoint
	 *            - the endpoint for the reference
	 * @param exceptions
	 *            - exceptions that will be passed back
	 * @return a list of references that can be used later
	 * @throws TeamRepositoryException
	 * @throws WorkItemCommandLineException
	 */
	private List<ReferenceData> createWorkItemReferences(ParameterValue parameter, IEndPointDescriptor endpoint,
			List<Exception> exceptions) throws TeamRepositoryException, WorkItemCommandLineException {
		List<ReferenceData> found = new ArrayList<ReferenceData>();
		List<String> notFoundList = new ArrayList<String>();
		HashMap<String, IWorkItem> workItemList = findWorkItemsByIDValues(parameter.getValue(), notFoundList,
				LINK_SEPARATOR);
		if (!workItemList.isEmpty()) {
			// Create the references
			for (IWorkItem item : workItemList.values()) {
				found.add(new ReferenceData(endpoint,
						IReferenceFactory.INSTANCE.createReferenceToItem(item.getItemHandle())));
			}
		} else {
			throw new WorkItemCommandLineException("Link items - no targets found: " + parameter.getAttributeID()
					+ " = " + parameter.getValue() + helpUsageWorkItemLinks());
		}
		if (!notFoundList.isEmpty()) {
			exceptions.add(
					new WorkItemCommandLineException("Create Links WorkItems not found: " + parameter.getAttributeID()
							+ " = " + parameter.getValue() + helpGetDisplayStringFromList(notFoundList)));
		}
		return found;
	}

	/**
	 * Creates a URI based reference to an object that is defined by an URI
	 * 
	 * @param parameter
	 *            - the parameter with the values
	 * @param endpoint
	 *            - the endpoint for the reference
	 * @param exceptions
	 *            - exceptions that will be passed back
	 * @return a list of references that can be used later
	 * @throws TeamRepositoryException
	 * @throws WorkItemCommandLineException
	 */
	private List<ReferenceData> createCLM_URI_References(ParameterValue parameter, IEndPointDescriptor endpoint,
			List<Exception> exceptions) throws TeamRepositoryException, WorkItemCommandLineException {
		List<ReferenceData> found = new ArrayList<ReferenceData>();
		List<String> itemURIS = StringUtil.splitStringToList(parameter.getValue(), LINK_SEPARATOR);
		for (String uri : itemURIS) {
			if (uri.startsWith(HTTP_PROTOCOL_PREFIX)) {
				// We have an URI
				IReference reference;
				try {
					reference = IReferenceFactory.INSTANCE.createReferenceFromURI(new URI(uri), uri);
					found.add(new ReferenceData(endpoint, reference));
				} catch (URISyntaxException e) {
					exceptions.add(new WorkItemCommandLineException("Creating URI Reference (" + uri + ") failed: "
							+ e.getMessage() + " \n " + parameter.getAttributeID() + " = " + parameter.getValue()
							+ helpUsageBuildResultLink()));
				}
			}
		}
		return found;
	}

	/**
	 * Creates links from the current work item to a list of target build
	 * results.
	 * 
	 * @param parameter
	 *            - the parameter with the values
	 * @param endpoint
	 *            - the endpoint for the reference
	 * @param exceptions
	 *            - exceptions that will be passed back
	 * @return
	 */
	private List<ReferenceData> createBuildResultReferences(ParameterValue parameter, IEndPointDescriptor endpoint,
			List<Exception> exceptions) {
		String basemessage = "Link to build result ";
		List<ReferenceData> found = new ArrayList<ReferenceData>();
		// Get the build results
		List<String> buildResults = StringUtil.splitStringToList(parameter.getValue(), LINK_SEPARATOR);
		if (buildResults.isEmpty()) {
			throw new WorkItemCommandLineException(basemessage + " - no build ID's/Labels specified: "
					+ parameter.getAttributeID() + " = " + parameter.getValue() + helpUsageBuildResultLink());
		}
		for (String buildResult : buildResults) {
			String message = basemessage + buildResult;
			// Try to find the build result handle
			IBuildResultHandle result = null;
			try {
				if (StringUtil.hasPrefix(buildResult, PREFIX_REFERENCETYPE)) {
					// Find by ID
					buildResult = StringUtil.removePrefix(buildResult, PREFIX_REFERENCETYPE);
					result = BuildUtil.findBuildResultbyID(buildResult, getTeamRepository(), monitor);
				} else {
					// Find by label
					result = findBuildResultByLabel(buildResult);
				}
				if (result == null) {
					throw new WorkItemCommandLineException(
							message + " failed: \n" + parameter.getValue() + helpUsageBuildResultLink());
				}
				IItemReference reference = IReferenceFactory.INSTANCE.createReferenceToItem(result);
				found.add(new ReferenceData(endpoint, reference));
				// createItemReference(buildResultEndpoint, result);
			} catch (TeamRepositoryException e) {
				throw new WorkItemCommandLineException(
						message + " failed: " + parameter.getValue() + helpUsageBuildResultLink(), e);
			} catch (WorkItemCommandLineException e) {
				exceptions.add(new WorkItemCommandLineException(message + " failed: " + e.getMessage() + " \n "
						+ parameter.getValue() + helpUsageBuildResultLink()));
			}
		}
		return found;
	}

	/**
	 * Try to set a workflow action
	 * 
	 * @param parameter
	 * @throws TeamRepositoryException
	 */
	private void setWorkFlowAction(ParameterValue parameter) throws TeamRepositoryException {
		Identifier<IWorkflowAction> foundAction = findAction(parameter.getValue());
		if (foundAction != null) {
			((WorkItemWorkingCopy) getWorkingCopy()).setWorkflowAction(foundAction.getStringIdentifier());
		} else {
			throw new WorkItemCommandLineException(
					"Action not found: " + parameter.getAttributeID() + " Value: " + parameter.getValue());
		}
	}

	/**
	 * Get a duration in long from a string representation.
	 * 
	 * @param value
	 *            - the duration
	 * @return the value of the duration (in milliseconds) or as string
	 *         representation like "1 hour 3 minutes"
	 */
	private long getDurationFromString(String value) {
		return SimpleDateFormatUtil.convertDurationToMiliseconds(value);
	}

	/**
	 * Gets a list of Tags
	 * 
	 * @param value
	 * @return
	 */
	private List<String> getTags(String value) {
		List<String> inputTags = StringUtil.splitStringToList(value, ITEM_SEPARATOR);
		List<String> trimmedTags = new ArrayList<String>(inputTags.size());
		for (String input : inputTags) {
			trimmedTags.add(input.trim());
		}
		HashSet<String> newTags = new HashSet<String>();
		newTags.addAll(trimmedTags);
		return new ArrayList<String>(newTags);
	}

	/**
	 * Gets a list of tags to be added from a string, and creates a tag list
	 * without duplicates.
	 * 
	 * @param value
	 *            - a string list of tags
	 * @param oldTags
	 *            - a list of old tags
	 * @return - a list of tags that can be set
	 */
	private List<String> getTagsList(List<String> oldTags, List<String> newTags) {
		HashSet<String> tags = new HashSet<String>();
		tags.addAll(oldTags);
		tags.addAll(newTags);
		return new ArrayList<String>(tags);
	}

	/**
	 * @return the teamrepository
	 */
	private ITeamRepository getTeamRepository() {
		return fTeamRepository;
	}

	/**
	 * @return the IWorkItemCommon client library
	 */
	private IWorkItemCommon getWorkItemCommon() {
		return (IWorkItemCommon) getTeamRepository().getClientLibrary(IWorkItemCommon.class);
	}

	/**
	 * @return the IAuditableCommon client library
	 */
	private IAuditableCommon getAuditableCommon() {
		return (IAuditableCommon) getTeamRepository().getClientLibrary(IAuditableCommon.class);
	}

	/**
	 * @return the IProcessClientService client library
	 */
	private IProcessClientService getProcessClientService() {
		return (IProcessClientService) getTeamRepository().getClientLibrary(IProcessClientService.class);
	}

	/**
	 * @return the IWorkItemClient client library
	 */
	private IWorkItemClient getWorkItemClient() {
		return (IWorkItemClient) getTeamRepository().getClientLibrary(IWorkItemClient.class);
	}

	/**
	 * @return the ITeamBuildClient client library
	 */
	private ITeamBuildClient getBuildClient() {
		return (ITeamBuildClient) getTeamRepository().getClientLibrary(ITeamBuildClient.class);
	}

	/**
	 * Get a list of contributors from a string of separated user ID's,
	 * separated by a specific separator.
	 * 
	 * @param value
	 *            - the string representation with a list of user ID's
	 * @param separator
	 *            - the separator to split the user ID's
	 * @param notFoundList
	 *            - a string list to contain the user Id's that where not found
	 * @return a Map of contributor UUID's and the found contributor objects
	 * @throws TeamRepositoryException
	 */
	private HashMap<String, IContributor> getContributors(String value, String separator, List<String> notFoundList)
			throws TeamRepositoryException {

		HashMap<String, IContributor> contributorList = new HashMap<String, IContributor>();
		if (value == null) {
			return contributorList;
		}
		List<String> users = StringUtil.splitStringToList(value, separator);
		for (String userID : users) {
			IContributor user = findContributorFromIDorName(userID);
			if (user == null) {
				if (!userID.trim().isEmpty()) {
					notFoundList.add(userID);
				}
			} else {
				contributorList.put(user.getItemId().getUuidValue(), user);
			}
		}
		return contributorList;
	}

	/**
	 * Gets an enumeration literal for an attribute that starts with a specific
	 * literal name.
	 * 
	 * @param attributeHandle
	 *            - the attribute handle
	 * @param literalNamePrefix
	 *            - the prefix literal to look for
	 * @return the literal ID or null
	 * @throws TeamRepositoryException
	 */
	@SuppressWarnings("unused")
	// Used if no exact match possible
	private Identifier<? extends ILiteral> getEnumerationLiteralStartsWithString(IAttributeHandle attributeHandle,
			String literalNamePrefix) throws TeamRepositoryException {
		Identifier<? extends ILiteral> literalID = null;
		IEnumeration<? extends ILiteral> enumeration = getWorkItemCommon().resolveEnumeration(attributeHandle, null);

		List<? extends ILiteral> literals = enumeration.getEnumerationLiterals();
		for (Iterator<? extends ILiteral> iterator = literals.iterator(); iterator.hasNext();) {
			ILiteral iLiteral = (ILiteral) iterator.next();
			if (iLiteral.getName().startsWith(literalNamePrefix.trim())) {
				literalID = iLiteral.getIdentifier2();
				break;
			}
		}
		return literalID;
	}

	/**
	 * Find an enumeration literal if you have the ID
	 * 
	 * @param enumeration
	 * @param identifierName
	 * @return
	 */
	@SuppressWarnings("unused")
	private ILiteral getEnumerationLiteralByID(final IEnumeration<? extends ILiteral> enumeration,
			final String identifierName) {
		final Identifier<? extends ILiteral> identifier = Identifier.create(ILiteral.class, identifierName);
		return enumeration.findEnumerationLiteral(identifier);
	}

	/**
	 * Gets an enumeration literal for an attribute that starts with a specific
	 * literal name.
	 * 
	 * @param attributeHandle
	 *            - the attribute handle
	 * @param literalName
	 *            - the literal display name to look for
	 * @return the literal ID or null
	 * @throws TeamRepositoryException
	 */
	private Identifier<? extends ILiteral> getEnumerationLiteralEqualsStringOrID(IAttributeHandle attributeHandle,
			String literalName) throws TeamRepositoryException {
		Identifier<? extends ILiteral> literalID = null;
		IEnumeration<? extends ILiteral> enumeration = getWorkItemCommon().resolveEnumeration(attributeHandle, monitor);

		List<? extends ILiteral> literals = enumeration.getEnumerationLiterals();
		for (Iterator<? extends ILiteral> iterator = literals.iterator(); iterator.hasNext();) {
			ILiteral iLiteral = (ILiteral) iterator.next();

			if (iLiteral.getName().equals(literalName.trim())) {
				literalID = iLiteral.getIdentifier2();
				break;
			} else if (iLiteral.getIdentifier2().getStringIdentifier().equals(literalName.trim())) {
				literalID = iLiteral.getIdentifier2();
				break;
			}
		}
		return literalID;
	}

	/**
	 * Gets an enumeration literal for an attribute that starts with a specific
	 * literal name.
	 * 
	 * @param attributeHandle
	 *            - the attribute handle
	 * @param literalName
	 *            - the literal display name to look for
	 * @return the literal ID or null
	 * @throws TeamRepositoryException
	 * @Deprecated
	 */
	@SuppressWarnings("unused")
	private Identifier<? extends ILiteral> getEnumerationLiteralEqualsString_old(IAttributeHandle attributeHandle,
			String literalName) throws TeamRepositoryException {
		Identifier<? extends ILiteral> literalID = null;
		IEnumeration<? extends ILiteral> enumeration = getWorkItemCommon().resolveEnumeration(attributeHandle, monitor);

		List<? extends ILiteral> literals = enumeration.getEnumerationLiterals();
		for (Iterator<? extends ILiteral> iterator = literals.iterator(); iterator.hasNext();) {
			ILiteral iLiteral = (ILiteral) iterator.next();
			if (iLiteral.getName().equals(literalName.trim())) {
				literalID = iLiteral.getIdentifier2();
				break;
			}
		}
		return literalID;
	}

	/**
	 * Utility method to upload and attach a file to a work item
	 * 
	 * @param fileName
	 *            - the file name of the file to upload
	 * @param description
	 *            - the description of the attachment
	 * @param contentType
	 *            - the content type of the file
	 * @param encoding
	 *            - the encoding of the file
	 * 
	 * @throws TeamRepositoryException
	 */
	private void attachFile(String fileName, String description, String contentType, String encoding)
			throws TeamRepositoryException {

		File attachmentFile = new File(fileName);
		FileInputStream fis;
		try {
			fis = new FileInputStream(attachmentFile);
			try {
				IAttachment newAttachment = getWorkItemClient().createAttachment(getWorkItem().getProjectArea(),
						attachmentFile.getName(), description, contentType, encoding, fis, monitor);

				newAttachment = (IAttachment) newAttachment.getWorkingCopy();
				newAttachment = getWorkItemCommon().saveAttachment(newAttachment, monitor);
				IItemReference reference = WorkItemLinkTypes.createAttachmentReference(newAttachment);

				getWorkingCopy().getReferences().add(WorkItemEndPoints.ATTACHMENT, reference);

			} finally {
				if (fis != null) {
					fis.close();
				}
			}
		} catch (FileNotFoundException e) {
			throw new WorkItemCommandLineException("Attach File - File not found: " + fileName, e);
		} catch (IOException e) {
			throw new WorkItemCommandLineException("Attach File - I/O Exception: " + fileName, e);
		}
	}

	/**
	 * Create an exception for an unsupported mode
	 * 
	 * @param parameter
	 * @param message
	 * @return
	 */
	private WorkItemCommandLineException modeNotSupportedException(ParameterValue parameter, String message) {
		String mode = "unknown";
		if (parameter.isDefault()) {
			mode = com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_DEFAULT;
		}
		if (parameter.isAdd()) {
			mode = com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_ADD;
		}
		if (parameter.isSet()) {
			mode = com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_SET;
		}
		if (parameter.isRemove()) {
			mode = com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_REMOVE;
		}
		return new WorkItemCommandLineException(message + " Mode: " + mode + " Attribute: " + parameter.getAttributeID()
				+ " Value: " + parameter.getValue());
	}

	/**
	 * Create an exception if the type of the expected value and the value found
	 * is incompatible
	 * 
	 * @param parameter
	 * @param message
	 * @return
	 * @throws TeamRepositoryException
	 */
	private WorkItemCommandLineException incompatibleAttributeValueTypeException(ParameterValue parameter,
			String message) throws TeamRepositoryException {
		return new WorkItemCommandLineException(message + " Incompatible item type :"
				+ parameter.getIAttribute().getIdentifier() + " Value: " + parameter.getValue());
	}

	/**
	 * Replaces certain substrings with line beaks
	 * 
	 * STRING_TYPE_HTML - nothing gets replaced
	 * 
	 * STRING_TYPE_PLAINSTRING - "\n" is replaced by the line break
	 * 
	 * STRING_TYPE_WIKI - "\n" and <br>
	 * are replaced by the line break
	 * 
	 * @param contentToAdd
	 * @param stringType
	 * @return a string with characters replaced
	 */
	private String insertLineBreaks(String contentToAdd, String stringType) {
		if (contentToAdd != null) {
			if (stringType.equals(STRING_TYPE_PLAINSTRING)) {
				contentToAdd = contentToAdd.replace(STRING_LINEBREAK_BACKSLASH_N, "\n");
			} else if (stringType.equals(STRING_TYPE_WIKI)) {
				contentToAdd = contentToAdd.replace(STRING_LINEBREAK_HTML_BR, "\n");
				contentToAdd = contentToAdd.replace(STRING_LINEBREAK_BACKSLASH_N, "\n");
			}
		}
		return contentToAdd;
	}

	/**
	 * Utility method to print a help screen
	 * 
	 * @param attribType
	 *            - the attribute type as string.
	 * @return a help description
	 */
	private String helpGetTypeProperties(String attribType) {
		String message = "Type: " + attribType + " [Primitive:" + AttributeTypes.isPrimitiveAttributeType(attribType)
				+ " Item:" + AttributeTypes.isItemAttributeType(attribType) + " Enum:"
				+ AttributeTypes.isEnumerationAttributeType(attribType) + " List:"
				+ AttributeTypes.isListAttributeType(attribType) + " Enum-List:"
				+ AttributeTypes.isEnumerationListAttributeType(attribType) + " Item-List:"
				+ AttributeTypes.isItemListAttributeType(attribType) + " Supported-Custom:"
				+ AttributeTypes.isSupportedCustomAttributeType(attribType) + "]";
		return message;
	}

	/**
	 * concatenates the elements in a list to create a help text
	 * 
	 * @param list
	 *            - list of string elements to be concatenated
	 * @return a help description
	 */
	private String helpGetDisplayStringFromList(List<String> list) {
		String display = "";
		for (String name : list) {
			display = display + name + " ";
		}
		return display;
	}

	/**
	 * Call from outside to get a complete help displayed
	 * 
	 * @return a help description
	 */
	public String helpGeneralUsage() {
		String usage = "";
		usage += "\n\nWorkItem attribute parameter and value examples:";
		usage += "\n" + helpUsageParameter();
		usage += "\n\nSpecial Properties:";
		usage += helpUsageSpecialProperties();
		usage += "\n\nWorkFlow Action: \n\tA pseudo parameter \"" + PSEUDO_ATTRIBUTE_TRIGGER_WORKFLOW_ACTION
				+ "\" can be used to set a workflow action to change the work item state when saving.";
		usage += helpUsageWorkflowAction();
		usage += "\n\tThis attribute requires only the value " + PSEUDO_ATTRIBUTEVALUE_YES + ".";
		usage += "\n\nAttachments: \n\tA pseudo parameter " + PSEUDO_ATTRIBUTE_ATTACHFILE
				+ " can be used to upload attachments.";
		usage += "\n\tThis attribute supports the modes default (same as) add, set and remove. "
				+ "\n\tSet removes all attachments, remove only removes attachments with the specified file path and description. "
				+ helpUsageAttachmentUpload();
		usage += "\n\n\tA pseudo parameter " + PSEUDO_ATTRIBUTE_DELETEATTACHMENTS + "=yes"
				+ " can be used to delete all attachments. ";
		usage += "\n\nLinks: \n\t A pseudo parameter " + PSEUDO_ATTRIBUTE_LINK
				+ " can be used to link the current work item to other objects."
				+ "\n\tLinks support the modes default (same as) add, set and remove. "
				+ "\n\tSet removes all links of the specified type before creating the new links. "
				+ helpUsageAllLinks();
		usage += "\nCSV Export/Import:";
				usage += "\nEncoding:"
				+ "\nImporting from a CSV file can fail if the file encoding does not match the expected encodig. "
				+ "An encoding can be provided for the CSV import and export. The default is " + IContent.ENCODING_UTF_8 +"."
				+ "\nFormat is: "
				+ "\n\t" + IWorkItemCommandLineConstants.PARAMETER_ENCODING
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR + "encodingID"
				+ "\n\n\t\tWhere encodingID is " + IContent.ENCODING_UTF_8 + " or " + IContent.ENCODING_UTF_16LE + " or "
				+ IContent.ENCODING_UTF_16BE + " or " + IContent.ENCODING_US_ASCII + "."
				+ "\n\n\tExample:" 
				+ "\n\n\t\t" + IWorkItemCommandLineConstants.PARAMETER_ENCODING
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR + IContent.ENCODING_UTF_8; 

		return usage;
	}

	/**
	 * Creates the help output for usage of special pseudo properties
	 * 
	 * @return a help description
	 */
	private String helpUsageSpecialProperties() {
		String usage = "\n\tWork Item ID: Parameter \"" + IWorkItem.ID_PROPERTY + "\" can not be changed.";
		usage += "\n\tProject Area: \n\tParameter \"" + IWorkItem.PROJECT_AREA_PROPERTY
				+ "\" can only be specified when creating the work item. It can not be set to a different value later.";
		usage += "\n\nComments: Parameter \"" + IWorkItem.COMMENTS_PROPERTY + "\" can be used to add a comment.";
		usage += "\n\tThis attribute only supports the default and add mode. Comments can not be removed.";
		usage += "\n\tExample: " + IWorkItem.COMMENTS_PROPERTY + "=" + "\"This is a comment\"";
		usage += "\n\nSubscriptions: \n\tParameter \"" + IWorkItem.SUBSCRIPTIONS_PROPERTY
				+ "\" can be used to subscribe a list of users using their user ID's.";
		usage += "\n\tThis attribute supports the modes default (same as) add, set and remove mode.";
		usage += "\n\tExample set specific users: " + IWorkItem.SUBSCRIPTIONS_PROPERTY
				+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.POSTFIX_PARAMETER_MANIPULATION_MODE
				+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_SET + "=" + "al" + ITEM_SEPARATOR
				+ "tammy";
		usage += "\n\tExample add users: " + IWorkItem.SUBSCRIPTIONS_PROPERTY
				+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.POSTFIX_PARAMETER_MANIPULATION_MODE
				+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_ADD + "=" + "deb" + ITEM_SEPARATOR
				+ "tanuj" + ITEM_SEPARATOR + "bob";
		usage += "\n\tExample remove users: " + IWorkItem.SUBSCRIPTIONS_PROPERTY
				+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.POSTFIX_PARAMETER_MANIPULATION_MODE
				+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_REMOVE + "=" + "sally"
				+ ITEM_SEPARATOR + "bob";
		usage += "\n\nTags: Parameter \"" + IWorkItem.TAGS_PROPERTY + "\" can be used to add a list of tags.";
		usage += "\n\tThis attribute supports the modes default (same as) add, set and remove mode.";
		usage += "\n\tExample: " + IWorkItem.TAGS_PROPERTY + "=" + "Tag1" + ITEM_SEPARATOR + ".." + ITEM_SEPARATOR
				+ "TagN";
		usage += "\n\nApprovals: \n\tParameter \"" + IWorkItem.APPROVALS_PROPERTY
				+ "\" can be used to add approvals and approvers using their user ID's.";
		usage += helpUsageApprovals();
		usage += "\nWork Item State: \n\tParameter \"" + IWorkItem.STATE_PROPERTY
				+ "\"  can be used to change the work item state.";
		usage += helpUsageStateChange();
		return usage;
	}

	/**
	 * Creates the help output for general usage of parameters
	 * 
	 * @return a help description
	 */
	private String helpUsageParameter() {
		String usage = "\nFormat for parameter is:\n";
		usage += "\tparameter[:mode]=value";
		usage += "\n";
		usage += "\nNo spaces are allowed between parameter, value and the =.";
		usage += "\nParameter and value can also not have spaces. Use \" to enclose values with spaces. Example: \"A Space\"";
		usage += "\n";
		usage += "\nParameters:";
		usage += "\nParameter is a work item attribute ID and value is a value or a list of values.";
		usage += "\nUse the command " + IWorkItemCommandLineConstants.PREFIX_COMMAND
				+ IWorkItemCommandLineConstants.COMMAND_PRINT_TYPE_ATTRIBUTES
				+ " to retrieve the available attribute ID's, or";
		usage += "\ninspect the process configuration of your project area to extract the attribute ID's.";
		usage += "\n";
		usage += "\nValues:";
		usage += "\nThe values are specified by a string. This is can be display name of that value (enumerations)";
		usage += "\nor composed of display values of the path to this item (category, iterations, process areas).";
		usage += "\nFor other attributes, such as users, work item types or work items, use the ID.";
		usage += "\n";
		usage += "\n\tExamples";
		usage += "\n\t- For enumeration based attributes use the display value for the enumeration literal:";
		usage += "\n\t\tinternalPriority=High";
		usage += "\n\t- For HTML and string based attributes use a string.";
		usage += "\n\t  HTML types like summary, description, comment and HTML support the syntax below.";
		usage += "\n\t\tdescription=\"Plain text<br/><b>bold text</b><br/><i>italic text</i><br/><a href=\"https://rsjazz.wordpress.com\">External RSJazz Link</a><br/>User link to <b>@ralph </b><br/>Work Item link to Defect 3 <br/>\"";
		usage += "\n\t- For Wiki and multi line text attributes use <br> or \\n for line breaks and check the syntax in the wiki editor.";
		usage += "\n\t\tcustom.wiki=\"<br>=Heading1<br><br>Plain text\\n==Heading 2\\n\\nNormal Text **bold text** <br>**bold text**<br>//Italics//\"";
		usage += "\n\t- For work item type, owner and some other attributes use the object ID.";
		usage += "\n\t\tworkItemType=task";
		usage += "\n\t\towner=tanuj";
		usage += "\n\t- Use the display name for simple attributes or the path composed out of the display names for hierarchical attributes.";
		usage += "\n\t\tcategory=JKE/BRN";
		usage += "\n\t\tfoundIn=\"Sprint 2 Development\"";
		usage += "\n\t\ttarget=\"Main Development/Release 1.0/Sprint 3\"";
		usage += "\n\t\tcustom.process.area=\"JKE Banking (Change Management)/Release Engineering\"";
		usage += "\n\t- Dates and Timestamps have to be specified in the Java SimpleDateFormat notation. The value \"unassigned\" can be used to delete the date.";
		usage += "\n\t\tdueDate=\"2015/02/01 12:30:00 GMT+01:00\"";
		usage += "\n\t\tdueDate=\"unassigned\"";
		usage += "\n\t- Duration values are specified in milliseconds, or a hours minutes format.";
		usage += "\n\t\tduration=1800000 correctedEstimate=3600000 timeSpent=60000";
		usage += "\n\t\tduration=\"1 hour\" correctedEstimate=\"2 hours 30 minutes\" timeSpent=\"30 minutes\"";
		usage += "\n\nWorkItem attribute values of <item type>List with a specified item type such as userList.";
		usage += helpUsageItemLists();
		usage += "\n";
		usage += "\nWorkItem attributes with an general attribute value such as Item or itemList require encoding to locate the items.";
		usage += helpUsageUnspecifiedItemValues();
		usage += "\n";
		usage += "\nModes:";
		usage += "\nModes allow different types of changes to attributes such as add values, append text or remove and set other data.";
		usage += "\nSupported modes are default (no mode specified), "
				+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_ADD + ", "
				+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_SET + ", "
				+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_REMOVE + ".";
		usage += "\nIf no mode is specified, the default mode for the parameter is used.";
		usage += "\n\tExample for " + com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_DEFAULT
				+ " mode: summary=\"This is a summary.\".";
		usage += "\n\tExample for " + com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_ADD
				+ " mode: summary:" + com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_ADD
				+ "=\" Add this to the summary.\".";
		usage += "\n\tExample for " + com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_SET
				+ " mode: summary:" + com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_SET
				+ "=\"Overwite the existing summary with this.\".";
		usage += "\n\tExample for " + com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_REMOVE
				+ " mode: custom.enumeration.list:"
				+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_REMOVE + "=$,Unassigned.";

		usage += "\n\nWhich modes are supported and their behavior depends on attribute type.";
		usage += "\nSingle value attributes typically support default and set mode, but not add and remove mode. ";
		usage += "\nMultiple value attributes typically support default, add, set and remove mode.";
		usage += "\nDefault mode for single value attributes sets the value.";
		usage += "\nDefault mode for multiple value attributes adds the value(s).";
		usage += "\nSet mode for multiple value attributes removes the old values and then adds the new value(s).";
		usage += "\nRemove mode for multiple value attributes removes the old values specified, that can be found.";
		usage += "\nString values such as HTML, Summary, Wiki type attributes support default (same behavior as set mode), set and add mode.";
		return usage;
	}

	/**
	 * Creates the help output for lists of items
	 * 
	 * @return a help description
	 */
	private String helpUsageItemLists() {
		String usage = "\nFormat is using the separator " + ITEM_SEPARATOR + " :";
		usage += "\n\t\"value1" + ITEM_SEPARATOR + "value2" + ITEM_SEPARATOR + "..." + ITEM_SEPARATOR + "valueN\"";
		usage += "\n\tExample: custom.user.list:add=\"deb" + ITEM_SEPARATOR + "al" + ITEM_SEPARATOR + "..."
				+ ITEM_SEPARATOR + "tanuj\"";
		return usage;
	}

	/**
	 * Help for unspecified item values
	 * 
	 * @return help description
	 */
	private String helpUsageUnspecifiedItemValues() {
		String usage = "\nFormat is:\n";
		usage += "\tcustom.item.list=value";
		usage += "\n";
		usage += "\n\tWhere value has the form: <value>{" + ITEM_SEPARATOR + "<value>}";
		usage += "\n\tWith <value> of the form <TypeDescriptor>" + ITEMTYPE_SEPARATOR + "<Item>.";
		usage += "\n";
		usage += "\n\tNo spaces are allowed in the value list.";
		usage += "\n";
		usage += "\n\tAvailable <TypeDescriptor> values are:";
		usage += "\n\t\t Project area: " + TYPE_PROJECT_AREA + " - specified by its name. Example: \""
				+ TYPE_PROJECT_AREA + ITEMTYPE_SEPARATOR + "JKE Banking (Change Management)\"";
		usage += "\n\t\t Team area: " + TYPE_TEAM_AREA + " - specified by its name path. Example: \"" + TYPE_TEAM_AREA
				+ ITEMTYPE_SEPARATOR + "JKE Banking (Change Management)/Release Engineering\"";
		usage += "\n\t\t Process area: " + TYPE_PROCESS_AREA + " - specified by its name path. Example: \""
				+ TYPE_PROCESS_AREA + ITEMTYPE_SEPARATOR
				+ "JKE Banking (Change Management)/Business Recovery Matters\"";
		usage += "\n\t\t Category: " + TYPE_CATEGORY + " - specified by its category path. Example: \"" + TYPE_CATEGORY
				+ ITEMTYPE_SEPARATOR + "JKE/BRM\"";
		usage += "\n\t\t User: " + TYPE_CONTRIBUTOR + " - specified by its id. Example: \"" + TYPE_CONTRIBUTOR
				+ ITEMTYPE_SEPARATOR + "tanuj";
		usage += "\n\t\t Iteration: " + TYPE_ITERATION
				+ " - specified by its name path (including the development line name). Example: \"" + TYPE_ITERATION
				+ ITEMTYPE_SEPARATOR + "Main Development/Release 1.0/Sprint 3\"";
		usage += "\n\t\t Work item: " + TYPE_WORKITEM + " - specified by its id. Example: \"" + TYPE_WORKITEM
				+ ITEMTYPE_SEPARATOR + "20\"";
		usage += "\n\t\t SCM component: " + TYPE_SCM_COMPONENT + " - specified by its name. Example: \""
				+ TYPE_SCM_COMPONENT + ITEMTYPE_SEPARATOR + "Build\"";
		return usage;
	}

	/**
	 * Creates the help output for creating approvals
	 * 
	 * @return a help description
	 */
	private String helpUsageApprovals() {
		String usage = "\n\tApprovals only support the modes default (same as) add, set and remove. "
				+ "\n\tSet and remove only affects approvals of the same type. ";
		usage += "\nFormat is:" + "\n\t" + IWorkItem.APPROVALS_PROPERTY + "[<ID>]["
				+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.POSTFIX_PARAMETER_MANIPULATION_MODE
				+ "<mode>]" + "=\"" + APPROVAL_TYPE_APPROVAL + APPROVAL_SEPARATOR + "Approval Name as string"
				+ APPROVAL_SEPARATOR + "userID1" + ITEM_SEPARATOR + ".." + ITEM_SEPARATOR + "userIDn\"";
		usage += "\n\tWhere <ID> can be left out if only one approval is specified or needs to be unique if multiple approvals are specified. "
				+ "Where <mode> can be left out and defaults to "
				+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_ADD + ".";
		usage += "\n\tAvailable modes are:" + com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_SET
				+ " " + com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_ADD
				+ " (set as default mode) and "
				+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_REMOVE + ".";
		usage += "\n\t Modes " + com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_SET + " and "
				+ com.ibm.js.team.workitem.commandline.framework.ParameterValue.MODE_REMOVE
				+ " only remove approvals of the same type and must be enabled using the switch "
				+ IWorkItemCommandLineConstants.SWITCH_ENABLE_DELETE_APPROVALS + ".\n";
		usage += "\tExample " + IWorkItem.APPROVALS_PROPERTY + "=\"" + APPROVAL_TYPE_REVIEW + APPROVAL_SEPARATOR
				+ "Please Review" + APPROVAL_SEPARATOR + "deb" + ITEM_SEPARATOR + "tanuj\"";
		usage += "\n\tExample " + IWorkItem.APPROVALS_PROPERTY + "=\"" + APPROVAL_TYPE_VERIFICATION + APPROVAL_SEPARATOR
				+ "Please verify" + APPROVAL_SEPARATOR + "sally" + ITEM_SEPARATOR + "al\""
				+ "\n\twhere the user list is optional and can contain one or more users ID's\n";
		return usage;
	}

	/**
	 * Creates the help output for changing states
	 * 
	 * @return a help description
	 */
	private String helpUsageStateChange() {
		String usage = "\n\tState change only supports the modes default and set. ";
		usage += "\nFormat is: " + "\n\t" + IWorkItem.STATE_PROPERTY + "=StateName"
				+ " to find a one step workflow action to change the state, and execute the action, or";
		usage += "\n\t" + IWorkItem.STATE_PROPERTY + "=" + STATECHANGE_FORCESTATE + FORCESTATE_SEPARATOR
				+ "StateName to force the state change to the target state even if no workflow action exists";
		return usage;
	}

	private String helpUsageWorkflowAction() {
		String usage = "\n\tThis attribute supports only the modes default and set. " + "\n\tExample: "
				+ PSEUDO_ATTRIBUTE_TRIGGER_WORKFLOW_ACTION + "=" + "\"Stop working\"";
		return usage;
	}

	/**
	 * Creates the help output for uploading attachments
	 * 
	 * @return a help description
	 */
	private String helpUsageAttachmentUpload() {
		String usage = "\nFormat is: " + "\n\t" + PSEUDO_ATTRIBUTE_ATTACHFILE + "[<IDString>][:mode]="
				+ "\"SomeFilePath" + ATTACHMENT_SEPARATOR + "Some Description" + ATTACHMENT_SEPARATOR + "ContentTypeID"
				+ ATTACHMENT_SEPARATOR + "EncodingID";
		usage += "\"\n\n\tWhere:\n\t\t"
				+ "<IDString> must be unique for multiple attachments in one command. If only one attachment is uploaded, the IDString can be left empty.";
		usage += "\n\t\tContentTypeID is " + IContent.CONTENT_TYPE_TEXT + " or " + IContent.CONTENT_TYPE_UNKNOWN
				+ " or " + IContent.CONTENT_TYPE_XML;
		usage += "\n\t\tEncodingID is " + IContent.ENCODING_UTF_8 + " or " + IContent.ENCODING_UTF_16LE + " or "
				+ IContent.ENCODING_UTF_16BE + " or " + IContent.ENCODING_US_ASCII + ".";
		usage += "\n\n\tThe file must be accessible and in the correct encoding.";
		usage += "\n\n\tExamples:" + "\n\t\t" + PSEUDO_ATTRIBUTE_ATTACHFILE + "=\"C:/temp/test.txt"
				+ ATTACHMENT_SEPARATOR + "Some Attachment" + ATTACHMENT_SEPARATOR + IContent.CONTENT_TYPE_TEXT
				+ ATTACHMENT_SEPARATOR + IContent.ENCODING_UTF_8 + "\"";
		usage += "\n\t\t" + PSEUDO_ATTRIBUTE_ATTACHFILE + "_1:add=\"./test1.txt" + ATTACHMENT_SEPARATOR
				+ "Some Attachment 1" + ATTACHMENT_SEPARATOR + IContent.CONTENT_TYPE_TEXT + ATTACHMENT_SEPARATOR
				+ IContent.ENCODING_UTF_8 + "\"" + " " + PSEUDO_ATTRIBUTE_ATTACHFILE + "_2=\"./test2.txt"
				+ ATTACHMENT_SEPARATOR + "Some Attachment 2" + ATTACHMENT_SEPARATOR + IContent.CONTENT_TYPE_TEXT
				+ ATTACHMENT_SEPARATOR + IContent.ENCODING_UTF_8 + "\"";
		usage += "\n\t\t" + PSEUDO_ATTRIBUTE_DELETEATTACHMENTS + "=\"" + PSEUDO_ATTRIBUTEVALUE_YES + "\"";
		return usage;
	}

	/**
	 * Creates the help output for all linking options
	 * 
	 * @return a help description
	 */
	private String helpUsageAllLinks() {
		if (isBulkUpdate()) {
			return "";
		}
		String usage = "\n\nWork Item Links - links between this work item and another work item within the same repository:"
				+ helpUsageWorkItemLinks();
		usage += "\n\n\tPlease note that the link \"Mentions\" can not directly be created during import or update operations. They can only be created indirectly by referring to work items and users in the description or comments.";
		usage += "\n\nCLM Work Item Links - CLM links between this work item and another work item within the same or acoross repositories"
				+ helpUsageCLMWorkItemLink();
		usage += "\n\nCLM URI Links - CLM links between this work item and another item, described by a valid URI, in a different repository"
				+ helpUsageCLMItemURILink();
		usage += "\n\n\tPlease note that the link \"Associate Work Item\" between a change set and the work item can only be created by the SCM component and not by this application.";
		usage += "\n\tThe link created here is the looser CLM link. Create the work item change set link using the SCM command line.";
		usage += "\n\nBuild result Links - Links from a work item to a build result in the same repository."
				+ helpUsageBuildResultLink();
		usage += "\n\nDelete all links of a link type " + helpUsageDeleteLinksOfType();
		return usage;
	}

	/**
	 * Creates the help output for linking this work item to other local work
	 * items
	 * 
	 * @return a help description
	 */
	private String helpUsageWorkItemLinks() {
		if (isBulkUpdate()) {
			return "";
		}
		String usage = "\nFormat is:";
		usage += "\n\t" + PSEUDO_ATTRIBUTE_LINK + "linktype=value\n";
		usage += "\n\tThe parameter value is a list of one or more work items specified by their ID. The separator is:"
				+ LINK_SEPARATOR_HELP + "\n\n";
		Set<String> wiLinkTypes = ReferenceUtil.getWorkItemEndPointDescriptorMap().keySet();
		for (String linktype : wiLinkTypes) {
			usage += "\t" + PSEUDO_ATTRIBUTE_LINK + linktype + "=id1" + LINK_SEPARATOR_HELP + "id2"
					+ LINK_SEPARATOR_HELP + "...\n";
		}
		usage += "\n\tExample:";
		usage += "\n\t\t" + PSEUDO_ATTRIBUTE_LINK + "related=123" + LINK_SEPARATOR_HELP + "80";
		return usage;
	}

	/**
	 * Creates the help output for linking this work item to other work items
	 * using CLM links
	 * 
	 * @return a help description
	 */
	private String helpUsageCLMWorkItemLink() {
		if (isBulkUpdate()) {
			return "";
		}
		String usage = "\nFormat is:";
		usage += "\n\t" + PSEUDO_ATTRIBUTE_LINK + "linktype=value\n";
		usage += "\n\tThe parameter value is a list of one or more work items specified by their ID (if they are in the same repository) or by the Item URI. The separator is:"
				+ LINK_SEPARATOR_HELP + "\n\n";
		Set<String> wiLinkTypes = ReferenceUtil.getCLM_WI_EndPointDescriptorMap().keySet();
		for (String linktype : wiLinkTypes) {
			usage += "\t" + PSEUDO_ATTRIBUTE_LINK + linktype + "=id1" + LINK_SEPARATOR_HELP + "id2"
					+ LINK_SEPARATOR_HELP + "URI2" + LINK_SEPARATOR_HELP + "...\n";
		}
		usage += "\n\tExample:";
		usage += "\n\t\t" + PSEUDO_ATTRIBUTE_LINK
				+ "tracks_workitem=\"https://clm.example.com:9443/ccm/resource/itemName/com.ibm.team.workitem.WorkItem/80"
				+ LINK_SEPARATOR_HELP + "120" + LINK_SEPARATOR_HELP + "150\"";
		return usage;
	}

	/**
	 * Links between this work item and other items provided by a CLM URI
	 * 
	 * @return a help description
	 */
	private String helpUsageCLMItemURILink() {
		if (isBulkUpdate()) {
			return "";
		}
		String usage = "\nFormat is:";
		usage += "\n\t" + PSEUDO_ATTRIBUTE_LINK + "linktype=value\n";
		usage += "\n\tThe parameter value is a list of one or more CLM URI's for elements that support this link type. The separator is:"
				+ LINK_SEPARATOR_HELP + "\n\n";
		Set<String> wiLinkTypes = ReferenceUtil.getCLM_URI_EndPointDescriptorMap().keySet();
		for (String linktype : wiLinkTypes) {
			usage += "\t" + PSEUDO_ATTRIBUTE_LINK + linktype + "=uri1" + LINK_SEPARATOR_HELP + "uri2"
					+ LINK_SEPARATOR_HELP + "...\n";
		}
		usage += "\n\tExample:";
		usage += "\n\t\t" + PSEUDO_ATTRIBUTE_LINK
				+ "affects_requirement=https://clm.example.com:9443/rm/resources/_848a30e315524069854f55e1d35a402d"
				+ LINK_SEPARATOR_HELP + "https://clm.example.com:9443/rm/resources/_6c96bedb0e9a490494273eefc6e1f7c5";
		return usage;
	}

	/**
	 * Creates the help output for linking to build results
	 * 
	 * @return a help description
	 */
	private String helpUsageBuildResultLink() {
		if (isBulkUpdate()) {
			return "";
		}
		String usage = "\nFormat is:\n";
		usage += "\t" + PSEUDO_ATTRIBUTE_LINK + ReferenceUtil.LINKTYPE_REPORTED_AGAINST_BUILDRESULT + "=buildResult1"
				+ LINK_SEPARATOR_HELP + "buildResult2" + LINK_SEPARATOR_HELP + "...\n";
		usage += "\n\tThe parameter value is a list of one or more Buildresults specified by their ID or their label. Prefix the build labels @. The separator is:"
				+ LINK_SEPARATOR_HELP + "\n\n";
		Set<String> wiLinkTypes = ReferenceUtil.getBuild_EndPointDescriptorMap().keySet();
		for (String linktype : wiLinkTypes) {
			usage += "\t" + PSEUDO_ATTRIBUTE_LINK + linktype + "=id1" + LINK_SEPARATOR_HELP + "@BuildLabel2"
					+ LINK_SEPARATOR_HELP + "...\n";
		}
		usage += "\n\n\tExample:";
		usage += "\n\n\t\t" + PSEUDO_ATTRIBUTE_LINK + ReferenceUtil.LINKTYPE_REPORTED_AGAINST_BUILDRESULT
				+ "=@_IjluoH-oEeSHhcw_WFU6CQ" + LINK_SEPARATOR_HELP + "P20141208-1713\n";
		return usage;
	}

	/**
	 * Creates the help output for linking to build results
	 * 
	 * @return a help description
	 */
	private String helpUsageDeleteLinksOfType() {
		if (isBulkUpdate()) {
			return "";
		}
		String usage = "\nFormat is:\n";
		usage += "\t" + PSEUDO_ATTRIBUTE_DELETELINKSOFTYPE + "linktype=yes\n";
		usage += "\n\n\tExample:";
		usage += "\n\n\t\t" + PSEUDO_ATTRIBUTE_DELETELINKSOFTYPE + ReferenceUtil.LINKTYPE_INCLUDEDINBUILD + "=yes\n";
		return usage;
	}

}
