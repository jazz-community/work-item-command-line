/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ibm.js.team.workitem.commandline.commands.ExportWorkItemsCommand;

/**
 * Utility class to collect the string related methods needed across the
 * commands
 * 
 */
public class StringUtil {

	/**
	 * Utility method that splits a string into a list of strings using a specific
	 * separator string
	 * 
	 * @param value         - the original string
	 * @param itemSeparator - the separator to be used
	 * @return a list of substrings
	 */
	public static List<String> splitStringToList(String value, String itemSeparator) {
		return Arrays.asList(value.split(itemSeparator));
	}

	/**
	 * Takes a list of strings and creates a string that has all the elements of the
	 * list separated by the given separator.
	 * 
	 * @param resultList
	 * @param seperator
	 * @return
	 */
	public static String listToString(List<String> resultList, String seperator) {
		String result = "";
		for (int i = 0; i < resultList.size(); i++) {
			if (i > 0) {
				result += seperator;
			}
			result += resultList.get(i);
		}
		return result;
	}

	/**
	 * Utility method that checks if a string has a specific prefix
	 * 
	 * @param value  - the string to check
	 * @param prefix - the prefix to look for
	 * @return true if the string has the given prefix, false otherwise
	 */
	public static boolean hasPrefix(String value, String prefix) {
		return value.startsWith(prefix);
	}

	/**
	 * Utility method that removes a prefix from a given string
	 * 
	 * @param value  - the string
	 * @param prefix - the prefix to remove
	 * @return the string with the prefix removed. The original string if the prefix
	 *         was not found.
	 */
	public static String removePrefix(String value, String prefix) {
		return value.substring(prefix.length());
	}

	/**
	 * Removes Prefixes from strings in a list and return the list
	 * 
	 * @param values
	 * @param prefixExistingworkitem
	 * @return
	 */
	public static List<String> removePrefixes(List<String> values, String prefixExistingworkitem) {
		List<String> newValues = new ArrayList<String>(values.size());
		for (String value : values) {
			String newValue = value;
			if (value.startsWith(ExportWorkItemsCommand.PREFIX_EXISTINGWORKITEM)) {
				newValue = StringUtil.removePrefix(value, ExportWorkItemsCommand.PREFIX_EXISTINGWORKITEM);
			}
			newValues.add(newValue);
		}
		return newValues;
	}

	/**
	 * @param value
	 * @return true if null or empty (whitespace removed).
	 */
	public static boolean isEmpty(String value) {
		if (value == null) {
			return true;
		}
		return value.trim().isEmpty();
	}
}
