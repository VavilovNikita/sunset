-- Job functions: a second, independent authorization axis alongside Role (ADMIN/MANAGER/
-- CASHIER/WAITER, unchanged). A user has exactly one role and zero or more functions - an
-- engineer is not "a cashier plus", it's a sideways job that shouldn't inherit or grant
-- anything on the role ladder. See JwtAuthFilter (grants FUNCTION_<NAME> authorities from this
-- column, checked with hasAuthority(), never hasRole()) and SecurityConfig's roleHierarchy()
-- bean comment.
--
-- Plain TEXT[] rather than a native Postgres enum array: RoomEntity.images is this codebase's
-- only existing array column and is TEXT[], and Hibernate's array-of-custom-enum mapping has no
-- precedent here to build on. The `JobFunction` enum still exists at the openapi.yaml/generated-
-- Java layer for API type safety; this CHECK constraint is the DB-side backstop, playing the
-- role a native enum type would otherwise play. Trade-off, deliberate: adding a third function
-- later means one migration updating this constraint, not the ALTER TYPE / separate-DML-
-- migration dance a real Postgres enum would require (see V2/V3, V6/V7) - a small win in
-- exchange for the array-of-enum risk avoided today.
ALTER TABLE "User" ADD COLUMN "jobFunctions" TEXT[] NOT NULL DEFAULT '{}';

ALTER TABLE "User" ADD CONSTRAINT user_job_functions_valid
  CHECK ("jobFunctions" <@ ARRAY['ENGINEER', 'HOUSEKEEPER']::text[]);
