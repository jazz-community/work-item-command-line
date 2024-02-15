WCL uses some libraries that don't ship with it to export and import.

To run the the work item exporter and importer download and collect the following jar files. Place them all into a folder e.g. C:\Dev\WCL_ExternalLibs.

1. Download opencsv from https://mvnrepository.com/artifact/com.opencsv/opencsv

It is possible to use the version or later 

2.a Download apache commons-text 1.11 or later from https://commons.apache.org/proper/commons-text/download_text.cgi
Extract the zip file and locate the file commons-text1-*.jar

2.b Download apache commons-lang 3.14 or later from https://commons.apache.org/lang/download_lang.cgi
Extract the zip file and locate the file commons-lang3-*.jar

3. Pull in juno-all jar from the EWM server install.

4. Pull in jena-core jar from EWM server install.

5. Pull in extra required EWM common jars

Copy the library file com.ibm.team.apt.common_*.jar and com.ibm.team.tpt.common_*.jar from server/conf/ccm/sites/update-site/plugins/ from the EWM server install into your folder

To use the files in a released WCL copy these files into the folder WCL/lib.

To use the files in an Eclipse workspace with WCL follow https://github.com/jazz-community/work-item-command-line/blob/master/README.md#prerequisites and add the files to your class path.
