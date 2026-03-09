# Stage 1: Build
FROM gradle:8.12-jdk17 AS builder
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN rm -f gradle.properties
RUN gradle build --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
