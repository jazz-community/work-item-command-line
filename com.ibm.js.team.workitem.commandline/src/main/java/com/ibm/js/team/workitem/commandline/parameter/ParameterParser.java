/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.parameter;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;

/**
 * A parser to parse strings for arguments, commands and switches
 * 
 */
public class ParameterParser {

	// The parameter list
	private ParameterList parameterList = null;

	/**
	 * The constructor for the parser
	 */
	private ParameterParser() {
		super();
		this.parameterList = new ParameterList();
	}

	/**
	 * @return the internal parameter list
	 */
	private ParameterList getParameterList() {
		return parameterList;
	}

	/**
	 * Parse all arguments one after the other and put them into properties
	 * 
	 * @param args
	 * @return
	 */
	private ParameterList parse(String[] args) {
		this.parameterList = new ParameterList();
		for (int i = 0; i < args.length; i++) {
			parseParameter(args[i]);
		}
		return this.parameterList;
	}

	/**
	 * Create a parser and parse the arguments
	 * 
	 * @param args
	 *            - Array of arguments
	 * @return the parameter list with the parameters
	 */
	public static ParameterList parseParameters(String[] args) {
		ParameterParser parser = new ParameterParser();
		return parser.parse(args);
	}

	/**
	 * Parses an input string and tries to split it into a parameter name and
	 * value and store it in the hashmap
	 * 
	 * Accepts forms like -testparameter testparameter=value testparameter
	 * 
	 * Parameters of the form command and testparameter=value are split into
	 * parameter name "testparameter" and parameter value "value".
	 * 
	 * @param inputString
	 * @param argsResult
	 * @return
	 * @return true if parameter was created. the parameter name and value is
	 *         returned as a side effect in argsResult
	 * 
	 */
	private void parseParameter(String inputString) {
		if (null == inputString) {
			// nothing to do);
			return;
		}
		if (inputString.startsWith(IWorkItemCommandLineConstants.PREFIX_COMMAND)) {
			// Found a command remember it, strip of the prefix
			String foundCommand = inputString.substring(1);
			addCommand(foundCommand);
		} else if (inputString.startsWith(IWorkItemCommandLineConstants.PREFIX_SWITCH)) {
			// Found a switch, strip of the prefix and remember it
			// switch can have additional parameter value
			String foundSwitch = inputString.substring(1);
			String switchValue = null;
			int delimiter = inputString.indexOf(IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR);
			if (delimiter > 0) {
				// Strip of the switch prefix
				foundSwitch = inputString.substring(1, delimiter);
				if (inputString.length() >= delimiter + 1) {
					switchValue = inputString.substring(delimiter + 1);
				}
			}
			addSwitch(foundSwitch, switchValue);
		} else {
			// found parameter/value pair
			String parameterName = inputString; // default grab all
			String parameterValue = null;

			parameterValue = null;
			int delimiter = inputString.indexOf(IWorkItemCommandLineConstants.INFIX_PARAMETER_VALUE_SEPARATOR);
			// If we find a separator, get the first section as parameterName
			// and the rest as value
			if (delimiter > 0) {
				parameterName = inputString.substring(0, delimiter);
				if (inputString.length() >= delimiter + 1) {
					parameterValue = inputString.substring(delimiter + 1);
				}
			}
			addParameterValue(parameterName, parameterValue);
		}
		return;
	}

	/**
	 * Add a parameter with value
	 * 
	 * @param parameterName
	 *            - the name of the parameter
	 * @param parameterValue
	 *            - the value of the parameter
	 */
	private void addParameterValue(String parameterName, String parameterValue) {
		if (getParameterList().getParameter(parameterName) != null) {
			throw new WorkItemCommandLineException("Duplicate parameter: " + parameterName);
		}
		getParameterList().addParameterValue(parameterName, parameterValue);
	}

	/**
	 * Add a switch
	 * 
	 * @param name
	 *            - the name of the switch
	 */
	private void addSwitch(String name, String value) {
		if (getParameterList().getParameter(name) != null) {
			throw new WorkItemCommandLineException(
					"Duplicate switch: " + IWorkItemCommandLineConstants.PREFIX_SWITCH + name);
		}
		getParameterList().addParameter(Parameter.createSwitch(name, value));
	}

	/**
	 * Add a command
	 * 
	 * @param name
	 *            - the name of the command
	 */
	private void addCommand(String name) {
		if (getParameterList().getCommand() != null) {
			throw new WorkItemCommandLineException(
					"Ambiguous command parameter: " + IWorkItemCommandLineConstants.PREFIX_COMMAND + name);
		}
		getParameterList().addCommand(name);
	}
}
