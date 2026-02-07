# Build stage - use Maven image since this service doesn't have Maven wrapper
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY order-service/pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY order-service/src ./src
RUN mvn package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=build /app/target/*.jar app.jar

# Copy OpenTelemetry Java agent from shared location
COPY opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

EXPOSE 8085

# JVM memory limits for resource control
ENV JAVA_OPTS="-Xmx256m -Xms128m"

# OpenTelemetry Java Agent configuration (gRPC protocol)
ENV OTEL_SERVICE_NAME="order-service" \
    OTEL_EXPORTER_OTLP_ENDPOINT="http://grafana-alloy:4317" \
    OTEL_EXPORTER_OTLP_PROTOCOL="grpc" \
    OTEL_METRICS_EXPORTER="otlp" \
    OTEL_LOGS_EXPORTER="otlp" \
    OTEL_TRACES_EXPORTER="otlp"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:/app/opentelemetry-javaagent.jar -jar app.jar"]
