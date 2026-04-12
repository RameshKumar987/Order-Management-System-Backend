FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /app

COPY gradle gradle
COPY build.gradle settings.gradle gradlew ./
RUN chmod +x gradlew
COPY src ./src
RUN ./gradlew build -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8084
ENV JAVA_TOOL_OPTIONS="-Xmx300m"
ENTRYPOINT ["java", "-jar", "app.jar"]
