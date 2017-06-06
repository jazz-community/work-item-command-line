/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.ibm.js.team.workitem.commandline.commands.CreateWorkItemCommand;
import com.ibm.js.team.workitem.commandline.commands.ExportWorkItemsCommand;
import com.ibm.js.team.workitem.commandline.commands.ImportWorkItemsCommand;
import com.ibm.js.team.workitem.commandline.commands.MigrateWorkItemAttributeCommand;
import com.ibm.js.team.workitem.commandline.commands.PrintTypeAttributesCommand;
import com.ibm.js.team.workitem.commandline.commands.UpdateWorkItemCommand;
import com.ibm.js.team.workitem.commandline.framework.IWorkItemCommand;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.helper.WorkItemUpdateHelper;
import com.ibm.js.team.workitem.commandline.parameter.Parameter;
import com.ibm.js.team.workitem.commandline.parameter.ParameterIDMapper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.js.team.workitem.commandline.parameter.ParameterParser;
import com.ibm.js.team.workitem.commandline.remote.IRemoteWorkItemOperationCall;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;

/**
 * The Launcher Class. This class coordinates all the work It also provides the
 * method for the RMI remote interface
 * 
 */
public class WorkitemCommandLine extends UnicastRemoteObject implements
		IRemoteWorkItemOperationCall {

	/**
	 * Serialisation for RMI
	 */
	private static final long serialVersionUID = -3533140774804941834L;

	// The list of commands supported and their implementation
	private HashMap<String, IWorkItemCommand> supportedCommands = new HashMap<String, IWorkItemCommand>();

	/**
	 * Basic constructor
	 * 
	 * @throws RemoteException
	 */
	public WorkitemCommandLine() throws RemoteException {
		super();
	}

	// I need to know if I am running as a RMI server, so that I don't terminate
	// with System.exit()
	private static boolean isServer = false;

	/**
	 * Checks if I am running in server mode or not
	 * 
	 * @return true if I am in server mode, false otherwise
	 */
	public static boolean isServer() {
		return isServer;
	}

	/**
	 * To set server mode
	 * 
	 * @param value
	 *            - true, if in server mode
	 */
	private static void setServer(boolean value) {
		isServer = value;
	}

	/**
	 * Add the supported commands. If introducing a new command, add it here.
	 * 
	 * @param parameterManager
	 */
	private void addSupportedCommands(ParameterManager parameterManager) {
		addSupportedCommand(new PrintTypeAttributesCommand(
				new ParameterManager(parameterManager.getArguments())));
		addSupportedCommand(new CreateWorkItemCommand(new ParameterManager(
				parameterManager.getArguments())));
		addSupportedCommand(new UpdateWorkItemCommand(new ParameterManager(
				parameterManager.getArguments())));
		addSupportedCommand(new MigrateWorkItemAttributeCommand(
				new ParameterManager(parameterManager.getArguments())));
		addSupportedCommand(new ImportWorkItemsCommand(new ParameterManager(
				parameterManager.getArguments())));
		addSupportedCommand(new ExportWorkItemsCommand(new ParameterManager(
				parameterManager.getArguments())));
	}

	/**
	 * Hook to terminate gracefully
	 * 
	 */
	private static class TerminateRuntimeHook extends Thread {
		@Override
		public void run() {
			if (TeamPlatform.isStarted()) {
				// System.out.println("Shutting down Team Platform ...");
				TeamPlatform.shutdown();
			}
		}
	}

	// Add the hook
	private static final TerminateRuntimeHook hook;
	static {
		hook = new TerminateRuntimeHook();
		Runtime.getRuntime().addShutdownHook(hook);
	}

	/**
	 * @return the list of supported commands.
	 */
	private HashMap<String, IWorkItemCommand> getSupportedCommands() {
		return supportedCommands;
	}

	/**
	 * Add a command to the list.
	 * 
	 * @param command
	 *            - the command implementation
	 */
	private void addSupportedCommand(IWorkItemCommand command) {
		command.initialize();
		supportedCommands.put(command.getCommandName(), command);
	}

	/**
	 * Find a command from its name
	 * 
	 * @param commandName
	 *            - the name of the command
	 * 
	 * @return the command or null, if it was not found
	 */
	private IWorkItemCommand getSupportedCommand(String commandName) {
		return supportedCommands.get(commandName);
	}

	/**
	 * The main entry point into the work item commandline
	 * 
	 * @param args
	 *            - the arguments to be used by the commandline
	 * @throws RemoteException
	 */
	public static void main(String[] args) {

		OperationResult result = new OperationResult();
		System.out.println("WorkItemCommandLine Version "
				+ IWorkItemCommandLineConstants.VERSIONINFO + "\n");
		WorkitemCommandLine commandline;
		try {
			commandline = new WorkitemCommandLine();
			result = commandline.run(args);
		} catch (RemoteException e) {
			result.appendResultString("RemoteException: " + e.getMessage());
			result.appendResultString(e.getStackTrace().toString());
		}
		System.out.println(result.getResultString());
		if (TeamPlatform.isStarted()) {
			TeamPlatform.shutdown();
		}
		if (!isServer()) {
			// If I am not in server mode, I need to exit and return success or
			// failure
			if (result.isSuccess()) {
				// If the operation was unsuccessful, terminate with an error
				System.exit(0);
			}
			System.exit(1);
		}
	}

	/**
	 * This method is the first not static and coordinates to run the work item
	 * commandline.
	 * 
	 * It determines if we are called as RMI server or RMI client and if so,
	 * prepares the related workflow by calling the methods to setup and run the
	 * RMI server or client
	 * 
	 * If we are not running in RMI mode, the code to run this locally is used
	 * 
	 * @param args
	 * @return
	 */
	private OperationResult run(String[] args) {
		ParameterManager parameterManager = new ParameterManager(
				ParameterParser.parseParameters(args));
		if (parameterManager
				.hasSwitch(IWorkItemCommandLineConstants.SWITCH_RMISERVER)) {
			// Started as RMI server
			Parameter rmiInfo = parameterManager.getArguments().getParameter(
					IWorkItemCommandLineConstants.SWITCH_RMISERVER);
			return startRMIServer(rmiInfo);
		} else if (parameterManager
				.hasSwitch(IWorkItemCommandLineConstants.SWITCH_RMICLIENT)) {
			// Started as RMI client
			Parameter rmiInfo = parameterManager.getArguments().getParameter(
					IWorkItemCommandLineConstants.SWITCH_RMICLIENT);
			return runRMIClient(rmiInfo, args);
		}
		// direct call, we are not running in RMI mode
		return runCommands(parameterManager);
	}

	/**
	 * The interface if I am running in RMI Server mode. The client calls this
	 * method and passes all the arguments. The operation returns an @see
	 * {@link OperationResult} that contains the commandline output, as well as
	 * if the operation succeeded.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.remote.IRemoteWorkItemOperationCall#runOperation(java.lang.String[])
	 */
	@Override
	public OperationResult runOperation(String[] args)
			throws TeamRepositoryException, RemoteException {
		ParameterManager parameterManager = new ParameterManager(
				ParameterParser.parseParameters(args));
		return runCommands(parameterManager);
	}

	/**
	 * This method sets up the commands to be called. It tries to find a
	 * matching command for the input parameters If it does, it tries to
	 * validate if parameters required by that command are available. In case of
	 * problems, it prints the help message.
	 * 
	 * If there are no issues, try to run the operation and return the result.
	 * 
	 * Exceptions thrown are handled and the messages appended to the result.
	 * 
	 * @param parameterManager
	 *            - the parametermanager that contains the parsed arguments.
	 * @return
	 */
	private OperationResult runCommands(ParameterManager parameterManager) {
		IProgressMonitor monitor = new NullProgressMonitor();
		OperationResult result = new OperationResult();
		addSupportedCommands(parameterManager);
		String command = parameterManager.getCommand();
		// try to find a command
		if (null == command) {
			result.appendResultString("Error: Command not provided! ");
			result.appendResultString(this.helpGeneralUsage(null));
			return result;
		}
		// Try to get the command to be run
		IWorkItemCommand toRun = getSupportedCommand(command);
		if (toRun == null) {
			result.appendResultString("Error: Command not supported: "
					+ command);
			result.appendResultString(IWorkItemCommandLineConstants.RESULT_FAILED);
			result.appendResultString(this.helpGeneralUsage(null));
			return result;
		}
		// We found a valid command, initialize it
		// The command adds its required parameters to the list
		try {
			// Do we have all parameters?
			result.appendResultString("Executing command: " + command + "\n");
			toRun.validateRequiredParameters();
		} catch (WorkItemCommandLineException e) {
			result.appendResultString(e.getMessage());
			result.appendResultString(IWorkItemCommandLineConstants.RESULT_FAILED);
			result.appendResultString(helpGeneralUsage(toRun.getCommandName()));
			return result;
		}
		try {
			// run the command
			result.addOperationResult(toRun.execute(monitor));
			if (result.isSuccess()) {
				result.appendResultString(IWorkItemCommandLineConstants.RESULT_SUCCESS);
				return result;
			}
		} catch (WorkItemCommandLineException e) {
			result.appendResultString(e.getMessage());
		} catch (TeamRepositoryException e) {
			result.appendResultString("TeamRepositoryException while executing command!");
			result.appendResultString(e.getMessage());
		}
		result.appendResultString(IWorkItemCommandLineConstants.RESULT_FAILED);
		return result;
	}

	/**
	 * Operation to prepare and run the work item commandline as RMI server.
	 * This sets us in server mode, so the command will not terminate
	 * 
	 * @param rmiInfo
	 * @return
	 */
	private OperationResult startRMIServer(Parameter rmiInfo) {
		OperationResult result = new OperationResult();
		try {
			// use a RMI server address that was passed with the flag
			// Create a URI from it, to be able to get the port, if provided
			URI rmiServerURI = getRMIServerURI(rmiInfo);
			int port = rmiServerURI.getPort();
			if (port == -1) {
				// No port specified, use the default port
				port = RMI_REGISTRY_PORT;
			}
			// Create and install a security manager
			if (System.getSecurityManager() == null) {
				System.setSecurityManager(new RMISecurityManager());
				result.appendResultString("Security manager installed...");
			} else {
				result.appendResultString("Security manager already exists...");
			}

			try { // Create a local registry on a specific port
				LocateRegistry.createRegistry(port);
				result.appendResultString("Java RMI registry created...");
			} catch (RemoteException e) {
				// do nothing, error means registry already exists
				result.appendResultString("java RMI registry already exists...");
			}
			try {
				// Instantiate RmiServer
				IRemoteWorkItemOperationCall obj = new WorkitemCommandLine();

				// Get the URL to be used for lookup
				String lookupURL = rmiServerURI.getSchemeSpecificPart();
				// Bind this object instance to the name "RmiServer"
				Naming.rebind(lookupURL, obj);
				result.appendResultString("PeerServer bound in registry...");
				result.setSuccess();
				// I am running as a server don't terminate
				setServer(true);
				return result;
			} catch (Exception e) {
				result.appendResultString("RMI server exception: "
						+ e.getMessage());
				result.appendResultString(e.getStackTrace().toString());
			}
		} catch (URISyntaxException e) {
			result.appendResultString("Error converting to URI: "
					+ rmiInfo.getValue() + "\n" + e.getMessage());
		}
		// If we get there, something went wrong
		result.setFailed();
		return result;
	}

	/**
	 * We are running as RMI client.
	 * 
	 * Try to locate the RMI server and pass the call to it.
	 * 
	 * @param rmiInfo
	 * @param args
	 * @return
	 */
	private OperationResult runRMIClient(Parameter rmiInfo, String[] args) {
		OperationResult result = new OperationResult();
		try {
			// use a RMI server address that was passed with the flag
			// Create a URI from it, to be able to get the port, if provided
			URI url = getRMIServerURI(rmiInfo);
			// Create and install a security manager
			if (System.getSecurityManager() == null) {
				System.setSecurityManager(new RMISecurityManager());
			}
			try {
				// Get the URL to be used for lookup
				String lookupURL = url.getSchemeSpecificPart();
				// Lookup the remote operation
				IRemoteWorkItemOperationCall operation = (IRemoteWorkItemOperationCall) Naming
						.lookup(lookupURL);
				// run the operation
				return operation.runOperation(args);
			} catch (Exception e) {
				result.appendResultString("RmiOperationClient exception: "
						+ e.getMessage());
				e.printStackTrace();
				return result;
			}
		} catch (URISyntaxException e) {
			result.appendResultString("Error converting to URL: "
					+ rmiInfo.getValue() + "\n" + e.getMessage());
		}
		// If we get here, something went wrong
		result.setFailed();
		return result;
	}

	/**
	 * Compose a new URI for the RMI connection info. We have to fake it with a
	 * http protocol to make it work, but after that it is easy to work with the
	 * result.
	 * 
	 * @param rmiInfo
	 * @return
	 * @throws URISyntaxException
	 */
	private URI getRMIServerURI(Parameter rmiInfo) throws URISyntaxException {
		String rmiName = rmiInfo.getValue();
		if (rmiName == null || rmiName.equals("")) {
			rmiName = IWorkItemCommandLineConstants.HTTP_PROTOCOL_PREFIX
					+ IRemoteWorkItemOperationCall.LOCALHOST_REMOTE_WORKITEM_COMMANDLINE_SERVER;
		}
		return new URI(IWorkItemCommandLineConstants.HTTP_PROTOCOL_PREFIX
				+ rmiName);
	}

	/**
	 * Create the help information
	 * 
	 * @param theCommand
	 * @return
	 */
	private String helpGeneralUsage(String theCommand) {
		// Print a usage help page
		String commandName = theCommand;
		if (commandName == null || commandName == "") {
			commandName = "command";
		}
		String usage = "\n";
		usage += "\n\nUsage (See http://wp.me/p2DlHq-s9 for a more complete description) :";
		usage += "\n";
		usage += IWorkItemCommandLineConstants.PREFIX_COMMAND + commandName
				+ " {switch}" + " {parameter[:mode]=value}";
		usage += "\n";
		usage += "\nMultiple parameter/value pairs and switches can be provided separated by spaces.";
		usage += "\nCommands might require specific parameters to be mandatory.";
		usage += "\n";
		usage += "\nSwitches influence the behavior of the operation.";
		usage += "\nThe switch " + IWorkItemCommandLineConstants.PREFIX_SWITCH
				+ IWorkItemCommandLineConstants.SWITCH_IGNOREERRORS
				+ " ignores errors such as attributes or values not available.";
		usage += "\n\nAvailable commands:";
		Set<String> commands = getSupportedCommands().keySet();
		for (String command : commands) {
			IWorkItemCommand aCommand = getSupportedCommand(command);
			usage += "\n" + IWorkItemCommandLineConstants.PREFIX_COMMAND
					+ aCommand.getCommandName() + " " + aCommand.helpUsage();
		}
		usage += "\n\nStart in RMI server mode:";
		usage += "\n\tUse the switch "
				+ IWorkItemCommandLineConstants.PREFIX_SWITCH
				+ IWorkItemCommandLineConstants.SWITCH_RMISERVER
				+ " to start an instance as RMI server.";
		usage += "\n\tIn this mode, the process will not terminate, but wait for RMI requests to perform commands. ";
		usage += "\n\tIt will service commands requested by other client instances that are started with the ";
		usage += "\n\tadditional switch "
				+ IWorkItemCommandLineConstants.PREFIX_SWITCH
				+ IWorkItemCommandLineConstants.SWITCH_RMICLIENT + " .";
		usage += "\n\tIt is not necessary to provide a command or any other input values, when starting the server ";
		usage += "\n\tas they will be ignored.";
		usage += "\n\tSince the TeamPlatform needs to be initilized only once in this mode, the performance";
		usage += "\n\tis considerably increased for multiple subsequent client calls.";
		usage += "\n\tBy default, the RMI server uses the name "
				+ IRemoteWorkItemOperationCall.LOCALHOST_REMOTE_WORKITEM_COMMANDLINE_SERVER
				+ " on port " + IRemoteWorkItemOperationCall.RMI_REGISTRY_PORT
				+ ".";
		usage += "\n\tIt is possible to specify a different name and port by providing a value to the switch.";
		usage += "\n\tThe client command must be started with the same name and port as the server using the corresponding client switch";
		usage += "\n\tExample server: "
				+ IWorkItemCommandLineConstants.PREFIX_SWITCH
				+ IWorkItemCommandLineConstants.SWITCH_RMISERVER
				+ "=//clm.example.com:1199/WorkItemCommandLine";
		usage += "\n\tExample client: -create "
				+ IWorkItemCommandLineConstants.PREFIX_SWITCH
				+ IWorkItemCommandLineConstants.SWITCH_RMICLIENT
				+ "=//clm.example.com:1199/WorkItemCommandLine repository=<repositoryURL> user=<user> password=<pass> projectArea=<paName> workItemType=task summary=\"New Item\"";
		usage += "\n\tPlease note, that the server and the client require a policy file for the security manager.";
		usage += "\n\tA Policy file rmi_no.policy is shipped with the download. Rename and modify the file to your requirements";
		usage += "\n\tTo enable security Java requires to call the class with the additional vm argument -Djava.security.policy=no.policy where the policy file name must exist.";
		WorkItemUpdateHelper helper = new WorkItemUpdateHelper();
		usage += helper.helpGeneralUsage();
		usage += "\n\nAliases for attribute ID's:\n";
		usage += ParameterIDMapper.helpParameterMappings();
		return usage;
	}
}
