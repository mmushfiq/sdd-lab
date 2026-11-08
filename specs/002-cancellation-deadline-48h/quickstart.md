# Quickstart: Validate Online Cancellation Deadline 48 Hours

## Prerequisites

- Java 25; run from the repository root.
- For the email by hand: a local SMTP catcher on `localhost:25` (e.g. Mailpit with `--smtp 0.0.0.0:25`),
  or set `spring.mail.port` to its port.

## Automated

```bash
./mvnw compile   # runs V4 and regenerates jOOQ (new column cancellation_hours_before)
./mvnw test      # CancellationDeadline unit tests, service tests, Browserless view tests
./mvnw verify    # plus Playwright ITs; the feature is done only when this is green
```

## By hand

1. Start the app (`./mvnw spring-boot:run`). Existing reservations in `./data/hrs` get the old rule.
2. **New booking, 48 hours (US1)**: on `/` book a room arriving in 5 days. The email arrives with
   "Free online cancellation until <arrival − 2 days>, 15:00 (Zurich time)" (US4,
   `contracts/confirmation-email.md`).
3. On `/my-reservation` look it up: the same text and a cancel button (US3). Cancel: succeeds (US1/AC1).
4. Book a room arriving tomorrow, look it up: "Online cancellation was possible until … Please call
   reception to cancel." and no button (US1/AC3, US3/AC2).
5. **Old booking (US2)**: a reservation created before step 1 that arrives tomorrow still shows a deadline
   of tomorrow 00:00 and can be cancelled online.
6. **Reception (FR-009)**: log in as Receptionist, find the reservations on `reception/reservations`; the
   "Online cancellation" column shows the same times. Change the arrival of the 5-day booking by a week:
   its deadline moves a week (FR-008).
7. **Mail down (US4/AC2)**: stop the SMTP catcher and book again: the confirmation shows, the reservation
   exists, a warning is logged.

Data model and texts: `data-model.md`. Screen changes: `contracts/ui-changes.md`.
