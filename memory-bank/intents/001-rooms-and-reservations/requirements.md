---
intent: 001-rooms-and-reservations
phase: inception
status: inception-complete
created: 2026-10-29T07:18:10Z
updated: 2026-10-29T07:40:42Z
---

# Requirements: Rooms and Reservations

## Intent Overview

Replace the hotel's paper lists with one shared, up-to-date record of rooms and reservations. Staff maintain the ten rooms, their room types and rates, search for free rooms by date range and number of guests, and create, change and cancel reservations for walk-in and phone guests. The system guarantees that no room is ever booked twice for overlapping nights.

Source: `docs/hotel-requirements.md`. Online booking by guests, check-in/check-out, housekeeping, login and reports follow in intents 002-005.

## Business Goals

| Goal | Success Metric | Priority |
|------|----------------|----------|
| No double bookings | Zero reservations for the same room with overlapping nights, ever | Must |
| One shared view of rooms and reservations | Reception finds any reservation without paper lists | Must |
| Fast reservation handling at reception | A receptionist creates a reservation in under 1 minute | Should |

---

## Functional Requirements

### FR-1: Manage room types
- **Description**: Staff can create, edit and delete room types. A room type has a unique name, a maximum number of guests (capacity) and a nightly rate.
- **Acceptance Criteria**:
  - Name is required and unique (case-insensitive).
  - Capacity is a whole number from 1 to 10.
  - Nightly rate is a positive amount in CHF with two decimal places.
  - A room type that is used by at least one room cannot be deleted; the user sees why.
  - Changing a nightly rate does not change the price of existing reservations.
- **Priority**: Must
- **Related Stories**: TBD

### FR-2: Manage rooms
- **Description**: Staff can create, edit and delete rooms. A room has a unique room number, a room type and an "out of service" flag.
- **Acceptance Criteria**:
  - Room number is required and unique.
  - Every room has exactly one room type.
  - A room with any reservation (past or future) cannot be deleted; it can only be set out of service.
  - Setting a room out of service is rejected while it has future confirmed reservations; the user sees the affected booking numbers.
  - A room that is out of service never appears in availability search and cannot be reserved.
- **Priority**: Must
- **Related Stories**: TBD

### FR-3: Search availability
- **Description**: Staff can search for rooms that are free for an arrival date, a departure date and a number of guests.
- **Acceptance Criteria**:
  - Arrival date must be today or later; departure date must be after the arrival date.
  - Number of guests is at least 1.
  - Results show only rooms that are in service, have capacity ≥ number of guests, and have no confirmed reservation overlapping the requested nights.
  - Nights are counted from arrival to departure: a reservation departing on a date does not block a new arrival on that same date.
  - Each result shows room number, room type, capacity, nightly rate and total price (nightly rate × number of nights).
  - If no room is free, the user sees a clear "no rooms available" message.
- **Priority**: Must
- **Related Stories**: TBD

### FR-4: Create a reservation
- **Description**: A receptionist creates a reservation for one room from the search results for a walk-in or phone guest.
- **Acceptance Criteria**:
  - A reservation is for exactly one room, one arrival date, one departure date and a number of guests.
  - Guest name, email and phone are required; email must be a valid address.
  - Number of guests must not exceed the room's capacity.
  - The total price is calculated as nightly rate × nights and stored on the reservation.
  - The system assigns a unique booking number and shows it after saving.
  - New reservations have status CONFIRMED.
  - If the room was booked by someone else in the meantime, saving is rejected with a clear message and nothing is stored.
- **Priority**: Must
- **Related Stories**: TBD

### FR-5: Find and view reservations
- **Description**: Staff can list reservations and find a reservation by booking number, guest name or date range.
- **Acceptance Criteria**:
  - The list shows booking number, guest name, room number, arrival, departure, number of guests, total price and status.
  - The list can be filtered by booking number, guest name (partial match) and a date range (reservations whose stay overlaps it).
  - By default the list shows confirmed reservations from today onwards, sorted by arrival date.
- **Priority**: Must
- **Related Stories**: TBD

### FR-6: Change a reservation
- **Description**: A receptionist can change the dates, room, number of guests and guest details of a confirmed reservation.
- **Acceptance Criteria**:
  - Only reservations with status CONFIRMED and a departure date in the future can be changed.
  - All rules of FR-3 and FR-4 apply to the changed reservation (dates, capacity, no overlap with other reservations, room in service).
  - The reservation does not conflict with itself when its own dates are changed.
  - The total price is recalculated with the current nightly rate when dates or room change.
  - The booking number never changes.
- **Priority**: Must
- **Related Stories**: TBD

### FR-7: Cancel a reservation
- **Description**: A receptionist can cancel a confirmed reservation at any time.
- **Acceptance Criteria**:
  - The user confirms the cancellation before it is applied.
  - The status changes to CANCELLED; the reservation is kept, not deleted.
  - The room is immediately free again for those nights in availability search.
  - A cancelled reservation cannot be changed or cancelled again.
- **Priority**: Must
- **Related Stories**: TBD

### FR-8: Anonymize guest data
- **Description**: On a guest's request, staff can anonymize the personal data of a reservation.
- **Acceptance Criteria**:
  - Only reservations that are CANCELLED or whose departure date is in the past can be anonymized.
  - Guest name, email and phone are replaced by a fixed placeholder; booking number, room, dates and price are kept for reporting.
  - The user confirms before anonymizing; the action cannot be undone.
- **Priority**: Should
- **Related Stories**: TBD

---

## Non-Functional Requirements

### Performance
| Requirement | Metric | Target |
|-------------|--------|--------|
| Availability search | Time until results are shown | < 1 s |
| Reservation list and save | Time until the view updates | < 1 s |

### Scalability
| Requirement | Metric | Target |
|-------------|--------|--------|
| Rooms | Number of rooms | 10 (single property; no scaling beyond that) |
| Concurrent staff users | Active sessions | 5 |

### Security
| Requirement | Standard | Notes |
|-------------|----------|-------|
| Authentication | None in this intent | Staff screens are open until intent 004 adds login and roles |

### Reliability
| Requirement | Metric | Target |
|-------------|--------|--------|
| Double-booking protection | Overlapping confirmed reservations for one room | 0, also when two users save at the same moment (enforced in the database transaction, not only in the UI) |

### Usability
| Requirement | Metric | Target |
|-------------|--------|--------|
| Screen sizes | Supported devices | Desktop (reception) and tablet |
| Reception speed | Time to create a reservation for a phone guest | < 1 minute |

### Compliance
| Requirement | Standard | Notes |
|-------------|----------|-------|
| Data minimization | GDPR | Store only guest name, email and phone |
| Right to erasure | GDPR | Anonymization on request (FR-8); automatic retention rules later |

---

## Constraints

### Technical Constraints

**Project-wide standards**: Required standards will be loaded from memory-bank standards folder by Construction Agent

**Intent-specific constraints**:
- Prices are stored in CHF with two decimal places; no currency conversion.

### Business Constraints
- Exactly ten rooms, one property.
- Payment is settled at the hotel; no online payment.

---

## Assumptions

| Assumption | Risk if Invalid | Mitigation |
|------------|-----------------|------------|
| One fixed nightly rate per room type (no seasonal or weekend rates) | Prices are wrong in high season | Rate model is isolated in the room type; add rate periods in a later intent |
| One reservation = one room | Families or groups need several bookings | Reception creates one reservation per room |
| Currency is CHF | Wrong currency shown | Currency is a single constant |
| Staff screens are open until intent 004 | Anyone with network access can change data | Run only on the hotel's local network until login is built |

---

## Open Questions

| Question | Owner | Due Date | Resolution |
|----------|-------|----------|------------|
| Are seasonal or weekend rates needed later? | Manager | Before intent 005 | Pending |
