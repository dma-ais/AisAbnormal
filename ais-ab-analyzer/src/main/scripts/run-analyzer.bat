@ECHO off
ECHO Starting AIS Abnormal event analyzer application

 java -jar ais-ab-analyzer-0.1-SNAPSHOT.jar^
  -downsampling 5^
  -eventDataRepositoryType h2^
  -eventDataDbFile events^
  -featureData stats-2013-06-dwn5-grid200^
  -inputDirectory ../../../../../data/ais/aisdk/2013-06^
  -input *.txt.gz

