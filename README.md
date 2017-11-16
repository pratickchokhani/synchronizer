# synchronizer
Synchronizes folder across two systems (master and slave). There is no distinction between master and slave, 
just that master creates ServerSocket and client connects to it.

There are few caveats in the current implementation:
1. Once the connection is disconnected, the system does not establish the connection again and server in both the systems will have to restart. 
This is because of time constraint and can be taken care of in the next version.
2. The content of the file is not checked while comparing and only the latest version is kept.


Requirements: maven, Java 1.8 or later

To build the Jar file:
mvn clean package

java -classpath "Path to jar file" com.socialcops.main.Main

Configuration can be set by placing the file "config.properties" in the directory from where jar file is executed.
The program must have write permission to the folder from where it is executing as it will stote log files in the location.

Sample config file:
# Specifies if the current system is master or slave
master=slave or master=master
# IP address to Master
server.ip=127.0.0.1 
# Port of master
server.port=5003
 # Path to the folder that is to be synced
sync.folder=data
# Time interval in which schedule for listening is to be executed
schedule.delay.in.millis=2000 
# Time interval in which file difference is to be calculated
data.delivery.delay=120000