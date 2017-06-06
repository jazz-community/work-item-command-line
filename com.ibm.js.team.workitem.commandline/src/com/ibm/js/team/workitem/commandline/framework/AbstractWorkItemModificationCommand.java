/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.framework;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.js.team.workitem.commandline.helper.WorkItemUpdateHelper;
import com.ibm.js.team.workitem.commandline.parameter.Parameter;
import com.ibm.js.team.workitem.commandline.parameter.ParameterList;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.WorkItemOperation;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.internal.IAdditionalSaveParameters;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.model.ItemProfile;

/**
 * Abstract Command that can be used as base class to implement new commands.
 * The class provides all the required methods to run in the context. This class
 * provides a WorkItemOperation that is used to perform the modifications within
 * a transaction.
 * 
 * It adds a new method Update that performs the attribute update operation and
 * does the error handling.
 * 
 */
@SuppressWarnings("restriction")
public abstract class AbstractWorkItemModificationCommand extends
		AbstractTeamRepositoryCommand {

	public final static String UPDATE_BACK_LINKS=IAdditionalSaveParameters.UPDATE_BACKLINKS;
	// 6.0 iFix3
	// private final static String SKIP_MAIL = IAdditionalSaveParameters.SKIP_MAIL;
	public final static String SKIP_MAIL= "com.ibm.team.workitem.common.internal.skipMail"; //$NON-NLS-1$
	
	// in some modes we want to be able to ignore an error e.g. when setting
	// multiple attributes, we might want to try to not fail if one attribute
	// can not be set.
	private boolean fIgnoreErrors = false;
	// Since 6.0 iFix3 there is an additional save parameter to avoid sending 
	// E-Mail notification to users (e.g. during automated updates).
	private boolean suppressMailNotification=false;

	// Create Back Links E-Mail notification to users (e.g. during automated updates).
	private boolean createBackLinks=false;

	/**
	 * Set the flag
	 * 
	 * @param value
	 *            true or false
	 */
	public void setIgnoreErrors(boolean value) {
		this.fIgnoreErrors = value;
	}

	/**
	 * Check if we are in ignore error mode
	 * 
	 * @return true if we are in ignore error mode, false if not
	 */
	public boolean isIgnoreErrors() {
		return this.fIgnoreErrors;
	}

	/**
	 * Skip Email Notification since 6.0 iFix3
	 * @param hasSwitch
	 */
	public void setSuppressMailNotification(boolean hasSwitch) {
		this.suppressMailNotification = hasSwitch;	
	}

	/**
	 * is email notification suppression
	 * @return 
	 */
	public boolean isSuppressMailNotification() {
		return this.suppressMailNotification;	
	}

	
	/**
	 * The constructor that initializes the class.
	 * 
	 * @param parametermanager
	 *            Used to pass the parameters and manage required parameters.
	 */
	protected AbstractWorkItemModificationCommand(
			ParameterManager parametermanager) {
		super(parametermanager);
	}

	/**
	 * The @see com.ibm.team.workitem.client.WorkItemOperation that is used to
	 * perform the modifications.
	 * 
	 */
	protected class ModifyWorkItem extends WorkItemOperation {

		/**
		 * Constructor
		 * 
		 * @param The
		 *            title message for the operation
		 */
		public ModifyWorkItem(String message) {
			super(message, IWorkItem.FULL_PROFILE);
		}

		/**
		 * @param message
		 *            The title message for the operation
		 * @param profile
		 *            The item load profile to be used for the operation.
		 */
		public ModifyWorkItem(String message, ItemProfile<IWorkItem> profile) {
			super(message, profile);
		}

		/*
		 * This is run by the framework
		 * 
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.ibm.team.workitem.client.WorkItemOperation#execute(com.ibm.team
		 * .workitem.client.WorkItemWorkingCopy,
		 * org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		protected void execute(WorkItemWorkingCopy workingCopy,
				IProgressMonitor monitor) throws TeamRepositoryException,
				RuntimeException {
			// run the special method in the execute.
			// This is called by the framework.

			// If desired suppress e-mail notification 
			if(isSuppressMailNotification()){
				workingCopy.getAdditionalSaveParameters().add(SKIP_MAIL);
			}
			// If desired create backlinks - this can be also added to the workingcopy elsewhere 
			if(isCreateBackLinks()){
				workingCopy.getAdditionalSaveParameters().add(UPDATE_BACK_LINKS);
			}
			update(workingCopy);
		}
	}

	/**
	 * This operation does the main task of updating the work item
	 * 
	 * @param workingCopy
	 *            the workingcopy of the workitem to be updated.
	 * 
	 * @throws RuntimeException
	 * @throws TeamRepositoryException
	 */
	public void update(WorkItemWorkingCopy workingCopy)
			throws RuntimeException, TeamRepositoryException {

		ParameterList arguments = getParameterManager().getArguments();

		// We use a WorkItemHelper to do the real work
		WorkItemUpdateHelper workItemHelper = new WorkItemUpdateHelper(
				workingCopy, arguments, getMonitor());

		// Run through all properties not yet consumed and try to set the values
		// as provided
		for (Parameter parameter : arguments) {
			if (!(parameter.isConsumed() || parameter.isSwitch() || parameter
					.isCommand())) {
				// Get the property ID
				String propertyName = parameter.getName();
				// Get the property value
				String propertyValue = parameter.getValue();
				try {
					workItemHelper.updateProperty(propertyName, propertyValue);
				} catch (WorkItemCommandLineException e) {
					if (this.isIgnoreErrors()) {
						this.appendResultString("Exception! " + e.getMessage());
						this.appendResultString("Ignored....... ");
					} else {
						throw e;
					}
				} catch (RuntimeException e) {
					this.appendResultString("Runtime Exception: Property "
							+ propertyName + " Value " + propertyValue + " \n"
							+ e.getMessage());
					e.printStackTrace();
					throw e;
				} catch (IOException e) {
					this.appendResultString("IO Exception: Property "
							+ propertyName + " Value " + propertyValue + " \n"
							+ e.getMessage());
					throw new RuntimeException(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Enforce creation of back links - also set in {@link WorkItemUpdateHelper}
	 * @return
	 */
	public boolean isCreateBackLinks() {
		return createBackLinks;
	}

	/**
	 * Enforce creation of back links - also set in {@link WorkItemUpdateHelper}
	 * @param createBackLinks
	 */
	public void setCreateBackLinks(boolean createBackLinks) {
		this.createBackLinks = createBackLinks;
	}

	/**
	 * The @see com.ibm.team.workitem.client.WorkItemOperation that is used to
	 * change a work item type.
	 * 
	 */
	protected class ChangeType extends WorkItemOperation {

		private IWorkItemType fOldType;
		private IWorkItemType fNewType;

		/**
		 * Constructor
		 * 
		 * @param The
		 *            title message for the operation
		 */
		public ChangeType(String message, IWorkItemType oldType,
				IWorkItemType newType) {
			super(message, IWorkItem.FULL_PROFILE);
			this.fOldType = oldType;
			this.fNewType = newType;
		}

		/*
		 * This is run by the framework
		 * 
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.ibm.team.workitem.client.WorkItemOperation#execute(com.ibm.team
		 * .workitem.client.WorkItemWorkingCopy,
		 * org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		protected void execute(WorkItemWorkingCopy workingCopy,
				IProgressMonitor monitor) throws TeamRepositoryException,
				RuntimeException {
			
			// If desired suppress e-mail notification 
			if(isSuppressMailNotification()){
				workingCopy.getAdditionalSaveParameters().add(SKIP_MAIL);
			}
			// change the type
			getWorkItemCommon().updateWorkItemType(workingCopy.getWorkItem(),
					fNewType, fOldType, getMonitor());
		}
	}

}
