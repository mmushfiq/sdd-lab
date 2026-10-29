---
id: 001-view-cleaning-list
unit: 001-housekeeping
intent: 003-check-in-check-out-housekeeping
status: ready
priority: must
created: 2026-10-29T07:58:01Z
assigned_bolt: 005-housekeeping
implemented: false
---

# Story: 001-view-cleaning-list

## User Story

**As a** housekeeping staff member
**I want** to see on my tablet which rooms need cleaning
**So that** I clean the right rooms first

## Acceptance Criteria

- [ ] **Given** the migration has run, **When** I look at any existing room, **Then** its cleaning status is CLEAN
- [ ] **Given** rooms 102 and 105 are DIRTY and a guest arrives in 105 today, **When** I open the cleaning list, **Then** I see 105 first, then 102, each with room number, type and dirty-since time, and 105 marked "Arrival today"
- [ ] **Given** no DIRTY rooms, **When** I open the list, **Then** I see "All rooms are clean"
- [ ] **Given** the room list of intent 001, **When** I open it, **Then** it shows each room's cleaning status

## Technical Notes

- Add `cleaning_status` (default CLEAN) and `dirty_since` to `room`.
- Expose `markDirty(roomId, time)` for check-out (bolt 006).

## Dependencies

### Requires
- intent 001 complete

### Enables
- 002-mark-room-cleaned, 002-front-desk stories

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Out-of-service room is dirty | Still listed |

## Out of Scope

- Assigning rooms to cleaners
