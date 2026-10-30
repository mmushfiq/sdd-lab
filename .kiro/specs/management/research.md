# Research & Design Decisions

## Summary
- **Feature**: `management`
- **Discovery Scope**: Extension (light discovery). Builds on foundation (`room`, `room_type`, `StaffLayout`, roles,
  test accounts, fixed clock), reservation-core (`reservation` read contract, pricing rule D2, room-row lock) and
  housekeeping (`room_cleaning_status` with `ON DELETE CASCADE`, missing status = Dirty). None of them is implemented
  yet; the codebase is the empty skeleton. No new library.
- **Key Findings**:
  - The foundation schema already enforces most value rules (`UNIQUE(number)`, `UNIQUE(name)`, `capacity > 0`,
    `nightly_rate > 0`, FK room → room type, FK reservation → room without cascade). No migration is needed; the
    service adds the rules the database cannot express (case-insensitive uniqueness, limits, reservations in use).
  - Reservation-core's `create`/`change` lock the room row (`SELECT … FOR UPDATE`) and then read the room type's
    capacity and rate in a second statement. Management locking the same room rows before a capacity change, a room
    type change or a removal serialises both sides (8.1) without any upstream change.
  - Reservation-core's read contract (`reservation.status`, `room_id`, `number_of_guests`) is enough for "in use"
    checks with read-only queries in management's own repository (`structure.md`); no upstream query is required.

## Research Log

### Out of service vs. delete (domain.md open question)
- **Context**: Brief asks to decide; reservation-core lists "adding an out-of-service flag to rooms (management)" as a
  revalidation trigger for availability.
- **Findings**: An out-of-service flag would need a new column on `room` (or a new table), a change to reservation-core
  availability and create (exclude such rooms), and a rule for existing reservations of that room. Reservations are
  never deleted (reservation-core 1.4) and reference rooms by a non-cascading FK, so a room that was ever reserved can
  never be deleted anyway.
- **Implications**: Decision M1: no out-of-service state; removal only of rooms that never had a reservation and of
  room types without rooms. No upstream change; the FK is the database backstop.

### Rate changes vs. existing reservations
- **Context**: Brief: rate changes follow reservation-core's pricing rule.
- **Findings**: `reservation.nightly_rate` is the booked snapshot (reservation-core D2); `room_type.nightly_rate` is
  read only at create and at a change to another room type.
- **Implications**: Management simply updates `room_type.nightly_rate`; nothing else is touched. Tests prove the
  snapshot stays (3.2, 6.7) and new reservations take the new rate (3.1).

### Capacity vs. existing reservations
- **Context**: "Changes never break existing reservations" — a smaller capacity could leave a reserved party in a too
  small room.
- **Findings**: Only `RESERVED` and `CHECKED_IN` occupy nights (reservation-core `occupiesNights`). A single query
  `max(number_of_guests)` over occupying reservations of the affected rooms answers it.
- **Implications**: Decision M3; refusal reason `IN_USE`.

### New rooms and the cleaning status
- **Context**: Housekeeping H5: room without recorded status = Dirty; its `room_cleaning_status` is housekeeping-owned.
- **Implications**: Management writes no cleaning status (M7); the confirmation tells the manager the room counts as
  dirty. Removing a room cascades its status row (housekeeping's FK), so management needs no housekeeping call.

### Test data without a test migration
- **Context**: Reserved test migrations V160–V169, ids 7000–7999, dates 2027-06.
- **Findings**: Permanent extra rooms or room types would change other specs' fixed expectations: reservation-core
  `AvailabilityTest` (exact room lists), housekeeping (`101`…`205`, an extra room without status is Dirty and on the
  cleaning list), foundation (exactly ten rooms with id < 1000).
- **Implications**: No test migration. Tests create their own room types/rooms (ids 7000–7999 or names `Test 7…`,
  room numbers `T7…`) and reservations (June 2027) and remove them in `@AfterEach`. V60–V69 and V160–V169 stay unused.

### Consulted skills and guidance
- kiro-spec-init, kiro-spec-requirements (EARS, review gate), kiro-spec-design (light discovery, synthesis, review
  gate), kiro-spec-tasks. Steering `product.md`, `tech.md`, `structure.md`, `vaadin-jooq.md`, `testing.md`,
  `domain.md`, `roadmap.md`; upstream foundation, reservation-core, housekeeping designs. No AIUP material used.

## Architecture Pattern Evaluation

| Option | Description | Strengths | Risks / Limitations | Notes |
|--------|-------------|-----------|---------------------|-------|
| One service + one repository for rooms and room types | `RoomManagementService`, `RoomManagementRepository` | Small, one lock order in one place | Two views share one service | Selected |
| Separate services per entity | `RoomTypeService`, `RoomService` | Mirrors the two pages | Lock order split across classes, duplicate "in use" queries | Rejected |
| Out-of-service flag | Column on `room`, filter in availability | Keeps history of retired rooms visible | Upstream change in reservation-core; more rules | Rejected (M1) |

## Design Decisions

### Decision: Lock order room type → room
- **Context**: 8.1; reservation-core locks reservation → room and never locks `room_type`.
- **Selected Approach**: Every management write locks the target `room_type` row first (when one is involved), then
  the affected `room` rows, then checks reservations, then writes.
- **Rationale**: One global order avoids deadlocks between management operations; reservation-core only ever waits on
  the room row, after which it reads committed room type values.
- **Trade-offs**: Room type writes lock all its rooms briefly (≤ 10 rows).

### Decision: Rules in the service, `Binder` only for required fields
- **Context**: `vaadin-jooq.md` puts field rules in the `Binder` and business rules in the service; the uniqueness,
  limit and in-use rules must be tested on the service anyway.
- **Selected Approach**: `Binder` with `asRequired` on every field gives immediate feedback; the service validates all
  rules (including missing values) and is the single source of messages.
- **Trade-offs**: "Required" is checked twice; acceptable for three fields.

### Decision: Store room numbers in upper case
- **Context**: M4 case-insensitive uniqueness.
- **Selected Approach**: Trim and upper-case before the existence check and the write; the existing `UNIQUE(number)`
  is then a full backstop. Room type names keep their spelling; uniqueness via `lower(trim(name))` in the service.

## Risks & Mitigations
- Two managers create "Suite" and "suite" at the same moment → both pass the case-insensitive check; the exact-name
  `UNIQUE` still holds. Accepted for a single small hotel (one manager).
- Route names `room-types`/`rooms` could collide with a parallel spec → Vaadin fails at startup with a duplicate-route
  error; listed as revalidation trigger.
- Test rooms leaking into other specs' tests → cleanup in `@AfterEach` by number/name prefix, also in the concurrency
  test.

## References
- `.kiro/specs/foundation/design.md` — V1 schema, `StaffLayout` extension contract, test accounts, fixed clock.
- `.kiro/specs/reservation-core/design.md` — D2, `lockRoom`, read contract on `reservation`.
- `.kiro/specs/housekeeping/design.md` — H5, `ON DELETE CASCADE`.
