rem set JAVA_HOME=..\TeamConcert\jazz\client\eclipse\jdk
rem set PLAIN_JAVA=../PlainJavaAPI
set JAVA_HOME=..\..\..\..\installs\TeamConcert\jazz\client\eclipse\jdk
set PLAIN_JAVA=../../../../installs/PlainJavaAPI

rem In the -Djava.ext.dirs always keep the Java distribution in the front
"%JAVA_HOME%\bin\java" -Djava.security.policy=rmi_no.policy -Djava.ext.dirs="%JAVA_HOME%/lib/ext;%JAVA_HOME%/jre/lib/ext;./lib;%PLAIN_JAVA%" -cp "./lib;%PLAIN_JAVA%;./bin com.ibm.js.team.workitem.commandline.WorkitemCommandLine" %*
pause