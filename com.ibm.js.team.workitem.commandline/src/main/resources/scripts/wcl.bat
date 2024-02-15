rem set JAVA_HOME=C:\PROGRA~1\Java\jre1.8.0_191
set PLAIN_JAVA=C:\RTC702SR1Dev\installs\PlainJavaApi

rem In the -Djava.ext.dirs always keep the Java distribution in the front
"%JAVA_HOME%\bin\java" -Djava.security.policy=rmi_no.policy -cp "%JAVA_HOME%/lib/ext;./lib/*;%PLAIN_JAVA%/*;wcl.jar" com.ibm.js.team.workitem.commandline.WorkitemCommandLine %*