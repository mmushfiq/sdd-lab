---
id: 007-anonymize-guest-data
unit: 002-reservation
intent: 001-rooms-and-reservations
status: complete
priority: should
created: '2026-10-29T07:20:41Z'
assigned_bolt: 003-reservation
implemented: true
---

# Story: 007-anonymize-guest-data

## User Story

**As a** receptionist
**I want** to anonymize a guest's personal data on their request
**So that** the hotel complies with the GDPR right to erasure

## Acceptance Criteria

- [ ] **Given** a CANCELLED reservation or one whose departure is in the past, **When** I anonymize and confirm, **Then** name, email and phone are replaced by "Anonymized" and booking number, room, dates, price and status stay
- [ ] **Given** a CONFIRMED reservation whose departure is today or later, **When** I open it, **Then** anonymizing is not possible
- [ ] **Given** an anonymized reservation, **When** I view it, **Then** it is shown as anonymized and cannot be anonymized or changed again
- [ ] **Given** the confirmation dialog, **When** I choose "No", **Then** nothing changes

## Technical Notes

- Store an `anonymized` flag; email column must accept the placeholder.
- Do not log the removed data.

## Dependencies

### Requires
- 003-find-reservations

### Enables
- None

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Guest has several reservations | Each is anonymized separately (bulk by email is out of scope) |

## Out of Scope

- Automatic retention/deletion rules
