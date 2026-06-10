# Ziyarah Backend Dockerfile
# Multi-stage build for Spring Boot application (Gradle-only)

# =============================================================================
# Build Stage
# =============================================================================
FROM gradle:8.12-jdk21 AS build

WORKDIR /app

# Build scripts
COPY --chown=gradle:gradle build.gradle.kts settings.gradle.kts ./

# Application source
COPY --chown=gradle:gradle src ./src

# Build the Spring Boot fat jar (skip tests for container builds)
# Cache Gradle deps and wrapper to avoid redownloading on rebuilds (BuildKit)
RUN --mount=type=cache,target=/home/gradle/.gradle/caches,uid=1000,gid=1000 \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper,uid=1000,gid=1000 \
    gradle bootJar -x test --no-daemon

# =============================================================================
# Runtime Stage
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# curl: health checks; su-exec: drop root after fixing volume permissions for /data/media
RUN apk add --no-cache curl su-exec

# Create non-root user for security
RUN addgroup -S ziyarah && adduser -S ziyarah -G ziyarah

# Copy boot jar from build stage.
# Matches both versioned releases (core-1.0.0.jar) and snapshots (core-1.0.0-SNAPSHOT.jar).
# The plain jar produced by the standard 'jar' task is excluded because its name never matches core-*.jar alone.
COPY --from=build /app/build/libs/core-*.jar app.jar

COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh && chown -R ziyarah:ziyarah /app

# Container starts as root so entrypoint can chown the upload volume; process becomes ziyarah.
# Expose application port
EXPOSE 8080

# JVM options for containerized environment (overridable)
ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseContainerSupport"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["/docker-entrypoint.sh"]
