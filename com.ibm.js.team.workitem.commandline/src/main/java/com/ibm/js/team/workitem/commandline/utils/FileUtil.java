/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

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
	
	
	/**
	 * Read a file into an array. Should be a short file
	 * 
	 * @param aFile
	 */
	public static ArrayList<String> getFileContent(String file) {
		ArrayList<String> list = new ArrayList<String>();
		try{
			FileInputStream fstream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null)   {
				list.add(strLine);
			}
			in.close();
		} catch (Exception e){
			System.out.println("Could not open file: "  + file);
			e.printStackTrace();
		}
		return list;
	}
	
	/**
	 * Read a file into an array. Should be a short file
	 * 
	 * @param aFile
	 */
	public static boolean storeFileContent(String fileName, String content) {
		File file= new File(fileName);
		try{
			FileUtils.writeByteArrayToFile(file,  content.getBytes());
		} catch (Exception e){
			System.out.println("Could not save file: "  + file);
			e.printStackTrace();
		}
		return true;
	}
}
