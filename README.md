# MambaSplit API (Spring Boot Skeleton)

Java 21 + Spring Boot + Postgres + Flyway + JWT (access+refresh)

## Quick start (local)
1) Start Postgres
```bash
docker compose up -d
```

2) Run the API
```bash
./mvnw spring-boot:run
```

API: http://localhost:8080

## Postgres basic commands
Start Postgres container:
```bash
docker compose up -d
```

Open `psql` in the running Postgres container:
```bash
docker compose exec db psql -U mambasplit -d mambasplit
```

Inside `psql`:
```sql
\db
\d
\q
```

## Test commands
```bash
./mvnw test
./mvnw verify
```
`./mvnw verify` runs integration tests like `*IT.java` via Maven Failsafe.
After ITs complete, Maven runs targeted DB cleanup for test users (`User A` / `User B`).
To keep IT data for manual DB queries, disable cleanup for that run:
```bash
./mvnw verify -DskipItCleanup=true
```

On Windows (PowerShell), use:
```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd verify -DskipItCleanup=true
```

## Money
All monetary values are stored as integer cents: `amount_cents BIGINT`.
See `domain/money/Money.java`.
