# Product Overview

The Hotel Reservation System (HRS) runs a single small hotel with ten rooms. It replaces paper lists and phone calls
with one shared, up-to-date view of every room: who booked it, who is staying in it, and whether it is clean. Guests
book online; staff manage reservations, stays and cleaning in the same system.

Source of truth for scope and goals: `docs/vision.md`.

## Core Capabilities

- **Availability and booking**: search rooms by date range and number of guests; guests book, view and cancel their
  own reservation online; reception creates, changes and cancels reservations for walk-in and phone guests.
- **Stay handling**: reception checks guests in and out and sees the state of all rooms at a glance.
- **Housekeeping**: rooms are dirty or clean; checked-out rooms appear on the cleaning list at once and are marked
  clean by housekeeping.
- **Administration and reporting**: the manager maintains rooms, room types and nightly rates and sees occupancy and
  revenue reports.
- **Access control**: login with roles for staff (receptionist, manager, housekeeping); guests book without a staff
  account.

## Target Use Cases

| Actor         | Main scenarios                                                       | Device               |
|---------------|----------------------------------------------------------------------|----------------------|
| Guest         | Search availability, book, view or cancel own reservation            | Any browser          |
| Receptionist  | Manage reservations, check in, check out, room overview              | Desktop at reception |
| Manager       | Rooms, room types, rates, occupancy and revenue reports              | Desktop              |
| Housekeeping  | Cleaning list, mark room clean                                       | Tablet               |

## Business Invariants

These rules come from the vision's goals and must hold in every feature, not just the one that introduces them:

- **No double bookings**: a room is never reserved by two guests for overlapping nights.
- **Clean before check-in**: a guest can only be checked in to a room marked clean.
- **Immediate cleaning list**: after check-out, the room shows up on housekeeping's list without delay.

## Value Proposition

- Removes double bookings and lost reservations caused by paper and phone.
- Lets guests self-serve (target: at least half of reservations online within six months; booking in under
  3 minutes).
- Keeps reception fast (check-in or check-out in under 1 minute).
- Gives the manager monthly occupancy and revenue without spreadsheets.

## Boundaries

- One property, ten rooms. Do not design for multi-hotel, scaling or high concurrency beyond a small staff.
- Out of scope: online payment (settled at the hotel), booking-channel integrations, extra-service billing, loyalty
  programs.
- Guest personal data is subject to GDPR: collect only what a reservation needs and keep it out of logs.

---
_Focus on patterns and purpose, not exhaustive feature lists_
