#!/bin/sh
#JAVA_HOME=/usr/java/jdk1.8.0_20/bin
PLAIN_JAVA=/usr/RTC606Dev/Installs/PlainJavaAPI

#export JAVA_HOME
export PLAIN_JAVA

$JAVA_HOME/bin/java -Djava.security.policy=rmi_no.policy -Djava.ext.dirs=./lib:$PLAIN_JAVA:$JAVA_HOME/jre/lib/ext -cp ./lib:$PLAIN_JAVA% -jar wcl.jar "$@"
