# syntax=docker/dockerfile:1
FROM openjdk:8-alpine
#use current Java 8 image because RTC and WCL currently run on Java 8 (especially for the java.ext.dirs property)

WORKDIR /app
# Copy the RTC Plain Java Client Libraries into the image
COPY rtc/. /var/plainjava
# Copy the WCL Libraries into the image
COPY WCL/. /app
# Set entrypoint to 
ENTRYPOINT ["./wcl.sh"]