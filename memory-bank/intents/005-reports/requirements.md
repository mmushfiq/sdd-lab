---
intent: 005-reports
phase: inception
status: inception-complete
created: 2026-10-29T07:58:01Z
updated: 2026-10-29T07:58:01Z
---

# Requirements: Reports

## Intent Overview

The Manager sees monthly occupancy and revenue, in total and per room type, without manual spreadsheet work. Reports count only stays that actually happened (CHECKED_IN or CHECKED_OUT).

Source: `docs/hotel-requirements.md`.

## Business Goals

| Goal | Success Metric | Priority |
|------|----------------|----------|
| No spreadsheets for monthly figures | Manager reads occupancy and revenue for any past or current month in the app | Must |

---

## Functional Requirements

### FR-1: Select a month
- **Description**: The Manager selects a month and year for the report.
- **Acceptance Criteria**:
  - Default is the current month.
  - Months after the current month cannot be selected.
- **Priority**: Must
- **Related Stories**: 001-monthly-occupancy-report

### FR-2: Monthly occupancy
- **Description**: Occupancy of the selected month.
- **Acceptance Criteria**:
  - Counted stays: reservations with status CHECKED_IN or CHECKED_OUT.
  - Each night of a stay counts in the month it falls in (a night is the date the guest sleeps; the departure date is not a night).
  - Occupied room-nights = number of counted nights in the month.
  - Available room-nights = number of rooms × days in the month (out-of-service rooms are included).
  - Occupancy % = occupied ÷ available × 100, shown with one decimal.
  - For the current month, only nights up to and including yesterday are counted in both numbers.
- **Priority**: Must
- **Related Stories**: 001-monthly-occupancy-report

### FR-3: Monthly revenue
- **Description**: Revenue of the selected month.
- **Acceptance Criteria**:
  - Revenue per night of a stay = total price ÷ number of nights of the stay.
  - Monthly revenue = sum of the revenue of all counted nights in the month, in CHF with two decimals.
  - Cancelled reservations and no-shows (CONFIRMED, never checked in) are not counted.
  - Rounding: per-night amounts are kept exact (BigDecimal) and only the monthly sum is rounded half-up to 0.01.
- **Priority**: Must
- **Related Stories**: 002-monthly-revenue-report

### FR-4: Breakdown by room type
- **Description**: Occupancy and revenue per room type for the selected month.
- **Acceptance Criteria**:
  - One row per room type: number of rooms, occupied room-nights, occupancy %, revenue.
  - The rows' room-nights and revenue add up to the totals.
  - A stay is assigned to the room type of its room.
- **Priority**: Must
- **Related Stories**: 003-report-by-room-type

---

## Non-Functional Requirements

### Performance
| Requirement | Metric | Target |
|-------------|--------|--------|
| Report | Load time for one month | < 2 s |

### Security
| Requirement | Standard | Notes |
|-------------|----------|-------|
| Access | Role MANAGER | Uses intent 004 role rules |

### Accuracy
| Requirement | Metric | Target |
|-------------|--------|--------|
| Calculation | Deviation from a hand-calculated test month | 0.00 CHF, 0.0 % |

---

## Constraints

### Technical Constraints

**Project-wide standards**: loaded from memory-bank standards folder by Construction Agent

**Intent-specific constraints**:
- Hotel time zone Europe/Zurich for "current month" and "yesterday".
- On screen only; no export.

### Business Constraints
- Single property; one currency (CHF).

---

## Assumptions

| Assumption | Risk if Invalid | Mitigation |
|------------|-----------------|------------|
| Room count is the current number of rooms | Past months skewed if rooms were added | Ten rooms are fixed for this hotel |
| Stored total price = revenue (payment at hotel not tracked) | Revenue differs from cash received | Record payments in a later intent if needed |

---

## Open Questions

| Question | Owner | Due Date | Resolution |
|----------|-------|----------|------------|
| Is an export (CSV/PDF) needed? | Manager | After go-live | Pending |
