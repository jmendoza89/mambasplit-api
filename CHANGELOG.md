# Changelog

## 2026-02-22

### Feature: Invite and API hardening

#### Implemented
- Secured invite acceptance flow to avoid token leakage in URLs.
  - Old: `POST /api/v1/invites/{token}/accept`
  - New: `POST /api/v1/invites/accept` with body `{ "token": "<value>" }`
- Moved invite token persistence to hash-only storage.
  - `invites.token` -> `invites.token_hash`
  - Added Flyway migration: `src/main/resources/db/migration/V2__invite_token_hash.sql`
- Added shared token utility for secure random generation and SHA-256 URL-safe hashing:
  - `src/main/java/io/mambatech/mambasplit/security/TokenCodec.java`
- Reworked group membership validation to use a batched DB check (removed N+1 lookup pattern).
- Added invite email validation and normalization:
  - Validation at API boundary (`@Email`, max length)
  - Service normalization (trim + lowercase)
- Improved DB error mapping:
  - `DataIntegrityViolationException` now maps to HTTP `409 Conflict`
  - Error payload includes a machine-readable `code`
- Restricted anonymous Swagger/OpenAPI access to `local`, `dev`, and `test` profiles only.

#### Compatibility Notes
- Client integration must update invite acceptance calls to the new endpoint/body contract.
- If any external tooling parsed error payloads, update for new `code` field.

#### Verification
- `.\mvnw.cmd verify` completed successfully after changes.
