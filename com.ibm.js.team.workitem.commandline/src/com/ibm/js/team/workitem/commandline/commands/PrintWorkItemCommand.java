/*******************************************************************************
 * Copyright (c) 2015-2019 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.commands;

import java.util.List;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.AbstractTeamRepositoryCommand;
import com.ibm.js.team.workitem.commandline.framework.ParameterValue;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.helper.ColumnHeaderMappingHelper;
import com.ibm.js.team.workitem.commandline.helper.WorkItemExportHelper;
import com.ibm.js.team.workitem.commandline.parameter.ColumnHeaderAttributeNameMapper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.js.team.workitem.commandline.utils.ProcessAreaUtil;
import com.ibm.js.team.workitem.commandline.utils.WorkItemUtil;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;

/**
 * Command to export a set of work items to a CSV file set the provided values
 * and save it.
 * 
 * The command supports an RTC compatible mode as well as a special mode that
 * exports IItem values in a way that is uniquely identifying the item and
 * allows to reconstruct the item in the import.
 * 
 */
public class PrintWorkItemCommand extends AbstractTeamRepositoryCommand {

	// Switch to export like RTC would do it
	public static final String SWITCH_RTC_ECLIPSE_EXPORT = "asrtceclipse";
	// The header column uses ID's instead of display names
	public static final String SWITCH_PRINT_ATTRIBUTE_ID = "attributeNamesAsIDs";
	// Try to determine all the supported attributes
	public static final String SWITCH_ALL_COLUMNS = "allColumns";
	// NewLine separator for lists in RTC compatible format
	public static final String SEPERATOR_NEWLINE = "\n";
	// The default separator for lists such as tags
	public static final String SEPERATOR_COMMA = ", ";
	// If there is no value export this
	public static final String CONSTANT_NO_VALUE = "";

	// Parameter for the export file name
	private static final String PARAMETER_ATTACHMENT_FOLDER = "attachmentFolder";
	private static final String PARAMETER_ATTACHMENT_FOLDER_EXAMPLE = "\"C:\\temp\\export\"";

	// Parameter to specify the query
	private static final String PARAMETER_QUERY_NAME = "query";
	private static final String PARAMETER_QUERY_NAME_EXAMPLE = "\"All WorkItems\"";

	// parameter to specify a sharing target for the query
	// The sharing target can be the project area or a
	// team area where the query is shared
	private static final String PARAMETER_SHAR_ING_TARGETS = "querysource";
	private static final String PARAMETER_SHARING_TARGETS_EXAMPLE = "\"JKE Banking(Change Management),JKE Banking(Change Management)/Business Recovery Matters\"";
	// parameter to specify which columns are supposed to be exported
	private static final String PARAMETER_EXPORT_COLUMNS = "columns";
	private static final String PARAMETER_EXPORT_COLUMNS_EXAMPLE1 = "\"Type,Id,Planned For,Filed Against,Description,Found In\"";

	private boolean fAttributeNamesAsIDs = false;
	// Ignore minor errors?
	private boolean fIgnoreErrors = false;
	// Suppress Attribute Not found Exception
	private boolean fSuppressAttributeErrors = false;
	private WorkItemExportHelper fWorkItemExportHelper;

	/**
	 * The constructor
	 * 
	 * @param parametermanager
	 */
	public PrintWorkItemCommand(ParameterManager parametermanager) {
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
		return IWorkItemCommandLineConstants.COMMAND_PRINT_WORKITEM;
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
		getParameterManager().syntaxAddRequiredParameter(IWorkItemCommandLineConstants.PARAMETER_WORKITEM_ID_PROPERTY,
				IWorkItemCommandLineConstants.PROPERTY_WORKITEM_ID_PROPERTY_EXAMPLE);
//		getParameterManager().syntaxAddRequiredParameter(
//				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY,
//				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY_EXAMPLE);
//		getParameterManager().syntaxAddRequiredParameter(PARAMETER_EXPORT_FILE, PARAMETER_EXPORT_FILE_EXAMPLE);
//		getParameterManager().syntaxAddRequiredParameter(PARAMETER_QUERY_NAME, PARAMETER_QUERY_NAME_EXAMPLE);
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.SWITCH_IGNOREERRORS);
		getParameterManager().syntaxAddSwitch(SWITCH_PRINT_ATTRIBUTE_ID);
		getParameterManager().syntaxAddSwitch(SWITCH_ALL_COLUMNS);
		getParameterManager().syntaxAddSwitch(SWITCH_RTC_ECLIPSE_EXPORT);

//		getParameterManager().syntaxAddSwitch(SWITCH_DISABLE_ATTACHMENT_EXPORT);
		getParameterManager()
				.syntaxAddSwitch(IWorkItemCommandLineConstants.SWITCH_EXPORT_SUPPRESS_ATTRIBUTE_EXCEPTIONS);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractCommand#
	 * helpSpecificUsage()
	 */
	@Override
	public String helpSpecificUsage() {
		return "[" + PARAMETER_EXPORT_COLUMNS + IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR
				+ PARAMETER_EXPORT_COLUMNS_EXAMPLE1 + "]" + " ["
				+ IWorkItemCommandLineConstants.PARAMETER_TIMESTAMP_ENCODING_EXAMPLE + "]";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractCommand#process()
	 */
	@Override
	public OperationResult process() throws TeamRepositoryException {
		setIgnoreErrors(getParameterManager().hasSwitch(IWorkItemCommandLineConstants.SWITCH_IGNOREERRORS));
		if (getParameterManager().hasSwitch(SWITCH_RTC_ECLIPSE_EXPORT)) {
			getWorkItemExportHelper().setRTCEclipseExport();
		}
		setAttributeNamesAsIDs(getParameterManager().hasSwitch(SWITCH_PRINT_ATTRIBUTE_ID));
		setSuppressAttributeErrors(getParameterManager()
				.hasSwitch(IWorkItemCommandLineConstants.SWITCH_EXPORT_SUPPRESS_ATTRIBUTE_EXCEPTIONS));

		String filePath = getParameterManager().consumeParameter(PARAMETER_ATTACHMENT_FOLDER);
		if (filePath == null) {
			getWorkItemExportHelper().disableSaveAttachments();
		} else {
			getWorkItemExportHelper().enableSaveAttachments(filePath);
		}

		// Read if there is a special date time format pattern provided
		String dateTimeFormatPattern = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_TIMESTAMP_ENCODING);
		if (dateTimeFormatPattern != null) {
			getWorkItemExportHelper().setSimpleDateTimeFormatPattern(dateTimeFormatPattern.trim());
		}
		// Get the parameters such as the work item ID and run the operation
		String wiID = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_WORKITEM_ID_PROPERTY);
		IWorkItem workItem = WorkItemUtil.findWorkItemByID(wiID, IWorkItem.SMALL_PROFILE, getWorkItemCommon(),
				getMonitor());
		if (workItem == null) {
			throw new WorkItemCommandLineException("Work item cannot be found ID: " + wiID);
		}
		getResult().appendResultString("Exporting work item " + workItem.getId());
		IProjectArea projectArea = ProcessAreaUtil.resolveProjectArea(workItem.getProjectArea(), getMonitor());

		ColumnHeaderMappingHelper columnHeaderMapping = new ColumnHeaderMappingHelper(projectArea, getWorkItemCommon(),
				getMonitor());

		// get the columns to export
		if (getParameterManager().hasSwitch(SWITCH_ALL_COLUMNS)) {
			ColumnHeaderAttributeNameMapper columnMapper = new ColumnHeaderAttributeNameMapper(projectArea,
					getWorkItemCommon(), getMonitor());
			String[] allColumns = columnMapper.getAllColumnsPreSorted();
			columnHeaderMapping.setColumns(allColumns);
		} else {
			String columns = getParameterManager().consumeParameter(PARAMETER_EXPORT_COLUMNS);
			if (columns != null) {
				columnHeaderMapping.setColumns(columns);
			}
		}

		getStringRepresentation(workItem, columnHeaderMapping);
		setSuccess();
		return getResult();
	}

	/**
	 * @param workItem
	 * @param columnHeaderMapping
	 * @throws WorkItemCommandLineException
	 * @throws TeamRepositoryException
	 */
	private void getStringRepresentation(IWorkItem workItem, ColumnHeaderMappingHelper columnHeaderMapping)
			throws WorkItemCommandLineException, TeamRepositoryException {
		List<String> headerNames = columnHeaderMapping.analyzeColumnHeader(getAttributeNamesAsIDs());
		List<ParameterValue> columns = columnHeaderMapping.getParameters();
		getResult().appendResultString("Printing work item " + workItem.getId());
		for (int i = 0; i < columns.size(); i++) {
			ParameterValue column = columns.get(i);
			String value = "";
			try {
				value = getStringRepresentation(workItem, column);
				getResult().appendResultString(headerNames.get(i) + " : " + value);
			} catch (WorkItemCommandLineException e) {
				String message = "Exception exporting work item " + workItem.getId() + " attribute "
						+ column.getAttributeID() + " : " + e.getMessage();
				if (isIgnoreErrors()) {
					if (!isSuppressAttributeErrors()) {
						this.getResult().appendResultString(message + " Ignored!");
					}
				} else {
					throw new WorkItemCommandLineException(message, e);
				}
			}
		}
	}

	/**
	 * This method tries to get the matching representation of the value to be set
	 * for a work item attribute. It basically goes through a list of properties an
	 * attribute can have and locates the target type. Based on that type it tries
	 * to create a matching value. The value is returned if it was possible to
	 * create it.
	 * 
	 * @param column - the IAttribute to find the representation for
	 * @param value  - the string value that is to be transformed.
	 * @param value2
	 * @return
	 * @throws TeamRepositoryException
	 * @throws WorkItemCommandLineException
	 */
	private String getStringRepresentation(IWorkItem workItem, ParameterValue column)
			throws TeamRepositoryException, WorkItemCommandLineException {
		if (column == null) {
			return CONSTANT_NO_VALUE;
		}

		String attributeID = column.getAttributeID();
		if (attributeID == null) {
			throw new WorkItemCommandLineException("AttributeID can not be null");
		}
		IAttribute attribute = column.getIAttribute();
		return getWorkItemExportHelper().getStringrepresentation(workItem, attributeID, attribute);
	}

	/**
	 * Get the getWorkItemExportHelper create it if it does not yet exist.
	 * 
	 * @return
	 */
	private WorkItemExportHelper getWorkItemExportHelper() {
		if (fWorkItemExportHelper == null) {
			fWorkItemExportHelper = new WorkItemExportHelper(getTeamRepository(), getMonitor());
		}
		return fWorkItemExportHelper;
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
	private void setAttributeNamesAsIDs(boolean hasSwitch) {
		fAttributeNamesAsIDs = hasSwitch;
	}

	/**
	 * @return
	 */
	private boolean getAttributeNamesAsIDs() {
		return fAttributeNamesAsIDs;
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
