# API Gateway

The single entry point for all client requests. Built with **Spring Cloud Gateway** (reactive/WebFlux).

---

## Port

`8080`

---

## Responsibilities

- **JWT validation** — Verifies `Authorization: Bearer <token>` on incoming requests
- **Header injection** — Sets `X-Authenticated-User` (userId) and `X-User-Role` on routed requests so downstream services trust without re-validating JWT
- **Request routing** — Routes to the appropriate microservice via Eureka discovery
- **Rate limiting** — Per-user rate limiting via Redis (default 100 req/min)
- **Correlation IDs** — Adds `X-Correlation-Id` header to every request for distributed tracing
- **CORS** — Configured to allow requests from the Angular frontend

---

## Running Locally

### Prerequisites
- Config Server running on `:8888`
- Service Discovery running on `:8761`
- Redis running on `:6379`
- At least `auth-service` running (gateway needs JWT secret config)

```bash
cd backend/infrastructure/api-gateway
mvn spring-boot:run
```

Verify:
```bash
curl http://localhost:8080/actuator/health
```

### With Docker

```bash
docker compose up -d api-gateway
```

---

## Route Reference

All client requests go through `http://localhost:8080`. The gateway strips the `/api` prefix before forwarding.

| Client Path | Forwards To | Auth Required |
|-------------|-------------|---------------|
| `POST /api/auth/register` | auth-service | No |
| `POST /api/auth/login` | auth-service | No |
| `POST /api/auth/refresh` | auth-service | No |
| `POST /api/auth/logout` | auth-service | Yes |
| `GET/PUT /api/users/**` | user-service | Yes |
| `GET/POST /api/accounts/**` | account-service | Yes |
| `GET/POST /api/transactions/**` | transaction-service | Yes |
| `GET/POST/DELETE /api/beneficiaries/**` | beneficiary-service | Yes |
| `GET /api/fraud/**` | fraud-service | Yes (ADMIN) |
| `GET /api/audit/**` | audit-service | Yes (ADMIN) |

---

## Authentication Flow

```
Client → Gateway
  1. Extract Bearer token from Authorization header
  2. Validate JWT signature + expiry
  3. Extract userId + role from claims
  4. Add X-Authenticated-User: <userId>
  5. Add X-User-Role: <ROLE_USER|ROLE_ADMIN>
  6. Forward request to downstream service
```

Public endpoints (no token required): `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`

---

## Environment Variables

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | Must match the secret used in auth-service |
| `REDIS_PASSWORD` | Used for rate-limiter state |
| `EUREKA_PASSWORD` | Eureka auth |
| `CONFIG_SERVER_PASSWORD` | Config Server auth |

---

## Rate Limiting

Default: **100 requests per 60 seconds** per authenticated user (Redis-backed token bucket).

Exceeded requests receive: `HTTP 429 Too Many Requests`

---

## Health Check

```bash
curl http://localhost:8080/actuator/health
```
