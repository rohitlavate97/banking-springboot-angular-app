# Fraud Detection Service

Consumes every transaction event and applies rule-based fraud detection. When a rule fires it publishes to the `fraud-alerts` Kafka topic and persists a `FraudAlert` record. Exposes an admin-only REST API for reviewing alerts.

---

## Port

`8087`

---

## Running Locally

### Prerequisites
- Config Server: `:8888`
- Service Discovery: `:8761`
- Kafka broker accessible (e.g., `localhost:9092`)
- PostgreSQL on `:5438` with database `fraud_db`

```bash
export POSTGRES_FRAUD_PASSWORD=fraud_secret_123

cd backend/services/fraud-service
mvn spring-boot:run
```

### With Docker

```bash
docker compose up -d fraud-service
```

---

## Database

- **Host:** `localhost:5438` (local) or `postgres-fraud:5432` (Docker)
- **Database:** `fraud_db`

Schema tables: `fraud_alerts`

---

## Detection Rules

| Rule | Condition | Severity |
|------|-----------|---------|
| `LARGE_TRANSACTION` | Transaction amount ≥ $10,000 | HIGH |
| `RAPID_REPEATED_TRANSFERS` | > 3 transfer transactions from same account within 5 minutes | MEDIUM |

Rules are evaluated **synchronously** inside the Kafka consumer. If any rule matches, an alert is saved and re-published.

---

## Kafka Integration

### Consumers

| Topic | Group ID | Action |
|-------|----------|--------|
| `transaction-events` | `fraud-detection-group` | Evaluate all fraud rules (manual acknowledge) |

### Producers

| Topic | Triggered by |
|-------|-------------|
| `fraud-alerts` | Any rule match |

The `fraud-alerts` topic is consumed by both the **Notification Service** (sends email) and the **Audit Service** (persists log entry).

---

## REST API (Admin Only)

All endpoints require an `Authorization` header with a valid JWT belonging to a user with `ROLE_ADMIN`.

### List All Fraud Alerts

```http
GET /api/fraud/alerts
Authorization: Bearer <admin-token>
```

**Response `200`:**
```json
[
  {
    "id": 1,
    "transactionId": 77,
    "userId": 42,
    "ruleTriggered": "LARGE_TRANSACTION",
    "severity": "HIGH",
    "status": "OPEN",
    "details": "Transaction amount $15,000.00 exceeds threshold",
    "createdAt": "2025-01-20T15:00:00"
  }
]
```

### Get Alert by ID

```http
GET /api/fraud/alerts/{id}
Authorization: Bearer <admin-token>
```

### Update Alert Status

```http
PUT /api/fraud/alerts/{id}/status
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "status": "RESOLVED",
  "notes": "Confirmed legitimate transfer by user."
}
```

**Alert statuses:** `OPEN`, `UNDER_REVIEW`, `RESOLVED`, `FALSE_POSITIVE`

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `POSTGRES_FRAUD_PASSWORD` | Yes | PostgreSQL password |
| `KAFKA_BOOTSTRAP_SERVERS` | Yes | Kafka broker(s) |
| `CONFIG_SERVER_PASSWORD` | Yes | Config Server auth |
| `EUREKA_PASSWORD` | Yes | Eureka auth |

---

## Swagger UI

http://localhost:8087/swagger-ui.html

---

## Tests

```bash
cd backend/services/fraud-service
mvn test -Dtest=FraudDetectionServiceTest
```

Test coverage:
- Small transaction → no alert published
- Large transaction → alert persisted + Kafka publish verified
- Rapid transfers rule triggers on 4th transfer within 5 min window
- Transaction set to `PENDING` status before evaluation
