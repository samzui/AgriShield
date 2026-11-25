# AgriShield Frontend — SRS Coverage

The React frontend preserves the supplied Manus visual language while exposing the operational modules required by the SRS and calling the Spring Boot API rather than hard-coded demo data.

## Navigation / UI modules
- Landing, Login, Registration, protected dashboard
- Role-aware navigation for Farmer, Surveyor, Bank Officer, Insurance Officer, State Officer and Admin
- Overview / KPI dashboard
- Policies and enrollment
- Loss notifications
- Survey / CCE assignments
- Claims, review, approval, rejection and DBT lifecycle
- Farmers / authenticated coverage view
- Documents: metadata upload, search, version history, signing
- Notifications and per-channel preferences
- Analytics: KPIs, trends, loss patterns, funnel, forecast, anomalies, benchmarks, weekly insight
- PMFBY report exports: PDF, Excel and CSV
- Scheduling / resource operations
- Workflow stage tracking
- Administration: user lifecycle, approval, activation/suspension, permissions visibility, CSV/XML import, backup status
- Audit trail
- External integrations monitoring / registration
- Compliance calendar
- System health / backup visibility
- Satellite / remote-sensing assessment context

## Important boundary
The SRS contains infrastructure/security requirements that are implemented by the backend/deployment rather than by React itself (AES-256 at rest, TLS 1.3/HSTS, immutable audit storage, intrusion blocking, encrypted geo-separated backups, HA/autoscaling, etc.). The frontend consumes the resulting secured APIs and exposes the corresponding operational controls where an interface is required.

Phase 2–4 roadmap items in the SRS (native offline mobile apps, blockchain, continuous ML retraining, AI extraction, BI marketplace, etc.) are not represented as baseline web features because the SRS labels them as future enhancements.

## Validation
- All frontend TS/TSX source was syntax-transpiled successfully in the build workspace.
- Full `npm run build` requires dependency installation in an environment with npm registry access; the provided environment timed out during `npm install`, so no claim of a completed production bundle is made here.
