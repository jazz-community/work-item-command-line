# EWM/RTC Work Item Command Line - Docker Section

WCL provides a dockerfile and instructions how to create a docker image on your own.

## Steps to build an image
- Download a WCL release binary bundle and extract to wcl directory (see below)
- Download latest RTC java plain libs and extract to rtc directory (see below)
- Modify wcl/wcl.sh and comment out the 2 JAVA_HOME lines
- Execute docker build . to build a local image - The image is ready to use
- If you would like to upload the image, tag the image to your purposes sudo docker tag \<original hash\> \<new Tag\> and push the image to repository

## How to retrieve the WCL binaries
To retrieve the WCL library follow the instructions:
- Open the page https://github.com/jazz-community/work-item-command-line/tags
- Select your release version you would like download
- Look into the Assets section
- Download the WCL-\<version\>.zip
- Extract the wcl zip file into "wcl" directory e.g. unzip -d wcl WCL-\<version\>.zip

## How to retrieve the RTC Plain Java Client Libraries
To retrieve the RTC Plain Java Client Libraries follow the instructions:
- Open the page https://jazz.net/products/rational-team-concert 
- Follow link to the "Download" section
- Select your RTC version 
- Goto "All Downloads"
- Scroll down to "Plain Java Client Libraries"
- Download "Plain Java Client Libraries" zip file
- Extract zip File into "rtc" directory e.g. unzip -d rtc \<name\>.zip

