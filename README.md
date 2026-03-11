# NexaBank — Digital Banking Platform

A production-grade, cloud-native digital banking platform built with **Spring Boot 3.2**, **Spring Cloud**, **Apache Kafka**, **PostgreSQL**, and an **Angular 17** frontend.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Technology Stack](#technology-stack)
3. [Prerequisites](#prerequisites)
4. [Project Structure](#project-structure)
5. [Quick Start (Docker — Recommended)](#quick-start-docker--recommended)
6. [Running Services Individually (Local Dev)](#running-services-individually-local-dev)
7. [Service Port Reference](#service-port-reference)
8. [API Documentation (Swagger)](#api-documentation-swagger)
9. [Environment Variables](#environment-variables)
10. [Monitoring & Observability](#monitoring--observability)
11. [Running Tests](#running-tests)
12. [User Guide](#user-guide)

---

## Architecture Overview

```
Browser (Angular 17)
        │
        ▼  :4200
  ┌─────────────┐
  │  API Gateway │  :8080  ← JWT validation, rate limiting, routing
  └──────┬──────┘
         │ Routes
   ┌─────┼──────────────────────────────────┐
   ▼     ▼         ▼          ▼             ▼
auth  account  transaction  user       beneficiary
:8081  :8083     :8084      :8082        :8085
                   │
           Kafka topics
     ┌─────────────┼──────────────┐
     ▼             ▼              ▼
  fraud        notification    audit
  :8087          :8086          :8088

  Infrastructure:
  Config Server :8888 | Service Discovery :8761
  PostgreSQL (8 DBs) | Redis | Kafka+Zookeeper
  Prometheus :9090 | Grafana :3000
```

**Key patterns:**
- **Saga (choreography):** Distributed transactions coordinated via Kafka events — debit → emit → credit → emit (with compensation on failure)
- **Pessimistic locking:** Account balance operations use `SELECT FOR UPDATE`
- **JWT stateless auth:** Gateway validates tokens; downstream services trust `X-Authenticated-User` / `X-User-Role` headers
- **Circuit breaker:** Resilience4j on `transaction-service → account-service` calls
- **Event sourcing audit:** Append-only `AuditLog` table written from all Kafka topics

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17, TypeScript |
| Backend framework | Spring Boot 3.2.3, Spring Cloud 2023.0.0 |
| API Gateway | Spring Cloud Gateway (reactive) |
| Service discovery | Netflix Eureka |
| Config management | Spring Cloud Config Server |
| Messaging | Apache Kafka 7.5.3 |
| Databases | PostgreSQL 16 (one per service) |
| Caching | Redis 7.2 |
| Auth | JWT (jjwt 0.12.5), BCrypt-12 |
| Resilience | Resilience4j (CircuitBreaker, Retry, TimeLimiter) |
| ORM | Spring Data JPA + Flyway migrations |
| Mapping | MapStruct 1.5.5 |
| Boilerplate | Lombok 1.18.30 |
| API docs | springdoc-openapi 2.3.0 |
| Monitoring | Prometheus + Grafana |
| Frontend | Angular 17.3.0, SCSS |
| Build | Maven 3.9 (multi-module) |
| Containers | Docker, Docker Compose |
| Tests | JUnit 5, Mockito, Testcontainers |

---

## Prerequisites

| Tool | Minimum Version | Check |
|------|----------------|-------|
| Docker Desktop | 24+ | `docker --version` |
| Docker Compose | 2.20+ | `docker compose version` |
| Java (JDK) | 17+ | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Node.js | 18+ | `node --version` |
| npm | 9+ | `npm --version` |

> **Docker is strongly recommended.** It starts all 25+ services with one command.

---

## Project Structure

```
banking-platform/
├── README.md                        ← You are here
├── pom.xml                          ← Maven root POM
├── docker-compose.yml               ← Full stack orchestration
├── .env.example                     ← Environment variable template
├── .dockerignore
│
├── backend/
│   ├── pom.xml                      ← Backend parent POM
│   ├── infrastructure/
│   │   ├── config-server/           ← Spring Cloud Config  :8888
│   │   ├── service-discovery/       ← Netflix Eureka       :8761
│   │   └── api-gateway/             ← Spring Cloud Gateway :8080
│   │
│   └── services/
│       ├── auth-service/            ← JWT auth, register/login :8081
│       ├── user-service/            ← User profiles, KYC       :8082
│       ├── account-service/         ← Bank accounts, balances  :8083
│       ├── transaction-service/     ← Transfers, Saga          :8084
│       ├── beneficiary-service/     ← Saved payees             :8085
│       ├── notification-service/    ← Email notifications      :8086
│       ├── fraud-service/           ← Fraud detection          :8087
│       └── audit-service/           ← Immutable audit logs     :8088
│
├── frontend/
│   └── angular-client/              ← Angular 17 SPA
│
├── infrastructure/
│   └── monitoring/
│       ├── prometheus/              ← Metrics scraping + alerts
│       └── grafana/                 ← Dashboards + provisioning
│
└── docs/
    └── architecture/
        └── system-architecture.md
```

---

## Quick Start (Docker — Recommended)

### 1. Clone and configure environment

```bash
# Clone the repository
git clone <repo-url>
cd banking-platform

# Copy and edit the environment file
cp .env.example .env
```

Open `.env` and change **all passwords** from their default values, especially in production.

### 2. Build all backend services

```bash
cd backend
mvn clean package -DskipTests
cd ..
```

### 3. Start the entire platform

```bash
docker compose up -d
```

This starts **25 containers**: 8 PostgreSQL databases, Redis, Zookeeper, Kafka, Config Server, Service Discovery, API Gateway, 8 business microservices, Prometheus, and Grafana.

### 4. Wait for health checks to pass

```bash
# Watch container status (services depend_on healthy infrastructure)
docker compose ps

# Follow startup logs
docker compose logs -f config-server service-discovery api-gateway
```

**Startup order** (enforced by `depends_on: condition: service_healthy`):
1. Databases + Redis + Kafka (~30s)
2. Config Server (~20s)
3. Service Discovery (~20s)
4. API Gateway + Business services (~30s each)

Full cold start takes approximately **3–5 minutes**.

### 5. Start the Angular frontend

```bash
cd frontend/angular-client
npm install
npm start
```

Open **http://localhost:4200** in your browser.

### Stopping the platform

```bash
# Stop all containers (preserves data volumes)
docker compose down

# Stop and remove all data (fresh start)
docker compose down -v
```

---

## Running Services Individually (Local Dev)

For local development you need to either run infrastructure via Docker or have it installed natively.

### Step 1 — Start infrastructure only

```bash
# Start only databases, Redis, and Kafka (not the Spring services)
docker compose up -d postgres-auth postgres-user postgres-account postgres-transaction \
  postgres-beneficiary postgres-notification postgres-fraud postgres-audit \
  redis zookeeper kafka
```

### Step 2 — Start Config Server first

```bash
cd backend/infrastructure/config-server
mvn spring-boot:run
# Listening on http://localhost:8888
```

### Step 3 — Start Service Discovery

```bash
cd backend/infrastructure/service-discovery
mvn spring-boot:run
# Eureka dashboard: http://localhost:8761
```

### Step 4 — Start business services (any order)

Each service can be run independently. Open a terminal per service:

```bash
# Auth Service
cd backend/services/auth-service
mvn spring-boot:run

# User Service
cd backend/services/user-service
mvn spring-boot:run

# Account Service
cd backend/services/account-service
mvn spring-boot:run

# Transaction Service
cd backend/services/transaction-service
mvn spring-boot:run

# Beneficiary Service
cd backend/services/beneficiary-service
mvn spring-boot:run

# Notification Service (requires SMTP config)
cd backend/services/notification-service
mvn spring-boot:run

# Fraud Service
cd backend/services/fraud-service
mvn spring-boot:run

# Audit Service
cd backend/services/audit-service
mvn spring-boot:run
```

### Step 5 — Start the API Gateway last

```bash
cd backend/infrastructure/api-gateway
mvn spring-boot:run
# Gateway on http://localhost:8080
```

### Step 6 — Start the Angular frontend

```bash
cd frontend/angular-client
npm install
npm start
# Dev server on http://localhost:4200
# Proxy: /api/* → http://localhost:8080
```

> **Tip:** Use `-Dspring-boot.run.profiles=local` if you have a local Spring profile defined.

---

## Service Port Reference

| Service | Port | URL |
|---------|------|-----|
| **API Gateway** | 8080 | http://localhost:8080 |
| Auth Service | 8081 | http://localhost:8081 |
| User Service | 8082 | http://localhost:8082 |
| Account Service | 8083 | http://localhost:8083 |
| Transaction Service | 8084 | http://localhost:8084 |
| Beneficiary Service | 8085 | http://localhost:8085 |
| Notification Service | 8086 | http://localhost:8086 |
| Fraud Service | 8087 | http://localhost:8087 |
| Audit Service | 8088 | http://localhost:8088 |
| Config Server | 8888 | http://localhost:8888 |
| Service Discovery (Eureka) | 8761 | http://localhost:8761 |
| **Angular Frontend** | 4200 | http://localhost:4200 |
| PostgreSQL (auth) | 5432 | — |
| PostgreSQL (user) | 5433 | — |
| PostgreSQL (account) | 5434 | — |
| PostgreSQL (transaction) | 5435 | — |
| PostgreSQL (beneficiary) | 5436 | — |
| PostgreSQL (notification) | 5437 | — |
| PostgreSQL (fraud) | 5438 | — |
| PostgreSQL (audit) | 5439 | — |
| Redis | 6379 | — |
| Kafka | 9092 | — |
| Prometheus | 9090 | http://localhost:9090 |
| Grafana | 3000 | http://localhost:3000 |

---

## API Documentation (Swagger)

Each service exposes a Swagger UI. All routes are also reachable through the gateway:

| Service | Swagger URL |
|---------|------------|
| Auth | http://localhost:8081/swagger-ui.html |
| User | http://localhost:8082/swagger-ui.html |
| Account | http://localhost:8083/swagger-ui.html |
| Transaction | http://localhost:8084/swagger-ui.html |
| Beneficiary | http://localhost:8085/swagger-ui.html |

**To authorize in Swagger:**
1. Call `POST /api/auth/login` to get an `accessToken`
2. Click **Authorize** (padlock icon) in Swagger UI
3. Enter: `Bearer <your_access_token>`

---

## Environment Variables

All secrets live in `.env` (created from `.env.example`). Key variables:

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | 256-bit hex secret for signing JWTs. Generate: `openssl rand -hex 32` |
| `POSTGRES_*_PASSWORD` | Per-service DB passwords |
| `REDIS_PASSWORD` | Redis AUTH password |
| `CONFIG_SERVER_PASSWORD` | Basic auth for Config Server |
| `EUREKA_PASSWORD` | Basic auth for Eureka |
| `MAIL_HOST/USERNAME/PASSWORD` | SMTP settings for notification emails |
| `GRAFANA_PASSWORD` | Grafana admin password |

---

## Monitoring & Observability

| Tool | URL | Credentials |
|------|-----|-------------|
| Grafana | http://localhost:3000 | `admin` / value of `GRAFANA_PASSWORD` |
| Prometheus | http://localhost:9090 | None |
| Eureka Dashboard | http://localhost:8761 | `eureka` / value of `EUREKA_PASSWORD` |

For full details see [infrastructure/monitoring/README.md](infrastructure/monitoring/README.md).

---

## Running Tests

```bash
# All tests (unit only — no containers needed)
cd backend
mvn test

# Single service
cd backend/services/auth-service
mvn test

# Integration tests (requires Docker for Testcontainers)
cd backend/services/transaction-service
mvn verify -Pfailsafe

# Specific test class
mvn test -Dtest=AuthServiceTest

# Skip tests during build
mvn package -DskipTests
```

---

## User Guide

See [docs/USER_GUIDE.md](docs/USER_GUIDE.md) for end-to-end usage instructions including registration, account management, transfers, and beneficiary setup.

---

## Security Notes

- Change **all default passwords** in `.env` before any deployment
- `JWT_SECRET` must be at least 256 bits. Regenerate with `openssl rand -hex 32`
- The `api-gateway` is the **only** service that should be publicly exposed
- Downstream services validate requests via trusted `X-Authenticated-User` headers set by the gateway — **never expose them directly**
- Account balance mutations use pessimistic locking to prevent race conditions
