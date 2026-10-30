# Research & Design Decisions

## Summary
- **Feature**: `reservation-core`
- **Discovery Scope**: Extension (light discovery). Builds on the foundation spec (`room`, `room_type`, `Clock` bean,
  `HrsBrowserlessTest` with fixed clock 2026-06-01). No new libraries; no UI.
- **Key Findings**:
  - The codebase is still the skeleton (`HrsApplication`, `HrsApplicationTests`, `TestHrsApplication`,
    `core/ui/HrsBrowserlessTest`, `core/ui/PlaywrightIT`); foundation is specified but not implemented, so this spec
    depends on foundation tasks 1.2 (clock) and 2.1 (room schema) being done first.
  - H2 cannot enforce "no overlapping date ranges" declaratively (no exclusion constraints), so the steering rule
    applies: check-then-write in one transaction with `SELECT ... FOR UPDATE` on the room row.
  - Six downstream specs call this one; a single `ReservationService` with plain records, one status enum and one
    exception type with a reason code is the smallest contract that all of them can use.

## Research Log

### Concurrency control on H2
- **Context**: Requirement 5.2 and 8.5 (simultaneous requests).
- **Sources Consulted**: H2 2.x grammar for `SELECT ... FOR UPDATE` and `LOCK_TIMEOUT`; jOOQ `SelectForUpdateStep.forUpdate()`;
  steering `vaadin-jooq.md` ("row lock where two users can race, `forUpdate()` on the room").
- **Findings**:
  - H2 (MVStore) locks the selected rows until commit; a second `FOR UPDATE` on the same row waits up to the lock
    timeout (H2 default 10 s) and then fails.
  - Default isolation is READ COMMITTED, so the overlap query executed after acquiring the lock sees rows committed by
    the transaction that held the lock before.
  - `FOR UPDATE` is not allowed with DISTINCT, GROUP BY or aggregates; a single-table select by primary key is the
    safe form.
- **Implications**: lock the `room` row by id alone (single-table select), then read room type data and check overlap
  in further statements of the same transaction. Status changes lock the `reservation` row. Lock order is always
  reservation row first, then room row, which rules out lock cycles between change and create.

### Price snapshot
- **Context**: brief and roadmap seam "rates vs. existing reservations"; reports need revenue per period.
- **Findings**: reports must split stays across months, so they need the nightly rate, not only a total. Storing the
  booked `nightly_rate` on the reservation is enough; nights and total are derived (`nights × nightly_rate`).
- **Implications**: no `total_price` column (no redundancy to keep consistent); `ReservationDetails.totalPrice()` is a
  derived accessor.

### Guest data shape and erasure
- **Context**: GDPR (product.md), D3, D4.
- **Findings**: one guest row per reservation (`guest.reservation_id UNIQUE`) makes erasure a plain delete of guest
  rows while the reservation stays; a guest profile shared across reservations would need matching rules and a
  longer retention argument.
- **Implications**: `guest` references `reservation` (not the other way round); "guest data erased" means "no guest
  row". The CHECK `email IS NOT NULL OR phone IS NOT NULL` backs 3.2 in the database.

### Scheduling the retention job
- **Context**: 10.1 "within one day without staff action".
- **Findings**: Spring's `@EnableScheduling` + `@Scheduled(cron = ...)` is built into Spring Framework; the
  `ScheduledAnnotationBeanPostProcessor` implements `ScheduledTaskHolder`, so a test can assert the cron task is
  registered without waiting for it.
- **Implications**: one small `GuestDataRetentionJob` (daily 03:00, server zone); tests call the erase method directly
  with an explicit cutoff in December 2024 so they cannot touch other specs' test rows.

## Architecture Pattern Evaluation

| Option | Description | Strengths | Risks / Limitations | Notes |
|--------|-------------|-----------|---------------------|-------|
| One service + one repository (selected) | `ReservationService` owns all rules, `ReservationRepository` all SQL | Matches `tech.md` layering; one contract for six consumers | Service grows to ~10 public methods | Ten rooms, low volume |
| Separate availability, booking, lifecycle services | Three services sharing a repository | Smaller classes | Three contracts for consumers; shared lock logic duplicated | Rejected (simplification) |
| Database-only protection (unique per room-night table) | One row per room and night with `UNIQUE(room_id, night)` | Database-enforced | Extra table, row explosion on change, harder reports | Rejected; steering prefers service + lock |

## Design Decisions

### Decision: Lock the room row for every write that claims nights
- **Context**: 5.1, 5.2.
- **Alternatives Considered**: 1) optimistic retry on a version column; 2) table lock; 3) room row `FOR UPDATE`.
- **Selected Approach**: 3. Create and change lock the target room, re-check overlap, then write.
- **Rationale**: prescribed by steering, precise (only bookings of the same room serialise), trivial on H2.
- **Trade-offs**: relies on every writer going through `ReservationService` (boundary rule; no other spec writes
  `reservation`).
- **Follow-up**: concurrency tests with two threads released by a latch, repeated over several rounds.

### Decision: One exception type with a reason enum
- **Context**: 11.2 needs four distinguishable reasons and a readable message.
- **Alternatives Considered**: four exception subclasses; result objects.
- **Selected Approach**: `ReservationException extends RuntimeException` with `Reason { NOT_FOUND, INVALID_INPUT,
  ROOM_NOT_AVAILABLE, TRANSITION_NOT_ALLOWED }`.
- **Rationale**: one catch block in every view; unchecked so `@Transactional` rolls back.
- **Trade-offs**: callers switch on `reason()` instead of catch clauses.

### Decision: Reservation code generated in the service
- **Context**: 1.3, D5; guest-booking needs a reference a guest can quote.
- **Selected Approach**: 8 characters from `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (no 0/O/1/I) drawn with `SecureRandom`;
  regenerate while the code exists; `UNIQUE` constraint as backstop.
- **Rationale**: 32^8 ≈ 10^12 codes for a ten-room hotel; not sequential, not guessable.

### Generalization, build vs. adopt, simplification
- **Generalization**: create and change share one "claim nights for a room" step (validate → lock room → overlap check
  excluding own id → rate). The three status transitions share one "transition" step driven by
  `ReservationStatus.canChangeTo`.
- **Build vs. adopt**: no library needed; Spring transactions, jOOQ `forUpdate()`, Spring scheduling and
  `SecureRandom` cover everything.
- **Simplification**: no UI, no total-price column, no audit timestamps, no guest profile, no separate code generator
  or validator classes (private methods in the service), no role checks in the service (D9, callers' views).

## Risks & Mitigations
- Other specs' test data on the same rooms and dates could change availability results — reservation-core test rows
  use February/March 2027 and December 2024 only; other specs should avoid those windows.
- The daily retention job erases guest data of any spec's test rows that departed more than 90 days before the real
  run date if it fires during a test run — it runs at 03:00 only; tests never call the clock-based path.
- A caller bypassing the service (direct insert) defeats the lock — boundary rule plus review; only
  `ReservationRepository` writes `reservation` and `guest`.
- Lock waits beyond H2's lock timeout surface as a data-access error — ten rooms and sub-second transactions make this
  practically impossible; not mapped to a business reason.

## References
- `.kiro/steering/vaadin-jooq.md` — overlap formula, `forUpdate()` rule, money and date types.
- `.kiro/steering/testing.md` — test data ranges, fixed clock, service tests.
- `.kiro/specs/foundation/design.md` — `room`, `room_type`, `Clock`, `HrsBrowserlessTest`.
- H2 Database grammar: SELECT ... FOR UPDATE; SET LOCK_TIMEOUT (h2database.com).
