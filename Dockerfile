FROM eclipse-temurin:21-jre-alpine

# Build-time argument — passed in via --build-arg or docker-build-push-action
ARG APP_VERSION

WORKDIR /app
# Uses the ARG to dynamically resolve the correct JAR name
COPY target/service-pulse-app-${APP_VERSION}.jar app.jar

EXPOSE 9092
ENTRYPOINT ["java", "-jar", "service-pulse-app.jar"]
