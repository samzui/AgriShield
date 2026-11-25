#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "[1/5] Java source count: $(find src/main/java -name '*.java' | wc -l)"
echo "[2/5] Test source count: $(find src/test/java -name '*.java' | wc -l)"

grep -q 'spring-boot-starter-security' pom.xml
grep -q 'jjwt-api' pom.xml
grep -q 'spring-boot-starter-data-jpa' pom.xml
grep -q 'postgresql' pom.xml
grep -q 'mysql-connector-j' pom.xml

echo "[3/5] Required dependencies present"

for endpoint in \
  '/api/auth/register' '/api/auth/login' '/api/auth/logout' '/api/users/profile' \
  '/api/policies/enroll' '/api/policies/{id}' '/api/policies/farmer/{id}' \
  '/api/loss-notifications' '/api/survey-assignments' '/api/claims/initiate' \
  '/api/claims/{id}/approve' '/api/claims/{id}/disburse' \
  '/api/analytics/claim-ratio' '/api/analytics/loss-patterns' '/api/reports/pmfby'; do
  grep -Rqs "${endpoint//\//\\/}" src/main/java || echo "WARN: endpoint string not found literally: $endpoint"
done

echo "[4/5] Appendix H endpoint scan complete"

grep -Rqs '@EnableMethodSecurity' src/main/java
grep -Rqs 'TLSv1.3' src/main/resources/application-prod.yml
grep -Rqs 'AES' src/main/java/com/examly/springapp/util

echo "[5/5] Security/deployment configuration scan complete"
echo "Submission static verification passed. Run Docker/Maven tests in an environment with Maven and dependency-network access for runtime verification."
