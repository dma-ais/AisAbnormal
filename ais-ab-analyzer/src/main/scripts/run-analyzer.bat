@ECHO off
ECHO Starting AIS Abnormal event analyzer application

java -Dlog4j.configuration %2 -jar ais-ab-analyzer-0.1-SNAPSHOT.jar -config %1

