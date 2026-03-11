# Manual Testing Guide — Banking Platform

All requests go through the **API Gateway** at `http://localhost:8080`.
Replace token placeholders like `<ACCESS_TOKEN>` with real values as you progress through the steps.

---

## Prerequisites

- Docker Desktop is running
- Application stack is up (`docker compose up -d`)
- Wait ~60 seconds after startup for all services to register with Eureka

Verify health before testing:
```bash
curl http://localhost:8080/actuator/health
# Expect: {"status":"UP"}
```

---

## Swagger UI (Optional)

Each service exposes its own Swagger UI if you prefer a browser-based interface:

| Service | URL |
|---------|-----|
| Auth | http://localhost:8081/swagger-ui/index.html |
| User | http://localhost:8082/swagger-ui/index.html |
| Account | http://localhost:8083/swagger-ui/index.html |
| Transaction | http://localhost:8084/swagger-ui/index.html |
| Beneficiary | http://localhost:8085/swagger-ui/index.html |
| Fraud | http://localhost:8087/swagger-ui/index.html |
| Audit | http://localhost:8088/swagger-ui/index.html |

---

## Test 1 — Register a Regular User

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "Password123!"
  }'
```

**Expected response `200 OK`:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "role": "ROLE_USER",
  "email": "alice@example.com"
}
```

Save the `accessToken` value as `USER_TOKEN` and the `refreshToken` as `REFRESH_TOKEN`.

---

## Test 2 — Register an Admin User

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "Admin123!"
  }'
```

> **Note:** Depending on your implementation, admin accounts may need to be seeded directly in the database or promoted via a DB query. If the role field defaults to `ROLE_USER`, run:
> ```sql
> UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'admin@example.com';
> ```
> Then log in again (Test 3) to get a token with the admin role.

Save the admin `accessToken` as `ADMIN_TOKEN`.

---

## Test 3 — Login

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "Password123!"
  }'
```

**Expected response `200 OK`:** Same shape as register. Overwrite `USER_TOKEN` with the new `accessToken`.

**Negative test — wrong password:**
```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "wrongpassword"
  }'
```
**Expected:** `401 Unauthorized`

---

## Test 4 — Refresh Access Token

```bash
curl -s -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<REFRESH_TOKEN>"
  }'
```

**Expected response `200 OK`:** New `accessToken` and `refreshToken` pair.

---

## Test 5 — Access a Protected Route Without Token

```bash
curl -s -X GET http://localhost:8080/api/users/me
```

**Expected:** `401 Unauthorized`

---

## Test 6 — Create User Profile

After registration an empty profile exists. Fill it in:

```bash
curl -s -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Alice",
    "lastName": "Smith",
    "phoneNumber": "+14155552671",
    "dateOfBirth": "1990-06-15",
    "nationalId": "AB123456",
    "nationality": "US",
    "address": {
      "street": "123 Maple Street",
      "city": "San Francisco",
      "state": "CA",
      "postalCode": "94102",
      "country": "US"
    }
  }'
```

**Expected response `200 OK`:**
```json
{
  "id": "...",
  "firstName": "Alice",
  "lastName": "Smith",
  "email": "alice@example.com",
  "kycStatus": "PENDING",
  ...
}
```

---

## Test 7 — Get Own Profile

```bash
curl -s -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <USER_TOKEN>"
```

**Expected response `200 OK`:** Full profile object with `kycStatus: PENDING`.

---

## Test 8 — Admin: Approve KYC

First, get Alice's user ID from the profile response above (the `id` field). Then use the admin token:

```bash
curl -s -X PUT "http://localhost:8080/api/users/<ALICE_USER_ID>/kyc?status=APPROVED" \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**Expected response `200 OK`:** Profile object with `kycStatus: APPROVED`.

**Negative test — regular user cannot approve KYC:**
```bash
curl -s -X PUT "http://localhost:8080/api/users/<ALICE_USER_ID>/kyc?status=APPROVED" \
  -H "Authorization: Bearer <USER_TOKEN>"
```
**Expected:** `403 Forbidden`

---

## Test 9 — Create a Checking Account

```bash
curl -s -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "accountType": "CHECKING",
    "currency": "USD",
    "alias": "My Main Account"
  }'
```

**Expected response `201 Created`:**
```json
{
  "id": "...",
  "accountNumber": "...",
  "accountType": "CHECKING",
  "status": "ACTIVE",
  "balance": 0.00,
  "availableBalance": 0.00,
  "currency": "USD",
  "alias": "My Main Account"
}
```

Save the `accountNumber` as `ACCOUNT_A`.

---

## Test 10 — Create a Savings Account

```bash
curl -s -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "accountType": "SAVINGS",
    "currency": "USD",
    "alias": "Emergency Fund"
  }'
```

Save this `accountNumber` as `ACCOUNT_B`.

---

## Test 11 — List All Accounts

```bash
curl -s -X GET http://localhost:8080/api/accounts \
  -H "Authorization: Bearer <USER_TOKEN>"
```

**Expected response `200 OK`:** Array containing both `ACCOUNT_A` and `ACCOUNT_B`.

---

## Test 12 — Get Account by Number

```bash
curl -s -X GET "http://localhost:8080/api/accounts/number/<ACCOUNT_A>" \
  -H "Authorization: Bearer <USER_TOKEN>"
```

**Expected response `200 OK`:** Single account object.

---

## Test 13 — Deposit Funds

```bash
curl -s -X POST http://localhost:8080/api/transactions/deposit \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "DEPOSIT",
    "sourceAccountNumber": "<ACCOUNT_A>",
    "destinationAccountNumber": "<ACCOUNT_A>",
    "amount": 5000.00,
    "currency": "USD",
    "description": "Initial deposit"
  }'
```

**Expected response `200 OK`:**
```json
{
  "referenceNumber": "TXN-...",
  "type": "DEPOSIT",
  "status": "COMPLETED",
  "amount": 5000.00,
  "currency": "USD"
}
```

Verify the balance updated by re-running Test 12.

---

## Test 14 — Withdraw Funds

```bash
curl -s -X POST http://localhost:8080/api/transactions/withdraw \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "WITHDRAWAL",
    "sourceAccountNumber": "<ACCOUNT_A>",
    "destinationAccountNumber": "<ACCOUNT_A>",
    "amount": 200.00,
    "currency": "USD",
    "description": "ATM withdrawal"
  }'
```

**Expected response `200 OK`:** Transaction with `status: COMPLETED`. Account balance should now be `4800.00`.

**Negative test — withdraw more than balance:**
```bash
curl -s -X POST http://localhost:8080/api/transactions/withdraw \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "WITHDRAWAL",
    "sourceAccountNumber": "<ACCOUNT_A>",
    "destinationAccountNumber": "<ACCOUNT_A>",
    "amount": 99999.00,
    "currency": "USD",
    "description": "Overdraft attempt"
  }'
```
**Expected:** `400 Bad Request` or transaction with `status: FAILED`.

---

## Test 15 — Transfer Between Accounts

```bash
curl -s -X POST http://localhost:8080/api/transactions/transfer \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "TRANSFER",
    "sourceAccountNumber": "<ACCOUNT_A>",
    "destinationAccountNumber": "<ACCOUNT_B>",
    "amount": 1000.00,
    "currency": "USD",
    "description": "Move to savings"
  }'
```

**Expected response `200 OK`:** Transaction with `status: COMPLETED`.  
Verify: `ACCOUNT_A` balance = `3800.00`, `ACCOUNT_B` balance = `1000.00`.

---

## Test 16 — List Transaction History

```bash
curl -s -X GET "http://localhost:8080/api/transactions?page=0&size=10" \
  -H "Authorization: Bearer <USER_TOKEN>"
```

**Expected response `200 OK`:** Paginated list containing the 3 transactions above, newest first.

---

## Test 17 — Get Transaction by Reference Number

Use a `referenceNumber` from any previous transaction response:

```bash
curl -s -X GET "http://localhost:8080/api/transactions/reference/<REFERENCE_NUMBER>" \
  -H "Authorization: Bearer <USER_TOKEN>"
```

**Expected response `200 OK`:** Single transaction object.

---

## Test 18 — Add a Beneficiary

```bash
curl -s -X POST http://localhost:8080/api/beneficiaries \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "Bob Jones",
    "accountNumber": "9876543210",
    "accountHolderName": "Robert Jones",
    "bankName": "Chase Bank",
    "bankCode": "CHASUS33",
    "currency": "USD"
  }'
```

**Expected response `201 Created`:**
```json
{
  "id": "...",
  "nickname": "Bob Jones",
  "accountNumber": "9876543210",
  "active": true
}
```

Save the `id` as `BENEFICIARY_ID`.

**Negative test — duplicate account number for same user:**  
Repeat the exact same request. **Expected:** `409 Conflict` or `400 Bad Request`.

---

## Test 19 — List Beneficiaries

```bash
curl -s -X GET http://localhost:8080/api/beneficiaries \
  -H "Authorization: Bearer <USER_TOKEN>"
```

**Expected response `200 OK`:** Array containing Bob Jones.

---

## Test 20 — Get Beneficiary by ID

```bash
curl -s -X GET "http://localhost:8080/api/beneficiaries/<BENEFICIARY_ID>" \
  -H "Authorization: Bearer <USER_TOKEN>"
```

**Expected response `200 OK`:** Single beneficiary object.

---

## Test 21 — Delete a Beneficiary

```bash
curl -s -X DELETE "http://localhost:8080/api/beneficiaries/<BENEFICIARY_ID>" \
  -H "Authorization: Bearer <USER_TOKEN>"
```

**Expected response `204 No Content`.**

Verify deletion:
```bash
curl -s -X GET http://localhost:8080/api/beneficiaries \
  -H "Authorization: Bearer <USER_TOKEN>"
```
**Expected:** Empty array `[]`.

---

## Test 22 — Trigger Fraud Detection (Large Transaction)

Fraud detection fires automatically when a transaction exceeds the threshold (default $10,000). First deposit enough funds to trigger it:

```bash
# Top up account
curl -s -X POST http://localhost:8080/api/transactions/deposit \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "DEPOSIT",
    "sourceAccountNumber": "<ACCOUNT_A>",
    "destinationAccountNumber": "<ACCOUNT_A>",
    "amount": 50000.00,
    "currency": "USD",
    "description": "Large deposit"
  }'

# Now trigger fraud alert with a large transfer
curl -s -X POST http://localhost:8080/api/transactions/transfer \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "TRANSFER",
    "sourceAccountNumber": "<ACCOUNT_A>",
    "destinationAccountNumber": "<ACCOUNT_B>",
    "amount": 15000.00,
    "currency": "USD",
    "description": "Large transfer test"
  }'
```

The transaction should complete, and a fraud alert should be created asynchronously within a few seconds.

---

## Test 23 — Admin: List Fraud Alerts

```bash
curl -s -X GET "http://localhost:8080/api/fraud/alerts?page=0&size=10" \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**Expected response `200 OK`:** Paginated list including the alert triggered in Test 22.  
Save the alert `id` as `FRAUD_ALERT_ID`.

---

## Test 24 — Admin: Update Fraud Alert Status

```bash
curl -s -X PATCH "http://localhost:8080/api/fraud/alerts/<FRAUD_ALERT_ID>/status" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "FALSE_POSITIVE"
  }'
```

**Expected response `200 OK`:** Alert with updated `status: FALSE_POSITIVE`.

---

## Test 25 — Admin: View Audit Logs

```bash
curl -s -X GET "http://localhost:8080/api/audit?page=0&size=20" \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**Expected response `200 OK`:** Paginated audit log entries covering registration, profile updates, KYC approval, and all transactions performed in tests above.

---

## Test 26 — Admin: Audit Logs for a Specific User

```bash
curl -s -X GET "http://localhost:8080/api/audit/user/<ALICE_USER_ID>?page=0&size=20" \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**Expected response `200 OK`:** Only audit entries associated with Alice.

---

## Test 27 — Logout

```bash
curl -s -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <USER_TOKEN>"
```

**Expected response `204 No Content`.**

Verify the token is revoked:
```bash
curl -s -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <USER_TOKEN>"
```
**Expected:** `401 Unauthorized`

---

## Test 28 — Rate Limiting

Send more than 40 requests to an auth endpoint in quick succession:

```bash
for i in {1..50}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"alice@example.com","password":"Password123!"}'
done
```

**Expected:** Responses transition from `200` to `429 Too Many Requests` once the burst limit is exceeded.

---

## Test 29 — Infrastructure Health Checks

```bash
# API Gateway
curl http://localhost:8080/actuator/health

# Eureka Dashboard (browser)
# Open: http://localhost:8761

# Grafana Dashboard (browser)
# Open: http://localhost:3000
# Default login: admin / admin_change_me (or value set in .env)
```

All 8 services should appear as UP in Eureka. Grafana dashboards should show live request metrics.

---

## Quick Reference

| Variable | Where to Get It |
|----------|----------------|
| `USER_TOKEN` | `accessToken` from Test 1 or Test 3 |
| `ADMIN_TOKEN` | `accessToken` after promoting admin and logging in |
| `REFRESH_TOKEN` | `refreshToken` from Test 1 |
| `ACCOUNT_A` | `accountNumber` from Test 9 |
| `ACCOUNT_B` | `accountNumber` from Test 10 |
| `ALICE_USER_ID` | `id` from Test 7 response |
| `BENEFICIARY_ID` | `id` from Test 18 response |
| `FRAUD_ALERT_ID` | `id` from Test 23 response |

---

## Expected Final Account Balances

After completing all tests in order (without the rate-limit test affecting balances):

| Account | Starting | Deposits | Withdrawals | Transfers | Expected Balance |
|---------|----------|----------|-------------|-----------|-----------------|
| `ACCOUNT_A` | 0 | +5000 +50000 | -200 | -1000 -15000 | 38800.00 |
| `ACCOUNT_B` | 0 | — | — | +1000 +15000 | 16000.00 |
