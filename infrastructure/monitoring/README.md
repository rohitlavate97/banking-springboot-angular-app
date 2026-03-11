# Monitoring Stack

Pre-configured observability stack for the NexaBank platform, consisting of **Prometheus** (metrics scraping), **Grafana** (dashboards and alerting), and **Alertmanager** (alert routing).

---

## Accessing the UIs

| Service | URL | Default Credentials |
|---------|-----|---------------------|
| Grafana | http://localhost:3000 | `admin` / `admin123` |
| Prometheus | http://localhost:9090 | none |
| Alertmanager | http://localhost:9093 | none |

---

## Starting the Stack

```bash
# Start only the monitoring services
docker compose up -d prometheus grafana alertmanager

# Or start everything together
docker compose up -d
```

---

## Prometheus

### Configuration

`infrastructure/monitoring/prometheus/prometheus.yml` defines scrape targets:

| Job | Endpoint | Interval |
|-----|----------|---------|
| `config-server` | `:8888/actuator/prometheus` | 15s |
| `service-discovery` | `:8761/actuator/prometheus` | 15s |
| `api-gateway` | `:8080/actuator/prometheus` | 15s |
| `auth-service` | `:8081/actuator/prometheus` | 15s |
| `user-service` | `:8082/actuator/prometheus` | 15s |
| `account-service` | `:8083/actuator/prometheus` | 15s |
| `transaction-service` | `:8084/actuator/prometheus` | 15s |
| `beneficiary-service` | `:8085/actuator/prometheus` | 15s |
| `notification-service` | `:8086/actuator/prometheus` | 15s |
| `fraud-service` | `:8087/actuator/prometheus` | 15s |
| `audit-service` | `:8088/actuator/prometheus` | 15s |

View scrape health at: http://localhost:9090/targets

### Alert Rules

`infrastructure/monitoring/prometheus/alert_rules.yml`:

| Rule | Condition | Severity |
|------|-----------|---------|
| `ServiceDown` | Instance unreachable > 1 min | critical |
| `HighErrorRate` | HTTP 5xx > 5% of requests | critical |
| `HighApiLatency` | p95 latency > 2s over 5 min | warning |
| `CircuitBreakerOpen` | Resilience4j CB state = open | critical |
| `KafkaConsumerLag` | Consumer lag > 1000 messages | warning |
| `JvmHeapHigh` | JVM heap usage > 85% | warning |

---

## Grafana

### Pre-Provisioned Dashboards

Dashboards auto-load from `infrastructure/monitoring/grafana/dashboards/`:

| Dashboard | Description |
|-----------|-------------|
| **Banking Overview** | Platform-wide request rate, error rate, latency, active services |
| **JVM Metrics** | Heap/non-heap memory, GC pause times, thread counts per service |
| **Kafka Consumers** | Consumer group lag per topic partition |
| **Transaction Saga** | Saga completion rate, compensation rate, average duration |

### First-Time Login

1. Open http://localhost:3000
2. Sign in with `admin` / `admin123`
3. Navigate to **Dashboards → Browse** to see all provisioned dashboards
4. Change the default password under **Profile → Change password**

### Datasource

Prometheus is pre-configured as a datasource via `infrastructure/monitoring/grafana/provisioning/datasources/prometheus.yml`. No manual setup required.

---

## Alertmanager

`infrastructure/monitoring/alertmanager/alertmanager.yml` routes alerts:

- **Critical alerts** → configured receiver (edit file to add Slack webhook, PagerDuty, email)
- **Warning alerts** → email receiver (configure `ALERTMANAGER_EMAIL` in `.env`)

To add a Slack webhook:

```yaml
receivers:
  - name: 'slack-critical'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
        channel: '#alerts'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ .CommonAnnotations.description }}'
```

---

## Custom Queries (Prometheus)

Useful PromQL queries to run at http://localhost:9090:

```promql
# Request rate per service (last 2 min)
sum(rate(http_server_requests_seconds_count[2m])) by (job)

# Error rate per service
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (job)
  /
sum(rate(http_server_requests_seconds_count[5m])) by (job)

# Kafka consumer lag per group
kafka_consumer_group_lag

# JVM heap usage percentage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}

# Resilience4j circuit breaker state (0=closed, 1=open, 2=half-open)
resilience4j_circuitbreaker_state
```
