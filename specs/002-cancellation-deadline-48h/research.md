# Research: Online Cancellation Deadline 48 Hours

All Technical Context items were known from the code; no NEEDS CLARIFICATION remained. The decisions below
settle how the spec is met in the existing code.

## R1: Store the rule per reservation, not the deadline

- **Decision**: New column `reservation.cancellation_hours_before` (int, not null). Existing rows get `15`,
  new bookings get `48`. The deadline is computed: arrival date at 15:00 minus that many hours.
- **Rationale**: 15 hours before 15:00 on the arrival date is 00:00 on the arrival date, which is exactly
  the 001 rule ("until the day before arrival", FR-003). When reception changes the dates (FR-008), the
  deadline follows the new arrival date and keeps the rule of the original booking without extra code.
  The application does not need to know the go-live date: the migration marks every row that exists
  before it runs.
- **Alternatives considered**:
  - Store the deadline timestamp: must be recalculated on every date change, and the rule it came from is
    lost.
  - Compare `created_at` with a go-live date in configuration: adds a setting that must never change and
    is easy to get wrong in tests.
  - A rule enum (`OLD`/`NEW`): the same as hours, but the number is self-explaining in the formula.

## R2: Wall-clock calculation in Europe/Zurich

- **Decision**: `deadline = arrivalDate.atTime(15:00).minusHours(hoursBefore)` as a `LocalDateTime` in
  Europe/Zurich. "Now" for the cancel check is `LocalDateTime.now(clock.withZone(Europe/Zurich))`; online
  cancellation is allowed while `now` is before the deadline (exclusive, spec edge case).
- **Rationale**: The spec states the deadline as a wall-clock time ("15:00 two days before arrival",
  "00:00 on the arrival date"). Wall-clock arithmetic keeps it at 15:00 / 00:00 even when a daylight
  saving change falls in between; exact-duration arithmetic would show 14:00 or 16:00.
- **Alternatives considered**: `ZonedDateTime.minusHours` (exact 48 h): breaks the stated wall-clock time
  on DST weekends. Changing the `Clock` bean to Zurich: would change "today" for every other feature.

## R3: One place for the deadline and its texts

- **Decision**: A small `reservation/CancellationDeadline` record (`of(arrivalDate, hoursBefore)`,
  `allowsOnlineCancellationAt(now)`, `text()`, `passedText()`), used by the cancel check, the guest lookup,
  the reception grid and the email.
- **Rationale**: FR-005 requires the same text on three screens/channels; one formatter
  (`d MMMM yyyy, HH:mm`, `Locale.ENGLISH`) prevents drift. It is pure (no Spring, explicit `now`), so the
  15:00 / 00:00 boundaries and DST are unit-tested without a fixed clock (the test context uses the real
  system clock).
- **Alternatives considered**: Static helpers in `ReservationService`: the service already has many duties;
  a view could not use them without pulling in the service.

## R4: Confirmation email

- **Decision**: Add `spring-boot-starter-mail`. New `reservation/BookingConfirmationEmail` service sends a
  plain-text `SimpleMailMessage` through `JavaMailSender`. `GuestBookingView` calls it after
  `ReservationService.book` returned (transaction committed) and before showing the confirmation. It catches
  `MailException`, logs a warning and returns. Reception bookings (`ReservationView`) do not call it.
- **Settings**: `spring.mail.host`, `spring.mail.port` (local default `localhost:25`), `hrs.mail.from`,
  connect/read/write timeouts of 5 s in `application.properties`; the test `application.properties` sets
  the same host so a `JavaMailSender` bean exists in every test context.
- **Rationale**: Sending outside the booking transaction means a mail failure can never undo the booking
  (FR-006). Same approach as the OpenSpec change `change-online-cancellation-deadline` (commit `a269e17`).
- **Alternatives considered**: Sending inside `book()`: a failure would roll back the booking and
  reception bookings would send mail. `@TransactionalEventListener` + `@Async`: does not block the guest,
  but adds events and async config for a ten-room hotel. HTML templates, retries: not required (spec
  Assumptions).

## R5: Existing tests

- **Decision**: Tests that cancel online "the day before arrival" with a reservation booked through
  `book()` now get the 48-hour rule. They either book further ahead or set
  `cancellation_hours_before = 15` on the row to test the old rule.
- **Affected**: `GuestReservationServiceTest`, `MyReservationViewTest`, `GuestCancelIT`; messages asserted
  there change to the new texts. Tests that book online (`GuestBookingViewTest`, `GuestBookingIT`) replace
  `JavaMailSender` with `@MockitoBean` where they check the email; elsewhere the send fails against
  `localhost` and is only logged.
