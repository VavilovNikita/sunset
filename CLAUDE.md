# CLAUDE.md — sunset (backend)

Backend for a small resort's PMS + restaurant POS. Java 21, Spring Boot, PostgreSQL, Flyway, contract-first via `openapi.yaml`. Single hotel, single instance, on a server physically in the hotel.

Rules below were learned from real bugs. Where one looks arbitrary, the reason is stated — read it before deciding to do otherwise.

---

## Code generation

`openapi.yaml` is the source of truth and is edited by hand. Regenerate only with `scripts/generate-api.sh` — it carries the exact flags the committed code was produced with and is idempotent (running it twice changes nothing).

**Some generated files are hand-maintained and will be silently clobbered by a blind copy.** `scripts/generate-api.sh` is the definitive list of what it skips or patches — check it before trusting this summary:
- `AuthApi` is never generated at all, so there's nothing to clobber: `login()` needs an `HttpServletRequest` the generated signature can't carry, and `register()` would collide with `registerUser()` as two handlers on one route.
- `AuthResponse`, `LoginRequest` are fully hand-written, not templated output — deliberately no `toString()` override, so a stray log line can never print a raw JWT or password.
- `DeleteRoomImageRequest` is deliberately missing `@NotNull` on `path` so a null `path` produces the documented plain `ErrorMessage` (via `RoomController`'s manual check) instead of the `ValidationError` shape `@Valid` would trigger.
- `BookingCreateInput`, `UserCreateInput` stay in the generated copy, but a `sed` step in the script reapplies `@Email(message = "Invalid email")` after every regeneration — openapi.yaml alone can't express a custom Bean Validation message.

PII redaction in `toString()` is not manual any more: mark a property `x-sensitive: true` in `openapi.yaml` and the template renders `[REDACTED]`. `ToStringRedactsGuestPiiTests` fails if this regresses.

**A `nullable: true` + `required` property in openapi.yaml generates a `@NotNull` that rejects the very null it's supposed to allow.** The codegen's `JsonNullable<T>` wrapper unwraps before Bean Validation sees it, so `@NotNull` validates the unwrapped value, not the wrapper. Fix by omitting the field from `required:` and validating manually in the service — don't add it back to `required:` to "match the schema."

## Migrations

**The schema cannot be built from migrations alone.** `V1` is an intentional no-op: the original tables were created by Prisma before this backend existed, and Flyway adopts that schema via `baseline-on-migrate`. Never drop the database expecting migrations to recreate it. Tests bootstrap from a committed baseline dump instead.

**`ALTER TYPE ... ADD VALUE` and any DML using the new value must be in separate migration files.** PostgreSQL forbids using a freshly added enum value in the same transaction, and Flyway wraps each migration in one. See V2/V3 (new `Role` values) and V6/V7 (new `PrintDocumentType` value, then a backfill — V6's own comment explains the split).

**A set of values on one row is `TEXT[]` plus a `CHECK` constraint, not a native Postgres enum array.** `Role` is a native enum (one value per row); `JobFunction` (`User.jobFunctions`, a set) is not, deliberately — this project has a working Hibernate mapping for a scalar native enum and for a plain array (`RoomEntity.images` is the only array column here, and it's `text[]`), but none for an *array of a custom enum type*, and `V33__user_job_functions.sql` wasn't the place to become the first. The `CHECK` constraint is the DB-side validation a native enum would otherwise provide. Don't "fix" this inconsistency by converting it to a native enum array — read `V33`'s own comment first. It's a deliberate trade-off, not an oversight: it costs `JobFunction` the type safety a native enum gets, but it buys a real simplification — adding a third value later is one migration updating the `CHECK` constraint, not the `ALTER TYPE`-plus-separate-migration dance the rule above describes.

Some migrations are destructive (dropped columns, deleted rows) — V4 and V11 are the two so far. Code rollback alone is not safe once one of these has run; restoring means restoring a dump. Take one before deploying.

## Money

**The server computes every amount. A client-supplied price or total is never trusted, ever.** This holds for room rates, order totals, payments, and folio balances.

**Agreed prices are frozen per night** (`BookingSegmentNightlyRate`). Extending a stay prices only the new nights; already-agreed nights keep their original rate. Repricing an existing night happens only through the explicit reprice action, and only for nights from today forward. This exists because the system used to recompute the whole stay from current rates, so extending a booking by one night silently repriced the entire stay.

Undoing a relocation restores the preserved original rates. It is not a new agreement.

## Concurrency

Availability writes run `SERIALIZABLE` in `BookingWriter`, and PostgreSQL serialization failures (SQLSTATE 40001) are translated to `ConflictException` with a human message. `BookingWriter` is a separate bean specifically so the `@Transactional` proxy applies — self-invocation would bypass it.

Do not copy `SERIALIZABLE` to writes that do not contend for inventory. POS order and payment writes use ordinary transactions; using SERIALIZABLE there would produce spurious conflicts during service hours.

Where a race is better solved by the database, use the database: one payment per order is enforced by a unique constraint, not by a check-then-write.

## Availability model

`Room` is a room *type*. `RoomUnit` is a physical room. A booking has segments (`BookingSegment`), one per room it occupies, covering the stay without gaps or overlaps — a booking with no relocation is one segment, not a special case.

Occupancy (`checkIn <= date < checkOut`) — the departure day is free. Back-to-back bookings on turnover day are valid.

`isActive = false` (permanently out of service) and `RoomUnitBlock` (temporarily out of sale, with dates and a reason) are **independent facts**. A blocked room is still active. Never collapse them into one "unavailable".

Occupancy state (checked in / departed) is a separate axis from booking status and does **not** affect availability.

The availability engine has been rewritten three times. Keep changes out of it unless the task is about it.

## Authorization

Hierarchy: `ADMIN > MANAGER > CASHIER > WAITER`. `/users/**` is `ADMIN` only and deliberately outside the hierarchy.

**Every path in `openapi.yaml` needs an explicit rule in `SecurityConfig`.** `EndpointCoverageTests` reflects into the built filter chain and fails if any path falls through to `anyRequest()`. Order matters: specific matchers before general ones.

**Compare roles with explicit constants, never `ordinal()`.** `Role` is generated from `openapi.yaml`; reordering values there would silently invert an ordinal comparison.

**If a role may perform an action, it must be able to read the data that action requires.** This asymmetry has been introduced and fixed three times — a cashier allowed to assign a room but not to list rooms, and so on.

Tokens carry a version. Changing a role, resetting a password, or disabling an account invalidates existing tokens immediately; `JwtAuthFilter` re-reads the user on every request. This is deliberate: without it a departing employee keeps access for a week.

## Failure handling

**Printing, email and audit writes must never break the operation they accompany.** An unreachable printer does not stop an order from being created or a shift from closing; the job is queued and retried. Audit records are written in their own transaction so a failure there cannot poison the main one.

Front-desk operations warn rather than block. Checking a guest into an uncleaned room, or checking out with an outstanding balance, is allowed with a visible warning — a person stands at the desk and the system is not the one deciding.

## Dates

Dates of stay are date-only strings (`YYYY-MM-DD`) everywhere in the API. Timestamps (`createdAt`, `paidAt`, …) stay date-time. `DateOnlyFieldsContractTests` walks real responses and fails if a stay date acquires a time component — mixing the two formats previously caused the dashboard to report zero revenue for months.

## Tests

Testcontainers, one PostgreSQL container shared across the suite. The schema is loaded from the committed baseline dump, then Flyway applies anything newer.

Write tests for the thing that would silently produce a wrong number, not for framework plumbing. Concurrency claims need a real race (`CyclicBarrier` + `ExecutorService`, no `@Transactional` on the class) — a test that cannot observe the race proves nothing.

**`AuditLogService.record()` runs `REQUIRES_NEW` and swallows its own exceptions** (deliberate — see its class javadoc), reading the acting user from `SecurityContextHolder` rather than a parameter. In a test this means: no stubbed security context silently produces no audit row instead of a failure, and because it's a separate transaction, its writes commit and survive even when the surrounding `@Transactional` test method rolls back — clean up any rows it wrote in `@AfterEach`.

Run the whole suite, not only new classes.

## Working style

Investigate before changing. Several bugs in this project were "fixed" in the wrong place because behaviour was assumed rather than read.

When a decision is a judgment call — a data model, a permission boundary, anything touching money — state the options and the recommendation and wait, rather than choosing silently.

Do not touch the public site (`/public/**`, guest booking flow) unless the task is about it.