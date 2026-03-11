# NexaBank — User Guide

End-to-end walkthrough for using every feature of the NexaBank platform. Examples use both `curl` for API users and browser instructions for UI users.

---

## Table of Contents

1. [Getting Started — Register an Account](#1-getting-started--register-an-account)
2. [Logging In](#2-logging-in)
3. [Creating a Bank Account](#3-creating-a-bank-account)
4. [Depositing Money](#4-depositing-money)
5. [Transferring Money](#5-transferring-money)
6. [Withdrawing Money](#6-withdrawing-money)
7. [Adding a Beneficiary](#7-adding-a-beneficiary)
8. [Viewing Transaction History](#8-viewing-transaction-history)
9. [Updating Your Profile](#9-updating-your-profile)
10. [Session Management — Refresh & Logout](#10-session-management--refresh--logout)

---

## 1. Getting Started — Register an Account

### UI
1. Open http://localhost:4200
2. Click **Create Account** on the login page.
3. Fill in first name, last name, email, phone number, and a password.
4. Password requirements: 8+ characters, at least one uppercase letter, one digit, one special character.
5. Click **Register**. You are redirected to the login page.

### API

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Alice",
    "lastName": "Smith",
    "email": "alice@example.com",
    "password": "SecurePass1!",
    "phoneNumber": "+1-555-0100",
    "dateOfBirth": "1990-06-15",
    "address": "123 Main St, New York, NY"
  }'
```

Response:
```json
{
  "message": "User registered successfully",
  "userId": 42
}
```

---

## 2. Logging In

### UI
1. Go to http://localhost:4200/auth/login
2. Enter your email and password.
3. Click **Sign In**. You are redirected to the Dashboard.

### API

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"SecurePass1!"}' \
  | jq -r '.accessToken')

echo "Access token: $TOKEN"
```

> **Note:** After 5 consecutive failed login attempts the account is locked for **30 minutes**.

---

## 3. Creating a Bank Account

### UI
1. Navigate to **Accounts** in the sidebar.
2. Click **+ New Account**.
3. Choose account type: **Savings**, **Checking**, or **Investment**.
4. Click **Create**. The new account appears in the grid with a `$0.00` balance.

### API

```bash
curl -s -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"accountType": "SAVINGS"}'
```

Response:
```json
{
  "id": 7,
  "accountNumber": "ACC-20250120-007",
  "accountType": "SAVINGS",
  "balance": 0.00,
  "currency": "USD",
  "status": "ACTIVE"
}
```

---

## 4. Depositing Money

### UI
1. Navigate to **Transactions**.
2. Click the **Deposit** tab.
3. Select the destination account from the dropdown.
4. Enter an amount (minimum $0.01).
5. Add an optional note and click **Submit**.

### API

```bash
curl -s -X POST http://localhost:8080/api/transactions/deposit \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 7,
    "amount": 1000.00,
    "currency": "USD",
    "description": "Initial deposit"
  }'
```

Response:
```json
{
  "transactionId": 101,
  "status": "COMPLETED",
  "amount": 1000.00,
  "type": "DEPOSIT",
  "createdAt": "2025-01-20T15:30:00"
}
```

---

## 5. Transferring Money

### UI
1. Navigate to **Transactions**.
2. Click the **Transfer** tab.
3. Select the **source account** (your account).
4. Enter the **destination account number** (could be any account, including your own).
5. Enter the amount and an optional note.
6. Click **Transfer**. A Saga processes the request — status updates within seconds.

### API

```bash
curl -s -X POST http://localhost:8080/api/transactions/transfer \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountId": 7,
    "destinationAccountNumber": "ACC-20250115-002",
    "amount": 250.00,
    "currency": "USD",
    "description": "Rent payment"
  }'
```

Response:
```json
{
  "transactionId": 102,
  "status": "COMPLETED",
  "amount": 250.00,
  "type": "TRANSFER",
  "sourceAccountId": 7,
  "destinationAccountNumber": "ACC-20250115-002",
  "createdAt": "2025-01-20T15:35:00"
}
```

Possible statuses:

| Status | Meaning |
|--------|---------|
| `PENDING` | Saga initiated, awaiting processing |
| `COMPLETED` | Both debit and credit succeeded |
| `FAILED` | Debit failed (e.g., insufficient funds) |
| `ROLLED_BACK` | Debit succeeded but credit failed; debit was reversed |

---

## 6. Withdrawing Money

### UI
1. Navigate to **Transactions**.
2. Click the **Withdraw** tab.
3. Select the account and enter the amount.

### API

```bash
curl -s -X POST http://localhost:8080/api/transactions/withdraw \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 7,
    "amount": 100.00,
    "currency": "USD",
    "description": "ATM withdrawal"
  }'
```

> Withdrawal will fail with `400 Bad Request` if funds are insufficient.

---

## 7. Adding a Beneficiary

Save a payee once so you can transfer to them quickly.

### UI
1. Navigate to **Beneficiaries**.
2. Click **+ Add Beneficiary**.
3. Fill in:
   - **Nickname** — e.g., "John's Rent"
   - **Account Holder Name** — full legal name
   - **Account Number** — their exact account number
   - **Bank Name** / **Bank Code** — optional for external banks
4. Click **Save**.

### API

```bash
curl -s -X POST http://localhost:8080/api/beneficiaries \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "John Rent",
    "accountHolderName": "John Doe",
    "accountNumber": "ACC-20250115-002",
    "bankName": "NexaBank",
    "bankCode": "NXBKUS33"
  }'
```

To **remove** a beneficiary:

```bash
curl -s -X DELETE http://localhost:8080/api/beneficiaries/1 \
  -H "Authorization: Bearer $TOKEN"
```

---

## 8. Viewing Transaction History

### UI
1. Navigate to **Transactions**.
2. All transactions appear in a paginated table.
3. Use the page controls at the bottom to navigate history.

### API

```bash
# Page 0, 10 per page, newest first
curl -s "http://localhost:8080/api/transactions?page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer $TOKEN"
```

Response:
```json
{
  "content": [
    {
      "id": 102,
      "type": "TRANSFER",
      "amount": 250.00,
      "status": "COMPLETED",
      "description": "Rent payment",
      "createdAt": "2025-01-20T15:35:00"
    }
  ],
  "totalElements": 14,
  "totalPages": 2,
  "size": 10,
  "number": 0
}
```

---

## 9. Updating Your Profile

### UI
1. Click your name in the top-right navigation bar.
2. Edit first name, last name, phone number, or address.
3. Click **Save Changes**.

### API

```bash
curl -s -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Alice",
    "lastName": "Johnson",
    "phoneNumber": "+1-555-0199",
    "address": "456 Park Avenue, New York, NY"
  }'
```

---

## 10. Session Management — Refresh & Logout

### Refreshing an Expired Access Token

Access tokens expire after **15 minutes**. The Angular client handles this automatically in the background. For direct API usage:

```bash
NEW_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}" \
  | jq -r '.accessToken')
```

Refresh tokens are valid for **7 days**. After expiry the user must log in again.

### Logout

```bash
curl -s -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}"
```

The refresh token is invalidated server-side. The access token will expire naturally (15-minute TTL).

---

## Troubleshooting

| Problem | Likely cause | Fix |
|---------|-------------|-----|
| `401 Unauthorized` | Token expired or missing | Refresh token or log in again |
| `403 Forbidden` | Insufficient role (e.g., non-admin accessing fraud alerts) | Use an admin account |
| `400 Insufficient funds` | Account balance too low | Deposit more funds first |
| `409 Conflict` on beneficiary | Duplicate account number | The payee is already saved |
| `503 Service Unavailable` | Circuit breaker open | Wait ~10 seconds, then retry |
| Emails not arriving | SMTP credentials wrong | Verify `MAIL_*` env vars and use an app password for Gmail |
