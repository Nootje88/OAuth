a#!/bin/bash

 # Database backup script for production environment
 # This script is executed from the backup container

 set -e

 # Configuration from environment variables
 DB_HOST="db"
 DB_NAME="${MYSQL_DATABASE}"
 DB_USER="${MYSQL_USER}"
 BACKUP_DIR="/backups"
 TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
 BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}-${TIMESTAMP}.sql.gz"
 RETENTION_DAYS=7

 echo "Starting backup of database $DB_NAME at $(date)"

 # Create backup
 mysqldump -h $DB_HOST -u $DB_USER $DB_NAME | gzip > $BACKUP_FILE

 # Check if backup was successful
 if [ $? -eq 0 ]; then
   echo "Backup completed successfully: $BACKUP_FILE"
   echo "File size: $(du -h $BACKUP_FILE | cut -f1)"
 else
   echo "Backup failed!"
   exit 1
 fi

 # Clean up old backups
 echo "Cleaning up backups older than $RETENTION_DAYS days..."
 find $BACKUP_DIR -name "${DB_NAME}-*.sql.gz" -mtime +$RETENTION_DAYS -delete

 echo "Backup process completed at $(date)"

 # If running as a one-off script, exit; otherwise sleep until next scheduled run
 if [ -z "$BACKUP_SCHEDULE" ]; then
   exit 0
 else
   echo "Waiting for next scheduled backup..."
   exec crond -f
 fi