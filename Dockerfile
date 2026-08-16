FROM openjdk:17-jdk-alpine
WORKDIR /to-do-service
MAINTAINER ravindra
COPY target/to-do-0.0.1-SNAPSHOT.jar  /to-do-service/to-do-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","to-do-0.0.1-SNAPSHOT.jar"]
EXPOSE 8084
