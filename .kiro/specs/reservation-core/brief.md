# Brief: reservation-core

## Problem
Paper lists cause double bookings and lost reservations. Guests and receptionists both create reservations, so the
rules must live in one place that both use.

## Current State
Rooms and room types exist (foundation). No reservation data or availability logic.

## Desired Outcome
- A reservation links a guest, one room and a date range (arrival inclusive, departure exclusive).
- Availability returns the rooms free for every night of a range that fit the number of guests.
- Creating or changing a reservation can never produce two reservations for the same room on a common night, even
  when two people book at the same moment.
- Status changes (reserved, cancelled, checked in, checked out) go only through this spec's service, with the allowed
  transitions enforced.
- The guest data a reservation needs is stored minimally (GDPR).

## Approach
`reservation` and guest tables owned here; a `ReservationService` with availability, create, change, cancel and the
lifecycle transitions other specs call; row locking for concurrent bookings. No UI of its own beyond what tests need.

## Scope
- **In**: reservation and guest data; availability query; create, change, cancel; lifecycle transitions and their
  rules; price of a reservation (nights x nightly rate) and whether it is fixed at booking time.
- **Out**: any screens (guest-booking, front-desk-reservations, reception); cleaning rules (housekeeping).

## Boundary Candidates
- Availability and double-booking protection
- Reservation lifecycle and transitions
- Guest personal data

## Out of Boundary
- How guests find their reservation online (guest-booking), check-in conditions such as clean rooms (reception).

## Upstream / Downstream
- **Upstream**: foundation
- **Downstream**: guest-booking, front-desk-reservations, reception, management, reports

## Existing Spec Touchpoints
- **Extends**: none
- **Adjacent**: management (rate changes vs. existing reservations), reports (reads statuses and prices)

## Constraints
- Decide here: exact status names, whether price is fixed at booking, which guest fields are required, retention of
  guest data (open questions in `domain.md`).
