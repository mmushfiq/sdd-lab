# Brief: guest-booking

## Problem
Guests have to phone the hotel to book, check or cancel a reservation.

## Current State
Reservation rules and availability exist in reservation-core; there are no public screens.

## Desired Outcome
- A guest searches available rooms for a date range and number of guests without logging in.
- The guest books a room by entering the needed personal data and gets a confirmation they can use later.
- The guest finds their own reservation again, sees its details and can cancel it.
- A booking takes under 3 minutes.

## Approach
Public (`@AnonymousAllowed`) Vaadin views on top of the `ReservationService`: search, booking form, confirmation,
look-up and cancel.

## Scope
- **In**: public search, booking, confirmation, guest look-up of own reservation, guest cancellation.
- **Out**: payment (out of scope), e-mail delivery unless the requirements ask for it, changing a reservation online
  (decide in requirements), staff screens.

## Boundary Candidates
- Public search and booking flow
- Guest self-service access to an existing reservation

## Out of Boundary
- Availability and double-booking rules (reservation-core).

## Upstream / Downstream
- **Upstream**: reservation-core, foundation (public entry page)
- **Downstream**: none

## Existing Spec Touchpoints
- **Extends**: none
- **Adjacent**: front-desk-reservations (same core, different actor)

## Constraints
- Decide here: how a guest identifies their reservation without an account; whether and until when online cancel is
  allowed (open questions in `domain.md`).
- No guest account; collect only the data a reservation needs.
