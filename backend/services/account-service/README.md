# Account Service

Manages bank accounts — creation, balance inquiries, and internal debit/credit operations. Uses pessimistic locking to prevent race conditions on concurrent balance mutations.

---

## Port

`8083`

---

## Key Design Decisions

- **Pessimistic locking:** `SELECT FOR UPDATE` via `@Lock(PESSIMISTIC_WRITE)` on all debit/credit repository methods
- **Optimistic version field:** `@Version` on `Account` entity for ORM-level conflict detection
- **Redis caching:** Account reads are cached with a 5-minute TTL; cache is evicted on every write
- **Internal endpoints:** `/api/accounts/internal/**` are called only by `transaction-service` (not exposed via gateway)

---

## Running Locally

### Prerequisites
- Config Server: `:8888`
- Service Discovery: `:8761`
- PostgreSQL on `:5434` with database `account_db`
- Redis on `:6379`

```bash
export POSTGRES_ACCOUNT_PASSWORD=account_secret_123
export REDIS_PASSWORD=redis_secret_123

cd backend/services/account-service
mvn spring-boot:run
```

### With Docker

```bash
docker compose up -d account-service
```

---

## Database

- **Host:** `localhost:5434` (local) or `postgres-account:5432` (Docker)
- **Database:** `account_db`
- Flyway migrations run automatically on startup

Schema tables: `accounts`

---

## API Endpoints

### List My Accounts

```http
GET /api/accounts
Authorization: Bearer <token>
```

**Response `200`:**
```json
[
  {
    "id": 1,
    "accountNumber": "ACC-20250115-001",
    "accountType": "SAVINGS",
    "balance": 2500.00,
    "status": "ACTIVE",
    "currency": "USD",
    "createdAt": "2025-01-15T10:30:00"
  }
]
```

### Get Account by ID

```http
GET /api/accounts/{id}
Authorization: Bearer <token>
```

### Create Account

```http
POST /api/accounts
Authorization: Bearer <token>
Content-Type: application/json

{
  "accountType": "SAVINGS",
  "initialDeposit": 500.00
}
```

**Account types:** `SAVINGS`, `CHECKING`, `INVESTMENT`

**Response `201`:** Created account object.

### Account Status Values

| Status | Meaning |
|--------|---------|
| `ACTIVE` | Operational — accepts debits and credits |
| `FROZEN` | Read-only — no transactions allowed |
| `CLOSED` | Permanently closed |

---

## Internal Endpoints (Service-to-Service Only)

These endpoints are called by `transaction-service` and are **not** routed through the gateway:

```http
POST /api/accounts/internal/debit
Content-Type: application/json

{
  "accountId": 1,
  "amount": 100.00
}
```

```http
POST /api/accounts/internal/credit
Content-Type: application/json

{
  "accountId": 1,
  "amount": 100.00
}
```

Both return `HTTP 200` on success or `HTTP 400` with error details on failure (e.g., insufficient funds).

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `POSTGRES_ACCOUNT_PASSWORD` | Yes | PostgreSQL password |
| `REDIS_PASSWORD` | Yes | Redis AUTH password |
| `CONFIG_SERVER_PASSWORD` | Yes | Config Server auth |
| `EUREKA_PASSWORD` | Yes | Eureka auth |

---

## Swagger UI

http://localhost:8083/swagger-ui.html

---

## Running Tests

```bash
cd backend/services/account-service
mvn test
```

Test class: `AccountServiceTest` — covers debit/credit, insufficient funds, frozen account, not-found, zero/negative amounts.
