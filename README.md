# AgriShield — Agricultural Crop Insurance & Loss Assessment System

Full-stack submission containing the Spring Boot backend and React frontend.

## Structure
- `backend/` — Spring Boot 3.x / Java 17+ REST API, security, JPA, PostgreSQL/MySQL support, operational services.
- `frontend/` — React + Vite operational console preserving the supplied Manus visual language.

## Run
Recommended: run the complete stack with Docker Desktop.

```powershell
docker compose build
docker compose up -d
docker compose ps
```

Frontend: `http://localhost:8081`  
Backend: `http://localhost:8080`  
Swagger: `http://localhost:8080/swagger-ui.html`

For a backend-only local build, Java 17+ and Maven 3.9+ are required. Test-only dependencies are isolated in the Maven `tests` profile; use `mvn -Ptests clean verify` when running the test suite.

See `frontend/FRONTEND_SRS_COVERAGE.md` for the frontend-to-SRS coverage map.
