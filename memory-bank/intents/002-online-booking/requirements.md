---
intent: 002-online-booking
phase: inception
status: complete
created: '2026-10-29T07:58:01Z'
updated: '2026-10-29T07:58:01Z'
---

# Requirements: Online Booking

## Intent Overview

Guests search for free rooms, book a room, and view or cancel their own reservation on the hotel's website without calling the hotel. Builds on the reservation domain of intent 001 (same availability and double-booking rules).

Source: `docs/hotel-requirements.md`.

## Business Goals

| Goal | Success Metric | Priority |
|------|----------------|----------|
| Guests book without calling | ≥ 50 % of reservations made online within six months | Must |
| Fast online booking | A guest completes a booking in under 3 minutes | Must |
| Fewer phone calls for cancellations | Guests cancel online within the deadline | Should |

---

## Functional Requirements

### FR-1: Search rooms online
- **Description**: A guest searches free rooms for arrival date, departure date and number of guests on a public page.
- **Acceptance Criteria**:
  - Same rules as intent 001 FR-3 (dates, capacity, no overlap, out-of-service rooms hidden, back-to-back allowed).
  - Results show room type, capacity, nightly rate and total price; room numbers are not shown to guests.
  - Rooms of the same type are shown once per type with the number of free rooms of that type.
- **Priority**: Must
- **Related Stories**: 001-search-rooms-online

### FR-2: Book a room online
- **Description**: A guest books one room of a chosen room type by entering name, email and phone.
- **Acceptance Criteria**:
  - Same validation and double-booking protection as intent 001 FR-4.
  - The system assigns any free room of the chosen type.
  - After saving, the guest sees booking number, dates, room type, number of guests and total price, with the note "Please keep your booking number; you need it with your email to view or cancel your reservation."
  - If the last room of that type was taken meanwhile, the guest sees "This room type is no longer available for these dates" and nothing is stored.
- **Priority**: Must
- **Related Stories**: 002-book-room-online

### FR-3: View own reservation
- **Description**: A guest views their reservation by entering booking number and email.
- **Acceptance Criteria**:
  - Booking number and email are both required; matching is case-insensitive.
  - On a match, the guest sees booking number, dates, room type, number of guests, total price and status.
  - On no match (either value wrong, or reservation anonymized) the guest sees one generic message "No reservation found for this booking number and email" without revealing which value was wrong.
- **Priority**: Must
- **Related Stories**: 003-view-own-reservation

### FR-4: Cancel own reservation
- **Description**: A guest cancels their own CONFIRMED reservation online until 14:00 on the day before arrival (hotel time).
- **Acceptance Criteria**:
  - Cancelling requires a confirmation.
  - Before the deadline: status becomes CANCELLED and the room is free again.
  - After the deadline: the cancel action is not offered and the guest sees "Online cancellation is no longer possible. Please call the hotel."
  - Cancelled reservations cannot be cancelled again.
- **Priority**: Must
- **Related Stories**: 004-cancel-own-reservation

### FR-5: Public guest area
- **Description**: All guest pages are under `/booking` and are reachable without login, also after intent 004 adds staff login. Guest pages never show other guests' data or staff functions.
- **Acceptance Criteria**:
  - `/booking` is the entry page with search; view/cancel is reachable from it.
  - No guest page links to staff views.
- **Priority**: Must
- **Related Stories**: 001-search-rooms-online

---

## Non-Functional Requirements

### Performance
| Requirement | Metric | Target |
|-------------|--------|--------|
| Search and booking | Time until the page updates | < 1 s |

### Security
| Requirement | Standard | Notes |
|-------------|----------|-------|
| Reservation lookup | Booking number + email | Booking numbers are random (32^8 combinations); generic error message on mismatch |
| Public pages | No login | No staff data or functions reachable from `/booking` |

### Usability
| Requirement | Metric | Target |
|-------------|--------|--------|
| Online booking | Time for a first-time guest | < 3 minutes |
| Devices | Supported | Desktop, tablet and phone browsers |

### Compliance
| Requirement | Standard | Notes |
|-------------|----------|-------|
| Data minimization | GDPR | Only name, email, phone collected; a short privacy note on the booking form |

---

## Constraints

### Technical Constraints

**Project-wide standards**: loaded from memory-bank standards folder by Construction Agent

**Intent-specific constraints**:
- "Today" and the 14:00 deadline use the hotel's time zone (Europe/Zurich).
- Reuse the reservation services of intent 001; no second booking implementation.

### Business Constraints
- No online payment; payment at the hotel.
- No confirmation email in this intent.

---

## Assumptions

| Assumption | Risk if Invalid | Mitigation |
|------------|-----------------|------------|
| Guests note their booking number from the screen | Guests lose it and call the hotel | Reception can look it up by name (intent 001); add email confirmation later |
| Standard check-in time is 14:00 | Wrong cancellation deadline | Check-in time is one constant |
| No bot protection needed for a ten-room hotel | Spam bookings block rooms | Add CAPTCHA later if it happens |

---

## Open Questions

| Question | Owner | Due Date | Resolution |
|----------|-------|----------|------------|
| Should guests get a confirmation email later? | Manager | After go-live | Pending |
