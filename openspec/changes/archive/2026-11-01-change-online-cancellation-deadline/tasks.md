# Tasks

## 1. Data model

- [x] 1.1 Add `src/main/resources/db/migration/V6__add_reservation_cancellation_hours_before.sql`: column
  `cancellation_hours_before INT NOT NULL DEFAULT 14 CHECK (cancellation_hours_before >= 0)`, then set its default to
  48 (design D1); verify `./mvnw compile` succeeds and the generated `ReservationRecord` has
  `getCancellationHoursBefore()`

## 2. Online cancellation deadline rule

- [x] 2.1 In `ReservationService` add `HOTEL_ZONE` (Europe/Zurich), `CHECK_IN_TIME` (14:00),
  `onlineCancellationDeadline(ReservationRecord)` and static `cancellationText(LocalDateTime)` (design D2, D3); change
  `guestMayCancel` to `BOOKED && now in Zurich is before the deadline`; verify with 2.3
- [x] 2.2 Change `CONTACT_HOTEL` to "The online cancellation deadline has passed. Please contact the hotel to cancel.";
  verify with 2.3
- [x] 2.3 In `ReservationServiceTest` add tests for: a new reservation gets 48 and deadline 8 March 2030 14:00 for
  arrival 10 March; a reservation with 14 has deadline 10 March 2030 00:00; guest may cancel one minute before and not
  at the deadline for both rules; `cancelByGuest` after the deadline but before arrival throws the new message;
  `change` to arrival 20 March keeps 48 (18 March 14:00) and keeps 14 (20 March 00:00); `cancellationText` returns
  "Free online cancellation until 8 March 2030, 14:00 (Zurich time)"; fix existing tests that cancel the day before
  arrival; verify `./mvnw test -Dtest=ReservationServiceTest` is green

## 3. Lookup page shows the deadline

- [x] 3.1 In `MyReservationView.show` add a paragraph with id `cancellation-deadline` and the `cancellationText` of the
  deadline when the cancel button is shown; keep the `CONTACT_HOTEL` message for a Booked reservation past its
  deadline; verify with 3.2
- [x] 3.2 In `MyReservationViewTest` (Browserless) cover: deadline text shown for a Booked reservation before its
  deadline; no cancel button and the new message after the deadline but before arrival; old-rule reservation can
  still be cancelled on the day before arrival at 20:00; cancelled reservation shows no deadline; update existing
  assertions on the old message; verify `./mvnw test -Dtest=MyReservationViewTest` is green

## 4. Booking confirmation email

- [x] 4.1 Add `spring-boot-starter-mail` to `pom.xml` and the mail settings (`spring.mail.host`, `spring.mail.port`,
  `hrs.mail.from`, 5 s timeouts) to `application.properties` (design D4); verify `./mvnw compile` succeeds and the
  application context starts in an existing `@SpringBootTest`
- [x] 4.2 Add `booking/BookingConfirmationEmail` that sends a plain-text `SimpleMailMessage` to the guest with
  reservation number, room type, arrival and departure date, number of guests, total price and the `cancellationText`,
  and catches `MailException` with a logged warning; verify with 4.4
- [x] 4.3 Call `BookingConfirmationEmail.send` in `BookingView.book()` after `create` returns and before
  `showConfirmation`; reception views do not call it; verify with 4.4
- [x] 4.4 In `BookingViewTest` (Browserless) with `@MockitoBean JavaMailSender` cover: booking sends one email to the
  guest's address containing the reservation number, "Double", the dates, 2 guests, the total price and
  "Free online cancellation until 8 March 2030, 14:00 (Zurich time)"; when `send` throws `MailSendException` the
  reservation is saved and the confirmation is shown; in `ReservationViewTest` creating a reservation by reception
  sends no email; verify `./mvnw test -Dtest=BookingViewTest,ReservationViewTest` is green

## 5. Integration

- [x] 5.1 Extend `OnlineBookingIT` (Playwright): book a room, find it on the lookup page and see the
  `cancellation-deadline` text; verify the test passes in `./mvnw verify`
- [x] 5.2 Run `./mvnw verify`; verify it ends with BUILD SUCCESS and no failing tests

## Workflow follow-up

- Run `/opsx:verify change-online-cancellation-deadline` and fix findings.
- Archive with `/opsx:archive change-online-cancellation-deadline` after review; the delta specs then merge into
  `openspec/specs/online-booking` and `openspec/specs/reservations`.
