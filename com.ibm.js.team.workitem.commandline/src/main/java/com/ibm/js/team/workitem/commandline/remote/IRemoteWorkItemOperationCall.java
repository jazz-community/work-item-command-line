/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.team.repository.common.TeamRepositoryException;

/**
 * The Interface to be used and implemented in RMI mode
 * 
 * The server has to implement the method. The client calls this method.
 * 
 * The result (e.g. error messages and other strings) is passed back
 * 
 */
public interface IRemoteWorkItemOperationCall extends Remote {

	public static final String LOCALHOST_REMOTE_WORKITEM_COMMANDLINE_SERVER = "//localhost/RemoteWorkitemCommandLineServer";
	public static final int RMI_REGISTRY_PORT = 1099;

	/**
	 * The main entry to call
	 * 
	 * @param args
	 * @return
	 * @throws TeamRepositoryException
	 * @throws RemoteException
	 */
	public abstract OperationResult runOperation(String[] args) throws TeamRepositoryException, RemoteException;
}