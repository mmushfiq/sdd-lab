---
id: 002-delete-room-type
unit: 001-room-catalog
intent: 001-rooms-and-reservations
status: ready
priority: must
created: 2026-10-29T07:20:41Z
assigned_bolt: 001-room-catalog
implemented: false
---

# Story: 002-delete-room-type

## User Story

**As a** manager
**I want** to delete a room type that is no longer used
**So that** the list only contains relevant types

## Acceptance Criteria

- [ ] **Given** a room type with no rooms, **When** I delete it and confirm, **Then** it disappears from the list
- [ ] **Given** a room type used by at least one room, **When** I try to delete it, **Then** I see "Room type is used by rooms and cannot be deleted" and it stays
- [ ] **Given** the delete confirmation, **When** I cancel, **Then** nothing is deleted

## Technical Notes

- Check usage in the service; the FK from `room` is the safety net.

## Dependencies

### Requires
- 001-manage-room-types

### Enables
- None

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Type deleted by another user meanwhile | Message "Room type no longer exists", list refreshes |

## Out of Scope

- Bulk delete
