FROM maven:3.8.4-openjdk-17-slim AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn package -DskipTests

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/target/user-ms-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8001

CMD ["java", "-jar", "/app/app.jar"]
