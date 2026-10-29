---
id: 003-view-own-reservation
unit: 001-guest-booking
intent: 002-online-booking
status: complete
priority: must
created: '2026-10-29T07:58:01Z'
assigned_bolt: 004-guest-booking
implemented: true
---

# Story: 003-view-own-reservation

## User Story

**As a** guest
**I want** to look up my reservation with booking number and email
**So that** I can check my booking details

## Acceptance Criteria

- [ ] **Given** a correct booking number and email (any letter case), **When** I search, **Then** I see booking number, dates, room type, guests, total price and status
- [ ] **Given** a wrong booking number or wrong email, **When** I search, **Then** I see "No reservation found for this booking number and email"
- [ ] **Given** an anonymized reservation, **When** I search with the old email, **Then** I see the same generic message
- [ ] **Given** either field empty, **When** I search, **Then** I see a required-field error

## Technical Notes

- Don't log the email entered.

## Dependencies

### Requires
- 002-book-room-online

### Enables
- 004-cancel-own-reservation

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Spaces around the booking number | Trimmed |

## Out of Scope

- Listing all reservations of an email address
