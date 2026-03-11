# syntax=docker/dockerfile:1
FROM maven:3.9.6-amazoncorretto-17 AS builder
WORKDIR /build
COPY pom.xml pom.xml
COPY backend/pom.xml backend/pom.xml
COPY backend/infrastructure/config-server/pom.xml backend/infrastructure/config-server/pom.xml
RUN mvn -B -f backend/infrastructure/config-server/pom.xml dependency:go-offline -pl . -am
COPY backend/infrastructure/config-server backend/infrastructure/config-server
RUN mvn -B -f backend/infrastructure/config-server/pom.xml clean package -DskipTests

FROM amazoncorretto:17-alpine3.19
WORKDIR /app
RUN addgroup -S banking && adduser -S banking -G banking
USER banking
COPY --from=builder --chown=banking:banking /build/backend/infrastructure/config-server/target/*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]
