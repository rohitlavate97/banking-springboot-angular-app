# syntax=docker/dockerfile:1
# Multi-stage build for auth-service
FROM maven:3.9.6-amazoncorretto-17 AS builder
WORKDIR /build

# Cache dependencies first
COPY pom.xml pom.xml
COPY backend/pom.xml backend/pom.xml
COPY backend/services/auth-service/pom.xml backend/services/auth-service/pom.xml
RUN mvn -B -f backend/services/auth-service/pom.xml dependency:go-offline -pl . -am

COPY backend/services/auth-service backend/services/auth-service
RUN mvn -B -f backend/services/auth-service/pom.xml clean package -DskipTests

# Runtime image
FROM amazoncorretto:17-alpine3.19
WORKDIR /app

RUN addgroup -S banking && adduser -S banking -G banking
USER banking

COPY --from=builder --chown=banking:banking /build/backend/services/auth-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
