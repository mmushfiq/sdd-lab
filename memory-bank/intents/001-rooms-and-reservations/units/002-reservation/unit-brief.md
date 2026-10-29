---
unit: 002-reservation
intent: 001-rooms-and-reservations
phase: inception
status: ready
unit_type: backend
default_bolt_type: ddd-construction-bolt
created: 2026-10-29T07:20:41Z
updated: 2026-10-29T07:20:41Z
---

# Unit Brief: Reservation

## Purpose

Let reception find free rooms and manage reservations for walk-in and phone guests, with a hard guarantee that a room is never booked twice for overlapping nights.

## Scope

### In Scope
- Availability search by dates and number of guests
- Create, change and cancel reservations; booking number; price calculation
- Reservation list with filters
- Blocking room deletion / out-of-service while reservations exist (FR-2 rules)
- Anonymizing guest data on request

### Out of Scope
- Online self-service by guests (intent 002)
- Check-in, check-out, statuses CHECKED_IN/CHECKED_OUT (intent 003)
- Login and roles (intent 004), reports (intent 005)
- Emails to guests, payments

---

## Assigned Requirements

| FR | Requirement | Priority |
|----|-------------|----------|
| FR-2 (partial) | Rooms with reservations cannot be deleted or set out of service while they have future reservations | Must |
| FR-3 | Search availability | Must |
| FR-4 | Create a reservation | Must |
| FR-5 | Find and view reservations | Must |
| FR-6 | Change a reservation | Must |
| FR-7 | Cancel a reservation | Must |
| FR-8 | Anonymize guest data | Should |

---

## Domain Concepts

### Key Entities
| Entity | Description | Attributes |
|--------|-------------|------------|
| Reservation (aggregate root) | One room booked for one stay | id, bookingNumber (unique), room, arrival, departure, numberOfGuests, guest, totalPrice (CHF), status (CONFIRMED, CANCELLED), anonymized |
| Guest (value object) | Contact data of the person booking | name, email, phone |
| StayPeriod (value object) | Nights from arrival to departure | arrival, departure; nights(); overlaps(other) uses half-open interval [arrival, departure) |

### Key Operations
| Operation | Description | Inputs | Outputs |
|-----------|-------------|--------|---------|
| searchAvailability | Free, in-service rooms with enough capacity | arrival, departure, guests | list of room + total price |
| createReservation | Book a room | roomId, period, guests, guest data | Reservation with booking number, or RoomNotAvailableException |
| changeReservation | Change dates/room/guests/guest data | bookingNumber, new values | updated Reservation, or exception |
| cancelReservation | Set status CANCELLED | bookingNumber | updated Reservation |
| findReservations | Filtered list | bookingNumber?, name?, date range? | list |
| anonymizeGuest | Replace guest data with placeholder | bookingNumber | updated Reservation |

---

## Story Summary

- **Total Stories**: 7
- **Must Have**: 6
- **Should Have**: 1
- **Could Have**: 0

### Stories

- [ ] **001-search-availability**: Search free rooms for dates and guests - Must - Planned
- [ ] **002-create-reservation**: Book a room for a phone or walk-in guest - Must - Planned
- [ ] **003-find-reservations**: List and filter reservations - Must - Planned
- [ ] **004-change-reservation**: Change dates, room, guests or guest data - Must - Planned
- [ ] **005-cancel-reservation**: Cancel a confirmed reservation - Must - Planned
- [ ] **006-protect-rooms-with-reservations**: Block delete / out-of-service for booked rooms - Must - Planned
- [ ] **007-anonymize-guest-data**: Anonymize guest data on request - Should - Planned

---

## Dependencies

### Depends On
| Unit | Reason |
|------|--------|
| 001-room-catalog | Rooms, capacity, out-of-service flag and nightly rates |

### Depended By
| Unit | Reason |
|------|--------|
| Intents 002, 003, 005 | Online booking, check-in/out and reports build on reservations |

### External Dependencies
| System | Purpose | Risk |
|--------|---------|------|
| - | None | - |

---

## Technical Context

### Suggested Technology
Per `standards/`: Flyway migration for `reservation`, jOOQ, Spring `@Service` with `@Transactional`, Vaadin Flow views. Package `ch.martinelli.edu.hrs.reservation`.

### Integration Points
| Integration | Type | Protocol |
|-------------|------|----------|
| 001-room-catalog | DB (FK to room) + Java service calls | in-process |

### Data Storage
| Data | Type | Volume | Retention |
|------|------|--------|-----------|
| reservation | SQL (H2) | ~3,000 / year | Permanent; personal data anonymizable |

---

## Constraints

- **Double-booking protection**: H2 has no exclusion constraints. Create and change must, in one transaction, lock the room row (`SELECT ... FOR UPDATE`), check for overlapping CONFIRMED reservations, then insert/update. A concurrency test with two parallel bookings for the same room and nights must show exactly one succeeding.
- Total price stored on the reservation (DECIMAL(10,2)), not recalculated when rates change later.
- Booking number: unique, 8 uppercase characters without ambiguous ones (no 0/O, 1/I), random so it can't be guessed (guests will use it with their email in intent 002).

---

## Success Criteria

### Functional
- [ ] All FR-3 to FR-8 acceptance criteria pass, plus the FR-2 reservation rules
- [ ] Departure-day/arrival-day back-to-back bookings are allowed

### Non-Functional
- [ ] Concurrency test proves zero double bookings
- [ ] Search and save under 1 s
- [ ] A phone reservation can be completed in under 1 minute (Playwright IT of the full flow)

### Quality
- [ ] Every acceptance criterion has a test
- [ ] Code reviewed and approved

---

## Bolt Suggestions

| Bolt | Type | Stories | Objective |
|------|------|---------|-----------|
| 002-reservation | DDD | 001, 002, 003 | Reservation model, overlap rule, search, booking, list |
| 003-reservation | DDD | 004, 005, 006, 007 | Lifecycle changes and data protection rules |

---

## Notes

- The overlap rule (half-open interval) is the core of the domain; model it once in `StayPeriod` and reuse it in search, create and change.
