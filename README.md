# MambaSplit API (Spring Boot Skeleton)

Java 21 + Spring Boot + Postgres + Flyway + JWT (access + refresh)

## Quick Start (Local)
1. Start Postgres:
```bash
docker compose up -d
```

2. Run the API:
```bash
./mvnw spring-boot:run
```

API: `http://localhost:8080`

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
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd verify
```

## Money
All monetary values are stored as integer cents: `amount_cents BIGINT`.
See `domain/money/Money.java`.
