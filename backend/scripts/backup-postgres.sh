#!/usr/bin/env bash
set -euo pipefail
: "${PGHOST:?PGHOST is required}"
: "${PGUSER:?PGUSER is required}"
: "${PGDATABASE:?PGDATABASE is required}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
mkdir -p "$BACKUP_DIR"
pg_dump --format=custom --no-owner --no-acl "$PGDATABASE" > "$BACKUP_DIR/crop-insurance-$(date -u +%Y%m%dT%H%M%SZ).dump"
echo "Store the dump in geo-separated encrypted object storage using your KMS/cloud policy."
