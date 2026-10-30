# Research & Design Decisions

## Summary
- **Feature**: `housekeeping`
- **Discovery Scope**: Extension (light discovery). Builds on the foundation spec (not yet implemented: the codebase is
  still the empty skeleton); no new library.
- **Key Findings**:
  - Foundation's extension contract (`@Route(layout = StaffLayout.class)`, `@Menu`, `@RolesAllowed(StaffRole.Names.*)`)
    gives the cleaning list its navigation entry, role filtering and wrong-role refusal with no foundation change.
  - `room` is owned by foundation/management (one owner per table, `structure.md`), so the cleaning status lives in an
    own table keyed by `room_id`, not in a new `room` column.
  - A left join from `room` to the status table answers "dirty or not recorded" in one query, which makes rooms that
    management adds later appear on the cleaning list without any cross-spec call.

## Research Log

### Where the cleaning status lives
- **Context**: Brief leaves "column or own table" to design; steering says one owner per table.
- **Findings**: A column on `room` would make housekeeping write foundation's table and force management's room
  inserts to know about cleaning. An own table `room_cleaning_status (room_id PK/FK)` keeps writes inside housekeeping;
  `ON DELETE CASCADE` lets management delete a room without knowing this table.
- **Implications**: Migration `V20` creates the table and back-fills one `CLEAN` row per existing room (H1).

### "Immediately" on the cleaning list
- **Context**: Brief asks whether live push is needed.
- **Findings**: Vaadin server push (`@Push`) or UI polling would update an open list, but adds an app-shell-wide
  setting (foundation's `HrsApplication`) or timing-dependent tests. Ten rooms, one or two housekeepers: re-reading on
  open plus a "Refresh" button meets the brief's definition ("next load or refresh without manual sync").
- **Implications**: No push, no polling. The view reloads on every navigation (`BeforeEnterObserver`) and on Refresh.

### Idempotent status writes
- **Context**: 3.2 (already clean) and 5.2 (already dirty) must not fail or duplicate.
- **Findings**: jOOQ `insertInto(...).onDuplicateKeyUpdate()` is emulated on H2 as `MERGE`; with `room_id` as primary
  key one row per room is guaranteed by the database.
- **Implications**: `markClean` / `markDirty` are an existence check plus an upsert in one transaction.

### Consulted skills and guidance
- Steering `vaadin-jooq.md`, `testing.md`, `structure.md`, `domain.md`; foundation `design.md` (StaffLayout extension
  contract, `StaffRole.Names`, test accounts, fixed clock). No AIUP material used.

## Architecture Pattern Evaluation

| Option | Description | Strengths | Risks / Limitations | Notes |
|--------|-------------|-----------|---------------------|-------|
| Own table + left join from `room` | `room_cleaning_status`, missing row = Dirty | One writer, no coupling to management | One extra join | Selected |
| Column `room.cleaning_status` | NOT NULL column with default | No join | Housekeeping writes foundation's table; management must keep default | Rejected |
| Domain event on check-out | Reception publishes, housekeeping listens | Loose coupling | More machinery than one method call; harder to test synchronously | Rejected |

## Design Decisions

### Decision: Room without status counts as Dirty
- **Context**: Management may add rooms after V20 ran.
- **Alternatives Considered**: 1. Management inserts a status row (cross-spec write); 2. treat missing as Clean.
- **Selected Approach**: Missing row = Dirty (H5); the first `markClean` creates the row.
- **Rationale**: Safe for the clean-before-check-in invariant and needs no cross-spec call.
- **Trade-offs**: A newly added room needs one "Mark clean" before its first check-in.

### Decision: One service, one read query
- **Context**: Reception needs "is clean", "mark dirty" and, for its room overview, all statuses.
- **Selected Approach**: `HousekeepingService` exposes `isClean`, `markDirty`, `markClean`, `findCleaningList`,
  `findAllCleaningStatuses`; the repository has one read query for all rooms and one for a single room.
- **Rationale**: Reception reads through the owning service (no second query on this table elsewhere); ten rows make
  filtering in Java free.

### Decision: Access check on the view only
- **Context**: `vaadin-jooq.md` asks services to reject sensitive operations too.
- **Selected Approach**: `@RolesAllowed(HOUSEKEEPER, MANAGER)` on `CleaningListView`; no method security on the
  service, because reception's flows (receptionist role) must call `markDirty`/`isClean`, and foundation does not
  enable method security.
- **Trade-offs**: Any server code can call `markClean`; acceptable because only the view does, and reviewers check it.

### Synthesis outcomes
- **Generalization**: "cleaning list" and "all statuses for reception" are the same query; the list is a filter.
- **Build vs adopt**: Vaadin `Grid` with a component column, `Notification`, foundation menu mechanism; nothing custom.
- **Simplification**: no timestamps, no history, no push, no test migration (tests arrange state through the service
  and restore it), no confirmation dialog.

## Risks & Mitigations
- Stale list: a housekeeper could mark clean a room that became dirty again after the list was shown — mitigated by
  Refresh and reload on open; accepted for ten rooms (no version check).
- Shared rows in tests: Browserless tests change seeded rooms' statuses — every test class restores all rooms to Clean
  in `@AfterEach` so reception's later tests start from H1.

## References
- Foundation design: `.kiro/specs/foundation/design.md` (StaffLayout extension contract, test accounts).
- jOOQ manual, "ON DUPLICATE KEY UPDATE" emulation via `MERGE` for H2.
