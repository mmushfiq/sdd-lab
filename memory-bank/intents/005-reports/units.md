---
intent: 005-reports
phase: inception
status: units-decomposed
updated: 2026-10-29T07:58:01Z
---

# Reports - Unit Decomposition

## Units Overview

One vertical unit.

### Unit 1: 001-management-reports

**Description**: Night-based occupancy and revenue calculation and the Manager's report view.

**Stories**:

- 001-monthly-occupancy-report
- 002-monthly-revenue-report
- 003-report-by-room-type

**Deliverables**:

- `report` package: `ReportService`, records, `MonthlyReportView` (Manager only)
- Service tests against a hand-calculated test month; Browserless test of the view

**Dependencies**:

- Depends on: intent 003 (stays), intent 004 (Manager role)
- Depended by: none

**Estimated Complexity**: S

## Requirement-to-Unit Mapping

- **FR-1** Select a month → `001-management-reports`
- **FR-2** Monthly occupancy → `001-management-reports`
- **FR-3** Monthly revenue → `001-management-reports`
- **FR-4** Breakdown by room type → `001-management-reports`

## Unit Dependency Graph

```text
[intent 003: 002-front-desk] ──┐
                               ├──> [001-management-reports]
[intent 004: 001-staff-access] ┘
```

## Execution Order

1. 001-management-reports
