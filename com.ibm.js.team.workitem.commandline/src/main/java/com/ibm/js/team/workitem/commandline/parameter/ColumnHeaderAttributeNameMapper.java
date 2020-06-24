/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.parameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.js.team.workitem.commandline.utils.ReferenceUtil;
import com.ibm.team.links.common.registry.IEndPointDescriptor;
import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;

/**
 * Class to map between internal representations and display values of work item
 * attributes.
 * 
 */
public class ColumnHeaderAttributeNameMapper {

	private HashMap<String, String> nameIDMap = new HashMap<String, String>(50);
	private HashMap<String, String> idNameMap = new HashMap<String, String>(50);
	private HashMap<String, IAttribute> attributeMap = new HashMap<String, IAttribute>(50);
	private HashMap<String, IEndPointDescriptor> linkMap = new HashMap<String, IEndPointDescriptor>(50);
	private HashMap<String, String> attributeIdNameMap = new HashMap<String, String>(50);

	/**
	 * To get a new mapper
	 * 
	 * @param projectAreaHandle
	 * @param workItemCommon
	 * @param monitor
	 * @throws TeamRepositoryException
	 */
	public ColumnHeaderAttributeNameMapper(IProjectAreaHandle projectAreaHandle, IWorkItemCommon workItemCommon,
			IProgressMonitor monitor) throws TeamRepositoryException {
		super();
		getAttributeNameMap(projectAreaHandle, workItemCommon, monitor);
	}

	/**
	 * Get and if necessary fill the map
	 * 
	 * @param projectAreaHandle
	 * @param workItemCommon
	 * @param monitor
	 * @throws TeamRepositoryException
	 */
	private void getAttributeNameMap(IProjectAreaHandle projectAreaHandle, IWorkItemCommon workItemCommon,
			IProgressMonitor monitor) throws TeamRepositoryException {

		// Add all the attributes
		List<IAttribute> attributes = workItemCommon.findAttributes(projectAreaHandle, monitor);
		for (IAttribute attribute : attributes) {
			// It is possible to have whitespaces in display names
			String displayName = attribute.getDisplayName().trim();
			String id = attribute.getIdentifier().trim();
			String object = nameIDMap.get(displayName);
			if (object == null || object.isEmpty()) {
				nameIDMap.put(displayName, id);
				idNameMap.put(id, displayName);
				attributeIdNameMap.put(id, displayName);
				attributeMap.put(id, attribute);
			} else {
				System.err.println("Attribute: " + displayName + " with id: " + id + " already exists with id: "
						+ object);
			}
		}
		// Add all the pseudo attribute attachments
		idNameMap.put(ParameterIDMapper.PSEUDO_ATTRIBUTE_ATTACHMENTS, ParameterIDMapper.PSEUDO_ATTRIBUTE_ATTACHMENTS);
		attributeIdNameMap.put(ParameterIDMapper.PSEUDO_ATTRIBUTE_ATTACHMENTS,
				ParameterIDMapper.PSEUDO_ATTRIBUTE_ATTACHMENTS);
		// Add all the links
		Set<String> linkNames = ParameterLinkIDMapper.getLinkNames();
		for (String linkName : linkNames) {
			if (nameIDMap.get(linkName) == null) { // Resolved By is both an
													// attribute and link name,
													// ignore link
				String linkID = ParameterLinkIDMapper.getinternalID(linkName);
				nameIDMap.put(linkName, linkID);
				idNameMap.put(linkID, linkName);
				linkMap.put(linkID, ReferenceUtil.getReferenceEndpointDescriptor(linkID));
			}
		}
	}

	/**
	 * Check if there is an attribute/link with the matching display name and
	 * get its ID.
	 * 
	 * @param propertyName
	 *            - the display name of the property
	 * @return the id of the attribute or null
	 */
	private String getIDForName(String propertyName) {
		String id = nameIDMap.get(propertyName);
		if (id == null) {
			return null;
		}
		return id;
	}

	/**
	 * Check if there is an alias available for a property ID and pass it back
	 * if available.
	 * 
	 * @param attributeName
	 * @return
	 */
	private String getAliasID(String attributeName) {
		return ParameterIDMapper.getAlias(attributeName);
	}

	/**
	 * 
	 * 
	 * @param attributeName
	 * @return
	 */
	private boolean hasID(String attributeName) {
		String alias = ParameterIDMapper.getAlias(attributeName);
		return idNameMap.containsKey(alias);
	}

	/**
	 * Get an internal ID for an Id or a DisplayName
	 * 
	 * @param column
	 * @return
	 */
	public String getID(String nameOrID) {
		if (hasID(nameOrID)) {
			// The column is an ID
			nameOrID = getAliasID(nameOrID);
		} else {
			// the column must be a name
			String idFromName = getIDForName(nameOrID);
			if (idFromName != null) {
				nameOrID = getAliasID(idFromName);
			}
		}
		return nameOrID;
	}

	/**
	 * @param attributeID
	 * @return
	 */
	public IAttribute getAttribute(String attributeID) {
		return attributeMap.get(attributeID);
	}

	/**
	 * @param id
	 * @return
	 */
	public String getDisplayNameForID(String id) {
		return this.idNameMap.get(id);
	}

	/**
	 * @param linkID
	 * @return
	 */
	public boolean isLinkType(String linkID) {
		return linkMap.containsKey(linkID);
	}

	/**
	 * @return
	 */
	public HashMap<String, IAttribute> getAttributeMap() {
		return attributeMap;
	}

	/**
	 * @return
	 */
	public HashMap<String, IEndPointDescriptor> getLinkMap() {
		return linkMap;
	}

	/**
	 * @return
	 */
	private HashMap<String, String> getAttributeIdNameMap() {
		return attributeIdNameMap;
	}

	/**
	 * Get the names of all the available attributes and supported links in a
	 * sorted way.
	 * 
	 * @param projectArea
	 * @return
	 * @throws TeamRepositoryException
	 */
	public String[] getAllColumnsPreSorted() throws TeamRepositoryException {

		ArrayList<String> priorityAttributeIDs = new ArrayList<String>(10);
		priorityAttributeIDs.add(IWorkItem.ID_PROPERTY);
		priorityAttributeIDs.add(IWorkItem.TYPE_PROPERTY);
		priorityAttributeIDs.add(IWorkItem.SUMMARY_PROPERTY);

		HashMap<String, String> attributeNameMap = getAttributeIdNameMap();

		ArrayList<String> sortedAttribs = new ArrayList<String>();
		for (String id : priorityAttributeIDs) {
			sortedAttribs.add(attributeNameMap.get(id));
			attributeNameMap.remove(id);
		}

		ArrayList<String> allAttribs = new ArrayList<String>(attributeNameMap.values().size());
		allAttribs.addAll(attributeNameMap.values());
		Collections.sort(allAttribs);
		sortedAttribs.addAll(allAttribs);

		Set<String> linkNames = ParameterLinkIDMapper.getLinkNames();
		ArrayList<String> sortedLinks = new ArrayList<String>(linkNames.size());
		sortedLinks.addAll(linkNames);
		Collections.sort(sortedLinks);

		sortedAttribs.addAll(sortedLinks);
		return sortedAttribs.toArray(new String[sortedAttribs.size()]);
	}

}
