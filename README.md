# MCP Context Orchestrator

[![Java](https://img.shields.io/badge/Java-21-%23ED8B00?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Kafka-3.6-231F20?logo=apachekafka)](https://kafka.apache.org/)
[![GraalVM](https://img.shields.io/badge/GraalVM-21-FF2600?logo=graalvm)](https://www.graalvm.org/)
[![gRPC](https://img.shields.io/badge/gRPC-1.62-4285F4)](https://grpc.io/)
[![Build](https://github.com/anomalyco/mcp-context-orchestrator/actions/workflows/ci.yml/badge.svg)](https://github.com/anomalyco/mcp-context-orchestrator/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                  Client Layer (REST / gRPC)                      │
├─────────────────────────────────────────────────────────────────┤
│                    Application Layer                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ QueryService │  │ IngestRes.   │  │    Domain Events      │  │
│  │  (UseCase)   │  │  (UseCase)   │  │ ResourceIngestedEvt   │  │
│  └──────┬───────┘  └──────┬───────┘  │ QueryExecutedEvt      │  │
│         │                  │          └──────────────────────┘  │
├─────────┼──────────────────┼────────────────────────────────────┤
│         │    Ports         │                                     │
│  ┌──────┴───────┐  ┌──────┴───────┐  ┌──────────────────┐     │
│  │ ResourceRepo │  │ CacheService │  │  AuditService    │     │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘     │
├─────────┼──────────────────┼───────────────────┼────────────────┤
│  Infra  │     Adapters     │                   │                │
│  ┌──────┴───────┐  ┌──────┴───────┐  ┌────────┴────────┐     │
│  │ DB Adapters  │  │ CacheConfig  │  │ KafkaAuditBus   │     │
│  │ Pg / MySQL / │  │ (Caffeine)   │  │ Producer/Cons.  │     │
│  │   MongoDB    │  └──────────────┘  └─────────────────┘     │
│  └──────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

## Features

- **Multi-source context building** – Query across PostgreSQL, MySQL, and MongoDB
- **Event-driven auditing** – All queries/ingests emitted as Kafka events
- **gRPC + REST** – Dual protocol support
- **GraalVM native-image** – Sub-second startup, minimal memory
- **Clean Architecture** – Domain isolated from infrastructure
- **Multi-profile** – Switch databases with `SPRING_PROFILES_ACTIVE`

## Quick Start

```bash
# Clone
git clone https://github.com/anomalyco/mcp-context-orchestrator.git
cd mcp-context-orchestrator

# Full stack
docker compose up -d

# Or local dev
cp .env.example .env
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## API Endpoints

### POST /api/mcp/query
Search resources and build context.

```json
{
  "query": "machine learning pipelines",
  "sources": ["postgres", "mongo"],
  "maxTokens": 2000,
  "filters": { "type": "documentation" }
}
```

### POST /api/mcp/ingest
Ingest a new resource.

```json
{
  "name": "ml-pipeline-guide",
  "type": "documentation",
  "content": "Complete guide to ML pipelines...",
  "source": "internal-wiki",
  "ttlSeconds": 3600
}
```

### GET /api/mcp/resources/{id}
Retrieve a resource by UUID.

## Profiles

| Profile   | Database   | Port |
|-----------|------------|------|
| `postgres`| PostgreSQL | 5433 |
| `mysql`   | MySQL      | 3307 |
| `mongo`   | MongoDB    | 27018|

## Build Native Image

```bash
mvn package -Pnative -DskipTests
docker build -t mcp-orchestrator .
```

## Tech Stack

- **Java 21** – Virtual threads, records, sealed classes
- **Spring Boot 3.3.5** – Auto-config, Actuator
- **Apache Kafka** – Async audit event bus
- **gRPC 1.62.2** – High-performance RPC (future use)
- **GraalVM 21** – Native compilation
- **Testcontainers** – Integration tests
- **JaCoCo** – Code coverage
- **Checkstyle / PMD** – Static analysis
