# build context is the repository root: the example needs the parent pom and the starter module
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY rate-limiter rate-limiter
COPY rate-limiter-example/pom.xml rate-limiter-example/
COPY rate-limiter-example/src rate-limiter-example/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl rate-limiter-example -am package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre AS final
WORKDIR /app
COPY --from=builder /app/rate-limiter-example/target/rate-limiter-example-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
