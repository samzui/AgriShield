# API smoke-test sequence

1. Register a farmer with `POST /api/auth/register`.
2. Verify email using the OTP sent by the configured email provider.
3. Login with `POST /api/auth/login` and store the access/refresh tokens.
4. Read `GET /api/users/profile`.
5. Read crop master `GET /api/crops`.
6. Enroll policy `POST /api/policies/enroll`.
7. Submit loss `POST /api/loss-notifications`.
8. Assign survey `POST /api/survey-assignments`.
9. Submit survey `PUT /api/survey-assignments/{id}/submit`.
10. Initiate claim `POST /api/claims/initiate`.
11. Review/approve claim using the officer role.
12. Disburse using the bank-officer role through `PUT /api/claims/{id}/disburse`.
13. Verify analytics using `/api/analytics/dashboard`, `/api/analytics/claim-ratio` and `/api/analytics/loss-patterns`.
14. Verify report export using `/api/reports/pmfby?format=csv`, `xlsx` and `pdf`.
15. Verify admin audit using `/api/audit-logs`.
