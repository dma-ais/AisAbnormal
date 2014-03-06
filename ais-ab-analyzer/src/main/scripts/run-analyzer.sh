#!/bin/sh
java -jar ais-ab-analyzer-0.1-SNAPSHOT.jar \
-downsampling 5 \
-eventDataRepositoryType h2 \
-eventDataDbFile events \
-featureData stats-2013-06-dwn5-grid200 \
-aisDataSourceURL file://../../../../../data/ais/aisdk/2013-06/*.txt.gz
