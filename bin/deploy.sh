#!/bin/bash

git pull

make build

lsof -t -i:5000 | xargs kill -9

nohup java -jar target/legomenon-*.jar > nohup.out 2>&1 &
