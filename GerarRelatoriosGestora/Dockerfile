FROM maven:3.9.5-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /build/target/*-jar-with-dependencies.jar api.jar
CMD ["java", "-jar", "api.jar"]