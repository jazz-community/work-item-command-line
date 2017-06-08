# work-item-command-line
## RTC Work Item Command Line

# WorkItemCommandLine Version V4.0

**Note:**
All of the following examples use bash multiline syntax for legibility. Leave them out when using single line commands, or replace them with a backtick (`) if you are using powershell.

## Usage 
`-command {switch} {parameter[:mode]=value}`

(See [A RTC WorkItem Command Line Version 2](http://wp.me/p2DlHq-s9) for a more complete description):

Multiple parameter/value pairs and switches can be provided separated by spaces.
Commands might require specific parameters to be mandatory.

Switches influence the behavior of the operation.
The switch /ignoreErrors ignores errors such as attributes or values not available.


Available commands:

```bash
 -migrateattribute \
    targetAttributeID="value" \ 
    projectArea="value" \
    /skipEmailNotification \ 
    repository="value" \ 
    password="value" \ 
    user="value" \
    sourceAttributeID="value" \
    /ignoreErrors workItemType="value"  
```


```bash
-update \
    /enableDeleteAttachment \
    /enableDeleteApprovals \
    /skipEmailNotification \
    repository="value" \
    /enforceSizeLimits \
    id="value" \
    password="value" \
    user="value" \
    /ignoreErrors \
    {parameter[:mode]=value}
```

```bash
-importworkitems \
    importFile="value" \
    /forcelinkcreation \
    /importmultipass \
    projectArea="value" \
    /skipEmailNotification \
    repository="value" \
    /enforceSizeLimits \
    password="value" \
    user="value" \
    /ignoreErrors \
    /importdebug \
    [mappingFile="C:\temp\mapping.xml"] \
    [encoding="UTF_16LE"] \
    [timestampFormat="TIMESTAMP_EXPORT_IMPORT_FORMAT_MMM_D_YYYY_HH_MM_A"] \
    [delimiter=","]
```

```bash
-exportworkitems \
    exportFile="value" \
    projectArea="value" \
    /asrtceclipse \
    query="value" \
    repository="value" \
    /suppressAttributeExceptions \
    /headerIDs \
    password="value" \
    user="value" \
    /ignoreErrors \
    [encoding="UTF_16LE"] \
    [delimiter=","] \
    [columns="Type,Id,Planned For,Filed Against,Description,Found In"] \
    [querysource="JKE Banking(Change Management),JKE Banking(Change Management)/Business Recovery Matters"] \
    [timestampFormat="TIMESTAMP_EXPORT_IMPORT_FORMAT_MMM_D_YYYY_HH_MM_A"]
```

```bash
-create \
    /enableDeleteAttachment \
    /enableDeleteApprovals \
    projectArea="value" \
    repository="value" \
    /enforceSizeLimits \
    password="value" \
    user="value" \
    /ignoreErrors \
    workItemType="value" \
    {parameter[:mode]=value}
```

```bash
-printtypeattributes \
    projectArea="value" \
    repository="value" \
    password="value" \
    user="value" \
    workItemType="value"  
```

## Start in RMI server mode
Use the switch `/rmiServer` to start an instance as RMI server. In this mode, the process will not terminate, but wait for RMI requests to perform commands. It will service commands requested by other client instances that are started with the additional switch `/rmiClient`. It is not necessary to provide a command or any other input values, when starting the server as they will be ignored. Since the TeamPlatform needs to be initilized only once in this mode, the performance is considerably increased for multiple subsequent client calls.

By default, the RMI server uses the name `//localhost/RemoteWorkitemCommandLineServer` on port `1099`.
It is possible to specify a different name and port by providing a value to the switch.

The client command must be started with the same name and port as the server using the corresponding client switch

Example server: 

`/rmiServer=//clm.example.com:1199/WorkItemCommandLine`

Example client: 

```bash
-create \
    /rmiClient=//clm.example.com:1199/WorkItemCommandLine \
    repository=<repositoryURL> \
    user=<user> \
    password=<pass> \
    projectArea=<paName> \
    workItemType=task \
    summary="New Item"
```

Please note, that the server and the client require a policy file for the security manager. A Policy file `rmi_no.policy` is shipped with the download. Rename and modify the file to your requirements. To enable security Java requires to call the class with the additional vm argument `-Djava.security.policy=no.policy` where the policy file name must exist.

## WorkItem attribute parameter and value examples

Format for parameter is:

    parameter[:mode]=value

No spaces are allowed between parameter, value and the `=`. Parameter and value can also not have spaces. Use `"` to enclose values with spaces. Example: `"A Space"`

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

- For Wiki and multi line text attributes use `<br>` or `\n` for line breaks and check the syntax in the wiki editor.
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

- Dates have to be specified in the Java SimpleDateFormat notation.

    `dueDate="2015/02/01 12:30:00 GMT+01:00"`

- Duration values are specified in milliseconds, or a hours minutes format.

    `duration=1800000 correctedEstimate=3600000 timeSpent=60000`
    `duration="1 hour" correctedEstimate="2 hours 30 minutes" timeSpent="30 minutes"`

- WorkItem attribute values of <item type> List with a specified item type such as userList. Format is using the separator `,`:

	`"value1,value2,...,valueN"`
	`Example: custom.user.list:add="deb,al,...,tanuj"`

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

### Special Properties
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
A pseudo parameter `@attachFile` can be used to upload attachments. This attribute supports the modes `default` (same as) `add`, `set` and `remove`. `Set` removes all attachments, `remove` only removes attachments with the specified file path and description. 

#### Format
    @attachFile[<IDString>]="SomeFilePath,Some Description,ContentTypeID,EncodingID"

Where:
`<IDString>` must be unique for multiple attachments in one command. If only one attachment is uploaded, the `IDString` can be left empty. `ContentTypeID` is `text/plain` or `application/unknown` or `application/xml`

`EncodingID` is `UTF-8` or `UTF-16LE` or `UTF-16BE` or `us-ascii`.

The file must be accessible and in the correct encoding.

Examples:

    @attachFile="C:/temp/test.txt:Some Attachment:text/plain:UTF-8"

    @attachFile_1="./test1.txt:Some Attachment 1:text/plain:UTF-8" @attachFile_2="./test2.txt:Some Attachment 2:text/plain:UTF-8"

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
CLM links between this work item and another work item within the same or acoross repositories

##### Format
    @link_linktype=value

The parameter value is a list of one or more work items specified by their ID (if they are in the same repository) or by the Item URI. The separator is:`|`

    @link_affects_plan_item=id1|id2|URI2|...
    @link_tracks_workitem=id1|id2|URI2|...
    @link_related_change_management=id1|id2|URI2|...
    @link_affected_by_defect=id1|id2|URI2|...

Example:

    @link_tracks_workitem="https://clm.example.com:9443/ccm/resource/itemName/com.ibm.team.workitem.WorkItem/80|120|150"

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

Please note that the link "Associate Work Item" between a change set and the work item can only be created by the SCM component. The link created here is the looser CLM link. Create the work item change set link using the SCM command line.

#### Build result Links 
Links from a work item to a build result in the same repository.

##### Format
    @link_reportAgainstBuild=buildResult1|buildResult2|...

The parameter value is a list of one or more Buildresults specified by their ID or their label. Prefix the build labels @. The separator is:`|`

    @link_reportAgainstBuild=id1|@BuildLabel2|...
    @link_includedInBuild=id1|@BuildLabel2|...


Example:

    @link_reportAgainstBuild=@_IjluoH-oEeSHhcw_WFU6CQ|P20141208-1713


##### Aliases for attribute ID's

Available mappings:

| From                                              | To                    |
| ------------------------------------------------  | --------------------- |
| com.ibm.team.workitem.attribute.version           | foundIn               |
| SUMMARY                                           | summary               |
| RESOLVER                                          | resolver              |
| MODIFIED                                          | modified              |
| CREATOR                                           | creator               |
| MODIFIED_BY                                       | modifiedBy            |
| CREATION_DATE                                     | creationDate          |
| PLANNED_FOR                                       | target                |
| com.ibm.team.workitem.attribute.severity          | internalSeverity      |
| com.ibm.team.workitem.attribute.category          | category              |
| com.ibm.team.workitem.attribute.duedate           | dueDate               |
| OWNER                                             | owner                 |
| PROJECT_AREA                                      | projectArea           |
| com.ibm.team.workitem.attribute.summary           | summary               |
| com.ibm.team.workitem.attribute.projectarea       | projectArea           |
| com.ibm.team.workitem.attribute.resolver          | resolver              |
| TAGS                                              | internalTags          |
| com.ibm.team.workitem.attribute.creator           | creator               |
| com.ibm.team.workitem.attribute.modified          | modified              |
| ID                                                | id                    |
| DESCRIPTION                                       | description           |
| TIME_SPENT                                        | timeSpent             |
| com.ibm.team.workitem.attribute.correctedestimate | correctedEstimate     |
| com.ibm.team.workitem.attribute.creationdate      | creationDate          |
| com.ibm.team.workitem.attribute.owner             | owner                 |
| RESOLUTION_DATE                                   | resolutionDate        |
| FOUND_IN                                          | foundIn               |
| com.ibm.team.workitem.attribute.target            | target                |
| PRIORITY                                          | internalPriority      |
| com.ibm.team.workitem.attribute.duration          | duration              |
| ESTIMATE                                          | duration              |
| com.ibm.team.workitem.attribute.description       | description           |
| FILED_AGAINST                                     | category              |
| com.ibm.team.workitem.attribute.tags              | internalTags          |
| com.ibm.team.workitem.attribute.resolutiondate    | resolutionDate        |
| RESOLUTION                                        | internalResolution    |
| com.ibm.team.workitem.attribute.id                | id                    |
| com.ibm.team.workitem.attribute.timespent         | timeSpent             |
| com.ibm.team.workitem.attribute.workitemtype      | workItemType          |
| STATE                                             | internalState         |
| com.ibm.team.workitem.attribute.modifiedby        | modifiedBy            |
| com.ibm.team.workitem.attribute.priority          | internalPriority      |
| DUE_DATE                                          | dueDate               |
| TYPE                                              | workItemType          |
| SEVERITY                                          | internalSeverity      |
| CORRECTED_ESTIMATE                                | correctedEstimate     |
| com.ibm.team.workitem.attribute.state             | internalState         |

