# =============================================================================
# Backend Dockerfile — Multi-stage build
# Stage 1: Build the fat JAR with Maven
# Stage 2: Minimal JRE runtime image
#
# WHY MULTI-STAGE BUILD?
# The Maven builder image includes: Maven, Java SDK, all tool binaries → ~600MB
# The runtime image needs only: Java JRE → ~200MB
# Result: 3x smaller image = faster pulls, less attack surface, cheaper storage.
# =============================================================================

# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml FIRST and download dependencies (Docker cache optimization).
# WHY? Docker caches each layer. If source code changes but pom.xml doesn't,
# Docker reuses the cached dependency layer → much faster rebuilds.
# If we copied everything at once, any source change invalidates the dep cache.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source code and build
COPY src ./src
RUN mvn package -DskipTests -B


# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
# eclipse-temurin:21-jre-jammy:
# - eclipse-temurin: OpenJDK distribution by Eclipse Foundation (free, enterprise-grade)
# - 21: Java 21 (Virtual Threads support)
# - jre: Java Runtime Environment (no compiler, no Maven) → smaller than JDK
# - jammy: Ubuntu 22.04 LTS base (stable, well-supported)
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create a non-root user for security.
# WHY? If the container is compromised, a non-root attacker has limited access.
# Running as root = full container control = potential host system access.
RUN groupadd -r myshop && useradd -r -g myshop myshop

# Copy ONLY the built JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Change ownership to the non-root user
RUN chown myshop:myshop app.jar

USER myshop

# Port the Spring Boot app listens on (configured in application.yml)
EXPOSE 8080

# JVM flags explanation:
# -XX:+UseContainerSupport: JVM respects Docker memory limits (default in Java 11+)
# -XX:MaxRAMPercentage=75.0: Use 75% of container memory for heap
# -Djava.security.egd: Faster random number generation (for UUID, JWT signing)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
