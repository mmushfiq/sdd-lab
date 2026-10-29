---
intent: 003-check-in-check-out-housekeeping
phase: inception
status: units-decomposed
updated: 2026-10-29T07:58:01Z
---

# Check-in, Check-out and Housekeeping - Unit Decomposition

## Units Overview

Two vertical units.

### Unit 1: 001-housekeeping

**Description**: Room cleaning status, cleaning list and "mark cleaned".

**Stories**:

- 001-view-cleaning-list
- 002-mark-room-cleaned

**Deliverables**:

- Migration adding `cleaning_status` and `dirty_since` to `room`
- `housekeeping` package: service, `CleaningListView`
- Browserless tests

**Dependencies**:

- Depends on: intent 001 (rooms, reservations)
- Depended by: 002-front-desk

**Estimated Complexity**: S

### Unit 2: 002-front-desk

**Description**: Check-in, check-out, room overview, and the new statuses in the reservation rules.

**Stories**:

- 001-check-in-guest
- 002-check-out-guest
- 003-room-overview
- 004-include-stays-in-reservation-rules

**Deliverables**:

- Migration for reservation `checked_in_at`, `checked_out_at`, statuses CHECKED_IN / CHECKED_OUT
- `frontdesk` package: service, `RoomOverviewView`, check-in/out dialogs
- Updates to intent-001/002 rules; service and Browserless tests

**Dependencies**:

- Depends on: 001-housekeeping, intent 001
- Depended by: intent 005 (reports use stays)

**Estimated Complexity**: M

## Requirement-to-Unit Mapping

- **FR-1** Room cleaning status → `001-housekeeping`
- **FR-2** Cleaning list → `001-housekeeping`
- **FR-3** Mark room cleaned → `001-housekeeping`
- **FR-4** Check in a guest → `002-front-desk`
- **FR-5** Check out a guest → `002-front-desk`
- **FR-6** Room overview → `002-front-desk`
- **FR-7** Stays in the reservation rules → `002-front-desk`

## Unit Dependency Graph

```text
[intent 001] ──> [001-housekeeping] ──> [002-front-desk]
```

## Execution Order

1. 001-housekeeping
2. 002-front-desk
