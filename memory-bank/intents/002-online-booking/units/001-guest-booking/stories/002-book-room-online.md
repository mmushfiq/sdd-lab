---
id: 002-book-room-online
unit: 001-guest-booking
intent: 002-online-booking
status: ready
priority: must
created: 2026-10-29T07:58:01Z
assigned_bolt: 004-guest-booking
implemented: false
---

# Story: 002-book-room-online

## User Story

**As a** guest
**I want** to book a room type by entering my contact data
**So that** my stay is reserved

## Acceptance Criteria

- [ ] **Given** a search result, **When** I choose a room type and enter name, email and phone and confirm, **Then** a CONFIRMED reservation for the free room of that type with the lowest number is stored, and I see booking number, dates, room type, guests, total price and the note to keep the booking number
- [ ] **Given** a missing or invalid field, **When** I confirm, **Then** I see a field error and nothing is stored
- [ ] **Given** the last free room of the type was booked after my search, **When** I confirm, **Then** I see "This room type is no longer available for these dates" and nothing is stored
- [ ] **Given** the booking form, **When** I read it, **Then** I see a short privacy note that only name, email and phone are stored to manage the reservation
- [ ] **Given** an online booking, **When** reception opens the reservation list, **Then** it is listed like any other reservation

## Technical Notes

- Call the intent-001 booking service per room (lowest number first), inside the same locked transaction pattern.

## Dependencies

### Requires
- 001-search-rooms-online; intent 001 story 002-create-reservation

### Enables
- 003-view-own-reservation

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Guest double-clicks confirm | Only one reservation is created (button disabled while saving) |

## Out of Scope

- Confirmation email, payment
