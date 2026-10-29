---
id: 005-cancel-reservation
unit: 002-reservation
intent: 001-rooms-and-reservations
status: ready
priority: must
created: 2026-10-29T07:20:41Z
assigned_bolt: 003-reservation
implemented: false
---

# Story: 005-cancel-reservation

## User Story

**As a** receptionist
**I want** to cancel a reservation
**So that** the room becomes free for other guests

## Acceptance Criteria

- [ ] **Given** a CONFIRMED reservation, **When** I cancel and confirm the dialog, **Then** its status is CANCELLED and it is not deleted
- [ ] **Given** the cancellation dialog, **When** I choose "No", **Then** nothing changes
- [ ] **Given** a cancelled reservation, **When** I search availability for its nights, **Then** the room is listed
- [ ] **Given** a CANCELLED reservation, **When** I view it, **Then** cancel and change are not possible

## Technical Notes

- Reception can cancel at any time (no 24 h rule; that rule applies only to guests online in intent 002).

## Dependencies

### Requires
- 003-find-reservations

### Enables
- None

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Cancel a reservation whose stay already started | Allowed for reception |

## Out of Scope

- Cancellation fees
