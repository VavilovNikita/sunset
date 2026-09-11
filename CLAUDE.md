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

## Spa billing

**What links a POS order to a spa appointment is presence, not a status.** `SpaAppointment.orderId` is set once — explicitly by the spa billing door (`POST /orders` with `spaAppointmentId`) or by `OrderService`'s own auto-link (by table, then by booking, tried at different moments — see `OrderService#autoLinkSpaAppointment`'s own javadoc) — and answers exactly one question: is this the order that bills this appointment's treatment. `Order.bookingId` is not the same fact and never was — see that field's own openapi.yaml description; the record of what a *closed* order was actually charged to is `Payment.bookingId`, set independently at `POST /orders/{id}/close`.

**Multi-treatment: a child row per treatment, no frozen price.** `SpaAppointmentTreatment` (`V50__spa_appointment_treatment.sql`) is one row per treatment on an appointment — the same treatment booked twice is two rows, not a quantity of two. `SpaAppointment.durationMinutes` stays a maintained sum over those rows (recomputed on every add/remove, never derived on the fly), specifically so both `EXCLUDE USING gist` constraints keep reading one plain column and never need to know the treatment table exists. `SpaAppointmentTreatment` freezes `durationMinutes` at add time (same "agreed terms are frozen" precedent as `BookingSegmentNightlyRate`) but deliberately **not price**: a room night bypasses the POS `OrderItem`/menu system entirely (`Payment.method=ROOM_CHARGE` reads `Booking.totalPrice` directly), so freezing there protects the only price authority that charge has. A spa treatment bills through the ordinary `OrderService#addItems`, which has exactly one pricing rule project-wide — read the menu live at the moment a line is added to an order. Freezing a second price on the appointment would manufacture a number that can silently disagree with the one actually charged, not protect a guest-facing quote (no price is shown when an appointment is booked). `SpaAppointmentTreatment.currentPrice` on the DTO is a live, unstored `MenuItem.price` read at response time — same "denormalized, not frozen" convention as `treatmentName` — shown for reception's own reference and never read by any billing path. Adding a treatment is legal only while `status` is `BOOKED` (growing `durationMinutes` can collide, same `translateOverlap` 409 handling as `create`); removing one is legal through `BOOKED` or `COMPLETED` (shrinking can never create a new overlap, so it never risks a 409) but always leaves at least one row — an appointment cannot exist with zero treatments. The old singular `SpaAppointment.treatmentMenuItemId` column stays on the table, unused by any code, until a full reader sweep clears it for `V53` to drop.

The billing *warning* is a separate question from the link, asked by the server, not by `OrderService`: `SpaAppointment.missingTreatmentNames` names which of this appointment's treatments the linked order doesn't (yet) carry, computed only once `status` is `COMPLETED` (empty for every other status, regardless of `orderId`) as a multiset comparison — for each `treatmentMenuItemId`, does the linked order's item quantity for it meet or exceed how many times this appointment booked it. **A `CANCELLED` linked order counts as carrying nothing of the appointment's treatments**, same as no order at all — a cancelled order keeps its items and keeps the link (nothing clears `orderId` on cancel), but it bills nothing, so treating its items as still-billing would let the warning go quiet on an appointment nobody actually charged. Presence still decides the *link*, completeness decides the *warning* — don't fold the two questions back into one gate. `SpaAppointmentService#getSchedule` (the day-grid read) computes this batched, not per-appointment: it only fetches order/order-item rows for appointments that are actually `COMPLETED` with a non-null `orderId`, via `OrderItemRepository#findByOrderIdIn`, so a day of mostly-`BOOKED` appointments costs nothing extra regardless of how many there are. The single-appointment write paths (`create`, `addTreatment`, `removeTreatment`, `updateStatus`, `updateSchedule`) share the same completeness computation (`SpaAppointmentService#computeMissingTreatmentNames`) fed per-appointment inputs instead — one method, two call shapes, so the batched and single-appointment reads can never silently diverge on what "missing" means.

## Concurrency

Availability writes run `SERIALIZABLE` in `BookingWriter`, and PostgreSQL serialization failures (SQLSTATE 40001) are translated to `ConflictException` with a human message. `BookingWriter` is a separate bean specifically so the `@Transactional` proxy applies — self-invocation would bypass it.

Do not copy `SERIALIZABLE` to writes that do not contend for inventory. POS order and payment writes use ordinary transactions; using SERIALIZABLE there would produce spurious conflicts during service hours.

Where a race is better solved by the database, use the database. Three tools exist here for three different shapes of conflict, and picking the wrong one either misses real races or produces spurious ones:
- **SERIALIZABLE** (`BookingWriter`) — when availability itself has to be recomputed from several sources (unit counts, blocks, other segments) and can't be expressed as a single row-level constraint.
- **A unique constraint** — when the rule is "at most one," not "no two may overlap": one payment per order is enforced this way, not by a check-then-write.
- **A Postgres `EXCLUDE USING gist` constraint** — when the rule is "no two rows may overlap on a range," and the database can express that directly. The spa module uses one per axis (`spa_appointment_no_table_overlap`, `spa_appointment_no_therapist_overlap` — see `V41__spa_appointment.sql`/`V43__spa_appointment_completed_still_occupies_slot.sql`) instead of a pre-write query: `SpaAppointmentService` never checks for a conflict before writing, it writes directly and lets the constraint reject, translated to a human message by matching the constraint's own name in the root-cause exception (`translateOverlap`). This is cheaper than SERIALIZABLE and correct here specifically because the check needs no unit-counting or block-checking — just "does this interval overlap that one" - which is exactly what the constraint already computes.

## Availability model

`Room` is a room *type*. `RoomUnit` is a physical room. A booking has segments (`BookingSegment`), one per room it occupies, covering the stay without gaps or overlaps — a booking with no relocation is one segment, not a special case.

Occupancy (`checkIn <= date < checkOut`) — the departure day is free. Back-to-back bookings on turnover day are valid.

`isActive = false` (permanently out of service) and `RoomUnitBlock` (temporarily out of sale, with dates and a reason) are **independent facts**. A blocked room is still active. Never collapse them into one "unavailable".

Occupancy state (checked in / departed) is a separate axis from booking status and does **not** affect availability.

**A relocated booking's room can still be changed without changing dates.** `BookingWriter#assignRoomUnit`/`unassignRoomUnit` only ever operate on a booking's *sole* segment — convenient shorthand for the common never-relocated case — and reject once a booking has more than one segment. For a long time that rejection was the whole story: the only way to touch a relocated booking's room was `relocate`/`undo-relocation`, both of which also move dates. It no longer is. `reassignSegmentRoomUnit`/`unassignSegmentRoomUnit` (`PUT /bookings/{id}/segments/{segmentId}/room-unit`) are the general primitive underneath the whole-booking pair — same three checks (room type, active, free for that segment's own dates), same SERIALIZABLE race-safety, addressed at a named segment instead of implicitly "the sole segment." `swapSegmentRoomUnits` (`POST .../swap-room-unit`) builds on that for the one case a single segment-scoped call can't express: exchanging two bookings' rooms atomically, because two sequential single-segment calls would let the second call see the first guest's segment still occupying the room it's moving into. Same room type only, checked before any availability work (a 400, not a 409) — a cross-type swap would mean either retroactive repricing or a guest paying the other room's frozen rate, neither of which this API does silently. Treat any comment or description that still frames a relocated booking's room as fixed-until-undo as describing the *whole-booking shorthand* only, not a ceiling on what's possible.

The availability engine has been rewritten three times. Keep changes out of it unless the task is about it.

## Authorization

Hierarchy: `ADMIN > MANAGER > CASHIER > WAITER`. `/users/**` is `ADMIN` only and deliberately outside the hierarchy.

**Every path in `openapi.yaml` needs an explicit rule in `SecurityConfig`.** `EndpointCoverageTests` reflects into the built filter chain and fails if any path falls through to `anyRequest()`. Order matters: specific matchers before general ones.

**Compare roles with explicit constants, never `ordinal()`.** `Role` is generated from `openapi.yaml`; reordering values there would silently invert an ordinal comparison.

**If a role may perform an action, it must be able to read the data that action requires.** This asymmetry has been introduced and fixed three times — a cashier allowed to assign a room but not to list rooms, and so on.

**Job functions are a second, independent authorization axis — not a fifth role.** `JobFunction` (`User.jobFunctions`, a set — see the Migrations section for why it's `TEXT[]`, not a native enum array) marks what a person can be assigned as (ENGINEER, HOUSEKEEPER, THERAPIST), not how much they can authorize. A role is one value on a strict ladder where each tier inherits everything below it; a function is a set a user can hold none, one, or several of, unrelated to seniority — a WAITER can also hold ENGINEER without that granting anything role-hierarchy-wide, and conversely a MANAGER can stand in for an engineer-gated action without ever holding the function (`SecurityConfig#engineerOrManagerPlus`). Folding a function into the role ladder would either grant privilege it shouldn't (inheriting up the hierarchy along with it) or force one skill per role tier, neither of which matches how a small hotel actually staffs. `JwtAuthFilter` grants each held function as its own `FUNCTION_<NAME>` authority; gate a path on one with `hasAuthority("FUNCTION_...")`, never `hasRole()`/`hasAnyRole()`, which would route it through the hierarchy and let it inherit along the role ladder. Most functions gate nothing at all today — THERAPIST and HOUSEKEEPER are pure domain-eligibility tags (who can be assigned as a spa therapist; nothing about permission) — ENGINEER is the one exception so far, and even there a MANAGER is an explicit fallback, not an implied one.

Tokens carry a version. Changing a role, resetting a password, or disabling an account invalidates existing tokens immediately; `JwtAuthFilter` re-reads the user on every request. This is deliberate: without it a departing employee keeps access for a week.

## Failure handling

**Printing, email and audit writes must never break the operation they accompany.** An unreachable printer does not stop an order from being created or a shift from closing; the job is queued and retried. Audit records are written in their own transaction so a failure there cannot poison the main one.

Front-desk operations warn rather than block. Checking a guest into an uncleaned room, or checking out with an outstanding balance, is allowed with a visible warning — a person stands at the desk and the system is not the one deciding.

The same rule covers blocking a room out of service (`RoomUnitService#createBlock`): a block is created even when it overlaps an existing booking, with a warning listing the affected booking(s) instead of a rejection. A burst pipe or a broken air conditioner is a fact about the room, not a request the software gets to veto because a reservation disagrees — refusing the block would leave staff unable to record what's actually true about the property, and would not fix the pipe. The warning exists so nobody unknowingly checks a guest into the affected room, or forgets to move one already there; deciding what to do about that guest stays a front-desk judgment call, same as everywhere else this rule applies.

## Dates

Dates of stay are date-only strings (`YYYY-MM-DD`) everywhere in the API. Timestamps (`createdAt`, `paidAt`, …) stay date-time. `DateOnlyFieldsContractTests` walks real responses and fails if a stay date acquires a time component — mixing the two formats previously caused the dashboard to report zero revenue for months.

## Tests

Testcontainers, one PostgreSQL container shared across the suite. The schema is loaded from the committed baseline dump, then Flyway applies anything newer.

Write tests for the thing that would silently produce a wrong number, not for framework plumbing. Concurrency claims need a real race (`CyclicBarrier` + `ExecutorService`, no `@Transactional` on the class) — a test that cannot observe the race proves nothing.

**`AuditLogService.record()` runs `REQUIRES_NEW` and swallows its own exceptions** (deliberate — see its class javadoc), reading the acting user from `SecurityContextHolder` rather than a parameter. In a test this means: no stubbed security context silently produces no audit row instead of a failure, and because it's a separate transaction, its writes commit and survive even when the surrounding `@Transactional` test method rolls back — clean up any rows it wrote in `@AfterEach`.

Run the whole suite, not only new classes.

## Naming

**"Shift" means a cash shift, and only a cash shift — never reuse it for the staff module's work-schedule concept.** `Shift`/`ShiftEntity`/`ShiftService` and everything else under that name is a cashier's session at a till: opened with a cash float, closed by counting the drawer, reconciled against `Payment` rows. The staff module (planned, not yet built) needs two other concepts, and they get their own words, reserved now so nobody reaches for "Shift" once that module is underway and has to invent a name under pressure:
- **"Roster"** for the planned schedule — who is due to work when.
- **"Attendance"** for what actually happened — clock in, clock out, hours worked.

These are deliberately two different words, not "roster" for both a plan and its own record: the whole point of that module is comparing what was planned against what happened, and one word for both would make that comparison unsayable — there'd be no way to ask "does the roster match attendance" without the question answering itself by definition. Keeping all three words - shift, roster, attendance - apart is what keeps that comparison a real question with a real answer, instead of a tautology.

## Working style

Investigate before changing. Several bugs in this project were "fixed" in the wrong place because behaviour was assumed rather than read.

When a decision is a judgment call — a data model, a permission boundary, anything touching money — state the options and the recommendation and wait, rather than choosing silently.

Do not touch the public site (`/public/**`, guest booking flow) unless the task is about it.