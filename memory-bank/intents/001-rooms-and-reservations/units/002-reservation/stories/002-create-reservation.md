---
id: 002-create-reservation
unit: 002-reservation
intent: 001-rooms-and-reservations
status: ready
priority: must
created: 2026-10-29T07:20:41Z
assigned_bolt: 002-reservation
implemented: false
---

# Story: 002-create-reservation

## User Story

**As a** receptionist
**I want** to book a room from the search results for a phone or walk-in guest
**So that** the room is reserved for them and nobody else can take it

## Acceptance Criteria

- [ ] **Given** a search result, **When** I choose a room and enter name, email and phone and save, **Then** a CONFIRMED reservation is stored with total price = rate × nights and I see its booking number
- [ ] **Given** a missing name, email or phone, or an invalid email, **When** I save, **Then** I see a field error and nothing is stored
- [ ] **Given** more guests than the room's capacity, **When** I save, **Then** I see a capacity error and nothing is stored
- [ ] **Given** another reservation for the same room and overlapping nights was saved after my search, **When** I save, **Then** I see "Room is no longer available for these dates" and nothing is stored
- [ ] **Given** two bookings for the same room and nights are saved at the same moment, **When** both transactions run, **Then** exactly one succeeds
- [ ] **Given** a room type rate changes after booking, **When** I view the reservation, **Then** its total price is unchanged

## Technical Notes

- `@Transactional` service: lock the room row (`SELECT ... FOR UPDATE`), re-check overlap, insert. Throw `RoomNotAvailableException` on conflict.
- Booking number: 8 random uppercase characters from an alphabet without 0/O/1/I; retry on the (unlikely) unique-constraint collision.
- Concurrency test in a service integration test with two threads.

## Dependencies

### Requires
- 001-search-availability

### Enables
- 003-find-reservations, 004-change-reservation, 005-cancel-reservation

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Room set out of service after search | "Room is no longer available", nothing stored |
| Email with uppercase letters | Stored as entered, compared case-insensitively later |

## Out of Scope

- Confirmation email to the guest
- Several rooms in one reservation
