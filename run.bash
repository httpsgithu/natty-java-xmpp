#!/usr/bin/env bash

function die() {
  echo $*
  exit 1
}
mvn install -Dmaven.test.skip=true

jar=`ls target/*shade.jar | sort | tail -n 1`

echo "Running jar $jar"
javaArgs="-XX:+HeapDumpOnOutOfMemoryError -jar $jar $*"

java $javaArgs || die "Could not run java"
