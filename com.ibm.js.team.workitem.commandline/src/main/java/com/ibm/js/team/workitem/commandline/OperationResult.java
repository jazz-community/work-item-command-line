/*******************************************************************************
 * Copyright (c) 2015-2023 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline;

import java.io.Serializable;

/**
 * Manages the result of an operation across RMI calls
 * 
 */
public class OperationResult implements Serializable {
	/**
	 * Serialisation for RMI
	 */
	private static final long serialVersionUID = -3508263528723690071L;

	private String resultMessage = "";
	private boolean result = false;

	/**
	 * Basic constructor
	 */
	public OperationResult() {
		super();
		this.resultMessage = "";
		this.result = false;
	}

	/**
	 * Add an operation result to an existing one and owerwrite the result
	 * 
	 * @param result
	 */
	public void addOperationResult(OperationResult result) {
		appendResultString(result.getResultString());
		this.result = result.isSuccess();
	}

	/**
	 * @return the result string
	 */
	public String getResultString() {
		return resultMessage;
	}

	/**
	 * Append a line to an existing result
	 * 
	 * @param value
	 */
	public void appendResultString(String value) {
		if (!WorkitemCommandLine.isServer()) {
			System.out.println(value);
		} else {
			this.resultMessage = this.resultMessage.concat(value + "\n");
		}
	}

	/**
	 * @return if the result is successful or not
	 */
	public boolean isSuccess() {
		return result;
	}

	/**
	 * Set the result to success
	 */
	public void setSuccess() {
		this.result = true;
	}

	/**
	 * Set the result to failed
	 */
	public void setFailed() {
		this.result = false;
	}
}