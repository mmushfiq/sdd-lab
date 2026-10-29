---
id: 002-monthly-revenue-report
unit: 001-management-reports
intent: 005-reports
status: ready
priority: must
created: 2026-10-29T07:58:01Z
assigned_bolt: 009-management-reports
implemented: false
---

# Story: 002-monthly-revenue-report

## User Story

**As a** manager
**I want** to see the revenue of a month
**So that** I know how the business is doing

## Acceptance Criteria

- [ ] **Given** a 3-night stay costing 450.00 entirely in the month, **When** I view the month, **Then** it contributes 450.00
- [ ] **Given** a 3-night stay costing 100.00 with 2 nights in October and 1 in November, **When** I view each month, **Then** October shows 66.67 and November 33.33
- [ ] **Given** an early check-out (stay shortened from 4 to 2 nights, price 600.00 unchanged), **When** I view the month, **Then** it contributes 600.00 over its 2 actual nights
- [ ] **Given** cancelled reservations and no-shows, **When** I view the month, **Then** they contribute 0
- [ ] **Given** the report, **When** it shows revenue, **Then** it is in CHF with two decimals

## Technical Notes

- Exact per-night BigDecimal (e.g. scale 10), round the month total half-up to 0.01.

## Dependencies

### Requires
- 001-monthly-occupancy-report

### Enables
- 003-report-by-room-type

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Sum of split months vs. stay price | Differs by at most 0.01 due to rounding; documented |

## Out of Scope

- Payments received, taxes
