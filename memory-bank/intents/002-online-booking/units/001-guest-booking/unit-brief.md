---
unit: 001-guest-booking
intent: 002-online-booking
phase: inception
status: complete
unit_type: backend
default_bolt_type: simple-construction-bolt
created: '2026-10-29T07:58:01Z'
updated: '2026-10-29T07:58:01Z'
---

# Unit Brief: Guest Booking

## Purpose

Give guests a public self-service area to find, book, view and cancel a room without calling the hotel.

## Scope

### In Scope
- Public views under `/booking`
- Availability by room type, booking with automatic room assignment
- Lookup by booking number + email; cancellation with the 14:00-day-before deadline

### Out of Scope
- Changing a reservation online (reception only)
- Emails, payment, CAPTCHA
- Login (intent 004 only protects staff views)

---

## Assigned Requirements

| FR | Requirement | Priority |
|----|-------------|----------|
| FR-1 | Search rooms online | Must |
| FR-2 | Book a room online | Must |
| FR-3 | View own reservation | Must |
| FR-4 | Cancel own reservation | Must |
| FR-5 | Public guest area | Must |

---

## Domain Concepts

### Key Entities
| Entity | Description | Attributes |
|--------|-------------|------------|
| Reservation (from intent 001) | Reused aggregate | as defined in intent 001 |
| RoomTypeAvailability (read model) | Free rooms per type for a period | roomType, capacity, nightlyRate, totalPrice, freeRooms |
| CancellationDeadline (value) | Last moment a guest may cancel online | 14:00 Europe/Zurich on arrival − 1 day |

### Key Operations
| Operation | Description | Inputs | Outputs |
|-----------|-------------|--------|---------|
| searchByRoomType | Free rooms grouped by type | arrival, departure, guests | list of RoomTypeAvailability |
| bookRoomType | Book any free room of a type | roomTypeId, period, guests, guest data | Reservation or exception |
| findByBookingNumberAndEmail | Guest lookup | bookingNumber, email | Optional reservation |
| cancelByGuest | Cancel before deadline | bookingNumber, email, now | updated reservation or DeadlinePassedException |

---

## Story Summary

- **Total Stories**: 4
- **Must Have**: 4
- **Should Have**: 0
- **Could Have**: 0

### Stories

- [ ] **001-search-rooms-online**: Search free room types on the public page - Must - Planned
- [ ] **002-book-room-online**: Book a room type and get a booking number - Must - Planned
- [ ] **003-view-own-reservation**: Look up own reservation with booking number + email - Must - Planned
- [ ] **004-cancel-own-reservation**: Cancel online before the deadline - Must - Planned

---

## Dependencies

### Depends On
| Unit | Reason |
|------|--------|
| intent 001 / 002-reservation | Availability, booking, double-booking protection, booking numbers |

### Depended By
| Unit | Reason |
|------|--------|
| intent 004 / 001-staff-access | Must keep `/booking` public |

### External Dependencies
| System | Purpose | Risk |
|--------|---------|------|
| - | None | - |

---

## Technical Context

### Suggested Technology
Vaadin Flow views annotated for anonymous access later (`@AnonymousAllowed` once Spring Security is added in intent 004). Inject a `Clock` (Europe/Zurich) for deadline tests.

### Integration Points
| Integration | Type | Protocol |
|-------------|------|----------|
| ReservationService (intent 001) | Java service calls | in-process |

### Data Storage
| Data | Type | Volume | Retention |
|------|------|--------|-----------|
| reservation (existing table) | SQL | + online share | as intent 001 |

---

## Constraints

- Room assignment for online bookings picks the free room of the type with the lowest room number (deterministic, testable).
- Booking must reuse the locking/overlap logic of intent 001.

---

## Success Criteria

### Functional
- [ ] All FR-1 to FR-5 acceptance criteria pass

### Non-Functional
- [ ] Playwright IT completes a guest booking flow; manual check under 3 minutes
- [ ] Generic error on lookup mismatch

### Quality
- [ ] Every acceptance criterion has a test
- [ ] Code reviewed and approved

---

## Bolt Suggestions

| Bolt | Type | Stories | Objective |
|------|------|---------|-----------|
| 004-guest-booking | Simple | 001-004 | Public guest area end to end |

---

## Notes

- Deadline tests must use a fixed `Clock`, not the system time.
