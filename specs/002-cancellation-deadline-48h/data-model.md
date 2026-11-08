# Data Model: Online Cancellation Deadline 48 Hours

Changes to the 001 data model only (see `specs/001-hotel-reservation-system/data-model.md`).

## Reservation (changed)

| Field | Type | Rule |
|---|---|---|
| `cancellation_hours_before` | int, not null | Hours before check-in (arrival date, 15:00 Europe/Zurich) until which the Guest may cancel online. `15` for reservations that existed before this feature (= 00:00 on the arrival date, 001 FR-015), `48` for every reservation booked afterwards, online or by reception. Set once at booking; never changed by `change()`. |

Migration `V4__reservation_cancellation_hours.sql` (new file, V3 stays untouched):

1. Add the column with default `15`, not null: every existing row gets the old rule.
2. Set the column default to `48`.

`ReservationService.book()` also sets `48` explicitly, so the rule does not depend on the database default.

## CancellationDeadline (derived, not stored)

| Part | Rule |
|---|---|
| `deadline` | `arrival_date` at 15:00 minus `cancellation_hours_before` hours, wall-clock, Europe/Zurich |
| online cancel allowed | status is `BOOKED` and now (Europe/Zurich) is before `deadline` |
| `text()` | `Free online cancellation until 10 March 2030, 15:00 (Zurich time)` |
| `passedText()` | `Online cancellation was possible until 10 March 2030, 15:00 (Zurich time). Please call reception to cancel.` |

Examples (arrival 12 March 2030):

| Rule | Deadline | Cancel at 10 March 14:59 | at 10 March 15:00 | at 11 March 23:59 |
|---|---|---|---|---|
| 48 (new) | 10 March 2030, 15:00 | allowed | rejected | rejected |
| 15 (old) | 12 March 2030, 00:00 | allowed | allowed | allowed |

A reception date change from arrival 12 March to 20 March moves a 48-hour deadline to 18 March 2030, 15:00
(FR-008).

## GuestReservation (changed)

Adds `CancellationDeadline cancellationDeadline`; `cancellable` stays and means "booked and the deadline has
not passed". No other guest data is added (001 FR-024).

## State transitions

Unchanged from 001. The deadline only limits the Guest's online `BOOKED → CANCELLED`; the Receptionist's
cancellation has no deadline (FR-007).
