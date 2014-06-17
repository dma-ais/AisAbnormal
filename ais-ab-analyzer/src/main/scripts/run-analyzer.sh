#!/bin/sh
java -jar ais-ab-analyzer-0.1-SNAPSHOT.jar \
-downsampling 5 \
-eventDataRepositoryType pgsql \
-eventDataDbHost ais-lin-u002 \
-eventDataDbPort 8432 \
-eventDataDbUsername abnormal \
-eventDataDbPassword "replace_with_secret" \
-eventDataDbName abnormal \
-statistics ~/AisAbnormal/data/2013H2-grid200-down10.MapDB-1_0_1 \
-aisDataSourceURL tcp://10.33.128.144:65261
