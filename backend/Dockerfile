# Build stage. Tests are not run here: they need Docker (Testcontainers) and run with ./mvnw verify.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
RUN groupadd --system vesta && useradd --system --gid vesta --no-create-home vesta
WORKDIR /app
COPY --from=build /workspace/target/vesta-backend-*.jar app.jar
USER vesta
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
