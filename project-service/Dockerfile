FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl project-service -am -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/project-service/target/project-service-1.0.0.jar app.jar
EXPOSE 8002
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
