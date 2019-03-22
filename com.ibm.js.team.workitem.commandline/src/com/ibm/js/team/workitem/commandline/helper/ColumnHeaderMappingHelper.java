/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.helper;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.js.team.workitem.commandline.framework.ParameterValue;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.parameter.ColumnHeaderAttributeNameMapper;
import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.IWorkItemCommon;

/**
 * Helper to help with mapping a header or the column definitions to attribute
 * ID's
 * 
 */
public class ColumnHeaderMappingHelper {

	public static final String SEPARATOR_COLUMNS = ",";
	private static final String DEFAULT_COLUMNS = "workItemType,id,internalState,internalPriority,internalSeverity,summary,owner,creator";

	IProjectAreaHandle fProjectArea;
	IWorkItemCommon fWorkItemCommon;
	IProgressMonitor fMonitor;

	List<ParameterValue> columns = new ArrayList<ParameterValue>();
	private String[] fColumns = null;

	/**
	 * The constructor
	 * 
	 * @param projectArea
	 * @param workItemCommon
	 * @param monitor
	 */
	public ColumnHeaderMappingHelper(IProjectAreaHandle projectArea, IWorkItemCommon workItemCommon,
			IProgressMonitor monitor) {
		super();
		this.fProjectArea = projectArea;
		this.fWorkItemCommon = workItemCommon;
		this.fMonitor = monitor;
	}

	/**
	 * This must be run before the parameters collection can be used.
	 * 
	 * @param getIDs
	 * @return
	 * @throws TeamRepositoryException
	 * @throws WorkItemCommandLineException
	 */
	public List<String> analyzeColumnHeader(boolean getIDs)
			throws TeamRepositoryException, WorkItemCommandLineException {
		String[] exportColumns = getColumns();

		// If the column header is provided as ID, map the ID. If the header is
		// not an ID, map the name to an ID.
		if (exportColumns == null) {
			throw new WorkItemCommandLineException("Column header can not be null in column ");
		}

		ColumnHeaderAttributeNameMapper nameToIdMapper = new ColumnHeaderAttributeNameMapper(fProjectArea,
				fWorkItemCommon, fMonitor);
		int size = exportColumns.length;
		List<String> header = new ArrayList<String>(size);

		for (int i = 0; i < size; i++) {
			String col = exportColumns[i];
			// Column can't be null
			if (col == null) {
				throw new WorkItemCommandLineException("Column ID can not be null in column " + i);
			}
			// Map the column to an attribute first
			String val = col.trim();
			String id = nameToIdMapper.getID(val);
			if (id == null) {
				throw new WorkItemCommandLineException("Column header '" + col + "' ID can not be mapped in column " + i);
			}
			if (getIDs) {
				header.add(id);
			} else {
				String name = nameToIdMapper.getDisplayNameForID(id);
				if (name == null) {
					throw new WorkItemCommandLineException("Column header '" + col
							+ "' can not be mapped from ID '" + id + "' in column " + i);
				}
				header.add(name);
			}
			ParameterValue columnParameter = new ParameterValue(id, null, fProjectArea, fMonitor);
			addColumnParameter(i, columnParameter);
		}
		return header;
	}

	/**
	 * Add a parameter for a column
	 * 
	 * @param i
	 * @param columnParameter
	 */
	private void addColumnParameter(int i, ParameterValue columnParameter) {
		getParameters().add(i, columnParameter);
	}

	/**
	 * Get the parameters for a column
	 * 
	 * @return
	 */
	public List<ParameterValue> getParameters() {
		return this.columns;
	}

	/**
	 * Set the columns by passing a list of values separated by ','
	 * 
	 * @param columns
	 */
	public void setColumns(String columns) {
		fColumns = columns.split(SEPARATOR_COLUMNS);
	}

	/**
	 * Set the columns by passing an array of values
	 * 
	 * @param columns
	 */
	public void setColumns(String[] columns) {
		fColumns = columns;
	}

	/**
	 * get the columns
	 * 
	 * @return
	 */
	public String[] getColumns() {
		if (fColumns == null) {
			fColumns = DEFAULT_COLUMNS.split(SEPARATOR_COLUMNS);
		}
		return fColumns;
	}
}
