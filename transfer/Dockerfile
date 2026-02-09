FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
RUN unzip app.jar -d unpacked_jar
ENTRYPOINT ["java", "-cp", "unpacked_jar/BOOT-INF/classes:unpacked_jar/BOOT-INF/lib/*", "io.github.habatoo.TransferApplication"]