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

## Test commands
```bash
./mvnw test
./mvnw verify
```
`./mvnw verify` runs integration tests like `*IT.java` via Maven Failsafe.

On Windows (PowerShell), use:
```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd verify
```

## Money
All monetary values are stored as integer cents: `amount_cents BIGINT`.
See `domain/money/Money.java`.
