-- New MenuDepartment value for schedulable treatments - its own migration file, separate from
-- V39 (which adds the durationMinutes column any SPA-department row needs) and from any future
-- DML that uses the value, per the ALTER TYPE ... ADD VALUE split rule (see V2/V3, V6/V7): a
-- freshly added enum value can't be referenced in the same transaction it was added in, and
-- Flyway commits each file as its own transaction.
ALTER TYPE "MenuDepartment" ADD VALUE 'SPA';
