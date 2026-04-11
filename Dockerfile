# syntax=docker/dockerfile:1
# 빌드: OpenJDK 17 (Gradle 공식 이미지). 실행: Eclipse Temurin 17 JRE (OpenJDK 배포판)

FROM gradle:8.11.1-jdk17 AS builder
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY src ./src
RUN gradle test bootJar --no-daemon \
    && JAR=$(ls build/libs/*.jar | grep -v -- '-plain.jar' | head -n1) \
    && cp "$JAR" /app/application.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/application.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
