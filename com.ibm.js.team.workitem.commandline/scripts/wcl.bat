set JAVA_HOME=..\TeamConcert\jazz\client\eclipse\jdk
set PLAIN_JAVA=../PlainJavaAPI

%JAVA_HOME%\jre\bin\java -Djava.security.policy=rmi_no.policy -Djava.ext.dirs=./lib;%PLAIN_JAVA%;%JAVA_HOME%/jre/lib/ext -cp ./lib;%PLAIN_JAVA% -jar wcl.jar %*
