# Banking Platform — System Architecture

## Overview

This document describes the high-level architecture of the production-grade digital banking platform built on microservices, event-driven design, and clean architecture principles.

---

## Architecture Diagram

```
                          ┌─────────────────────────────────────────────┐
                          │              CLIENT LAYER                    │
                          │         Angular 17 SPA (Browser)            │
                          └────────────────────┬────────────────────────┘
                                               │ HTTPS
                          ┌────────────────────▼────────────────────────┐
                          │              API GATEWAY                     │
                          │   Spring Cloud Gateway  :8080                │
                          │  ● JWT Validation                            │
                          │  ● Rate Limiting (Redis)                     │
                          │  ● Request Routing                           │
                          │  ● Centralized Logging                       │
                          └──────┬──────┬──────┬──────┬──────┬──────────┘
                                 │      │      │      │      │
               ┌─────────────────┘      │      │      │      └──────────────────────┐
               │                ┌───────┘      └──────┐                             │
               ▼                ▼                     ▼                             ▼
    ┌──────────────┐  ┌──────────────────┐  ┌──────────────────┐       ┌──────────────────┐
    │ auth-service │  │  user-service    │  │ account-service  │       │transaction-service│
    │   :8081      │  │    :8082         │  │     :8083        │       │      :8084        │
    │ OAuth2 + JWT │  │ Customer/KYC     │  │ Accounts/Balance │       │Deposits/Transfers │
    └──────┬───────┘  └────────┬─────────┘  └────────┬─────────┘       └────────┬──────────┘
           │                   │                     │                           │
           └───────────────────┴─────────────────────┴───────────────────────────┘
                                               │
                                    ┌──────────▼──────────┐
                                    │    Apache Kafka      │
                                    │  Event Streaming Bus │
                                    │                      │
                                    │  Topics:             │
                                    │  ● transaction-events│
                                    │  ● fraud-alerts      │
                                    │  ● notification-events│
                                    │  ● audit-events      │
                                    └──────────┬───────────┘
                                               │
               ┌───────────────────────────────┼───────────────────────────────┐
               ▼                               ▼                               ▼
    ┌──────────────────┐           ┌──────────────────┐           ┌──────────────────┐
    │  fraud-service   │           │notification-service│          │  audit-service   │
    │     :8087        │           │      :8086         │          │     :8088        │
    │ Fraud Detection  │           │  Email/SMS Alerts  │          │ Compliance Logs  │
    └──────────────────┘           └──────────────────┘           └──────────────────┘

  ┌──────────────────────────────────────────────────────────────────────────────────┐
  │                           SUPPORTING SERVICES                                    │
  │                                                                                  │
  │  ┌──────────────────────┐   ┌──────────────────────┐   ┌──────────────────────┐ │
  │  │   config-server      │   │  service-discovery   │   │ beneficiary-service  │ │
  │  │       :8888          │   │  (Eureka)  :8761     │   │       :8085          │ │
  │  │ Centralized Config   │   │ Service Registration │   │  Saved Payees        │ │
  │  └──────────────────────┘   └──────────────────────┘   └──────────────────────┘ │
  └──────────────────────────────────────────────────────────────────────────────────┘

  ┌──────────────────────────────────────────────────────────────────────────────────┐
  │                           DATA & CACHE LAYER                                     │
  │                                                                                  │
  │  ┌──────────────────────┐   ┌──────────────────────┐   ┌──────────────────────┐ │
  │  │    PostgreSQL         │   │       Redis          │   │      Zookeeper       │ │
  │  │  Per-service schemas  │   │  Cache / Sessions /  │   │  Kafka Coordination  │ │
  │  │  :5432               │   │  Rate Limiting :6379 │   │      :2181           │ │
  │  └──────────────────────┘   └──────────────────────┘   └──────────────────────┘ │
  └──────────────────────────────────────────────────────────────────────────────────┘

  ┌──────────────────────────────────────────────────────────────────────────────────┐
  │                         OBSERVABILITY LAYER                                      │
  │                                                                                  │
  │  ┌──────────────────────┐   ┌──────────────────────┐                            │
  │  │     Prometheus        │   │       Grafana        │                            │
  │  │  Metrics Scraping     │   │  Dashboards :3000    │                            │
  │  │      :9090           │   │                      │                            │
  │  └──────────────────────┘   └──────────────────────┘                            │
  └──────────────────────────────────────────────────────────────────────────────────┘
```

---

## Service Port Map

| Service               | Port  | Role                        |
|-----------------------|-------|-----------------------------|
| config-server         | 8888  | Centralized configuration   |
| service-discovery     | 8761  | Eureka service registry     |
| api-gateway           | 8080  | Entry point, JWT validation |
| auth-service          | 8081  | OAuth2 + JWT issuance       |
| user-service          | 8082  | Customer profiles / KYC     |
| account-service       | 8083  | Accounts and balances       |
| transaction-service   | 8084  | Deposits, withdrawals, transfers |
| beneficiary-service   | 8085  | Saved payees                |
| notification-service  | 8086  | Email/SMS alerts            |
| fraud-service         | 8087  | Fraud detection             |
| audit-service         | 8088  | Compliance audit logs       |
| PostgreSQL            | 5432  | Relational database         |
| Redis                 | 6379  | Cache / Rate limiting       |
| Kafka                 | 9092  | Event streaming             |
| Zookeeper             | 2181  | Kafka coordination          |
| Prometheus            | 9090  | Metrics scraping            |
| Grafana               | 3000  | Monitoring dashboards       |

---

## Kafka Event Flow

```
[transaction-service]
        │
        │ Publishes: TransactionCreatedEvent
        ▼
[Kafka Topic: transaction-events]
        │
        ├──────────────────────▶ [fraud-service]
        │                              │ Publishes: FraudAlertEvent
        │                              ▼
        │                     [Kafka Topic: fraud-alerts]
        │                              │
        │                              ▼
        │                         [audit-service] — logs fraud alert
        │
        ├──────────────────────▶ [notification-service]
        │                              │ Sends email/SMS
        │
        └──────────────────────▶ [audit-service]
                                       │ Logs transaction event
```

---

## Security Architecture

```
  Client ──HTTPS──▶ API Gateway ──▶ (JWT Validation Filter)
                                          │
                              Valid? ─────┤
                                    Yes   │   No ──▶ 401 Unauthorized
                                          │
                              ────────────▼────────────
                              Downstream Microservice
                              (Security Context Populated)
```

- **Auth Service** acts as the OAuth2 Authorization Server
- Issues **JWT access tokens** (15 min TTL) and **refresh tokens** (7 days TTL)
- All downstream services validate JWT using shared public key
- Role-based access: `ROLE_CUSTOMER`, `ROLE_ADMIN`
- Passwords hashed with **BCrypt** (strength 12)

---

## Clean Architecture Layers (per service)

```
┌────────────────────────────────────────────────────┐
│                   Controller Layer                  │  ← REST API endpoints
├────────────────────────────────────────────────────┤
│                    Service Layer                    │  ← Business logic
├────────────────────────────────────────────────────┤
│                  Repository Layer                   │  ← Data access
├────────────────────────────────────────────────────┤
│                    Entity Layer                     │  ← Domain models
├────────────────────────────────────────────────────┤
│            DTO / Mapper / Event Layer               │  ← Data transfer & events
├────────────────────────────────────────────────────┤
│         Config / Security / Exception Layer         │  ← Cross-cutting concerns
└────────────────────────────────────────────────────┘
```

---

## Saga Pattern — Money Transfer

```
transaction-service receives POST /api/transactions/transfer
  │
  ├─1─ Validate sender account balance (account-service via REST)
  │
  ├─2─ Lock/Debit sender account (account-service)
  │       └── On failure → COMPENSATE: release lock, return error
  │
  ├─3─ Emit TransactionDebitedEvent (Kafka)
  │
  ├─4─ Credit receiver account (account-service)
  │       └── On failure → COMPENSATE: reverse debit, emit rollback event
  │
  ├─5─ Emit TransactionCompletedEvent (Kafka)
  │       → fraud-service consumes
  │       → notification-service consumes
  │       → audit-service consumes
  │
  └─6─ Return success response
```

---

## Technology Decisions

| Concern            | Technology           | Reason                                      |
|--------------------|----------------------|---------------------------------------------|
| API Gateway        | Spring Cloud Gateway | Non-blocking, reactive, JWT support         |
| Service Discovery  | Eureka               | Spring Cloud native, battle-tested          |
| Config Management  | Spring Cloud Config  | Centralized, environment-specific           |
| Event Streaming    | Apache Kafka         | High-throughput, durable, replay            |
| Caching            | Redis                | Sub-millisecond latency, distributed        |
| Database           | PostgreSQL           | ACID, relational, financial-grade           |
| Auth               | Spring Security OAuth2 | Standard, extensible, JWT support         |
| Resilience         | Resilience4j         | Lightweight, functional, Spring Boot native |
| Observability      | Prometheus + Grafana | Industry standard, pull-based metrics       |
| Migrations         | Flyway               | Version-controlled, repeatable DB migrations|
