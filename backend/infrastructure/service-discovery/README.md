# Service Discovery (Eureka)

Netflix Eureka server for service registration and discovery. All microservices register here and use it to locate each other.

---

## Port

`8761`

---

## Responsibilities

- Maintains a registry of all running service instances
- Provides the Eureka dashboard for monitoring registered services
- Enables client-side load balancing via Spring Cloud LoadBalancer

---

## Running Locally

### Prerequisites
- Config Server must be running on port `8888`

```bash
cd backend/infrastructure/service-discovery
mvn spring-boot:run
```

Open the Eureka dashboard: **http://localhost:8761**

Login credentials:
- **Username:** `eureka`
- **Password:** value of `EUREKA_PASSWORD` (default: `eureka_secret_change_me`)

### With Docker

```bash
docker compose up -d service-discovery
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8761` | HTTP port |
| `EUREKA_PASSWORD` | `eureka_secret_change_me` | Basic auth password |
| `CONFIG_SERVER_PASSWORD` | — | Password to read config from Config Server |

---

## Verifying Registered Services

Once all services are running, the Eureka dashboard at **http://localhost:8761** shows:

| Service Name | Expected Count |
|-------------|----------------|
| API-GATEWAY | 1 |
| AUTH-SERVICE | 1 |
| USER-SERVICE | 1 |
| ACCOUNT-SERVICE | 1 |
| TRANSACTION-SERVICE | 1 |
| BENEFICIARY-SERVICE | 1 |
| NOTIFICATION-SERVICE | 1 |
| FRAUD-SERVICE | 1 |
| AUDIT-SERVICE | 1 |

You can also query the REST API:
```bash
curl -u eureka:eureka123 http://localhost:8761/eureka/apps
```

---

## Health Check

```bash
curl http://localhost:8761/actuator/health
```

Expected: `{"status":"UP"}`
