FROM openjdk:11-jdk-slim

ARG JAR_FILE

COPY target/${JAR_FILE} /opt/elasticsearch-proxy.jar

ENTRYPOINT ["java", "-jar", "/opt/elasticsearch-proxy.jar"]