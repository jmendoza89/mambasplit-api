# MambaSplit API

Java 21 + Spring Boot + Postgres + Flyway + JWT (access + refresh)

## Quick Start (Local)
1. Start Postgres:
```bash
docker compose up -d
```

2. Run the API with the `local` profile:
```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

API: `http://localhost:8080`
Frontend UI (separate repo): `C:\MambaSplit\mambasplit-web` on `http://localhost:5173`

## Recent API/Security Changes
- Invite acceptance endpoint changed from `POST /api/v1/invites/{token}/accept` to `POST /api/v1/invites/accept` with JSON body:
```json
{ "token": "invite-token-value" }
```
- Invite tokens are now hashed at rest (`invites.token_hash`), with migration `V2__invite_token_hash.sql`.
- Group creators are automatically added to `group_members` as `OWNER` when a group is created.
- Group deletion endpoint added: `DELETE /api/v1/groups/{groupId}` (owner-only).
- Public Swagger/OpenAPI access is only enabled in `local`, `dev`, and `test` profiles.
- Error responses now include a machine-readable `code` field.

## Frontend
The frontend is maintained in a separate repo:
- `C:\MambaSplit\mambasplit-web`

Run it with Vite while this API runs locally:
- Frontend: `http://localhost:5173`
- API: `http://localhost:8080`

## Configuration & Secrets
Default (`application.yml`) does not include sensitive defaults.

Required environment variables for non-local environments:
- `APP_SECURITY_JWT_SECRET`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

If these are missing outside local dev, application startup fails fast.

Local development values are in `src/main/resources/application-local.yml` and only apply when `local` profile is active.

Bash examples:
```bash
# Local dev
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run

# Non-local
APP_SECURITY_JWT_SECRET='replace-with-strong-secret'
SPRING_DATASOURCE_URL='jdbc:postgresql://db-host:5432/mambasplit'
SPRING_DATASOURCE_USERNAME='mambasplit'
SPRING_DATASOURCE_PASSWORD='replace-password'
./mvnw spring-boot:run
```

## Postgres Basic Commands
Start Postgres container:
```bash
docker compose up -d
```

Open `psql` in the running Postgres container:
```bash
docker compose exec db psql -U mambasplit -d mambasplit
```

Open `psql` for the test database:
```bash
docker compose exec db psql -U mambasplit -d mambasplit_test
```

Inside `psql`:
```sql
\db
\d
\q
```

Create the dedicated test database once:
```sql
CREATE DATABASE mambasplit_test;
```

## Test Commands
Run unit and integration tests:
```bash
./mvnw test
./mvnw verify
```

`./mvnw verify` runs integration tests like `*IT.java` via Maven Failsafe.

Integration tests run with Spring's `test` profile (`src/test/resources/application-test.yml`) and connect to `mambasplit_test` by default.

## Windows (PowerShell)
```powershell
$env:SPRING_PROFILES_ACTIVE='local'; .\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd verify
```

Non-local PowerShell example:
```powershell
$env:APP_SECURITY_JWT_SECRET='replace-with-strong-secret'
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://db-host:5432/mambasplit'
$env:SPRING_DATASOURCE_USERNAME='mambasplit'
$env:SPRING_DATASOURCE_PASSWORD='replace-password'
.\mvnw.cmd spring-boot:run
```

## Money
All monetary values are stored as integer cents: `amount_cents BIGINT`.
See `domain/money/Money.java`.
