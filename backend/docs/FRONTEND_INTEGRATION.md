# Frontend Integration Contract

## Local development

- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Default React origin allowed by CORS: `http://localhost:8081`

Set `CORS_ORIGINS` to the actual frontend origin(s) before deployment.

## Authentication

Login returns an access token and refresh token. Send the access token as:

`Authorization: Bearer <access-token>`

Use `POST /api/auth/refresh` to rotate the access token when required. Logout calls `POST /api/auth/logout`.

## Roles

`FARMER`, `BANK_OFFICER`, `SURVEYOR`, `INSURANCE_OFFICER`, `STATE_OFFICER`, `ADMIN`, `GUEST`

The frontend must hide role-inappropriate navigation, but backend authorization remains authoritative.

## Main frontend modules

1. Authentication / registration / OTP
2. Role-aware dashboard
3. Crop master and policy enrollment
4. Loss notification with geo/evidence data
5. Survey and CCE assessment
6. Claim review / approval / DBT disbursement
7. Documents and evidence
8. Notifications
9. Scheduling and workflow
10. Analytics and reports
11. Administration, permissions, audit and compliance
12. Integration/health monitoring
13. Mobile/offline synchronization

## Response shape

Most JSON APIs use the common `ApiResponse` envelope. File/report endpoints return binary content with appropriate content-disposition headers.
