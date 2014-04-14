#!/usr/bin/env bash

function die() {
  echo $*
  exit 1
}

function installsmack() {
  mvn install:install-file -Dfile=smack-3.4.1-0cec571.jar -DgroupId=org.jivesoftware -DartifactId=smack -Dversion=3.4.1-0cec571 -Dpackaging=jar || die "Could not install smack"
  mvn install:install-file -Dfile=smackx-3.4.1-0cec571.jar -DgroupId=org.jivesoftware -DartifactId=smackx -Dversion=3.4.1-0cec571 -Dpackaging=jar || die "Could not install smackx"
}

test -f target/*shade* || installsmack
mvn install -Dmaven.test.skip=true

jar=`ls target/*shade.jar | sort | tail -n 1`

echo "Running jar $jar"
javaArgs="-XX:+HeapDumpOnOutOfMemoryError -jar $jar $*"

java $javaArgs || die "Could not run java"
