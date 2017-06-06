/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.AbstractTeamRepositoryCommand;
import com.ibm.js.team.workitem.commandline.framework.ParameterValue;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.helper.ColumnHeaderMappingHelper;
import com.ibm.js.team.workitem.commandline.helper.DevelopmentLineHelper;
import com.ibm.js.team.workitem.commandline.helper.WorkItemUpdateHelper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterIDMapper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.js.team.workitem.commandline.utils.AccessContextUtil;
import com.ibm.js.team.workitem.commandline.utils.AttachmentUtil;
import com.ibm.js.team.workitem.commandline.utils.BuildUtil;
import com.ibm.js.team.workitem.commandline.utils.ProcessAreaUtil;
import com.ibm.js.team.workitem.commandline.utils.QueryUtil;
import com.ibm.js.team.workitem.commandline.utils.ReferenceUtil;
import com.ibm.js.team.workitem.commandline.utils.SimpleDateFormatUtil;
import com.ibm.js.team.workitem.commandline.utils.StringUtil;
import com.ibm.js.team.workitem.commandline.utils.WorkItemUtil;
import com.ibm.team.build.common.model.IBuildDefinition;
import com.ibm.team.build.common.model.IBuildResult;
import com.ibm.team.build.common.model.IBuildResultHandle;
import com.ibm.team.links.common.IItemReference;
import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.IURIReference;
import com.ibm.team.links.common.registry.IEndPointDescriptor;
import com.ibm.team.process.common.IAccessGroup;
import com.ibm.team.process.common.IIteration;
import com.ibm.team.process.common.IIterationHandle;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProcessAreaHandle;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.ITeamArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.common.IAuditableHandle;
import com.ibm.team.repository.common.IContext;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.IItem;
import com.ibm.team.repository.common.IItemHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.UUID;
import com.ibm.team.scm.common.IComponent;
import com.ibm.team.workitem.common.internal.attributeValueProviders.SecurityContextProvider;
import com.ibm.team.workitem.common.model.AttributeTypes;
import com.ibm.team.workitem.common.model.IApproval;
import com.ibm.team.workitem.common.model.IApprovalDescriptor;
import com.ibm.team.workitem.common.model.IApprovalState;
import com.ibm.team.workitem.common.model.IApprovals;
import com.ibm.team.workitem.common.model.IAttachment;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.ICategory;
import com.ibm.team.workitem.common.model.ICategoryHandle;
import com.ibm.team.workitem.common.model.IComment;
import com.ibm.team.workitem.common.model.IComments;
import com.ibm.team.workitem.common.model.IDeliverable;
import com.ibm.team.workitem.common.model.IDeliverableHandle;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.ISubscriptions;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.WorkItemApprovals;
import com.ibm.team.workitem.common.query.IQueryDescriptor;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResult;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;
import com.opencsv.CSVWriter;

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
public class ExportWorkItemsCommand extends AbstractTeamRepositoryCommand {

	// Switch to export like RTC would do it
	public static final String SWITCH_RTC_ECLIPSE_EXPORT = "asrtceclipse";
	// The header column uses ID's instead of display names
	public static final String SWITCH_HEADER_AS_ID = "headerIDs";
	// NewLine separator for lists in RTC compatible format
	public static final String SEPERATOR_NEWLINE = "\n";
	// The default separator for lists such as tags
	public static final String SEPERATOR_COMMA = ", ";
	// If there is no value export this
	public static final String CONSTANT_NO_VALUE = "";
	// Parameter for the export file name
	private static final String PARAMETER_EXPORT_FILE = "exportFile";
	private static final String PARAMETER_EXPORT_FILE_EXAMPLE = "\"C:\\temp\\export.csv\"";

	// Parameter to specify the query
	private static final String PARAMETER_QUERY_NAME = "query";
	private static final String PARAMETER_QUERY_NAME_EXAMPLE = "\"All WorkItems\"";

	// parameter to specify a sharing target for the query
	// The sharing target can be the project area or a
	// team area where the query is shared
	private static final String PARAMETER_SHARING_TARGETS = "querysource";
	private static final String PARAMETER_SHARING_TARGETS_EXAMPLE = "\"JKE Banking(Change Management),JKE Banking(Change Management)/Business Recovery Matters\"";
	// parameter to specify which columns are supposed to be exported
	private static final String PARAMETER_EXPORT_COLUMNS = "columns";
	private static final String PARAMETER_EXPORT_COLUMNS_EXAMPLE1 = "\"Type,Id,Planned For,Filed Against,Description,Found In\"";
	// private static final String PARAMETER_EXPORT_COLUMNS_EXAMPLE2 =
	// "\"id,workItemType,internalState,internalPriority,internalSeverity,summary,owner,creator\"";

	// prefix to be used when exporting work item ID's
	public static final String PREFIX_EXISTINGWORKITEM = "#";

	// private static final String SWITCH_COLUMNHEADER_AS_IDs =
	// "columnheader_as_id";

	// The encoding to be used when saving the file
	private String fFileEncoding = IWorkItemCommandLineConstants.DEFAULT_ENCODING_UTF_16LE;
	// Delimiter to be used for columns
	private char fDelimiter = IWorkItemCommandLineConstants.DEFAULT_DELIMITER;
	// RTC export or custom export
	private boolean fRTCEclipseCompatible = false;
	// Export headers as ID's?
	private boolean fHeaderAsIDs = false;
	// Ignore minor errors?
	private boolean fIgnoreErrors = true;
	// The output file
	private File fOutputFile = null;
	// The pattern to export time stamps
	private String fSimpleDateTimeFormatPattern=IWorkItemCommandLineConstants.TIMESTAMP_EXPORT_IMPORT_FORMAT_MMM_D_YYYY_HH_MM_A;
	// Suppress Attribute Not found Exception
	private boolean fSuppressAttributeErrors = false;
	
	/**
	 * The constructor
	 * 
	 * @param parametermanager
	 */
	public ExportWorkItemsCommand(ParameterManager parametermanager) {
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
		return IWorkItemCommandLineConstants.COMMAND_EXPORT_WORKITEMS;
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
		super.setRequiredParameters();
		// Add the parameters required to perform the operation
		getParameterManager()
				.syntaxAddRequiredParameter(
						IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY,
						IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddRequiredParameter(PARAMETER_EXPORT_FILE,
				PARAMETER_EXPORT_FILE_EXAMPLE);
		getParameterManager().syntaxAddRequiredParameter(PARAMETER_QUERY_NAME,
				PARAMETER_QUERY_NAME_EXAMPLE);
		getParameterManager().syntaxAddSwitch(
				IWorkItemCommandLineConstants.SWITCH_IGNOREERRORS);
		getParameterManager().syntaxAddSwitch(SWITCH_HEADER_AS_ID);
		getParameterManager().syntaxAddSwitch(SWITCH_RTC_ECLIPSE_EXPORT);
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.SWITCH_EXPORT_SUPPRESS_ATTRIBUTE_EXCEPTIONS);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractCommand#
	 * helpSpecificUsage()
	 */
	@Override
	public String helpSpecificUsage() {
		return " [" + IWorkItemCommandLineConstants.PARAMETER_ENCODING
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR
				+ IWorkItemCommandLineConstants.PARAMETER_ENCODING_EXAMPLE + "]" 
				+ " [" + IWorkItemCommandLineConstants.PARAMETER_DELIMITER
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR
				+ IWorkItemCommandLineConstants.PARAMETER_DELIMITER_EXAMPLE	+ "]" 
				+ " [" + PARAMETER_EXPORT_COLUMNS
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR
				+ PARAMETER_EXPORT_COLUMNS_EXAMPLE1 + "]" 
				+ " [" + PARAMETER_SHARING_TARGETS
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR
				+ PARAMETER_SHARING_TARGETS_EXAMPLE + "]"
				+ "[" + IWorkItemCommandLineConstants.PARAMETER_TIMESTAMP_ENCODING
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR
				+ IWorkItemCommandLineConstants.PARAMETER_TIMESTAMP_ENCODING_EXAMPLE + "]" 
;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.js.team.workitem.commandline.framework.AbstractCommand#process()
	 */
	@Override
	public OperationResult process() throws TeamRepositoryException {
		setIgnoreErrors(getParameterManager().hasSwitch(
				IWorkItemCommandLineConstants.SWITCH_IGNOREERRORS));
		setRTCEclipseExport(getParameterManager().hasSwitch(
				SWITCH_RTC_ECLIPSE_EXPORT));
		setHeaderAsIDs(getParameterManager().hasSwitch(SWITCH_HEADER_AS_ID));
		setSuppressAttributeErrors(getParameterManager().hasSwitch(IWorkItemCommandLineConstants.SWITCH_EXPORT_SUPPRESS_ATTRIBUTE_EXCEPTIONS));
		
		String projectAreaName = getParameterManager()
				.consumeParameter(
						IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY)
				.trim();
		// Find the project area
		IProjectArea projectArea = ProcessAreaUtil.findProjectAreaByFQN(
				projectAreaName, getProcessClientService(), getMonitor());
		if (projectArea == null) {
			throw new WorkItemCommandLineException("Project Area not found: "
					+ projectAreaName);
		}
		ColumnHeaderMappingHelper columnHeaderMapping = new ColumnHeaderMappingHelper(
				projectArea, getWorkItemCommon(), getMonitor());

		String queryName = getParameterManager().consumeParameter(
				PARAMETER_QUERY_NAME);
		if (queryName == null) {
			throw new WorkItemCommandLineException(
					"Query name must not be provided.");
		}
		String sharingTargetNames = getParameterManager().consumeParameter(
				PARAMETER_SHARING_TARGETS);

		String filePath = getParameterManager().consumeParameter(
				PARAMETER_EXPORT_FILE);
		if (filePath == null) {
			throw new WorkItemCommandLineException(
					"Export file path must be provided.");
		}

		// Read if there is a special encoding provided
		String encoding = getParameterManager().consumeParameter(
				IWorkItemCommandLineConstants.PARAMETER_ENCODING);
		if (encoding != null) {
			setFileEncoding(encoding.trim());
		}

		// Read if there is a special date time format pattern provided
		String dateTimeFormatPattern = getParameterManager().consumeParameter(
				IWorkItemCommandLineConstants.PARAMETER_TIMESTAMP_ENCODING);
		if (dateTimeFormatPattern != null) {
			setSimpleDateTimeFormatPattern(dateTimeFormatPattern.trim());
		}

		// Can not "trim() to allow whitespace characters"
		String delimiter = getParameterManager().consumeParameter(
				IWorkItemCommandLineConstants.PARAMETER_DELIMITER);
		if (delimiter != null) {
			setDelimiter(delimiter);
		}
		
		// get the columns to export
		String columns = getParameterManager().consumeParameter(
				PARAMETER_EXPORT_COLUMNS);
		if (columns != null) {
			columnHeaderMapping.setColumns(columns);
		}

		// Create the writer
		CSVWriter writer = createWriter(filePath);
		List<String> headerNames = columnHeaderMapping
				.analyzeColumnHeader(getHeaderAsIDs());
		writer.writeNext(headerNames.toArray(new String[headerNames.size()]));

		// Get the query
		IQueryDescriptor query = null;
		if (sharingTargetNames == null) {
			// If there is no sharing target try to find a personal query
			query = QueryUtil.findPersonalQuery(queryName, projectArea,
					getTeamRepository().loggedInContributor(), getMonitor());
		} else {
			List<IAuditableHandle> sharingTargets = QueryUtil
					.findSharingTargets(sharingTargetNames,
							getProcessClientService(), getMonitor());
			if (sharingTargets == null) {
				throw new WorkItemCommandLineException(
						"ProcessArea that shares the query not found "
								+ sharingTargetNames);
			}
			query = QueryUtil.findSharedQuery(queryName, projectArea,
					sharingTargets, getMonitor());

		}
		if (query == null) {
			throw new WorkItemCommandLineException("Query not found "
					+ queryName);
		}

		// Query the work items
		IQueryResult<IResult> results = QueryUtil.getUnresolvedQueryResult(
				query, isOverrideQueryResultSizeLimit());

		while (results.hasNext(null)) {
			IResult result = results.next(null);
			IWorkItem workItem = WorkItemUtil.resolveWorkItem(
					(IWorkItemHandle) result.getItem(), IWorkItem.FULL_PROFILE,
					getWorkItemCommon(), getMonitor());
			ArrayList<String> row = getRow(workItem,
					columnHeaderMapping.getParameters());
			writer.writeNext(row.toArray(new String[row.size()]));
		}
		try {
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new WorkItemCommandLineException(e);
		}
		setSuccess();
		return getResult();
	}

	/**
	 * Create a CSV file writer to read the CSV file with the specific encoding
	 * 
	 * @param filePath
	 * @return
	 * @throws WorkItemCommandLineException
	 */
	private CSVWriter createWriter(String filePath)
			throws WorkItemCommandLineException {
		CSVWriter writer = null;
		fOutputFile = new File(filePath);
		try {
			// @see http://opencsv.sourceforge.net/
			writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(
					fOutputFile), getFileEncoding()), getDelimiter(),
					getQuoteChar());
		} catch (UnsupportedEncodingException e) {
			throw new WorkItemCommandLineException(
					"Exception creating output writer!", e);
		} catch (FileNotFoundException e) {
			throw new WorkItemCommandLineException(
					"Exception creating output writer!", e);
		}
		return writer;
	}

	/**
	 * Get the values for a row from the work items attributes.
	 * 
	 * @param workItem
	 * @param columns
	 * @return
	 * @throws WorkItemCommandLineException
	 * @throws TeamRepositoryException
	 */
	private ArrayList<String> getRow(IWorkItem workItem,
			List<ParameterValue> columns) throws WorkItemCommandLineException,
			TeamRepositoryException {
		ArrayList<String> row = new ArrayList<String>(columns.size());
		getResult().appendResultString(
				"Exporting work item " + workItem.getId());
		for (int i = 0; i < columns.size(); i++) {
			ParameterValue column = columns.get(i);
			String value = "";
			try {
				value = getStringRepresentation(workItem, column);
			} catch (WorkItemCommandLineException e) {
				String message = "Exception exporting work item "
						+ workItem.getId() + " column " + i + " attribute "
						+ column.getAttributeID() + " : " + e.getMessage();
				if (isIgnoreErrors()) {
					if(!isSuppressAttributeErrors()){
						this.getResult().appendResultString(message + " Ignored!");
					}
				} else {
					throw new WorkItemCommandLineException(message, e);
				}
			}
			row.add(i, value);
		}
		return row;
	}

	/**
	 * This method tries to get the matching representation of the value to be
	 * set for a work item attribute. It basically goes through a list of
	 * properties an attribute can have and locates the target type. Based on
	 * that type it tries to create a matching value. The value is returned if
	 * it was possible to create it.
	 * 
	 * @param column
	 *            - the IAttribute to find the representation for
	 * @param value
	 *            - the string value that is to be transformed.
	 * @param value2
	 * @return
	 * @throws TeamRepositoryException
	 * @throws WorkItemCommandLineException
	 */
	private String getStringRepresentation(IWorkItem workItem,
			ParameterValue column) throws TeamRepositoryException,
			WorkItemCommandLineException {
		if (column == null) {
			return CONSTANT_NO_VALUE;
		}

		String attributeID = column.getAttributeID();
		if (attributeID == null) {
			throw new WorkItemCommandLineException(
					"AttributeID can not be null");
		}
		IAttribute attribute = column.getIAttribute();
		if (attribute == null) {
			// If I don't get an attribute, this is a link or it is not
			// supported
			if (ReferenceUtil.isLinkType(attributeID)) {
				return calculateLinkAsString(workItem, attributeID);
			}
			if (attributeID
					.trim()
					.toLowerCase()
					.equals(ParameterIDMapper.PSEUDO_ATTRIBUTE_ATTACHMENTS
							.toLowerCase())) {
				return calculateAttachmentsAsString(workItem);
			}
			
			throw new WorkItemCommandLineException("Attribute not found ID: " + attributeID);
			
		}
		if (!workItem.hasAttribute(attribute)) {
			return CONSTANT_NO_VALUE;
		}
		String attribType = attribute.getAttributeType();

		if (attribute.getIdentifier().equals(IWorkItem.STATE_PROPERTY)) {
			// Handle states
			return calculateStateAsString(workItem);
		}
		if (attribType.equals(AttributeTypes.APPROVALS)) {
			// Handle approvals
			return calculateApprovalsAsString(workItem);
		}
		if (attribType.equals(AttributeTypes.COMMENTS)) {
			// Handle comments
			return calculateCommentsAsString(workItem);
		}
		if (attribType.equals(AttributeTypes.SUBSCRIPTIONS)) {
			// handle subscriptions
			return calculateSubscriptionsAsString(workItem);
		}

		Object value = workItem.getValue(attribute);
		// Handle list attribute types first
		if (AttributeTypes.isListAttributeType(attribType)) {
			if (AttributeTypes.isItemListAttributeType(attribType)) {
				// Item List Types that are supported
				if (attribType.equals(AttributeTypes.CONTRIBUTOR_LIST)) {
					// A list of contributors
					return calculateContributorListAsString(value,
							SEPERATOR_NEWLINE);
				}
				if (attribType.equals(AttributeTypes.PROCESS_AREA_LIST)) {
					// A list of process areas (ProjectArea/TeamArea)
					return calculateProcessAreaListAsString(value, false);
				}
				if (attribType.equals(AttributeTypes.PROJECT_AREA_LIST)) {
					// A list of process areas (ProjectAreas)
					return calculateProcessAreaListAsString(value, false);
				}
				if (attribType.equals(AttributeTypes.TEAM_AREA_LIST)) {
					// A list of process areas (TeamAreas)
					return calculateProcessAreaListAsString(value, false);
				}
				if (attribType.equals(AttributeTypes.WORK_ITEM_LIST)) {
					// A list of work items
					return calculateWorkItemListAsString(value);
				}
				if (attribType.equals(AttributeTypes.ITEM_LIST)) {
					// ItemList with unspecified IItems
					return calculateItemListAsString(value);
				}
			}
			if (attribType.equals(AttributeTypes.TAGS)) {
				// Handle Tags - also detected as list type
				return calculateTagListAsString(value);
			}
			if (attribType.equals(AttributeTypes.STRING_LIST)) {
				// A list of strings
				return calculateStringListAsString(value);
			}
			if (AttributeTypes.isEnumerationListAttributeType(attribType)) {
				// Handle all Enumeration List Types
				return calculateEnumerationLiteralListAsString(value, attribute);
			}
			throw new WorkItemCommandLineException(
					"Type not recognized - type not yet supported: "
							+ attribType + " ID " + attribute.getIdentifier());
		} else {
			// Handle non list types - the simple ones first.

			if (attribType.equals(AttributeTypes.WIKI)) {
				return calculateString(value);
			}
			if (AttributeTypes.STRING_TYPES.contains(attribType)) {
				return calculateString(value);
			}
			if (AttributeTypes.HTML_TYPES.contains(attribType)) {
				return calculateString(value);
			}
			if (attribType.equals(AttributeTypes.BOOLEAN)) {
				if (value instanceof Boolean) {
					return ((Boolean) value).toString();
				}
				throw new WorkItemCommandLineException(
						"Type not expected - expected boolean: " + attribType
								+ " ID " + attribute.getIdentifier());
			}
			if (AttributeTypes.NUMBER_TYPES.contains(attribType)) {
				// different number types
				try {
					if (attribType.equals(AttributeTypes.DURATION)) {
						return calculateDurationAsString(value, attribType);
					}

					return calculateNumberAsString(value, attribType);
				} catch (NumberFormatException e) {
					throw new WorkItemCommandLineException(
							"Attribute Value not valid - Number format exception: "
									+ value, e);
				}
			}
			if (attribType.equals(AttributeTypes.DELIVERABLE)) {
				// Handle deliverables - Found In and other attributes
				// referencing a release.
				return calculateDeliverableAsString(value);
			}
			if (attribType.equals(AttributeTypes.CATEGORY)) {
				// Work item category - Filed Against and other attributes
				return calculateCategoryAsString(value);
			}
			if (attribType.equals(AttributeTypes.ITERATION)) {
				// Iterations - Planned For and other such attributes
				return calculateIterationAsString(value);
			}
			if (attribType.equals(AttributeTypes.CONTRIBUTOR)) {
				// Contributors - user ID's
				return calculateContributorAsString(value);
			}
			if (attribType.equals(AttributeTypes.TIMESTAMP)) {
				// Timestamp types e.g. dates
				return calculateTimestampAsString(value);
			}
			if (attribType.equals(AttributeTypes.PROJECT_AREA)) {
				// ProjectArea type attributes
				return calculateProcessAreaAsString(value, false);
			}
			if (attribType.equals(AttributeTypes.TEAM_AREA)) {
				// TeamArea type attributes
				return calculateProcessAreaAsString(value, false);
			}
			if (attribType.equals(AttributeTypes.PROCESS_AREA)) {
				// Process Area type attributes (TeamArea/ProjectArea)
				return calculateProcessAreaAsString(value, false);
			}
			if (attribType.equals(AttributeTypes.WORK_ITEM)) {
				// Work Item attributes
				return calculateWorkItemAsString(value);
			}
			if (attribType.equals(AttributeTypes.ITEM)) {
				// Handle items where the type is not specified in the attribute
				return calculateItemAsString(value);
			}
			if (attribType.equals(AttributeTypes.TYPE)) {
				// The work item type
				return calculateWorkItemTypeAsString(workItem, value);
			}
			if (AttributeTypes.isEnumerationAttributeType(attribType)) {
				// Handle all enumeration types
				return calculateEnumerationLiteralAsString(value, attribute);
			}
			if (attribType.equals(AttributeTypes.UUID)) {
				// The work item restricted Access UUID
				return calculateUUIDAsString(value, attribute);
			}

			// In case we forgot something or a new type gets implemented
			throw new WorkItemCommandLineException(
					"AttributeType not yet supported: " + attribType + " ID "
							+ attribute.getIdentifier());
		}
	}

	/**
	 * @param workItem
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateAttachmentsAsString(IWorkItem workItem)
			throws TeamRepositoryException {
		String rootFoldeName = fOutputFile.getParentFile().getAbsolutePath();
		String relativePath = ParameterIDMapper.PSEUDO_ATTRIBUTE_ATTACHMENTS
				+ File.separator + workItem.getId();
		String outputFolderName = rootFoldeName + File.separator + relativePath;
		List<String> resultList = new ArrayList<String>();
		List<IAttachment> attachments = AttachmentUtil.saveAttachmentsToDisk(
				new File(outputFolderName), workItem, getWorkItemCommon(),
				getMonitor());
		for (IAttachment attachment : attachments) {
			String result = "";
			if (isRTCEclipseExport()) {
				result = attachment.getName();
			} else {
				String fileName = "." + File.separator + relativePath
						+ File.separator + attachment.getName();
				fileName = fileName.replace("\\", "/");
				result = fileName + WorkItemUpdateHelper.ATTACHMENT_SEPARATOR
						+ attachment.getDescription()
						+ WorkItemUpdateHelper.ATTACHMENT_SEPARATOR
						+ attachment.getContent().getContentType()
						+ WorkItemUpdateHelper.ATTACHMENT_SEPARATOR
						+ attachment.getContent().getCharacterEncoding();
			}
			resultList.add(result);
		}
		return StringUtil.listToString(resultList, SEPERATOR_NEWLINE);
	}

	/**
	 * Get the string representation for a referenced object
	 * 
	 * @param workItem
	 * @param linkTypeID
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateLinkAsString(IWorkItem workItem, String linkTypeID)
			throws TeamRepositoryException {
		String linkType = ReferenceUtil.getReferenceType(linkTypeID);
		if (linkType == null) {
			throw new WorkItemCommandLineException(
					"Linktype not yet supported: ID " + linkTypeID);
		}
		IEndPointDescriptor endpoint = ReferenceUtil
				.getReferenceEndpointDescriptor(linkTypeID);
		IWorkItemReferences wiReferences = getWorkItemCommon()
				.resolveWorkItemReferences(workItem, getMonitor());
		List<String> referenceRepresentations = new ArrayList<String>();
		List<IReference> references = wiReferences.getReferences(endpoint);
		for (IReference aReference : references) {

			if (linkType.equals(ReferenceUtil.CATEGORY_LINKTYPE_WORK_ITEM)) {
				// get the reference and calculate the value.
				if (aReference.isItemReference()) {
					IItemHandle referencedItem = ((IItemReference) aReference)
							.getReferencedItem();
					if (referencedItem instanceof IWorkItemHandle) {
						IWorkItem item = WorkItemUtil.resolveWorkItem(
								(IWorkItemHandle) referencedItem,
								IWorkItem.SMALL_PROFILE, getWorkItemCommon(),
								getMonitor());
						referenceRepresentations.add(PREFIX_EXISTINGWORKITEM
								+ Integer.toString(item.getId()));
					}
				} else {
					throw new WorkItemCommandLineException(
							"Unexpected reference type ItemReference expected: "
									+ linkTypeID);
				}
			} else if (linkType
					.equals(ReferenceUtil.CATEGORY_LINKTYPE_CLM_WORKITEM)) {
				referenceRepresentations.add(getURIReferenceAsString(
						aReference, linkTypeID));
			} else if (linkType.equals(ReferenceUtil.CATEGORY_LINKTYPE_CLM_URI)) {
				referenceRepresentations.add(getURIReferenceAsString(
						aReference, linkTypeID));
			} else if (linkType.equals(ReferenceUtil.CATEGORY_LINKTYPE_BULD)) {
				referenceRepresentations.add(getItemReferenceAsString(
						aReference, linkTypeID));
			}
		}
		return calculateStringListAsString(referenceRepresentations);
	}

	/**
	 * Create the presentation for a URI Reference such as tracks links and
	 * other items in other applications
	 * 
	 * @param uriReference
	 * @param linkTypeID
	 * @return
	 */
	private String getURIReferenceAsString(IReference uriReference,
			String linkTypeID) {
		if (uriReference.isURIReference()) {
			IURIReference reference = ((IURIReference) uriReference);

			if (isRTCEclipseExport()) {
				return reference.getComment();
			} else {
				return reference.getURI().toString();
			}
		} else {
			throw new WorkItemCommandLineException(
					"Unexpected reference type URIReference expected: "
							+ linkTypeID);
		}
	}

	/**
	 * Create the presentation for an item reference - Work Item and build
	 * result
	 * 
	 * @param reference
	 * @param linkTypeID
	 * @return
	 * @throws WorkItemCommandLineException
	 * @throws TeamRepositoryException
	 */
	private String getItemReferenceAsString(IReference reference,
			String linkTypeID) throws WorkItemCommandLineException,
			TeamRepositoryException {
		// get the reference and calculate the value.
		if (reference.isItemReference()) {
			IItemHandle referencedItem = ((IItemReference) reference)
					.getReferencedItem();
			if (referencedItem instanceof IBuildResultHandle) {
				IBuildResult buildResult = BuildUtil.resolveBuildResult(
						(IBuildResultHandle) referencedItem,
						getTeamRepository(), getMonitor());
				if (isRTCEclipseExport()) {
					IBuildDefinition buildDefinition = BuildUtil
							.resolveBuildDefinition(
									buildResult.getBuildDefinition(),
									getTeamRepository(), getMonitor());
					return buildDefinition.getId() + " "
							+ buildResult.getLabel();
				} else {
					return buildResult.getLabel();
				}
			}
			if (referencedItem instanceof IWorkItemHandle) {
				IWorkItem item = WorkItemUtil.resolveWorkItem(
						(IWorkItemHandle) referencedItem,
						IWorkItem.SMALL_PROFILE, getWorkItemCommon(),
						getMonitor());
				return PREFIX_EXISTINGWORKITEM + Integer.toString(item.getId());
			}
		}
		throw new WorkItemCommandLineException(
				"Unexpected reference type ItemReference expected: "
						+ linkTypeID);
	}

	/**
	 * Convert a work item state to a string
	 * 
	 * @param workItem
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateStateAsString(IWorkItem workItem)
			throws TeamRepositoryException {
		Identifier<IState> state = workItem.getState2();
		IWorkflowInfo wfInfo = getWorkItemCommon().findWorkflowInfo(workItem,
				getMonitor());
		String stateName = wfInfo.getStateName(state);
		if (stateName == null) {
			return "";
		}
		return stateName;
	}

	/**
	 * Get the subscribers and create a string that contains the ID's
	 * 
	 * @param workItem
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateSubscriptionsAsString(IWorkItem workItem)
			throws TeamRepositoryException {
		ISubscriptions subscriptions = workItem.getSubscriptions();
		IContributorHandle[] contributors = subscriptions.getContents();
		List<IContributorHandle> subscribers = Arrays.asList(contributors);
		return calculateContributorListAsString(subscribers, SEPERATOR_NEWLINE);
	}

	/**
	 * Get the name of the object described by the UUID as a string
	 * representation
	 * 
	 * @param value
	 * @param attribute
	 * @return
	 * @throws TeamRepositoryException
	 * 
	 * @see SecurityContextProvider
	 */
	private String calculateUUIDAsString(Object value, IAttribute attribute)
			throws TeamRepositoryException {
		if (value != null) {
			if (value instanceof UUID) {
				UUID uuid = (UUID) value;
				return getAccessContextFromUUID(uuid);
			}
		}
		return CONSTANT_NO_VALUE;
	}

	/**
	 * For a given value from the restricted access attribute, compute the name
	 * of the object. First try if this is a project area, then try a team area,
	 * finally search through the groups.
	 * 
	 * @param uuid
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String getAccessContextFromUUID(UUID uuid)
			throws TeamRepositoryException {
		if (uuid != null && IContext.PUBLIC.equals(uuid)) {
			return AccessContextUtil.PUBLIC_ACCESS;
		}
		Object context = AccessContextUtil.getAccessContextFromUUID(uuid,
				getTeamRepository(), getAuditableCommon(), getMonitor());
		if (context == null) {
			return CONSTANT_NO_VALUE;
		}
		if (context instanceof IProcessArea) {
			IProcessArea pa = (IProcessArea) context;
			if (isRTCEclipseExport()) {
				return pa.getName();
			}
			return ProcessAreaUtil.getFullQualifiedName(pa, getMonitor());
		}
		if (context instanceof IAccessGroup) {
			IAccessGroup accessgroup = (IAccessGroup) context;
			return accessgroup.getName();
		}
		return CONSTANT_NO_VALUE;
	}

	/**
	 * Convert the comments data into a string containing all the comments
	 * 
	 * @param workItem
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateCommentsAsString(IWorkItem workItem)
			throws TeamRepositoryException {

		IComments comments = workItem.getComments();
		IComment[] theComments = comments.getContents();
		List<String> commentText = new ArrayList<String>(theComments.length);
		int i = 1;
		for (IComment aComment : theComments) {
			if (i > 1) {
				commentText.add("\r");
			}
			commentText.add(i + ". " + getCommentAsString(aComment));
			i++;
		}
		return StringUtil.listToString(commentText, SEPERATOR_NEWLINE);
	}

	/**
	 * Get one comment as string
	 * 
	 * @param aComment
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String getCommentAsString(IComment aComment)
			throws TeamRepositoryException {
		String creator = calculateContributorAsString(aComment.getCreator());
		String creationDate = calculateTimestampAsString(aComment
				.getCreationDate());
		return creator + " - " + creationDate + SEPERATOR_NEWLINE
				+ aComment.getHTMLContent().getPlainText();
	}

	/**
	 * Compute the string representation for an approval
	 * 
	 * <pre>
	 * 
	 * "Approval Subject: Pending (2 of 3) Review Subject: Pending (1 of 1)
	 * Verification Subject: Approved (1 of 1) Verification Subject 2: Pending
	 * (0 of 0)"
	 * 
	 * </pre>
	 * 
	 * @param workItem
	 * @return
	 */
	private String calculateApprovalsAsString(IWorkItem workItem) {

		IApprovals approvals = workItem.getApprovals();
		Map<IApprovalDescriptor, Collection<IApproval>> approvalmap = WorkItemApprovals
				.groupByApprovalDescriptors(approvals);
		List<IApprovalDescriptor> descriptors = approvals.getDescriptors();
		List<String> resultList = new ArrayList<String>(descriptors.size());
		for (IApprovalDescriptor approvalDescriptor : descriptors) {

			resultList.add(getApprovalAsString(approvalDescriptor,
					approvalmap.get(approvalDescriptor)));
		}
		return StringUtil.listToString(resultList, SEPERATOR_NEWLINE);
	}

	/**
	 * Compute the string representation for an approval
	 * 
	 * <pre>
	 * 
	 * "Approval Subject: Rejected (1 of 3) Review Subject: Pending (1 of 1)
	 * Verification Subject: Approved (1 of 1) Verification Subject 2: Pending
	 * (0 of 0) Review: Pending (4 of 4)"
	 * 
	 * </pre>
	 * 
	 * @param approvals
	 * @param approvalDescriptor
	 * @param collection
	 * @return
	 */
	private String getApprovalAsString(IApprovalDescriptor approvalDescriptor,
			Collection<IApproval> approvals) {
		String approvalAsText = "";
		approvalAsText += approvalDescriptor.getName() + ": ";
		// colon as separator
		IApprovalState approvalOverAllState = WorkItemApprovals
				.getState(approvalDescriptor.getCumulativeStateIdentifier());
		approvalAsText += approvalOverAllState.getDisplayName();
		int approvalStateCount = 0;
		int approverCount = 0;
		for (IApproval approval : approvals) {
			approverCount++;
			String approvalState = approval.getStateIdentifier();
			if (approvalOverAllState.getIdentifier().equals(approvalState)) {
				approvalStateCount++;
			}
		}
		approvalAsText += " (" + approvalStateCount + " of " + approverCount
				+ ")";
		return approvalAsText;
	}

	/**
	 * Compute a string representation for the enumeration literal list
	 * 
	 * @param value
	 * @param attribute
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateEnumerationLiteralListAsString(Object value,
			IAttribute attribute) throws TeamRepositoryException {
		if (value == null) {
			return CONSTANT_NO_VALUE;
		}
		List<String> resultList = new ArrayList<String>();
		if (!(value instanceof List<?>)) {
			return "Result not a List type";
		}
		List<?> valueList = (List<?>) value;
		for (Object object : valueList) {
			resultList.add(calculateEnumerationLiteralAsString(object,
					attribute));
		}
		return StringUtil.listToString(resultList, SEPERATOR_NEWLINE);
	}

	/**
	 * Compute a string representation for one enumeration literal
	 * 
	 * @param value
	 * @param attribute
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateEnumerationLiteralAsString(Object value,
			IAttribute attribute) throws TeamRepositoryException {
		if (value == null) {
			return CONSTANT_NO_VALUE;
		}
		if (!(value instanceof Identifier<?>)) {
			return "Value not an enumeration literal";
		}
		IEnumeration<? extends ILiteral> enumeration = getWorkItemCommon()
				.resolveEnumeration(attribute, getMonitor());

		@SuppressWarnings("unchecked")
		Identifier<? extends ILiteral> currentIdentifier = (Identifier<? extends ILiteral>) value;
		ILiteral literal = enumeration
				.findEnumerationLiteral(currentIdentifier);
		return literal.getName();
	}

	/**
	 * Compute a string representation for a work item type
	 * 
	 * @param workItem
	 * @param value
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateWorkItemTypeAsString(IWorkItem workItem,
			Object value) throws TeamRepositoryException {
		// I get the ID
		if (!(value instanceof String)) {
			return "Value not a String";
		}
		IWorkItemType workItemType = getWorkItemCommon().findWorkItemType(
				workItem.getProjectArea(), (String) value, getMonitor());

		return workItemType.getDisplayName();
	}

	/**
	 * Compute a string representation for an IItem
	 * 
	 * @param value
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateItemAsString(Object value)
			throws TeamRepositoryException {
		String prefix = "";
		if (value == null) {
			return CONSTANT_NO_VALUE;
		}
		if (!(value instanceof IItemHandle)) {
			return "Value not a IItemHandle";
		}
		IItemHandle handle = (IItemHandle) value;
		// Resolve handle
		IItem item = getTeamRepository().itemManager().fetchCompleteItem(
				handle, IItemManager.DEFAULT, getMonitor());
		if (item instanceof IProcessArea) {
			return calculateProcessAreaAsString(value, true);
		}
		if (item instanceof ICategory) {
			if (!isRTCEclipseExport()) {
				prefix = WorkItemUpdateHelper.TYPE_CATEGORY
						+ WorkItemUpdateHelper.ITEMTYPE_SEPARATOR;
			}
			return prefix + calculateCategoryAsString(value);
		}
		if (item instanceof IContributor) {
			if (!isRTCEclipseExport()) {
				prefix = WorkItemUpdateHelper.TYPE_CONTRIBUTOR
						+ WorkItemUpdateHelper.ITEMTYPE_SEPARATOR;
			}
			return prefix + calculateContributorAsString(value);
		}
		if (item instanceof IIteration) {
			if (!isRTCEclipseExport()) {
				prefix = WorkItemUpdateHelper.TYPE_ITERATION
						+ WorkItemUpdateHelper.ITEMTYPE_SEPARATOR;
			}
			return prefix + calculateIterationAsString(value);
		}
		if (item instanceof IWorkItem) {
			if (!isRTCEclipseExport()) {
				prefix = WorkItemUpdateHelper.TYPE_WORKITEM
						+ WorkItemUpdateHelper.ITEMTYPE_SEPARATOR;
			}
			return prefix + calculateWorkItemAsString(value);
		}
		if (item instanceof IComponent) {
			if (!isRTCEclipseExport()) {
				prefix = WorkItemUpdateHelper.TYPE_SCM_COMPONENT
						+ WorkItemUpdateHelper.ITEMTYPE_SEPARATOR;
			}
			return prefix + ((IComponent) item).getName();
		}
		return "Unrecognized Item Type: " + item.getItemType().getName();
	}

	/**
	 * Creates the string representation of a work item
	 * 
	 * @param value
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateWorkItemAsString(Object value)
			throws TeamRepositoryException {
		if (value == null) {
			return CONSTANT_NO_VALUE;
		}
		if (!(value instanceof IWorkItemHandle)) {
			throw new WorkItemCommandLineException(
					"Calculate work item - Incompatible Type Exception: "
							+ value.toString());
		}
		// Resolve handle
		IWorkItem workItem = (IWorkItem) getTeamRepository().itemManager()
				.fetchCompleteItem((IWorkItemHandle) value,
						IItemManager.DEFAULT, getMonitor());
		if (isRTCEclipseExport()) {
			return PREFIX_EXISTINGWORKITEM + workItem.getId();
		}
		return new Integer(workItem.getId()).toString();
	}

	/**
	 * Compute the string representation for a process area
	 * 
	 * @param value
	 * @param asItem
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateProcessAreaAsString(Object value, boolean asItem)
			throws TeamRepositoryException {
		if (value == null) {
			return CONSTANT_NO_VALUE;
		}
		if (!(value instanceof IProcessAreaHandle)) {
			throw new WorkItemCommandLineException(
					"Convert process area - Incompatible Type Exception: "
							+ value.toString());
		}
		if (isRTCEclipseExport()) {
			return ProcessAreaUtil.getName((IProcessAreaHandle) value,
					getMonitor());
		}
		String prefix = "";
		IProcessArea area = ProcessAreaUtil.resolveProcessArea(
				(IProcessAreaHandle) value, getMonitor());
		if (asItem) {
			if (area instanceof IProjectArea) {
				prefix = WorkItemUpdateHelper.TYPE_PROJECT_AREA
						+ WorkItemUpdateHelper.ITEMTYPE_SEPARATOR;
			} else if (area instanceof ITeamArea) {
				prefix = WorkItemUpdateHelper.TYPE_TEAM_AREA
						+ WorkItemUpdateHelper.ITEMTYPE_SEPARATOR;
			} else {
				prefix = WorkItemUpdateHelper.TYPE_PROCESS_AREA
						+ WorkItemUpdateHelper.ITEMTYPE_SEPARATOR;
			}
		}
		return prefix
				+ ProcessAreaUtil.getFullQualifiedName(area, getMonitor());
	}

	/**
	 * Compute the string representation for a timestamp
	 * 
	 * 
	 * @param value
	 * @return
	 */
	private String calculateTimestampAsString(Object value) {
		if (value != null) {
			if (value instanceof Timestamp) {
				Timestamp timestamp = (Timestamp) value;
				return SimpleDateFormatUtil.getDate(timestamp,
						getSimpleDateTimeFormatPattern());
			}
			throw new WorkItemCommandLineException(
					"Convert timestamp - Incompatible Type Exception: "
							+ value.toString());
		}
		return CONSTANT_NO_VALUE;
	}

	/**
	 * Compute the string representation for a contributor/user
	 * 
	 * @param value
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateContributorAsString(Object value)
			throws TeamRepositoryException {
		if (value == null) {
			return CONSTANT_NO_VALUE;
		}
		if ((value instanceof IContributorHandle)) {
			IContributor contributor = (IContributor) getTeamRepository()
					.itemManager().fetchCompleteItem(
							(IContributorHandle) value, IItemManager.DEFAULT,
							getMonitor());
			return contributor.getName();
		}
		throw new WorkItemCommandLineException(
				"Convert Contributor - Incompatible Type Exception: "
						+ value.toString());
	}

	/**
	 * For a given iteration, calculate the value to export. Two modes are
	 * supported, - one exports only the label of the iteration - one exports
	 * the full path including the timeline
	 * 
	 * @param value
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateIterationAsString(Object value)
			throws TeamRepositoryException {
		if (value != null) {
			if (value instanceof IIterationHandle) {
				DevelopmentLineHelper dh = new DevelopmentLineHelper(
						getTeamRepository(), getMonitor());
				if (isRTCEclipseExport()) {
					// RTC Eclipose export only exports the Label of the
					// iteration e.g. "Sprint 1"
					return dh.getIterationAsString((IIterationHandle) value,
							DevelopmentLineHelper.BYLABEL);
				} else {
					// This reports the path e.g.
					// "Main Development/Release 1.0/Sprint 1"
					return dh.getIterationAsFullPath((IIterationHandle) value,
							DevelopmentLineHelper.BYLABEL);
				}
			}
			throw new WorkItemCommandLineException(
					"Convert iteration - Incompatible Type Exception: "
							+ value.toString());
		}
		return CONSTANT_NO_VALUE;
	}

	/**
	 * Compute the string representation for a category
	 * 
	 * @param value
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateCategoryAsString(Object value)
			throws TeamRepositoryException {
		if (value != null) {
			if (value instanceof ICategoryHandle) {
				return getWorkItemCommon().resolveHierarchicalName(
						(ICategoryHandle) value, getMonitor());
			}
			throw new WorkItemCommandLineException(
					"Convert Category - Incompatible Type Exception: "
							+ value.toString());
		}
		return CONSTANT_NO_VALUE;
	}

	/**
	 * Compute the string representation for a delivery/release
	 * 
	 * @param value
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateDeliverableAsString(Object value)
			throws TeamRepositoryException {
		if (value != null) {
			if (value instanceof IDeliverableHandle) {
				IDeliverable deliverable = (IDeliverable) getTeamRepository()
						.itemManager().fetchCompleteItem(
								(IDeliverableHandle) value,
								IItemManager.DEFAULT, getMonitor());
				return deliverable.getName();
			}
			throw new WorkItemCommandLineException(
					"Calculate deliverable - Incompatible Type Exception: "
							+ value.toString());
		}
		return CONSTANT_NO_VALUE;
	}

	/**
	 * Compute the string representation for a number
	 * 
	 * @param value
	 * @param format
	 * @return
	 */
	private String calculateNumberAsString(Object value, String format) {
		if (value == null) {
			return CONSTANT_NO_VALUE;
		}
		if (value instanceof Integer) {
			return ((Integer) value).toString();
		}
		if (value instanceof Long) {
			return ((Long) value).toString();
		}
		if (value instanceof Float) {
			return ((Float) value).toString();
		}
		if (value instanceof BigDecimal) {
			return ((BigDecimal) value).toString();
		}
		throw new WorkItemCommandLineException(
				"Calculate number - Incompatible Type Exception: "
						+ value.toString());
	}

	/**
	 * Compute the string representation for a duration
	 * 
	 * @param value
	 * @param attribType
	 * @return
	 */
	private String calculateDurationAsString(Object value, String attribType) {
		if (value != null) {
			if (value instanceof Long) {
				Long milliseconds = (Long) value;
				return SimpleDateFormatUtil.convertToTimeSpent(milliseconds);
			}
			throw new WorkItemCommandLineException(
					"Calculate Duration - Incompatible Type Exception: "
							+ value.toString());
		}
		return CONSTANT_NO_VALUE;
	}

	/**
	 * Compute the string representation for a string object
	 * 
	 * @param value
	 * @return
	 */
	private String calculateString(Object value) {
		if (value != null) {
			if (value instanceof String) {
				return (String) value;
			}
			throw new WorkItemCommandLineException(
					"Convert string - Incompatible Type Exception: "
							+ value.toString());
		}
		return CONSTANT_NO_VALUE;
	}

	/**
	 * Compute the string representation for a string list object
	 * 
	 * @param value
	 * @return
	 */
	private String calculateStringListAsString(Object value) {
		if (value == null) {
			return CONSTANT_NO_VALUE;
		}
		List<String> resultList = new ArrayList<String>();
		if (value instanceof List<?>) {
			List<?> items = (List<?>) value;
			for (Object object : items) {
				resultList.add(calculateString(object));
			}
		}
		return StringUtil.listToString(resultList, SEPERATOR_NEWLINE);
	}

	/**
	 * Compute the string representation for a tag list
	 * 
	 * @param value
	 * @return
	 */
	private String calculateTagListAsString(Object value) {
		if (value == null) {
			return CONSTANT_NO_VALUE;
		}
		List<String> resultList = new ArrayList<String>();
		if (value instanceof List<?>) {
			List<?> items = (List<?>) value;
			for (Object object : items) {
				resultList.add(calculateString(object));
			}
		}
		return StringUtil.listToString(resultList, SEPERATOR_COMMA);
	}

	/**
	 * Compute the string representation for an IItem list
	 * 
	 * @param value
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateItemListAsString(Object value)
			throws TeamRepositoryException {
		if (value == null) {
			return CONSTANT_NO_VALUE;
		}
		List<String> resultList = new ArrayList<String>();
		if (value instanceof List<?>) {
			List<?> items = (List<?>) value;
			for (Object object : items) {
				resultList.add(calculateItemAsString(object));
			}
		}
		return StringUtil.listToString(resultList, SEPERATOR_NEWLINE);
	}

	/**
	 * Compute the string representation for a work item list
	 * 
	 * @param value
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateWorkItemListAsString(Object value)
			throws TeamRepositoryException {
		if (value == null) {
			return CONSTANT_NO_VALUE;
		}
		List<String> resultList = new ArrayList<String>();
		if (value instanceof List<?>) {
			List<?> items = (List<?>) value;
			for (Object object : items) {
				resultList.add(calculateWorkItemAsString(object));
			}
		}
		return StringUtil.listToString(resultList, SEPERATOR_NEWLINE);
	}

	/**
	 * Compute the string representation for a process area list
	 * 
	 * @param value
	 * @param asItem
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateProcessAreaListAsString(Object value, boolean asItem)
			throws TeamRepositoryException {
		if (value == null) {
			return CONSTANT_NO_VALUE;
		}
		List<String> resultList = new ArrayList<String>();
		if (value instanceof List<?>) {
			List<?> items = (List<?>) value;
			for (Object object : items) {
				resultList.add(calculateProcessAreaAsString(object, asItem));
			}
		}
		return StringUtil.listToString(resultList, SEPERATOR_NEWLINE);
	}

	/**
	 * Compute the string representation for a contributor/user list
	 * 
	 * @param value
	 * @param seperator
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String calculateContributorListAsString(Object value,
			String seperator) throws TeamRepositoryException {
		if (value == null) {
			return CONSTANT_NO_VALUE;
		}
		List<String> resultList = new ArrayList<String>();
		if (value instanceof List<?>) {
			List<?> items = (List<?>) value;
			for (Object object : items) {
				resultList.add(calculateContributorAsString(object));
			}
		}
		return StringUtil.listToString(resultList, seperator);
	}

	/**
	 * get the default quote character
	 * 
	 * @return
	 */
	private char getQuoteChar() {
		return IWorkItemCommandLineConstants.DEFAULT_QUOTE_CHAR;
	}

	/**
	 * If we want to override the query result set size limit.
	 * 
	 * @return
	 */
	private boolean isOverrideQueryResultSizeLimit() {
		return true;
	}

	/**
	 * Set the file encoding to be used
	 * 
	 * @param encoding
	 */
	private void setFileEncoding(String encoding) {
		fFileEncoding = encoding;

	}

	/**
	 * Get the file encoding
	 * 
	 * @return
	 */
	private String getFileEncoding() {
		return fFileEncoding;
	}

	/**
	 * To set the delimiter
	 * 
	 * @param delimiter
	 */
	private void setDelimiter(String delimiter) {
		if (delimiter.length() != 1) {
			throw new WorkItemCommandLineException(
					"Can not convert delimiter. Delimiter must have size 1 >"
							+ delimiter + "<");
		}
		fDelimiter = delimiter.charAt(0);
	}

	/**
	 * Get the delimiter character
	 * 
	 * @return
	 */
	private char getDelimiter() {
		return fDelimiter;
	}

	/**
	 * @param hasSwitch
	 */
	private void setIgnoreErrors(boolean hasSwitch) {
		this.fIgnoreErrors = true;
	}

	/**
	 * @return
	 */
	private boolean isIgnoreErrors() {
		return this.fIgnoreErrors;
	}

	/**
	 * @param hasSwitch
	 */
	private void setHeaderAsIDs(boolean hasSwitch) {
		fHeaderAsIDs = hasSwitch;
	}

	/**
	 * @return
	 */
	private boolean getHeaderAsIDs() {
		return fHeaderAsIDs;
	}

	/**
	 * Enable RTC compatible export
	 * 
	 * @param hasSwitch
	 */
	private void setRTCEclipseExport(boolean hasSwitch) {
		fRTCEclipseCompatible = hasSwitch;
	}

	/**
	 * Check if RTC Eclipse compatible export is enabled.
	 * 
	 * @return
	 */
	private boolean isRTCEclipseExport() {
		return fRTCEclipseCompatible;
	}

	/**
	 * Set the SimpleDateTimeFormat pattern
	 * 
	 * @return
	 */
	private String getSimpleDateTimeFormatPattern() {
		return fSimpleDateTimeFormatPattern;
	}

	/**
	 * Get the SimpleDateTimeFormat pattern
	 * 
	 * @param fSimpleDateTimeFormatPattern
	 */
	private void setSimpleDateTimeFormatPattern(String simpleDateTimeFormatPattern) {
		this.fSimpleDateTimeFormatPattern = simpleDateTimeFormatPattern;
	}

	/**
	 * Is Suppress Attribute Exceptions
	 * 
	 * @return
	 */
	private boolean isSuppressAttributeErrors() {
		return fSuppressAttributeErrors;
	}

	/**
	 * Set Suppress Attribute Exceptions
	 * 
	 * @param suppressAttributeErrors
	 */
	private void setSuppressAttributeErrors(boolean suppressAttributeErrors) {
		this.fSuppressAttributeErrors = suppressAttributeErrors;
	}

}
