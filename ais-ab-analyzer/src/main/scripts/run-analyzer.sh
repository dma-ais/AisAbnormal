#!/bin/sh
java -jar ais-ab-analyzer-0.1-SNAPSHOT.jar \
-downsampling 2 \
-eventDataRepositoryType pgsql \
-eventDataDbHost $1 \
-eventDataDbPort $2 \
-eventDataDbUsername $3 \
-eventDataDbPassword $4 \
-eventDataDbName $5 \
-statistics $6 \
-aisDataSourceURL $7
