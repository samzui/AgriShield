#!/usr/bin/env bash
set -euo pipefail
: "${BACKUP_FILE:?BACKUP_FILE is required}"
: "${PGDATABASE:?PGDATABASE is required}"
RESTORE_DB="${RESTORE_DB:-crop_insurance_restore_test}"
createdb -T template0 "$RESTORE_DB" || true
pg_restore --clean --if-exists --no-owner --no-acl -d "$RESTORE_DB" "$BACKUP_FILE"
echo "Restore verification completed for $RESTORE_DB"
