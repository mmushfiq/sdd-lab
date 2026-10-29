---
stage: plan
bolt: 004-guest-booking
created: 2026-10-29T12:34:03Z
---

## Implementation Plan: guest-booking

### Objective

A public guest area under `/booking`, without the staff navigation: guests search free room types, book one (the free room of that type with the lowest number is assigned), and later look up or cancel their reservation with booking number + email until 14:00 Europe/Zurich on the day before arrival. All booking and cancelling goes through the intent-001 `ReservationService`; no second booking implementation.

### Deliverables

New package `src/main/java/ch/martinelli/edu/hrs/guestbooking/`:

- `GuestBookingService.java` - guest operations on top of `ReservationService`:
  - `searchByRoomType(period, guests)` - calls `searchAvailability` and groups the rooms by room type
  - `bookRoomType(roomTypeName, period, guests, guest)` - tries `createReservation` for each free room of the type, lowest number first; returns the booked `GuestReservation`
  - `findByBookingNumberAndEmail(bookingNumber, email)` - trimmed, case-insensitive match, never matches anonymized reservations; returns `Optional<GuestReservation>`
  - `cancelByGuest(bookingNumber, email)` - re-checks match, status and deadline with the injected `Clock`, then calls `ReservationService.cancelReservation`
- `RoomTypeAvailability.java` - record: room type, capacity, nightly rate, total price, free rooms
- `GuestReservation.java` - record: booking number, arrival, departure, room type, guests, total price, status; plus `cancellationDeadline()` and `cancellableAt(Instant)`
- `RoomTypeNotAvailableException.java` - "This room type is no longer available for these dates"
- `CancellationDeadlinePassedException.java` - "Online cancellation is no longer possible. Please call the hotel."
- `GuestSearchView.java` - route `booking`, no `MainLayout`: search form, result grid by room type (no room numbers), "Book" per row, "No rooms available for these dates", link "View or cancel my reservation"
- `GuestBookingDialog.java` - name/email/phone + privacy note; confirm button disabled while saving; on success shows booking number, dates, room type, guests, total price and the "Please keep your booking number…" note
- `GuestReservationView.java` - route `booking/reservation`, no `MainLayout`: booking number + email lookup, details, "Cancel reservation" with confirmation, or the deadline message

Small changes outside the package:

- `HrsApplication.java` - a `Clock` bean in Europe/Zurich
- `reservation/GuestFields.java` - made `public` so the guest dialog reuses the same name/email/phone validation

### Dependencies

- `ReservationService` (intent 001): `searchAvailability`, `createReservation` (room row lock, ADR-001), `cancelReservation`
- `reservation` / `room` / `room_type` tables: read directly for the guest lookup (same approach as ADR-002)
- No new libraries, no migration

### Technical Approach

- **Public layout**: `@Route(value = "booking", autoLayout = false)` keeps the guest views out of `MainLayout`, so no staff links appear. `@AnonymousAllowed` follows with intent 004.
- **Search**: group `searchAvailability` results by room type name (unique) in Java; the free-room count is the group size. Reuses the intent-001 date, capacity, overlap and out-of-service rules.
- **Booking / lowest room**: `bookRoomType` is not `@Transactional` itself, so each `createReservation` attempt runs in its own locked transaction; a `RoomNotAvailableException` on one room just moves on to the next. If every room fails → `RoomTypeNotAvailableException`. Rooms come back sorted by room number (`searchAvailability` order).
- **Lookup**: one jOOQ query joining reservation → room → room_type, `upper(booking_number) = ?`, `lower(guest_email) = ?`, `anonymized = false`. Without the anonymized filter, an email of "anonymized" would match every anonymized reservation. The email is never logged.
- **Deadline**: `arrival − 1 day, 14:00 Europe/Zurich`; allowed while `now < deadline` (13:59:59 OK, 14:00:00 rejected). The view hides the cancel button using the same rule, and the service checks it again at confirm time.
- **Clock**: `GuestBookingService` takes a `Clock`; tests build it with `Clock.fixed(...)` around the real `ReservationService` and DSL.
- Validation messages, errors, logging and test style follow `coding-standards.md`.

### Acceptance Criteria

Story 001 - search rooms online
- [ ] `/booking` opens without login and shows arrival, departure, guests and a search button
- [ ] For 2 guests, each room type with capacity ≥ 2 and at least one free room is shown with capacity, nightly rate, total price and free-room count; no room numbers
- [ ] A fully booked "Double" is not listed
- [ ] Past arrival or departure not after arrival shows a date validation error
- [ ] Nothing free shows "No rooms available for these dates"
- [ ] The guest page has no links to staff views

Story 002 - book room online
- [ ] Booking a room type with name, email, phone stores a CONFIRMED reservation on the free room of that type with the lowest number, and shows booking number, dates, room type, guests, total price and the keep-your-booking-number note
- [ ] A missing or invalid field shows a field error and stores nothing
- [ ] If the last room of the type was taken after the search, "This room type is no longer available for these dates" is shown and nothing is stored
- [ ] The booking form shows the privacy note
- [ ] The online booking appears in the reception reservation list
- [ ] The confirm button is disabled while saving (no double booking on double click)

Story 003 - view own reservation
- [ ] Correct booking number and email (any case, booking number trimmed) show booking number, dates, room type, guests, total price and status
- [ ] Wrong booking number or wrong email shows "No reservation found for this booking number and email"
- [ ] An anonymized reservation shows the same generic message
- [ ] An empty field shows a required-field error

Story 004 - cancel own reservation
- [ ] Before 14:00 the day before arrival, cancel + confirm sets CANCELLED and the room is free again
- [ ] From 14:00 the day before arrival, no cancel action and "Online cancellation is no longer possible. Please call the hotel."
- [ ] A CANCELLED reservation shows no cancel action
- [ ] Choosing "No" in the confirmation changes nothing
- [ ] 13:59:59 allowed; page opened before the deadline but confirmed after → rejected with the deadline message (service-level, fixed clock)

### Test Approach (Stage 3)

- `GuestBookingServiceTest` (Spring context, H2, `@Transactional` where possible): grouping, lowest-room assignment, last-room race, lookup rules, deadline boundaries with fixed clocks.
- `GuestSearchViewTest`, `GuestReservationViewTest` (extend `HrsBrowserlessTest`): every UI criterion. Deadline UI cases use an arrival of today (deadline already passed) vs. far in the future, so they don't depend on the time of day.
- `GuestBookingIT` (extends `PlaywrightIT`): search → book → view → cancel.

### Known Limits

- `ReservationService` still uses `LocalDate.now()` (JVM zone) for "today". Only the guest code uses the Zurich `Clock`; changing intent-001 code is out of scope.
- "Lowest number" uses the existing string sort on room number, which is correct for the three-digit numbers 101-110.
