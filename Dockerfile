# Stage 1: Build the native image
FROM ghcr.io/graalvm/native-image-community:21 AS builder

WORKDIR /app
COPY . .

# Ensure mvnw has execute permissions and Unix line endings
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
# Compile native image (mounts Maven settings for authenticated repositories like GitHub Packages if provided)
RUN mvn clean install

# Stage 2: Create a lightweight runtime image
FROM ubuntu:noble

WORKDIR /app
# The native executable is generated in the target directory with the name of the artifact
COPY --from=builder /app/target/service-pulse-app /app/service-pulse-app

EXPOSE 9092

ENTRYPOINT ["/app/service-pulse-app"]
