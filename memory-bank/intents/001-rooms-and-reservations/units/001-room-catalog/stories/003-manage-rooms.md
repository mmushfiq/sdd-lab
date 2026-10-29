---
id: 003-manage-rooms
unit: 001-room-catalog
intent: 001-rooms-and-reservations
status: ready
priority: must
created: 2026-10-29T07:20:41Z
assigned_bolt: 001-room-catalog
implemented: false
---

# Story: 003-manage-rooms

## User Story

**As a** manager
**I want** to create, edit, list and delete rooms with a room number and room type
**So that** the system knows which rooms the hotel can sell

## Acceptance Criteria

- [ ] **Given** the room view, **When** I open it, **Then** I see all rooms with number, room type, capacity, nightly rate and out-of-service state, sorted by number
- [ ] **Given** room number "111" and type "Double", **When** I save, **Then** the room appears in the list
- [ ] **Given** an existing room "101", **When** I save another room "101", **Then** I see "Room number already exists" and nothing is saved
- [ ] **Given** no room type selected, **When** I save, **Then** I see a required-field error
- [ ] **Given** an existing room, **When** I change its type and save, **Then** the list shows the new type
- [ ] **Given** a room, **When** I delete it and confirm, **Then** it disappears from the list

## Technical Notes

- Migration creates `room` (unique `number`, FK to `room_type`, `out_of_service` default false) and seeds rooms 101-110 (placeholder mix: 3 Single, 5 Double, 2 Family).
- Deleting a room with reservations is blocked later by 002-reservation story 006.

## Dependencies

### Requires
- 001-manage-room-types

### Enables
- 004-set-room-out-of-service, unit 002-reservation

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Room number with spaces | Trimmed |
| Room number longer than 10 characters | Validation error |

## Out of Scope

- Floor, view, amenities
