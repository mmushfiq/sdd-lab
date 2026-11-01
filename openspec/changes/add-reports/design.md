# Design

## Context

Builds on `add-check-in-out` (statuses `CHECKED_IN`, `CHECKED_OUT`; early check-out shortens `departure_date`),
`add-reception-reservations` (`reservation.nightly_rate`) and `add-room-management` (`room`, `StaffLayout`).
Requirements are in `specs/reports/spec.md`.

## Goals / Non-Goals

**Goals:**

- Figures computed live from `reservation`; nothing stored that can drift.

**Non-Goals:**

- Export (CSV, PDF), charts, reports per room type, share of online bookings.

## Decisions

### 1. Compute in Java, not in SQL

`ReportService.yearReport(year)` in `ch.martinelli.edu.hrs.report` loads the counted reservations that overlap the
year (`status IN ('CHECKED_IN','CHECKED_OUT') AND arrival_date < Jan 1 next year AND departure_date > Jan 1`) and
walks each stay night by night, adding 1 room-night and `nightly_rate` to the month of that night (nights outside
the year are skipped). Ten rooms give at most 3,650 nights a year, so this is instant. Alternative: a SQL query with
a date series per month. Rejected: harder to read and to test for a few thousand rows.

The number of rooms is `count(*)` from `room` at report time. Occupancy = nights / (rooms × `YearMonth.lengthOfMonth()`),
year total = nights / (rooms × `Year.length()`), both `BigDecimal` rounded `HALF_UP` to one decimal of percent.

### 2. Result type

`record MonthFigures(YearMonth month, int roomNights, BigDecimal occupancyPercent, BigDecimal revenue)` and
`record YearReport(int year, List<MonthFigures> months, MonthFigures total)`.

### 3. View

| Class        | Package                            | Route     | Access |
|--------------|------------------------------------|-----------|--------|
| `ReportView` | `ch.martinelli.edu.hrs.report.ui`  | `reports` | `@RolesAllowed("MANAGER")`, `@AccessDeniedErrorRouter` |

`Select<Integer>` for the year (from the first reservation's year to next year, default current year from the
`Clock` bean), a `Grid` with month name, room-nights, occupancy (`20.0 %`) and revenue (`CHF 860.00`), and the total
row in the grid footer.

## Risks / Trade-offs

- [Number of rooms changes during a year] → Occupancy uses today's room count; acceptable for a hotel with a fixed
  ten rooms.
- [Checked in stays counted with their planned departure] → Figures for the current month can drop after an early
  check-out; this matches "nights actually stayed" once the guest has left.

## Migration Plan

No database migration.
