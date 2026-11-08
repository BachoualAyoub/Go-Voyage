# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-noble AS compilation
WORKDIR /projet
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 chmod +x mvnw && ./mvnw -B -ntp package

FROM eclipse-temurin:21-jre-noble
WORKDIR /app
COPY --from=compilation /projet/target/backend-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
