# Config Server

Centralized configuration service powered by **Spring Cloud Config Server**. All other microservices read their configuration from here at startup.

---

## Port

`8888`

---

## Responsibilities

- Serves `application.yml` properties to all registered services
- Secured with HTTP Basic Auth
- Supports Spring profiles (`default`, `docker`, `prod`)

---

## Running Locally

### Prerequisites
- Java 17+ installed
- No external dependencies required (serves files from classpath or a Git repository)

```bash
cd backend/infrastructure/config-server
mvn spring-boot:run
```

Verify it's running:
```bash
curl -u config:config123 http://localhost:8888/application/default
```

### With Docker

```bash
docker compose up -d config-server
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8888` | HTTP port |
| `CONFIG_SERVER_PASSWORD` | `config_secret_change_me` | Basic auth password for `config` user |

---

## Configuration

`src/main/resources/application.yml`:

```yaml
server:
  port: 8888

spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config
  profiles:
    active: native
```

---

## Security

All service-to-server communication uses HTTP Basic Auth:
- **Username:** `config`
- **Password:** value of `CONFIG_SERVER_PASSWORD` env variable

> **Important:** Change the default password before deploying.

---

## Health Check

```bash
curl http://localhost:8888/actuator/health
```

Expected: `{"status":"UP"}`
