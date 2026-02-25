# Installation / Quick Start

This section explains how to run the project locally using Docker Compose, including HTTPS termination via Nginx (self-signed certificate).

## 0) Prerequisites

#### 1) Docker Desktop (Windows):

Verify Docker is running:

```powershell
docker version
docker info
```

Clone git repository:

```powershell
git clone https://github.com/Nanuccio01/VaultBank.git
cd VaultBank
```

#### 2) Create the local .env file (secrets + ports):

Create .env from template:

```powershell
Copy-Item .env.example .env
```

Generate a JWT secret (base64, single-line) and inject it into .env:

```powershell
$jwt = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
(Get-Content .env) -replace "SBANK_JWT_HS256_SECRET_B64=REPLACE_ME", "SBANK_JWT_HS256_SECRET_B64=$jwt" | Set-Content .env
```

Generate an AES key (base64 of 32 bytes) and append it to .env:

```powershell
$aes = [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
(Get-Content .env) -replace "SBANK_AES_KEY_B64=REPLACE_ME", "SBANK_AES_KEY_B64=$aes" | Set-Content .env
```

#### 3) Generate HTTPS certificates (self-signed)

Create the certificates folder:

```powershell
New-Item -ItemType Directory -Force .\frontend\certs | Out-Null
```
Generate certificate and key using a Docker container (no OpenSSL installation required on Windows):

```powershell
$certPath = (Resolve-Path .\frontend\certs).Path
docker run --rm -v "${certPath}:/certs" alpine:3.20 sh -c "apk add --no-cache openssl >/dev/null && \
openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
-keyout /certs/localhost.key -out /certs/localhost.crt \
-subj '/CN=localhost'"
```
This creates:
- frontend/certs/localhost.crt
- frontend/certs/localhost.key

Note: Browsers will show a warning (“Not secure”) because the certificate is self-signed. 


#### 4) Start the application (Docker Compose)

From the repository root:

```powershell
docker compose up --build
```

Expected services:
- postgres (database)
- backend (Spring Boot REST API)
- frontend (Nginx + React SPA + HTTPS termination + reverse proxy to backend)


#### 5) First run:
- Go to https://localhost:8443/register and create a user.
- Go to https://localhost:8443/login and sign in.

into the /dashboard you can:
- view IBAN + balance
- create a transfer
- verify “Last Trasaction”

To test an internal transfer (credit on the recipient account), register a second user and transfer to the second user’s IBAN.

#### 6) Stop/Reset:
Stop containers (keep databases volumes):

```powershell
docker compose down
```

Full reset (delete database volume and all data):

```powershell
docker compose down -v
```

## 1) Tech Stack Overview

This project is a minimal full-stack “secure banking demo” designed for a live university demo. It is built as a **single backend** + **single frontend** + **PostgreSQL**, fully containerized with Docker Compose.

### Languages:
- **Java 21** (backend)
- **TypeScript** (frontend)
- **SQL** (PostgreSQL)
- **YAML** (configuration / docker-compose)
- **Nginx config** (reverse proxy + HTTPS)

---

### Backend (API):
- **Spring Boot**  
  REST API and application bootstrap.
- **Spring Web**  
  Controllers and REST endpoints (`/api/...`).
- **Spring Security** + **OAuth2 Resource Server**  
  Stateless authentication using **JWT Bearer tokens** (Authorization header).
- **JWT (Nimbus JOSE)**  
  Token generation/validation with **HS256** (HMAC).
- **Spring Data JPA (Hibernate)**  
  ORM mapping for `users`, `transfers` (and related queries).
- **PostgreSQL JDBC Driver**  
  Database connectivity.
- **Jakarta Validation**  
  Input validation with annotations (`@Email`, `@Pattern`, `@Digits`, etc.).
- **Spring Boot Actuator**  
  Health endpoint used for Docker healthchecks (`/actuator/health`).
- **Resilience4j RateLimiter**  
  Rate limiting for sensitive endpoints (login / step-up / transfer).

---

### Data Protection / Security Features (Backend):
- **BCrypt** for password hashing.
- **AES-GCM** for encryption at rest:
    - PII fields (first name, last name, phone)
    - Account balance (encrypted in DB, decrypted when returned by API)
- **IBAN generation (Italian format)**  
  Generates realistic “fake” Italian IBANs on registration.
- **Antifraud module**
    - Detects anomalous transfers (amount/velocity/time/new beneficiary)
    - Step-up authentication (re-enter password) for suspicious actions
    - Temporary lock with auto-unlock timer after repeated step-up failures

---

### Frontend (UI):
- **React + Vite**  
  Single Page Application (SPA).
- **React Router**  
  Routes: `/register`, `/login`, `/dashboard`.
- **Axios**  
  HTTP client with interceptor to attach `Authorization: Bearer <token>`.
- **react-hook-form + Zod**  
  Form validation (login/register/transfer) with custom errors.
- **react-hot-toast**  
  User-friendly toasts for success/error notifications.
- **Tailwind CSS**  
  Styling system for a clean and modern UI.

---

### Reverse Proxy / HTTPS:
- **Nginx (frontend container)**
    - Serves the built SPA
    - Reverse proxies `/api/*` and `/actuator/*` to the backend
    - Terminates **HTTPS** using a local self-signed certificate (demo/local)

---

### Containerization:
- **Docker Compose** with 3 services:
    - `postgres` (with volume persistence)
    - `backend` (multi-stage Dockerfile build with Maven wrapper)
    - `frontend` (multi-stage build + Nginx static hosting + HTTPS)
- **.env / .env.example**
    - Secrets and config kept out of Git
    - JWT secret + AES key injected at runtime

---


## 4) Antifraud Module (Behavioral Detection + Step-Up + Auto-Unlock)

The antifraud module is executed **before every transfer** (`POST /api/banking/transfer`) to detect anomalous behavior and reduce fraud risk.  
It implements a pragmatic flow suitable for a live demo:

- **Normal transfers** → allowed immediately
- **Suspicious transfers** → require **Step-Up Authentication** (re-enter password)
- **Repeated Step-Up failures** → temporary lock (auto-unlock after a timer)

#### 1) What it analyzes (rules)
The antifraud engine evaluates a risk score using simple behavioral signals:

- **High amount**
    - Larger amounts increase risk (e.g., ≥ 1,500 € or ≥ 5,000 €).
- **Unusual time window**
    - Transfers during night hours (00:00–05:59 Europe/Rome) increase risk.
- **Transfer velocity**
    - Too many transfers in a short time window (e.g., several transfers within 60 seconds).
- **New beneficiary**
    - A recipient IBAN never used before by that sender + non-trivial amount increases risk.

The module outputs a decision:
- `ALLOW` → proceed
- `STEP_UP` → ask the user to re-authenticate
  (Optionally a `BLOCK` decision can exist, but in this implementation suspicious actions are handled primarily through Step-Up + lock on failure.)

#### 2) Step-Up Authentication (re-enter password)
If the antifraud decision is `STEP_UP` and the current JWT does **not** contain `stepup=true`:
- the API returns **403** with `code = FRAUD_STEPUP_REQUIRED` and a list of `reasons`
- the client prompts the user for the account password
- the client calls:

`POST /api/auth/stepup` (protected endpoint)

If the password is correct, the backend returns a short-lived JWT that includes:
- `stepup=true` claim (and the usual `uid`, `scope`)

The client retries the original transfer using this new step-up token.

#### 3) Temporary Lock + Auto-Unlock (after Step-Up failures)
To mitigate brute-force / suspicious confirmations:
- if the user enters a wrong password during `POST /api/auth/stepup`, the backend applies a **temporary lock** (default: **3 minutes**)
- during this lock:
    - transfers are rejected with **403** `code = FRAUD_BLOCKED`
    - the response includes:
        - `retryAfterSeconds`
        - `lockedUntil`
        - also a `Retry-After` header

Auto-unlock behavior:
- after the timer expires, the lock is automatically cleared on the next request (no manual action required)

The lock duration is configurable via environment variable:
- `VAULTBANK_FRAUD_LOCK_SECONDS` (e.g., `20` for quick testing, `180` for 3 minutes)

#### 4) Client UX behavior (frontend)
- When `FRAUD_STEPUP_REQUIRED` is returned:
    - the UI asks for password (Step-Up)
    - then retries the transfer with the step-up token
- When `FRAUD_BLOCKED` is returned:
    - the UI shows a countdown (“Retry in mm:ss”)
    - the transfer button is disabled until the timer expires
    - when the countdown reaches 0, the UI automatically re-enables the action


 
