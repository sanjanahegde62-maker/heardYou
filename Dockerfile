FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .
RUN chmod +x mvnw && ./mvnw -B clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/heardYou-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
