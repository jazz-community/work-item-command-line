#!/bin/sh
JAVA_HOME=/usr/bin/java
export JAVA_HOME
PLAIN_JAVA=/var/plainjava
export PLAIN_JAVA

# In the -Djava.ext.dirs always keep the Java distribution in the front
$JAVA_HOME/bin/java -Djava.security.policy=rmi_no.policy -cp $JAVA_HOME/lib/ext:$JAVA_HOME/jre/lib/ext:./lib:$PLAIN_JAVA:wcl.jar" com.ibm.js.team.workitem.commandline.WorkitemCommandLine "$@"
