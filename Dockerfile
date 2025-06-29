# -------- Build Stage --------
FROM --platform=linux/amd64 maven:3.9.6-eclipse-temurin-17 as builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package

# -------- Run Stage --------
FROM --platform=linux/amd64 eclipse-temurin:17-jdk
WORKDIR /app

# Copy jar file after build
COPY --from=builder /app/target/*.jar app.jar

# Expose port (optional, but good practice)
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]