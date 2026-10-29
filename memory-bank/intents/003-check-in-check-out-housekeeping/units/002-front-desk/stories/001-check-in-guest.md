---
id: 001-check-in-guest
unit: 002-front-desk
intent: 003-check-in-check-out-housekeeping
status: ready
priority: must
created: 2026-10-29T07:58:01Z
assigned_bolt: 006-front-desk
implemented: false
---

# Story: 001-check-in-guest

## User Story

**As a** receptionist
**I want** to check in an arriving guest
**So that** the system knows the room is occupied

## Acceptance Criteria

- [ ] **Given** a CONFIRMED reservation arriving today in a CLEAN room, **When** I check it in, **Then** its status is CHECKED_IN with the check-in time
- [ ] **Given** the room is DIRTY, **When** I try to check in, **Then** I see "Room 104 is not clean yet" and nothing changes
- [ ] **Given** today is before the arrival date, **When** I try to check in, **Then** I see "Check-in is possible from the arrival date" and nothing changes
- [ ] **Given** today is the departure date or later, **When** I try to check in, **Then** I see "The stay has already ended" and nothing changes
- [ ] **Given** a guest arrived one day late and departure is still in the future, **When** I check in, **Then** it succeeds
- [ ] **Given** a CANCELLED or already CHECKED_IN reservation, **When** I open it, **Then** check-in is not offered

## Technical Notes

- Transition method on the reservation aggregate; service checks cleaning status in the same transaction.

## Dependencies

### Requires
- 001-housekeeping/002-mark-room-cleaned

### Enables
- 002-check-out-guest, 003-room-overview

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Room marked dirty between opening and confirming | Rejected with the not-clean message |

## Out of Scope

- Moving the guest to another room at check-in (use change reservation before check-in)
