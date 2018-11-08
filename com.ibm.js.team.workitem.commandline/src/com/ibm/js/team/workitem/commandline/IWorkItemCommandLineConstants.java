/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline;

/**
 * Some global constants that are required in several commands.
 * 
 */
public interface IWorkItemCommandLineConstants {

	public static final String VERSIONINFO = "V4.2";

	// Commands available
	public static final String COMMAND_CREATE = "create";
	public static final String COMMAND_UPDATE = "update";
	public static final String COMMAND_PRINT_TYPES = "printtypes";
	public static final String COMMAND_PRINT_TYPE_ATTRIBUTES = "printtypeattributes";
	public static final String COMMAND_IMPORT_WORKITEMS = "importworkitems";
	public static final String COMMAND_EXPORT_WORKITEMS = "exportworkitems";
	public static final String COMMAND_VALIDATE_OSLC_LINKS = "validateoslclinks";

	// Switch to ignore single errors and trying to perform operation even if
	// some parameters and values are not recognizable
	public static final String SWITCH_IGNOREERRORS = "ignoreErrors";

	// Switches to enable deletion of information
	public static final String SWITCH_ENABLE_DELETE_ATTACHMENTS = "enableDeleteAttachment";
	public static final String SWITCH_ENABLE_DELETE_APPROVALS = "enableDeleteApprovals";

	// Switches for Import
	// Display more detailed messages
	public static final String SWITCH_IMPORT_DEBUG = "importdebug";
	// Internal switch that suppresses detailed help
	public static final String SWITCH_BULK_OPERATION = "bulkupdate";

	// Switch to enable truncating string values to a size below the storage
	// limits
	public static final String SWITCH_ENFORCE_SIZE_LIMITS = "enforceSizeLimits";

	// Required parameters needed for all commands as well as the examples for
	// the help
	public static final String PARAMETER_REPOSITORY_URL_PROPERTY = "repository";
	public static final String PARAMETER_REPOSITORY_URL_PROPERTY_EXAMPLE = "\"https://clm.example.com:9443/ccm\"";
	public static final String PARAMETER_USER_ID_PROPERTY = "user";
	public static final String PARAMETER_USER_ID_PROPERTY_EXAMPLE = "user";
	public static final String PARAMETER_PASSWORD_PROPERTY = "password";
	public static final String PARAMETER_PASSWORD_PROPERTY_EXAMPLE = "password";

	// Special parameter constants needed by several commands
	public static final String PARAMETER_WORKITEM_ID_PROPERTY = "id";
	public static final String PROPERTY_WORKITEM_ID_PROPERTY_EXAMPLE = "123";

	// The project area
	public static final String PARAMETER_PROJECT_AREA_NAME_PROPERTY = "projectArea";
	public static final String PARAMETER_PROJECT_AREA_NAME_PROPERTY_EXAMPLE = "\"JKE Banking (Change Mangement)\"";

	// The work item type
	public static final String PARAMETER_WORKITEM_TYPE_PROPERTY = "workItemType";
	public static final String PARAMETER_WORKITEM_TYPE_PROPERTY_EXAMPLE = "defect";

	// The format for exporting and importing dates
	public static final String TIMESTAMP_EXPORT_IMPORT_FORMAT_MMM_D_YYYY_HH_MM_A = "MMM d, yyyy hh:mm a";

	// the mapping file
	public static final String PARAMETER_TIMESTAMP_ENCODING = "timestampFormat";
	public static final String PARAMETER_TIMESTAMP_ENCODING_EXAMPLE = "\"TIMESTAMP_EXPORT_IMPORT_FORMAT_MMM_D_YYYY_HH_MM_A\"";

	// Parsing
	public static final String INFIX_PARAMETER_VALUE_SEPARATOR = "=";
	public static final String PREFIX_COMMAND = "-";
	public static final String PREFIX_SWITCH = "/";

	// Result String
	public static final String RESULT_SUCCESS = "Success!";
	public static final String RESULT_FAILED = "Failed!";

	// Switches for RMI
	public static final String SWITCH_RMISERVER = "rmiServer";
	public static final String SWITCH_RMICLIENT = "rmiClient";

	// Import, Export constants

	// Prefix to create a legal URI
	public static final String HTTP_PROTOCOL_PREFIX = "http";

	// Import/Export work items
	public static final char DEFAULT_DELIMITER = ',';

	// The default encoding
	public static final String DEFAULT_ENCODING_UTF_16LE = "UTF-16LE";

	// The default Quote character
	public static final char DEFAULT_QUOTE_CHAR = '"';

	// parameter to specify the encoding
	public static final String PARAMETER_ENCODING = "encoding";
	public static final String PARAMETER_ENCODING_EXAMPLE = "\"UTF_16LE\"";

	// Parameter to specify the delimiter character
	public static final String PARAMETER_DELIMITER = "delimiter";
	public static final String PARAMETER_DELIMITER_EXAMPLE = "\",\"";

	public static final String SWITCH_EXPORT_SUPPRESS_ATTRIBUTE_EXCEPTIONS = "suppressAttributeExceptions";
	public static final String SWITCH_SUPPRESS_MAIL_NOTIFICATION = "skipEmailNotification";

}
