FROM openjdk:11-jdk-slim

ARG JAR_FILE

WORKDIR /opt/elasticsearch-proxy

COPY target/${JAR_FILE} elasticsearch-proxy.jar

ENTRYPOINT ["java", "-jar", "elasticsearch-proxy.jar"]