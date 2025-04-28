FROM gradle:8.13-jdk17 AS build
WORKDIR /app
COPY . /app
ENV JAVA_HOME=/opt/java/openjdk
RUN gradle clean :backend:shadowJar --no-daemon

RUN find /app -name "backend.jar" || echo "backend.jar not found"
RUN ls -la /app/backend/build/libs || echo "Directory /app/backend/build/libs not found"

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/backend/build/libs/backend.jar /app/backend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/backend.jar"]