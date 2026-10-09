FROM eclipse-temurin:21-jdk@sha256:3e3c176ffed168beb42c607be9bc1639b466cf00261a0fb04425562c9d0c5c2b AS build
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

# The bootJar build resolves the build.buf.gen:* dependency from buf.build/gen/maven
# (HTTP header auth) and runs `buf generate` against remote plugins, both of which need
# the BSR token. It is mounted as a BuildKit secret so it never lands in an image layer.
RUN --mount=type=secret,id=buf_token \
    export BUF_TOKEN="$(cat /run/secrets/buf_token)" \
    && export ORG_GRADLE_PROJECT_bufAuthHeaderName=Authorization \
    && export ORG_GRADLE_PROJECT_bufAuthHeaderValue="Bearer ${BUF_TOKEN}" \
    && ./gradlew :billing-bootstrap:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine@sha256:51ab5e3302e7141ce665ca3ea85e8b5cd648eafbc3c0c90dd79d6537684e4555 AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/billing-bootstrap/build/libs/*.jar app.jar

USER appuser

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
