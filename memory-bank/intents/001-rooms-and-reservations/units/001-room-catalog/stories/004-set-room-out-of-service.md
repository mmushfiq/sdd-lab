---
id: 004-set-room-out-of-service
unit: 001-room-catalog
intent: 001-rooms-and-reservations
status: complete
priority: must
created: '2026-10-29T07:20:41Z'
assigned_bolt: 001-room-catalog
implemented: true
---

# Story: 004-set-room-out-of-service

## User Story

**As a** manager
**I want** to mark a room as out of service and back in service
**So that** a room under repair cannot be booked

## Acceptance Criteria

- [ ] **Given** an in-service room, **When** I set it out of service and save, **Then** the room list shows it as out of service
- [ ] **Given** an out-of-service room, **When** I set it back in service and save, **Then** the room list shows it as in service
- [ ] **Given** out-of-service rooms, **When** the room catalog service is asked for bookable rooms, **Then** they are not included

## Technical Notes

- Provide `findBookableRooms(minCapacity)` (in service, capacity ≥ n) for use by the reservation unit.
- The "reject while future reservations exist" rule is added by 002-reservation story 006.

## Dependencies

### Requires
- 003-manage-rooms

### Enables
- 002-reservation: 001-search-availability, 006-protect-rooms-with-reservations

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| All rooms out of service | Bookable rooms list is empty |

## Out of Scope

- Out-of-service periods with start/end dates
