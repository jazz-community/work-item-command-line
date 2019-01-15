/*******************************************************************************
 * Copyright (c) 2015-2019 IBM Corporation
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.AbstractTeamRepositoryCommand;
import com.ibm.js.team.workitem.commandline.framework.ParameterValue;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.helper.ColumnHeaderMappingHelper;
import com.ibm.js.team.workitem.commandline.helper.WorkItemExportHelper;
import com.ibm.js.team.workitem.commandline.parameter.ColumnHeaderAttributeNameMapper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterIDMapper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterLinkIDMapper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.js.team.workitem.commandline.utils.ProcessAreaUtil;
import com.ibm.js.team.workitem.commandline.utils.QueryUtil;
import com.ibm.js.team.workitem.commandline.utils.WorkItemUtil;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.IAuditableHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.query.IQueryDescriptor;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResult;
import com.ibm.team.workitem.common.query.ResultSize;
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

	// Switch to disable attachment export
	private static final String SWITCH_DISABLE_ATTACHMENT_EXPORT = "disableAttachmentExport";
	// Switch to export like RTC would do it
	public static final String SWITCH_RTC_ECLIPSE_EXPORT = "asrtceclipse";
	// The header column uses ID's instead of display names
	public static final String SWITCH_HEADER_AS_ID = "headerIDs";
	// Try to determine all the supported attributes
	public static final String SWITCH_ALL_COLUMNS = "allColumns";
	// NewLine separator for lists in RTC compatible format
	public static final String SEPERATOR_NEWLINE = "\n";
	// The default separator for lists such as tags
	public static final String SEPERATOR_COMMA = ", ";
	// If there is no value export this
	public static final String CONSTANT_NO_VALUE = "";
	// prefix to be used when exporting work item ID's
	public static final String PREFIX_EXISTINGWORKITEM = "#";

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

	// The encoding to be used when saving the file
	private String fFileEncoding = IWorkItemCommandLineConstants.DEFAULT_ENCODING_UTF_16LE;
	// Delimiter to be used for columns
	private char fDelimiter = IWorkItemCommandLineConstants.DEFAULT_DELIMITER;
	// Export headers as ID's?
	private boolean fHeaderAsIDs = false;
	// Ignore minor errors?
	private boolean fIgnoreErrors = true;
	// Suppress Attribute Not found Exception
	private boolean fSuppressAttributeErrors = false;
	private WorkItemExportHelper fWorkItemExportHelper;

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
		getParameterManager().syntaxAddRequiredParameter(
				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddRequiredParameter(PARAMETER_EXPORT_FILE, PARAMETER_EXPORT_FILE_EXAMPLE);
		getParameterManager().syntaxAddRequiredParameter(PARAMETER_QUERY_NAME, PARAMETER_QUERY_NAME_EXAMPLE);
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.SWITCH_IGNOREERRORS);
		getParameterManager().syntaxAddSwitch(SWITCH_HEADER_AS_ID);
		getParameterManager().syntaxAddSwitch(SWITCH_RTC_ECLIPSE_EXPORT);
		getParameterManager().syntaxAddSwitch(SWITCH_DISABLE_ATTACHMENT_EXPORT);
		getParameterManager().syntaxAddSwitch(SWITCH_ALL_COLUMNS);
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
		return " [" + IWorkItemCommandLineConstants.PARAMETER_ENCODING
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR
				+ IWorkItemCommandLineConstants.PARAMETER_ENCODING_EXAMPLE + "]" + " ["
				+ IWorkItemCommandLineConstants.PARAMETER_DELIMITER
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR
				+ IWorkItemCommandLineConstants.PARAMETER_DELIMITER_EXAMPLE + "]" + " [" + PARAMETER_EXPORT_COLUMNS
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR + PARAMETER_EXPORT_COLUMNS_EXAMPLE1
				+ "]" + " [" + PARAMETER_SHARING_TARGETS + IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR
				+ PARAMETER_SHARING_TARGETS_EXAMPLE + "]" + "["
				+ IWorkItemCommandLineConstants.PARAMETER_TIMESTAMP_ENCODING
				+ IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR
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
		if(getParameterManager().hasSwitch(SWITCH_RTC_ECLIPSE_EXPORT)) {
			getWorkItemExportHelper().setRTCEclipseExport();
		}
		setHeaderAsIDs(getParameterManager().hasSwitch(SWITCH_HEADER_AS_ID));
		setSuppressAttributeErrors(getParameterManager()
				.hasSwitch(IWorkItemCommandLineConstants.SWITCH_EXPORT_SUPPRESS_ATTRIBUTE_EXCEPTIONS));

		String projectAreaName = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY).trim();
		// Find the project area
		IProjectArea projectArea = ProcessAreaUtil.findProjectAreaByFQN(projectAreaName, getProcessClientService(),
				getMonitor());
		if (projectArea == null) {
			throw new WorkItemCommandLineException("Project Area not found: " + projectAreaName);
		}

		// Read if there is a special encoding provided
		String encoding = getParameterManager().consumeParameter(IWorkItemCommandLineConstants.PARAMETER_ENCODING);
		if (encoding != null) {
			setFileEncoding(encoding.trim());
		}

		// Read if there is a special date time format pattern provided
		String dateTimeFormatPattern = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_TIMESTAMP_ENCODING);
		if (dateTimeFormatPattern != null) {
			getWorkItemExportHelper().setSimpleDateTimeFormatPattern(dateTimeFormatPattern.trim());
		}

		// Can not "trim() to allow whitespace characters"
		String delimiter = getParameterManager().consumeParameter(IWorkItemCommandLineConstants.PARAMETER_DELIMITER);
		if (delimiter != null) {
			setDelimiter(delimiter);
		}

		ColumnHeaderMappingHelper columnHeaderMapping = new ColumnHeaderMappingHelper(projectArea, getWorkItemCommon(),
				getMonitor());

		// get the columns to export
		if (getParameterManager().hasSwitch(SWITCH_ALL_COLUMNS)) {
			String[] allColumns = getAllColumnsPreSorted(projectArea);
			columnHeaderMapping.setColumns(allColumns);
		} else {
			String columns = getParameterManager().consumeParameter(PARAMETER_EXPORT_COLUMNS);
			if (columns != null) {
				columnHeaderMapping.setColumns(columns);
			}
		}

		String queryName = getParameterManager().consumeParameter(PARAMETER_QUERY_NAME);
		if (queryName == null) {
			throw new WorkItemCommandLineException("Query name must be provided.");
		}
		String sharingTargetNames = getParameterManager().consumeParameter(PARAMETER_SHARING_TARGETS);
		
		IQueryDescriptor query = getWorlkItemQuery(projectArea, queryName, sharingTargetNames);

		String filePath = getParameterManager().consumeParameter(PARAMETER_EXPORT_FILE);
		if (filePath == null) {
			throw new WorkItemCommandLineException("Export file path must be provided.");
		}

		exportCSV(filePath, query, columnHeaderMapping);
		return getResult();
	}

	/**
	 * Get all the available attributes and supported links.
	 * 
	 * @param projectArea
	 * @return
	 * @throws TeamRepositoryException
	 */
	private String[] getAllColumnsPreSorted(IProjectArea projectArea) throws TeamRepositoryException {

		ColumnHeaderAttributeNameMapper columnMapper = new ColumnHeaderAttributeNameMapper(projectArea, getWorkItemCommon(), getMonitor());
		HashMap<String, IAttribute> attributeMap = columnMapper.getAttributeMap(); 

		ArrayList<String> sortedAttribs = new ArrayList<String>();
		sortedAttribs.add(IWorkItem.ID_PROPERTY);
		sortedAttribs.add(IWorkItem.TYPE_PROPERTY);
		sortedAttribs.add(IWorkItem.SUMMARY_PROPERTY);

		ArrayList<String> allAttribs = new ArrayList<String>();
		attributeMap.remove(IWorkItem.ID_PROPERTY);
		attributeMap.remove(IWorkItem.TYPE_PROPERTY);
		attributeMap.remove(IWorkItem.SUMMARY_PROPERTY);
		allAttribs.addAll(attributeMap.keySet());
		Collections.sort(allAttribs);

		ArrayList<String> sortedLinks = new ArrayList<String>(ParameterLinkIDMapper.getLinkIDs());
		Collections.sort(sortedLinks);

		sortedAttribs.addAll(allAttribs);
		sortedAttribs.add(ParameterIDMapper.PSEUDO_ATTRIBUTE_ATTACHMENTS); // This is not a property add the artificial one
		sortedAttribs.addAll(sortedLinks);
		return sortedAttribs.toArray(new String[sortedAttribs.size()]);
	}
	

	/**
	 * Get a work item query to locate the work items
	 * 
	 * @param projectArea
	 * @param queryName
	 * @param sharingTargetNames
	 * @return
	 * @throws TeamRepositoryException
	 */
	private IQueryDescriptor getWorlkItemQuery(IProjectArea projectArea, String queryName, String sharingTargetNames)
			throws TeamRepositoryException {
		// Get the query
		IQueryDescriptor query = null;
		if (sharingTargetNames == null) {
			// If there is no sharing target try to find a personal query
			query = QueryUtil.findPersonalQuery(queryName, projectArea, getTeamRepository().loggedInContributor(),
					getMonitor());
		} else {
			List<IAuditableHandle> sharingTargets = QueryUtil.findSharingTargets(sharingTargetNames,
					getProcessClientService(), getMonitor());
			if (sharingTargets == null) {
				throw new WorkItemCommandLineException(
						"ProcessArea that shares the query not found " + sharingTargetNames);
			}
			query = QueryUtil.findSharedQuery(queryName, projectArea, sharingTargets, getMonitor());

		}
		if (query == null) {
			throw new WorkItemCommandLineException("Query not found " + queryName);
		}
		return query;
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
	private void exportCSV(String filePath, IQueryDescriptor query,
			ColumnHeaderMappingHelper columnHeaderMapping) throws TeamRepositoryException {
		CSVWriter writer = null;
		try {
			// Create the writer
			writer = createWriter(filePath);
			exportAllData(columnHeaderMapping, query, writer);
		} finally {
			try {
				if (writer != null) {
					writer.flush();
					writer.close();
				}
			} catch (IOException e) {
				throw new WorkItemCommandLineException(e);
			}
		}
		setSuccess();
		
	}

	/**
	 * Create a CSV file writer to write the CSV file with the specific encoding
	 * 
	 * @param filePath
	 * @return
	 * @throws WorkItemCommandLineException
	 */
	private CSVWriter createWriter(String filePath) throws WorkItemCommandLineException {
		CSVWriter writer = null;
		try {
			// Create the file
			File outputFile = new File(filePath);
			// enable saving attachments
			if (getParameterManager().hasSwitch(SWITCH_DISABLE_ATTACHMENT_EXPORT)) {
				getWorkItemExportHelper().disableSaveAttachments();
			} else {
				getWorkItemExportHelper().enableSaveAttachments(outputFile.getParentFile().getAbsolutePath());
			}
			// @see http://opencsv.sourceforge.net/
			writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(outputFile), getFileEncoding()),
					getDelimiter(), getQuoteChar());
		} catch (UnsupportedEncodingException e) {
			throw new WorkItemCommandLineException("Exception creating CSV output writer: " + filePath, e);
		} catch (FileNotFoundException e) {
			throw new WorkItemCommandLineException("Exception creating CSV output writer: " + filePath, e);
		}
		return writer;
	}

	/**
	 * Perform the export
	 * 
	 * @param columnHeaderMapping
	 * @param query
	 * @param writer
	 * @throws TeamRepositoryException
	 */
	private void exportAllData(ColumnHeaderMappingHelper columnHeaderMapping, IQueryDescriptor query, CSVWriter writer)
			throws TeamRepositoryException {

		List<String> headerNames = columnHeaderMapping.analyzeColumnHeader(getHeaderAsIDs());
		writer.writeNext(headerNames.toArray(new String[headerNames.size()]));
		// Query the work items
		IQueryResult<IResult> results = QueryUtil.getUnresolvedQueryResult(query, isOverrideQueryResultSizeLimit());
		ResultSize resultSize = results.getResultSize(getMonitor());
		List<IWorkItemHandle> workItems = new ArrayList<IWorkItemHandle>(resultSize.getTotal());
		while (results.hasNext(null)) {
			IResult result = results.next(null);
			workItems.add((IWorkItemHandle) result.getItem());
		}

		for (IWorkItemHandle handle : workItems) {
			IWorkItem workItem = WorkItemUtil.resolveWorkItem(handle, IWorkItem.FULL_PROFILE, getWorkItemCommon(),
					getMonitor());
			if (workItem != null) {
				ArrayList<String> row = getRow(workItem, columnHeaderMapping.getParameters());
				writer.writeNext(row.toArray(new String[row.size()]));
				try {
					writer.flush();
				} catch (IOException e) {
					throw new WorkItemCommandLineException(e);
				}
			}
		}
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
	private ArrayList<String> getRow(IWorkItem workItem, List<ParameterValue> columns)
			throws WorkItemCommandLineException, TeamRepositoryException {
		ArrayList<String> row = new ArrayList<String>(columns.size());
		getResult().appendResultString("Exporting work item " + workItem.getId());
		for (int i = 0; i < columns.size(); i++) {
			ParameterValue column = columns.get(i);
			String value = "";
			try {
				value = getStringRepresentation(workItem, column);
			} catch (WorkItemCommandLineException e) {
				String message = "Exception exporting work item " + workItem.getId() + " column " + i + " attribute "
						+ column.getAttributeID() + " : " + e.getMessage();
				if (isIgnoreErrors()) {
					if (!isSuppressAttributeErrors()) {
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
					"Can not convert delimiter. Delimiter must have size 1 >" + delimiter + "<");
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
