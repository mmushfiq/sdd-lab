# Proposal

## Why

Too many guests cancel online the day before they arrive, and the hotel cannot resell the room in time. The online
cancellation deadline moves earlier to 48 hours before check-in, while reservations booked before this change keep the
deadline they were given. Guests also cannot see when their deadline is, so the system shows it to them.

## What Changes

- Check-in time is 14:00, hotel time Europe/Zurich. The online cancellation deadline of a new reservation is 48 hours
  before check-in, i.e. 14:00 two days before the arrival date.
- Reservations that exist when this change goes live keep their current deadline: the end of the day before the
  arrival date (midnight before arrival).
- A reservation keeps its deadline rule when the receptionist changes its dates; the deadline moves with the new
  arrival date.
- **BREAKING**: a guest whose deadline has passed can no longer cancel online, even before the arrival date. The
  message changes from "Please contact the hotel to cancel on or after the arrival date" to "The online cancellation
  deadline has passed. Please contact the hotel to cancel."
- The page to find a reservation shows the exact last online cancellation time of a Booked reservation.
- New: after an online booking the system sends the guest a confirmation email with the reservation details and the
  exact last online cancellation time. A failed email does not undo the booking.
- Cancellation by the receptionist is not affected.

## Capabilities

### New Capabilities

_None._ The confirmation email is part of online booking and is added as a new requirement to `online-booking`.

### Modified Capabilities

- `online-booking`: guest cancellation follows the reservation's online cancellation deadline instead of the arrival
  date (modified); finding a reservation shows the last online cancellation time (modified); the online cancellation
  deadline rule and the booking confirmation email are new (added).
- `reservations`: changing a reservation's dates keeps its deadline rule and recalculates the deadline from the new
  arrival date (added).

## Impact

- Database: new Flyway migration on `reservation` to record each reservation's deadline rule; existing rows keep the
  old rule.
- Code: `ReservationService` (`guestMayCancel`, `cancelByGuest`, create and change), `booking/ui/MyReservationView`,
  `booking/ui/BookingView`.
- Dependency: `spring-boot-starter-mail` for sending the confirmation email; mail server settings in
  `application.properties`.
- Tests: existing guest cancellation tests change (new message, deadline instead of arrival date); new tests for the
  deadline display, the old-rule reservations and the email.
