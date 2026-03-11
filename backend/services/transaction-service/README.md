# Transaction Service

Orchestrates all money movement — deposits, withdrawals, and transfers — using the **Saga (choreography) pattern** coordinated via Apache Kafka events.

---

## Port

`8084`

---

## Saga Pattern

```
Transfer Request
      │
      ▼
[1] Save transaction (PENDING)
      │
      ▼
[2] Publish TransactionCreatedEvent → Kafka: transaction-events
      │
      ▼
[3] Debit sourceAccount via account-service (internal REST)
      │
   ┌──┴──────────────────────────┐
   │ Success                     │ Failure
   ▼                             ▼
[4] Credit destinationAccount  [4b] Publish TransactionFailedEvent
      │                              Status → FAILED
   ┌──┴────────────────────────┐
   │ Success                   │ Failure
   ▼                           ▼
[5] Publish Completed         [5b] Re-credit sourceAccount (compensation)
    Status → COMPLETED             Publish TransactionFailedEvent
                                   Status → ROLLED_BACK
```

---

## Running Locally

### Prerequisites
- Config Server: `:8888`
- Service Discovery: `:8761`
- PostgreSQL on `:5435` with database `transaction_db`
- Kafka on `:9092`
- `account-service` running (called via REST)

```bash
export POSTGRES_TRANSACTION_PASSWORD=txn_secret_123

cd backend/services/transaction-service
mvn spring-boot:run
```

### With Docker

```bash
docker compose up -d transaction-service
```

---

## Database

- **Host:** `localhost:5435` (local) or `postgres-transaction:5432` (Docker)
- **Database:** `transaction_db`

Schema tables: `transactions`

---

## Kafka Topics Published

| Topic | When | Consumers |
|-------|------|-----------|
| `transaction-events` (6 partitions) | Every new transaction | fraud-service, audit-service |
| `transaction-completed` (6 partitions) | On success | notification-service, audit-service |
| `transaction-failed` (3 partitions) | On failure/rollback | notification-service, audit-service |

---

## API Endpoints

### Deposit

```http
POST /api/transactions/deposit
Authorization: Bearer <token>
Content-Type: application/json

{
  "destinationAccountId": 1,
  "amount": 500.00,
  "description": "Paycheck deposit"
}
```

**Response `200`:**
```json
{
  "id": 42,
  "transactionType": "DEPOSIT",
  "amount": 500.00,
  "destinationAccountId": 1,
  "status": "COMPLETED",
  "createdAt": "2025-03-11T09:00:00"
}
```

### Withdraw

```http
POST /api/transactions/withdraw
Authorization: Bearer <token>
Content-Type: application/json

{
  "sourceAccountId": 1,
  "amount": 200.00,
  "description": "ATM withdrawal"
}
```

### Transfer

```http
POST /api/transactions/transfer
Authorization: Bearer <token>
Content-Type: application/json

{
  "sourceAccountId": 1,
  "destinationAccountId": 2,
  "amount": 150.00,
  "description": "Rent payment"
}
```

### Get My Transactions (Paginated)

```http
GET /api/transactions?page=0&size=10
Authorization: Bearer <token>
```

**Response `200`:**
```json
{
  "content": [ ... ],
  "number": 0,
  "size": 10,
  "totalElements": 47,
  "totalPages": 5,
  "last": false
}
```

### Get Transaction by ID

```http
GET /api/transactions/{id}
Authorization: Bearer <token>
```

---

## Transaction Status Values

| Status | Meaning |
|--------|---------|
| `PENDING` | Created, debit not yet attempted |
| `COMPLETED` | All steps succeeded |
| `FAILED` | Debit failed — no money moved |
| `ROLLED_BACK` | Credit failed — debit was compensated |

---

## Resilience

Circuit breaker wraps all calls to `account-service`:
- **Sliding window:** 10 calls
- **Failure rate threshold:** 50%
- **Wait in OPEN state:** 10 seconds
- **Retry:** 3 attempts, 500ms between retries
- **Time limiter:** 3 seconds per call

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `POSTGRES_TRANSACTION_PASSWORD` | Yes | PostgreSQL password |
| `CONFIG_SERVER_PASSWORD` | Yes | Config Server auth |
| `EUREKA_PASSWORD` | Yes | Eureka auth |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Yes | Kafka broker address |

---

## Swagger UI

http://localhost:8084/swagger-ui.html

---

## Running Tests

```bash
cd backend/services/transaction-service

# Unit tests (no Docker required)
mvn test

# Integration tests (Docker required for Testcontainers)
mvn verify
```

Test classes:
- `TransactionServiceTest` — Saga happy path, credit failure compensation, debit failure
- `TransactionServiceIntegrationTest` — Testcontainers with real PostgreSQL + Kafka
