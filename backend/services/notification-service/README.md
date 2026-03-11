# Notification Service

Purely event-driven service that sends email notifications to users when transactions complete, fail, or trigger fraud alerts. It has **no REST API** and is not registered as a routable path in the API Gateway.

---

## Port

`8086` (management/actuator only)

---

## Running Locally

### Prerequisites
- Config Server: `:8888`
- Service Discovery: `:8761`
- Kafka broker accessible (e.g., `localhost:9092`)
- SMTP server credentials (see env vars below)

```bash
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=your-app@gmail.com
export MAIL_PASSWORD=your_app_password

cd backend/services/notification-service
mvn spring-boot:run
```

### With Docker

```bash
docker compose up -d notification-service
```

---

## Event Consumers

The service subscribes to three Kafka topics:

| Topic | Group ID | Trigger | Email Sent |
|-------|----------|---------|------------|
| `transaction-completed` | `notification-group` | Transfer/deposit/withdrawal succeeded | "Your transaction of $X was successful" |
| `transaction-failed` | `notification-group` | Saga rollback or processing failure | "Your transaction of $X has failed" |
| `fraud-alerts` | `notification-group` | Fraud rule triggered | "We detected suspicious activity on your account" |

---

## Email Templates

Emails are plain-text by default. Subject lines follow this pattern:

| Event | Subject |
|-------|---------|
| Completed | `Transaction Successful — Ref #<id>` |
| Failed | `Transaction Failed — Ref #<id>` |
| Fraud alert | `Security Alert — Suspicious Activity Detected` |

---

## SMTP Configuration

### Using Gmail

1. Enable **2-Step Verification** in your Google account.
2. Go to **Security → App Passwords**.
3. Generate an app password for "Mail / Other".
4. Use that 16-character password as `MAIL_PASSWORD`.

```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=you@gmail.com
MAIL_PASSWORD=abcd efgh ijkl mnop
```

### Using Mailtrap (development)

[Mailtrap.io](https://mailtrap.io) is useful for testing — emails are captured and never delivered to real recipients.

```
MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=<mailtrap-user>
MAIL_PASSWORD=<mailtrap-password>
```

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `MAIL_HOST` | Yes | SMTP server hostname |
| `MAIL_PORT` | Yes | SMTP port (587 for STARTTLS, 465 for SSL) |
| `MAIL_USERNAME` | Yes | SMTP login username |
| `MAIL_PASSWORD` | Yes | SMTP login password |
| `KAFKA_BOOTSTRAP_SERVERS` | Yes | Kafka broker(s), e.g. `localhost:9092` |
| `CONFIG_SERVER_PASSWORD` | Yes | Config Server auth |
| `EUREKA_PASSWORD` | Yes | Eureka auth |

---

## Health Check

```bash
curl http://localhost:8086/actuator/health
```

No REST endpoints are exposed beyond Spring Actuator.
