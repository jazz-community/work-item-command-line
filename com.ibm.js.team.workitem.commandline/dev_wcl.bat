rem set JAVA_HOME=..\TeamConcert\jazz\client\eclipse\jdk
rem set PLAIN_JAVA=../PlainJavaAPI
set JAVA_HOME=..\..\..\..\installs\TeamConcert\jazz\client\eclipse\jdk
set PLAIN_JAVA=../../../../installs/PlainJavaAPI

%JAVA_HOME%\jre\bin\java -Djava.ext.dirs=./lib;%PLAIN_JAVA%;%JAVA_HOME%/jre/lib/ext; -cp ./lib;%PLAIN_JAVA%;./bin com.ibm.js.team.workitem.commandline.WorkitemCommandLine %*

pause

