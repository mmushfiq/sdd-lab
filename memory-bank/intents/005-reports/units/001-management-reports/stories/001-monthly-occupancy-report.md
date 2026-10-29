---
id: 001-monthly-occupancy-report
unit: 001-management-reports
intent: 005-reports
status: ready
priority: must
created: 2026-10-29T07:58:01Z
assigned_bolt: 009-management-reports
implemented: false
---

# Story: 001-monthly-occupancy-report

## User Story

**As a** manager
**I want** to see the occupancy of a month
**So that** I know how well the hotel is used without a spreadsheet

## Acceptance Criteria

- [ ] **Given** I open the report, **When** it loads, **Then** the current month is selected and months after it cannot be chosen
- [ ] **Given** September 2026 with 10 rooms and 150 counted nights, **When** I select it, **Then** I see available 300, occupied 150, occupancy 50.0 %
- [ ] **Given** a stay from 30 Sept to 2 Oct, **When** I view September and October, **Then** September counts 1 night (30th) and October 1 night (1st)
- [ ] **Given** a cancelled reservation and a no-show (CONFIRMED, never checked in), **When** I view the month, **Then** neither counts
- [ ] **Given** the current month and today is the 10th, **When** I view it, **Then** only nights 1st–9th count, and available = 10 rooms × 9 days
- [ ] **Given** a Receptionist, **When** they try to open the report, **Then** access is denied

## Technical Notes

- Stay nights = dates in [arrival, departure); departure after early check-out is the actual one.

## Dependencies

### Requires
- intent 003 bolt 006, intent 004 bolt 007

### Enables
- 002-monthly-revenue-report, 003-report-by-room-type

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Month with no stays | 0 occupied, 0.0 % |
| Today is the 1st of the month | Current month shows 0 available, occupancy "–" |

## Out of Scope

- Charts, yearly view
