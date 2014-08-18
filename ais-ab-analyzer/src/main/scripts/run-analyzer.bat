@ECHO off
ECHO Starting AIS Abnormal event analyzer application

 java -jar ais-ab-analyzer-0.1-SNAPSHOT.jar^
  -downsampling 2^
  -eventDataRepositoryType h2^
  -eventDataDbFile events^
  -statistics stats-2013-06-dwn5-grid200^
  -aisDataSourceURL file://../../../../../data/ais/aisdk/2013-06/*.txt.gz
