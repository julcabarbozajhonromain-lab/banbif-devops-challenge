# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy Maven descriptor first to leverage Docker layer cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source and package JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Create a non-privileged group and user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the compiled artifact from builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar /app/app.jar

# Switch to non-root user
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
