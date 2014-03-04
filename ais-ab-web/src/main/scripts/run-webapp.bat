@ECHO off
ECHO Starting AIS Abnormal Behaviour web application

java -jar ais-ab-web-0.1-SNAPSHOT.jar^
 -eventDataRepositoryType h2^
 -eventDataDbFile ../../data/events/test^
 -featureData ../../../../data/features/aisdk-2013H2-grid200-dwn5^
 -port 8080
