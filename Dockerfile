ARG CGV=ghcr.io/graalvm/graalvm-community:21

FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -Pnative -B

FROM ${CGV} AS native
WORKDIR /build
COPY --from=builder /build/target/*.jar app.jar
RUN native-image \
    -jar app.jar \
    -H:Name=application \
    -H:Class=com.mcporchestrator.McpContextOrchestratorApplication \
    --no-fallback \
    --enable-url-protocols=http \
    -H:+ReportUnsupportedElementsAtRuntime \
    -H:+AllowVMInspection \
    -J-Xmx6g

FROM gcr.io/distroless/base-debian12:nonroot
WORKDIR /app
COPY --from=native /build/application application
EXPOSE 8080 9090
ENTRYPOINT ["/app/application"]
