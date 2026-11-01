# Design

## Context

Builds on `add-reception-reservations`: `AvailabilityService`, `ReservationService` (room lock, overlap check,
reservation number, `ReservationException` with message texts), the `Clock` bean and table `reservation`. Guest
pages are public via `@AnonymousAllowed` (`add-staff-login`). Requirements are in `specs/online-booking/spec.md`.

## Goals / Non-Goals

**Goals:**

- Guest booking reuses the reception booking path, so the no-double-booking rule exists exactly once.
- A booking in a few steps on one page, usable on a phone.

**Non-Goals:**

- Email confirmation, guest accounts, payment (out of scope per vision and planning).
- Changing a reservation by the guest (only reception changes reservations).
- Rate limiting of the lookup page (see Risks).

## Decisions

### 1. No new table

The guest booking is a normal `reservation` row. Nothing marks it as online-made: no requirement needs the
difference. Alternative: a `source` column for the "half of all reservations online" success measure. Rejected for
now: no report asks for it; a later change can add it.

### 2. Service additions in `ReservationService`

- `findForGuest(reservationNumber, email)`: `upper(trim(number))` against `reservation_number` and
  `lower(trim(email))` against `lower(guest_email)`; returns `Optional`. Both "number unknown" and "email wrong"
  give an empty result, so the page cannot tell them apart.
- `cancelByGuest(reservationNumber, email)`: loads with `findForGuest`, rejects unless status is `BOOKED` and
  `today < arrival_date`, then reuses the existing cancel. The rule is checked in the service, not only by hiding
  the button.

### 3. Views (package `ch.martinelli.edu.hrs.booking.ui`)

| Class                  | Route            | Access              |
|------------------------|------------------|---------------------|
| `GuestLayout`          | layout           | –                   |
| `BookingView`          | `""`             | `@AnonymousAllowed` |
| `MyReservationView`    | `my-reservation` | `@AnonymousAllowed` |

- `GuestLayout`: hotel name header with links "Book a room" and "My reservation"; no staff navigation.
- `BookingView`: search form (two `DatePicker`, guests `IntegerField`), result cards with room type, capacity,
  nightly rate and total price, then a guest form (name, email, phone, privacy notice) and a "Book" button, then
  the confirmation with the reservation number. Kept in one view with three states; no new routes, so a page reload
  starts over (acceptable for a 3-minute flow).
- `MyReservationView`: reservation number and email fields, "Find" button, details panel, "Cancel reservation"
  with `ConfirmDialog`.
- The guest sees the room type, not the room number, in results and confirmation; the reservation still holds a
  concrete room.

### 4. Room taken during booking

`ReservationService.create` already rejects an overlap with "The room is no longer available for these dates";
`BookingView` shows it and re-runs the search with the same input.

## Risks / Trade-offs

- [Guessing reservation numbers on the lookup page] → 32^8 numbers and the matching email are both needed; rate
  limiting can be added later if logs show abuse.
- [Guest loses the reservation number] → Reception finds the reservation by guest name.

## Migration Plan

No database migration. Deploy with the application.
