# syntax=docker/dockerfile:1
FROM gradle:9.1.0-jdk21 AS build

WORKDIR /app

COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
COPY . .

RUN ./gradlew build -x test --parallel

FROM amazoncorretto:21-alpine3.20-jdk

WORKDIR /app

COPY --from=builder /app/${SERVICE_NAME}/build/libs/*.jar app.jar

ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]