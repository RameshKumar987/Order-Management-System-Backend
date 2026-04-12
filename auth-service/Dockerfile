FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /app

# Copy gradle files
COPY gradle gradle
COPY build.gradle settings.gradle gradlew ./

# Give execution permission to gradlew
RUN chmod +x gradlew

# Copy source code
COPY src ./src

# Build the application
RUN ./gradlew build -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8081

# Optimize JVM memory for small Render instances
ENV JAVA_TOOL_OPTIONS="-Xmx300m"

ENTRYPOINT ["java", "-jar", "app.jar"]
