#!/bin/bash

ME=`realpath $0`
DIR=`dirname $ME`

cat -u | tee -a /tmp/lsp.in.log | $JAVA_HOME/bin/java -cp $DIR/target/com.ibm.wala.cast.python-0.0.1-SNAPSHOT.jar com.ibm.wala.cast.python.PythonDriver | tee -a /tmp/lsp.out.log
