---
id: 006-protect-rooms-with-reservations
unit: 002-reservation
intent: 001-rooms-and-reservations
status: complete
priority: must
created: '2026-10-29T07:20:41Z'
assigned_bolt: 003-reservation
implemented: true
---

# Story: 006-protect-rooms-with-reservations

## User Story

**As a** manager
**I want** the system to stop me from deleting or blocking a room that guests have booked
**So that** existing reservations are never lost or left without a room

## Acceptance Criteria

- [ ] **Given** a room with any reservation (any status, past or future), **When** I try to delete it, **Then** I see "Room has reservations and cannot be deleted; set it out of service instead" and it stays
- [ ] **Given** a room with future CONFIRMED reservations, **When** I try to set it out of service, **Then** I see the message with the affected booking numbers and nothing changes
- [ ] **Given** a room whose reservations are all cancelled or in the past, **When** I set it out of service, **Then** it succeeds

## Technical Notes

- Implements the reservation-dependent rules of FR-2. The room catalog asks a reservation-side check before delete / out-of-service (avoid a cycle: room catalog calls an interface, reservation implements it, or the check is a query on the reservation table done in the room service).

## Dependencies

### Requires
- 002-create-reservation; 001-room-catalog stories 003 and 004

### Enables
- None

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Reservation departs today | Counts as not future; out of service allowed |

## Out of Scope

- Automatic moving of reservations to another room
