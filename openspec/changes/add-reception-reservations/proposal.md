# Proposal

## Why

Reservations are kept on paper lists today, which causes double bookings and lost reservations. Reception needs to
find free rooms for a date range and record, change and cancel reservations for walk-in and phone guests, with the
system guaranteeing that a room is never reserved twice for overlapping nights.

## What Changes

- Availability search: for an arrival date, a departure date and a number of guests, the system lists the rooms
  that are free for every night and large enough, with nightly rate and total price.
- The receptionist creates a reservation for one room with the guest's name, email and optional phone number.
- Each reservation gets a unique, hard-to-guess reservation number.
- The reservation keeps the nightly rate valid at booking time; later rate changes do not change it.
- No double bookings: two reservations that are not cancelled never overlap on the same room. The departure day is
  free for the next guest.
- The receptionist lists reservations, changes dates, room or number of guests, and cancels reservations, only
  while a reservation is Booked.

## Capabilities

### New Capabilities

- `availability`: finding the rooms that are free for a date range and fit the number of guests.
- `reservations`: creating, changing and cancelling reservations, the reservation number, the saved nightly rate and
  the no-double-booking rule.

### Modified Capabilities

None.

## Impact

- New Flyway migration with table `reservation`.
- New services for availability and reservations; new receptionist view for reservations.
- Depends on `add-staff-login` (Receptionist role) and `add-room-management` (rooms, room types).
- `add-online-booking`, `add-check-in-out` and `add-reports` build on these capabilities.
