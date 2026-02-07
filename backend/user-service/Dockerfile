# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY user-service/.mvn/ .mvn/
COPY user-service/mvnw user-service/pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build
COPY user-service/src ./src
RUN ./mvnw package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=build /app/target/*.jar app.jar

# Copy OpenTelemetry Java agent from shared location
COPY opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

EXPOSE 8081

# JVM memory limits for resource control
ENV JAVA_OPTS="-Xmx256m -Xms128m"

# OpenTelemetry Java Agent configuration (gRPC protocol)
ENV OTEL_SERVICE_NAME="user-service" \
    OTEL_EXPORTER_OTLP_ENDPOINT="http://grafana-alloy:4317" \
    OTEL_EXPORTER_OTLP_PROTOCOL="grpc" \
    OTEL_METRICS_EXPORTER="otlp" \
    OTEL_LOGS_EXPORTER="otlp" \
    OTEL_TRACES_EXPORTER="otlp"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:/app/opentelemetry-javaagent.jar -jar app.jar"]
