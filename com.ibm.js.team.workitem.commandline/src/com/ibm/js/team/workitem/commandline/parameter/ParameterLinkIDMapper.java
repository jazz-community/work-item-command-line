/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.parameter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import com.ibm.js.team.workitem.commandline.utils.ReferenceUtil;

/**
 * Simple mapping tool to provide custom aliases that can be used for link ID's.
 * 
 */
public class ParameterLinkIDMapper {

	// RTC Export Link type column header
	public static final String LINKNAME_AFFECTED_BY_DEFECT = "Affected by Defect";
	public static final String LINKNAME_AFFECTS_PLAN_ITEM = "Affects Plan Item";
	public static final String LINKNAME_AFFECTS_REQUIREMENT = "Affects Requirement";
	public static final String LINKNAME_AFFECTS_TEST_RESULT = "Affects Test Result";
	public static final String LINKNAME_BLOCKS = "Blocks";
	public static final String LINKNAME_BLOCKS_TEST_EXECUTION = "Blocks Test Execution";
	public static final String LINKNAME_CHILDREN = "Children";
	public static final String LINKNAME_CONTRIBUTES_TO = "Contributes To";
	public static final String LINKNAME_COPIED_FROM = "Copied From";
	public static final String LINKNAME_COPIES = "Copies";
	public static final String LINKNAME_DEPENDS_ON = "Depends On";
	public static final String LINKNAME_DUPLICATED_BY = "Duplicated By";
	public static final String LINKNAME_DUPLICATE_OF = "Duplicate Of";
	public static final String LINKNAME_IMPLEMENTS_REQUIREMENT = "Implements Requirement";
	public static final String LINKNAME_INCLUDED_IN_BUILDS = "Included in Builds";
	public static final String LINKNAME_PARENT = "Parent";
	public static final String LINKNAME_PREDECESSOR = "Predecessor";
	public static final String LINKNAME_RELATED = "Related";
	public static final String LINKNAME_RELATED_ARTIFACTS = "Related Artifacts";
	public static final String LINKNAME_RELATED_TEST_CASE = "Related Test Case";
	public static final String LINKNAME_RELATED_TEST_EXECUTION_RECORD = "Related Test Execution Record";
	public static final String LINKNAME_RELATED_TEST_PLAN = "Related Test Plan";
	public static final String LINKNAME_RELATED_TEST_SCRIPT = "Related Test Script";
	public static final String LINKNAME_RELATED_TEST_SUITE = "Related Test Suite";
	public static final String LINKNAME_REPORTED_AGAINST_BUILD = "Reported Against Builds";
	public static final String LINKNAME_RESOLVED_BY = "Resolved By";
	public static final String LINKNAME_RESOLVES = "Resolves";
	public static final String LINKNAME_SUCCESSOR = "Successor";
	public static final String LINKNAME_TESTED_BY_TEST_CASE = "Tested By Test Case";
	public static final String LINKNAME_TRACKS = "Tracks";
	public static final String LINKNAME_TRACKS_REQUIREMENT = "Tracks Requirement";
	// public static final String LINKNAME_CHANGE_SETS = "Change Sets";
	// public static final String LINKNAME_ATTACHMENTS = "Attachments";
	// public static final String LINKNAME_CHANGE_SETS_FILL_GAPS =
	// "Change Sets That Fill Gaps";
	// public static final String link10 = "Change Sets That Were Not Promoted";
	// public static final String link12 = "ClearCase Activities";
	// public static final String link13 = "ClearCase Versions";
	// public static final String link18 = "Deployment Definition";
	// public static final String link19 = "Deployment Result";
	// public static final String link23 = "importer dependency";
	// public static final String link25 = "Included in Deployments";
	// public static final String link26 = "Included in Packages";
	// public static final String link27 = "Included in Promotions";
	// public static final String link28 = "Mentions";
	// public static final String link29 = "Packaging Definition";
	// public static final String link30 = "Packaging Result";
	// public static final String link31 = "Packaging Summary Work Item";
	// public static final String link34 = "Previously Promoted Change Sets";
	// public static final String link35 = "Promoted Build Maps";
	// public static final String link36 = "Promoted Change Sets";
	// public static final String link37 = "Promoted Work Items";
	// public static final String link38 = "Promotion Build Result";
	// public static final String link39 = "Promotion Definition";
	// public static final String link51 = "SVN Revisions";
	// public static final String link53 = "Time Sheet Entry";
	// public static final String link56 = "tracks UCM object";
	// public static final String link57 = "Work Items Included in Packages";

	protected HashMap<String, String> iDMap = null;
	private static ParameterLinkIDMapper theMapper = null;

	private ParameterLinkIDMapper() {
		super();
		this.iDMap = new HashMap<String, String>();
		setMappings();
	}

	/**
	 * Creates a map from an external name (alias) to the internal representation
	 * 
	 * putMap("key", "Value");
	 */
	protected void setMappings() {
		// TODO: ReferenceUtil.LINKTYPE_RELATED_CHANGE_MANAGEMENT
		putMap(LINKNAME_PARENT, ReferenceUtil.LINKTYPE_PARENT);
		putMap(LINKNAME_CHILDREN, ReferenceUtil.LINKTYPE_CHILD);
		putMap(LINKNAME_PREDECESSOR, ReferenceUtil.LINKTYPE_PREDECESSOR_WORKITEM);
		putMap(LINKNAME_SUCCESSOR, ReferenceUtil.LINKTYPE_SUCCESSOR_WORKITEM);
		putMap(LINKNAME_TRACKS, ReferenceUtil.LINKTYPE_TRACKS_WORK_ITEM);
		// putMap(LINKNAME_CONTRIBUTES_TO,
		// ReferenceUtil.LINKTYPE_CONTRIBUTES_TO_WORK_ITEM);
		putMap(LINKNAME_AFFECTED_BY_DEFECT, ReferenceUtil.LINKTYPE_AFFECTED_BY_DEFECT);
		putMap(LINKNAME_AFFECTS_TEST_RESULT, ReferenceUtil.LINKTYPE_AFFECTS_EXECUTION_RESULT);
		putMap(LINKNAME_AFFECTS_PLAN_ITEM, ReferenceUtil.LINKTYPE_AFFECTS_PLAN_ITEM);
		putMap(LINKNAME_AFFECTS_REQUIREMENT, ReferenceUtil.LINKTYPE_AFFECTS_REQUIREMENT);
		putMap(LINKNAME_BLOCKS_TEST_EXECUTION, ReferenceUtil.LINKTYPE_BLOCKS_TEST_EXECUTION);
		putMap(LINKNAME_BLOCKS, ReferenceUtil.LINKTYPE_BLOCKS_WORKITEM);
		putMap(LINKNAME_COPIED_FROM, ReferenceUtil.LINKTYPE_COPIED_FROM_WORKITEM);
		putMap(LINKNAME_COPIES, ReferenceUtil.LINKTYPE_COPIED_WORKITEM);
		putMap(LINKNAME_DEPENDS_ON, ReferenceUtil.LINKTYPE_DEPENDS_ON_WORKITEM);
		putMap(LINKNAME_DUPLICATE_OF, ReferenceUtil.LINKTYPE_DUPLICATE_OF_WORKITEM);
		putMap(LINKNAME_DUPLICATED_BY, ReferenceUtil.LINKTYPE_DUPLICATE_WORKITEM);
		putMap(LINKNAME_IMPLEMENTS_REQUIREMENT, ReferenceUtil.LINKTYPE_IMPLEMENTS_REQUIREMENT);
		putMap(LINKNAME_INCLUDED_IN_BUILDS, ReferenceUtil.LINKTYPE_INCLUDEDINBUILD);
		// putMap(, ReferenceUtil.LINKTYPE_RELATED_CHANGE_MANAGEMENT);
		putMap(LINKNAME_RELATED_ARTIFACTS, ReferenceUtil.LINKTYPE_RELATED_ARTIFACT);
		putMap(LINKNAME_RELATED_TEST_CASE, ReferenceUtil.LINKTYPE_RELATED_TEST_CASE);
		putMap(LINKNAME_RELATED_TEST_EXECUTION_RECORD, ReferenceUtil.LINKTYPE_RELATED_TEST_EXECUTION_RECORD);
		putMap(LINKNAME_RELATED_TEST_PLAN, ReferenceUtil.LINKTYPE_RELATED_TEST_PLAN);
		putMap(LINKNAME_RELATED, ReferenceUtil.LINKTYPE_RELATED_WORKITEM);
		putMap(LINKNAME_REPORTED_AGAINST_BUILD, ReferenceUtil.LINKTYPE_REPORTED_AGAINST_BUILDRESULT);
		putMap(LINKNAME_RESOLVED_BY, ReferenceUtil.LINKTYPE_RESOLVED_BY_WORKITEM);
		putMap(LINKNAME_RESOLVES, ReferenceUtil.LINKTYPE_RESOLVES_WORKITEM);
		putMap(LINKNAME_TESTED_BY_TEST_CASE, ReferenceUtil.LINKTYPE_TESTED_BY_TEST_CASE);
		// putMap(LINKNAME_TRACKS, ReferenceUtil.LINKTYPE_TRACKS_CHANGES);
		putMap(LINKNAME_TRACKS_REQUIREMENT, ReferenceUtil.LINKTYPE_TRACKS_REQUIREMENT);
		// putMap(, ReferenceUtil.);
	}

	/**
	 * Put an entry into the map
	 * 
	 * @param key   - The external name or alias of the link (attribute)
	 * @param value - The internal representation
	 */
	protected void putMap(String key, String value) {
		getMap().put(key, value);
	}

	/**
	 * Get the map
	 * 
	 * @return
	 */
	protected HashMap<String, String> getMap() {
		return this.iDMap;
	}

	/**
	 * Check if there is an alias available for a property ID and pass it back if
	 * available.
	 * 
	 * @param linkName
	 * @return
	 */
	public String getFromAlias(String linkName) {
		String newVal = iDMap.get(linkName);
		if (null != newVal)
			return newVal;
		return linkName;
	}

	/**
	 * @return a string with
	 */
	public String helpMappings() {
		String mappings = "Available mappings:\n";

		Set<String> keys = iDMap.keySet();
		for (String key : keys) {
			mappings += "\n\t" + key + ": " + iDMap.get(key);
		}
		return mappings + "\n";
	}

	/**
	 * This is a singleton
	 * 
	 * @return the mapper
	 */
	private static ParameterLinkIDMapper getMapper() {
		if (theMapper == null) {
			theMapper = new ParameterLinkIDMapper();
		}
		return theMapper;
	}

	/**
	 * Check if there is an alias available for a link name and pass the ID it back
	 * if available.
	 * 
	 * @param linkName - the ID of the property
	 * @return the alias or the original property
	 */
	public static String getinternalID(String linkName) {
		return getMapper().getFromAlias(linkName);
	}

	/**
	 * Help to display
	 * 
	 * @return
	 */
	public static String helpParameterMappings() {
		return getMapper().helpMappings();
	}

	/**
	 * Get all the external link names (alias)
	 * 
	 * @return
	 */
	public static Set<String> getLinkNames() {
		return getMapper().getMap().keySet();
	}

	/**
	 * Get all the external link names (alias)
	 * 
	 * @return
	 */
	public static Collection<String> getLinkIDs() {
		return getMapper().getMap().values();
	}

}
