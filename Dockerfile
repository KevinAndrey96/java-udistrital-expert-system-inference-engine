# Multi-stage Docker build for Universidad Distrital Inference Engine
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build
COPY pom.xml AbsoluteLayout.jar ./
COPY src ./src

# Install system dependency or compile
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=builder /build/target/*.jar ./app.jar
COPY --from=builder /build/AbsoluteLayout.jar ./AbsoluteLayout.jar

LABEL maintainer="Universidad Distrital Francisco Jose de Caldas - Kevin Andrey"
LABEL description="Expert System & Inference Engine (Forward, Backward, Hybrid, Certainty Factors)"

ENTRYPOINT ["java", "-cp", "app.jar:AbsoluteLayout.jar", "edu.udistrital.expert.cli.ExpertSystemCli"]
CMD ["--demo"]
