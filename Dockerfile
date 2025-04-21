FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

# Pre-download dependencies without compiling the project
# -B = batch mode (no interactive prompts)
# Speeds up future builds by caching dependencies unless pom.xml changes
RUN mvn dependency:go-offline -B

COPY src ./src

# Clean previous builds, compile the source, and package it into a .jar
# Skip tests for now since there are none implemented
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY --from=build /app/target/*with-dependencies.jar app.jar

EXPOSE 8080

# Define the startup command to run the Java application
ENTRYPOINT ["java", "-jar", "app.jar"]
