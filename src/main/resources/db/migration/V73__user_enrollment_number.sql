-- The fingerprint terminal knows an employee by this number, not by our own id - see User's own
-- openapi.yaml description. Nullable: most staff never punch a terminal, and nothing can attribute
-- a device punch to someone without one. Unique so the same number can't identify two different
-- people at once; an ordinary unique index treats every NULL as distinct from every other NULL, so
-- any number of non-punching staff can share NULL with no partial-index trick needed.
ALTER TABLE "User" ADD COLUMN "enrollmentNumber" integer;
CREATE UNIQUE INDEX "User_enrollmentNumber_key" ON "User" ("enrollmentNumber");
