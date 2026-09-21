# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy maven wrapper and pom
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Ensure mvnw has execution permissions
RUN chmod +x mvnw

# Download dependencies (caching layer)
RUN ./mvnw dependency:go-offline

# Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-privileged user to run the application
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy the built jar from stage 1
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# The application expects configuration via environment variables
ENTRYPOINT ["java", "-jar", "app.jar"]
