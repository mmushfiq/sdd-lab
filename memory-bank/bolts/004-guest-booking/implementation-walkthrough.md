---
stage: implement
bolt: 004-guest-booking
created: 2026-10-29T12:50:00Z
---

## Implementation Walkthrough: guest-booking

### Summary

A public guest area under `/booking` lets guests search free room types, book one, and look up or cancel their reservation with booking number and email. A new `guestbooking` package sits on top of the intent-001 `ReservationService`; booking and cancelling reuse its locking and rules, and only the guest lookup reads the tables directly.

### Structure Overview

Two views (`booking`, `booking/reservation`) opt out of the staff `MainLayout`, so no staff navigation is reachable. They call `GuestBookingService`, which groups availability by room type, books the lowest-numbered free room of a type, matches booking number + email, and enforces the online cancellation deadline with an injected `Clock`. A shared read-only details component shows a reservation after booking and on lookup.

### Completed Work

- [x] `src/main/java/ch/martinelli/edu/hrs/guestbooking/GuestBookingService.java` - search by room type, book a room type, guest lookup, cancellation deadline check and guest cancel
- [x] `src/main/java/ch/martinelli/edu/hrs/guestbooking/RoomTypeAvailability.java` - a bookable room type with prices and free-room count, no room numbers
- [x] `src/main/java/ch/martinelli/edu/hrs/guestbooking/GuestReservation.java` - the guest's view of a reservation and its cancellation deadline (14:00 Europe/Zurich the day before arrival)
- [x] `src/main/java/ch/martinelli/edu/hrs/guestbooking/RoomTypeNotAvailableException.java` - no room of the chosen type left
- [x] `src/main/java/ch/martinelli/edu/hrs/guestbooking/CancellationDeadlinePassedException.java` - online cancellation no longer allowed
- [x] `src/main/java/ch/martinelli/edu/hrs/guestbooking/GuestSearchView.java` - public `/booking` page: search form, room-type grid, empty message, link to the reservation lookup
- [x] `src/main/java/ch/martinelli/edu/hrs/guestbooking/GuestBookingDialog.java` - guest data with privacy note, double-click protection, confirmation with booking number and keep-it note
- [x] `src/main/java/ch/martinelli/edu/hrs/guestbooking/GuestReservationView.java` - public `/booking/reservation` page: lookup, details, cancel with Yes/No confirmation or the deadline message
- [x] `src/main/java/ch/martinelli/edu/hrs/guestbooking/ReservationDetails.java` - read-only reservation details shared by the dialog and the lookup view
- [x] `src/main/java/ch/martinelli/edu/hrs/HrsApplication.java` - `Clock` bean in Europe/Zurich
- [x] `src/main/java/ch/martinelli/edu/hrs/reservation/GuestFields.java` - made public so the guest dialog reuses the name/email/phone validation

### Key Decisions

- **No layout on guest routes**: `autoLayout = false` keeps the staff side navigation off guest pages without a second layout class.
- **Booking not transactional at the guest level**: each room attempt is its own locked transaction in `ReservationService`; a single outer transaction would be marked rollback-only by the first "room taken" failure.
- **Anonymized reservations excluded in the lookup query**: anonymizing writes "Anonymized" into the email, which would otherwise be a matching value.
- **Email lookup field is plain text**: an email format error would reveal which of the two values is wrong.
- **Search refreshes whenever the booking dialog closes**: free-room counts are current after a booking, a conflict or a cancel.

### Deviations from Plan

- Added `ReservationDetails` (shared details component) and `GuestBookingService.canCancel` so the lookup view needs no `Clock` of its own. Otherwise none.

### Dependencies Added

None.

### Developer Notes

- `ReservationService` still uses the JVM zone for "today"; only the guest code uses the Zurich `Clock`. Around midnight the two can differ by a day if the JVM is not on Zurich time.
- Service tests can build `GuestBookingService` with `Clock.fixed(...)` around the real `ReservationService` and `DSLContext`.
- `@AnonymousAllowed` on both guest views is due with intent 004.
