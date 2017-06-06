/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.utils;

import java.io.File;

/**
 * Tool for file access
 * 
 * @see com.ibm.team.filesystem.rcp.core.internal.compare.ExternalCompareToolsUtil
 * @see com.ibm.team.filesystem.setup.junit.internal.SourceControlContribution
 * 
 * 
 */
public class FileUtil {

	/**
	 * Create a folder
	 * 
	 * @param aFolder
	 */
	public static void createFolderWithParents(File aFolder) {
		if (!aFolder.exists()) {
			aFolder.mkdirs();
		}
	}

	/**
	 * Create a folder
	 * 
	 * @param aFolder
	 */
	public static void createFolderWithParents(String folderName) {
		File aFolder = new File(folderName);
		if (!aFolder.exists()) {
			aFolder.mkdirs();
		}
	}
}
