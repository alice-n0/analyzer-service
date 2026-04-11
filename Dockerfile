# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Jenkins에서 생성된 JAR 복사
COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
