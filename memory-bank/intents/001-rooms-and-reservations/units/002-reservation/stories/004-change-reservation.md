---
id: 004-change-reservation
unit: 002-reservation
intent: 001-rooms-and-reservations
status: ready
priority: must
created: 2026-10-29T07:20:41Z
assigned_bolt: 003-reservation
implemented: false
---

# Story: 004-change-reservation

## User Story

**As a** receptionist
**I want** to change the dates, room, number of guests or guest data of a reservation
**So that** I can handle guests' change requests

## Acceptance Criteria

- [ ] **Given** a CONFIRMED future reservation, **When** I change its dates to free nights and save, **Then** it is updated, the total price is recalculated with the current rate, and the booking number is unchanged
- [ ] **Given** a reservation from the 10th to the 12th, **When** I extend it to the 13th and no one else has the 12th, **Then** the change succeeds (no conflict with itself)
- [ ] **Given** the new dates overlap another confirmed reservation of the room, **When** I save, **Then** I see "Room is not available for these dates" and nothing changes
- [ ] **Given** a different room with too small capacity or out of service, **When** I save, **Then** I see an error and nothing changes
- [ ] **Given** a CANCELLED reservation or one whose departure is in the past, **When** I open it, **Then** changing is not possible
- [ ] **Given** I change only the guest phone, **When** I save, **Then** the price is not recalculated

## Technical Notes

- Same transaction + lock pattern as 002-create-reservation; exclude the reservation's own id from the overlap check.

## Dependencies

### Requires
- 003-find-reservations

### Enables
- None

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Arrival already today (guest staying) | Dates can't move arrival into the past; departure may change |
| Concurrent change and new booking for same nights | Exactly one succeeds |

## Out of Scope

- Change history / audit trail
