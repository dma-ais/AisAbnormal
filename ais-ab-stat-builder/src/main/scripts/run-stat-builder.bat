@ECHO off
ECHO Starting AIS Abnormal Behaviour statistics builder application

java -jar ais-ab-stat-builder-0.1-SNAPSHOT.jar^
 -downsampling 5^
 -gridsize 200^
 -inputDirectory ../../../../../data/ais/aisdk/2013-06^
 -input *.txt.gz^
 -output stats-2013-06-dwn5-grid200