---
id: 004-cancel-own-reservation
unit: 001-guest-booking
intent: 002-online-booking
status: complete
priority: must
created: '2026-10-29T07:58:01Z'
assigned_bolt: 004-guest-booking
implemented: true
---

# Story: 004-cancel-own-reservation

## User Story

**As a** guest
**I want** to cancel my reservation online
**So that** I don't have to call the hotel when my plans change

## Acceptance Criteria

- [ ] **Given** my CONFIRMED reservation and now is before 14:00 on the day before arrival (Europe/Zurich), **When** I cancel and confirm, **Then** the status is CANCELLED and the room is free again
- [ ] **Given** now is 14:00 or later on the day before arrival, **When** I view my reservation, **Then** no cancel action is shown and I see "Online cancellation is no longer possible. Please call the hotel."
- [ ] **Given** a CANCELLED reservation, **When** I view it, **Then** no cancel action is shown
- [ ] **Given** the confirmation dialog, **When** I choose "No", **Then** nothing changes

## Technical Notes

- Check the deadline again in the service (not only in the UI) with an injected `Clock`.

## Dependencies

### Requires
- 003-view-own-reservation; intent 001 story 005-cancel-reservation

### Enables
- None

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| 13:59:59 the day before arrival | Cancellation allowed |
| Page opened before deadline, confirmed after | Rejected with the deadline message |

## Out of Scope

- Cancellation fees, changing the reservation
