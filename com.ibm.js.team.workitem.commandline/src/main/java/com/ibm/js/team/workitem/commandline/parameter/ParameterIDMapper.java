/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.parameter;

import java.util.HashMap;
import java.util.Set;

import com.ibm.team.workitem.common.model.IWorkItem;

/**
 * Simple mapping tool to provide custom aliases that can be used for attribute
 * ID's.
 * 
 * <pre>
 * 
 * Internal and external ID's for built in attributes 
 * 
 * Archived 			ID: archived				External_ID: archived 	
 * Corrected Estimate 	ID: correctedEstimate		External_ID: com.ibm.team.workitem.attribute.correctedestimate
 * Created By 			ID: creator 				External_ID: com.ibm.team.workitem.attribute.creator
 * Creation Date 		ID: creationDate 			External_ID: com.ibm.team.workitem.attribute.creationdate
 * Due Date 			ID: dueDate					External_ID: com.ibm.team.workitem.attribute.duedate
 * Description 			ID: description				External_ID: com.ibm.team.workitem.attribute.description
 * Estimate 			ID: duration				External_ID: com.ibm.team.workitem.attribute.duration
 * Filed Against	 	ID: category				External_ID: com.ibm.team.workitem.attribute.category
 * Found In 			ID: foundIn					External_ID: com.ibm.team.workitem.attribute.version
 * Id 					ID: id 						External_ID: com.ibm.team.workitem.attribute.id
 * Modified Date 		ID: modified 				External_ID: com.ibm.team.workitem.attribute.modified	
 * Modified By 		 	ID: modifiedBy				External_ID: com.ibm.team.workitem.attribute.modifiedby
 * Owned By 			ID: owner					External_ID: com.ibm.team.workitem.attribute.owner
 * Planned For 			ID: target					External_ID: com.ibm.team.workitem.attribute.target
 * Priority 			ID: internalPriority		External_ID: com.ibm.team.workitem.attribute.priority
 * Project Area 		ID: projectArea				External_ID: com.ibm.team.workitem.attribute.projectarea
 * Resolution 			ID: internalResolution		External_ID: com.ibm.team.workitem.attribute.resolutiondate	
 * Resolution Date 		ID: resolutionDate			External_ID: com.ibm.team.workitem.attribute.resolutiondate
 * Resolved By 			ID: resolver				External_ID: com.ibm.team.workitem.attribute.resolver
 * Restricted Access 	ID: contextId				External_ID: contextId
 * Severity 			ID: internalSeverity		External_ID: com.ibm.team.workitem.attribute.severity
 * Start Date 			ID: startDate				External_ID: startDate
 * Status 				ID: internalState			External_ID: com.ibm.team.workitem.attribute.state
 * Subscribed By 		ID: internalSubscriptions	External_ID: com.ibm.team.workitem.attribute.subscriptions
 * Summary 				ID: summary					External_ID: com.ibm.team.workitem.attribute.summary
 * Tags 				ID: internalTags			External_ID: com.ibm.team.workitem.attribute.tags
 * Time Spent 			ID: timeSpent				External_ID: com.ibm.team.workitem.attribute.timespent
 * Type		 			ID: workItemType			External_ID: com.ibm.team.workitem.attribute.workitemtype
 * 
 * </pre>
 * 
 */
public class ParameterIDMapper {

	// This is a special external ID that is used during export/mapping
	public static final String ID_COM_IBM_TEAM_WORKITEM_ATTRIBUTE_WORKITEMTYPE = "com.ibm.team.workitem.attribute.workitemtype";
	// Pseudo Attribute Attachments
	public static final String PSEUDO_ATTRIBUTE_ATTACHMENTS = "Attachments";

	protected HashMap<String, String> iDMap = null;

	private static ParameterIDMapper theMapper = null;

	/**
	 * Constructor
	 * 
	 */
	private ParameterIDMapper() {
		this.iDMap = new HashMap<String, String>();
		setMappings();
	}

	/**
	 * Create the mapping of aliases/display names for attributes to the
	 * internal representation
	 * 
	 */
	private void setMappings() {
		// JavaScript
		putMap("SEVERITY", IWorkItem.SEVERITY_PROPERTY);
		putMap("PRIORITY", IWorkItem.PRIORITY_PROPERTY);
		putMap("FOUND_IN", IWorkItem.FOUND_IN_PROPERTY);
		putMap("ID", IWorkItem.ID_PROPERTY);
		putMap("TYPE", IWorkItem.TYPE_PROPERTY);
		putMap("PROJECT_AREA", IWorkItem.PROJECT_AREA_PROPERTY);
		putMap("SUMMARY", IWorkItem.SUMMARY_PROPERTY);
		putMap("STATE", IWorkItem.STATE_PROPERTY);
		putMap("CREATOR", IWorkItem.CREATOR_PROPERTY);
		putMap("OWNER", IWorkItem.OWNER_PROPERTY);
		putMap("DESCRIPTION", IWorkItem.DESCRIPTION_PROPERTY);
		putMap("CREATION_DATE", IWorkItem.CREATION_DATE_PROPERTY);
		putMap("RESOLUTION", IWorkItem.RESOLUTION_PROPERTY);
		putMap("DUE_DATE", IWorkItem.DUE_DATE_PROPERTY);
		putMap("ESTIMATE", "duration");
		putMap("CORRECTED_ESTIMATE", "correctedEstimate");
		putMap("TIME_SPENT", "timeSpent");
		putMap("FILED_AGAINST", IWorkItem.CATEGORY_PROPERTY);
		putMap("PLANNED_FOR", IWorkItem.TARGET_PROPERTY);
		putMap("RESOLVER", IWorkItem.RESOLVER_PROPERTY);
		putMap("RESOLUTION_DATE", IWorkItem.RESOLUTION_DATE_PROPERTY);
		putMap("TAGS", IWorkItem.TAGS_PROPERTY);
		putMap("MODIFIED", IWorkItem.MODIFIED_PROPERTY);
		putMap("MODIFIED_BY", IWorkItem.MODIFIED_BY_PROPERTY);

		// Importer
		putMap("com.ibm.team.workitem.attribute.severity", IWorkItem.SEVERITY_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.priority", IWorkItem.PRIORITY_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.version", IWorkItem.FOUND_IN_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.id", IWorkItem.ID_PROPERTY);
		putMap(ID_COM_IBM_TEAM_WORKITEM_ATTRIBUTE_WORKITEMTYPE, IWorkItem.TYPE_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.projectarea", IWorkItem.PROJECT_AREA_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.summary", IWorkItem.SUMMARY_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.state", IWorkItem.STATE_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.creator", IWorkItem.CREATOR_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.owner", IWorkItem.OWNER_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.description", IWorkItem.DESCRIPTION_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.creationdate", IWorkItem.CREATION_DATE_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.resolutiondate", IWorkItem.RESOLUTION_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.duedate", IWorkItem.DUE_DATE_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.duration", IWorkItem.DURATION_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.correctedestimate", "correctedEstimate");
		getMap().put("com.ibm.team.workitem.attribute.timespent", "timeSpent");
		putMap("com.ibm.team.workitem.attribute.category", IWorkItem.CATEGORY_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.target", IWorkItem.TARGET_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.resolver", IWorkItem.RESOLVER_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.resolutiondate", IWorkItem.RESOLUTION_DATE_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.tags", IWorkItem.TAGS_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.modified", IWorkItem.MODIFIED_PROPERTY);
		putMap("com.ibm.team.workitem.attribute.modifiedby", IWorkItem.MODIFIED_BY_PROPERTY);
	}

	/**
	 * This is a singleton
	 * 
	 * @return the mapper
	 */
	private static ParameterIDMapper getMapper() {
		if (theMapper == null) {
			theMapper = new ParameterIDMapper();
		}
		return theMapper;
	}

	/**
	 * Check if there is an alias available for a property ID and pass it back
	 * if available.
	 * 
	 * @param propertyID
	 *            - the ID of the property
	 * @return the alias or the original property
	 */
	public static String getAlias(String propertyID) {
		return getMapper().getFromAlias(propertyID);
	}

	/**
	 * To print in help
	 * 
	 * @return
	 */
	public static String helpParameterMappings() {
		return getMapper().helpMappings();
	}

	/**
	 * Get the whole map
	 * 
	 * @return
	 */
	protected HashMap<String, String> getMap() {
		return this.iDMap;
	}

	/**
	 * Put an entry into the map
	 * 
	 * @param key
	 * @param value
	 */
	protected void putMap(String key, String value) {
		getMap().put(key, value);
	}

	/**
	 * Check if there is an alias available for a property ID and pass it back
	 * if available.
	 * 
	 * @param propertyID
	 * @return
	 */
	public String getFromAlias(String propertyID) {
		String newVal = iDMap.get(propertyID);
		if (null != newVal)
			return newVal;
		return propertyID;
	}

	/**
	 * Generate help content
	 * 
	 * @return a string with
	 */
	private String helpMappings() {
		String mappings = "Available mappings:\n";

		Set<String> keys = iDMap.keySet();
		for (String key : keys) {
			mappings += "\n\t" + key + ": " + iDMap.get(key);
		}
		return mappings + "\n";
	}

}
