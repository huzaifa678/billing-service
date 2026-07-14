FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# buf CLI is required to generate the subscription gRPC stubs (see billing-adapters).
ARG BUF_VERSION=1.68.4
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
    && curl -sSL "https://github.com/bufbuild/buf/releases/download/v${BUF_VERSION}/buf-$(uname -s)-$(uname -m)" \
        -o /usr/local/bin/buf \
    && chmod +x /usr/local/bin/buf \
    && rm -rf /var/lib/apt/lists/*

# Build scripts first so dependency resolution is cached independently of sources.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY billing-domain/build.gradle billing-domain/
COPY billing-application/build.gradle billing-application/
COPY billing-adapters/build.gradle billing-adapters/buf.yaml billing-adapters/buf.gen.yaml billing-adapters/
COPY billing-bootstrap/build.gradle billing-bootstrap/
RUN chmod +x gradlew

# Sources for every module.
COPY billing-domain/src billing-domain/src
COPY billing-application/src billing-application/src
COPY billing-adapters/src billing-adapters/src
COPY billing-bootstrap/src billing-bootstrap/src

RUN ./gradlew :billing-bootstrap:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/billing-bootstrap/build/libs/*.jar app.jar

USER appuser

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
