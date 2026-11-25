# AgriShield — first run

Open this folder in VS Code. The project root is the folder containing `backend`, `frontend`, and `docker-compose.yml`.

## Build
```powershell
docker compose build
```

Do not run `docker builder prune` unless Docker itself is corrupted. The backend production image intentionally uses `-Dmaven.test.skip=true` so test sources are not compiled into the runtime image. Tests remain available under the Maven `tests` profile.

## Start
```powershell
docker compose up -d
docker compose ps
```

## URLs
- Frontend: http://localhost:8081
- Backend: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## Demo accounts
- farmer@example.com
- bank_officer@example.com
- surveyor@example.com
- insurance_officer@example.com
- state_officer@example.com
- admin@example.com

Password for the seeded demo accounts: `Admin@1234`
