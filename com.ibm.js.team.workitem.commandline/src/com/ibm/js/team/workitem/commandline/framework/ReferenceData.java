/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.framework;

import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.registry.IEndPointDescriptor;

/**
 * Class to manage references and pass them through the hierarchy
 * 
 */
public class ReferenceData {

	private IEndPointDescriptor endpoint = null;
	private IReference reference = null;

	/**
	 * Create a reference
	 * 
	 * @param endPoint  the endpoint to be used
	 * @param reference the reference to the item
	 */
	public ReferenceData(IEndPointDescriptor endPoint, IReference reference) {
		this.endpoint = endPoint;
		this.reference = reference;
	}

	/**
	 * @return the endpoint descriptor
	 */
	public IEndPointDescriptor getEndPointDescriptor() {
		return this.endpoint;
	}

	/**
	 * @return the reference
	 */
	public IReference getReference() {
		return this.reference;
	}
}
