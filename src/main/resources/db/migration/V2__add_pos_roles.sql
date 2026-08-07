-- Adds the two new staff roles the POS module needs. Kept as its own migration, separate
-- from V3 (which adds the POS tables): Postgres allows ALTER TYPE ... ADD VALUE inside a
-- transaction, but a newly added enum value can't be referenced until that transaction has
-- committed. Flyway commits each migration file as its own transaction, so splitting these
-- into V2/V3 means a later migration is always free to use CASHIER/WAITER without hitting
-- "unsafe use of new value" errors.
ALTER TYPE "Role" ADD VALUE 'CASHIER';
ALTER TYPE "Role" ADD VALUE 'WAITER';
