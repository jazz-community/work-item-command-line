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
import com.ibm.team.process.common.IProjectArea;
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
	private HashMap<String, IAttribute> attributeMap = new HashMap<String, IAttribute>(
			50);
	private HashMap<String, IEndPointDescriptor> linkMap = new HashMap<String, IEndPointDescriptor>(
			50);

	/**
	 * To get a new mapper
	 * 
	 * @param projectAreaHandle
	 * @param workItemCommon
	 * @param monitor
	 * @throws TeamRepositoryException
	 */
	public ColumnHeaderAttributeNameMapper(
			IProjectAreaHandle projectAreaHandle,
			IWorkItemCommon workItemCommon, IProgressMonitor monitor)
			throws TeamRepositoryException {
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
	private void getAttributeNameMap(IProjectAreaHandle projectAreaHandle,
			IWorkItemCommon workItemCommon, IProgressMonitor monitor)
			throws TeamRepositoryException {

		// Add all the attributes
		List<IAttribute> attributes = workItemCommon.findAttributes(
				projectAreaHandle, monitor);
		for (IAttribute attribute : attributes) {
			String displayName = attribute.getDisplayName();
			String id = attribute.getIdentifier();
			nameIDMap.put(displayName, id);
			idNameMap.put(id, displayName);
			attributeMap.put(id, attribute);
		}
		// Add all the links
		Set<String> linkNames = ParameterLinkIDMapper.getLinkNames();
		for (String linkName : linkNames) {
			String linkID = ParameterLinkIDMapper.getinternalID(linkName);
			nameIDMap.put(linkName, linkID);
			idNameMap.put(linkID, linkName);
			linkMap.put(linkID,
					ReferenceUtil.getReferenceEndpointDescriptor(linkID));
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
		return this.nameIDMap.get(id);
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
	 * Get all the available attributes and supported links in a sorted way.
	 * 
	 * @param projectArea
	 * @return
	 * @throws TeamRepositoryException
	 */
	public String[] getAllColumnsPreSorted() throws TeamRepositoryException {

		HashMap<String, IAttribute> attributeMap = getAttributeMap(); 

		ArrayList<String> sortedAttribs = new ArrayList<String>();
		sortedAttribs.add(IWorkItem.ID_PROPERTY);
		sortedAttribs.add(IWorkItem.TYPE_PROPERTY);
		sortedAttribs.add(IWorkItem.SUMMARY_PROPERTY);

		attributeMap.remove(IWorkItem.ID_PROPERTY);
		attributeMap.remove(IWorkItem.TYPE_PROPERTY);
		attributeMap.remove(IWorkItem.SUMMARY_PROPERTY);
		ArrayList<String> allAttribs = new ArrayList<String>(attributeMap.keySet().size());
		allAttribs.addAll(attributeMap.keySet());
		Collections.sort(allAttribs);

		HashMap<String, IEndPointDescriptor> linkMap = getLinkMap();
		ArrayList<String> sortedLinks = new ArrayList<String>(linkMap.keySet().size());
		sortedLinks.addAll(linkMap.keySet());
		Collections.sort(sortedLinks); 

		sortedAttribs.addAll(allAttribs);
		sortedAttribs.add(ParameterIDMapper.PSEUDO_ATTRIBUTE_ATTACHMENTS); // This is not a property add the artificial one
		sortedAttribs.addAll(sortedLinks);
		return sortedAttribs.toArray(new String[sortedAttribs.size()]);
	}

}
