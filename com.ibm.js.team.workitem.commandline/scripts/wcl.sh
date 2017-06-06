#!/bin/sh
JAVA_HOME=../TeamConcert/jazz/client/eclipse/jdk
PLAIN_JAVA=../PlainJavaAPI
export JAVA_HOME
export PLAIN_JAVA
env
$JAVA_HOME/bin/java -Djava.security.policy=rmi_no.policy -Djava.ext.dirs=./lib:$PLAIN_JAVA:$JAVA_HOME/jre/lib/ext -cp ./lib:$PLAIN_JAVA% -jar wcl.jar "$@"
