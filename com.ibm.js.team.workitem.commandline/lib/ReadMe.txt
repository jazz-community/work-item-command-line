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
The org.apache.commons.lang jar can also be found in the EWM server install.

1.a Copy the library file commons-lang.jar into /com.ibm.js.team.workitem.commandline/lib to be able to run from Eclipse.
1.b Place the library commons-lang.jar into the folder lib in the WCL folder (see /com.ibm.js.team.workitem.commandline/ReadMe - HowToRelease.txt). 

3. Pull in juno-all jar from the EWM server install.

3.a Copy the library file juno-all*.jar from /support/mxbean-datacollection/jarlib/ from the EWM server install into /com.ibm.js.team.workitem.commandline/lib to be able to run from Eclipse.
3.b Place the library juno-all*.jar from /support/mxbean-datacollection/jarlib/ from the EWM server install into the folder lib in the WCL folder (see /com.ibm.js.team.workitem.commandline/ReadMe - HowToRelease.txt).

4. Pull in jena-core jar from EWM server install.

4.a Copy the library file jena-core*.jar or com.hp.hpl.jena*.jar from server/conf/ccm/sites/update-site/plugins/ from the EWM server install into /com.ibm.js.team.workitem.commandline/lib to be able to run from Eclipse.
4.b Place the library jena-core*.jar from server/conf/ccm/sites/update-site/plugins/ from the EWM server install into the folder lib in the WCL folder (see /com.ibm.js.team.workitem.commandline/ReadMe - HowToRelease.txt).

5. Pull in extra required EWM common jars

5.a Copy the library file com.ibm.team.apt.common_*.jar and com.ibm.team.tpt.common_*.jar from server/conf/ccm/sites/update-site/plugins/ from the EWM server install into /com.ibm.js.team.workitem.commandline/lib to be able to run from Eclipse.
5.b Place the library com.ibm.team.apt.common_*.jar and com.ibm.team.tpt.common_*.jar from server/conf/ccm/sites/update-site/plugins/ from the EWM server install into the folder lib in the WCL folder (see /com.ibm.js.team.workitem.commandline/ReadMe - HowToRelease.txt).