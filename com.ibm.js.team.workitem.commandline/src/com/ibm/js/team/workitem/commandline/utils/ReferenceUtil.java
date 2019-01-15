/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.utils;

import java.util.HashMap;

import com.ibm.js.team.workitem.commandline.parameter.ParameterLinkIDMapper;
import com.ibm.team.build.internal.common.links.BuildLinkTypes;
import com.ibm.team.links.common.registry.IEndPointDescriptor;
import com.ibm.team.links.common.registry.ILinkTypeRegistry;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.common.model.WorkItemLinkTypes;

/**
 * Utility class to help with references Matches external (attribute) names to
 * internal format Provides the endpoint descriptors
 * 
 */
public class ReferenceUtil {

	// Link type categories used to determine how to export and import the links
	public static final String CATEGORY_LINKTYPE_CLM_WORKITEM = "LINK_TYPE_CATEGORY_CLM_WORK_ITEM";
	public static final String CATEGORY_LINKTYPE_CLM_URI = "LINK_TYPE_CATEGORY_CLM_URI";
	public static final String CATEGORY_LINKTYPE_BULD = "LINK_TYPE_CATEGORY_BULD";
	public static final String CATEGORY_LINKTYPE_WORK_ITEM = "LINK_TYPE_CATEGORY_WORK_ITEM";
	// WorkItem Link Types
	public static final String LINKTYPE_PARENT = "parent";
	public static final String LINKTYPE_CHILD = "child";
	public static final String LINKTYPE_BLOCKS_WORKITEM = "blocks";
	public static final String LINKTYPE_COPIED_FROM_WORKITEM = "copied_from";
	public static final String LINKTYPE_COPIED_WORKITEM = "copied";
	public static final String LINKTYPE_DEPENDS_ON_WORKITEM = "depends_on";
	public static final String LINKTYPE_DUPLICATE_OF_WORKITEM = "duplicate_of";
	public static final String LINKTYPE_DUPLICATE_WORKITEM = "duplicate";
	public static final String LINKTYPE_MENTIONS_WORKITEM = "mentions";
	public static final String LINKTYPE_PREDECESSOR_WORKITEM = "predecessor";
	public static final String LINKTYPE_RELATED_WORKITEM = "related";
	public static final String LINKTYPE_RESOLVED_BY_WORKITEM = "resolved_by";
	public static final String LINKTYPE_RESOLVES_WORKITEM = "resolves";
	public static final String LINKTYPE_SUCCESSOR_WORKITEM = "successor";

	// Build result link types
	public static final String LINKTYPE_REPORTED_AGAINST_BUILDRESULT = "reportAgainstBuild";
	public static final String LINKTYPE_INCLUDEDINBUILD = "includedInBuild";

	// URI/Location based link types
	public static final String LINKTYPE_RELATED_ARTIFACT = "related_artifact";
	public static final String LINKTYPE_AFFECTS_EXECUTION_RESULT = "affects_execution_result";
	public static final String LINKTYPE_IMPLEMENTS_REQUIREMENT = "implements_requirement";
	public static final String LINKTYPE_RELATED_TEST_EXECUTION_RECORD = "related_execution_record";
	public static final String LINKTYPE_AFFECTS_REQUIREMENT = "affects_requirement";
	public static final String LINKTYPE_RELATED_TEST_CASE = "related_test_case";
	public static final String LINKTYPE_RELATED_TEST_PLAN = "related_test_plan";
	public static final String LINKTYPE_TESTED_BY_TEST_CASE = "tested_by_test_case";
	public static final String LINKTYPE_TRACKS_CHANGES = "scm_tracks_scm_changes";
	public static final String LINKTYPE_TRACKS_REQUIREMENT = "tracks_requirement";
	public static final String LINKTYPE_BLOCKS_TEST_EXECUTION = "blocks_test_execution";

	// work Item to work item links across repositories named location based
	public static final String LINKTYPE_AFFECTS_PLAN_ITEM = "affects_plan_item";
	// // the reverse to affects plan item
	public static final String LINKTYPE_AFFECTED_BY_DEFECT = "affected_by_defect";
	public static final String LINKTYPE_RELATED_CHANGE_MANAGEMENT = "related_change_management";
	public static final String LINKTYPE_TRACKS_WORK_ITEM = "tracks_workitem";
//	public static final String LINKTYPE_CONTRIBUTES_TO_WORK_ITEM = "contributes_to_workitem";
	// SCM change set link, can only be set by the SCM component
	// public static final String LINKTYPE_CHANGESET = "scm_changeset";
	// Designmanager
	// public static final String LINKTYPE_ELABORATED_BY = "elaborated_by";

	private static HashMap<String, IEndPointDescriptor> fWorkItemEndPointDescriptorMap = null;
	private static HashMap<String, IEndPointDescriptor> fCLM_URI_EndPointDescriptorMap = null;
	private static HashMap<String, IEndPointDescriptor> fCLM_WI_EndPointDescriptorMap = null;
	private static HashMap<String, IEndPointDescriptor> fBuild_EndPointDescriptorMap = null;

	// private static HashMap<String, String> fBuild_EndPointDescriptorMap =
	// null;

	/**
	 * Creates a map with string to IEndPointDescriptor values to create links
	 * to local work items
	 * 
	 * @return the map created
	 */
	public static HashMap<String, IEndPointDescriptor> getWorkItemEndPointDescriptorMap() {
		if (fWorkItemEndPointDescriptorMap == null) {
			HashMap<String, IEndPointDescriptor> map = new HashMap<String, IEndPointDescriptor>();
			map.put(ReferenceUtil.LINKTYPE_PARENT,
					WorkItemEndPoints.PARENT_WORK_ITEM);
			map.put(ReferenceUtil.LINKTYPE_CHILD,
					WorkItemEndPoints.CHILD_WORK_ITEMS);
			map.put(ReferenceUtil.LINKTYPE_BLOCKS_WORKITEM,
					WorkItemEndPoints.BLOCKS_WORK_ITEM);
			map.put(ReferenceUtil.LINKTYPE_COPIED_FROM_WORKITEM,
					WorkItemEndPoints.COPIED_FROM_WORK_ITEM);
			map.put(ReferenceUtil.LINKTYPE_COPIED_WORKITEM,
					WorkItemEndPoints.COPIED_WORK_ITEM);
			map.put(ReferenceUtil.LINKTYPE_DEPENDS_ON_WORKITEM,
					WorkItemEndPoints.DEPENDS_ON_WORK_ITEM);
			map.put(ReferenceUtil.LINKTYPE_DUPLICATE_OF_WORKITEM,
					WorkItemEndPoints.DUPLICATE_OF_WORK_ITEM);
			map.put(ReferenceUtil.LINKTYPE_DUPLICATE_WORKITEM,
					WorkItemEndPoints.DUPLICATE_WORK_ITEM);
			map.put(ReferenceUtil.LINKTYPE_MENTIONS_WORKITEM,
					WorkItemEndPoints.MENTIONS);
			map.put(ReferenceUtil.LINKTYPE_PREDECESSOR_WORKITEM,
					WorkItemEndPoints.PREDECESSOR_WORK_ITEMS);
			map.put(ReferenceUtil.LINKTYPE_RELATED_WORKITEM,
					WorkItemEndPoints.RELATED_WORK_ITEM);
			map.put(ReferenceUtil.LINKTYPE_RESOLVED_BY_WORKITEM,
					WorkItemEndPoints.RESOLVED_BY_WORK_ITEM);
			map.put(ReferenceUtil.LINKTYPE_RESOLVES_WORKITEM,
					WorkItemEndPoints.RESOLVES_WORK_ITEM);
			map.put(ReferenceUtil.LINKTYPE_SUCCESSOR_WORKITEM,
					WorkItemEndPoints.SUCCESSOR_WORK_ITEMS);
			fWorkItemEndPointDescriptorMap = map;
		}

		return fWorkItemEndPointDescriptorMap;
	}

	/**
	 * Creates a map with string to IEndPointDescriptor values to create links
	 * to CLM URI's (not work items)
	 * 
	 * @return the map created
	 */
	public static HashMap<String, IEndPointDescriptor> getCLM_URI_EndPointDescriptorMap() {
		if (fCLM_URI_EndPointDescriptorMap == null) {
			HashMap<String, IEndPointDescriptor> map = new HashMap<String, IEndPointDescriptor>();
			map.put(ReferenceUtil.LINKTYPE_RELATED_ARTIFACT,
					WorkItemEndPoints.RELATED_ARTIFACT);
			map.put(ReferenceUtil.LINKTYPE_AFFECTS_EXECUTION_RESULT,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							WorkItemLinkTypes.AFFECTS_EXECUTION_RESULT)
							.getTargetEndPointDescriptor());
			// map.put(ReferenceUtil.LINKTYPE_ELABORATED_BY,
			// ILinkTypeRegistry.INSTANCE
			// .getLinkType(WorkItemLinkTypes.ELABORATED_BY)
			// .getTargetEndPointDescriptor());
			map.put(ReferenceUtil.LINKTYPE_IMPLEMENTS_REQUIREMENT,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							WorkItemLinkTypes.IMPLEMENTS_REQUIREMENT)
							.getTargetEndPointDescriptor());
			map.put(ReferenceUtil.LINKTYPE_RELATED_TEST_EXECUTION_RECORD,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							WorkItemLinkTypes.RELATED_EXECUTION_RECORD)
							.getTargetEndPointDescriptor());
			map.put(ReferenceUtil.LINKTYPE_AFFECTS_REQUIREMENT,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							WorkItemLinkTypes.RELATED_REQUIREMENT)
							.getTargetEndPointDescriptor());
			map.put(ReferenceUtil.LINKTYPE_RELATED_TEST_CASE,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							WorkItemLinkTypes.RELATED_TEST_CASE)
							.getTargetEndPointDescriptor());
			map.put(ReferenceUtil.LINKTYPE_RELATED_TEST_PLAN,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							WorkItemLinkTypes.RELATED_TEST_PLAN)
							.getTargetEndPointDescriptor());
			map.put(ReferenceUtil.LINKTYPE_BLOCKS_TEST_EXECUTION,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							WorkItemLinkTypes.BLOCKS_EXECUTION_RECORD)
							.getTargetEndPointDescriptor());
			map.put(ReferenceUtil.LINKTYPE_TESTED_BY_TEST_CASE,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							WorkItemLinkTypes.TESTED_BY_TEST_CASE)
							.getTargetEndPointDescriptor());
			map.put(ReferenceUtil.LINKTYPE_TRACKS_CHANGES,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							WorkItemLinkTypes.TRACKS_CHANGES)
							.getTargetEndPointDescriptor());
			// map.put(LINKTYPE_CHANGESET, ILinkTypeRegistry.INSTANCE
			// .getLinkType(WorkItemLinkTypes.CHANGE_SET)
			// .getTargetEndPointDescriptor());
			map.put(ReferenceUtil.LINKTYPE_TRACKS_REQUIREMENT,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							WorkItemLinkTypes.TRACKS_REQUIREMENT)
							.getTargetEndPointDescriptor());
			fCLM_URI_EndPointDescriptorMap = map;
		}
		return fCLM_URI_EndPointDescriptorMap;
	}

	/**
	 * Creates a map with string to IEndPointDescriptor values to create CLM
	 * links to local and remote work items (e.g. located on a different CCM
	 * server)
	 * 
	 * @return the map created
	 */
	public static HashMap<String, IEndPointDescriptor> getCLM_WI_EndPointDescriptorMap() {

		if (fCLM_WI_EndPointDescriptorMap == null) {
			HashMap<String, IEndPointDescriptor> map = new HashMap<String, IEndPointDescriptor>();
			map.put(ReferenceUtil.LINKTYPE_AFFECTED_BY_DEFECT,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							WorkItemLinkTypes.AFFECTED_BY_DEFECT)
							.getTargetEndPointDescriptor());
			map.put(ReferenceUtil.LINKTYPE_AFFECTS_PLAN_ITEM,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							WorkItemLinkTypes.AFFECTS_PLAN_ITEM)
							.getTargetEndPointDescriptor());
			map.put(ReferenceUtil.LINKTYPE_RELATED_CHANGE_MANAGEMENT,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							WorkItemLinkTypes.RELATED_CHANGE_MANAGEMENT)
							.getTargetEndPointDescriptor());
			map.put(ReferenceUtil.LINKTYPE_TRACKS_WORK_ITEM,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							WorkItemLinkTypes.TRACKS_WORK_ITEM)
							.getTargetEndPointDescriptor());
			fCLM_WI_EndPointDescriptorMap = map;
		}
		return fCLM_WI_EndPointDescriptorMap;
	}

	/**
	 * Creates a map with string to IEndPointDescriptor values to create CLM
	 * links to local and remote work items (e.g. located on a different CCM
	 * server)
	 * 
	 * @return the map created
	 */
	public static HashMap<String, IEndPointDescriptor> getBuild_EndPointDescriptorMap() {

		if (fBuild_EndPointDescriptorMap == null) {
			HashMap<String, IEndPointDescriptor> map = new HashMap<String, IEndPointDescriptor>();
			map.put(ReferenceUtil.LINKTYPE_REPORTED_AGAINST_BUILDRESULT,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							BuildLinkTypes.REPORTED_WORK_ITEMS)
							.getSourceEndPointDescriptor());
			map.put(ReferenceUtil.LINKTYPE_INCLUDEDINBUILD,
					ILinkTypeRegistry.INSTANCE.getLinkType(
							BuildLinkTypes.INCLUDED_WORK_ITEMS)
							.getSourceEndPointDescriptor());
			fBuild_EndPointDescriptorMap = map;
		}
		return fBuild_EndPointDescriptorMap;
	}

	/**
	 * Get the endpoint descriptor for a link ID
	 * 
	 * @param linkID
	 * @return
	 */
	public static IEndPointDescriptor getReferenceEndpointDescriptor(
			String linkID) {
		linkID = ParameterLinkIDMapper.getinternalID(linkID);
		if (linkID == null) {
			return null;
		}
		// Now I have the ID, I need to find it in a map.
		IEndPointDescriptor link = getWorkItemLinkType(linkID);
		if (link != null) {
			return link;
		}
		link = getBuildLinkType(linkID);
		if (link != null) {
			return link;
		}
		link = getCLM_URI_LinkType(linkID);
		if (link != null) {
			return link;
		}
		link = getCLM_WI_LinkType(linkID);
		if (link != null) {
			return link;
		}
		return null;
	}

	/**
	 * Returns a string with the reference type for a given link ID
	 * 
	 * @param linkID
	 * @return
	 */
	public static String getReferenceType(String linkID) {
		linkID = ParameterLinkIDMapper.getinternalID(linkID);
		if (linkID == null) {
			return null;
		}
		// Now I have the ID, I need to find it in a map.
		IEndPointDescriptor link = getWorkItemLinkType(linkID);
		if (link != null) {
			return CATEGORY_LINKTYPE_WORK_ITEM;
		}
		link = getBuildLinkType(linkID);
		if (link != null) {
			return CATEGORY_LINKTYPE_BULD;
		}
		link = getCLM_URI_LinkType(linkID);
		if (link != null) {
			return CATEGORY_LINKTYPE_CLM_URI;
		}
		link = getCLM_WI_LinkType(linkID);
		if (link != null) {
			return CATEGORY_LINKTYPE_CLM_WORKITEM;
		}
		return null;
	}

	/**
	 * Test if a link ID is a link type
	 * 
	 * @param linkID
	 * @return
	 */
	public static boolean isLinkType(String linkID) {
		if (getReferenceEndpointDescriptor(linkID) == null) {
			return false;
		}
		return true;
	}

	/**
	 * Am I a build result related link?
	 * 
	 * @param linkID
	 * @return
	 */
	private static IEndPointDescriptor getBuildLinkType(String linkID) {
		HashMap<String, IEndPointDescriptor> map = getBuild_EndPointDescriptorMap();
		return map.get(linkID);
	}

	/**
	 * Am I a CLM link to a URI?
	 * 
	 * @param linkID
	 * @return
	 */
	private static IEndPointDescriptor getCLM_URI_LinkType(String linkID) {
		HashMap<String, IEndPointDescriptor> map = getCLM_URI_EndPointDescriptorMap();
		return map.get(linkID);
	}

	/**
	 * Am I a CLM link to a work item?
	 * 
	 * @param linkID
	 * @return
	 */
	private static IEndPointDescriptor getCLM_WI_LinkType(String linkID) {
		HashMap<String, IEndPointDescriptor> map = getCLM_WI_EndPointDescriptorMap();
		return map.get(linkID);
	}

	/**
	 * Am I a work item link?
	 * 
	 * @param linkID
	 * @return
	 */
	private static IEndPointDescriptor getWorkItemLinkType(String linkID) {
		HashMap<String, IEndPointDescriptor> map = getWorkItemEndPointDescriptorMap();
		return map.get(linkID);
	}

	/**
	 * Checks if this is a link with a work item as target item link
	 * 
	 * @param linkID
	 * @return
	 */
	public static boolean isWorkItemLink(String linkID) {
		if (null != ReferenceUtil.getCLM_WI_EndPointDescriptorMap().get(linkID)) {
			return true;
		}
		if (null != ReferenceUtil.getWorkItemEndPointDescriptorMap()
				.get(linkID)) {
			return true;
		}
		return false;
	}
}
