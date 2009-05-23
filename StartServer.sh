#!/bin/sh

export LD_LIBRARY_PATH=/usr/lib/jvm/java-6-sun/jre/lib/i386/server:/home/blezek/Source/fuse4j/native

# /home/blezek/Source/fuse4j/native/javafs -Corg/xnat/xnatfs/xnatfs -J-Djava.class.path=`pwd`/xnatfs.jar:. xnatfs.mount -f


java -classpath xnatfs.jar:. org.xnat.xnatfs.xnatfs xnatfs.mount -f