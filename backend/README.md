# Backend — Spring Boot Microservices

This directory contains all backend services organized as a Maven multi-module project.

---

## Module Structure

```
backend/
├── pom.xml                     ← Parent POM (manages all dependencies & versions)
│
├── infrastructure/
│   ├── config-server/          ← Centralized configuration  :8888
│   ├── service-discovery/      ← Eureka registry           :8761
│   └── api-gateway/            ← Gateway + JWT auth        :8080
│
└── services/
    ├── auth-service/           ← Authentication & authorization  :8081
    ├── user-service/           ← User profiles & KYC            :8082
    ├── account-service/        ← Bank accounts & balances        :8083
    ├── transaction-service/    ← Transfers, Saga orchestration   :8084
    ├── beneficiary-service/    ← Saved payees                    :8085
    ├── notification-service/   ← Email notifications             :8086
    ├── fraud-service/          ← Automated fraud detection       :8087
    └── audit-service/          ← Immutable audit log             :8088
```

---

## Building the Entire Backend

```bash
# From the banking-platform root
cd backend

# Clean build, skip tests
mvn clean package -DskipTests

# Clean build, run all tests
mvn clean verify

# Build a single module
mvn clean package -pl services/auth-service -am -DskipTests
```

The `-am` flag (also-make) ensures dependent modules are also built.

---

## Dependency Versions (managed in root pom.xml)

| Dependency | Version |
|-----------|---------|
| Spring Boot | 3.2.3 |
| Spring Cloud | 2023.0.0 |
| jjwt | 0.12.5 |
| MapStruct | 1.5.5 |
| Lombok | 1.18.30 |
| springdoc-openapi | 2.3.0 |
| Testcontainers | 1.19.5 |
| Logstash Logback Encoder | 7.4 |

---

## Startup Order

Services must be started in this order when running locally:

```
1. PostgreSQL databases (all 8)
2. Redis
3. Zookeeper → Kafka
4. config-server        (others read config from here)
5. service-discovery    (others register with Eureka)
6. api-gateway + all business services (can start in any order after #5)
```

---

## Infrastructure Services

See individual READMEs:
- [Config Server](infrastructure/config-server/README.md)
- [Service Discovery](infrastructure/service-discovery/README.md)
- [API Gateway](infrastructure/api-gateway/README.md)

## Business Services

See individual READMEs:
- [Auth Service](services/auth-service/README.md)
- [User Service](services/user-service/README.md)
- [Account Service](services/account-service/README.md)
- [Transaction Service](services/transaction-service/README.md)
- [Beneficiary Service](services/beneficiary-service/README.md)
- [Notification Service](services/notification-service/README.md)
- [Fraud Service](services/fraud-service/README.md)
- [Audit Service](services/audit-service/README.md)

---

## Common Configuration

Every service connects to Config Server and Eureka on startup. The required bootstrap properties are:

```yaml
spring:
  config:
    import: "configserver:http://localhost:8888"
  cloud:
    config:
      username: config
      password: ${CONFIG_SERVER_PASSWORD}
```

In Docker, these are supplied via environment variables in `docker-compose.yml`.

---

## Logging

All services use **structured JSON logging** via Logstash Logback Encoder. Log entries include:

| Field | Description |
|-------|-------------|
| `traceId` | Distributed trace ID |
| `spanId` | Current span ID |
| `correlationId` | Request correlation ID (set by gateway) |
| `userId` | Authenticated user ID (from JWT) |
| `service` | Service name |

Logs are written to stdout and can be collected by any log aggregator (ELK, Loki, etc.).

---

## Health & Actuator Endpoints

Every service exposes Spring Boot Actuator endpoints:

```
GET /actuator/health       → Service health status
GET /actuator/info         → Build info
GET /actuator/metrics      → Micrometer metrics
GET /actuator/prometheus   → Prometheus-format metrics
```

These are scraped by Prometheus every 15 seconds.
