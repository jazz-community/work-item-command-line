/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.parameter;

/**
 * Class that holds parameter descriptions to help processing commands and
 * printing the help
 * 
 */
public class Parameter {
	// The parameter name
	private String fName = null;
	// The parameter value
	private String fValue = null;
	// An example value for the parameter
	private String fExample = null;
	// If the parameter is required or optional
	private boolean fRequired = false;
	// If the parameter is a switch
	private boolean isSwitch = false;
	// If the parameter is a command
	private boolean isCommand = false;
	// used to mark parameters as consumed - this allows to avoid picking up
	// parameters already used in sub commands
	private boolean isConsumed = false;

	/**
	 * The constructor is hidden, use the static methods provided. Create a new
	 * parameter with its properties.
	 * 
	 * @param name
	 *            - the name of the parameter
	 * @param value
	 *            - the value of the parameter
	 * @param required
	 *            - true if the parameter is required
	 * @param isSwitch
	 *            - true if the parameter is a switch
	 * @param isCommand
	 *            - true if the parameter is a command
	 * @param example
	 *            - An example for the parameter value format
	 */
	private Parameter(String name, String value, boolean required, boolean isSwitch, boolean isCommand,
			String example) {
		super();
		this.fName = name;
		this.fExample = (null == example) ? "" : "=" + example;
		this.fRequired = required;
		this.isSwitch = isSwitch;
		this.isCommand = isCommand;
		this.fValue = value;
		// Test consistency?
	}

	/**
	 * The constructor is hidden, use the static methods provided. Create a new
	 * parameter with its properties. the parameter is not a switch, command,
	 * and not required
	 * 
	 * @param name
	 *            - the name of the parameter
	 * @param value
	 *            - the value of the parameter
	 */
	private Parameter(String name, String value) {
		super();
		this.fName = name;
		this.fExample = null;
		this.fRequired = false;
		this.isSwitch = false;
		this.isCommand = false;
		this.fValue = value;
	}

	/**
	 * Get the parameter name
	 * 
	 * @return
	 */
	public String getName() {
		return fName;
	}

	/**
	 * Get an example for the calue
	 * 
	 * @return
	 */
	public String getExample() {
		return fExample;
	}

	/**
	 * Get the value
	 * 
	 * @return
	 */
	public String getValue() {
		return fValue;
	}

	/**
	 * Set the vslue
	 * 
	 * @param value
	 */
	public void setValue(String value) {
		fValue = value;
	}

	/**
	 * Is the parameter required or not?
	 * 
	 * @return true if the parameter is required
	 */
	public boolean isRequired() {
		return fRequired;
	}

	/**
	 * Is this a switch rather than a parameter?
	 * 
	 * @return
	 */
	public boolean isSwitch() {
		return isSwitch;
	}

	/**
	 * Is this a command?
	 * 
	 * @return
	 */
	public boolean isCommand() {
		return isCommand;
	}

	/**
	 * Test if a parameter has been consumed
	 * 
	 * @return - true if the parameter has already been consumed
	 */
	public boolean isConsumed() {
		return isConsumed;
	}

	/**
	 * Set the parameter consumed
	 */
	public void setConsumed() {
		isConsumed = true;
	}

	/**
	 * Create a required parameter with name and example value
	 * 
	 * @param name
	 *            - the parameter name
	 * @param example
	 *            - an example for the parameter value
	 * @return the parameter created
	 */
	public static Parameter createRequiredParameter(String name, String example) {
		return new Parameter(name, null, true, false, false, example);
	}

	/**
	 * Create a parameter with name and value
	 * 
	 * @param name
	 *            - the parameter name
	 * @param value
	 *            - an example for the parameter value
	 * @return the parameter created
	 */
	public static Parameter createParameterValue(String name, String value) {
		Parameter parameter = new Parameter(name, value, false, false, false, null);
		parameter.setValue(value);
		return parameter;
	}

	/**
	 * Create a command parameter
	 * 
	 * @param name
	 *            the name of the command
	 * @return the parameter created
	 */
	public static Parameter createCommand(String name) {
		return new Parameter(name, null, true, false, true, null);
	}

	/**
	 * Create a switch
	 * 
	 * @param name
	 *            - the name of the switch
	 * @return the parameter created
	 */
	public static Parameter createSwitch(String name, String value) {
		return new Parameter(name, value, false, true, false, null);
	}

}
