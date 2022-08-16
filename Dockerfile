FROM openjdk:8-jdk

COPY target/assess-service-adapter.jar assess-service-adapter.jar

EXPOSE 8080

ENV TZ=Europe/London
ENTRYPOINT ["java","-jar","assess-service-adapter.jar"]
