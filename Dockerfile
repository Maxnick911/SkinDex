FROM gradle:8.13-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle clean :backend:shadowJar --no-daemon

FROM openjdk:17-jdk-slim
WORKDIR /app
RUN mkdir -p /app/uploads && chmod 777 /app/uploads

COPY --from=build /app/backend/build/libs/*.jar ./backend.jar

EXPOSE 8080
CMD ["java", "-jar", "/app/backend.jar"]
