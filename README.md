# pgjdbc-ng-jts
Support for Java Topology Suite (JTS) types for the pgjdbc-ng driver (https://github.com/impossibl/pgjdbc-ng).
This does a direct conversion from WKB to JTS, avoiding the overhead of converting to Postgis JDBC types which libraries like done in Hibernate Spatial.

