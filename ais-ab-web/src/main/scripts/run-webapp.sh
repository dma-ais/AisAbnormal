#!/bin/sh
java -jar ais-ab-web-0.1-SNAPSHOT.jar \
-eventDataRepositoryType pgsql \
-eventDataDbHost $2 \
-eventDataDbPort $3 \
-eventDataDbUsername $4 \
-eventDataDbPassword $5 \
-eventDataDbName $6 \
-statistics $7 \
-port $1 
