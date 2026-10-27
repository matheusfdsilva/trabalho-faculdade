FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S -g 10001 app && adduser -S -D -u 10001 -G app app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/mercadinho-cerrado-0.1.0-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/login.html || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
