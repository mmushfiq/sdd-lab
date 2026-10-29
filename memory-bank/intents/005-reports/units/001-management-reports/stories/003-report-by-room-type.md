---
id: 003-report-by-room-type
unit: 001-management-reports
intent: 005-reports
status: ready
priority: must
created: 2026-10-29T07:58:01Z
assigned_bolt: 009-management-reports
implemented: false
---

# Story: 003-report-by-room-type

## User Story

**As a** manager
**I want** occupancy and revenue per room type
**So that** I can see which room types sell well and adjust rates

## Acceptance Criteria

- [ ] **Given** a month, **When** I view the report, **Then** I see one row per room type with number of rooms, occupied room-nights, occupancy % and revenue
- [ ] **Given** the rows, **When** I add up room-nights and revenue, **Then** they equal the totals (revenue may differ by rounding of at most 0.01 per row, and is shown so the total equals the sum of exact values)
- [ ] **Given** a room type with no stays, **When** I view the month, **Then** its row shows 0 and 0.0 %

## Technical Notes

- Room type of a stay = current type of its room.

## Dependencies

### Requires
- 002-monthly-revenue-report

### Enables
- None

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Room type without rooms | Row shows 0 rooms, occupancy "–" |

## Out of Scope

- Breakdown per room
