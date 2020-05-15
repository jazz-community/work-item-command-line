# To Build the WorkItem CommandLine for Shipping

## Exporting from Eclipse

1. Right-click on the Project and select export. Select Runnable JAR

2. in the Runnable JAR File Specification select 
Launch Configuration: WorkitemCommandLine - runnable JAR
Export destination: C:\Temp\WCL\WCL.jar 
Library Handling: copy required libraries into a sub filder next to the generated JAR 
Press Finish

3. Open the folder C:\Temp\WCL\ delete the folder WCL_lib.

4. Copy the scripts wcl*.* and the other files from folder scripts into the export location for example C:\Temp\wcl\

5. Copy the folder /lib into the export location for example C:\Temp\wcl\ but remove all files except the ReadMe.txt
   Optional, follow lib/ReadMe.txt and download and add the libraries required for CSV import and export

6. Select the folder, for example C:\Temp\wcl\ and compress the file

7. Rename the archive file to WCL-Vx-YYYYMMDD.zip, 
   where YYYY is the year, MM is the month and DD is the day

8. The file is now ready for publishing 

## Building from Gradle

1. [Download](https://gradle.org/next-steps/?version=5.6.2&format=bin) and install gradle.

2. From the Eclipse project directory (com.ibm.js.team.workitem.commandline):

    gradle -Pplain_java=<path to plain java lib folder> pkg
    
3. Extract wcl-<version>.zip from the `build` folder to `<target path>\WCL`, ensuring to maintaining the top level `WCL` folder name.
