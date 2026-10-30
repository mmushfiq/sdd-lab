# Research & Design Decisions

## Summary
- **Feature**: `reception`
- **Discovery Scope**: Extension (light discovery). New views and one small table on top of the reservation-core,
  housekeeping and foundation contracts; no new library.
- **Key Findings**:
  - Every call reception needs exists upstream: `ReservationService.checkIn/checkOut/search`,
    `HousekeepingService.isClean/markDirty/findAllCleaningStatuses`, `StaffLayout` + `@Menu` + `@RolesAllowed`, the
    `Clock` bean. Both upstream services use default propagation, so one reception transaction can contain the status
    change, the cleaning check or mark-dirty and reception's own write.
  - `ReservationService` locks the reservation row (`FOR UPDATE`) inside `checkIn`/`checkOut`; calling it first makes
    that lock the first lock of reception's transaction and freezes the reservation's room and dates for the rest of it.
  - Upstream test data contains Checked-in reservations that were never checked in through reception (reservation-core
    `2001` in room 104, February 2027; `2012` in room 103, December 2024). A definition of "guest is staying" based on
    the status alone would make reception's lists and its occupancy check depend on other specs' rows.

## Research Log

### Upstream contracts
- **Context**: reception must not redefine or bypass upstream rules.
- **Sources Consulted**: `.kiro/specs/reservation-core/design.md` (service interface, `ReservationException.Reason`,
  messages, read contract, lock order), `.kiro/specs/housekeeping/design.md` (`HousekeepingService`, H1/H4/H5,
  propagation note, test restore rule), `.kiro/specs/foundation/design.md` (extension contract, `StaffRole.Names`,
  test accounts, fixed clock `2026-06-01T12:00:00Z`), steering files, `roadmap.md` (reserved numbers, shared seams).
- **Findings**:
  - `checkIn`/`checkOut` check only the status (reservation-core D9); date, cleaning and occupancy conditions belong
    to reception.
  - `ReservationService.search(new ReservationSearch(null, from, to))` returns every reservation with
    `arrival < to AND departure > from` in any status, with guest data, ordered by arrival date and room number; `to`
    must be after `from`.
  - There is no lookup by a set of ids and no status filter. Guest data may be read only through `ReservationService`;
    read-only queries on `reservation.status/room_id/arrival_date/departure_date` in an own repository are allowed.
  - `HousekeepingService.isClean` is `@Transactional(readOnly = true)`. Spring applies `readOnly` only when it starts
    a transaction; when it joins reception's read-write transaction the flag has no effect.
  - Housekeeping tests restore all rooms to Clean after each test; reception tests must do the same.
- **Implications**: reception composes upstream calls inside its own `@Transactional` methods and adds only what the
  upstream specs leave to it (date window, occupancy, the stay record, the lists).

### Which reservations are "staying"
- **Context**: departures list, room overview and the occupancy check need the guests who are currently checked in,
  also when their departure date has passed (R5).
- **Findings**: a status-based query (`status = CHECKED_IN`, any date) is unbounded in time and sees the upstream test
  rows `2001`/`2012`; a date-bounded query misses overdue guests.
- **Implications**: reception records every check-in in its own `stay` table; "staying" = a stay row without a
  check-out day. All check-ins go through reception (reservation-core lists reception as the caller of
  `checkIn`/`checkOut`), so in production every Checked-in reservation has such a row; other specs' test rows are
  invisible to reception.

### Loading guest names without a query per row
- **Context**: the open stays (at most ten) need guest names, which only `ReservationService` may return; steering
  forbids one query per grid row.
- **Findings**: any reservation whose arrival date lies in `[a, b]` is returned by `search(null, a, b + 1)`
  (`arrival < b + 1` and `departure > arrival >= a`).
- **Implications**: one `search` call over the arrival-date span of the wanted reservations, filtered by id in Java.
  A future `ReservationService.findByIds` would remove the span step; it is an optional improvement, not a
  prerequisite.

### Consulted skills and guidance
- Steering `vaadin-jooq.md` (transactions, row locks, `ConfirmDialog`, ids, access), `testing.md` (base classes, test
  data ranges, restore rule, no mocks, traceability), `structure.md`, `domain.md`. No AIUP material used.

## Architecture Pattern Evaluation

| Option | Description | Strengths | Risks / Limitations | Notes |
|--------|-------------|-----------|---------------------|-------|
| Reception service composes upstream services in one transaction | `ReceptionService.checkIn/checkOut` call `ReservationService` and `HousekeepingService` with default propagation | One commit, no partial state; no new mechanism | Inner exceptions mark the transaction rollback-only, so reception must never catch them inside the transaction | Selected |
| Domain event after check-out (`@TransactionalEventListener`) | Housekeeping marks dirty after commit | Loose coupling | Check-out could commit while mark-dirty fails; violates 5.2/5.3 | Rejected |
| Lock the room row before the checks | `SELECT room FOR UPDATE` first | Matches the generic steering hint | Opposite lock order to reservation-core (reservation → room) → deadlock with a concurrent `change`; not needed (see decision) | Rejected |

## Design Decisions

### Decision: Status change first, checks after, inside one transaction
- **Context**: requirement 5 (all-or-nothing, races).
- **Alternatives Considered**: 1. read reservation, check, then `checkIn` (room may change in between through a
  concurrent `change`); 2. room lock first (deadlock risk).
- **Selected Approach**: `checkIn`: `ReservationService.checkIn` (locks reservation, Reserved → Checked in) → date
  window → occupancy (open stay in the room) → `isClean` → insert stay row. `checkOut`: `ReservationService.checkOut`
  → close stay → `markDirty`. Any exception rolls back every step.
- **Rationale**: the reservation lock keeps room and dates fixed while they are checked; reception takes no other lock,
  so lock order stays reservation → room as in reservation-core. Per room and day at most one Reserved reservation can
  be checked in (no double booking + check-in window = its own nights), so two check-ins never compete for a room.
  Check-out commits Checked out, closed stay and Dirty together, so a concurrent check-in of the next guest sees
  either the open stay (occupied) or the dirty room — never a free, clean room.
- **Trade-offs**: a refused check-in performs and rolls back one update; negligible.

### Decision: Own `stay` table with check-in and check-out day
- **Context**: requirement 9; reports' brief expects check-in/check-out data from reception; early and late
  check-out (R4, R5) make the reservation's dates differ from the nights stayed.
- **Selected Approach**: `stay(reservation_id PK/FK, checked_in_on DATE, checked_out_on DATE NULL)`, written only by
  reception in the same transaction as the status change.
- **Trade-offs**: reservations set to Checked in/out without reception (test data of other specs) have no stay row;
  reports fall back to arrival/departure dates for them (read contract in design).

### Decision: No-show, early/late check-in and check-out, room move (R1–R6)
- **Context**: open questions of `domain.md` assigned to reception.
- **Selected Approach**: no new status and no job; check-in window = `[arrival, departure)`; check-out any time while
  checked in; no room move inside check-in — room, dates and cancellation stay with the reservation pages.
- **Rationale**: keeps reservation-core's four statuses and transitions untouched; every decision is enforced by the
  date window plus existing upstream operations.

### Decision: Work lists and two views
- **Selected Approach**: one page with arrivals and departures for a chosen day (rows leave once done) and one room
  overview for today; shared confirm/notify helper for check-in and check-out.
- **Rationale**: two clicks per action (7.1, 7.2); early check-out reachable from the room overview (6.5).

### Synthesis outcomes
- **Generalization**: arrivals, departures and room overview are filters over one `search` result plus the open stays;
  check-in and check-out share one confirm-and-notify helper.
- **Build vs adopt**: Vaadin `Grid`, `DatePicker`, `ConfirmDialog`, `Notification`; Spring transaction propagation;
  foundation menu mechanism. Nothing custom.
- **Simplification**: no reception-specific records for list rows (upstream `ReservationDetails` reused), one record
  for the overview row, no room lock, no event, no scheduled job, no undo of check-in, no manual "mark dirty".

## Risks & Mitigations
- Another feature calls `ReservationService.checkIn/checkOut` directly → no stay row, no clean check. Mitigation:
  boundary note in design; reservation-core lists reception as the caller.
- Exceptions caught inside the transaction would produce `UnexpectedRollbackException`. Mitigation: the service never
  catches; only the view catches, outside the transaction.
- Tests change shared rooms' cleaning status and reception rows. Mitigation: a test helper restores reservation
  statuses, stay rows (6000–6999) and all rooms to Clean after every test.

## References
- `.kiro/specs/reservation-core/design.md` — ReservationService contract, messages, lock order, read contract.
- `.kiro/specs/housekeeping/design.md` — HousekeepingService contract, propagation note, test restore rule.
- `.kiro/specs/foundation/design.md` — StaffLayout extension contract, roles, test accounts, fixed clock.
- Spring Framework reference, "Transaction propagation" — participating transactions ignore `readOnly`; inner
  rollback marks the outer transaction rollback-only.
