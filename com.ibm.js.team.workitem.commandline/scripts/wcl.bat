rem set JAVA_HOME=C:\PROGRA~1\Java\jre1.8.0_191
set PLAIN_JAVA=C:\RTC606Dev\Installs\PlainJavaAPI

"%JAVA_HOME%\bin\java" -Djava.security.policy=rmi_no.policy -Djava.ext.dirs="./lib;%PLAIN_JAVA%;%JAVA_HOME%/jre/lib/ext" -cp "./lib;%PLAIN_JAVA%" -jar wcl.jar%*
