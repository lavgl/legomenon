#!/bin/bash

git pull

make build

PID=$(lsof -i -P -n | grep 5000 | awk '{ print $2 }')
kill -9 $PID

find . -name "*.jar" | xargs -n 1 java -jar
