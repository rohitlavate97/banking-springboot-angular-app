# Auth Service

Handles user registration, login, JWT token issuance, refresh token rotation, and logout. Implements account lockout after repeated failed attempts.

---

## Port

`8081`

---

## Technology

- Spring Security + BCrypt (cost 12) for password hashing
- JWT access tokens (15 min expiry) via jjwt 0.12.5
- Refresh tokens (7 days) stored in PostgreSQL
- Account lockout: 5 failed attempts → locked for 30 minutes

---

## Running Locally

### Prerequisites
- Config Server: `:8888`
- Service Discovery: `:8761`
- PostgreSQL on `:5432` with database `auth_db`

```bash
# Set environment
export POSTGRES_AUTH_PASSWORD=auth_secret_123
export JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970

cd backend/services/auth-service
mvn spring-boot:run
```

### With Docker

```bash
docker compose up -d auth-service
```

---

## Database

- **Host:** `localhost:5432` (local) or `postgres-auth:5432` (Docker)
- **Database:** `auth_db`
- **User:** `auth_user`
- Migrations managed by **Flyway** — schema is created automatically on startup

Schema tables: `users`, `refresh_tokens`

---

## API Endpoints

All endpoints are accessible via the gateway at `http://localhost:8080/api/auth/**`

### Register

```http
POST /api/auth/register
Content-Type: application/json

{
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane@example.com",
  "password": "SecurePass@1"
}
```

**Response `201`:**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Password requirements:** 8+ characters, must include uppercase, lowercase, number, and special character (`@$!%*?&`).

---

### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "jane@example.com",
  "password": "SecurePass@1"
}
```

**Response `200`:** Same as register response.

**Error responses:**
- `401` — Invalid credentials
- `423` — Account locked (too many failed attempts)

---

### Refresh Token

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGci..."
}
```

**Response `200`:** New `accessToken` + new `refreshToken` (token rotation).

---

### Logout

```http
POST /api/auth/logout
Authorization: Bearer <accessToken>
```

**Response `204`** — Refresh token is revoked in the database.

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `POSTGRES_AUTH_PASSWORD` | Yes | PostgreSQL password |
| `JWT_SECRET` | Yes | Hex-encoded 256-bit signing key |
| `CONFIG_SERVER_PASSWORD` | Yes | Config Server basic auth |
| `EUREKA_PASSWORD` | Yes | Eureka basic auth |

---

## Swagger UI

http://localhost:8081/swagger-ui.html

---

## Running Tests

```bash
cd backend/services/auth-service
mvn test
```

Test classes:
- `AuthServiceTest` — register, login, lockout, duplicate email
- `JwtServiceTest` — token generation, validation, claims, expiry
