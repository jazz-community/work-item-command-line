rem set JAVA_HOME=C:\PROGRA~1\Java\jre1.8.0_191
set PLAIN_JAVA=C:\RTC606Dev\Installs\PlainJavaAPI

rem In the -Djava.ext.dirs always keep the Java distribution in the front
"%JAVA_HOME%\bin\java" -Djava.security.policy=rmi_no.policy -Djava.ext.dirs="%JAVA_HOME%/lib/ext;%JAVA_HOME%/jre/lib/ext;./lib;%PLAIN_JAVA%" -cp "./lib;%PLAIN_JAVA%" -jar wcl.jar %*
