# How to Run NexaBank — Step-by-Step Startup Guide

This guide gets the entire platform running from scratch. Follow every step in order.

---

## Prerequisites

Install these tools before you begin:

| Tool | Minimum Version | How to check |
|------|----------------|--------------|
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | 24+ | `docker --version` |
| [Docker Compose](https://docs.docker.com/compose/) | 2.20+ | `docker compose version` |
| [Java JDK](https://adoptium.net/) | 17+ | `java -version` |
| [Apache Maven](https://maven.apache.org/) | 3.9+ | `mvn -version` |
| [Node.js](https://nodejs.org/) | 18+ | `node --version` |
| npm | 9+ | `npm --version` |

> Make sure Docker Desktop is **running** before proceeding.

---

## Step 1 — Get the Code

```bash
git clone <your-repo-url>
cd banking-platform
```

---

## Step 2 — Set Up Environment Variables

```bash
# Windows (Command Prompt)
copy .env.example .env

# macOS / Linux / Git Bash
cp .env.example .env
```

Open `.env` in any text editor and set your passwords. The key variables are:

```
MYSQL_ROOT_PASSWORD=Admin@123
JWT_SECRET=your_256_bit_secret_here
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your_app_password
```

> For Gmail, use an **App Password** (Google Account → Security → 2-Step Verification → App passwords).  
> If you do not need email notifications right now, leave the `MAIL_*` values as-is — the notification service will log errors but everything else will work.

---

## Step 3 — Build All Backend Services

Run this once from the project root. It compiles all 11 Spring Boot services and creates their JAR files.

```bash
cd backend
mvn clean package -DskipTests
cd ..
```

Expected output: `BUILD SUCCESS` for all modules. This takes **2–4 minutes** on first run.

---

## Step 4 — Start the Backend with Docker Compose

```bash
docker compose up -d
```

This single command starts **25 containers**:
- 8 × MySQL 8.0 databases (one per service)
- Redis 7.2
- Zookeeper + Apache Kafka
- Config Server
- Eureka Service Discovery
- API Gateway
- 8 × Business microservices
- Prometheus + Grafana

---

## Step 5 — Wait for Everything to Be Healthy

Services start in dependency order. The full cold start takes **3–5 minutes**.

```bash
# Check that all containers are running (no "Restarting" or "Exit" states)
docker compose ps
```

You should see all containers with status `Up` or `Up (healthy)`.

To watch the startup logs live:

```bash
docker compose logs -f config-server service-discovery api-gateway
```

Press `Ctrl+C` to stop following logs (containers keep running).

### Verify key services are up

```bash
# Config Server
curl http://localhost:8888/actuator/health

# Eureka (Service Discovery)
curl http://localhost:8761/actuator/health

# API Gateway
curl http://localhost:8080/actuator/health
```

Each should return `{"status":"UP"}`.

---

## Step 6 — Start the Angular Frontend

Open a **new terminal window**, then:

```bash
cd frontend/angular-client
npm install
npm start
```

Wait for the output:
```
** Angular Live Development Server is listening on localhost:4200 **
```

---

## Step 7 — Open the Application

Open your browser and go to:

**http://localhost:4200**

You will see the NexaBank login page.

---

## Quick Access — All URLs

| Service | URL | Notes |
|---------|-----|-------|
| **NexaBank App** | http://localhost:4200 | Angular frontend |
| **API Gateway** | http://localhost:8080 | All API calls go here |
| **Eureka Dashboard** | http://localhost:8761 | Registered services |
| **Grafana** | http://localhost:3000 | Login: `admin` / `admin123` |
| **Prometheus** | http://localhost:9090 | Metrics & alert rules |
| Auth Service Swagger | http://localhost:8081/swagger-ui.html | |
| User Service Swagger | http://localhost:8082/swagger-ui.html | |
| Account Service Swagger | http://localhost:8083/swagger-ui.html | |
| Transaction Service Swagger | http://localhost:8084/swagger-ui.html | |
| Beneficiary Service Swagger | http://localhost:8085/swagger-ui.html | |
| Fraud Service Swagger | http://localhost:8087/swagger-ui.html | |
| Audit Service Swagger | http://localhost:8088/swagger-ui.html | |

---

## First-Time Use — Create an Account

1. Go to http://localhost:4200
2. Click **Create Account**
3. Fill in your details (name, email, password, phone)
4. Password must be 8+ characters with an uppercase letter, number, and special character
5. Click **Register** → you are redirected to login
6. Sign in with your email and password
7. From the Dashboard, click **+ New Account** to create a bank account (Savings, Checking, or Investment)
8. Use **Transactions** to deposit, withdraw, or transfer money

---

## Stopping the Application

```bash
# Stop frontend: press Ctrl+C in the terminal running npm start

# Stop all backend containers (data is preserved)
docker compose down

# Stop and wipe all data for a clean reset
docker compose down -v
```

---

## Troubleshooting

### A container keeps restarting

```bash
# See what went wrong
docker compose logs <service-name>

# Example
docker compose logs transaction-service
```

The most common cause is a wrong password in `.env` — the service can't connect to its database.

### Port already in use

```bash
# Windows — find what is using port 8080
netstat -ano | findstr :8080

# Kill it (replace PID with the number shown)
taskkill /PID <PID> /F
```

### Eureka shows no services registered

The business services register themselves after Config Server and Eureka are fully healthy. Wait an extra 60 seconds and refresh http://localhost:8761.

### `BUILD FAILURE` in Step 3

```bash
# Try clearing the Maven cache and rebuilding
cd backend
mvn clean package -DskipTests -U
```

### Angular won't start (`npm start` fails)

```bash
cd frontend/angular-client
rm -rf node_modules   # Windows: rmdir /s /q node_modules
npm install
npm start
```

---

## Rebuilding After Code Changes

If you change any backend service:

```bash
# Rebuild only the changed service (e.g., account-service)
cd backend
mvn clean package -DskipTests -pl services/account-service -am
cd ..

# Restart just that container
docker compose up -d --build account-service
```

If you change the Angular frontend, the dev server (`npm start`) hot-reloads automatically — no restart needed.
