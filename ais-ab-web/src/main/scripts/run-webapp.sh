#!/bin/sh
java -jar ais-ab-web-0.1-SNAPSHOT.jar \
-eventDataRepositoryType h2 \
-eventDataDbFile $2 \
-statistics $3 \
-port $1 
