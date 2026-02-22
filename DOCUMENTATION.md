# Personal Finance Tracker — Documentation

> **For everyone:** The first two sections (Overview and User Guide) are written in plain language. Everything after that goes progressively deeper into technical detail.

---

## Table of Contents

1. [What is This App?](#1-what-is-this-app)
2. [User Guide](#2-user-guide)
3. [System Architecture](#3-system-architecture)
4. [Authentication & Security](#4-authentication--security)
5. [API Reference](#5-api-reference)
6. [Database Schema](#6-database-schema)
7. [Environment Variables & Configuration](#7-environment-variables--configuration)
8. [Deployment](#8-deployment)
9. [Troubleshooting](#9-troubleshooting)

---

## 1. What is This App?

**Personal Finance Tracker** is a web application that helps you track your money — what comes in, what goes out, and where your budget stands at any point in time.

### What you can do with it

| Feature | What it means |
|---|---|
| Track income | Log every salary, freelance payment, or any money you receive |
| Track expenses | Log every purchase or bill, tagged to a category |
| Set budgets | Assign a monthly or yearly spending limit to any category |
| See alerts | Get notified when you've exceeded a budget |
| View summaries | See totals and charts broken down by month, year, or category |

### Where it lives

| Part | URL |
|---|---|
| Frontend (the website you open) | https://yourfinanceguy.netlify.app |
| Backend (the server handling data) | https://personal-finance-tracker-nyez.onrender.com |

### Tech stack (at a glance)

```
Frontend   →  HTML + CSS + JavaScript (vanilla)  →  hosted on Netlify
Backend    →  Java 17 + Spring Boot 3             →  hosted on Render
Database   →  PostgreSQL                          →  hosted on Render
```

---

## 2. User Guide

### Signing up

1. Open https://yourfinanceguy.netlify.app
2. Click **Register**
3. Enter a username, a valid email address, and a password (minimum 6 characters)
4. Click **Register** — you'll be redirected to the login page

### Logging in

1. Enter your username and password
2. Click **Login**
3. **If OTP is enabled:** You'll see a code input — check your email for a 6-digit code and enter it within 5 minutes
4. **If OTP is disabled:** You're taken straight to the dashboard

### Dashboard

The dashboard is your home screen. It shows:
- **Total Income** this month
- **Total Expense** this month
- **Net Balance** (Income minus Expenses)
- A **doughnut chart** of spending by category
- **Budget alerts** for any category where you've overspent

### Managing categories

Before adding transactions, create categories (e.g. "Food", "Salary", "Rent"):

1. Go to **Categories**
2. Click **Add Category**
3. Give it a name, pick a type (Income or Expense), and optionally set a monthly/yearly budget
4. Click **Save**

### Adding a transaction

1. Go to **Transactions**
2. Click **Add Transaction**
3. Fill in: type (Income/Expense), amount, description, category, date
4. Click **Save**

You can also add expenses and incomes separately from their dedicated pages.

### Filtering transactions

On the Transactions page, use the filters at the top to narrow results by:
- Category
- Date range (from / to)

### Viewing reports

Go to **Summary** to see:
- Monthly view: pick a month and year to see totals, category breakdown, and budget alerts
- Yearly view: pick a year to see the annual picture

### Logging out

Click **Logout** in the navigation bar. Your session ends immediately.

---

## 3. System Architecture

```
                        ┌─────────────────────────────┐
                        │   Browser (Netlify)          │
                        │   HTML / CSS / JavaScript    │
                        │   localStorage: JWT token    │
                        └──────────────┬──────────────┘
                                       │  HTTPS + JSON
                                       │  Authorization: Bearer <token>
                        ┌──────────────▼──────────────┐
                        │   Spring Boot API (Render)   │
                        │                              │
                        │  ┌─────────────────────┐    │
                        │  │  Security Layer       │    │
                        │  │  JwtFilter + CORS     │    │
                        │  └──────────┬────────────┘    │
                        │             │                 │
                        │  ┌──────────▼────────────┐   │
                        │  │  Controllers           │   │
                        │  │  Auth / User /         │   │
                        │  │  Transaction /         │   │
                        │  │  Category / Summary    │   │
                        │  └──────────┬────────────┘   │
                        │             │                 │
                        │  ┌──────────▼────────────┐   │
                        │  │  Services              │   │
                        │  │  Business logic        │   │
                        │  └──────────┬────────────┘   │
                        │             │                 │
                        │  ┌──────────▼────────────┐   │
                        │  │  Repositories (JPA)    │   │
                        │  └──────────┬────────────┘   │
                        └─────────────┼───────────────┘
                                      │  JDBC
                        ┌─────────────▼───────────────┐
                        │   PostgreSQL (Render)        │
                        │   users, transactions,       │
                        │   categories, expenses,      │
                        │   incomes, otps              │
                        └─────────────────────────────┘
                                      │  (when OTP enabled)
                        ┌─────────────▼───────────────┐
                        │   Gmail SMTP                 │
                        │   Sends OTP emails           │
                        └─────────────────────────────┘
```

### Source code layout

```
personal-finance-tracker/
├── frontend/
│   ├── index.html          Dashboard
│   ├── login.html          Login + OTP verification
│   ├── register.html       User registration
│   ├── transactions.html   Transaction list + add
│   ├── expenses.html       Expense list + add
│   ├── incomes.html        Income list + add
│   ├── categories.html     Category management
│   ├── summary.html        Monthly / yearly charts
│   ├── app.js              All frontend logic + API calls
│   └── config.js           Backend URL configuration
│
└── backend/
    └── src/main/java/com/chetan/personalfinancetracker/
        ├── config/         CORS, Security, PasswordEncoder
        ├── controller/     HTTP endpoint handlers
        ├── dto/            Request / response shapes
        ├── exception/      Error handling
        ├── mapper/         Entity ↔ DTO conversion
        ├── model/          Database entity classes
        ├── repository/     Database query interfaces
        ├── security/       JWT filter and utility
        └── service/        Business logic
```

---

## 4. Authentication & Security

### How login works — two paths

```
User submits username + password
            │
            ▼
  Spring AuthenticationManager
  validates credentials against DB
            │
     ┌──────┴──────┐
     │             │
 OTP enabled   OTP disabled
     │             │
     ▼             ▼
Generate 6-digit  Generate JWT
OTP, save to DB,  token directly
send via email    │
     │            └──► Return { token, otpRequired: false }
     ▼
Return { otpRequired: true,
         message: "OTP sent" }
     │
User submits OTP
     │
     ▼
Verify OTP (not expired, not used)
Mark OTP as used
Generate JWT token
Return { token }
```

### JWT token

- **Algorithm:** HMAC-SHA (symmetric, secret key)
- **Expiry:** 24 hours from issue time
- **Issuer:** `personal-finance-app`
- **Payload:** username, issued-at, expiry
- **Usage:** sent in every request header as `Authorization: Bearer <token>`

The frontend stores the token in `localStorage`. On every page load, it checks whether the token exists and hasn't expired. If either check fails, the user is redirected to the login page.

### OTP details

| Property | Value | Configurable via |
|---|---|---|
| Length | 6 digits | `OTP_LENGTH` env var |
| Expiry | 5 minutes | `OTP_EXPIRY_MINUTES` env var |
| Cleanup job | Runs every hour | Automatic (scheduled) |
| Transport | Gmail SMTP (TLS 587) | `MAIL_*` env vars |

OTP rows are marked `used = true` after successful verification, so they cannot be replayed.

### Password storage

Passwords are **never stored in plain text**. They are hashed using **BCrypt** (an adaptive one-way hash function) before being written to the database. Even if the database were compromised, passwords could not be recovered from the hashes.

### CORS policy

The API only accepts requests from these origins:

```
https://yourfinanceguy.netlify.app   (production frontend)
http://localhost:3000                 (local dev)
http://localhost:5500                 (VS Code Live Server)
http://127.0.0.1:5500
http://localhost:8080
http://127.0.0.1:3000
http://127.0.0.1:8080
```

All other origins are rejected before they reach the application code.

Allowed HTTP methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`
Allowed headers: `Authorization`, `Content-Type`

---

## 5. API Reference

**Base URL:** `https://personal-finance-tracker-nyez.onrender.com/api`

Endpoints marked with **[Auth]** require the header:
```
Authorization: Bearer <your-jwt-token>
```

---

### Auth endpoints

#### POST `/auth/register`

Create a new user account.

**Request body:**
```json
{
  "username": "john_doe",
  "password": "secret123",
  "email": "john@example.com"
}
```

**Success response `200`:**
```
"User registered successfully"
```

**Error responses:**
- `400` — username already exists, or validation failed
- `400` — invalid email format

---

#### POST `/auth/login`

Log in with credentials.

**Request body:**
```json
{
  "username": "john_doe",
  "password": "secret123"
}
```

**Success response `200` — OTP disabled:**
```json
{
  "token": "eyJhbGciOiJIUzI1...",
  "otpRequired": false
}
```

**Success response `200` — OTP enabled:**
```json
{
  "otpRequired": true,
  "message": "OTP has been sent to your email"
}
```

**Error responses:**
- `401` — wrong username or password
- `500` — OTP email could not be sent (SMTP misconfigured)

---

#### POST `/auth/verify-otp`

Verify the OTP received by email and receive a JWT token.

**Request body:**
```json
{
  "username": "john_doe",
  "otp": "482910"
}
```

**Success response `200`:**
```json
{
  "token": "eyJhbGciOiJIUzI1..."
}
```

**Error responses:**
- `400` — OTP is invalid or expired

---

### User endpoints

#### GET `/users/me` **[Auth]**

Get the currently logged-in user's details.

**Response `200`:**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com"
}
```

---

### Category endpoints

#### GET `/categories` **[Auth]**

Get all categories belonging to the current user.

**Response `200`:**
```json
[
  {
    "id": 1,
    "name": "Food",
    "type": "EXPENSE",
    "monthlyBudget": 5000.0,
    "yearlyBudget": 60000.0
  }
]
```

---

#### POST `/categories` **[Auth]**

Create a new category.

**Request body:**
```json
{
  "name": "Food",
  "type": "EXPENSE",
  "monthlyBudget": 5000.0,
  "yearlyBudget": 60000.0
}
```

**Response `200`:** Created category object (same shape as above, with `id`)

---

### Transaction endpoints

#### POST `/transactions` **[Auth]**

Add a new transaction.

**Request body:**
```json
{
  "amount": 1200.00,
  "type": "EXPENSE",
  "date": "2026-02-15",
  "description": "Grocery run",
  "categoryId": 1
}
```

**Response `200`:** Created transaction object

---

#### GET `/transactions` **[Auth]**

Get all transactions for the current user.

**Response `200`:** Array of transaction objects

---

#### GET `/transactions/filter` **[Auth]**

Filter transactions by category and/or date range.

**Query parameters (all optional):**

| Parameter | Format | Example |
|---|---|---|
| `category` | string | `Food` |
| `from` | `YYYY-MM-DD` | `2026-01-01` |
| `to` | `YYYY-MM-DD` | `2026-01-31` |

**Example:**
```
GET /api/transactions/filter?category=Food&from=2026-01-01&to=2026-01-31
```

---

#### GET `/transactions/summary` **[Auth]**

Get summary for a specific month.

**Query parameters:**

| Parameter | Type | Example |
|---|---|---|
| `month` | integer (1–12) | `2` |
| `year` | integer | `2026` |

**Response `200`:**
```json
{
  "totalIncome": 50000.0,
  "totalExpense": 18500.0,
  "netBalance": 31500.0,
  "categorySummary": {
    "Food": 5200.0,
    "Transport": 1800.0
  },
  "budgetAlerts": [
    {
      "category": "Food",
      "spent": 5200.0,
      "budget": 5000.0,
      "exceeded": true,
      "period": "MONTHLY"
    }
  ]
}
```

---

### Expense endpoints

#### GET `/expenses/my` **[Auth]**

Get all expenses for the current user.

#### POST `/expenses` **[Auth]**

Add a new expense.

**Request body:**
```json
{
  "description": "Electricity bill",
  "amount": 2500.00,
  "date": "2026-02-10",
  "categoryId": 3
}
```

---

### Income endpoints

#### GET `/incomes/user` **[Auth]**

Get all incomes for the current user.

#### POST `/incomes` **[Auth]**

Add a new income record.

**Request body:**
```json
{
  "source": "Salary",
  "amount": 50000.00,
  "date": "2026-02-01",
  "categoryId": 2
}
```

---

### Summary endpoints

#### GET `/summary/monthly` **[Auth]**

```
GET /api/summary/monthly?month=2&year=2026
```

Returns `MonthlySummaryDTO` (same shape as `/transactions/summary`).

#### GET `/summary/yearly` **[Auth]**

```
GET /api/summary/yearly?year=2026
```

Returns yearly totals and category breakdown.

#### GET `/summary/budget-alerts` **[Auth]**

```
GET /api/summary/budget-alerts?month=2&year=2026
```

Returns only the budget alerts array for the given period.

---

### Error response format

All errors follow a consistent shape:

```json
{
  "status": 401,
  "message": "Invalid username or password",
  "path": "/api/auth/login"
}
```

| HTTP status | Meaning |
|---|---|
| `200` | Success |
| `400` | Bad request (validation error, duplicate username) |
| `401` | Unauthenticated (wrong credentials or missing/expired token) |
| `404` | Resource not found |
| `500` | Server error (unexpected failure) |

---

## 6. Database Schema

The application uses **PostgreSQL**. Hibernate manages the schema (`ddl-auto=validate`), meaning the schema must already exist — Hibernate only checks that the tables match the models.

```
┌──────────────────────────────────────────────────────────────────┐
│  users                                                           │
│  id (PK) | username (unique) | email (unique) | password (hash) │
└────────────────────────┬─────────────────────────────────────────┘
                         │ 1
                         │
         ┌───────────────┼──────────────────────┐
         │               │                      │
         │ n             │ n                    │ n
┌────────▼────────┐  ┌───▼──────────────┐  ┌───▼──────────────┐
│  categories     │  │  transactions    │  │  expenses        │
│  id (PK)        │  │  id (PK)         │  │  id (PK)         │
│  name           │  │  amount          │  │  amount          │
│  type           │  │  type            │  │  description     │
│  budget_limit   │  │  description     │  │  date            │
│  monthly_budget │  │  date            │  │  category_id(FK) │
│  yearly_budget  │  │  category_id(FK) │  │  user_id (FK)    │
│  user_id (FK)   │  │  user_id (FK)    │  └──────────────────┘
└─────────────────┘  └──────────────────┘
                                               ┌──────────────────┐
                                               │  incomes         │
                                               │  id (PK)         │
                                               │  source          │
                                               │  amount          │
                                               │  description     │
                                               │  date            │
                                               │  category (text) │
                                               │  user_id (FK)    │
                                               └──────────────────┘

┌─────────────────────────────────────────────┐
│  otps                                       │
│  id (PK) | username | otp | expiry_time     │
│  used (boolean, default false)              │
└─────────────────────────────────────────────┘
```

**Cascade rules:**
- Deleting a user cascades to their categories, transactions, expenses, and incomes
- Deleting a category cascades to its transactions (orphan removal enabled)

---

## 7. Environment Variables & Configuration

All secrets and deployment-specific values are passed via environment variables — nothing sensitive is hardcoded.

### Required variables

| Variable | Description | Example |
|---|---|---|
| `DB_URL` | PostgreSQL JDBC connection string | `jdbc:postgresql://host:5432/finance_tracker` |
| `DB_USERNAME` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `strongpassword` |
| `JWT_SECRET` | Secret key for signing JWT tokens (min 32 chars) | `a-very-long-random-secret-string` |

### OTP / Email variables

Only required when `OTP_ENABLED=true`.

| Variable | Description | Default |
|---|---|---|
| `OTP_ENABLED` | Enable or disable OTP login step | `false` |
| `MAIL_HOST` | SMTP server hostname | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_USERNAME` | Gmail address used to send OTPs | _(none)_ |
| `MAIL_PASSWORD` | Gmail **App Password** (not login password) | _(none)_ |
| `MAIL_FROM` | From address in OTP emails | _(none)_ |

> **Gmail App Password:** Go to Google Account → Security → 2-Step Verification → App passwords. Generate one for "Mail". Use that 16-character code as `MAIL_PASSWORD`.

### Optional variables

| Variable | Description | Default |
|---|---|---|
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed frontend origins | `https://yourfinanceguy.netlify.app,...` |
| `JPA_DDL_AUTO` | Hibernate schema mode | `validate` |

### SMTP timeouts

These are hardcoded in `application.properties` to prevent SMTP hangs:

```
connectiontimeout = 5000 ms
timeout           = 5000 ms
writetimeout      = 5000 ms
```

---

## 8. Deployment

### Backend — Render

The backend runs as a **Java web service** on Render.

**Build command:**
```bash
cd backend && ./mvnw clean package -DskipTests
```

**Start command:**
```bash
java -jar backend/target/*.jar
```

The service uses a **Render PostgreSQL** instance. The `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` environment variables are set in Render's dashboard under the service's **Environment** tab.

> **Free tier note:** Render's free tier spins down services after 15 minutes of inactivity. The first request after a period of no traffic can take 30–90 seconds while the service wakes up. This is normal behaviour on the free plan.

### Frontend — Netlify

The frontend is static HTML/CSS/JS deployed directly to Netlify. No build step is needed — Netlify serves the files as-is from the `frontend/` directory.

The backend URL is configured in `frontend/config.js`:
```javascript
window.API_BASE_URL = "https://personal-finance-tracker-nyez.onrender.com/api";
```

### Docker (optional)

A `Dockerfile` is included in the `backend/` directory for containerised deployments:

```bash
cd backend
docker build -t finance-tracker-backend .
docker run -p 8080:8080 \
  -e DB_URL=... \
  -e DB_USERNAME=... \
  -e DB_PASSWORD=... \
  -e JWT_SECRET=... \
  finance-tracker-backend
```

---

## 9. Troubleshooting

### Login hangs forever / no response

**Cause:** OTP is enabled but SMTP is not configured. `JavaMailSender` attempts to connect to the mail server and hangs without a timeout.

**Fix:** Set `OTP_ENABLED=false` in Render environment variables, or configure all `MAIL_*` variables correctly.

---

### Login returns 500

**Cause:** OTP is enabled, SMTP timeouts fire after 5 seconds, and the email failure propagates as an unhandled server error.

**Fix:** Same as above — set `OTP_ENABLED=false` or configure SMTP properly.

---

### Login returns 401

**Cause:** Wrong username or password, or the user account does not exist in the database.

**Fix:** Register first, then log in. Passwords are case-sensitive.

---

### API calls return 401 after login

**Cause:** JWT token is missing, expired (tokens last 24 hours), or malformed.

**Fix:** Log out and log back in to get a fresh token.

---

### API calls blocked by CORS

**Cause:** The frontend is being served from an origin not in the allowed list.

**Fix:** Add the origin to the `CORS_ALLOWED_ORIGINS` environment variable on Render (comma-separated, no spaces).

---

### Database connection errors on startup

**Cause:** `DB_URL`, `DB_USERNAME`, or `DB_PASSWORD` are wrong, or the database is not reachable.

**Fix:** Verify all three environment variables on Render. Use the internal connection string provided by Render's PostgreSQL dashboard, not the external one (internal is faster and does not count against egress).

---

### OTP email never arrives

Possible causes, in order of likelihood:

1. `MAIL_PASSWORD` is the Gmail login password — it must be an **App Password**
2. 2-Step Verification is not enabled on the Gmail account (required for App Passwords)
3. The OTP expired (5-minute window) — request a new login
4. Email landed in spam

---

*Last updated: February 2026*