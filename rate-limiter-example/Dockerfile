# build context is the repository root: the starter is installed first, then the example is built against it
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY rate-limiter-starter rate-limiter-starter
COPY rate-limiter-example/pom.xml rate-limiter-example/
COPY rate-limiter-example/src rate-limiter-example/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B install -Dmaven.test.skip=true -Dgpg.skip=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true \
    && mvn -B -f rate-limiter-example/pom.xml package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre AS final
WORKDIR /app
COPY --from=builder /app/rate-limiter-example/target/rate-limiter-example-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
