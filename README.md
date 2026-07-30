# MCP Context Orchestrator

![Java 21](https://img.shields.io/badge/Java-21-%23ED8B00?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?logo=springboot)
![Kafka](https://img.shields.io/badge/Kafka-3.6-231F20?logo=apachekafka)
![GraalVM](https://img.shields.io/badge/GraalVM-21-FF2600?logo=graalvm)
![gRPC](https://img.shields.io/badge/gRPC-1.62-4285F4)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)
![MongoDB](https://img.shields.io/badge/MongoDB-7.0-green?logo=mongodb)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)
![Micrometer](https://img.shields.io/badge/Micrometer-1.13-brightgreen)
![Prometheus](https://img.shields.io/badge/Prometheus-2.53-E6522C)
![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-1.37-4A154B?logo=opentelemetry)

**Orquestrador de contexto MCP (Model Context Protocol).** Consulta e ingestão de recursos em múltiplos bancos de dados (PostgreSQL, MySQL, MongoDB) com cache inteligente, auditoria baseada em eventos Kafka e suporte a gRPC + REST.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Arquitetura](#arquitetura)
- [Funcionalidades](#funcionalidades)
- [Stack Tecnológica](#stack-tecnológica)
- [Pré-requisitos](#pré-requisitos)
- [Início Rápido](#início-rápido)
- [Endpoints da API](#endpoints-da-api)
- [Peris de Banco de Dados](#peris-de-banco-de-dados)
- [Build Nativo (GraalVM)](#build-nativo-graalvm)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Testes](#testes)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Contribuição](#contribuição)
- [Licença](#licença)

---

## Visão Geral

O MCP Context Orchestrator centraliza consultas a múltiplas fontes de dados, permitindo que sistemas de IA (como assistentes MCP) busquem contexto enriquecido de onde quer que os dados estejam. Cada operação é auditada via Kafka, e um cache inteligente (Caffeine) reduz a latência de consultas repetidas.

### Casos de Uso

- Assistentes de IA que precisam consultar bancos heterogêneos
- Middleware de contexto para sistemas MCP (Model Context Protocol)
- Plataformas de busca unificada com cache e auditoria
- Migração gradual de banco de dados (rota seletiva por perfil)
- Sistemas multi-tenant com isolamento por fonte de dados

---

## Arquitetura

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

---

## Funcionalidades

- **Multi-fonte** — Consulta simultânea em PostgreSQL, MySQL e MongoDB
- **Cache Inteligente** — Caffeine cache com TTL configurável por recurso
- **Auditoria por Eventos** — Cada consulta/ingestão vira evento Kafka
- **Dois Protocolos** — REST e gRPC para máxima compatibilidade
- **GraalVM Native** — Startup sub-segundo e consumo mínimo de RAM
- **Multi-profile** — Troque de banco com `SPRING_PROFILES_ACTIVE`
- **Clean Architecture** — Domínio 100% isolado de frameworks e bancos
- **TTL por Recurso** — Controle de expiração de cache por resource

---

## Stack Tecnológica

| Tecnologia | Versão | Propósito |
|------------|--------|-----------|
| Java | 21 | Virtual Threads, Records, Sealed Classes |
| Spring Boot | 3.3.5 | Auto-config, Actuator |
| Apache Kafka | 3.6 | Barramento de auditoria assíncrona |
| gRPC | 1.62.2 | RPC de alta performance |
| GraalVM | 21 | Compilação nativa |
| PostgreSQL | 16 | Banco relacional principal |
| MySQL | 8.0 | Banco relacional secundário |
| MongoDB | 7.0 | Banco documental |
| Caffeine | 3.x | Cache em memória |
| Testcontainers | 1.19.8 | Testes de integração |
| JaCoCo | 0.8.11 | Cobertura de código |
| Checkstyle / PMD | - | Análise estática |
| Prometheus | 2.53 | Métricas e monitoramento |
| OpenTelemetry | 1.37 | Distributed tracing |
| Zipkin | 3.4 | Trace visualization |
| Jaeger | 1.60 | Trace visualization (OTLP) |

---

## Pré-requisitos

- **JDK 21+**
- **Docker & Docker Compose**
- **Maven 3.9+**
- **(Opcional) GraalVM 21** para build nativo

---

## Início Rápido

```bash
# 1. Clone
git clone https://github.com/amaralphp/mcp-context-orchestrator.git
cd mcp-context-orchestrator

# 2. Configure
cp .env.example .env

# 3. Stack completo (recomendado)
docker compose up -d

# 4. Ou desenvolvimento local com PostgreSQL
mvn spring-boot:run -Dspring-boot.run.profiles=postgres

# Acesse: http://localhost:8080
```

---

## Endpoints da API

### POST /api/mcp/query

Buscar recursos e montar contexto.

```json
{
  "query": "pipelines de machine learning",
  "sources": ["postgres", "mongo"],
  "maxTokens": 2000,
  "filters": { "type": "documentacao" }
}
```

### POST /api/mcp/ingest

Ingerir novo recurso no sistema.

```json
{
  "name": "guia-ml-pipeline",
  "type": "documentacao",
  "content": "Guia completo sobre pipelines de ML...",
  "source": "wiki-interna",
  "ttlSeconds": 3600
}
```

### GET /api/mcp/resources/{id}

Recuperar um recurso específico por UUID.

---

## Peris de Banco de Dados

| Perfil | Banco | Porta (Docker) | Observação |
|--------|-------|:--------------:|------------|
| `postgres` | PostgreSQL | 5433 | Perfil padrão |
| `mysql` | MySQL | 3307 | |
| `mongo` | MongoDB | 27018 | |

```bash
# Exemplo: rodar com MySQL
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

---

## Build Nativo (GraalVM)

```bash
# Build nativo (requer GraalVM)
mvn package -Pnative -DskipTests

# Container Docker otimizado
docker build -t mcp-orchestrator .

# Execute
docker run -p 8080:8080 mcp-orchestrator
```

Benefícios: inicialização em <100ms, memória <50MB RAM.

---

## Estrutura do Projeto

```
src/main/java/com/mcporchestrator/
├── domain/
│   ├── entity/
│   │   ├── MCPResource.java        # Recurso MCP
│   │   ├── ContextQuery.java       # Consulta de contexto
│   │   ├── QueryResult.java        # Resultado da consulta
│   │   └── AuditEntry.java         # Entrada de auditoria
│   └── service/
│       └── ContextBuilderService.java  # Montagem de contexto
├── application/
│   ├── port/
│   │   ├── input/                  # Use cases
│   │   ├── output/                 # Repositories, Cache, Audit
│   │   └── event/                  # Eventos de domínio
│   └── usecase/
│       ├── QueryService.java
│       └── IngestResourceService.java
└── infrastructure/
    ├── adapter/
    │   ├── persistence/            # DB adapters (Pg, MySQL, Mongo)
    │   ├── cache/                  # Caffeine cache config
    │   ├── kafka/                  # Audit producer/consumer
    │   └── rest/                   # Controllers, DTOs
    └── config/                     # DatabaseConfig, etc.
```

---

## Testes

```bash
# Todos os testes
mvn test

# Com análise estática
mvn verify

# Qualidade
mvn checkstyle:check pmd:check pmd:cpd-check
```

## Monitoramento

| Endpoint | Descrição |
|----------|-----------|
| `GET /actuator/health` | Health check (DB, Kafka) |
| `GET /actuator/info` | Informações da aplicação |
| `GET /actuator/metrics` | Métricas Micrometer |
| `GET /actuator/prometheus` | Métricas no formato Prometheus |

### Tracing Distribuído

Tracing via OpenTelemetry com exportação para Zipkin e Jaeger.

---

## Variáveis de Ambiente

| Variável | Default | Descrição |
|----------|---------|-----------|
| `SPRING_PROFILES_ACTIVE` | `postgres` | Perfil de banco |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Broker Kafka |
| `SERVER_PORT` | `8080` | Porta HTTP |

---

## Contribuição

1. Fork o projeto
2. Crie sua branch (`git checkout -b feature/nova-funcionalidade`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/nova-funcionalidade`)
5. Abra um Pull Request

---

## Licença

Apache 2.0
