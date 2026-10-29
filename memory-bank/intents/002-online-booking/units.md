---
intent: 002-online-booking
phase: inception
status: units-decomposed
updated: 2026-10-29T07:58:01Z
---

# Online Booking - Unit Decomposition

## Units Overview

One vertical unit (same reasoning as intent 001: Vaadin views and services in one feature package).

### Unit 1: 001-guest-booking

**Description**: Public guest views for search, booking, viewing and cancelling, reusing the reservation services of intent 001.

**Stories**:

- 001-search-rooms-online
- 002-book-room-online
- 003-view-own-reservation
- 004-cancel-own-reservation

**Deliverables**:

- `guestbooking` package: `GuestSearchView` (`/booking`), `GuestBookingView`, `MyReservationView`; small additions to the reservation service (search by room type, lookup by booking number + email, guest cancellation with deadline)
- Browserless tests, one Playwright IT for the full guest booking flow

**Dependencies**:

- Depends on: intent 001 (unit 002-reservation)
- Depended by: intent 004 (keeps `/booking` public)

**Estimated Complexity**: S

## Requirement-to-Unit Mapping

- **FR-1** Search rooms online → `001-guest-booking`
- **FR-2** Book a room online → `001-guest-booking`
- **FR-3** View own reservation → `001-guest-booking`
- **FR-4** Cancel own reservation → `001-guest-booking`
- **FR-5** Public guest area → `001-guest-booking`

## Unit Dependency Graph

```text
[intent 001: 002-reservation] ──> [001-guest-booking]
```

## Execution Order

1. 001-guest-booking (after intent 001 is built)
