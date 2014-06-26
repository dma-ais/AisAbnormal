#!/bin/sh
java \
-d64 -Xmx12G -XX:+UseG1GC \
-jar ais-ab-stat-builder-0.1-SNAPSHOT.jar \
-downsampling 5 \
-gridsize 200 \
-inputDirectory /data/aisdata/abnormal_dump \
-input *.txt.gz \
-output stats-20130610-20140610-dwn5-grid200
