#! /bin/bash

mvn package
mv target/TBar-0.0.1-SNAPSHOT.jar target/dependency/TBar-0.0.1-SNAPSHOT.jar
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

if [ "$1" == "run" ]; then
  bash ./run.sh $2 $3
fi
