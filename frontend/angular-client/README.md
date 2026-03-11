# Angular Client — NexaBank Frontend

Single-page application built with **Angular 17** and lazy-loaded feature modules. Communicates exclusively with the API Gateway at `http://localhost:8080` via an `/api` proxy.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Node.js | 18+ |
| npm | 9+ |
| Angular CLI | 17.x (`npm i -g @angular/cli@17`) |

---

## Quick Start

```bash
cd frontend/angular-client

# Install dependencies
npm install

# Start dev server (http://localhost:4200)
npm start
```

The dev server proxies all `/api/*` requests to `http://localhost:8080` (API Gateway). Make sure the backend is running before navigating.

---

## Available Scripts

| Command | Purpose |
|---------|---------|
| `npm start` | Dev server on port 4200 with proxy |
| `npm run build` | Production build to `dist/` |
| `npm run build:dev` | Development build to `dist/` |
| `npm test` | Karma unit tests (headless Chrome) |
| `npm run lint` | ESLint checks |

---

## Application Structure

```
src/
├── app/
│   ├── core/
│   │   ├── guards/
│   │   │   ├── auth.guard.ts          # Redirects to /auth/login if not authenticated
│   │   │   └── admin.guard.ts         # Restricts to ROLE_ADMIN users
│   │   ├── interceptors/
│   │   │   └── jwt.interceptor.ts     # Attaches Bearer token; queues requests on 401
│   │   ├── models/                    # TypeScript interfaces (User, Account, Transaction…)
│   │   └── services/
│   │       ├── auth.service.ts        # Login, register, refresh, logout + BehaviorSubject
│   │       ├── account.service.ts
│   │       ├── transaction.service.ts
│   │       └── beneficiary.service.ts
│   ├── features/
│   │   ├── auth/                      # Login + Register (lazy)
│   │   ├── dashboard/                 # Summary cards + quick actions (lazy)
│   │   ├── accounts/                  # Account list + create modal (lazy)
│   │   ├── transactions/              # Paginated history + deposit/withdraw/transfer (lazy)
│   │   └── beneficiaries/             # Payee list + add/delete modal (lazy)
│   ├── app-routing.module.ts
│   ├── app.module.ts
│   ├── app.component.ts/.html/.scss   # Nav shell with router-outlet
│   └── main.ts
├── environments/
│   ├── environment.ts                 # { production: false, apiUrl: '/api' }
│   └── environment.prod.ts            # { production: true, apiUrl: '/api' }
└── styles.scss                        # Global design system (CSS variables, components)
```

---

## Feature Modules

### Auth (`/auth`)
- **`/auth/login`** — Email + password form. JWT tokens are stored in `localStorage` on success.
- **`/auth/register`** — Full registration form with password match validation and complexity requirements.

### Dashboard (`/dashboard`) — requires auth
- Summary cards: Total balance, active accounts, recent transactions
- Account list with quick-action links
- Recent 5 transactions table

### Accounts (`/accounts`) — requires auth
- Grid of account cards (Savings, Checking, Investment) with balance
- Inline modal to create a new account — choose type, provide an initial nickname
- Click any account to open the filtered transaction list

### Transactions (`/transactions`) — requires auth
- Paginated table (10 per page) of all transactions
- Supports deep-link actions via query param: `?action=deposit`, `?action=withdraw`, `?action=transfer`
- Tabbed form (Deposit / Withdraw / Transfer):
  - **Deposit / Withdraw:** Select account, enter amount, optional note
  - **Transfer:** Source account, destination account number, amount, note

### Beneficiaries (`/beneficiaries`) — requires auth
- Table of saved payees with avatar initials
- Add beneficiary modal (nickname, account number, holder name, bank name/code)
- Soft-delete with confirmation dialog (calls `DELETE /api/beneficiaries/{id}`)

---

## Authentication Flow

1. User logs in → backend returns `accessToken` + `refreshToken`
2. Tokens stored in `localStorage` as `access_token` / `refresh_token`
3. `JwtInterceptor` attaches `Authorization: Bearer <accessToken>` to every outgoing request
4. On `401` response, interceptor pauses outgoing queue, calls `/api/auth/refresh`, then retries
5. Logout clears both tokens and navigates to `/auth/login`

---

## Proxy Configuration

`src/proxy.conf.json`:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "info"
  }
}
```

The `angular.json` `serve` target references this file via `"proxyConfig": "src/proxy.conf.json"`.

---

## Environment Files

Modify these to point to a remote API:

```typescript
// environment.ts (development)
export const environment = {
  production: false,
  apiUrl: '/api'   // proxied to localhost:8080
};

// environment.prod.ts (production build)
export const environment = {
  production: true,
  apiUrl: 'https://api.yourbank.com'  // change for deployment
};
```

---

## Production Build

```bash
npm run build
```

Output is placed in `dist/banking-app/`. Serve the `dist/` folder with any static file server (Nginx, AWS S3, etc.). Ensure the server redirects all 404s to `index.html` for Angular routing to work.

Example Nginx snippet:

```nginx
location / {
  root /usr/share/nginx/html;
  try_files $uri $uri/ /index.html;
}
```
