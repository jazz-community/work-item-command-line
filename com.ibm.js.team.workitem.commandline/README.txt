

WorkItemCommandLine Version 5.1

Usage (See https://rsjazz.wordpress.com/2019/07/03/work-item-command-line-5-0/ for a more complete description) :
-command {switch} {parameter[:mode]=value}

Multiple parameter/value pairs and switches can be provided separated by spaces.
Commands might require specific parameters to be mandatory.

Switches influence the behavior of the operation.
The switch /ignoreErrors ignores errors such as attributes or values not available.

Available commands:
-migrateattribute sourceAttributeID="value" password="value" /skipEmailNotification projectArea="value" targetAttributeID="value" workItemType="value" repository="value" /ignoreErrors user="value"  
-validateoslclinks password="value" /trace /debug projectArea="value" query="value" repository="value" user="value"  
-bulkupdate password="value" /skipEmailNotification projectArea="value" query="value" /enableDeleteApprovals repository="value" /ignoreErrors user="value" /enableDeleteAttachment   [querysource="JKE Banking(Change Management),JKE Banking(Change Management)/Business Recovery Matters"]
-exportworkitems exportFile="value" password="value" /headerIDs /suppressAttributeExceptions projectArea="value" query="value" /asrtceclipse /disableAttachmentExport /allColumns repository="value" /ignoreErrors user="value"   [encoding="UTF-8"] [delimiter=","] [columns="Type,Id,Planned For,Filed Against,Description,Found In"] [querysource="JKE Banking(Change Management),JKE Banking(Change Management)/Business Recovery Matters"][timestampFormat="MMM d, yyyy hh:mm a"]
-importworkitems /importdebug password="value" /skipEmailNotification projectArea="value" /importmultipass /ignoreemptycolumnvalues importFile="value" /forcelinkcreation /enforceSizeLimits repository="value" /ignoreErrors user="value"   [mappingFile="C:\temp\mapping.xml"] [encoding="UTF-8"][timestampFormat="MMM d, yyyy hh:mm a"] [delimiter=","]
-create password="value" projectArea="value" workItemType="value" /enableDeleteApprovals /enforceSizeLimits repository="value" /ignoreErrors user="value" /enableDeleteAttachment  {parameter[:mode]=value}
-update password="value" /skipEmailNotification /ignoreMissingAttributes /enableDeleteApprovals id="value" /enforceSizeLimits repository="value" /ignoreErrors user="value" /enableDeleteAttachment  {parameter[:mode]=value}
-printtypeattributes password="value" projectArea="value" workItemType="value" repository="value" user="value"  
-printworkitem password="value" /suppressAttributeExceptions /asrtceclipse /attributeNamesAsIDs /allColumns id="value" repository="value" /ignoreErrors user="value"  [columns="Type,Id,Planned For,Filed Against,Description,Found In"] [timestampFormat="MMM d, yyyy hh:mm a"] [attachmentFolder="C:\temp\export"]
-printtypes password="value" projectArea="value" repository="value" user="value"  

Start in RMI server mode:
	Use the switch /rmiServer to start an instance as RMI server.
	In this mode, the process will not terminate, but wait for RMI requests to perform commands. 
	It will service commands requested by other client instances that are started with the 
	additional switch /rmiClient .
	It is not necessary to provide a command or any other input values, when starting the server 
	as they will be ignored.
	Since the TeamPlatform needs to be initilized only once in this mode, the performance
	is considerably increased for multiple subsequent client calls.
	By default, the RMI server uses the name //localhost/RemoteWorkitemCommandLineServer on port 1099.
	It is possible to specify a different name and port by providing a value to the switch.
	The client command must be started with the same name and port as the server using the corresponding client switch
	Example server: /rmiServer=//clm.example.com:1199/WorkItemCommandLine
	Example client: -create /rmiClient=//clm.example.com:1199/WorkItemCommandLine repository=<repositoryURL> user=<user> password=<pass> projectArea=<paName> workItemType=task summary="New Item"
	Please note, that the server and the client require a policy file for the security manager.
	A Policy file rmi_no.policy is shipped with the download. Rename and modify the file to your requirements
	To enable security Java requires to call the class with the additional vm argument -Djava.security.policy=no.policy where the policy file name must exist.

WorkItem attribute parameter and value examples:

Format for parameter is:
	parameter[:mode]=value

No spaces are allowed between parameter, value and the =.
Parameter and value can also not have spaces. Use " to enclose values with spaces. Example: "A Space"

Parameters:
Parameter is a work item attribute ID and value is a value or a list of values.
Use the command -printtypeattributes to retrieve the available attribute ID's, or
inspect the process configuration of your project area to extract the attribute ID's.

Values:
The values are specified by a string. This is can be display name of that value (enumerations)
or composed of display values of the path to this item (category, iterations, process areas).
For other attributes, such as users, work item types or work items, use the ID.

	Examples
	- For enumeration based attributes use the display value for the enumeration literal:
		internalPriority=High
	- For HTML and string based attributes use a string.
	  HTML types like summary, description, comment and HTML support the syntax below.
		description="Plain text<br/><b>bold text</b><br/><i>italic text</i><br/><a href="https://rsjazz.wordpress.com">External RSJazz Link</a><br/>User link to <b>@ralph </b><br/>Work Item link to Defect 3 <br/>"
	- For Wiki and multi line text attributes use <br> or \n for line breaks and check the syntax in the wiki editor.
		custom.wiki="<br>=Heading1<br><br>Plain text\n==Heading 2\n\nNormal Text **bold text** <br>**bold text**<br>//Italics//"
	- For work item type, owner and some other attributes use the object ID.
		workItemType=task
		owner=tanuj
	- Use the display name for simple attributes or the path composed out of the display names for hierarchical attributes.
		category=JKE/BRN
		foundIn="Sprint 2 Development"
		target="Main Development/Release 1.0/Sprint 3"
		custom.process.area="JKE Banking (Change Management)/Release Engineering"
	- Dates and Timestamps have to be specified in the Java SimpleDateFormat notation. The value "unassigned" can be used to delete the date.
		dueDate="2015/02/01 12:30:00 GMT+01:00"
		dueDate="unassigned"
	- Duration values are specified in milliseconds, or a hours minutes format.
		duration=1800000 correctedEstimate=3600000 timeSpent=60000
		duration="1 hour" correctedEstimate="2 hours 30 minutes" timeSpent="30 minutes"

WorkItem attribute values of <item type>List with a specified item type such as userList.
Format is using the separator , :
	"value1,value2,...,valueN"
	Example: custom.user.list:add="deb,al,...,tanuj"

WorkItem attributes with an general attribute value such as Item or itemList require encoding to locate the items.
Format is:
	custom.item.list=value

	Where value has the form: <value>{,<value>}
	With <value> of the form <TypeDescriptor>:<Item>.

	No spaces are allowed in the value list.

	Available <TypeDescriptor> values are:
		 Project area: ProjectArea - specified by its name. Example: "ProjectArea:JKE Banking (Change Management)"
		 Team area: TeamArea - specified by its name path. Example: "TeamArea:JKE Banking (Change Management)/Release Engineering"
		 Process area: ProcessArea - specified by its name path. Example: "ProcessArea:JKE Banking (Change Management)/Business Recovery Matters"
		 Category: Category - specified by its category path. Example: "Category:JKE/BRM"
		 User: User - specified by its id. Example: "User:tanuj
		 Iteration: Iteration - specified by its name path (including the development line name). Example: "Iteration:Main Development/Release 1.0/Sprint 3"
		 Work item: WorkItem - specified by its id. Example: "WorkItem:20"
		 SCM component: SCMComponent - specified by its name. Example: "SCMComponent:Build"

Modes:
Modes allow different types of changes to attributes such as add values, append text or remove and set other data.
Supported modes are default (no mode specified), add, set, remove.
If no mode is specified, the default mode for the parameter is used.
	Example for default mode: summary="This is a summary.".
	Example for add mode: summary:add=" Add this to the summary.".
	Example for set mode: summary:set="Overwite the existing summary with this.".
	Example for remove mode: custom.enumeration.list:remove=$,Unassigned.

Which modes are supported and their behavior depends on attribute type.
Single value attributes typically support default and set mode, but not add and remove mode. 
Multiple value attributes typically support default, add, set and remove mode.
Default mode for single value attributes sets the value.
Default mode for multiple value attributes adds the value(s).
Set mode for multiple value attributes removes the old values and then adds the new value(s).
Remove mode for multiple value attributes removes the old values specified, that can be found.
String values such as HTML, Summary, Wiki type attributes support default (same behavior as set mode), set and add mode.

Special Properties:
	Work Item ID: Parameter "id" can not be changed.
	Project Area: 
	Parameter "projectArea" can only be specified when creating the work item. It can not be set to a different value later.

Comments: Parameter "internalComments" can be used to add a comment.
	This attribute only supports the default and add mode. Comments can not be removed.
	Example: internalComments="This is a comment"

Subscriptions: 
	Parameter "internalSubscriptions" can be used to subscribe a list of users using their user ID's.
	This attribute supports the modes default (same as) add, set and remove mode.
	Example set specific users: internalSubscriptions:set=al,tammy
	Example add users: internalSubscriptions:add=deb,tanuj,bob
	Example remove users: internalSubscriptions:remove=sally,bob

Tags: Parameter "internalTags" can be used to add a list of tags.
	This attribute supports the modes default (same as) add, set and remove mode.
	Example: internalTags=Tag1,..,TagN

Approvals: 
	Parameter "internalApprovals" can be used to add approvals and approvers using their user ID's.
	Approvals only support the modes default (same as) add, set and remove. 
	Set and remove only affects approvals of the same type. 
Format is:
	internalApprovals[<ID>][:<mode>]="approval:Approval Name as string:userID1,..,userIDn"
	Where <ID> can be left out if only one approval is specified or needs to be unique if multiple approvals are specified. Where <mode> can be left out and defaults to add.
	Available modes are:set add (set as default mode) and remove.
	 Modes set and remove only remove approvals of the same type and must be enabled using the switch enableDeleteApprovals.
	Example internalApprovals="review:Please Review:deb,tanuj"
	Example internalApprovals="verification:Please verify:sally,al"
	where the user list is optional and can contain one or more users ID's

Work Item State: 
	Parameter "internalState"  can be used to change the work item state.
	State change only supports the modes default and set. 
Format is: 
	internalState=StateName to find a one step workflow action to change the state, and execute the action, or
	internalState=forceState:StateName to force the state change to the target state even if no workflow action exists

WorkFlow Action: 
	A pseudo parameter "@workflowAction" can be used to set a workflow action to change the work item state when saving.
	This attribute supports only the modes default and set. 
	Example: @workflowAction="Stop working"
	This attribute requires only the value yes.

Attachments: 
	A pseudo parameter @attachFile can be used to upload attachments.
	This attribute supports the modes default (same as) add, set and remove. 
	Set removes all attachments, remove only removes attachments with the specified file path and description. 
Format is: 
	@attachFile[<IDString>][:mode]="SomeFilePath,Some Description,ContentTypeID,EncodingID"

	Where:
		<IDString> must be unique for multiple attachments in one command. If only one attachment is uploaded, the IDString can be left empty.
		ContentTypeID is text/plain or application/unknown or application/xml
		EncodingID is UTF-8 or UTF-16LE or UTF-16BE or us-ascii.

	The file must be accessible and in the correct encoding.

	Examples:
		@attachFile="C:/temp/test.txt,Some Attachment,text/plain,UTF-8"
		@attachFile_1:add="./test1.txt,Some Attachment 1,text/plain,UTF-8" @attachFile_2="./test2.txt,Some Attachment 2,text/plain,UTF-8"
		@deleteAttachments="yes"

	A pseudo parameter @deleteAttachments=yes can be used to delete all attachments. 

Links: 
	 A pseudo parameter @link_ can be used to link the current work item to other objects.
	Links support the modes default (same as) add, set and remove. 
	Set removes all links of the specified type before creating the new links. 

Work Item Links - links between this work item and another work item within the same repository:
Format is:
	@link_linktype=value

	The parameter value is a list of one or more work items specified by their ID. The separator is:|

	@link_resolves=id1|id2|...
	@link_parent=id1|id2|...
	@link_depends_on=id1|id2|...
	@link_successor=id1|id2|...
	@link_copied=id1|id2|...
	@link_blocks=id1|id2|...
	@link_duplicate=id1|id2|...
	@link_predecessor=id1|id2|...
	@link_duplicate_of=id1|id2|...
	@link_resolved_by=id1|id2|...
	@link_related=id1|id2|...
	@link_mentions=id1|id2|...
	@link_copied_from=id1|id2|...
	@link_child=id1|id2|...

	Example:
		@link_related=123|80

CLM Work Item Links - CLM links between this work item and another work item within the same or acoross repositories
Format is:
	@link_linktype=value

	The parameter value is a list of one or more work items specified by their ID (if they are in the same repository) or by the Item URI. The separator is:|

	@link_tracks_workitem=id1|id2|URI2|...
	@link_affects_plan_item=id1|id2|URI2|...
	@link_affected_by_defect=id1|id2|URI2|...
	@link_related_change_management=id1|id2|URI2|...

	Example:
		@link_tracks_workitem="https://clm.example.com:9443/ccm/resource/itemName/com.ibm.team.workitem.WorkItem/80|120|150"

CLM URI Links - CLM links between this work item and another item, described by a valid URI, in a different repository
Format is:
	@link_linktype=value

	The parameter value is a list of one or more CLM URI's for elements that support this link type. The separator is:|

	@link_related_execution_record=uri1|uri2|...
	@link_related_test_case=uri1|uri2|...
	@link_affects_execution_result=uri1|uri2|...
	@link_affects_requirement=uri1|uri2|...
	@link_related_artifact=uri1|uri2|...
	@link_implements_requirement=uri1|uri2|...
	@link_tested_by_test_case=uri1|uri2|...
	@link_scm_tracks_scm_changes=uri1|uri2|...
	@link_related_test_plan=uri1|uri2|...
	@link_tracks_requirement=uri1|uri2|...
	@link_blocks_test_execution=uri1|uri2|...

	Example:
		@link_affects_requirement=https://clm.example.com:9443/rm/resources/_848a30e315524069854f55e1d35a402d|https://clm.example.com:9443/rm/resources/_6c96bedb0e9a490494273eefc6e1f7c5

	Please note that the link "Associate Work Item" between a change set and the work item can only be created by the SCM component.
	The link created here is the looser CLM link. Create the work item change set link using the SCM command line.

Build result Links - Links from a work item to a build result in the same repository.
Format is:
	@link_reportAgainstBuild=buildResult1|buildResult2|...

	The parameter value is a list of one or more Buildresults specified by their ID or their label. Prefix the build labels @. The separator is:|

	@link_includedInBuild=id1|@BuildLabel2|...
	@link_reportAgainstBuild=id1|@BuildLabel2|...


	Example:

		@link_reportAgainstBuild=@_IjluoH-oEeSHhcw_WFU6CQ|P20141208-1713


Delete all links of a link type 
Format is:
	@deleteLinks_linktype=yes


	Example:

		@deleteLinks_includedInBuild=yes

Encoding:
Importing from a CSV file can fail if the file encoding does not match the expected encodig. An encoding can be provided for the CSV import and export. The default is UTF-8.
Format is: 
	encoding=encodingID

		Where encodingID is UTF-8 or UTF-16LE or UTF-16BE or us-ascii.

	Example:

		encoding=UTF-8

Aliases for attribute ID's:
Available mappings:

	com.ibm.team.workitem.attribute.owner: owner
	com.ibm.team.workitem.attribute.duration: duration
	MODIFIED_BY: modifiedBy
	com.ibm.team.workitem.attribute.correctedestimate: correctedEstimate
	com.ibm.team.workitem.attribute.version: foundIn
	com.ibm.team.workitem.attribute.workitemtype: workItemType
	PROJECT_AREA: projectArea
	STATE: internalState
	PRIORITY: internalPriority
	RESOLVER: resolver
	com.ibm.team.workitem.attribute.resolver: resolver
	OWNER: owner
	SUMMARY: summary
	DESCRIPTION: description
	CREATOR: creator
	FILED_AGAINST: category
	com.ibm.team.workitem.attribute.resolutiondate: resolutionDate
	com.ibm.team.workitem.attribute.description: description
	ID: id
	TYPE: workItemType
	PLANNED_FOR: target
	com.ibm.team.workitem.attribute.priority: internalPriority
	com.ibm.team.workitem.attribute.creationdate: creationDate
	DUE_DATE: dueDate
	TIME_SPENT: timeSpent
	com.ibm.team.workitem.attribute.state: internalState
	CORRECTED_ESTIMATE: correctedEstimate
	RESOLUTION: internalResolution
	CREATION_DATE: creationDate
	com.ibm.team.workitem.attribute.projectarea: projectArea
	com.ibm.team.workitem.attribute.id: id
	com.ibm.team.workitem.attribute.modified: modified
	com.ibm.team.workitem.attribute.timespent: timeSpent
	RESOLUTION_DATE: resolutionDate
	com.ibm.team.workitem.attribute.modifiedby: modifiedBy
	com.ibm.team.workitem.attribute.severity: internalSeverity
	com.ibm.team.workitem.attribute.creator: creator
	com.ibm.team.workitem.attribute.category: category
	com.ibm.team.workitem.attribute.summary: summary
	com.ibm.team.workitem.attribute.tags: internalTags
	FOUND_IN: foundIn
	MODIFIED: modified
	SEVERITY: internalSeverity
	ESTIMATE: duration
	TAGS: internalTags
	com.ibm.team.workitem.attribute.duedate: dueDate
	com.ibm.team.workitem.attribute.target: target
	