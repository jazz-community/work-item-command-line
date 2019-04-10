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
 * Tool to manage parameters for a command line. It gets passed a parameterlist
 * that has been parsed and provides access to their values. It allows to build
 * up a list of required parameters with syntax hints.
 * 
 */
public class ParameterManager {

	// The required parameters
	private ParameterList fRequiredParameters = new ParameterList();
	// the parsed parameters
	private ParameterList fParsedParameters = null;

	/**
	 * Constructor for the ParameterManager. Pass a parameter list parsed from the
	 * input arguments.
	 * 
	 * @param arguments
	 */
	public ParameterManager(ParameterList arguments) {
		super();
		this.fParsedParameters = arguments;
	}

	/**
	 * @return the required parameters
	 */
	private ParameterList getRequiredParameters() {
		return fRequiredParameters;
	}

	/**
	 * Delegate to create a required parameter
	 * 
	 * @param name    - Name of the parameter
	 * @param example - An example for the parameter usage
	 */
	public void syntaxAddRequiredParameter(String name, String example) {
		getRequiredParameters().addParameter(Parameter.createRequiredParameter(name, example));
	}

	/**
	 * Add a supported switch
	 * 
	 * @param name name of the switch
	 */
	public void syntaxAddSwitch(String name, String value) {
		getRequiredParameters().addParameter(Parameter.createSwitch(name, value));
	}

	/**
	 * Add a supported switch
	 * 
	 * @param name name of the switch
	 */
	public void syntaxAddSwitch(String name) {
		getRequiredParameters().addParameter(Parameter.createSwitch(name, null));
	}

	/**
	 * Delegate to search for a switch
	 * 
	 * @param name
	 * @return
	 */
	public boolean hasSwitch(String name) {
		return fParsedParameters.hasSwitch(name);
	}

	/**
	 * Delegate to find a parameter, return the value and to mark the parameter as
	 * consumed
	 * 
	 * @param name - name of the parameter
	 * 
	 * @return the value of the parameter or null
	 */
	public String consumeParameter(String name) {
		return fParsedParameters.consumeParameter(name);
	}

	/**
	 * Get the name of the command from the arguments
	 * 
	 * @return the name (may be "") or null, if no command was provided
	 */
	public String getCommand() {
		Parameter command = fParsedParameters.getCommand();
		if (command == null) {
			return null;
		}
		return command.getName();
	}

	/**
	 * Provide the argument list
	 * 
	 * @return the Parameters
	 */
	public ParameterList getArguments() {
		return fParsedParameters;
	}

	/**
	 * Validate if all the parameters required are available. If not throw an
	 * exception with a list of missing parameters.
	 * 
	 * @throws WorkItemCommandLineException with a description of what is missing
	 */
	public void validateRequiredParameters() throws WorkItemCommandLineException {
		ParameterList missingParameters = new ParameterList();
		ParameterList required = getRequiredParameters();
		for (Parameter requiredParameter : required) {
			if (requiredParameter.isRequired()) {
				if (fParsedParameters.getParameter(requiredParameter.getName()) == null && fParsedParameters
						.getParameter(ParameterIDMapper.getAlias(requiredParameter.getName())) == null) {
					missingParameters.addParameter(requiredParameter);
				}
			}
		}
		if (!missingParameters.isEmpty()) {
			String missing = getParameterHelp(missingParameters, true);
			throw new WorkItemCommandLineException("Missing required parameters:\n" + missing);
		}
	}

	/**
	 * Create a usage string with the required parameters
	 * 
	 * @return a string with a usage description
	 */
	public String helpUsageRequiredParameters() {
		return getParameterHelp(fRequiredParameters, false);
	}

	/**
	 * Create a help test for a given parameter list
	 * 
	 * @param parameters - the parameters to be printed
	 * @param detailled  - if true, the parameters come in one line each and have an
	 *                   example provided.
	 * @return a string with help info
	 */
	private String getParameterHelp(ParameterList parameters, boolean detailled) {
		String separator = detailled ? "\n" : " ";
		String linePrefix = detailled ? "\t Required: " : "";
		String missing = "";
		for (Parameter parameter : parameters) {
			String commandSwitchPrefix = "";
			String value = "";
			if (parameter.isSwitch()) {
				commandSwitchPrefix = IWorkItemCommandLineConstants.PREFIX_SWITCH;
			} else if (parameter.isCommand()) {
				commandSwitchPrefix = IWorkItemCommandLineConstants.PREFIX_COMMAND;
			} else {
				value = "=\"value\"";
			}
			missing += linePrefix + commandSwitchPrefix + parameter.getName() + value;
			if (detailled) {
				missing += " Example: " + commandSwitchPrefix + parameter.getName() + parameter.getExample();
			}
			missing += separator;
		}
		return missing;
	}
}
