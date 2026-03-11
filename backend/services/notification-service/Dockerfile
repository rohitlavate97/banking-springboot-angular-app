# syntax=docker/dockerfile:1
FROM maven:3.9.6-amazoncorretto-17 AS builder
WORKDIR /build
COPY pom.xml pom.xml
COPY backend/pom.xml backend/pom.xml
COPY backend/services/notification-service/pom.xml backend/services/notification-service/pom.xml
RUN mvn -B -f backend/services/notification-service/pom.xml dependency:go-offline -pl . -am
COPY backend/services/notification-service backend/services/notification-service
RUN mvn -B -f backend/services/notification-service/pom.xml clean package -DskipTests

FROM amazoncorretto:17-alpine3.19
WORKDIR /app
RUN addgroup -S banking && adduser -S banking -G banking
USER banking
COPY --from=builder --chown=banking:banking /build/backend/services/notification-service/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]
