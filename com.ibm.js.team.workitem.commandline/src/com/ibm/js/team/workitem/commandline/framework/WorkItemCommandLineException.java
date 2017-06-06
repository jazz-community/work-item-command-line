/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.framework;

/**
 * Exception used internally to wrap issues to throw and process them.
 * 
 */
public class WorkItemCommandLineException extends RuntimeException {

	Throwable ex = null;

	/**
	 * Base constructor
	 */
	public WorkItemCommandLineException() {
		super();
	}

	/**
	 * Just throw with a simple message.
	 * 
	 * @param message
	 */
	public WorkItemCommandLineException(String message) {
		super(message);
	}

	/**
	 * Just throw another throwable
	 * 
	 * @param throwable
	 */
	public WorkItemCommandLineException(Throwable throwable) {
		super(throwable);
	}

	/**
	 * Constructor to warp an exception into another one and provide both info.
	 * 
	 * @param message
	 * @param throwable
	 */
	public WorkItemCommandLineException(String message, Throwable throwable) {
		super(message, throwable);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 7933361626497401499L;

}
