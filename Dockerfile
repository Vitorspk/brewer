# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests for faster build, tests run separately)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for healthcheck (Alpine doesn't include it by default)
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Create photos directory and copy mock images (before switching to spring user)
RUN mkdir -p /home/spring/.brewerfotos && chown -R spring:spring /home/spring/.brewerfotos

USER spring:spring

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy mock images to photos directory
COPY --from=build --chown=spring:spring /app/src/main/resources/static/fotos/*mock*.png /home/spring/.brewerfotos/

# Expose port
EXPOSE 8080

# Health check using curl (more reliable than wget in Alpine)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM configuration for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -Djava.security.egd=file:/dev/./urandom"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:prod} -jar app.jar"]