WCL uses some libraries that don't ship with it to export and import.

To run the the work item exporter and importer

1. Download opencsv 2.3 from 
http://sourceforge.net/projects/opencsv/files/opencsv/2.3/ 
and place the library opencsv-2.3.jar into the folder lib in the WCL folder.

Please note, I had problems with openCSV 3.x probably due to the Java Version.


2. Copy the Eclipse plugins from RTC Eclipse into the lib folder

Open the Eclipse Plugins folder for examle jazz\client\eclipse\plugins and search for a file 
com.ibm.team.workitem.rcp.core_*.jar 

The last part of the Jar file name is a version number and can be different. 
An Example: com.ibm.team.workitem.rcp.core_3.1.800.v20140706_1427.jar

Copy this jar File into the folder lib in the WCL folder.

You can now use the script files to run WCL.

To run the workitem command line in Eclipse, add the jar files to the classpath e.g. as User Library.
  