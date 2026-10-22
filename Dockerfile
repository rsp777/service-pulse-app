FROM gcr.io/distroless/java21-debian12:nonroot

WORKDIR /app

# Copy the built application JAR (supports CI app.jar or Maven service-pulse-app-*.jar)
COPY target/service-pulse-app-*.jar service-pulse-app.jar

EXPOSE 9092

ENTRYPOINT ["java", "-jar", "service-pulse-app.jar"]
