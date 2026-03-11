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

---

## Running Locally (Without Docker)

Use this approach when you want to run and debug individual services directly on your machine — for example, in IntelliJ or VS Code — rather than inside containers.

The strategy is:
- **Infrastructure** (MySQL, Redis, Kafka) still runs in Docker — starting 11 lightweight containers is much easier than installing each tool natively.
- **Spring Boot services** and the **Angular frontend** run directly on your machine.

---

### Local Prerequisites

Everything from the main prerequisites table, plus:

| Tool | Purpose |
|------|---------|
| MySQL 8.0 client (`mysql` CLI) | Verify DB connectivity |
| A Java IDE (IntelliJ IDEA / VS Code + Extension Pack for Java) | Recommended for debugging |

---

### Local Step 1 — Start Only the Infrastructure Containers

```bash
docker compose up -d \
  mysql-auth mysql-user mysql-account mysql-transaction \
  mysql-beneficiary mysql-notification mysql-fraud mysql-audit \
  redis zookeeper kafka
```

Wait for all containers to become healthy (~30 seconds):

```bash
docker compose ps
```

Verify MySQL is accessible (password is `Admin@123`):

```bash
mysql -h 127.0.0.1 -P 3306 -u root -pAdmin@123 -e "SHOW DATABASES;"
```

---

### Local Step 2 — Start Config Server

Open a dedicated terminal window for each service below, or run them as IDE run configurations.

```bash
cd backend/infrastructure/config-server
mvn spring-boot:run
```

Wait until you see:
```
Started ConfigServerApplication in X.XXX seconds
```

Verify:
```bash
curl http://localhost:8888/actuator/health
# {"status":"UP"}
```

---

### Local Step 3 — Start Service Discovery (Eureka)

New terminal:

```bash
cd backend/infrastructure/service-discovery
mvn spring-boot:run
```

Verify at http://localhost:8761 — the Eureka dashboard should load.

---

### Local Step 4 — Start the 8 Microservices

Start each in its own terminal. They can be started in parallel once Eureka is up.

```bash
# Terminal A
cd backend/services/auth-service
mvn spring-boot:run

# Terminal B
cd backend/services/user-service
mvn spring-boot:run

# Terminal C
cd backend/services/account-service
mvn spring-boot:run

# Terminal D
cd backend/services/transaction-service
mvn spring-boot:run

# Terminal E
cd backend/services/beneficiary-service
mvn spring-boot:run

# Terminal F
cd backend/services/notification-service
mvn spring-boot:run

# Terminal G
cd backend/services/fraud-service
mvn spring-boot:run

# Terminal H
cd backend/services/audit-service
mvn spring-boot:run
```

Each service connects to its own MySQL container on `localhost` (ports 3306–3313) using the defaults in `application.yml` (`root` / `Admin@123`).

After all services start, refresh http://localhost:8761 — all 8 services should appear as **UP**.

---

### Local Step 5 — Start the API Gateway

New terminal:

```bash
cd backend/infrastructure/api-gateway
mvn spring-boot:run
```

Verify:
```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

---

### Local Step 6 — Start the Angular Frontend

New terminal:

```bash
cd frontend/angular-client
npm install        # only needed on first run
npm start
```

Open http://localhost:4200 in your browser.

The dev server proxies all `/api/*` calls to `http://localhost:8080` (the local API Gateway), so no config change is needed.

---

### Service Port Reference (Local)

| Service | Port | Database host:port |
|---------|----|-------------------|
| Config Server | 8888 | — |
| Eureka | 8761 | — |
| API Gateway | 8080 | — |
| Auth Service | 8081 | localhost:3306 |
| User Service | 8082 | localhost:3307 |
| Account Service | 8083 | localhost:3308 |
| Transaction Service | 8084 | localhost:3309 |
| Beneficiary Service | 8085 | localhost:3310 |
| Notification Service | 8086 | localhost:3311 |
| Fraud Service | 8087 | localhost:3312 |
| Audit Service | 8088 | localhost:3313 |
| Redis | — | localhost:6379 |
| Kafka | — | localhost:9092 |
| Angular Frontend | 4200 | — |

---

### Running a Single Service in Debug Mode

#### IntelliJ IDEA

1. Open `banking-platform/backend` as a Maven project.
2. Navigate to the service's `*Application.java` main class.
3. Click the **Debug** button (bug icon) next to `main`.
4. Set breakpoints as needed — IntelliJ will stop execution at them.

#### VS Code

1. Open the `backend` folder in VS Code with the **Extension Pack for Java** installed.
2. Open the `*Application.java` file for the service you want to debug.
3. Press `F5` or click **Run → Start Debugging**.
4. VS Code detects the Spring Boot main class automatically.

#### Command line (debug port 5005)

```bash
cd backend/services/auth-service
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

Then attach your IDE's remote debugger to `localhost:5005`.

---

### Stopping Local Services

```bash
# Stop each Spring Boot process with Ctrl+C in its terminal

# Stop infrastructure containers (data preserved)
docker compose stop mysql-auth mysql-user mysql-account mysql-transaction \
  mysql-beneficiary mysql-notification mysql-fraud mysql-audit \
  redis zookeeper kafka

# Or stop and remove everything
docker compose down -v
```

---

### Local Troubleshooting

**Service fails to connect to MySQL**  
Confirm the correct container port is exposed. Each service has its own MySQL container on a different host port:
```bash
docker compose ps | grep mysql
```

**`Access denied for user 'root'@'...'`**  
The container may still be initialising. Wait 10 seconds and retry. If the problem persists:
```bash
docker compose restart mysql-auth   # replace with the failing service's db
```

**Port conflict — another process is using 8081 (or any service port)**  
```bash
# Windows
netstat -ano | findstr :8081
taskkill /PID <PID> /F

# macOS / Linux
lsof -ti :8081 | xargs kill -9
```

**Config Server flapping / services not finding config**  
The config import is declared `optional:`, so services fall back to their local `application.yml` values if Config Server is unreachable. You can skip starting Config Server entirely during local development — all services will work with their bundled defaults.

**Eureka shows a service with status DOWN**  
A service registered but its health check is failing. Check the service's terminal output for stack traces. The most common causes are a failed DB migration (Flyway error) or a missing environment variable.
