-- Per-employee, not per-shift-code: the hotel's overtime rule ("everyone is eligible except
-- staff on the OP code, plus one individual exception on an ordinary code") has an exception
-- that doesn't correlate with any code group, so it can't be computed from a RosterEntry's
-- shift code on the fly - see User's own openapi.yaml description. Defaults every existing and
-- new row to eligible; the known exceptions are set by hand via PATCH /users/{id}/overtime-
-- eligibility. Nothing reads this to compute or accrue overtime pay yet - it only records the
-- fact now so it doesn't have to be reconstructed later.
ALTER TABLE "User" ADD COLUMN "overtimeEligible" boolean NOT NULL DEFAULT true;
