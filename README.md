# RTC Work Item Command Line

Work Item Command Line Version 5.3

See [Work Item Command Line 5.0](https://rsjazz.wordpress.com/2019/07/03/work-item-command-line-5-0/) for instructions how to setup and install WCL.

This software is licensed under the MIT license which is as follows: [MIT License](com.ibm.js.team.workitem.commandline/LICENSE.txt)

**Note:**
All of the following examples use bash multiline syntax for legibility. Leave them out when using single line commands, or replace them with a backtick (`) if you are using powershell.

# Table of Contents
-   [RTC Work Item Command Line](#rtc-work-item-command-line)
    -   [Usage](#usage)
    -   [Start in RMI server mode](#start-in-rmi-server-mode)
    - [Command Parameters](#command-parameters)
    -   [WorkItem attribute parameter and value
        examples](#workitem-attribute-parameter-and-value-examples)
        -   [Parameters](#parameters)
        -   [Values](#values)
        -   [Examples](#examples)
    -   [Modes](#modes)
    -   [Special Properties](#special-properties)
        -   [Comments](#comments)
        -   [Subscriptions](#subscriptions)
        -   [Tags](#tags)
        -   [Approvals](#approvals)
        -   [Work Item State:](#work-item-state)
        -   [WorkFlow Action](#workflow-action)
        -   [Attachments](#attachments)
        -   [Links](#links)
    -   [Attribute ID Aliases](#attribute-id-aliases)
    -   [Link types](#link-types)
    -   [Import/Export](#importexport)
        -   [Export Modes](#export-modes)
        -   [Import/Export](#importexport)
        -   [Attachments](#attachments-1)
    -   [License](#license)
    
 
## Usage 
`-[command] {switch} {parameter[:mode]=value}`

(See [A RTC WorkItem Command Line Version 2](http://wp.me/p2DlHq-s9) for a more complete description):

Multiple parameter/value pairs and switches can be provided separated by spaces.
Commands might require specific parameters to be mandatory.

Switches influence the behavior of the operation.
The switch /ignoreErrors ignores errors such as attributes or values not available.


Available commands:

```bash
-printtypes 
    repository="value" 
    user="value"   
    password="value" 
    projectArea="value" 
```

```bash
-printtypeattributes 
    repository="value" 
    user="value" 
    password="value" 
    projectArea="value" 
    workItemType="value"  
```

```bash
-printworkitem
    repository="value" 
    user="value" 
    password="value" 
    id="value"
    /allColumns 
    /asrtceclipse
    /attributeNamesAsIDs	
    /ignoreErrors 
    /suppressAttributeExceptions 
    /ignoreErrors 
    [attachmentFolder="C:\temp\export"]
    [timestampFormat="MMM d, yyyy hh:mm a"]
    [columns="Type,Id,Planned For,Filed Against,Description,Found In"]
```

```bash
-create 
    repository="value" 
    user="value" 
    password="value" 
    projectArea="value" 
    workItemType="value" 
    /enforceSizeLimits 
    /ignoreErrors 
    /enableDeleteAttachment 
    /enableDeleteApprovals 
    {parameter[:mode]=value}
```

```bash
-update 
    repository="value" 
    user="value" 
    password="value" 
    id="value" 
    /enableDeleteAttachment 
    /enableDeleteApprovals 
    /skipEmailNotification 
    /enforceSizeLimits 
    /ignoreErrors 
    {parameter[:mode]=value}
```

```bash
-bulkupdate 
    repository="value" 
    user="value" 
    password="value" 
    query="value"
    querysource="value"
    /enableDeleteAttachment 
    /enableDeleteApprovals 
    /skipEmailNotification 
    /enforceSizeLimits 
    /ignoreErrors 
    {parameter[:mode]=value}
```

```bash
-importworkitems 
    repository="value" 
    user="value" 
    password="value" 
    projectArea="value" 
    importFile="value" 
    /skipEmailNotification 
    /ignoreErrors 
    /importdebug 
    /forcelinkcreation 
    /enforceSizeLimits 
    /importmultipass
    /ignoreemptycolumnvalues
    /suppressIgnoredAttributeWarnings
    [mappingFile="C:\temp\mapping.xml"] 
    [encoding="UTF-8"] 
    [timestampFormat="MMM d, yyyy hh:mm a"] 
    [delimiter=","]
```

```bash
-exportworkitems
    repository="value" 
    user="value" 
    password="value"
    projectArea="value" 
    query="value" 
    exportFile="value"
    /allColumns
    /asrtceclipse 
    /suppressAttributeExceptions 
    /headerIDs 
    /ignoreErrors 
    [encoding="UTF-8"] 
    [delimiter=","] 
    [columns="Type,Id,Planned For,Filed Against,Description,Found In"] 
    [querysource="JKE Banking(Change Management),JKE Banking(Change Management)/Business Recovery Matters"] 
    [timestampFormat="MMM d, yyyy hh:mm a"]
```

```bash
 -migrateattribute 
    repository="value"  
    user="value" 
    password="value"  
    projectArea="value"  
    targetAttributeID="value" 
    sourceAttributeID="value"
    workItemType="value"
    /ignoreErrors 
    /skipEmailNotification
```

```bash
-validateoslclinks
    repository="value"
    user="value"
    password="value"
    projectArea="value"
    query="value"
    /trace
    /debug
```
## Command parameters

### Query 

Provide a query name. The query by default is searched as a personal query owned by the logged in contributor. Provide a Query Source using the parameter `querysource`. The query source can contain a list of project area and team area names.   

### Query Source

Provide a list of project area and team area names. 

## Start in RMI server mode
Use the switch `/rmiServer` to start an instance as RMI server. In this mode, the process will not terminate, but wait for RMI requests to perform commands. It will service commands requested by other client instances that are started with the additional switch `/rmiClient`. It is not necessary to provide a command or any other input values, when starting the server as they will be ignored. Since the TeamPlatform needs to be initialized only once in this mode, the performance is considerably increased for multiple subsequent client calls.

By default, the RMI server uses the name `//localhost/RemoteWorkitemCommandLineServer` on port `1099`.
It is possible to specify a different name and port by providing a value to the switch.

The client command must be started with the same name and port as the server using the corresponding client switch

Example start as an RMI server: 

`/rmiServer=//clm.example.com:1199/WorkItemCommandLine`

Example start as an RMI client with a command: 

```bash
-create 
    /rmiClient=//clm.example.com:1199/WorkItemCommandLine 
    repository="https://clm.example.com:9443/ccm" 
    user=functional 
    password=********* 
    projectArea="Test" 
    workItemType=task 
    summary="New Item"
```

Please note, that the server and the client require a policy file for the security manager. A Policy file `rmi_no.policy` is shipped with the download. Rename and modify the file to your requirements. To enable security Java requires to call the class with the additional vm argument `-Djava.security.policy=no.policy` where the policy file name must exist.

## WorkItem attribute parameter and value examples

Format for parameter is:

    parameter[:mode]=value

No spaces are allowed between parameter, value and the `=`. Parameter and value can also not have spaces. Use `"` to enclose values with spaces. 

Example: `"A Space"`

### Parameters
Parameter is a work item attribute ID and value is a value or a list of values. Use the command `-printtypeattributes` to retrieve the available attribute ID's, or inspect the process configuration of your project area to extract the attribute ID's.

### Values
The values are specified by a string. This is can be display name of that value (enumerations) or composed of display values of the path to this item (category, iterations, process areas). For other attributes, such as users, work item types or work items, use the ID.

### Examples
- For enumeration based attributes use the display value for the enumeration literal:

    `internalPriority=High`

- For HTML and string based attributes use a string. HTML types like summary, description, comment and HTML support the following syntax.

    ```html
    description="Plain text<br/><b>bold text</b><br/><i>italic text</i><br/><a href="https://rsjazz.wordpress.com">External RSJazz Link</a><br/>User link to <b>@ralph </b><br/>Work Item link to Defect 3 <br/>"
    ```

- For Wiki and multi line text attributes use `<br>` or `\n` for line breaks and check the syntax in the wiki editor. The Wiki syntax can be found here http://www.wikicreole.org/ .

    ```html
    custom.wiki="<br>=Heading1<br><br>Plain text\n==Heading 2\n\nNormal Text **bold text** <br>**bold text**<br>//Italics//"
    ```

- For work item type, owner and some other attributes use the object ID.

    `workItemType=task`
    
    `owner=tanuj`

- Use the display name for simple attributes or the path composed out of the display names for hierarchical attributes.

    `category=JKE/BRN`
    
    `foundIn="Sprint 2 Development"`
    
    `target="Main Development/Release 1.0/Sprint 3"`
    
    `custom.process.area="JKE Banking (Change Management)/Release Engineering"`

- Dates and Timestamps have to be specified in the Java SimpleDateFormat notation. The value "unassigned" can be used to delete the date.

    `dueDate="2015/02/01 12:30:00 GMT+01:00"`
    
    `dueDate="unassigned"`

- Duration values are specified in milliseconds, or a hours minutes format.

    `duration=1800000 correctedEstimate=3600000 timeSpent=60000`
    
    `duration="1 hour" correctedEstimate="2 hours 30 minutes" timeSpent="30 minutes"`

- WorkItem attribute values of <item type> List with a specified item type such as userList. Format is using the separator `,`:

	`"value1,value2,...,valueN"`

	Example: `custom.user.list:add="deb,al,...,tanuj"`

- WorkItem attributes with an general attribute value such as Item or itemList require encoding to locate the items. Format is:

	`custom.item.list=value`

	Where value has the form: `<value>{,<value>}`
	
	With `<value>` of the form `<TypeDescriptor>:<Item>`

	No spaces are allowed in the value list.

	Available <TypeDescriptor> values are:

     - Project area: ProjectArea - specified by its name. 

        Example: `"ProjectArea:JKE Banking (Change Management)"`
	
     - Team area: TeamArea - specified by its name path. 

        Example: `"TeamArea:JKE Banking (Change Management)/Release Engineering"`
	
     - Process area: ProcessArea - specified by its name path. 

        Example: `"ProcessArea:JKE Banking (Change Management)/Business Recovery Matters"`
	
     - Category: Category - specified by its category path. 

        Example: `"Category:JKE/BRM"`
	
     - User: User - specified by its id. 

        Example: `"User:tanuj"`
	
     - Iteration: Iteration - specified by its name path (including the development line name). 

        Example: `"Iteration:Main Development/Release 1.0/Sprint 3"`
	
     - Work item: WorkItem - specified by its id. 

        Example: `"WorkItem:20"`
	
     - SCM component: SCMComponent - specified by its name. 

        Example: `"SCMComponent:Build"`

## Modes
Modes allow different types of changes to attributes such as add values, append text or remove and set other data.
Supported modes are `default` (no mode specified), `add`, `set`, `remove`.
If no mode is specified, the default mode for the parameter is used.

- Example for `default` mode: `summary="This is a summary."`.
- Example for `add` mode: `summary:add=" Add this to the summary."`.
- Example for `set` mode: `summary:set="Overwite the existing summary with this."`.
- Example for `remove` mode: `custom.enumeration.list:remove=$,Unassigned`.

Which modes are supported and their behavior depends on attribute type. Single value attributes typically support `default` and `set` mode, but not `add` and `remove` mode. Multiple value attributes typically support `default` , `add` , `set` and `remove` mode. `Default` mode for single value attributes sets the value. `Default` mode for multiple value attributes adds the value(s). `Set` mode for multiple value attributes removes the old values and then adds the new value(s). `Remove` mode for multiple value attributes removes the old values specified, that can be found.

String values such as HTML, Summary, Wiki type attributes support `default` (same behavior as `set` mode), `set` and `add` mode.

## Special Properties
**Work Item ID:** Parameter `"id"` can not be changed.

**Project Area:** Parameter `"projectArea"` can only be specified when creating the work item. It can not be set to a different value later.

### Comments
Parameter `"internalComments"` can be used to add a comment. This attribute only supports the default and add mode. Comments can not be removed.

Example: `internalComments="This is a comment"`

### Subscriptions 
Parameter `"internalSubscriptions"` can be used to subscribe a list of users using their user ID's. This attribute supports the modes default (same as) `add`, `set` and `remove` mode.

- Example set specific users: `internalSubscriptions:set=al,tammy`
- Example add users: `internalSubscriptions:add=deb,tanuj,bob`
- Example remove users: `internalSubscriptions:remove=sally,bob`

### Tags
Parameter `"internalTags"` can be used to add a list of tags. This attribute supports the modes default (same as) `add`, `set` and `remove` mode.

Example: `internalTags=Tag1,..,TagN`

### Approvals
Parameter `"internalApprovals"` can be used to add approvals and approvers using their user ID's. Approvals only support the modes `default` (same as) `add`, `set` and `remove`. Set and remove only affects approvals of the same type. 

#### Format
    internalApprovals[<ID>][:<mode>]="approval:Approval Name as string:userID1,..,userIDn"

Where `<ID>` can be left out if only one approval is specified or needs to be unique if multiple approvals are specified. Where `<mode>` can be left out and defaults to `add`.

Available modes are: `set` `add` (set as default mode) and `remove`.

Modes `set` and `remove` only remove approvals of the same type and must be enabled using the switch `enableDeleteApprovals`.

Example: `internalApprovals="review:Please Review:deb,tanuj"`
Example: `internalApprovals="verification:Please verify:sally,al"`

where the user list is optional and can contain one or more users ID's

### Work Item State:
Parameter `"internalState"` can be used to change the work item state. State change only supports the modes `default` and `set`. 

#### Format

`internalState=StateName` to find a one step workflow action to change the state, and execute the action, or
`internalState=forceState:StateName` to force the state change to the target state even if no workflow action exists

### WorkFlow Action 
A pseudo parameter `"@workflowAction"` can be used to set a workflow action to change the work item state when saving. This attribute supports only the modes `default` and `set`. 

Example: `@workflowAction="Stop working"`

### Attachments

Attachments are not stored in an attribute, therefore there is no attribute name or ID. WCL solves that by using pseudo attributes.

CSV export and import support attachments. The pseudo attribute name and type `Attachment` or `attachment` is used as column header representing the attachments.   

For work item creation and update operations, a pseudo parameter `@attachFile` can be used to upload attachments. This attribute supports the modes `default` (same as) `add`, `set` and `remove`. `Set` removes all attachments, `remove` only removes attachments with the specified file path and description. 

#### Format
    @attachFile[<IDString>][:<mode>]="SomeFilePath,Some Description,ContentTypeID,EncodingID"

Where:
`<IDString>` must be unique for multiple attachments in one command. If only one attachment is uploaded, the `IDString` can be left empty. `ContentTypeID` is `text/plain` or `application/unknown` or `application/xml`

`EncodingID` is `UTF-8` (default) or `UTF-16LE` or `UTF-16BE` or `us-ascii`.

The file must be accessible and in the correct encoding.

Examples:

    @attachFile="C:/temp/test.txt,Some Attachment,text/plain,UTF-8"
    @attachFile_1:add="./test1.txt,Some Attachment 1,text/plain,UTF-8" @attachFile_2="./test2.txt,Some Attachment 2,text/plain,UTF-8"
 
A pseudo parameter `@deleteAttachments` can be used to delete all attachments. Modes have no influence on this attribute. This attribute supports the value `yes`. 

Example:

    @deleteAttachments=yes

### Links
A pseudo parameter `@link_ can` be used to link the current work item to other objects. Links support the modes default (same as) `add`, `set` and `remove`. `Set` removes all links of the specified type before creating the new links. 

#### Work Item Links
links between this work item and another work item within the same repository

##### Format
    @link_linktype=value

The parameter value is a list of one or more work items specified by their ID. The separator is:`|`

    @link_copied=id1|id2|...
    @link_copied_from=id1|id2|...
    @link_successor=id1|id2|...
    @link_blocks=id1|id2|...
    @link_resolves=id1|id2|...
    @link_mentions=id1|id2|...
    @link_predecessor=id1|id2|...
    @link_parent=id1|id2|...
    @link_duplicate_of=id1|id2|...
    @link_duplicate=id1|id2|...
    @link_related=id1|id2|...
    @link_depends_on=id1|id2|...
    @link_child=id1|id2|...
    @link_resolved_by=id1|id2|...

Example:

    @link_related=123|80

#### CLM Work Item Links 
CLM links between this work item and another work item within the same or across repositories

##### Format
    @link_linktype=value

The parameter value is a list of one or more work items specified by their ID (if they are in the same repository) or by the Item URI. The separator is:`|`

    @link_affects_plan_item=id1|id2|URI2|...
    @link_tracks_workitem=id1|id2|URI2|...
    @link_related_change_management=id1|id2|URI2|...
    @link_affected_by_defect=id1|id2|URI2|...

Example:

    @link_tracks_workitem="https://clm.example.com:9443/ccm/resource/itemName/com.ibm.team.workitem.WorkItem/80|120|150"

Please note that the link "Mentions" can not directly be created during import or update operations. They can only be created indirectly by referring to work items and users in the description or comments. 

#### CLM URI Links 
CLM links between this work item and another item, described by a valid URI, in a different repository

##### Format
    @link_linktype=value

The parameter value is a list of one or more CLM URI's for elements that support this link type. The separator is:`|`

    @link_related_test_plan=uri1|uri2|...
    @link_affects_requirement=uri1|uri2|...
    @link_tested_by_test_case=uri1|uri2|...
    @link_blocks_test_execution=uri1|uri2|...
    @link_implements_requirement=uri1|uri2|...
    @link_affects_execution_result=uri1|uri2|...
    @link_related_artifact=uri1|uri2|...
    @link_related_test_case=uri1|uri2|...
    @link_tracks_requirement=uri1|uri2|...
    @link_scm_tracks_scm_changes=uri1|uri2|...
    @link_related_execution_record=uri1|uri2|...

Example:
    @link_affects_requirement=https://clm.example.com:9443/rm/resources/_848a30e315524069854f55e1d35a402d|https://clm.example.com:9443/rm/resources/_6c96bedb0e9a490494273eefc6e1f7c5

Please note that the link "Associate Work Item" between a change set and the work item can only be created by the SCM component and therefore not by the WCL. The link created here is the looser CLM link. Create the work item change set link using the SCM command line.
 
#### Build result Links 
Links from a work item to a build result in the same repository.

##### Format
    @link_reportAgainstBuild=buildResult1|buildResult2|...

The parameter value is a list of one or more Build results specified by their ID or their label. Prefix the build labels @. The separator is:

    @link_reportAgainstBuild=id1|@BuildLabel2|...
    @link_includedInBuild=id1|@BuildLabel2|...


Example:

    @link_reportAgainstBuild=@_IjluoH-oEeSHhcw_WFU6CQ|P20141208-1713

#### Delete all links of a link type 
Delete all links of a link type.

##### Format is:

    @deleteLinks_linktype=yes

Example:

    @deleteLinks_includedInBuild=yes

#### Encoding

Importing from a CSV file can fail if the file encoding does not match the expected encodig. An encoding can be provided for the CSV import and export. The default encoding is: UTF-8

#### Format is: 

	encoding=encodingID

		Where encodingID is UTF-8 or UTF-16LE or UTF-16BE or us-ascii.

Example:

		encoding=UTF-8

## Attribute ID Aliases

The attribute IDs available to a work item type can be listed with the pryttypeattributes command. It is also possible to look into the process specification. The attribute type ID's can be looked up in the RTC project area administration Web UI. Note that the Eclipse project area administation UI does not show the correct ID. For example for the category attribute, the Eclipse project area admin UI shows com.ibm.team.workitem.attribute.category but the correct value of the attribute is category. For Severity the correct ID is internalSeverity and not com.ibm.team.workitem.attribute.severity as the Eclipse Admin UI shows. To mitigate this problem and to allow to use the values similar to the constants shown in attribute customization, WCL provides a mapping for additional strings representing the same attribute ID. The table below shows which attribute ID aliases map to the predefined values. 


Available mappings:

| From                                              | To                    |
| ------------------------------------------------- | --------------------- |
| CORRECTED_ESTIMATE                                | correctedEstimate     |
| CREATION_DATE                                     | creationDate          |
| CREATOR                                           | creator               |
| DESCRIPTION                                       | description           |
| DUE_DATE                                          | dueDate               |
| FOUND_IN                                          | foundIn               |
| ID                                                | id                    |
| MODIFIED                                          | modified              |
| MODIFIED_BY                                       | modifiedBy            |
| OWNER                                             | owner                 |
| PLANNED_FOR                                       | target                |
| PRIORITY                                          | internalPriority      |
| PROJECT_AREA                                      | projectArea           |
| RESOLUTION_DATE                                   | resolutionDate        |
| RESOLVER                                          | resolver              |
| ESTIMATE                                          | duration              |
| FILED_AGAINST                                     | category              |
| RESOLUTION                                        | internalResolution    |
| SEVERITY                                          | internalSeverity      |
| STATE                                             | internalState         |
| SUMMARY                                           | summary               |
| TAGS                                              | internalTags          |
| TIME_SPENT                                        | timeSpent             |
| TYPE                                              | workItemType          |
| com.ibm.team.workitem.attribute.category          | category              |
| com.ibm.team.workitem.attribute.correctedestimate | correctedEstimate     |
| com.ibm.team.workitem.attribute.creationdate      | creationDate          |
| com.ibm.team.workitem.attribute.creator           | creator               |
| com.ibm.team.workitem.attribute.description       | description           |
| com.ibm.team.workitem.attribute.duedate           | dueDate               |
| com.ibm.team.workitem.attribute.duration          | duration              |
| com.ibm.team.workitem.attribute.id                | id                    |
| com.ibm.team.workitem.attribute.modified          | modified              |
| com.ibm.team.workitem.attribute.modifiedby        | modifiedBy            |
| com.ibm.team.workitem.attribute.owner             | owner                 |
| com.ibm.team.workitem.attribute.priority          | internalPriority      |
| com.ibm.team.workitem.attribute.projectarea       | projectArea           |
| com.ibm.team.workitem.attribute.resolver          | resolver              |
| com.ibm.team.workitem.attribute.resolutiondate    | resolutionDate        |
| com.ibm.team.workitem.attribute.severity          | internalSeverity      |
| com.ibm.team.workitem.attribute.state             | internalState         |
| com.ibm.team.workitem.attribute.summary           | summary               |
| com.ibm.team.workitem.attribute.tags              | internalTags          |
| com.ibm.team.workitem.attribute.target            | target                |
| com.ibm.team.workitem.attribute.timespent         | timeSpent             |
| com.ibm.team.workitem.attribute.version           | foundIn               |
| com.ibm.team.workitem.attribute.workitemtype      | workItemType          |



## Link types

The following link types are supported. Please note that some link types might only be supported in some of the commands or use cases.

| Display name                  | Id                       |
| ----------------------------- | ------------------------ |
| Affected by Defect            | affected_by_defect       |
| Affects Plan Item             | affects_plan_item        |
| Affects Requirement           | affects_requirement      |
| Affects Test Result           | affects_execution_result |
| Blocks                        | blocks                   |
| Blocks Test Execution         | blocks_test_execution    |
| Children                      | child                    |
| Contributes To                | contributes_to_workitem  |
| Copied From                   | copied_from              |
| Copies                        | copied                   |
| Depends On                    | depends_on               |
| Duplicate Of                  | duplicate_of             |
| Duplicated By                 | duplicate                |
| Implements Requirement        | implements_requirement   |
| Included in Builds            | includedInBuild          |
| Parent                        | parent                   |
| Predecessor                   | predecessor              |
| Related                       | related                  |
| Related Artifacts             | related_artifact         |
| Related Test Case             | related_test_case        |
| Related Test Execution Record | related_execution_record |
| Related Test Plan             | related_test_plan        |
| Reported Against Builds       | reportAgainstBuild       |
| Resolved By                   | resolved_by              |
| Resolves                      | resolves                 |
| Successor                     | successor                |
| Tested By Test Case           | tested_by_test_case      |
| Tracks                        | tracks_workitem          |
| Tracks Requirement            | tracks_requirement       |


## Import/Export 

The import and export capabilities of the Work Item Command Line have some special behavior that are not obvious. Here a summary of those topics.

### Export Modes

By default, the WCL RTC Export exports the attribute data in a format that is compatile with the WCL work item operations. It is possible to use the switch /asrtceclipse for the export. In this mode attachments are not downloaded and the attachment information is provided similar to the built in CSV export.

### Attachments

The export to CSV can be used to download and store attachments. In the default mode, if the attachments column is detected in the exported attributes, the attachments are stored in a folder structure in the folder used to export the CSV file. In the CSV file, the exported attachment is referenced as path to the download location. This information can be used during import as well. 

In the mode /asrtceclipse, attachment download is not supported. The exported data in the CSV file shows the same information that is provided in the RTC CSV export format.

# License

Licensed under the MIT License.
