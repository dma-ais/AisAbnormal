AisAbnormal
===========

System to detect abnormal vessel behavior based on historical AIS data

## Introduction ##

TBD

## Prerequisites ##

* Java 1.7
* Maven 3

## Building ##

        mvn clean install
        
## Devloping in Eclipse ##

Either use Eclipse M2 plugin or Maven eclipse target

    mvn eclipse:eclipse
    
## Running ##

Executable jars will be created for the command line application. E.g.

    java -jar ais-ab-stat-builder/target/ais-ab-stat-builder-0.1-SNAPSHOT.jar
    
## Modules ###

###ais-ab-common###

All common resources for the project.

###ais-ab-stat-builder###

Command line application building/updating a statistics database using historical AIS data from a file.

###ais-ab-stat-db###

Interface and implementation of a statistics database API. Suitable database? The database must support infrequent writes and frequent reads, which may not be the primary objective of many No-SQL databases. 

###ais-ab-analyzer###

Command line tool to analyze AIS data for abnormal events. In some cases based on the statistical database. The tool must have two modes of working:
On request - A historical dataset is analyzed and and a list of events is output in appropriate format.
Real-time - A live data stream is analyzed and events are saved to a database and maybe distributed by email or other mean
The analyzer produces Event objects that describe the event type, the involved parties, the time and location, and the statistic used as a basis for the event.

###ais-ab-event-db###

Interface and implementation of an event database API. Suitable database? The database must support frequent writes and infrequent reads. Some No-SQL DB may be a good choice.

###ais-ab-web###

Web application for presenting statistical data and events using OpenLayers and appropriate WMS layer for geospatial presentation. A loosly coupled frontend and backend is to be used with JAX-RS rest API as backend.

To run Jetty servlet container with automatic redeploy in development:
    
     mvn clean jetty:run


