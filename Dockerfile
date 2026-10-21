FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built application JAR (supports CI app.jar or Maven service-pulse-app-*.jar)
COPY target/*app*.jar app.jar

EXPOSE 9092

ENTRYPOINT ["java", "-jar", "app.jar"]
