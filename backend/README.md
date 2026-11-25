# Agricultural Crop Insurance and Loss Assessment System — Backend

Production-oriented Spring Boot 3 / Java 17 REST backend implementing the core PMFBY crop-insurance lifecycle and the SRS security, audit, document, notification, scheduling, analytics, reporting, integration, mobile-sync and administration requirements.

## Architecture

Controller → DTO → Service → Repository → JPA Entity

Cross-cutting layers:
- Spring Security + JWT HS256
- role-based and database-backed runtime permissions
- validation + centralized exception handling
- immutable/tamper-evident audit events
- OTP, password history, progressive lockout and session inactivity
- AES-256-GCM encryption for sensitive bank-account data
- WebSocket notifications
- document versioning, rollback, retention and signature metadata
- analytics, anomaly detection and demand forecasting
- CSV / XLSX / PDF reporting
- external integration logging, webhook HMAC verification and dead-letter storage
- mobile offline-sync conflict responses
- Docker and Kubernetes deployment assets

## Requirements

- Java 17+
- Maven 3.9+
- PostgreSQL 15/16 recommended; MySQL is also supported through JPA configuration
- Docker / Docker Compose for local infrastructure

## Run locally

1. Copy `.env.example` to your environment and replace secrets.
2. Start PostgreSQL/Redis with `docker compose up -d postgres redis`.
3. Run `mvn clean verify`.
4. Run `mvn spring-boot:run`.
5. API: `http://localhost:8080`
6. Swagger UI: `http://localhost:8080/swagger-ui.html`
7. OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Development seed accounts use password `Admin@1234`. Change these credentials immediately outside a local demo environment.

## Core Appendix-H APIs

- POST `/api/policies/enroll`
- GET `/api/policies/{id}`
- GET `/api/policies/farmer/{id}`
- POST `/api/loss-notifications`
- GET `/api/loss-notifications/{id}`
- POST `/api/survey-assignments`
- PUT `/api/survey-assignments/{id}/submit`
- POST `/api/claims/initiate`
- PUT `/api/claims/{id}/approve`
- PUT `/api/claims/{id}/disburse`
- GET `/api/analytics/claim-ratio`
- GET `/api/analytics/loss-patterns`
- GET `/api/reports/pmfby`
- POST `/api/auth/register`
- POST `/api/auth/login`
- POST `/api/auth/logout`
- GET `/api/users/profile`

Additional APIs are grouped under `/api/admin`, `/api/permissions`, `/api/documents`, `/api/notifications`, `/api/schedules`, `/api/workflows`, `/api/integrations`, `/api/mobile`, `/api/identity`, `/api/satellite`, `/api/compliance`, `/api/crops`, `/api/devices`, `/api/scanners` and `/api/audit-logs`.

## Security

Never use the development secret defaults in production. Supply JWT/AES secrets through a secret manager, terminate TLS 1.3 at the load balancer or application server, keep HSTS enabled, use least-privilege database credentials, enable encrypted backups, and configure production email/SMS/DBT/Aadhaar/SSO providers.

## Deployment

- `Dockerfile` — container image
- `docker-compose.yml` — local PostgreSQL/Redis/backend
- `deploy/kubernetes/deployment.yaml` — two-replica deployment + HPA + health probes
- `deploy/aws/ecs-task-definition.json` — ECS/Fargate baseline
- `deploy/azure/README.md` — AKS deployment guidance
- `deploy/gcp/README.md` — GKE deployment guidance
- `scripts/backup-postgres.sh` and `scripts/restore-test.sh` — backup/restore procedures

## SRS traceability

See `SRS_TRACEABILITY.md` for requirement-to-implementation mapping and the small set of infrastructure/provider responsibilities that cannot be proven by a source archive alone (for example 99.9% uptime, multi-region failover, professional penetration testing and production provider contracts).
