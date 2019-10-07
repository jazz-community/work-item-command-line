/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.js.team.workitem.commandline.IWorkItemCommandLineConstants;
import com.ibm.js.team.workitem.commandline.OperationResult;
import com.ibm.js.team.workitem.commandline.framework.AbstractTeamRepositoryCommand;
import com.ibm.js.team.workitem.commandline.framework.AbstractWorkItemModificationCommand;
import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.js.team.workitem.commandline.helper.WorkItemTypeHelper;
import com.ibm.js.team.workitem.commandline.parameter.ParameterManager;
import com.ibm.js.team.workitem.commandline.utils.ProcessAreaUtil;
import com.ibm.js.team.workitem.commandline.utils.WorkItemUtil;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IQueryClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.WorkItemOperation;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.expression.AttributeExpression;
import com.ibm.team.workitem.common.expression.Expression;
import com.ibm.team.workitem.common.expression.IQueryableAttribute;
import com.ibm.team.workitem.common.expression.QueryableAttributes;
import com.ibm.team.workitem.common.expression.Term;
import com.ibm.team.workitem.common.model.AttributeOperation;
import com.ibm.team.workitem.common.model.AttributeTypes;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.ItemProfile;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResult;

/**
 * Command to migrate a work item with a work item attribute of type string that
 * contains a list of enumeration literal as used up to RTC 3.x to the itemLists
 * available in RTC 4.x
 * 
 */
public class MigrateWorkItemAttributeCommand extends AbstractTeamRepositoryCommand {

	public static final String COMMAND_MIGRATE_ENUMERATION_LIST_ATTRIBUTE = "migrateattribute";
	public static final String PARAMETER_SOURCE_ATTRIBUTE_ID = "sourceAttributeID";
	public static final String PARAMETER_SOURCE_ATTRIBUTE_ID_EXAMPLE = "com.acme.custom.enum.multiselect";
	public static final String PARAMETER_TARGET_ATTRIBUTE_ID = "targetAttributeID";
	public static final String PARAMETER_TARGET_ATTRIBUTE_ID_EXAMPLE = "com.acme.custom.enum.list";

	public static final String SEPARATOR_ENUMERATION_LITERAL_ID_LIST = ",";
	// Since 6.0 iFix3 there is an additional save parameter to avoid sending
	// E-Mail notification to users (e.g. during automated updates).

	private boolean fSuppressMailNotification = false;

	public void setSuppressMailNotification(boolean hasSwitch) {
		fSuppressMailNotification = hasSwitch;
	}

	private boolean isSuppressMailNotification() {
		return fSuppressMailNotification;
	}

	private boolean fIgnoreErrors = false;

	private void setIgnoreErrors() {
		fIgnoreErrors = true;
	}

	private boolean isIgnoreErrors() {
		return fIgnoreErrors;
	}

	public MigrateWorkItemAttributeCommand(ParameterManager parametermanager) {
		super(parametermanager);
	}

	/***
	 * Add required parameters
	 * 
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractTeamRepositoryCommand#setRequiredParameters()
	 */
	@Override
	public void setRequiredParameters() {
		super.setRequiredParameters();
		// Copied from CreateWorkItemCommand
		getParameterManager().syntaxAddRequiredParameter(
				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddRequiredParameter(IWorkItemCommandLineConstants.PARAMETER_WORKITEM_TYPE_PROPERTY,
				IWorkItemCommandLineConstants.PARAMETER_WORKITEM_TYPE_PROPERTY_EXAMPLE);
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.SWITCH_IGNOREERRORS);
		getParameterManager().syntaxAddSwitch(IWorkItemCommandLineConstants.SWITCH_SUPPRESS_MAIL_NOTIFICATION);
		getParameterManager().syntaxAddRequiredParameter(PARAMETER_SOURCE_ATTRIBUTE_ID,
				PARAMETER_SOURCE_ATTRIBUTE_ID_EXAMPLE);
		getParameterManager().syntaxAddRequiredParameter(PARAMETER_TARGET_ATTRIBUTE_ID,
				PARAMETER_TARGET_ATTRIBUTE_ID_EXAMPLE);
	}

	/***
	 * Return the command
	 * 
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.IWorkItemCommand#getCommandName()
	 */
	@Override
	public String getCommandName() {
		return COMMAND_MIGRATE_ENUMERATION_LIST_ATTRIBUTE;
	}

	/***
	 * Perform the command
	 * 
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.js.team.workitem.commandline.framework.AbstractCommand#process()
	 */
	@Override
	public OperationResult process() throws TeamRepositoryException {
		// From CreateWorkItemCommand
		// Get the parameters such as project area name and Attribute Type and
		// run the operation
		String projectAreaName = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_PROJECT_AREA_NAME_PROPERTY).trim();
		// Find the project area
		IProjectArea projectArea = ProcessAreaUtil.findProjectAreaByFQN(projectAreaName, getProcessClientService(),
				getMonitor());
		if (projectArea == null) {
			throw new WorkItemCommandLineException("Project Area not found: " + projectAreaName);
		}

		String workItemTypeID = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_WORKITEM_TYPE_PROPERTY).trim();
		// Find the work item type
		IWorkItemType workItemType = WorkItemTypeHelper.findWorkItemType(workItemTypeID, projectArea.getProjectArea(),
				getWorkItemCommon(), getMonitor());

		// Get the parameter values - The source attribute
		String sourceAttributeID = getParameterManager().consumeParameter(PARAMETER_SOURCE_ATTRIBUTE_ID).trim();
		// check if old attribute ID is string type
		IAttribute sourceIAttribute = getWorkItemCommon().findAttribute(projectArea, sourceAttributeID, getMonitor());
		if (sourceIAttribute == null) {
			throw new WorkItemCommandLineException("Source Attribute not found: " + sourceAttributeID);
		}
		if (!AttributeTypes.STRING_TYPES.contains(sourceIAttribute.getAttributeType())) {
			throw new WorkItemCommandLineException("Source Attribute is not a String type: " + sourceAttributeID);
		}

		// Get the parameter values - The target attribute
		String targetAttributeID = getParameterManager().consumeParameter(PARAMETER_TARGET_ATTRIBUTE_ID).trim();
		// check if new attribute ID is EnumerationList
		IAttribute targetIAttribute = getWorkItemCommon().findAttribute(projectArea, targetAttributeID, getMonitor());
		if (targetIAttribute == null) {
			throw new WorkItemCommandLineException("Target Attribute not found: " + targetAttributeID);
		}
		if (!AttributeTypes.isEnumerationListAttributeType(targetIAttribute.getAttributeType())) {
			throw new WorkItemCommandLineException("Target Attribute is not an EnumerationList: " + targetAttributeID);
		}
		// Since 6.0 iFix3 there is an additional save parameter to avoid sending
		// E-Mail notification to users (e.g. during automated updates).
		this.setSuppressMailNotification(
				getParameterManager().hasSwitch(IWorkItemCommandLineConstants.SWITCH_SUPPRESS_MAIL_NOTIFICATION));

		if (getParameterManager().hasSwitch(IWorkItemCommandLineConstants.SWITCH_IGNOREERRORS)) {
			setIgnoreErrors();
		}
		String wiID = getParameterManager()
				.consumeParameter(IWorkItemCommandLineConstants.PARAMETER_WORKITEM_ID_PROPERTY);
		if (wiID != null) {
			IWorkItem wi = WorkItemUtil.findWorkItemByID(wiID, IWorkItem.SMALL_PROFILE, getWorkItemCommon(),
					getMonitor());
			if (!wi.getWorkItemType().equals(workItemType.getIdentifier())) {
				throw new WorkItemCommandLineException("Work item type mismatch: " + workItemType.getIdentifier()
						+ " specified " + workItemType.getIdentifier());
			}
			migrateSingleWorkItem(wi, sourceIAttribute, targetIAttribute);
		} else {
			// Update all work items of this type.
			migrateAllWorkItems(projectArea, workItemType, sourceIAttribute, targetIAttribute);
		}
		// If we got here, we succeeded
		getResult().setSuccess();
		return getResult();
	}

	/**
	 * Migrate one specific work item - for testing
	 * 
	 * @param wi
	 * @param sourceIAttribute
	 * @param targetIAttribute
	 * @throws TeamRepositoryException
	 */
	private void migrateSingleWorkItem(IWorkItem wi, IAttribute sourceIAttribute, IAttribute targetIAttribute)
			throws TeamRepositoryException {
		MigrateWorkItem operation = new MigrateWorkItem("Migrate", IWorkItem.FULL_PROFILE, sourceIAttribute,
				targetIAttribute);
		performMigration((IWorkItemHandle) wi.getItemHandle(), operation);
	}

	/**
	 * Migrate all work items of a specific type in a project area
	 * 
	 * @param projectArea
	 * @param workItemType
	 * @param sourceIAttribute
	 * @param targetIAttribute
	 * @throws TeamRepositoryException
	 */
	private void migrateAllWorkItems(IProjectArea projectArea, IWorkItemType workItemType, IAttribute sourceIAttribute,
			IAttribute targetIAttribute) throws TeamRepositoryException {
		// Find all work items of this type.
		// Create an Expression to find them
		IQueryableAttribute attribute = QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE).findAttribute(projectArea,
				IWorkItem.PROJECT_AREA_PROPERTY, getAuditableCommon(), getMonitor());
		IQueryableAttribute type = QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE).findAttribute(projectArea,
				IWorkItem.TYPE_PROPERTY, getAuditableCommon(), getMonitor());
		Expression inProjectArea = new AttributeExpression(attribute, AttributeOperation.EQUALS, projectArea);
		Expression isType = new AttributeExpression(type, AttributeOperation.EQUALS, workItemType.getIdentifier());
		Term typeinProjectArea = new Term(Term.Operator.AND);
		typeinProjectArea.add(inProjectArea);
		typeinProjectArea.add(isType);

		// Run the Expression
		IQueryClient queryClient = getWorkItemClient().getQueryClient();
		IQueryResult<IResult> results = queryClient.getExpressionResults(projectArea, typeinProjectArea);
		// Override the result set limit so that we get more than 1000 items if
		// there are more
		results.setLimit(Integer.MAX_VALUE);
		MigrateWorkItem operation = new MigrateWorkItem("Migrate", IWorkItem.FULL_PROFILE, sourceIAttribute,
				targetIAttribute);
		// Run the operation for each result
		while (results.hasNext(getMonitor())) {
			IResult result = (IResult) results.next(getMonitor());
			performMigration((IWorkItemHandle) result.getItem(), operation);
		}
	}

	/**
	 * Perform the update and handle errors
	 * 
	 * @param handle
	 * @param operation
	 * @throws WorkItemCommandLineException
	 */
	private void performMigration(IWorkItemHandle handle, MigrateWorkItem operation)
			throws WorkItemCommandLineException {
		String workItemID = "undefined";
		try {
			IWorkItem workItem = WorkItemUtil.resolveWorkItem((IWorkItemHandle) handle, IWorkItem.SMALL_PROFILE,
					getWorkItemCommon(), getMonitor());
			workItemID = getWorkItemIDString(workItem);
			operation.run(handle, getMonitor());
			getResult().appendResultString("Migrated work item " + workItemID + ".");
		} catch (TeamRepositoryException e) {
			throw new WorkItemCommandLineException(getResult().getResultString() + "TeamRepositoryException: Work item "
					+ workItemID + " attribute not migrated. " + e.getMessage(), e);
		} catch (WorkItemCommandLineException e) {
			String message = "WorkItemCommandLineException Work item " + workItemID + " attribute not migrated. "
					+ e.getMessage();
			if (!isIgnoreErrors()) {
				throw new WorkItemCommandLineException(getResult().getResultString() + message, e);
			} else {
				getResult().appendResultString(message);
			}
		}
	}

	/**
	 * We need this client libraries to run queries
	 * 
	 * @return
	 */
	private IWorkItemClient getWorkItemClient() {
		return (IWorkItemClient) getTeamRepository().getClientLibrary(IWorkItemClient.class);
	}

	/**
	 * Get the work item ID as string
	 * 
	 * @param workItem
	 * @return
	 */
	private String getWorkItemIDString(IWorkItem workItem) {
		return new Integer(workItem.getId()).toString();
	}

	/**
	 * The @see com.ibm.team.workitem.client.WorkItemOperation that is used to
	 * perform the modifications.
	 * 
	 */
	private class MigrateWorkItem extends WorkItemOperation {

		IAttribute fsourceAttribute = null;
		IAttribute fTargetAttribute = null;

		/**
		 * Constructor
		 * 
		 * @param The             title message for the operation
		 * @param message
		 * @param profile
		 * @param sourceAttribute
		 * @param targetAttribute
		 */
		public MigrateWorkItem(String message, ItemProfile<?> profile, IAttribute sourceAttribute,
				IAttribute targetAttribute) {
			super(message, profile);
			fsourceAttribute = sourceAttribute;
			fTargetAttribute = targetAttribute;
		}

		/***
		 * This gets called if run() is called
		 * 
		 * @see com.ibm.team.workitem.client.WorkItemOperation#execute(com.ibm.team
		 *      .workitem.client.WorkItemWorkingCopy,
		 *      org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		protected void execute(WorkItemWorkingCopy workingCopy, IProgressMonitor monitor)
				throws TeamRepositoryException, RuntimeException {

			// If desired suppress e-mail notification
			if (isSuppressMailNotification()) {
				workingCopy.getAdditionalSaveParameters().add(AbstractWorkItemModificationCommand.SKIP_MAIL);
			}

			IWorkItem workItem = workingCopy.getWorkItem();
			String thisItemID = getWorkItemIDString(workItem);
			if (!workItem.hasAttribute(fsourceAttribute)) {
				throw new WorkItemCommandLineException(
						"Work Item " + thisItemID + " Source Attribute not available - Synchronize Attributes: "
								+ fsourceAttribute.getIdentifier());
			}
			if (!workItem.hasAttribute(fTargetAttribute)) {
				throw new WorkItemCommandLineException(
						"Work Item " + thisItemID + " Target Attribute not available - Synchronize Attributes: "
								+ fTargetAttribute.getIdentifier());
			}
			// get the old value - a string with literals separated by a comma
			Object ovalue = workItem.getValue(fsourceAttribute);
			// compute the result values
			String sourceValues = "";
			if (null != ovalue && ovalue instanceof String) {
				sourceValues = (String) ovalue;
			}
			if (!sourceValues.equals("")) {
				String[] values = sourceValues.split(SEPARATOR_ENUMERATION_LITERAL_ID_LIST);
				IEnumeration<? extends ILiteral> enumeration = getWorkItemCommon().resolveEnumeration(fTargetAttribute,
						monitor);

				List<Object> results = new ArrayList<Object>();
				for (String literalID : values) {
					if (literalID == "") {
						// Nothing to do
						continue;
					}
					Identifier<? extends ILiteral> literal = getLiteralEqualsIDString(enumeration, literalID);
					if (null == literal) {
						throw new WorkItemCommandLineException(
								"Work Item " + thisItemID + " Target literal ID not available: " + literalID
										+ " Attribute " + fTargetAttribute.getIdentifier());
					}
					results.add(literal);
				}
				// Set the value
				workItem.setValue(fTargetAttribute, results);
			}
			getResult().appendResultString("Migrated work item " + thisItemID);
		}

		/**
		 * Gets an enumeration literal for an attribute that has the specific literal
		 * ID.
		 * 
		 * @param enumeration     - the enumeration to look for
		 * @param literalIDString - the literal ID name to look for
		 * @return the literal or null
		 * @throws TeamRepositoryException
		 */
		private Identifier<? extends ILiteral> getLiteralEqualsIDString(
				final IEnumeration<? extends ILiteral> enumeration, String literalIDString)
				throws TeamRepositoryException {
			List<? extends ILiteral> literals = enumeration.getEnumerationLiterals();
			for (Iterator<? extends ILiteral> iterator = literals.iterator(); iterator.hasNext();) {
				ILiteral iLiteral = (ILiteral) iterator.next();
				if (iLiteral.getIdentifier2().getStringIdentifier().equals(literalIDString.trim())) {
					return iLiteral.getIdentifier2();
				}
			}
			return null;
		}
	}

	@Override
	public String helpSpecificUsage() {
		return "";
	}
}
