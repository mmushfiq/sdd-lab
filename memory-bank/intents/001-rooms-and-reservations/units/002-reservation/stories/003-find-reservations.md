---
id: 003-find-reservations
unit: 002-reservation
intent: 001-rooms-and-reservations
status: complete
priority: must
created: '2026-10-29T07:20:41Z'
assigned_bolt: 002-reservation
implemented: true
---

# Story: 003-find-reservations

## User Story

**As a** receptionist
**I want** to list reservations and find one by booking number, guest name or dates
**So that** I can answer a guest's question without paper lists

## Acceptance Criteria

- [ ] **Given** the reservation list, **When** I open it, **Then** I see CONFIRMED reservations with departure today or later, sorted by arrival, with booking number, guest name, room, arrival, departure, guests, total price and status
- [ ] **Given** a booking number, **When** I filter by it, **Then** I see exactly that reservation (any status)
- [ ] **Given** a partial guest name "mül", **When** I filter, **Then** I see reservations whose guest name contains it, case-insensitive
- [ ] **Given** a date range, **When** I filter, **Then** I see reservations whose stay overlaps the range
- [ ] **Given** the "show cancelled" option, **When** I enable it, **Then** cancelled reservations are included

## Technical Notes

- Lazy-loading Grid is not needed at this volume; a simple filtered query is enough.

## Dependencies

### Requires
- 002-create-reservation

### Enables
- 004-change-reservation, 005-cancel-reservation, 007-anonymize-guest-data (actions start from the list)

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| No match | Empty list with "No reservations found" |
| Booking number in lowercase | Matched case-insensitively |

## Out of Scope

- Export, printing
