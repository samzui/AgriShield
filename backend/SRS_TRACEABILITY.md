# SRS Traceability — Backend

| SRS area | Backend coverage |
|---|---|
| Authentication / JWT / RBAC | AuthController, JwtTokenProvider, JwtAuthenticationFilter, PermissionService, PermissionFilter |
| OTP / password recovery | OtpService, AuthController |
| Progressive lockout | AuthService + LoginAttempt persistence |
| Device recognition / 2FA | DeviceRecognitionService, DeviceController, TwoFactorService, TwoFactorController |
| Aadhaar / identity | AadhaarVerificationProvider, IdentityVerificationService |
| SSO boundary | SsoProvider, SsoService, SsoController |
| Policy lifecycle | PolicyController, PolicyService, Policy entity/repository |
| Loss notification | LossNotificationController, LossNotificationService |
| Survey / CCE / satellite fields | SurveyController, SurveyService, SurveyAssignment CCE fields, SatelliteController |
| Claim lifecycle / status history | ClaimController, ClaimService, StatusHistory |
| DBT | DbtPaymentProvider + sandbox provider |
| Scheduling / conflict detection | SchedulingService, ScheduleController |
| Documents / versioning / rollback / retention / signatures | DocumentService, DocumentController, DocumentVersion, DigitalSignature |
| Notifications / templates / retries / preferences | NotificationService, NotificationController, WebSocket |
| Analytics / forecasting / anomaly detection / weekly insight | AnalyticsService, AnalyticsController, PredictiveController |
| Reports | ReportService + ReportController (CSV/XLSX/PDF) |
| External REST/webhook/CSV/XML integration boundary | IntegrationService, IntegrationController, HMAC validation, DLQ |
| Mobile offline synchronization | MobileSyncService, MobileController |
| Admin / compliance | AdminController, PermissionController, ComplianceController, BackupController |
| Audit / tamper evidence | AuditService, AuditAspect, AuditController, event hashes |
| Security controls | AES-256-GCM, TLS 1.3 configuration, HSTS, rate limiting, intrusion blocking, session inactivity |
| Deployment / HA | Docker, Compose, Kubernetes two replicas + HPA, AWS ECS baseline, Azure/GCP guidance |

## Important interpretation

The SRS also specifies operational outcomes that cannot be established merely by compiling a repository: 99.9% uptime, 500+ concurrent-user performance, regional disaster recovery, external provider availability, professional penetration testing, vulnerability-scan cadence and real cloud KMS/backup configuration. The repository contains configuration and deployment hooks for those requirements, but those outcomes must be verified in the target environment.

The SRS roadmap phases 2–4 are future enhancements rather than baseline requirements; they are intentionally not represented as completed production features.
