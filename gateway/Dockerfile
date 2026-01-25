FROM gradle:9.1.0-jdk21 AS build

WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

COPY microservice-chassis/ microservice-chassis/
COPY account/ account/
COPY gateway/ gateway/
COPY cash/ cash/
COPY notification/ notification/
COPY front-ui/ front-ui/
COPY transfer/ transfer/

RUN ./gradlew :gateway:bootJar -x test --no-daemon

FROM amazoncorretto:21-alpine3.20-jdk

WORKDIR /app

COPY --from=build /app/gateway/build/libs/*.jar app.jar

ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom \
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -Dfile.encoding=UTF-8"

RUN apk add --no-cache curl

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]