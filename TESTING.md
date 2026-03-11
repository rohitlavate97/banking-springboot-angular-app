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

---

## Manual Test Scenarios

The scenarios below are structured for a QA tester. Each scenario has a unique ID, preconditions, steps, and expected result. Mark each as **Pass / Fail / Blocked** during your test run.

---

### AUTH-001 — Register with already used email

| Field | Value |
|-------|-------|
| **Precondition** | `alice@example.com` already registered (Test 1 complete) |
| **Steps** | POST `/api/auth/register` with `email: alice@example.com` |
| **Expected** | `409 Conflict` — duplicate email rejected |
| **Result** | Pass / Fail / Blocked |

---

### AUTH-002 — Register with weak password

| Field | Value |
|-------|-------|
| **Precondition** | None |
| **Steps** | POST `/api/auth/register` with `password: "abc"` (fewer than 8 characters) |
| **Expected** | `400 Bad Request` with validation error referencing `password` |
| **Result** | Pass / Fail / Blocked |

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"newuser@example.com","password":"abc"}'
```

---

### AUTH-003 — Register with invalid email format

| Field | Value |
|-------|-------|
| **Precondition** | None |
| **Steps** | POST `/api/auth/register` with `email: "not-an-email"` |
| **Expected** | `400 Bad Request` with validation error referencing `email` |
| **Result** | Pass / Fail / Blocked |

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"not-an-email","password":"Password123!"}'
```

---

### AUTH-004 — Login with non-existent email

| Field | Value |
|-------|-------|
| **Precondition** | None |
| **Steps** | POST `/api/auth/login` with an email that was never registered |
| **Expected** | `401 Unauthorized` — must NOT reveal whether the email exists |
| **Result** | Pass / Fail / Blocked |

---

### AUTH-005 — Use expired / tampered access token

| Field | Value |
|-------|-------|
| **Precondition** | None |
| **Steps** | Modify one character in the middle of a valid JWT and send it as `Authorization: Bearer <tampered>` |
| **Expected** | `401 Unauthorized` |
| **Result** | Pass / Fail / Blocked |

```bash
curl -s -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJtampered.payload.signature"
```

---

### AUTH-006 — Refresh with an invalid refresh token

| Field | Value |
|-------|-------|
| **Precondition** | None |
| **Steps** | POST `/api/auth/refresh` with `refreshToken: "garbage"` |
| **Expected** | `401 Unauthorized` |
| **Result** | Pass / Fail / Blocked |

---

### AUTH-007 — Use access token after logout

| Field | Value |
|-------|-------|
| **Precondition** | Alice is logged in with a valid `USER_TOKEN` |
| **Steps** | 1. POST `/api/auth/logout` with `USER_TOKEN` → expect `204`. 2. GET `/api/users/me` with the same `USER_TOKEN` |
| **Expected** | Step 2 returns `401 Unauthorized` — revoked token no longer valid |
| **Result** | Pass / Fail / Blocked |

---

### USER-001 — Update profile with invalid phone number

| Field | Value |
|-------|-------|
| **Precondition** | Alice is logged in |
| **Steps** | PUT `/api/users/me` with `phoneNumber: "12345"` (too short, no country code) |
| **Expected** | `400 Bad Request` referencing `phoneNumber` |
| **Result** | Pass / Fail / Blocked |

```bash
curl -s -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Alice","lastName":"Smith","phoneNumber":"12345"}'
```

---

### USER-002 — Update profile with future date of birth

| Field | Value |
|-------|-------|
| **Precondition** | Alice is logged in |
| **Steps** | PUT `/api/users/me` with `dateOfBirth: "2099-01-01"` |
| **Expected** | `400 Bad Request` — date must be in the past |
| **Result** | Pass / Fail / Blocked |

---

### USER-003 — Regular user cannot access another user's profile

| Field | Value |
|-------|-------|
| **Precondition** | Alice (`USER_TOKEN`) and Bob (`BOB_TOKEN`) are both registered |
| **Steps** | GET `/api/users/<ALICE_USER_ID>` using `BOB_TOKEN` |
| **Expected** | `403 Forbidden` — only admins can fetch profiles by ID |
| **Result** | Pass / Fail / Blocked |

---

### USER-004 — Admin fetches profile by user ID

| Field | Value |
|-------|-------|
| **Precondition** | Admin token available, Alice's user ID known |
| **Steps** | GET `/api/users/<ALICE_USER_ID>` using `ADMIN_TOKEN` |
| **Expected** | `200 OK` with full profile |
| **Result** | Pass / Fail / Blocked |

---

### USER-005 — KYC rejection

| Field | Value |
|-------|-------|
| **Precondition** | Alice's KYC is `PENDING` |
| **Steps** | PUT `/api/users/<ALICE_USER_ID>/kyc?status=REJECTED` using `ADMIN_TOKEN` |
| **Expected** | `200 OK` with `kycStatus: REJECTED` |
| **Result** | Pass / Fail / Blocked |

---

### ACCOUNT-001 — Create account with invalid currency code

| Field | Value |
|-------|-------|
| **Precondition** | Alice is logged in |
| **Steps** | POST `/api/accounts` with `currency: "DOLLARS"` (not a valid ISO 4217 3-letter code) |
| **Expected** | `400 Bad Request` |
| **Result** | Pass / Fail / Blocked |

```bash
curl -s -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"accountType":"CHECKING","currency":"DOLLARS"}'
```

---

### ACCOUNT-002 — Create account with invalid type

| Field | Value |
|-------|-------|
| **Precondition** | Alice is logged in |
| **Steps** | POST `/api/accounts` with `accountType: "BITCOIN"` |
| **Expected** | `400 Bad Request` |
| **Result** | Pass / Fail / Blocked |

---

### ACCOUNT-003 — Get account that belongs to another user

| Field | Value |
|-------|-------|
| **Precondition** | Bob has account `BOB_ACCOUNT`. Alice has `USER_TOKEN`. |
| **Steps** | GET `/api/accounts/<BOB_ACCOUNT_ID>` using Alice's `USER_TOKEN` |
| **Expected** | `403 Forbidden` or `404 Not Found` — Alice cannot see Bob's account |
| **Result** | Pass / Fail / Blocked |

---

### TXN-001 — Deposit zero amount

| Field | Value |
|-------|-------|
| **Precondition** | Alice is logged in, `ACCOUNT_A` exists |
| **Steps** | POST `/api/transactions/deposit` with `amount: 0` |
| **Expected** | `400 Bad Request` — minimum amount is `0.01` |
| **Result** | Pass / Fail / Blocked |

```bash
curl -s -X POST http://localhost:8080/api/transactions/deposit \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"type":"DEPOSIT","sourceAccountNumber":"<ACCOUNT_A>","destinationAccountNumber":"<ACCOUNT_A>","amount":0,"currency":"USD"}'
```

---

### TXN-002 — Deposit negative amount

| Field | Value |
|-------|-------|
| **Precondition** | Alice is logged in, `ACCOUNT_A` exists |
| **Steps** | POST `/api/transactions/deposit` with `amount: -50.00` |
| **Expected** | `400 Bad Request` |
| **Result** | Pass / Fail / Blocked |

---

### TXN-003 — Withdraw more than available balance

| Field | Value |
|-------|-------|
| **Precondition** | `ACCOUNT_A` has balance `5000.00` |
| **Steps** | POST `/api/transactions/withdraw` with `amount: 9999.00` |
| **Expected** | `400 Bad Request` or transaction with `status: FAILED` — insufficient funds |
| **Result** | Pass / Fail / Blocked |

---

### TXN-004 — Transfer to non-existent account

| Field | Value |
|-------|-------|
| **Precondition** | `ACCOUNT_A` has sufficient balance |
| **Steps** | POST `/api/transactions/transfer` with `destinationAccountNumber: "0000000000"` (does not exist) |
| **Expected** | `404 Not Found` or transaction with `status: FAILED` |
| **Result** | Pass / Fail / Blocked |

---

### TXN-005 — Transfer to own account (same source and destination)

| Field | Value |
|-------|-------|
| **Precondition** | `ACCOUNT_A` has sufficient balance |
| **Steps** | POST `/api/transactions/transfer` with `sourceAccountNumber` = `destinationAccountNumber` = `ACCOUNT_A` |
| **Expected** | `400 Bad Request` — self-transfer should be rejected |
| **Result** | Pass / Fail / Blocked |

---

### TXN-006 — Access transaction that belongs to another user

| Field | Value |
|-------|-------|
| **Precondition** | Bob has performed a transaction. Alice has `USER_TOKEN`. |
| **Steps** | GET `/api/transactions/<BOB_TXN_ID>` using Alice's `USER_TOKEN` |
| **Expected** | `403 Forbidden` or `404 Not Found` |
| **Result** | Pass / Fail / Blocked |

---

### TXN-007 — Pagination boundary — last page

| Field | Value |
|-------|-------|
| **Precondition** | 3 transactions exist for Alice |
| **Steps** | GET `/api/transactions?page=0&size=2` then `page=1&size=2` |
| **Expected** | Page 0 → 2 items; Page 1 → 1 item; Page 2 → empty list (not an error) |
| **Result** | Pass / Fail / Blocked |

---

### TXN-008 — Currency mismatch on transfer

| Field | Value |
|-------|-------|
| **Precondition** | `ACCOUNT_A` is `USD`, `ACCOUNT_B` is `USD` |
| **Steps** | POST `/api/transactions/transfer` with `currency: "EUR"` |
| **Expected** | `400 Bad Request` — currency must match the account's currency, or be explicitly handled |
| **Result** | Pass / Fail / Blocked |

---

### BEN-001 — Create beneficiary with missing required fields

| Field | Value |
|-------|-------|
| **Precondition** | Alice is logged in |
| **Steps** | POST `/api/beneficiaries` omitting `accountHolderName` |
| **Expected** | `400 Bad Request` with field-level validation error |
| **Result** | Pass / Fail / Blocked |

```bash
curl -s -X POST http://localhost:8080/api/beneficiaries \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"nickname":"Bob","accountNumber":"9876543210","bankName":"Chase"}'
```

---

### BEN-002 — Duplicate beneficiary (same user + account number)

| Field | Value |
|-------|-------|
| **Precondition** | Beneficiary for `9876543210` already exists for Alice |
| **Steps** | POST `/api/beneficiaries` again with the same `accountNumber` |
| **Expected** | `409 Conflict` — unique constraint on (user_id, account_number) |
| **Result** | Pass / Fail / Blocked |

---

### BEN-003 — Delete a beneficiary that doesn't exist

| Field | Value |
|-------|-------|
| **Precondition** | Alice is logged in |
| **Steps** | DELETE `/api/beneficiaries/00000000-0000-0000-0000-000000000000` |
| **Expected** | `404 Not Found` |
| **Result** | Pass / Fail / Blocked |

---

### BEN-004 — User cannot delete another user's beneficiary

| Field | Value |
|-------|-------|
| **Precondition** | Bob has a beneficiary `BOB_BEN_ID`. Alice has `USER_TOKEN`. |
| **Steps** | DELETE `/api/beneficiaries/<BOB_BEN_ID>` using Alice's `USER_TOKEN` |
| **Expected** | `403 Forbidden` or `404 Not Found` |
| **Result** | Pass / Fail / Blocked |

---

### FRAUD-001 — Non-admin cannot access fraud alerts

| Field | Value |
|-------|-------|
| **Precondition** | Alice has `USER_TOKEN` (not admin) |
| **Steps** | GET `/api/fraud/alerts` using `USER_TOKEN` |
| **Expected** | `403 Forbidden` |
| **Result** | Pass / Fail / Blocked |

---

### FRAUD-002 — Update fraud alert with invalid status value

| Field | Value |
|-------|-------|
| **Precondition** | A fraud alert exists; `ADMIN_TOKEN` available |
| **Steps** | PATCH `/api/fraud/alerts/<FRAUD_ALERT_ID>/status` with `status: "RESOLVED"` (not a valid enum value) |
| **Expected** | `400 Bad Request` |
| **Result** | Pass / Fail / Blocked |

```bash
curl -s -X PATCH "http://localhost:8080/api/fraud/alerts/<FRAUD_ALERT_ID>/status" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"status":"RESOLVED"}'
```

---

### FRAUD-003 — Rapid transfer fraud trigger

| Field | Value |
|-------|-------|
| **Precondition** | `ACCOUNT_A` has balance ≥ 1500.00. Admin token available. |
| **Steps** | Send 4 transfer requests within 60 seconds (each `amount: 100.00` from `ACCOUNT_A` to `ACCOUNT_B`) |
| **Expected** | A fraud alert with `ruleType: RAPID_TRANSFERS` appears in GET `/api/fraud/alerts` |
| **Result** | Pass / Fail / Blocked |

```bash
for i in 1 2 3 4; do
  curl -s -X POST http://localhost:8080/api/transactions/transfer \
    -H "Authorization: Bearer <USER_TOKEN>" \
    -H "Content-Type: application/json" \
    -d "{\"type\":\"TRANSFER\",\"sourceAccountNumber\":\"<ACCOUNT_A>\",\"destinationAccountNumber\":\"<ACCOUNT_B>\",\"amount\":100.00,\"currency\":\"USD\",\"description\":\"Rapid $i\"}"
done
```

---

### AUDIT-001 — Non-admin cannot access audit logs

| Field | Value |
|-------|-------|
| **Precondition** | Alice has `USER_TOKEN` (not admin) |
| **Steps** | GET `/api/audit` using `USER_TOKEN` |
| **Expected** | `403 Forbidden` |
| **Result** | Pass / Fail / Blocked |

---

### AUDIT-002 — Audit log immutability

| Field | Value |
|-------|-------|
| **Precondition** | At least one audit log entry exists |
| **Steps** | Attempt PUT or DELETE on `/api/audit/<LOG_ID>` with `ADMIN_TOKEN` |
| **Expected** | `405 Method Not Allowed` — audit log is append-only |
| **Result** | Pass / Fail / Blocked |

---

### AUDIT-003 — Audit log created for every transaction

| Field | Value |
|-------|-------|
| **Precondition** | Admin token available |
| **Steps** | 1. Perform a deposit. 2. Note the `referenceNumber`. 3. GET `/api/audit/user/<ALICE_USER_ID>` and search for an entry whose `entityId` matches the reference or transaction ID |
| **Expected** | An audit entry exists for the transaction within a few seconds (async via Kafka) |
| **Result** | Pass / Fail / Blocked |

---

### SEC-001 — SQL injection attempt in login

| Field | Value |
|-------|-------|
| **Precondition** | None |
| **Steps** | POST `/api/auth/login` with `email: "' OR '1'='1"` |
| **Expected** | `400 Bad Request` (invalid email format) — not `200 OK` or `500` |
| **Result** | Pass / Fail / Blocked |

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"' OR '1'='1\",\"password\":\"anything\"}"
```

---

### SEC-002 — XSS payload in profile fields

| Field | Value |
|-------|-------|
| **Precondition** | Alice is logged in |
| **Steps** | PUT `/api/users/me` with `firstName: "<script>alert(1)</script>"` |
| **Expected** | `400 Bad Request`, OR the value stored and returned as an escaped plain string — never executed as HTML |
| **Result** | Pass / Fail / Blocked |

---

### SEC-003 — Access internal account endpoints directly

| Field | Value |
|-------|-------|
| **Precondition** | Any valid `USER_TOKEN` |
| **Steps** | POST `http://localhost:8080/api/accounts/internal/debit` with `accountNumber` and `amount` |
| **Expected** | `403 Forbidden` or `404 Not Found` — internal endpoints must not be exposed via the gateway |
| **Result** | Pass / Fail / Blocked |

---

### SEC-004 — Missing Content-Type header

| Field | Value |
|-------|-------|
| **Precondition** | None |
| **Steps** | POST `/api/auth/login` without `Content-Type: application/json` header, sending a JSON body |
| **Expected** | `415 Unsupported Media Type` |
| **Result** | Pass / Fail / Blocked |

---

### INFRA-001 — Service resilience: restart one service

| Field | Value |
|-------|-------|
| **Precondition** | All services running |
| **Steps** | 1. `docker compose restart user-service`. 2. Wait 30 seconds. 3. GET `/api/users/me` with `USER_TOKEN` |
| **Expected** | After restart and re-registration with Eureka, requests succeed normally |
| **Result** | Pass / Fail / Blocked |

---

### INFRA-002 — Verify Eureka registration

| Field | Value |
|-------|-------|
| **Precondition** | All services running |
| **Steps** | Open `http://localhost:8761` in a browser |
| **Expected** | All 8 business services + API Gateway visible under "Instances currently registered with Eureka" with status UP |
| **Result** | Pass / Fail / Blocked |

---

### INFRA-003 — Prometheus metrics endpoint

| Field | Value |
|-------|-------|
| **Precondition** | All services running |
| **Steps** | `curl http://localhost:8081/actuator/prometheus` |
| **Expected** | Plain text response containing metric lines such as `jvm_memory_used_bytes` and `http_server_requests_seconds_count` |
| **Result** | Pass / Fail / Blocked |

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

---

## UI Testing Scenarios — NexaBank (Angular Frontend)

**Base URL:** `http://localhost:4200`  
**Prerequisite:** Frontend is running (`npm start` from `frontend/angular-client/`) and the backend stack is up.

Each scenario has a unique ID, preconditions, numbered steps, and expected result.  
Mark each as **Pass / Fail / Blocked** after execution.

---

### UI-AUTH-001 — Login page loads and is the default entry for unauthenticated users

| Field | Value |
|-------|-------|
| **Precondition** | Not logged in, no token in localStorage |
| **Steps** | 1. Open `http://localhost:4200` in a fresh browser tab |
| **Expected** | Redirected to `/login`. Login form is visible with **Email** and **Password** fields and a **Login** button. No navbar is shown. |
| **Result** | Pass / Fail / Blocked |

---

### UI-AUTH-002 — Register link navigates to the registration page

| Field | Value |
|-------|-------|
| **Precondition** | On the `/login` page |
| **Steps** | 1. Click the **Register** / "Don't have an account?" link |
| **Expected** | Navigates to `/register`. Registration form is visible with fields for First Name, Last Name, Email, Password, and Confirm Password. |
| **Result** | Pass / Fail / Blocked |

---

### UI-AUTH-003 — Successful registration

| Field | Value |
|-------|-------|
| **Precondition** | On the `/register` page |
| **Steps** | 1. Enter `First Name: Alice`, `Last Name: Smith`, `Email: alice@example.com`, `Password: Password123!`, `Confirm Password: Password123!`. 2. Click **Register**. |
| **Expected** | Request succeeds. User is redirected to `/dashboard` or `/login` with a success message. |
| **Result** | Pass / Fail / Blocked |

---

### UI-AUTH-004 — Register form inline validation

| Field | Value |
|-------|-------|
| **Precondition** | On the `/register` page |
| **Steps** | 1. Click inside the **Email** field then click away without typing. 2. Type `"abc"` in **Password** then click away. 3. Type `"Password123!"` in **Password** and `"different"` in **Confirm Password** then click away. |
| **Expected** | Each field shows an inline error: "Email is required", "Password must be at least 8 characters", "Passwords do not match". The **Register** button remains disabled while errors exist. |
| **Result** | Pass / Fail / Blocked |

---

### UI-AUTH-005 — Register with mismatched passwords

| Field | Value |
|-------|-------|
| **Precondition** | On the `/register` page |
| **Steps** | 1. Fill all fields correctly except set `Password: Password123!` and `Confirm Password: Password999!`. 2. Attempt to submit. |
| **Expected** | Form stays on page; inline error "Passwords do not match" visible; no API call made. |
| **Result** | Pass / Fail / Blocked |

---

### UI-AUTH-006 — Successful login and dashboard redirect

| Field | Value |
|-------|-------|
| **Precondition** | `alice@example.com` is already registered |
| **Steps** | 1. Go to `/login`. 2. Enter correct email and password. 3. Click **Login**. |
| **Expected** | Redirected to `/dashboard`. Navbar appears with links: **Dashboard**, **Accounts**, **Transactions**, **Beneficiaries**, and a **Logout** button. |
| **Result** | Pass / Fail / Blocked |

---

### UI-AUTH-007 — Login with wrong password shows error message

| Field | Value |
|-------|-------|
| **Precondition** | On `/login` page |
| **Steps** | 1. Enter a valid registered email with an incorrect password. 2. Click **Login**. |
| **Expected** | An error message appears on screen (e.g., "Invalid credentials"). User stays on the login page. |
| **Result** | Pass / Fail / Blocked |

---

### UI-AUTH-008 — Login form inline validation

| Field | Value |
|-------|-------|
| **Precondition** | On `/login` page |
| **Steps** | 1. Click **Login** without typing anything. |
| **Expected** | Both Email and Password fields show "required" errors. API not called. |
| **Result** | Pass / Fail / Blocked |

---

### UI-AUTH-009 — Protected route redirects unauthenticated user to login

| Field | Value |
|-------|-------|
| **Precondition** | Not logged in |
| **Steps** | 1. Manually navigate to `http://localhost:4200/accounts` in the address bar |
| **Expected** | Redirected to `/login` by the AuthGuard. |
| **Result** | Pass / Fail / Blocked |

---

### UI-AUTH-010 — Logout clears session and redirects to login

| Field | Value |
|-------|-------|
| **Precondition** | Logged in as Alice |
| **Steps** | 1. Click **Logout** in the navbar. |
| **Expected** | Tokens removed from localStorage. User redirected to `/login`. Navbar disappears. Pressing browser Back does not restore authenticated state. |
| **Result** | Pass / Fail / Blocked |

---

### UI-DASH-001 — Dashboard loads account summary

| Field | Value |
|-------|-------|
| **Precondition** | Logged in as Alice; at least one account exists |
| **Steps** | 1. Navigate to `/dashboard` |
| **Expected** | Dashboard displays a summary of Alice's accounts (account number, type, balance). No console errors. |
| **Result** | Pass / Fail / Blocked |

---

### UI-DASH-002 — Dashboard with no accounts shows empty state

| Field | Value |
|-------|-------|
| **Precondition** | Logged in as a newly registered user with no accounts |
| **Steps** | 1. Navigate to `/dashboard` |
| **Expected** | A meaningful empty state message such as "No accounts yet" or "Open your first account" is shown, not a blank screen or error. |
| **Result** | Pass / Fail / Blocked |

---

### UI-ACCT-001 — Accounts page lists all accounts

| Field | Value |
|-------|-------|
| **Precondition** | Logged in; `ACCOUNT_A` (Checking) and `ACCOUNT_B` (Savings) both exist |
| **Steps** | 1. Click **Accounts** in the navbar |
| **Expected** | Both accounts are listed with their account number, type, balance, and status. |
| **Result** | Pass / Fail / Blocked |

---

### UI-ACCT-002 — Create a new Checking account

| Field | Value |
|-------|-------|
| **Precondition** | Logged in, on the `/accounts` page |
| **Steps** | 1. Click **Create Account** (or equivalent button). 2. Select type **Checking**, enter alias `"Primary Account"`, currency `USD`. 3. Submit. |
| **Expected** | New account appears in the list immediately. Success notification or message displayed. |
| **Result** | Pass / Fail / Blocked |

---

### UI-ACCT-003 — Account balance updates after a transaction

| Field | Value |
|-------|-------|
| **Precondition** | `ACCOUNT_A` balance is `5000.00` |
| **Steps** | 1. Perform a `200.00` deposit via the Transactions page. 2. Navigate back to `/accounts`. |
| **Expected** | `ACCOUNT_A` balance shows `5200.00`. |
| **Result** | Pass / Fail / Blocked |

---

### UI-TXN-001 — Transactions page shows history

| Field | Value |
|-------|-------|
| **Precondition** | Logged in; at least 3 transactions exist |
| **Steps** | 1. Click **Transactions** in the navbar |
| **Expected** | Transaction history is displayed in a table/list showing reference number, type, amount, status, and date. Most recent first. |
| **Result** | Pass / Fail / Blocked |

---

### UI-TXN-002 — Deposit flow

| Field | Value |
|-------|-------|
| **Precondition** | Logged in; `ACCOUNT_A` exists |
| **Steps** | 1. Navigate to `/transactions`. 2. Select action **Deposit**. 3. Choose `ACCOUNT_A` as the destination. 4. Enter amount `500.00` and description `"UI deposit test"`. 5. Click **Submit**. |
| **Expected** | Success message shown. New transaction with type `DEPOSIT` and status `COMPLETED` appears at the top of the transaction list. |
| **Result** | Pass / Fail / Blocked |

---

### UI-TXN-003 — Withdrawal flow

| Field | Value |
|-------|-------|
| **Precondition** | Logged in; `ACCOUNT_A` has balance ≥ 100.00 |
| **Steps** | 1. Select action **Withdraw**. 2. Choose `ACCOUNT_A` as source. 3. Enter amount `100.00`. 4. Submit. |
| **Expected** | Transaction with type `WITHDRAWAL` and `COMPLETED` status appears in history. Account balance decreases by `100.00`. |
| **Result** | Pass / Fail / Blocked |

---

### UI-TXN-004 — Transfer flow

| Field | Value |
|-------|-------|
| **Precondition** | Logged in; `ACCOUNT_A` has balance ≥ 200.00; `ACCOUNT_B` exists |
| **Steps** | 1. Select action **Transfer**. 2. Choose `ACCOUNT_A` as source and `ACCOUNT_B` as destination. 3. Enter amount `200.00`. 4. Submit. |
| **Expected** | Transaction with type `TRANSFER` and `COMPLETED` status appears. `ACCOUNT_A` balance decreases, `ACCOUNT_B` balance increases. |
| **Result** | Pass / Fail / Blocked |

---

### UI-TXN-005 — Transfer form shows both source and destination fields only for Transfer action

| Field | Value |
|-------|-------|
| **Precondition** | On the Transfer/Deposit/Withdraw form |
| **Steps** | 1. Select **Deposit** — observe visible fields. 2. Switch to **Withdraw** — observe visible fields. 3. Switch to **Transfer** — observe visible fields. |
| **Expected** | Deposit: only destination account. Withdraw: only source account. Transfer: both source and destination accounts are shown. |
| **Result** | Pass / Fail / Blocked |

---

### UI-TXN-006 — Transaction form rejects zero or negative amount

| Field | Value |
|-------|-------|
| **Precondition** | On the transfer form |
| **Steps** | 1. Enter `amount: 0`. 2. Try to submit. |
| **Expected** | Inline validation error "Amount must be greater than 0" shown. Form not submitted. |
| **Result** | Pass / Fail / Blocked |

---

### UI-TXN-007 — Transaction form requires account selection

| Field | Value |
|-------|-------|
| **Precondition** | On the transfer form, action = Deposit |
| **Steps** | 1. Leave the destination account dropdown unselected. 2. Enter a valid amount. 3. Click Submit. |
| **Expected** | Validation error on the account field. Form not submitted. |
| **Result** | Pass / Fail / Blocked |

---

### UI-BEN-001 — Beneficiaries page lists saved beneficiaries

| Field | Value |
|-------|-------|
| **Precondition** | Logged in; at least one beneficiary exists |
| **Steps** | 1. Click **Beneficiaries** in the navbar |
| **Expected** | Beneficiary list is displayed with nickname, account number, account holder name, and bank name. |
| **Result** | Pass / Fail / Blocked |

---

### UI-BEN-002 — Add a new beneficiary

| Field | Value |
|-------|-------|
| **Precondition** | On the `/beneficiaries` page |
| **Steps** | 1. Click **Add Beneficiary**. 2. Fill in: Nickname `"Bob Jones"`, Account Number `"9876543210"`, Account Holder `"Robert Jones"`, Bank Name `"Chase Bank"`, Currency `"USD"`. 3. Submit. |
| **Expected** | New beneficiary appears in the list. Success notification shown. |
| **Result** | Pass / Fail / Blocked |

---

### UI-BEN-003 — Beneficiary form validation

| Field | Value |
|-------|-------|
| **Precondition** | Add beneficiary form is open |
| **Steps** | 1. Submit the form without filling in any fields. |
| **Expected** | All required fields (Nickname, Account Number, Account Holder Name, Bank Name) show "required" error messages. |
| **Result** | Pass / Fail / Blocked |

---

### UI-BEN-004 — Delete a beneficiary with confirmation

| Field | Value |
|-------|-------|
| **Precondition** | At least one beneficiary in the list |
| **Steps** | 1. Click **Delete** on a beneficiary. 2. If a confirmation dialog appears, confirm the deletion. |
| **Expected** | Beneficiary is removed from the list. Success notification shown. |
| **Result** | Pass / Fail / Blocked |

---

### UI-NAV-001 — Navbar links navigate to correct pages

| Field | Value |
|-------|-------|
| **Precondition** | Logged in, on Dashboard |
| **Steps** | 1. Click **Accounts** → verify URL is `/accounts`. 2. Click **Transactions** → verify `/transactions`. 3. Click **Beneficiaries** → verify `/beneficiaries`. 4. Click **Dashboard** → verify `/dashboard`. |
| **Expected** | Each click navigates to the correct route with no full page reload (Angular SPA routing). |
| **Result** | Pass / Fail / Blocked |

---

### UI-NAV-002 — Active nav link is highlighted

| Field | Value |
|-------|-------|
| **Precondition** | Logged in |
| **Steps** | 1. Navigate to each section via the navbar |
| **Expected** | The currently active nav link is visually highlighted (different colour, underline, or bold) compared to inactive links. |
| **Result** | Pass / Fail / Blocked |

---

### UI-NAV-003 — Unknown route shows fallback

| Field | Value |
|-------|-------|
| **Precondition** | Logged in |
| **Steps** | 1. Manually navigate to `http://localhost:4200/completely-unknown-route` |
| **Expected** | Redirected to `/dashboard` (wildcard `**` route) or a 404 page is shown — not a blank white screen. |
| **Result** | Pass / Fail / Blocked |

---

### UI-RESP-001 — Mobile viewport: hamburger menu

| Field | Value |
|-------|-------|
| **Precondition** | Logged in; browser DevTools open |
| **Steps** | 1. In DevTools, set viewport to 375×812 (iPhone 14 size) or use responsive mode. 2. Refresh. |
| **Expected** | Navbar collapses to a hamburger icon. Clicking it opens the navigation menu with all links accessible. |
| **Result** | Pass / Fail / Blocked |

---

### UI-RESP-002 — Tablet viewport layout

| Field | Value |
|-------|-------|
| **Precondition** | Logged in; browser DevTools open |
| **Steps** | 1. Set viewport to 768×1024 (iPad). 2. Navigate through Dashboard, Accounts, Transactions pages. |
| **Expected** | All content is readable and accessible. No horizontal overflow or broken layouts. |
| **Result** | Pass / Fail / Blocked |

---

### UI-SEC-001 — localStorage stores JWT, not session cookies

| Field | Value |
|-------|-------|
| **Precondition** | Just logged in |
| **Steps** | 1. Open DevTools → Application → Local Storage → `http://localhost:4200`. |
| **Expected** | `access_token` and `refresh_token` keys are present. Values are JWTs (three dot-separated base64 segments). No sensitive data (password) stored. |
| **Result** | Pass / Fail / Blocked |

---

### UI-SEC-002 — Token removed from localStorage after logout

| Field | Value |
|-------|-------|
| **Precondition** | Logged in; tokens visible in localStorage (UI-SEC-001 passed) |
| **Steps** | 1. Click **Logout**. 2. Open DevTools → Application → Local Storage. |
| **Expected** | `access_token` and `refresh_token` keys are gone from localStorage. |
| **Result** | Pass / Fail / Blocked |

---

### UI-SEC-003 — API requests include Authorization header

| Field | Value |
|-------|-------|
| **Precondition** | Logged in |
| **Steps** | 1. Open DevTools → Network tab. 2. Navigate to `/accounts`. 3. Inspect the outgoing `GET /api/accounts` request headers. |
| **Expected** | `Authorization: Bearer eyJ...` header is present on the request (injected by the JWT interceptor). |
| **Result** | Pass / Fail / Blocked |

---

### UI-ERR-001 — API error shown as user-friendly message (not raw JSON)

| Field | Value |
|-------|-------|
| **Precondition** | Logged in |
| **Steps** | 1. Stop the `account-service` container: `docker compose stop account-service`. 2. Navigate to `/accounts`. |
| **Expected** | A user-friendly error message like "Unable to load accounts. Please try again." is shown — not a raw `500` or JSON error dump. |
| **Cleanup** | `docker compose start account-service` |
| **Result** | Pass / Fail / Blocked |

---

### UI-ERR-002 — 401 response auto-redirects to login

| Field | Value |
|-------|-------|
| **Precondition** | Logged in |
| **Steps** | 1. Manually delete `access_token` from localStorage via DevTools. 2. Click on **Accounts** or any protected nav item. |
| **Expected** | The app detects the missing/invalid token and redirects to `/login` rather than showing a blank screen or unhandled error. |
| **Result** | Pass / Fail / Blocked |
