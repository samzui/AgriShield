# SRS Compliance Matrix — Submission Build

This backend is the submission build for the Agricultural Crop Insurance and Loss Assessment System SRS.

## Core requirements

| SRS requirement | Submission implementation |
|---|---|
| Java 17+ / Spring Boot 3.x | `pom.xml` uses Java 17 and Spring Boot 3.5.x |
| REST API on port 8080 | `application.yml` / `server.port` |
| PostgreSQL + MySQL JPA compatibility | PostgreSQL and MySQL drivers; JPA entities/repositories |
| JWT authentication | `AuthController`, `JwtTokenProvider`, `JwtAuthenticationFilter` |
| Role-specific token lifetime | Admin 24h, staff 12h, other authenticated roles 8h |
| 30-day refresh token | Persistent refresh-token entity + rotation |
| OTP verification | Email OTP issue/verify/resend/reset flows |
| Aadhaar/identity validation | Identity verification service + provider abstraction |
| SSO | SSO exchange endpoint + provider abstraction |
| Device recognition / 2FA | Device registration and 2FA challenge/verification endpoints |
| Progressive lockout | Persistent login attempts and progressive lockout policy |
| RBAC | Spring method security + runtime permission matrix/filter |
| Operational-hours control | Configurable operational-hours component |
| Policy lifecycle | Enrollment, retrieval, farmer lookup, status lifecycle |
| Loss notification | Geo-tagged loss notification and evidence references |
| Survey / CCE | Survey assignment, CCE fields, geo-location, evidence, submission |
| Satellite assessment | Satellite/NDVI provider boundary and assessment endpoint |
| Claims | Initiate, review, approve, reject, disburse, history/status tracking |
| DBT | DBT provider abstraction and payment-state recording |
| Scheduling | Availability, conflict detection, reschedule and cancellation |
| Workflow | Workflow/stage persistence and tracking |
| Documents | Upload, metadata, versions, rollback, signature metadata, retention, search, content access |
| Notifications | In-app/email/SMS abstractions, templates, preferences, broadcast, retry, WebSocket path |
| Analytics | Dashboard, claim ratio, loss patterns, trends, benchmarks, anomalies, funnel, weekly insight |
| Predictive analytics | Demand forecast / anomaly baseline endpoints |
| Reports | PMFBY CSV/XLSX/PDF export |
| Scheduled reports | Scheduled report CRUD |
| External integrations | REST/sync boundary, webhooks, HMAC validation, integration logs, DLQ |
| CSV/XML import | Admin multipart import with XML hardening/staging |
| Mobile offline sync | Mobile sync endpoint and conflict response model |
| Audit | Searchable audit logs, append-only/tamper-evident event hashing |
| Security | AES-256-GCM converter, TLS 1.3 configuration, HSTS/CSP, rate limiting, intrusion blocking |
| Admin | User lifecycle, role/status/approval, runtime permissions, compliance, backup status, integration status |
| Health monitoring | Actuator health/metrics endpoints |
| Deployment | Docker, Compose, Kubernetes, HPA, AWS ECS baseline, Azure/GCP guidance |
| API documentation | Springdoc OpenAPI / Swagger UI |
| Exception handling | Validation, not-found, duplicate, authorization and generic API responses |

## Required Appendix H endpoints

- `POST /api/policies/enroll`
- `GET /api/policies/{id}`
- `GET /api/policies/farmer/{id}`
- `POST /api/loss-notifications`
- `GET /api/loss-notifications/{id}`
- `POST /api/survey-assignments`
- `PUT /api/survey-assignments/{id}/submit`
- `POST /api/claims/initiate`
- `PUT /api/claims/{id}/approve`
- `PUT /api/claims/{id}/disburse`
- `GET /api/analytics/claim-ratio`
- `GET /api/analytics/loss-patterns`
- `GET /api/reports/pmfby`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/users/profile`

## Important verification boundary

The source repository can implement configuration, interfaces, persistence, security controls, API contracts and deployment manifests, but it cannot prove external operational outcomes such as 99.9% uptime, 500+ concurrent-user performance, real production provider availability, geo-separated disaster recovery, professional penetration testing or a measured 85% CI coverage result. Those require the target deployment environment and test evidence.

SRS roadmap phases 2–4 are future enhancements and are not represented as mandatory completed baseline functionality.
