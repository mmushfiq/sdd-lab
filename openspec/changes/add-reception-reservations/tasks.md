# Tasks

## 1. Table

- [ ] 1.1 Create `src/main/resources/db/migration/V3__create_reservation.sql` with table `reservation` and its index
  (design.md, decision 1); verify `./mvnw compile` generates the jOOQ class `RESERVATION`
- [ ] 1.2 Add a `Clock` bean (system default zone) for "today"; verify `HrsApplicationTests.contextLoads` passes

## 2. Availability

- [ ] 2.1 Implement `AvailabilityService` in `ch.martinelli.edu.hrs.reservation` (overlap query, capacity filter,
  input validation, total price); verify with a service test for every scenario of `specs/availability/spec.md`
  (free room, overlap, departure day free, cancelled does not block, too small, none available, total price, past
  arrival, departure not after arrival, no guests)

## 3. Reservations

- [ ] 3.1 Implement `ReservationService` with create, change and cancel (room lock `FOR UPDATE`, overlap check,
  capacity check, saved nightly rate, reservation number generation with retry); verify with a service test for
  overlap rejected, back-to-back allowed, too many guests, rate kept after a room type rate change, rate taken over
  on a move to another room type, change creating an overlap, cancel frees the room, change/cancel of a non-Booked
  reservation rejected, and the reservation number format
- [ ] 3.2 Add a concurrency test: two threads book overlapping nights on room "104" at the same time; verify exactly
  one succeeds and the other gets "The room is no longer available for these dates" (scenario "Two bookings at the
  same moment")

## 4. Reservations view

- [ ] 4.1 Implement `ReservationView` (route `reservations`, Receptionist only, `@AccessDeniedErrorRouter`, in
  `StaffLayout`) with search field and grid; verify `./mvnw compile` succeeds
- [ ] 4.2 Implement `ReservationDialog` (availability search step, guest data step, edit mode, cancel with
  `ConfirmDialog`, read-only when not Booked); verify `./mvnw compile` succeeds
- [ ] 4.3 Write a Browserless test for `ReservationView` and `ReservationDialog`: create a reservation and see its
  number; missing name/email and invalid email are marked; search by "Meier"; only reservations with departure ≥
  today listed; change dates; cancel with and without confirmation; no change/cancel for a Cancelled reservation;
  "No rooms available for these dates"; `housekeeping` gets "Access denied"; verify `./mvnw test` passes

## 5. End-to-end

- [ ] 5.1 Write `ReceptionReservationIT` extending `PlaywrightIT`: log in as `reception`, create a reservation for
  room "104" for two nights from tomorrow, see it in the list, try a second overlapping reservation for room "104"
  and see that "104" is not offered, cancel the first and see "104" offered again; verify
  `./mvnw verify -Dit.test=ReceptionReservationIT` passes
- [ ] 5.2 Run `./mvnw verify`; verify the build ends green

## Workflow follow-up

- Run `/opsx:verify add-reception-reservations`, then `/opsx:archive add-reception-reservations`.
