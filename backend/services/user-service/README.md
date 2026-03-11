# User Service

Manages user profiles, personal information, address details, and KYC (Know Your Customer) status.

---

## Port

`8082`

---

## Running Locally

### Prerequisites
- Config Server: `:8888`
- Service Discovery: `:8761`
- PostgreSQL on `:5433` with database `user_db`

```bash
export POSTGRES_USER_PASSWORD=user_secret_123

cd backend/services/user-service
mvn spring-boot:run
```

### With Docker

```bash
docker compose up -d user-service
```

---

## Database

- **Host:** `localhost:5433` (local) or `postgres-user:5432` (Docker)
- **Database:** `user_db`
- Flyway manages migrations automatically

Schema tables: `user_profiles`

---

## API Endpoints

All endpoints require authentication. The userId is extracted from the `X-Authenticated-User` header set by the gateway.

### Get My Profile

```http
GET /api/users/me
Authorization: Bearer <token>
```

**Response `200`:**
```json
{
  "id": 1,
  "userId": 42,
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane@example.com",
  "phoneNumber": "+1-555-0100",
  "dateOfBirth": "1990-05-15",
  "address": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "US"
  },
  "kycStatus": "VERIFIED",
  "createdAt": "2025-01-15T10:30:00"
}
```

### Update My Profile

```http
PUT /api/users/me
Authorization: Bearer <token>
Content-Type: application/json

{
  "phoneNumber": "+1-555-0101",
  "address": {
    "street": "456 Park Ave",
    "city": "New York",
    "state": "NY",
    "zipCode": "10022",
    "country": "US"
  }
}
```

### Get Profile by ID (Admin only)

```http
GET /api/users/{userId}
Authorization: Bearer <admin-token>
```

---

## KYC Status Values

| Status | Meaning |
|--------|---------|
| `PENDING` | Profile created, verification not submitted |
| `UNDER_REVIEW` | Documents submitted, under review |
| `VERIFIED` | KYC verification passed |
| `REJECTED` | KYC verification failed |

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `POSTGRES_USER_PASSWORD` | Yes | PostgreSQL password |
| `CONFIG_SERVER_PASSWORD` | Yes | Config Server auth |
| `EUREKA_PASSWORD` | Yes | Eureka auth |

---

## Swagger UI

http://localhost:8082/swagger-ui.html
