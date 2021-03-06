FROM openjdk:8-jdk

COPY target/assess-service-adapter.jar assess-service-adapter.jar

EXPOSE 8080

ENV TZ=UTC
ENTRYPOINT ["java","-jar","assess-service-adapter.jar"]
