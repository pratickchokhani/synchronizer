# synchronizer
Synchronizes folder across two systems (master and slave). There is no distinction between master and slave, 
just that master creates ServerSocket and client connects to it

Requirements: maven, Java 1.8 or later

To build the Jar file:
mvn clean package

java -classpath "Path to jar file" com.socialcops.main.Main

Configuration can be set by placing the file "config.properties" in the directory from where jar file is executed.

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