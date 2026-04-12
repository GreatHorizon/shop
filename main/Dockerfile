# =============================================================================
# Stage 1: Build
# =============================================================================
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src src
RUN mvn clean package -DskipTests -B

# =============================================================================
# Stage 2: Runtime
# =============================================================================
FROM eclipse-temurin:21-jre-jammy

# create user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

# create uploads folder HERE
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

COPY --from=builder /build/target/main-0.0.1-SNAPSHOT.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]