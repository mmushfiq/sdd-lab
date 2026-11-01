# Tasks

## 1. Service

- [x] 1.1 Add `findForGuest` and `cancelByGuest` to `ReservationService` (design.md, decision 2); verify with a
  service test: found with matching number and email (also with other case and spaces), empty for unknown number and
  for wrong email, cancel before arrival works, cancel on the arrival day and after is rejected, cancel of a
  Cancelled reservation is rejected

## 2. Guest layout and booking view

- [x] 2.1 Implement `GuestLayout` and `BookingView` (route `""`, `@AnonymousAllowed`) with search, result cards,
  guest form with privacy notice and confirmation; verify `./mvnw compile` succeeds
- [x] 2.2 Write a Browserless test for `BookingView` without login: search shows free rooms with total price; past
  arrival and "No rooms available for these dates" messages; missing name/email and invalid email are marked;
  successful booking shows the reservation number; privacy notice text shown; when the room was booked meanwhile
  (insert an overlapping reservation before confirming) the message "The room is no longer available for these
  dates" appears and the results are shown again; verify `./mvnw test` passes

## 3. My reservation view

- [x] 3.1 Implement `MyReservationView` (route `my-reservation`, `@AnonymousAllowed`) with lookup form, details and
  cancel with `ConfirmDialog`; verify `./mvnw compile` succeeds
- [x] 3.2 Write a Browserless test for `MyReservationView` without login: found shows details; unknown number and
  wrong email both show "No reservation found for this reservation number and email" and no data; cancel with and
  without confirmation; Cancelled reservation has no cancel action; on the arrival day the message "Please contact
  the hotel to cancel on or after the arrival date" is shown and no cancel action (fixed `Clock`); verify
  `./mvnw test` passes

## 4. End-to-end

- [x] 4.1 Write `OnlineBookingIT` extending `PlaywrightIT`: without login open `/`, search two nights from tomorrow
  for 2 guests, book a room, read the reservation number, open "My reservation", find it with number and email,
  cancel it and see the status Cancelled; verify `./mvnw verify -Dit.test=OnlineBookingIT` passes
- [x] 4.2 Run `./mvnw verify`; verify the build ends green

## Workflow follow-up

- Run `/opsx:verify add-online-booking`, then `/opsx:archive add-online-booking`.
