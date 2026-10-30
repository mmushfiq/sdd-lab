# Brief: reception

## Problem
Check-in and check-out are slow and error-prone, guests are checked in to rooms that aren't clean, and reception has
no single view of all rooms.

## Current State
Reservations and their lifecycle exist (reservation-core); cleaning status exists (housekeeping).

## Desired Outcome
- The receptionist checks a guest in only if the reserved room is clean; otherwise the check-in is refused with a
  clear reason.
- The receptionist checks a guest out; the room becomes dirty and appears on the cleaning list at once.
- Each check-in or check-out takes under 1 minute.
- A room overview shows, for every room, who has booked it, who is staying in it and whether it is clean.

## Approach
Receptionist views in the staff layout: today's arrivals and departures, check-in, check-out, room overview. Status
changes go through `ReservationService`; cleaning status through the housekeeping service.

## Scope
- **In**: arrivals and departures for a day; check-in with clean check; check-out marking the room dirty; room
  overview of all ten rooms.
- **Out**: creating or changing reservations (front-desk-reservations), marking rooms clean (housekeeping), payment
  (out of scope).

## Boundary Candidates
- Check-in and check-out flows
- Room overview

## Out of Boundary
- Lifecycle rules (reservation-core), cleaning list (housekeeping).

## Upstream / Downstream
- **Upstream**: reservation-core, housekeeping, foundation
- **Downstream**: reports

## Existing Spec Touchpoints
- **Extends**: none
- **Adjacent**: front-desk-reservations (shared reservation detail), housekeeping

## Constraints
- Decide here: no-show handling, early or late check-in and check-out, moving a guest to another room at check-in
  (open questions in `domain.md`).
