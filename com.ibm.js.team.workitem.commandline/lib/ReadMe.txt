WCL uses some libraries that don't ship with it to export and import.

To run the the work item exporter and importer

1. Download opencsv from 
http://sourceforge.net/projects/opencsv/files/opencsv/3.7/

It is possible to use the latest version 4.3.2 from https://sourceforge.net/projects/opencsv/files/opencsv/

Rename the jar file e.g. opencsv-3.7.jar to opencsv.jar 

1.a Copy the library file opencsv.jar into /com.ibm.js.team.workitem.commandline/lib to be able to run from Eclipse.
1.b Place the library opencsv.jar into the folder lib in the WCL folder (see /com.ibm.js.team.workitem.commandline/ReadMe - HowToRelease.txt). 

2. Download apache commons-lang from 
https://commons.apache.org/proper/commons-lang/download_lang.cgi

Version 3-3.1 works.

Rename the jar file e.g. commons-lang3-3.1.jar to commons-lang.jar

1.a Copy the library file commons-lang.jar into /com.ibm.js.team.workitem.commandline/lib to be able to run from Eclipse.
1.b Place the library commons-lang.jar into the folder lib in the WCL folder (see /com.ibm.js.team.workitem.commandline/ReadMe - HowToRelease.txt). 
