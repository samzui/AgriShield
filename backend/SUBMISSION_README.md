# Final Submission Build

## Project
Agricultural Crop Insurance and Loss Assessment System

## What is included

This package contains the Spring Boot 3.x / Java 17 backend, PostgreSQL/MySQL JPA support, JWT authentication, role-based authorization, policy/loss/survey/claim/DBT workflows, documents, notifications, scheduling, analytics, reporting, integrations, mobile synchronization, administration, compliance, audit, security controls, OpenAPI/Swagger and deployment assets.

## Roles

- FARMER
- BANK_OFFICER
- SURVEYOR
- INSURANCE_OFFICER
- STATE_OFFICER
- ADMIN
- GUEST

## Demo credentials

Development seed users are created only when the database is empty and the active profile permits the development initializer.

Password for seeded demo accounts: `Admin@1234`

Examples:
- `farmer@example.com`
- `bank_officer@example.com`
- `surveyor@example.com`
- `insurance_officer@example.com`
- `state_officer@example.com`
- `admin@example.com`

Change/remove these accounts before any real deployment.

## Run with Docker

```bash
docker compose up --build
```

Backend: `http://localhost:8080`
Swagger: `http://localhost:8080/swagger-ui.html`

## Run locally

Set PostgreSQL connection variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) and start the Spring Boot application with Java 17+.

## Frontend

The backend is configured for a React frontend on port 8081 by default. Change `CORS_ORIGINS` when the generated Manus frontend uses a different origin.

See `docs/FRONTEND_INTEGRATION.md`.

## Verification

Linux/macOS/WSL:

```bash
./scripts/verify-submission.sh
```

Windows PowerShell:

```powershell
./scripts/verify-submission.ps1
```

## SRS evidence

See:
- `SRS_TRACEABILITY.md`
- `docs/SRS_COMPLIANCE_MATRIX.md`
- `docs/API_SMOKE_TESTS.md`
- `PRODUCTION_READINESS.md`

The source implements the SRS application capabilities and deployment hooks. External operational acceptance criteria (real cloud HA, measured concurrency/latency, provider credentials, geo-DR, professional security testing and measured CI coverage) must be verified in the target environment rather than claimed from source code alone.
