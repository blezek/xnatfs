#!/bin/sh

export DYLD_LIBRARY_PATH=/usr/lib/jvm/java-6-sun/jre/lib/i386/server:/home/blezek/Source/fuse4j/native

JAVA_HOME=/usr/lib/java
DYLD_LIBRARY_PATH=$FUSE_HOME/lib:${JAVA_HOME}/jre/lib/i386/server
export LD_LIBRARY_PATH

# /home/blezek/Source/fuse4j/native/javafs -Corg/xnat/xnatfs/xnatfs -J-Djava.class.path=`pwd`/xnatfs.jar:. xnatfs.mount -f


java -classpath xnatfs.jar:. org.xnat.xnatfs.xnatfs xnatfs.mount -f
