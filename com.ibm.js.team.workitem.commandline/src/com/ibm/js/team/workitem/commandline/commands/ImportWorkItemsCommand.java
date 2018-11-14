/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.AbstractWorkItemModificationCommand;
import com.ibm.js.team.workitem.commandline.framework.ParameterValue;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.helper.WorkItemTypeHelper;
import com.ibm.js.team.workitem.commandline.helper.WorkItemUpdateHelper;
import com.ibm.js.team.workitem.commandline.parameter.ColumnHeaderAttributeNameMapper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterIDMapper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterList;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.js.team.workitem.commandline.utils.ProcessAreaUtil;
import com.ibm.js.team.workitem.commandline.utils.ReferenceUtil;
import com.ibm.js.team.workitem.commandline.utils.SimpleDateFormatUtil;
import com.ibm.js.team.workitem.commandline.utils.StringUtil;
import com.ibm.js.team.workitem.commandline.utils.WorkItemUtil;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.advice.TeamOperationCanceledException;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.internal.importer.bugzilla.mappers.BugzillaMapping;
import com.ibm.team.workitem.common.internal.importer.bugzilla.mappers.BugzillaMapping.AttributeMapping;
import com.ibm.team.workitem.common.internal.importer.bugzilla.mappers.BugzillaMapping.ValueMapping;
import com.ibm.team.workitem.common.model.AttributeTypes;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.opencsv.CSVReader;

/**
 * Command to import a set of work items from a CSV file set the provided values
 * and save them. The save operation is governed by the process and might fail
 * if required parameters are missing.
 * 
 * This command uses opencsv-2.3jar @see http://opencsv.sourceforge.net/ as
 * external library to read the CSV file.
 * 
 * This command uses the com.ibm.team.workitem.rcp.core.jar file from the RTC
 * Eclipse client to be able to reuse the Bugzilla and CSV import mapping
 * capabilities.
 * 
 * The API from com.ibm.team.workitem.rcp.core.jar is internal API.
 * 
 */
@SuppressWarnings("restriction")
public class ImportWorkItemsCommand extends AbstractWorkItemModificationCommand {

	private static final String HTML_PATH_SLASH = "/";

	// Pass number for import only
	private static final int MULTI_PASS_IMPORT = 1;
	// Pass number for linking
	private static final int MULTI_PASS_LINKMAPPING = 2;

	// The clumn header for the column providing the original work item id's.
	public static final String ORIGINAL_WORK_ITEM_ID = "com.ibm.js.oldid";

	private static final String SEPERATOR_LINK_TARGETS = "|";

	public static final int ATTRIBUTE_SUMMARY_MAXLENGTH = 1000;

	// Multi pass import to recreate work item links
	public static final String SWITCH_MULTI_PASS_IMPORT = "importmultipass";
	// If no target work item was found in the map, use the original ID from the
	// import file
	public static final String SWITCH_FORCE_LINK_CREATION = "forcelinkcreation";

	// To find the state attribute - needed to force the state
	private static final String ATTRIBUTE_STATE = "com.ibm.team.workitem.attribute.state";

	// file with the data for import
	private static final String PARAMETER_IMPORT_FILE = "importFile";
	private static final String PARAMETER_IMPORT_FILE_EXAMPLE = "\"C:\\temp\\export.csv\"";

	// the mapping file
	private static final String PARAMETER_CUSTOM_MAPPING_FILE = "mappingFile";
	private static final String PARAMETER_CUSTOM_MAPPING_FILE_EXAMPLE = "\"C:\\temp\\mapping.xml\"";

	// To determine if we are in debug mode
	private boolean fDebug;

	// To store a comment that could be created from comments and approvals
	private String fComment = "";

	/**
	 * The string attributes have a size limit. This flag forces cutting the
	 * input data down to the limit and truncates the value if necessary
	 */
	private boolean fEnforceSizeLimits = false;
	/**
	 * The bugzilla mapping file, if available
	 */
	private BugzillaMapping fCustomMapping;
	/**
	 * A counter to generate unique ID's for each work item to allow adding
	 * parameters for multiple attachments
	 */
	private Integer fUniqueID;
	/**
	 * The input File
	 */
	private File fInputFile = null;
	
	// The pattern to export time stamps
	private String fSimpleDateTimeFormatPattern=IWorkItemCommandLineConstants.TIMESTAMP_EXPORT_IMPORT_FORMAT_MMM_D_YYYY_HH_MM_A;
	
	
	/**
	 * A hashMap, to map the original work item ID to a new work item ID in
	 * order to be able to map work item links
	 */
	private HashMap<String, String> workItemIDMap = new HashMap<String, String>();

	/**
	 * A counter to count the passes in multi pass import to allow work item
	 * link mapping
	 */
	private int passCount = 0;

	// The delimiter to be used
	private char fDelimiter = IWorkItemCommandLineConstants.DEFAULT_DELIMITER;

	// The default encoding
	private String fEncoding = IWorkItemCommandLineConstants.DEFAULT_ENCODING_UTF_16LE;

	// Multi pass mode
	private boolean fMultipass = false;
	// Use original ID if no mapping was found
	private boolean fForceLinkCreation = false;

	/**
	 * The constructor
	 * 
	 * @param parametermanager
	 */
	public ImportWorkItemsCommand(ParameterManager parametermanager) {
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
		return IWorkItemCommandLineConstants.COMMAND_IMPORT_WORKITEMS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.
	 * AbstractWorkItemCommandLineCommand#setRequiredParameters()
	 */
	public void setRequiredParameters() {
		super.setRequiredParameters();
		// Add the parameters required to perform the operation
		getParameterManager()
				.syntaxAddRequiredParameter(
						IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY,
						IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddRequiredParameter(PARAMETER_IMPORT_FILE,
				PARAMETER_IMPORT_FILE_EXAMPLE);
		getParameterManager().syntaxAddSwitch(
				IWorkItemCommandLineConstants.SWITCH_IGNOREERRORS);
		getParameterManager().syntaxAddSwitch(
				IWorkItemCommandLineConstants.SWITCH_IMPORT_DEBUG);
		getParameterManager().syntaxAddSwitch(
				IWorkItemCommandLineConstants.SWITCH_ENFORCE_SIZE_LIMITS);
		getParameterManager().syntaxAddSwitch(SWITCH_MULTI_PASS_IMPORT);
		getParameterManager().syntaxAddSwitch(SWITCH_FORCE_LINK_CREATION);
        getParameterManager().syntaxAddSwitch(
                IWorkItemCommandLineConstants.SWITCH_SUPPRESS_MAIL_NOTIFICATION);
	}

	/**
	 * A way to add help to a command. This allows for specific parameters e.g.
	 * not required ones
	 * 
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractCommand#helpSpecificUsage()
	 */
	@Override
	public String helpSpecificUsage() {
		return " [" + PARAMETER_CUSTOM_MAPPING_FILE
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR
				+ PARAMETER_CUSTOM_MAPPING_FILE_EXAMPLE + "] " 
				+ "[" + IWorkItemCommandLineConstants.PARAMETER_ENCODING
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR
				+ IWorkItemCommandLineConstants.PARAMETER_ENCODING_EXAMPLE + "]" 
				+ "[" + IWorkItemCommandLineConstants.PARAMETER_TIMESTAMP_ENCODING
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR
				+ IWorkItemCommandLineConstants.PARAMETER_TIMESTAMP_ENCODING_EXAMPLE + "]" 
				+ " [" + IWorkItemCommandLineConstants.PARAMETER_DELIMITER
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR
				+ IWorkItemCommandLineConstants.PARAMETER_DELIMITER_EXAMPLE	+ "]";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.
	 * AbstractWorkItemCommandLineCommand#process()
	 */
	@Override
	public OperationResult process() throws TeamRepositoryException {
		setMultiPass(getParameterManager().hasSwitch(SWITCH_MULTI_PASS_IMPORT));
		this.setForceLinkCreation(getParameterManager().hasSwitch(
				SWITCH_FORCE_LINK_CREATION));
		this.setEnforceSizeJimits(getParameterManager().hasSwitch(
				IWorkItemCommandLineConstants.SWITCH_ENFORCE_SIZE_LIMITS));
		this.setIgnoreErrors(getParameterManager().hasSwitch(
				IWorkItemCommandLineConstants.SWITCH_IGNOREERRORS));
		this.setImportDebug(getParameterManager().hasSwitch(
				IWorkItemCommandLineConstants.SWITCH_IMPORT_DEBUG));
		this.setSuppressMailNotification(getParameterManager().hasSwitch(
				IWorkItemCommandLineConstants.SWITCH_SUPPRESS_MAIL_NOTIFICATION));

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

		// Can not "trim() to allow whitespace characters"
		String delimiter = getParameterManager().consumeParameter(
				IWorkItemCommandLineConstants.PARAMETER_DELIMITER);
		if (delimiter != null) {
			setDelimiter(delimiter);
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
		
		// Get the required input file to import the data
		String inputFileName = getParameterManager().consumeParameter(
				PARAMETER_IMPORT_FILE).trim();
		File input = new File(inputFileName);
		if (!(input.exists() && input.canRead())) {
			throw new WorkItemCommandLineException(
					"Can not access import file " + inputFileName);
		}
		setImportFile(input);

		try {
			boolean result = true;
			int passes = 1;
			if (isMultiPass()) {
				passes = 2;
			}
			// Do multiple passes in a single pass scenario the pass number is 0
			// In a multipass scenario the first pass is 1 and the second pass
			// is 2
			for (int i = 0; i < passes; i++) {
				if (isMultiPass()) {
					increasePass();
				}
				// Try to import the data
				getResult().appendResultString("...Pass: " + getPassNumber());
				result &= importItems(projectArea);
			}
			if (result) {
				this.setSuccess();
				return this.getResult();
			}
		} catch (InvocationTargetException e) {
			throw new WorkItemCommandLineException(e);
		}

		this.setFailed();
		return this.getResult();
	}


	/**
	 * Try to create a mapping for the input to RTC. Read the items found in the
	 * input file and try to create (or update) the items.
	 * 
	 * @param projectArea
	 * @return
	 * @throws TeamRepositoryException
	 * @throws InvocationTargetException
	 */
	private boolean importItems(IProjectArea projectArea)
			throws TeamRepositoryException, InvocationTargetException {

		// Is a custom mapping file configured?
		String customMappingFile = getParameterManager().consumeParameter(
				PARAMETER_CUSTOM_MAPPING_FILE);

		boolean result = true;

		fCustomMapping = null;
		/* Read mapping file if provided */
		if (customMappingFile != null && new File(customMappingFile).exists()) {
			try {
				URL url = new File(customMappingFile).toURI().toURL();
				debug("Reading mapping file " + url.toString());
				fCustomMapping = BugzillaMapping.readMapping(url.toString());
				if (fCustomMapping != null) {
					// for debugging encoding issues, show the keyset of the
					// mapping
					Map<String, AttributeMapping> mapping_info = fCustomMapping
							.getAttributeMappingsBySourceId();
					debug("getAttributeMappingsBySourceId "
							+ mapping_info.keySet().toString());
				}
			} catch (MalformedURLException x) {
				throw new InvocationTargetException(x);
			} catch (TeamRepositoryException x) {
				throw new InvocationTargetException(x);
			}
		}

		return performImport(projectArea, result);
	}

	/**
	 * Perform the full import
	 * 
	 * @param projectArea
	 * @param result
	 * @return
	 * @throws TeamRepositoryException
	 * @throws WorkItemCommandLineException
	 */
	private boolean performImport(IProjectArea projectArea, boolean result)
			throws TeamRepositoryException, WorkItemCommandLineException {
		try {
			// Read the input data with encoding and try to iterate through the
			// data.
			// Try to create or update work items based on the data read
			// @see http://opencsv.sourceforge.net/
			CSVReader reader = new CSVReader(new InputStreamReader(
					new FileInputStream(getImportFile()), getFileEncoding()),
					getDelimiter(), getQuoteChar());

			ColumnHeaderAttributeNameMapper attributeNameMapper = new ColumnHeaderAttributeNameMapper(
					projectArea, getWorkItemCommon(), getMonitor());

			debug("Importing File: " + getImportFile().getAbsolutePath());
			List<String[]> myEntries = reader.readAll();

			boolean skiptitle = true;
			String[] header = null;
			int rowID = 0;
			for (String[] row : myEntries) {
				rowID++;
				debug("Reading row " + rowID + " : "
						+ Arrays.asList(row).toString());
				if (skiptitle) {
					// The first row is the header we use to map attributes,
					// store the header
					header = row;
					skiptitle = false;
					continue;
				}
				try {
					// Try to import the work item
					result &= updateCreateWorkItem(projectArea, header, row,
							rowID, attributeNameMapper);
				} catch (WorkItemCommandLineException e) {
					if (isIgnoreErrors()) {
						result = false;
						getResult().appendResultString(e.getMessage());
					} else {
						throw e;
					}
					e.printStackTrace();
				} finally {
					reader.close();
				}
			}
			debug("Imported");
		} catch (FileNotFoundException e) {
			throw new WorkItemCommandLineException("Import File not found: "
					+ getImportFile().getAbsolutePath(), e);
		} catch (IOException e) {
			throw new WorkItemCommandLineException(e);
		}
		return result;
	}

	/**
	 * This creates or updates a work item from the information found in a row
	 * of data. The header is used to find the attributes to map to
	 * 
	 * @param projectArea
	 * @param header
	 * @param row
	 * @param customMapping
	 * @param attributeMapping
	 * @return
	 * @throws TeamRepositoryException
	 */
	private boolean updateCreateWorkItem(IProjectArea projectArea,
			String[] header, String[] row, int rowID,
			ColumnHeaderAttributeNameMapper attributeMapping)
			throws TeamRepositoryException {

		// Get the parameters for this work item from the CSV data and the
		// mapping provided
		ParameterList parameters = processRow(header, row, rowID,
				attributeMapping);
		if (getPassNumber() > 0) {
			if (null == parameters.getParameter(ORIGINAL_WORK_ITEM_ID)) {
				throw new WorkItemCommandLineException(
						"Multi Pass import requires column with work item ID's nanmed "
								+ ORIGINAL_WORK_ITEM_ID);
			}
		}
		parameters.addSwitch(
				IWorkItemCommandLineConstants.SWITCH_BULK_OPERATION, "");
		if (isEnforceSizeJimits()) {
			parameters.addSwitch(
					IWorkItemCommandLineConstants.SWITCH_ENFORCE_SIZE_LIMITS,
					"");
		}
		if (isIgnoreErrors()) {
			parameters.addSwitch(
					IWorkItemCommandLineConstants.SWITCH_IGNOREERRORS, "");
		}
		if (isSuppressMailNotification()) {
			parameters.addSwitch(
					IWorkItemCommandLineConstants.SWITCH_SUPPRESS_MAIL_NOTIFICATION, "");
		}

		// For each work item we create a new parameter manager that is then
		// used
		// in the subsequent call to update or create the work item
		ParameterManager newParameterManager = new ParameterManager(parameters);
		// Set the new parameter manager
		this.setParameterManager(newParameterManager);
		// if the mapping provides the work item ID, we can try to find
		// the work item.
		String wiID = getParameterManager().consumeParameter(
				IWorkItem.ID_PROPERTY);

		// Try to get the original work item ID to be able to do the mapping
		String originalWorkItemID = getParameterManager().consumeParameter(
				ORIGINAL_WORK_ITEM_ID);
		if (getPassNumber() == MULTI_PASS_LINKMAPPING) {
			if (originalWorkItemID == null) {
				for (String val : row) {
					System.out.print(val + ",");
				}
				throw new WorkItemCommandLineException(
						"Original Work Item ID not provided in 2nd import pass in row: "
								+ rowID + "! Row value: \n");
			}
			wiID = getMappedWorkItemID(originalWorkItemID);
		}
		IWorkItem workItem = null;
		if (wiID != null) {
			// Try to locate the work item if we have an ID.
			workItem = WorkItemUtil.findWorkItemByID(wiID,
					IWorkItem.SMALL_PROFILE, getWorkItemCommon(), getMonitor());
			if (workItem == null) {
				this.appendResultString("Work Item " + wiID
						+ " specified but not found. Creating new work item");
			}
		}
		if (workItem != null) {
			// Update the work item we found
			return updateWorkItem(workItem);
		} else {
			// We have to create a new work item
			// Try to find the work item type
			String workItemTypeIDorName = getParameterManager()
					.consumeParameter(IWorkItem.TYPE_PROPERTY);
			// Alternative ID for the type
			if (workItemTypeIDorName == null) {
				// If we have no type we can't create the work item
				throw new WorkItemCommandLineException(
						"Work item type not specified. ");
			}
			// Find the work item type specified
			IWorkItemType workItemType = WorkItemTypeHelper
					.findWorkItemTypeByIDAndDisplayName(workItemTypeIDorName,
							projectArea.getProjectArea(), getWorkItemCommon(),
							getMonitor());
			if (workItemType == null) {
				// If we have no type we can't create the work item
				throw new WorkItemCommandLineException("Work item type "
						+ workItemTypeIDorName + " not found in project area. ");
			}
			// Create the work item
			return createWorkItem(workItemType, originalWorkItemID);
		}
	}

	/**
	 * Create the work item and set the required attribute values.
	 * 
	 * @param workItemType
	 * @param originalWorkItemID
	 * @return
	 * @throws TeamRepositoryException
	 */
	private boolean createWorkItem(IWorkItemType workItemType,
			String originalWorkItemID) throws TeamRepositoryException {

		ModifyWorkItem operation = new ModifyWorkItem("Creating Work Item");
		IWorkItemHandle handle;
		try {
			handle = operation.run(workItemType, getMonitor());
		} catch (TeamOperationCanceledException e) {
			throw new WorkItemCommandLineException("Work item not created. "
					+ e.getMessage(), e);
		} catch (TeamRepositoryException e) {
			throw new WorkItemCommandLineException("Work item not created. "
					+ e.getMessage(), e);
		}
		if (handle == null) {
			throw new WorkItemCommandLineException(
					"Work item not created, cause unknown.");
		} else {
			IWorkItem workItem = WorkItemUtil.resolveWorkItem(handle,
					IWorkItem.SMALL_PROFILE, getWorkItemCommon(), getMonitor());
			String newWorkItemID = new Integer(workItem.getId()).toString();
			this.appendResultString("Created work item " + newWorkItemID + ".");
			mapNewWorkItemID(originalWorkItemID, newWorkItemID);
		}
		return true;
	}

	/**
	 * Run the update work item operation
	 * 
	 * @param workItem
	 * @return
	 * @throws TeamRepositoryException
	 */
	private boolean updateWorkItem(IWorkItem workItem)
			throws TeamRepositoryException {
		ModifyWorkItem operation = new ModifyWorkItem("Updating work Item",
				IWorkItem.FULL_PROFILE);
		try {
			this.appendResultString("Updating work item " + workItem.getId()
					+ ".");

			// Do we have to change the type?
			String workItemTypeIDOrName = getParameterManager()
					.consumeParameter(IWorkItem.TYPE_PROPERTY);
			// There is a type provided
			if (workItemTypeIDOrName != null) {
				IWorkItemType newType = WorkItemTypeHelper
						.findWorkItemTypeByIDAndDisplayName(
								workItemTypeIDOrName,
								workItem.getProjectArea(), getWorkItemCommon(),
								getMonitor());
				// If we can't find the type we can't change it
				if (newType == null) {
					// If we have no type we can't create the work item
					throw new WorkItemCommandLineException("Work item type "
							+ workItemTypeIDOrName
							+ " not found in project area. ");
				}
				// For output purposes
				IWorkItemType oldType = WorkItemTypeHelper.findWorkItemType(
						workItem.getWorkItemType(), workItem.getProjectArea(),
						getWorkItemCommon(), getMonitor());
				ChangeType changeTypeOperation = new ChangeType(
						"Changing work item type", oldType, newType);
				changeTypeOperation.run(workItem, getMonitor());
			}
			operation.run(workItem, getMonitor());
			this.appendResultString("Updated work item " + workItem.getId()
					+ ".");
		} catch (TeamOperationCanceledException e) {
			throw new WorkItemCommandLineException("Work item not updated. "
					+ e.getMessage(), e);
		} catch (TeamRepositoryException e) {
			throw new WorkItemCommandLineException("Work item not updated. "
					+ e.getMessage(), e);
		}
		return true;
	}

	/**
	 * Based on the header, the row and the mapping, construct a list of
	 * parameters that we can use to create or update a work item
	 * 
	 * Mappings with mapping for the attribute value
	 * 
	 * <attribute sourceId="Priority"
	 * targetId="com.ibm.team.workitem.attribute.priority"> <value
	 * sourceId="Medium" targetId="priority.literal.l07"/> <value
	 * sourceId="High" targetId="priority.literal.l11"/> </attribute> <attribute
	 * sourceId="Type" targetId="com.ibm.team.workitem.attribute.workitemtype">
	 * <value sourceId="Story" targetId="com.ibm.team.apt.workItemType.story"/>
	 * <value sourceId="Task" targetId="task"/> </attribute>
	 * 
	 * 
	 * Mappings without mapping for the attribute value
	 * 
	 * <attribute sourceId="Summary"
	 * targetId="com.ibm.team.workitem.attribute.summary"> </attribute>
	 * <attribute sourceId="Filed Against"
	 * targetId="com.ibm.team.workitem.attribute.category"> </attribute>
	 * 
	 * @param header
	 *            - The header row of the CSV file
	 * @param row
	 *            - The current row we are processing
	 * @param customMapping
	 *            - The mapping or null
	 * @param attributeMapping
	 *            - an internal mapping that maps attributeNames to ID's
	 * 
	 * @return a list of parameters
	 */
	private ParameterList processRow(String[] header, String[] row, int rowID,
			ColumnHeaderAttributeNameMapper attributeMapping) {
		boolean rowHasData = false;
		ParameterList parameters = new ParameterList();
		clearComment();
		resetUniqueID();

		// iterate the row data and try to map it to some
		// work item attribute name/ID and attribute value name/id
		for (int i = 0; i < row.length; i++) {
			String inputAttribute = header[i];
			String inputValue = row[i];
			// Target values without mapping
			String targetAttribute = inputAttribute;
			String targetValue = inputValue;

			if (fCustomMapping != null) {
				// Get the maps from the mapping
				Map<String, AttributeMapping> mapping = fCustomMapping
						.getAttributeMappingsBySourceId();
				// Get the mapping for the attribute ID's
				AttributeMapping attrmap = mapping.get(inputAttribute.trim());
				if (attrmap != null) {
					// If there is a mapping get the value for the target
					// attribute
					// This could be a name or an ID
					targetAttribute = attrmap.getTargetId();
					targetAttribute = ParameterIDMapper
							.getAlias(targetAttribute);
					// Get the value mapping if exists
					ValueMapping valuemap = attrmap.getValueMapping(inputValue);
					if (valuemap != null) {
						// Get the mapped target value for the attribute
						targetValue = valuemap.getTargetId();
					}
				} else {
					// There is no mapping for this input attribute to a target
					// attribute
					debug("Missing attributeMap for: " + inputAttribute);
					// If there is a mapping and an attribute does not have an
					// entry in the mapping, skip the attribute.
					continue;
				}
			} else {
				// Use an internal mapping, values are not mapped
				String inputID = inputAttribute.trim();
				String newMapping = attributeMapping.getID(inputID);
				if (newMapping != null) {
					targetAttribute = newMapping;
				}
			}
			if (targetValue != null && !targetValue.equals("")) {
				rowHasData = true;
			}
			// Add the new parameter
			processAttribute(attributeMapping, parameters, targetAttribute,
					targetValue);

		}
		// Add all the information that is not imported as comment data.
		String comment = getComment();
		if (comment.length() > 0) {
			rowHasData = true;
			addParameter(parameters, IWorkItem.COMMENTS_PROPERTY, comment);
		}
		if (!rowHasData) {
			throw new WorkItemCommandLineException(
					"No data found in row or row malformed! Row: " + rowID);
		}
		return parameters;
	}

	private void resetUniqueID() {
		fUniqueID = new Integer(0);
	}

	private Integer getUniqueID() {
		return ++fUniqueID;
	}

	/**
	 * Process an attribute and try to create a parameter with the data needed
	 * to create the work item
	 * 
	 * @param parameters
	 * @param headerMapping
	 * @param parameters
	 * @param targetAttribute
	 * @param targetValue
	 */
	private void processAttribute(
			ColumnHeaderAttributeNameMapper headerMapping,
			ParameterList parameters, String attributeID, String targetValue) {
		// If the parameter has no value, we don't process it.
		if (targetValue.equals("")) {
			return;
		}
		// Try to get the attribute from the header mapping
		IAttribute attribute = headerMapping.getAttribute(attributeID);
		// Handle non attribute based values
		if (attribute == null) {
			// Check for the mapping to the original work item
			if (attributeID.trim().toLowerCase().matches(ORIGINAL_WORK_ITEM_ID)) {
				addParameter(parameters, ORIGINAL_WORK_ITEM_ID, targetValue);
				return;
			}
			// check for a link
			if (headerMapping.isLinkType(attributeID)) {
				if (getPassNumber() == MULTI_PASS_IMPORT) {
					// Ignore links in the first pass
					return;
				}
				// Handle links in the second pass or if not in multi pass mode
				addLinkParameter(parameters, attributeID, targetValue);
				return;
			}
			// Don't do anything else than adding links in pass 2.
			if (getPassNumber() > 1) {
				return;
			}
			if (attributeID
					.trim()
					.toLowerCase()
					.equals(ParameterIDMapper.PSEUDO_ATTRIBUTE_ATTACHMENTS
							.toLowerCase())) {
				addAttachmentParameters(parameters, attributeID, targetValue);
				return;
			}
			getResult().appendResultString(
					"No matching attribute found: " + attributeID);
			// addParameter(parameters, attributeID, targetValue);
			return;
		}
		// Ignore everything else in the second pass
		if (getPassNumber() == MULTI_PASS_LINKMAPPING) {
			return;
		}

		String attribType = attribute.getAttributeType();

		// If the mapping is for the state attribute, we want to force the state
		// as work items might not have an action to the state.
		if (attribType.equals(ATTRIBUTE_STATE)
				|| attribute.getIdentifier().equals(IWorkItem.STATE_PROPERTY)) {
			targetValue = WorkItemUpdateHelper.STATECHANGE_FORCESTATE
					+ WorkItemUpdateHelper.FORCESTATE_SEPARATOR + targetValue;
		}
		// Ignore attributes that can not be set, but are enforced by other
		// attributes
		if (attribute.getIdentifier().equals(IWorkItem.PROJECT_AREA_PROPERTY)) {
			// Ignore
			getResult().appendResultString(
					"Ignored: Attribute is calculated and can not be set: "
							+ attributeID + " mapped to: "
							+ attribute.getIdentifier());
			return;
		}

		if (attribType.equals(AttributeTypes.APPROVALS)) {
			// handle Approvals
			// For now we basically create a comment with the content.
			addComment("Approval data from import", targetValue);
			return;
		}
		if (attribType.equals(AttributeTypes.COMMENTS)) {
			// Handle comments
			// For now we basically create a comment with the content.
			addComment("Comment data from import", targetValue);
			return;
		}
		if (attribType.equals(AttributeTypes.SUBSCRIPTIONS)) {
			// Handle Subscriptions
			String contributorList = convertItemList(targetValue);
			addParameter(parameters, attributeID, contributorList);
			return;
		}

		// Handle list attribute types first
		if (AttributeTypes.isListAttributeType(attribType)) {
			if (AttributeTypes.isItemListAttributeType(attribType)) {
				// Item List Types that are supported
				if (attribType.equals(AttributeTypes.CONTRIBUTOR_LIST)) {
					// A list of contributors
					String contributorList = convertItemList(targetValue);
					addParameter(parameters, attributeID, contributorList);
					return;
				}
				if (attribType.equals(AttributeTypes.PROCESS_AREA_LIST)) {
					// A list of process areas (ProjectArea/TeamArea)
					String contributorList = convertItemList(targetValue);
					addParameter(parameters, attributeID, contributorList);
					return;
				}
				if (attribType.equals(AttributeTypes.PROJECT_AREA_LIST)) {
					// A list of process areas (ProjectAreas)
					String contributorList = convertItemList(targetValue);
					addParameter(parameters, attributeID, contributorList);
					return;
				}
				if (attribType.equals(AttributeTypes.TEAM_AREA_LIST)) {
					// A list of process areas (TeamAreas)
					String contributorList = convertItemList(targetValue);
					addParameter(parameters, attributeID, contributorList);
					return;
				}
				if (attribType.equals(AttributeTypes.WORK_ITEM_LIST)) {
					// A list of work items
					String contributorList = convertItemList(targetValue);
					addParameter(parameters, attributeID, contributorList);
					return;
				}
				if (attribType.equals(AttributeTypes.ITEM_LIST)) {
					String contributorList = convertItemList(targetValue);
					addParameter(parameters, attributeID, contributorList);
					return;
				}
			}
			if (attribType.equals(AttributeTypes.TAGS)) {
				// Handle Tags - also detected as list type
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (attribType.equals(AttributeTypes.STRING_LIST)) {
				// A list of strings
				String contributorList = convertItemList(targetValue);
				addParameter(parameters, attributeID, contributorList);
				return;
			}
			if (AttributeTypes.isEnumerationListAttributeType(attribType)) {
				// Handle all Enumeration List Types
				String contributorList = convertItemList(targetValue);
				addParameter(parameters, attributeID, contributorList);
				return;
			}
			throw new WorkItemCommandLineException(
					"Type not recognized - type not yet supported: "
							+ attribType + " ID " + attribute.getIdentifier());
		} else {
			// Handle non list types - the simple ones first.

			if (attribType.equals(AttributeTypes.WIKI)) {
				// return calculateString(value);
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (AttributeTypes.STRING_TYPES.contains(attribType)) {
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (AttributeTypes.HTML_TYPES.contains(attribType)) {
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (attribType.equals(AttributeTypes.BOOLEAN)) {
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (AttributeTypes.NUMBER_TYPES.contains(attribType)) {
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (attribType.equals(AttributeTypes.DELIVERABLE)) {
				// Handle deliverables - Found In and other attributes
				// referencing a release.
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (attribType.equals(AttributeTypes.CATEGORY)) {
				// Work item category - Filed Against and other attributes
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (attribType.equals(AttributeTypes.ITERATION)) {
				// Iterations - Planned For and other such attributes
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (attribType.equals(AttributeTypes.CONTRIBUTOR)) {
				// Contributors - user ID's
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (attribType.equals(AttributeTypes.TIMESTAMP)) {
				// Timestamp types e.g. dates
				String timestamp = convertTimestamp(targetValue);
				if (timestamp != null) {
					addParameter(parameters, attributeID, timestamp);
				}
				return;
			}
			if (attribType.equals(AttributeTypes.PROJECT_AREA)) {
				// ProjectArea type attributes
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (attribType.equals(AttributeTypes.TEAM_AREA)) {
				// TeamArea type attributes
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (attribType.equals(AttributeTypes.PROCESS_AREA)) {
				// Process Area type attributes (TeamArea/ProjectArea)
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (attribType.equals(AttributeTypes.WORK_ITEM)) {
				// Work Item attributes
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (attribType.equals(AttributeTypes.ITEM)) {
				// Handle items where the type is not specified in the attribute
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (attribType.equals(AttributeTypes.TYPE)) {
				// The work item type
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (AttributeTypes.isEnumerationAttributeType(attribType)) {
				// Handle all enumeration types
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			if (attribType.equals(AttributeTypes.UUID)) {
				// The work item restricted Access UUID
				addParameter(parameters, attributeID, targetValue);
				return;
			}
			// In case we forgot something or a new type gets implemented
			throw new WorkItemCommandLineException(
					"AttributeType not yet supported: " + attribType + " ID "
							+ attribute.getIdentifier());
		}
	}

	/**
	 * Converts a list of attachments into single parameters for each attachment
	 * 
	 * @param parameters
	 * @param attributeID
	 * @param targetValue
	 */
	private void addAttachmentParameters(ParameterList parameters,
			String attributeID, String targetValue) {
		if (targetValue.equals("")) {
			return;
		}
		List<String> attachments = StringUtil.splitStringToList(targetValue,
				ExportWorkItemsCommand.SEPERATOR_NEWLINE);
		String rootfolder = getImportFile().getParent();
		for (String attachment : attachments) {
			addAttachmenParameter(parameters, attachment, rootfolder);
		}
	}

	/**
	 * Create a new parameter for an attachment
	 * 
	 * @param parameters
	 * @param attachment
	 * @param rootfolder
	 */
	private void addAttachmenParameter(ParameterList parameters,
			String attachment, String rootfolder) {
		List<String> attachmentData = StringUtil.splitStringToList(attachment,
				WorkItemUpdateHelper.ATTACHMENT_SEPARATOR);
		String fileName = "";
		String description = "";
		String contentType = "application/unknown";
		String encoding = "";

		if (attachmentData.size() > 0) {
			fileName = attachmentData.get(0);
			File newFile = new File(fileName);
			// If the file has an absolute path, don't add the root folder
			if (newFile.isAbsolute()) {
				rootfolder = "";
			}
		}
		if (attachmentData.size() > 1) {
			description = attachmentData.get(1);
		}
		if (attachmentData.size() > 2) {
			contentType = attachmentData.get(2);
		}
		if (attachmentData.size() > 3) {
			encoding = attachmentData.get(3);
		}

		// TODO: Detect absolute path.
		String attachmentvalue = rootfolder + File.separator + fileName
				+ WorkItemUpdateHelper.ATTACHMENT_SEPARATOR + description
				+ WorkItemUpdateHelper.ATTACHMENT_SEPARATOR + contentType
				+ WorkItemUpdateHelper.ATTACHMENT_SEPARATOR + encoding;

		parameters.addParameterValue(
				WorkItemUpdateHelper.PSEUDO_ATTRIBUTE_ATTACHFILE + "_"
						+ getUniqueID().toString(), attachmentvalue.trim());
	}

	/**
	 * Change format from import to internal format to create links.
	 * 
	 * @param parameters
	 * @param linkID
	 * @param targetValue
	 */
	private void addLinkParameter(ParameterList parameters, String linkID,
			String targetValue) {
		if (null == targetValue || targetValue.equals("")) {
			// Nothing to do
			return;
		}
		// In a multi pass scenario we want to delay link creation to pass 2
		if (getPassNumber() == MULTI_PASS_IMPORT) {
			return;
		}
		// Links are separated by newline - compute it to match the workItem
		// command line format.
		String linkName = WorkItemUpdateHelper.PSEUDO_ATTRIBUTE_LINK + linkID;
		List<String> values = StringUtil.splitStringToList(targetValue,
				ExportWorkItemsCommand.SEPERATOR_NEWLINE);
		List<String> resultValues = new ArrayList<String>(values.size());
		// Special handling for work item links to be able to map the link to
		// imported items, other links are just passed through
		if (ReferenceUtil.isWorkItemLink(linkID)) {
			// Check for URL's and numbers
			// map the work item ID's
			for (String idval : values) {
				String workItemID = getWorkItemLink(idval);
				if (workItemID == null) {
					// If the ID does no make sense, skip this.
					continue;
				}
				resultValues.add(workItemID);
			}
			// Replace the old value with the calculated new ones
			values = resultValues;
		}
		values = StringUtil.removePrefixes(values,
				ExportWorkItemsCommand.PREFIX_EXISTINGWORKITEM);
		String importValue = StringUtil.listToString(values,
				SEPERATOR_LINK_TARGETS);
		addParameter(parameters, linkName, importValue);
	}

	/**
	 * Try to get a work item from a link In a multi pass scenario, try to find
	 * the work item that was created from the import.
	 * 
	 * @param idval
	 * @return
	 */
	private String getWorkItemLink(String idval) {
		boolean foundURIbasedWI = false;

		String workItemID = idval;
		if (StringUtil.hasPrefix(idval,
				ExportWorkItemsCommand.PREFIX_EXISTINGWORKITEM)) {
			workItemID = StringUtil.removePrefix(idval,
					ExportWorkItemsCommand.PREFIX_EXISTINGWORKITEM);
		}
		String urlprefix = null;
		String tempWorkItemID = null;
		if (workItemID.startsWith(WorkItemUpdateHelper.HTTP_PROTOCOL_PREFIX)) {
			try {
				// Try to get the work item id from the URL and keep the prefix
				int length = workItemID.length();
				int lastindex = workItemID.lastIndexOf(HTML_PATH_SLASH);
				urlprefix = workItemID.substring(0, lastindex + 1);
				tempWorkItemID = workItemID.substring(lastindex + 2, length);
				foundURIbasedWI = true;
			} catch (IndexOutOfBoundsException e) {
				// Nothing to do
			}
		}
		if (getPassNumber() == MULTI_PASS_LINKMAPPING) {
			// Try the mapping of the old work item id to the new work item id
			if (foundURIbasedWI) {
				workItemID = tempWorkItemID;
			}
			String newID = getMappedWorkItemID(workItemID);
			if (workItemID.equals(newID)) {
				if (!isForceLinkCreation())// Use the original ID provided in
											// the import
				{
					System.out.println("Could not find new ID for item "
							+ workItemID);
					// Don't create the link
					return null;
				}
			}
			// Create a new work item link target
			if (foundURIbasedWI) {
				workItemID = urlprefix + newID;
			} else {
				workItemID = newID;
			}
		}
		return workItemID;
	}

	/**
	 * Convert the timestamp format from the one used to export to the one used
	 * in the creation of the work item
	 * 
	 * @param date
	 * @return
	 */
	private String convertTimestamp(String date) {
		if (date == null || date.equals("")) {
			return null;
		}
		Timestamp ts = SimpleDateFormatUtil
				.createTimeStamp(
						date,
						getSimpleDateTimeFormatPattern());
		String importdate = SimpleDateFormatUtil
				.getDate(
						ts,
						SimpleDateFormatUtil.SIMPLE_DATE_FORMAT_PATTERN_YYYY_MM_DD_HH_MM_SS_Z);
		return importdate;
	}

	/**
	 * Converts an item list from the export format used by RTC into the one
	 * needed in WCL. Replace line breaks with other separator
	 * 
	 * @param targetValue
	 * @return
	 */
	private String convertItemList(String targetValue) {
		if (targetValue.indexOf(ExportWorkItemsCommand.SEPERATOR_NEWLINE) > 0) {
			List<String> temp = StringUtil.splitStringToList(targetValue,
					ExportWorkItemsCommand.SEPERATOR_NEWLINE);
			return StringUtil.listToString(temp,
					WorkItemUpdateHelper.ITEM_SEPARATOR);
		}
		return targetValue;
	}

	/**
	 * Add a parameter to be used in the work item update
	 * 
	 * @param parameters
	 * @param attributeID
	 * @param targetValue
	 */
	private void addParameter(ParameterList parameters, String attributeID,
			String targetValue) {
		debug("Mapped Attribute: [" + attributeID + "] Mapped Value: ["
				+ targetValue + "]");
		// Currently using default TODO: add, set, default?
		parameters.addParameterValue(attributeID, targetValue);
	}

	/**
	 * Add a parameter to be used in the work item update
	 * 
	 * @param parameters
	 * @param attributeID
	 * @param updateMode
	 * @param targetValue
	 */
	@SuppressWarnings("unused")
	private void addParameterMode(ParameterList parameters, String attributeID, String updateMode,
			String targetValue) {
		String modeValue= "";
		debug("Mapped Attribute: [" + attributeID + "] Mapped Value: ["
				+ targetValue + "]");
		// Currently using default TODO: add, set, default?
		if(updateMode!=null){
			if (ParameterValue.MODE_ADD.equals(updateMode) || ParameterValue.MODE_REMOVE.equals(updateMode)
					|| ParameterValue.MODE_SET.equals(updateMode)) {
				modeValue = ParameterValue.POSTFIX_PARAMETER_MANIPULATION_MODE + updateMode;
			}	
		} 
		parameters.addParameterValue(attributeID+modeValue, targetValue);
	}
	
	/**
	 * Get the ID for a mapped work item
	 * 
	 * @param id
	 * @return
	 */
	private String getMappedWorkItemID(String id) {
		String newID = workItemIDMap.get(id);
		if (newID != null) {
			return newID;
		}
		return id;
	}

	/**
	 * Create a mapping between the original ID and the new ID, in order to be
	 * able to modify the link ID's for work item links.
	 * 
	 * @param originalWorkItemID
	 * @param newWorkItemID
	 */
	private void mapNewWorkItemID(String originalWorkItemID,
			String newWorkItemID) {
		if (originalWorkItemID != null) {
			workItemIDMap.put(originalWorkItemID, newWorkItemID);
		}
	}

	/**
	 * Set the import file
	 * 
	 * @param importFile
	 */
	private void setImportFile(java.io.File importFile) {
		fInputFile = importFile;
	}

	/**
	 * @return the import file
	 */
	private File getImportFile() {
		if (fInputFile == null) {
			throw new WorkItemCommandLineException(
					"Import file can not be null");
		}
		return fInputFile;
	}

	/**
	 * From the flag set if we are in debug mode.
	 * 
	 * @param value
	 */
	private void setImportDebug(boolean value) {
		fDebug = value;
	}

	/**
	 * @return true if we are in debug mode
	 */
	private boolean isImportDebug() {
		return fDebug;
	}

	/**
	 * Clear the comment
	 */
	private void clearComment() {
		fComment = "";
	}

	/**
	 * Add something to the comment
	 * 
	 * @param title
	 * @param comment
	 */
	private void addComment(String title, String comment) {
		fComment += "<b>" + title + "</b><br/><br/>" + comment + "<br/><br/>";
	}

	/**
	 * Get the comment
	 * 
	 * @return
	 */
	private String getComment() {
		return fComment;
	}

	/**
	 * Create a debug message
	 * 
	 * @param message
	 */
	private void debug(String message) {
		if (isImportDebug()) {
			this.appendResultString(message);
		}
	}

	/**
	 * Getter for the quote character
	 * 
	 * @return
	 */
	private char getQuoteChar() {
		return IWorkItemCommandLineConstants.DEFAULT_QUOTE_CHAR;
	}

	/**
	 * Getter for the quote character
	 * 
	 * @return
	 */
	private char getDelimiter() {
		return fDelimiter;
	}

	/**
	 * setter for the quote character
	 * 
	 * @return
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
	 * Getter for the encoding
	 * 
	 * @return
	 */
	private String getFileEncoding() {
		return fEncoding;
	}

	/**
	 * Getter for the encoding
	 * 
	 * @return
	 */
	private void setFileEncoding(String encoding) {
		fEncoding = encoding;
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
	private boolean isEnforceSizeJimits() {
		return fEnforceSizeLimits;
	}

	/**
	 * @return
	 */
	private boolean isMultiPass() {
		return fMultipass;
	}

	/**
	 * @param hasSwitch
	 */
	private void setMultiPass(boolean hasSwitch) {
		fMultipass = hasSwitch;
	}

	/**
	 * Increase the pass number
	 */
	private void increasePass() {
		this.passCount++;
	}

	/**
	 * Get the pass number
	 * 
	 * @return
	 */
	private int getPassNumber() {
		return this.passCount;
	}

	/**
	 * Set flag to Use the original import ID if no mapping ID was found
	 * 
	 * @param hasSwitch
	 */
	private void setForceLinkCreation(boolean hasSwitch) {
		fForceLinkCreation = hasSwitch;
	}

	/**
	 * Flag to use the original import ID if no mapping ID was found
	 * 
	 * @return
	 */
	private boolean isForceLinkCreation() {
		return fForceLinkCreation;
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

}
