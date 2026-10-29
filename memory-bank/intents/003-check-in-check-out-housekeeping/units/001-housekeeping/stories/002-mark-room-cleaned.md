---
id: 002-mark-room-cleaned
unit: 001-housekeeping
intent: 003-check-in-check-out-housekeeping
status: ready
priority: must
created: 2026-10-29T07:58:01Z
assigned_bolt: 005-housekeeping
implemented: false
---

# Story: 002-mark-room-cleaned

## User Story

**As a** housekeeping staff member
**I want** to mark a room as cleaned with one tap
**So that** reception can give it to the next guest

## Acceptance Criteria

- [ ] **Given** room 102 is on the cleaning list, **When** I tap "Cleaned", **Then** it is CLEAN and disappears from the list
- [ ] **Given** room 102 was marked clean on another tablet, **When** I tap "Cleaned" on my stale list, **Then** I see "Room is already clean" and the list refreshes
- [ ] **Given** the tablet layout, **When** I use the list, **Then** each "Cleaned" button is at least 44 px high

## Technical Notes

- Conditional update `WHERE cleaning_status = 'DIRTY'`; 0 rows updated → "already clean".

## Dependencies

### Requires
- 001-view-cleaning-list

### Enables
- 002-front-desk/001-check-in-guest

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Double tap | Second tap shows "already clean", nothing breaks |

## Out of Scope

- Undo
