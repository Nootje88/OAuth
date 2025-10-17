#!/bin/bash
set -e

DB_HOST="${DB_HOST:-db}"
DB_NAME="${MYSQL_DATABASE}"
DB_USER="${MYSQL_USER}"
BACKUP_DIR="/backups"
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}-${TIMESTAMP}.sql.gz"
RETENTION_DAYS="${RETENTION_DAYS:-7}"

echo "Starting backup for $DB_NAME at $(date)"

if [ -z "$MYSQL_PWD" ]; then
  echo "ERROR: MYSQL_PWD is not set"; exit 1
fi

mkdir -p "$BACKUP_DIR"
mysqldump -h "$DB_HOST" -u "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_FILE"
echo "Created backup: $BACKUP_FILE"

echo "Cleaning up backups older than $RETENTION_DAYS days..."
find "$BACKUP_DIR" -name "${DB_NAME}-*.sql.gz" -mtime +$RETENTION_DAYS -delete
echo "Backup process completed at $(date)"
