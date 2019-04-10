/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.parameter;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Maintains a list of parameters and provides the required access methods
 * 
 */
public class ParameterList implements Iterable<Parameter> {

	// Parameter list is stored as hash map
	private HashMap<String, Parameter> fParameterList = new HashMap<String, Parameter>();
	// a parameter identified as command
	private Parameter fCommand;

	public ParameterList() {
		super();
	}

	/**
	 * Adds a new parameter with name and value to the list.
	 * 
	 * @param name  - the name of the parameter
	 * @param value - the value of the parameter
	 */
	public void addParameterValue(String name, String value) {
		if (value != null) {
			value = value.trim();
		}
		fParameterList.put(name, Parameter.createParameterValue(name, value));

	}

	/**
	 * Adds a new command with name to the list.
	 * 
	 * @param name - the name of the command
	 */
	public void addCommand(String name) {
		Parameter command = Parameter.createCommand(name);
		this.fCommand = command;
		fParameterList.put(command.getName(), command);
	}

	/**
	 * Adds a parameter to the list
	 * 
	 * @param parameter - the parameter to be aded
	 */
	public void addParameter(Parameter parameter) {
		fParameterList.put(parameter.getName(), parameter);
	}

	/**
	 * Internally create a switch to add it to a parameter list.
	 * 
	 * @param switchName
	 * @param switchValue
	 */
	public void addSwitch(String switchName, String switchValue) {
		Parameter aSwitch = Parameter.createSwitch(switchName, switchValue);
		fParameterList.put(aSwitch.getName(), aSwitch);
	}

	/**
	 * Get the command parameter
	 * 
	 * @return parameter describing the command
	 */
	public Parameter getCommand() {
		return fCommand;
	}

	/**
	 * Tests if the parameter list contains a switch
	 * 
	 * @param name - the name of the switch
	 * @return true if the switch was found, false otherwise
	 */
	public boolean hasSwitch(String name) {
		Parameter param = fParameterList.get(name);
		return (param != null && param.isSwitch());
	}

	/**
	 * Get the parameterValue amd consume the parameter
	 * 
	 * @param name - the name of the parameter
	 * @return the value of the parameter if it exists, null otherwise
	 */
	public String consumeParameter(String name) {
		Parameter param = fParameterList.get(name);
		if (param != null) {
			param.setConsumed();
			return param.getValue();
		}
		return null;
	}

	/**
	 * Return a parameter with a given name
	 * 
	 * @param name - the name of the parameter
	 * @return the parameter that was found or null if none was found
	 */
	public Parameter getParameter(String name) {
		return fParameterList.get(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Parameter> iterator() {
		return fParameterList.values().iterator();
	}

	/**
	 * @return true if the parameter list is empty, false otherwise
	 */
	public boolean isEmpty() {
		return fParameterList.isEmpty();
	}

}
