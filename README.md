# Finance Backend — Production-Grade API

A full-featured finance dashboard backend built with **Spring Boot 3**, **MySQL**, **Flyway**, and **JWT authentication with refresh tokens**.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (JJWT 0.11) |
| Database | MySQL 8 |
| Migrations | Flyway |
| ORM | Spring Data JPA / Hibernate |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Observability | Spring Actuator + Logback |
| Containerization | Docker + Docker Compose |
| CI/CD | GitHub Actions |
| Build | Maven |
| Testing | JUnit 5 + Mockito + MockMvc (H2) |

---

## Quick Start

### Option A — Docker 
```bash
cp .env

docker-compose up --build
```

App starts at http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui.html

### Option B — Local 
```bash
# 1. Create DB
mysql -u root -p -e "CREATE DATABASE finance_db;"

# 2. Configure credentials
cp .env


# 3. Export env vars 
export DB_PASSWORD=your_password
export JWT_SECRET=$(openssl rand -hex 32)

# 4. Run
mvn spring-boot:run
```

Flyway will automatically run all migrations (V1, V2, V3) and create all tables + seed admin.

---

## Environment Variables

All secrets are externalized. Never commit `.env` to git.

| Variable | Description | Default |
|---|---|---|
| `DB_URL` | JDBC connection string | localhost:3306/finance_db |
| `DB_USERNAME` | Database username | root |
| `DB_PASSWORD` | Database password | **required** |
| `JWT_SECRET` | 32+ char hex secret for signing JWT | **required in prod** |
| `JWT_ACCESS_EXPIRY_MS` | Access token TTL in ms | 900000 (15 min) |
| `JWT_REFRESH_EXPIRY_MS` | Refresh token TTL in ms | 604800000 (7 days) |
| `SERVER_PORT` | HTTP port | 8080 |

Generate a secure JWT secret:
```bash
openssl rand -hex 32
```

---

## Database Migrations (Flyway)

Migrations live in `src/main/resources/db/migration/`. Flyway runs them automatically on startup in order.

| Version | File | Description |
|---|---|---|
| V1 | `V1__initial_schema.sql` | Creates `users` and `transactions` tables |
| V2 | `V2__refresh_tokens.sql` | Creates `refresh_tokens` table |
| V3 | `V3__seed_admin.sql` | Seeds default admin user |


**Default admin:** `admin@finance.com` / `admin123` 
---

## API Reference

### Base URL
```
http://localhost:8080
```

### Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### Authentication Flow

#### Login
```
POST /api/auth/login
Content-Type: application/json

{ "email": "admin@finance.com", "password": "admin123" }
```
Returns `accessToken` (15 min) + `refreshToken` (7 days).

#### Use Access Token
```
Authorization: Bearer <accessToken>
```

#### Refresh Tokens
```
POST /api/auth/refresh
{ "refreshToken": "<your refresh token>" }
```
Issues a new access + refresh token pair (rotation — old token is revoked).

#### Logout
```
POST /api/auth/logout
{ "refreshToken": "<your refresh token>" }
```
Revokes the refresh token server-side.

---

### Endpoints Summary

| Method | Endpoint | Roles | Description |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Login |
| POST | `/api/auth/refresh` | Public | Refresh tokens |
| POST | `/api/auth/logout` | Public | Logout |
| POST | `/api/users` | ADMIN | Create user |
| GET | `/api/users` | ADMIN | List users |
| GET | `/api/users/{id}` | ADMIN | Get user |
| PUT | `/api/users/{id}` | ADMIN | Update user |
| DELETE | `/api/users/{id}` | ADMIN | Deactivate user |
| POST | `/api/transactions` | ADMIN, ANALYST | Create transaction |
| GET | `/api/transactions` | All | List (with filters) |
| GET | `/api/transactions/{id}` | All | Get by ID |
| PUT | `/api/transactions/{id}` | ADMIN, ANALYST | Update |
| DELETE | `/api/transactions/{id}` | ADMIN, ANALYST | Soft delete |
| GET | `/api/dashboard/summary` | All | Aggregated summary |
| GET | `/actuator/health` | Public | Health check |
| GET | `/actuator/metrics` | ADMIN | Metrics |

---

## Role-Based Access Control

| Action | VIEWER | ANALYST | ADMIN |
|---|---|---|---|
| Login / Refresh / Logout | ✅ | ✅ | ✅ |
| View transactions | ✅ | ✅ | ✅ |
| View dashboard | ✅ | ✅ | ✅ |
| Create / Update / Delete transactions | ❌ | ✅ | ✅ |
| Manage users | ❌ | ❌ | ✅ |
| View actuator metrics | ❌ | ❌ | ✅ |

---

## Observability

### Health Check
```
GET /actuator/health
```

### Metrics
```
GET /actuator/metrics          (ADMIN only)
GET /actuator/metrics/jvm.memory.used
```

### Request ID Tracing
Every response includes `X-Request-ID` header. Every log line includes `[requestId]`:
```
2024-06-01 10:00:00.123 [main] INFO [a1b2c3d4] c.f.l.RequestIdFilter - GET /api/transactions → 200 (45ms)
```
Logs are written to `logs/finance-backend.log` with daily rotation (30-day retention).

---

## Running Tests

```bash
mvn test
```

Tests use H2 in-memory database. Flyway is disabled for tests — Hibernate manages the schema.

Test coverage includes:
- Unit tests: UserService, TransactionService, DashboardService
- Repository tests: RefreshTokenRepository
- Integration tests: Full RBAC enforcement, token refresh flow, response consistency

---

## CI/CD (GitHub Actions)

Pipeline runs on every push to `main` or `develop`, and on all pull requests to `main`.

1. Build + unit tests (with H2)
2. Upload test results as artifact
3. Package JAR
4. Build Docker image (on `main` merges only)


