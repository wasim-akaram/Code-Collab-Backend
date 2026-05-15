# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy all source code from backend directory
COPY . .

# First, install common-lib to the local Maven repository
RUN --mount=type=cache,target=/root/.m2 mvn -s settings.xml -f common-lib/pom.xml clean install -DskipTests

# Define an argument to select which service to build
ARG SERVICE_NAME

# Build the selected service
RUN --mount=type=cache,target=/root/.m2 mvn -s settings.xml -f ${SERVICE_NAME}/pom.xml clean package -DskipTests

# Stage 2: Create a lightweight runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Define the service name again for this stage
ARG SERVICE_NAME

# Copy the compiled jar from the builder stage
COPY --from=builder /app/${SERVICE_NAME}/target/*.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
