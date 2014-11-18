@ECHO off
ECHO Starting AIS Abnormal event analyzer application

java -Dlog4j.debug -jar ais-ab-analyzer-0.1-SNAPSHOT.jar -config %1

