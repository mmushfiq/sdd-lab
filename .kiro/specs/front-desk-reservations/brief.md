# Brief: front-desk-reservations

## Problem
Walk-in and phone guests are handled on paper, so reservations get lost or double-booked.

## Current State
Reservation rules exist in reservation-core; there are no staff screens for reservations.

## Desired Outcome
- A receptionist searches availability, creates a reservation for a walk-in or phone guest, and finds existing
  reservations by guest or date.
- The receptionist changes dates, room or number of guests of a reservation, and cancels reservations.
- Every change obeys the same availability rules as online booking.

## Approach
Receptionist-only Vaadin views in the staff layout on top of the `ReservationService`: reservation search/list,
create, change, cancel with confirmation.

## Scope
- **In**: find reservations; create; change; cancel; reservations made online are visible and manageable here too.
- **Out**: check-in and check-out (reception), room overview (reception), guest self-service (guest-booking).

## Boundary Candidates
- Reservation search and list for staff
- Create and change forms for staff

## Out of Boundary
- Availability rules (reservation-core), stay handling (reception).

## Upstream / Downstream
- **Upstream**: reservation-core, foundation (staff layout, receptionist role)
- **Downstream**: reception (may link from a reservation to check-in)

## Existing Spec Touchpoints
- **Extends**: none
- **Adjacent**: guest-booking (same core), reception (shares reservation detail views)

## Constraints
- Desktop at the reception desk; fast keyboard-friendly forms.
- Decide here whether the manager may also manage reservations.
