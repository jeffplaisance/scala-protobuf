#!/bin/sh
SCRIPT=`readlink -f $0`
SCRIPTPATH=`dirname $SCRIPT`

CLASSPATH=${SCALA_HOME}/lib/scala-library.jar
for i in ${SCRIPTPATH}/../lib/*.jar
do
  CLASSPATH=${CLASSPATH}:${i}
done

java -Xmx512M -cp ${CLASSPATH} protobuf.compiler.ScalaProtoWrapperGenerator