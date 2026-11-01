# Design

## Context

See proposal.md for why and specs/ for the required behavior.

- `ReservationService.guestMayCancel` allows a guest cancel while `LocalDate.now(clock)` is before the arrival date.
  `MyReservationView` hides the cancel button and shows `CONTACT_HOTEL` otherwise.
- The application `Clock` bean is `Clock.systemDefaultZone()`, so "today" depends on the server's zone. Tests replace
  the clock.
- `reservation` (V3) stores `arrival_date` and `created_at`, nothing about cancellation.
- No mail support exists: no `spring-boot-starter-mail`, no mail settings.
- `BookingView.book()` calls `ReservationService.create` (transactional) and then shows the confirmation. Reception
  creates reservations through the same `create`.

## Goals / Non-Goals

**Goals:**
- One place that calculates a reservation's online cancellation deadline, used by the cancel check, the lookup page
  and the email.
- Existing reservations keep their deadline without the application knowing a go-live date.

**Non-Goals:**
- Retrying failed emails, email templates (HTML), emails for changes or cancellations.
- Making check-in time or the 48 hours configurable by staff.
- Changing the application `Clock` bean's zone (other features keep their current behavior).

## Decisions

### D1: Store the deadline rule as "hours before check-in" per reservation

Both rules are "N hours before check-in at 14:00 on the arrival date":
new rule N = 48 (14:00 two days before), old rule N = 14 (00:00 on the arrival date).

New column:

| Table         | Column                       | Type | Constraints                                   |
|---------------|------------------------------|------|-----------------------------------------------|
| `reservation` | `cancellation_hours_before`  | INT  | NOT NULL, DEFAULT 48, CHECK (value >= 0)      |

Migration `V6__add_reservation_cancellation_hours_before.sql`: add the column with DEFAULT 14 so every existing row
gets the old rule, then change the default to 48 so every row inserted afterwards gets the new rule. `create` sets 48
explicitly, because jOOQ does not read database defaults back after an insert and the returned record must carry the
value for the confirmation email; the database default stays as a safety net. `change` does not touch the column, so
a moved reservation keeps its rule and the deadline follows the new arrival date (reservations spec).

Alternatives:
- Store the deadline timestamp: `change` would have to know which rule the row was created with, so the rule is
  needed anyway.
- Compare `created_at` with a go-live constant: needs the exact deployment time in code or config, and `created_at`
  is in server time.
- A rule enum (`OLD`/`NEW`): same column count, but needs a mapping to times in code; the number is the rule.

### D2: Calculate in hotel time Europe/Zurich with wall-clock arithmetic

`deadline = arrivalDate.atTime(14:00).minusHours(cancellationHoursBefore)` as a `LocalDateTime` in Europe/Zurich.
"Now" is `LocalDateTime.now(clock.withZone(ZoneId.of("Europe/Zurich")))`. Guest may cancel while
`status == BOOKED && now.isBefore(deadline)`.

Wall-clock arithmetic keeps the spec's "14:00 two days before" also across a daylight saving switch (then the real
gap is 47 or 49 hours). The zone is applied only here, not to the `Clock` bean.

Code: `ReservationService.onlineCancellationDeadline(ReservationRecord)` returns the `LocalDateTime`;
`guestMayCancel` uses it. Constants `CHECK_IN_TIME`, `HOTEL_ZONE` live in `ReservationService`.

### D3: One display format

`"Free online cancellation until " + d MMMM yyyy, HH:mm (Locale.ENGLISH) + " (Zurich time)"`, e.g.
"Free online cancellation until 8 March 2030, 14:00 (Zurich time)". A static
`ReservationService.cancellationText(LocalDateTime)` builds it for the lookup page and the email.
`CONTACT_HOTEL` becomes "The online cancellation deadline has passed. Please contact the hotel to cancel."

`MyReservationView.show` adds a paragraph with id `cancellation-deadline` showing that text when the cancel button is
shown.

### D4: Confirmation email after the booking transaction, failures caught

- Add `spring-boot-starter-mail`; send a plain-text `SimpleMailMessage` through `JavaMailSender`.
- New `booking/BookingConfirmationEmail` service with `send(ReservationRecord, String roomTypeName)`. It catches
  `MailException`, logs a warning and returns.
- `BookingView.book()` calls it after `create` returned and before `showConfirmation`. The booking is already
  committed, so a mail failure can never undo it. Reception does not call it, so reception bookings send no email.
- Settings in `application.properties`: `spring.mail.host` / `spring.mail.port` (local default `localhost:25`),
  `hrs.mail.from`, and connect/read timeouts of 5 s
  (`spring.mail.properties.mail.smtp.connectiontimeout`, `.timeout`, `.writetimeout`).

Alternatives:
- Send inside `ReservationService.create`: a mail failure would roll back the booking (spec forbids), and reception
  bookings would send mail too.
- `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`: does not block the guest, but adds an event type and async
  config for a ten-room hotel; not needed now.

### D5: Tests

- Service tests use the existing test `Clock` to check both rules, the 14:00 / 00:00 boundaries and a changed
  arrival date.
- Tests replace `JavaMailSender` with `@MockitoBean` and verify the sent `SimpleMailMessage` (recipient, content), and
  make it throw `MailSendException` for the failure scenario.
- Existing guest-cancel tests that cancel the day before arrival must set `cancellation_hours_before = 14` on their
  reservation or move to an earlier date.

## Risks / Trade-offs

- [Mail server slow or down blocks the booking page] → 5 s timeouts; booking is already saved; failure is logged.
- [A failed email is lost, the guest has no email] → the confirmation page and the lookup page still show the
  reservation number and the deadline; retry is a non-goal.
- [Rows created between migration and app start get the old rule] → Flyway runs at application start, so there is no
  such window.
- [Server zone differs from Zurich] → deadline uses Europe/Zurich explicitly; "today" in other features is unchanged.

## Migration Plan

1. Deploy: Flyway runs V6 at start-up; existing reservations get 14, new ones 48.
2. Configure `spring.mail.host`, `spring.mail.port`, credentials and `hrs.mail.from` for production.
3. Rollback: deploy the previous version; it ignores the new column. Do not drop the column, or existing
   reservations lose their rule when rolling forward again.

## Open Questions

- Production mail server and sender address (`hrs.mail.from`): configuration only, no change to the approach.
