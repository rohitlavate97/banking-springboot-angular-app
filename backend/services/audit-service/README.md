# Audit Service

Append-only audit trail for every significant platform event. Subscribes to all four Kafka topics and persists structured log entries. Records are immutable — there is no delete or update API. Exposes admin-only query endpoints.

---

## Port

`8088`

---

## Running Locally

### Prerequisites
- Config Server: `:8888`
- Service Discovery: `:8761`
- Kafka broker accessible (e.g., `localhost:9092`)
- PostgreSQL on `:5439` with database `audit_db`

```bash
export POSTGRES_AUDIT_PASSWORD=audit_secret_123

cd backend/services/audit-service
mvn spring-boot:run
```

### With Docker

```bash
docker compose up -d audit-service
```

---

## Database

- **Host:** `localhost:5439` (local) or `postgres-audit:5432` (Docker)
- **Database:** `audit_db`

Schema tables: `audit_logs`

The `AuditLog` entity is annotated `@Immutable`. Hibernate will **never** issue an `UPDATE` statement against this table. All records are write-once.

---

## Kafka Consumers

The service subscribes to all platform event topics using a generic `Map<String, Object>` deserialization to avoid class coupling:

| Topic | Group ID | Example events captured |
|-------|----------|------------------------|
| `transaction-events` | `audit-group` | Transaction initiated |
| `transaction-completed` | `audit-group` | Transaction confirmed |
| `transaction-failed` | `audit-group` | Saga rollback |
| `fraud-alerts` | `audit-group` | Fraud rule triggered |

Each consumed event is stored with:
- **entityType** — derived from topic name (e.g., `TRANSACTION`, `FRAUD_ALERT`)
- **entityId** — extracted from the event payload
- **userId** — extracted from the event payload
- **action** — e.g., `CREATED`, `COMPLETED`, `FAILED`, `FRAUD_DETECTED`
- **details** — full raw JSON payload serialized as a string
- **createdAt** — server timestamp (immutable)

---

## REST API (Admin Only)

All endpoints require `ROLE_ADMIN`.

### List All Audit Logs

```http
GET /api/audit/logs?page=0&size=50
Authorization: Bearer <admin-token>
```

**Response `200`:**
```json
{
  "content": [
    {
      "id": 1,
      "entityType": "TRANSACTION",
      "entityId": "77",
      "userId": "42",
      "action": "COMPLETED",
      "details": "{\"amount\":500.00,\"type\":\"TRANSFER\",...}",
      "createdAt": "2025-01-20T15:00:00"
    }
  ],
  "totalElements": 348,
  "totalPages": 7,
  "size": 50,
  "number": 0
}
```

### Logs by User

```http
GET /api/audit/logs/user/{userId}
Authorization: Bearer <admin-token>
```

Returns all audit entries associated with the specified user ID.

### Logs by Entity

```http
GET /api/audit/logs/entity/{entityType}/{entityId}
Authorization: Bearer <admin-token>
```

Examples:
```
GET /api/audit/logs/entity/TRANSACTION/77
GET /api/audit/logs/entity/FRAUD_ALERT/5
```

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `POSTGRES_AUDIT_PASSWORD` | Yes | PostgreSQL password |
| `KAFKA_BOOTSTRAP_SERVERS` | Yes | Kafka broker(s) |
| `CONFIG_SERVER_PASSWORD` | Yes | Config Server auth |
| `EUREKA_PASSWORD` | Yes | Eureka auth |

---

## Swagger UI

http://localhost:8088/swagger-ui.html
