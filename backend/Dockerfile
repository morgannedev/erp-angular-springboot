FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN useradd --system --create-home --home-dir /app algedro
COPY --from=build /app/target/*.jar /app/app.jar
RUN chown -R algedro:algedro /app

USER algedro
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
