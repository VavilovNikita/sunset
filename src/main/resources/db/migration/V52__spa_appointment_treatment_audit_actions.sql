-- Its own migration, separate from V50/V51 and from any DML using these values - see the
-- ALTER TYPE ... ADD VALUE split rule (V2/V3, V6/V7). Two values in one file, same as V42.
ALTER TYPE "AuditAction" ADD VALUE 'SPA_APPOINTMENT_TREATMENT_ADDED';
ALTER TYPE "AuditAction" ADD VALUE 'SPA_APPOINTMENT_TREATMENT_REMOVED';
