FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./

RUN mvn -q -DskipTests dependency:go-offline

COPY src src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/SpringBootProject-0.0.1-SNAPSHOT.war /app/app.war

RUN mkdir -p /mnt/uploads/covers /mnt/uploads/pages /run/secrets

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.war"]
