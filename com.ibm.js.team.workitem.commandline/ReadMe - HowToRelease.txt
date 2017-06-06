To Build the WorkItem CommandLine for Shipping

1. In the project open the folder build
2. Right-click on the file wcl.jardesc and select Open With>JAR Export wizard
3. In "Select The Export Location" you see C:\Temp\wcl\wcl.jar 
   you can change the root for the export if needed but keep the top folder name 
   WCL and don't modify the name of the JAR file; 
   Click Finish and allow to create the folder.
4. Copy the scripts wcl*.* and the other files from folder scripts into the export location for example C:\Temp\wcl\
5. Copy the folder /lib into the export location for example C:\Temp\wcl\ but remove all files except the ReadMe.txt
6. Select the folder, for example C:\Temp\wcl\ and compress the file
7. Rename the archive file to wcl-Vx-YYYYMMDD.zip, 
   where YYYY is the year, MM is the month and DD is the day
8. The file is now ready for publishing 
