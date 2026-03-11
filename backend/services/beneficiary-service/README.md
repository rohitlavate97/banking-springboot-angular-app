# Beneficiary Service

Manages saved payees (beneficiaries) for each user. Enables quick transfers to frequently used accounts.

---

## Port

`8085`

---

## Running Locally

### Prerequisites
- Config Server: `:8888`
- Service Discovery: `:8761`
- PostgreSQL on `:5436` with database `beneficiary_db`

```bash
export POSTGRES_BENEFICIARY_PASSWORD=ben_secret_123

cd backend/services/beneficiary-service
mvn spring-boot:run
```

### With Docker

```bash
docker compose up -d beneficiary-service
```

---

## Database

- **Host:** `localhost:5436` (local) or `postgres-beneficiary:5432` (Docker)
- **Database:** `beneficiary_db`

Schema tables: `beneficiaries`

Beneficiary records are **soft-deleted** (an `active` flag is set to `false`; the row is never physically removed).

---

## API Endpoints

### List My Beneficiaries

```http
GET /api/beneficiaries
Authorization: Bearer <token>
```

**Response `200`:**
```json
[
  {
    "id": 1,
    "userId": 42,
    "nickname": "John's Savings",
    "accountHolderName": "John Doe",
    "accountNumber": "ACC-20250115-002",
    "bankName": "NexaBank",
    "bankCode": "NXBKUS33",
    "active": true,
    "createdAt": "2025-01-20T14:00:00"
  }
]
```

### Add Beneficiary

```http
POST /api/beneficiaries
Authorization: Bearer <token>
Content-Type: application/json

{
  "nickname": "John's Savings",
  "accountHolderName": "John Doe",
  "accountNumber": "ACC-20250115-002",
  "bankName": "NexaBank",
  "bankCode": "NXBKUS33"
}
```

**Business rules:**
- Duplicate account numbers (per user) are rejected with `409 Conflict`
- `nickname` must be 2–50 characters

**Response `201`:** Created beneficiary object.

### Get Beneficiary by ID

```http
GET /api/beneficiaries/{id}
Authorization: Bearer <token>
```

Users can only access their own beneficiaries. Returns `403` if the beneficiary belongs to another user.

### Delete (Soft) Beneficiary

```http
DELETE /api/beneficiaries/{id}
Authorization: Bearer <token>
```

**Response `204`** — Beneficiary is marked inactive and will not appear in list responses.

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `POSTGRES_BENEFICIARY_PASSWORD` | Yes | PostgreSQL password |
| `CONFIG_SERVER_PASSWORD` | Yes | Config Server auth |
| `EUREKA_PASSWORD` | Yes | Eureka auth |

---

## Swagger UI

http://localhost:8085/swagger-ui.html
