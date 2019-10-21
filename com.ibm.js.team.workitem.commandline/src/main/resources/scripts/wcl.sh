#!/bin/sh
#JAVA_HOME=/usr/java/jdk1.8.0_20/bin
#export JAVA_HOME

PLAIN_JAVA=/usr/RTC606Dev/Installs/PlainJavaAPI
export PLAIN_JAVA

# In the -Djava.ext.dirs always keep the Java distribution in the front
$JAVA_HOME/bin/java -Djava.security.policy=rmi_no.policy -Djava.ext.dirs=$JAVA_HOME/lib/ext:$JAVA_HOME/jre/lib/ext:./lib:$PLAIN_JAVA -cp ./lib:$PLAIN_JAVA% -jar wcl.jar "$@"
