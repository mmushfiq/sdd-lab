---
unit: 001-management-reports
intent: 005-reports
phase: inception
status: ready
unit_type: backend
default_bolt_type: ddd-construction-bolt
created: 2026-10-29T07:58:01Z
updated: 2026-10-29T07:58:01Z
---

# Unit Brief: Management Reports

## Purpose

Give the Manager exact monthly occupancy and revenue figures from actual stays.

## Scope

### In Scope
- Month selection, occupancy, revenue, breakdown by room type

### Out of Scope
- Export, charts, forecasts of future bookings, payment reconciliation

---

## Assigned Requirements

| FR | Requirement | Priority |
|----|-------------|----------|
| FR-1 | Select a month | Must |
| FR-2 | Monthly occupancy | Must |
| FR-3 | Monthly revenue | Must |
| FR-4 | Breakdown by room type | Must |

---

## Domain Concepts

### Key Entities
| Entity | Description | Attributes |
|--------|-------------|------------|
| Stay (read model) | A counted reservation | room, roomType, arrival, departure, totalPrice |
| StayNight (derived) | One night of a stay | date, roomType, revenue (= totalPrice ÷ nights) |
| MonthlyReport (value) | Result | month, availableRoomNights, occupiedRoomNights, occupancy %, revenue, rows per room type |

### Key Operations
| Operation | Description | Inputs | Outputs |
|-----------|-------------|--------|---------|
| monthlyReport | Calculate the report | YearMonth, today | MonthlyReport |

---

## Story Summary

- **Total Stories**: 3
- **Must Have**: 3
- **Should Have**: 0
- **Could Have**: 0

### Stories

- [ ] **001-monthly-occupancy-report**: Occupancy % for a month - Must - Planned
- [ ] **002-monthly-revenue-report**: Revenue for a month - Must - Planned
- [ ] **003-report-by-room-type**: Breakdown per room type - Must - Planned

---

## Dependencies

### Depends On
| Unit | Reason |
|------|--------|
| intent 003 / 002-front-desk | CHECKED_IN / CHECKED_OUT stays |
| intent 004 / 001-staff-access | Manager-only access |

### Depended By
| Unit | Reason |
|------|--------|
| - | - |

### External Dependencies
| System | Purpose | Risk |
|--------|---------|------|
| - | None | - |

---

## Technical Context

### Suggested Technology
Load the month's overlapping stays with one jOOQ query and split them into nights in Java (≤ 310 room-nights per month, so no SQL date series needed). Package `ch.martinelli.edu.hrs.report`. Injected `Clock`.

### Integration Points
| Integration | Type | Protocol |
|-------------|------|----------|
| reservation, room, room_type tables | DB read | in-process |

### Data Storage
| Data | Type | Volume | Retention |
|------|------|--------|-----------|
| - | Computed on request, nothing stored | - | - |

---

## Constraints

- BigDecimal for money; round only the final sums.

---

## Success Criteria

### Functional
- [ ] FR-1 to FR-4 pass against a hand-calculated test month incl. a stay crossing the month boundary and an early check-out

### Non-Functional
- [ ] Report in under 2 s; Manager-only

### Quality
- [ ] Every acceptance criterion has a test
- [ ] Code reviewed and approved

---

## Bolt Suggestions

| Bolt | Type | Stories | Objective |
|------|------|---------|-----------|
| 009-management-reports | DDD | 001-003 | Night-based calculation and report view |

---

## Notes

- The night-splitting rule is the core; model it once and reuse it for both occupancy and revenue.
