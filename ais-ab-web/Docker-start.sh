#!/bin/sh
cd /AisAbnormal/ais-ab-web/target/ais-ab-web-0.1-SNAPSHOT
./run-webapp.sh 80 $DB_HOST $DB_PORT $DB_USER $DB_PASS $DB_INST $STAT_FILE
