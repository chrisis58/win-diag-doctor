# Build stage
FROM maven:3.9-eclipse-temurin-17 AS maven_builder
WORKDIR /app

COPY pom.xml .
COPY win-diag-doctor-app/pom.xml win-diag-doctor-app/
COPY win-diag-doctor-common/pom.xml win-diag-doctor-common/
COPY win-diag-doctor-probe/pom.xml win-diag-doctor-probe/
COPY win-diag-doctor-protocol/pom.xml win-diag-doctor-protocol/

RUN mvn dependency:go-offline -B

RUN mvn -B -pl win-diag-doctor-probe -Pbundle-exe generate-resources

COPY win-diag-doctor-app/src win-diag-doctor-app/src
COPY win-diag-doctor-common/src win-diag-doctor-common/src
COPY win-diag-doctor-probe/src win-diag-doctor-probe/src
COPY win-diag-doctor-protocol/src win-diag-doctor-protocol/src

RUN mvn package -DskipTests -Pbundle-exe

# Final stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=maven_builder /app/win-diag-doctor-app/target/*.jar app.jar

COPY --from=maven_builder /app/data /app/data

EXPOSE 8093

ENTRYPOINT ["java", "-jar", "app.jar"]
