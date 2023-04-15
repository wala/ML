#!/bin/bash

ME=`realpath $0`
DIR=`dirname $ME`

cat -u | tee -a /tmp/lsp.in.log | $JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:6660,server=y,suspend=n -jar $DIR/target/com.ibm.wala.cast.python.ml-0.0.1-SNAPSHOT-shaded.jar --mode stdio | tee -a /tmp/lsp.out.log
