# PostgreSQL backup, PITR, and verification (Ziyara)

Operational checklist aligned with production PostgreSQL (15+). Schema objects such as [sys_backup_verification_log](../migrations/032_database_hardening.sql) support recording outcomes from your backup tool.

## 1. Baseline: full backups

- Schedule **logical** (`pg_dump` / `pg_dumpall` for roles) or **physical** (base backup via `pg_basebackup`, Barman, WAL-G, pgBackRest).
- Store backups **off-server** and **encrypt** at rest (object storage SSE-KMS, disk encryption, or vendor-managed).

## 2. Point-in-time recovery (PITR)

1. Set `wal_level = replica` (or `logical` if logical replication is required).
2. Enable `archive_mode = on` and set `archive_command` to ship WAL segments to durable storage (e.g. `wal-g wal-push %p`, `aws s3 cp`, or pgBackRest).
3. Set `archive_timeout` (e.g. 300s) if low write volume to cap data loss window.
4. For replicas: configure `max_wal_senders`, replication slots as needed.
5. Document **RPO** (acceptable WAL loss) and **RTO** (restore time target).

### Recovery smoke test (quarterly)

1. Restore latest base backup to a **non-production** host.
2. Replay WAL to a known timestamp before a deliberate test write.
3. Verify application connectivity and row counts on critical tables (`sys_users`, `bkg_bookings`, `pay_payments`).

### SQL checks

```sql
SELECT * FROM pg_stat_archiver;
SELECT slot_name, active, restart_lsn FROM pg_replication_slots;
```

## 3. Backup verification log

- After each backup job, insert into `sys_backup_verification_log` (columns: `backup_date`, `backup_type`, `backup_size_bytes`, `backup_location`, `verification_status`, `checksum_valid`).
- Optional: restore to a disposable instance monthly and set `restore_test_performed = true`.

## 4. Connection pool (application)

Hikari defaults live in [application.yml](../../core/src/main/resources/application.yml). In production, size `maximum-pool-size` against Postgres `max_connections` and leave headroom for admin and replication connections.

## 5. Docker / Compose

- Mount Postgres data on a **named volume** or host path covered by backups.
- Do not rely on container ephemeral storage for the only copy of data.
