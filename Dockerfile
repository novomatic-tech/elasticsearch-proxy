FROM openjdk:11-jdk-slim

ARG JAR_FILE

WORKDIR /opt/elasticsearch-proxy

COPY target/${JAR_FILE} elasticsearch-proxy.jar

ENV JAVA_OPTS ""

ENTRYPOINT ["java", "$JAVA_OPTS", "-jar", "elasticsearch-proxy.jar"]