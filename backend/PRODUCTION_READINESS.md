# Production Readiness Checklist

## Implemented in source

- Java 17 / Spring Boot 3.x layered backend
- PostgreSQL/MySQL JPA configuration
- JWT HS256 access tokens with role-specific lifetimes
- 30-day refresh token rotation and access-token blacklist
- BCrypt passwords and password history
- email OTP with 15-minute expiry and attempt limit
- progressive lockout thresholds
- session inactivity timeout
- database-backed runtime permissions
- RBAC and record ownership checks for policy access
- AES-256-GCM sensitive-data converter
- TLS 1.3/HSTS application configuration
- immutable-style audit append flow with event hash
- claim status history
- document versioning/rollback/retention/signature metadata
- notification templates/preferences/retry/broadcast/WebSocket delivery
- scheduling availability and conflict detection
- external integration logs, HMAC webhook verification and DLQ persistence
- mobile offline sync conflict response
- KPI analytics, claim ratio, loss patterns, anomaly detection and demand forecast
- CSV/XLSX/PDF reporting
- compliance calendar and admin lifecycle APIs
- Docker/Kubernetes/AWS/Azure/GCP deployment guidance

## Provider/infrastructure configuration required before go-live

- production Aadhaar/identity provider
- corporate OIDC/SSO tenant and role mapping
- SMTP and SMS provider credentials
- real bank/DBT gateway contract and credentials
- satellite/NDVI provider or authorized imagery pipeline
- Redis/Kafka/RabbitMQ multi-node topology if distributed messaging is required
- cloud KMS/S3 or equivalent encrypted object storage
- geo-separated encrypted database backups and tested regional restore
- load balancer, two or more application instances, autoscaling and health monitoring
- operational on-call and 99.9% availability evidence
- annual professional penetration test and recurring vulnerability scans
- GDPR-aligned deletion process and regulator-specific retention/legal review
- JMeter performance evidence for the SRS concurrency/latency targets
- 85%+ unit/integration coverage measured by CI after the full test suite is run
