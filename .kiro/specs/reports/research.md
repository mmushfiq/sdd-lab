# Research & Design Decisions

## Summary
- **Feature**: `reports`
- **Discovery Scope**: Extension (light discovery): a read-only staff page on top of specified upstream tables; no new
  library, no schema change.
- **Key Findings**:
  - Reception's read contract already defines a stay (status `CHECKED_IN`/`CHECKED_OUT`), its stayed nights
    `[checked_in_on, checked_out_on)` and the fallback to `[arrival_date, departure_date)` when no `stay` row exists.
  - `room` has no creation date and management can add rooms at any time; an exact historical room count is not
    available without a new column, which the brief rules out ("no new tables") and which management owns.
  - Ten rooms and a few reservations per day: one query per report returning the overlapping stays, clipped per month in
    Java, is simpler and easier to test than generating a calendar of nights in H2 SQL.

## Research Log

### Upstream data available to reports
- **Context**: Which columns may reports read, and who defines their meaning?
- **Sources Consulted**: `.kiro/specs/reservation-core/design.md` (read contract, D2, statuses),
  `.kiro/specs/reception/design.md` (Data Models → Read contract for reports, R1–R5), `.kiro/specs/management/design.md`
  (M1, room removal 7.3/7.4), `.kiro/specs/foundation/design.md` (`room`, `StaffLayout`, `Clock`, test base classes).
- **Findings**:
  - `reservation(status, arrival_date, departure_date, nightly_rate)` may be read in a feature's own repository; the
    booked `nightly_rate` is fixed at booking (D2). Reservations are never deleted (reservation-core 1.4), guest erasure
    removes only `guest` rows (10.2).
  - `stay(reservation_id, checked_in_on, checked_out_on)`; open while `checked_out_on IS NULL`; check-out day may lie
    after the departure date (R5 Overdue) and check-in day after the arrival date (R3). Check-in before arrival is
    refused (R2), so `checked_in_on >= arrival_date`.
  - Price never changes with late check-in, early or late check-out (R3, R4, R5).
  - Rooms with any reservation cannot be removed (management 7.4); rooms can be added; no out-of-service state.
  - Menu orders used: 0 (home), 10/11 (front desk), 30/31 (reception), 50 (housekeeping), 80/81 (management).
- **Implications**: reports read `reservation`, `stay` and `room` only, never `guest`; menu order 90 is free.

### Room count history
- **Context**: Requirement for an occupancy denominator of "rooms existing in the period".
- **Findings**: No column records when a room was added; removed rooms are gone from `room`. Removed rooms never had a
  reservation, so occupied nights and revenue are unaffected.
- **Implications**: denominator = rooms at report time × nights (RP3), labelled on the page. Exact history would need a
  management-owned column (`room.created_on`) — listed as an optional upstream change, not required.

### Test data isolation
- **Context**: Other specs' rows are present in the shared test database.
- **Findings**: Reserved windows: reports 2026-01-01 – 2026-04-30, ids 8000–8999, test migration V170–V179. No other spec
  has reservations in that window; reservations in the past cannot be created through `ReservationService` (past arrival
  refused), so no other test creates rows there at runtime. Reception lists treat an open `stay` row as "staying"
  (departures list, room overview) — an open stay in V170 would appear in reception's lists for today and break its
  tests. Management's capacity check reads `RESERVED`/`CHECKED_IN` reservations of any date (largest number of guests).
- **Implications**: V170 has no open stay (the open-stay case is inserted by the test and removed afterwards); every
  reports reservation has one guest so it never constrains a capacity change; tests read the room count at test time
  instead of assuming ten; the "including tonight" case is asserted as a difference with and without the test's row.

## Architecture Pattern Evaluation

| Option | Description | Strengths | Risks / Limitations | Notes |
|--------|-------------|-----------|---------------------|-------|
| One query + Java clipping | Fetch stays overlapping the period, clip ranges per month in the service | Small, readable, unit-testable night arithmetic; one query per report | Rows grow with period length (tens per month) | Selected |
| SQL calendar aggregation | Generate one row per night (recursive CTE / `SYSTEM_RANGE`) and `GROUP BY` month | Pure SQL aggregates | H2-specific functions, harder to read and to keep identical for occupancy and revenue | Rejected |
| Materialised report table | Write nightly figures on check-in/out | Fast reads | Brief forbids new tables; writes from reports violate read-only | Rejected |

## Design Decisions

### Decision: Occupancy from actual stay nights, revenue from booked nights of stays
- **Context**: Open question in `domain.md`: which nights are occupied, which reservations earn revenue and when.
- **Alternatives Considered**:
  1. Both from actual stayed nights — one night set; but early check-out lowers revenue although the guest pays the full
     price, and Overdue nights add revenue nobody charges.
  2. Revenue at check-out (whole price in the check-out month) — simple, but a stay across month end lands in one month
     and checked-in guests earn nothing yet.
  3. Occupancy from actual nights, revenue per booked night of stays — selected.
- **Selected Approach**: RP4 and RP5 in requirements.md.
- **Rationale**: occupancy answers "were the rooms used", revenue answers "what did we charge"; reception R3–R5 keep the
  price unchanged, so revenue over all months equals the sum of the prices of all stays.
- **Trade-offs**: revenue nights and occupied nights of a month can differ (late check-in, early/late check-out); the
  page explains this (1.6). A late arrival checked in later makes its earlier booked nights count retroactively.
- **Follow-up**: `domain.md` "Revenue" wording ("from nights stayed") must be refined by the steering owner.

### Decision: Period limited to tonight
- **Context**: A checked-in guest's future nights and Reserved reservations are not facts yet.
- **Selected Approach**: last night ≤ today; open stays occupy up to tonight (RP2, RP4).
- **Trade-offs**: the current month is shown only up to today, which the manager chooses explicitly.

### Synthesis outcomes
- **Generalization**: month rows and the total row are the same computation on a night range; one method computes a
  `ReportRow` for any `[first, last]`, called once per month and once for the whole period.
- **Build vs. adopt**: Vaadin `DatePicker` and `Grid`, `java.time.YearMonth`, `BigDecimal` with `RoundingMode.HALF_UP`;
  no chart or export library (out of scope).
- **Simplification**: one page, one grid (occupancy and revenue columns side by side) instead of two views; one
  repository query; no separate period record (two `LocalDate` arguments); no caching.

### Consulted skills and guidance
- `kiro-spec-design` rules (light discovery, synthesis, review gate); steering `vaadin-jooq.md` (records, readOnly reads,
  ids, `Clock`), `testing.md` (Browserless default, one Playwright journey, traceability comments, own ID/date window).

## Risks & Mitigations
- Figures for recent months change when a late arrival is checked in later or an overdue guest checks out — inherent in
  the definitions; explained on the page.
- Rooms added later lower past occupancy (RP3) — labelled; exact history needs an upstream column (optional).
- Another spec adding reservations in Jan–Apr 2026 would change expected figures — window reserved in `roadmap.md`.

## References
- `.kiro/specs/reception/design.md` — stay read contract for reports.
- `.kiro/specs/reservation-core/design.md` — D2 booked rate, statuses, read contract.
- `.kiro/steering/roadmap.md` — reserved numbers per spec.
