FROM gradle:8.14-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :server:bootJar --no-daemon -x test

FROM eclipse-temurin:21.0.7_6-jre-alpine
WORKDIR /app
# Copy the executable JAR from the build stage
COPY --from=build /app/server/build/libs/server-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
