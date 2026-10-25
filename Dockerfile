FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .
RUN chmod +x mvnw && ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/heardYou-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
# Use shell form so ${PORT} is expanded by the shell at runtime.
# Render sets PORT to the port it expects the container to bind; Spring Boot also
# reads it via application.properties server.port=${PORT:8080}, but passing it
# explicitly as a JVM system property is belt-and-suspenders.
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
