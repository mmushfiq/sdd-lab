---
id: 004-include-stays-in-reservation-rules
unit: 002-front-desk
intent: 003-check-in-check-out-housekeeping
status: ready
priority: must
created: 2026-10-29T07:58:01Z
assigned_bolt: 006-front-desk
implemented: false
---

# Story: 004-include-stays-in-reservation-rules

## User Story

**As a** receptionist
**I want** the reservation rules to account for guests who are checked in or have left
**So that** no room is double-booked and finished stays can't be changed

## Acceptance Criteria

- [ ] **Given** room 101 is CHECKED_IN until the 12th, **When** anyone searches the 10th–11th, **Then** room 101 is not available
- [ ] **Given** a CHECKED_OUT reservation after early departure today, **When** anyone searches from today, **Then** the room is available
- [ ] **Given** a CHECKED_IN or CHECKED_OUT reservation, **When** reception or the guest opens it, **Then** change and cancel are not offered
- [ ] **Given** a room with a CHECKED_IN reservation, **When** the manager tries to set it out of service or delete it, **Then** it is rejected with the booking number
- [ ] **Given** a CHECKED_OUT reservation, **When** reception anonymizes it, **Then** it succeeds
- [ ] **Given** all tests of intents 001 and 002, **When** they run, **Then** they still pass

## Technical Notes

- Overlap predicate becomes `status <> 'CANCELLED'`; keep it in one place.

## Dependencies

### Requires
- 001-check-in-guest, 002-check-out-guest

### Enables
- intent 005 reports

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Guest leaves early, new booking for the freed nights | Allowed |

## Out of Scope

- History of status changes
