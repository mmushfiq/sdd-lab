# Design

## Context

Builds on `add-staff-login` (roles, `StaffLayout` access filtering) and `add-room-management` (tables `room_type`,
`room`, ten seeded rooms, `StaffLayout`, migration `V2`). Requirements are in `specs/availability/spec.md` and
`specs/reservations/spec.md`.

## Goals / Non-Goals

**Goals:**

- One availability query and one booking service that `add-online-booking` reuses unchanged.
- No double booking, also under concurrent saves, on H2.

**Non-Goals:**

- Several rooms in one reservation, deposits, cancellation fees (payment is out of scope).
- Check-in and check-out statuses (added by `add-check-in-out`).
- Deleting guest data after the stay (GDPR retention); see Open Questions.

## Decisions

### 1. Table `reservation` (migration `V3__create_reservation.sql`)

| Column               | Type            | Constraints                                                    |
|----------------------|-----------------|----------------------------------------------------------------|
| `id`                 | `BIGINT`        | primary key, generated identity                                |
| `reservation_number` | `CHAR(8)`       | not null, unique                                               |
| `room_id`            | `BIGINT`        | not null, foreign key to `room(id)`                            |
| `arrival_date`       | `DATE`          | not null                                                       |
| `departure_date`     | `DATE`          | not null, check `departure_date > arrival_date`                |
| `number_of_guests`   | `INT`           | not null, check `number_of_guests >= 1`                        |
| `guest_name`         | `VARCHAR(100)`  | not null                                                       |
| `guest_email`        | `VARCHAR(254)`  | not null                                                       |
| `guest_phone`        | `VARCHAR(30)`   | null                                                           |
| `nightly_rate`       | `DECIMAL(10,2)` | not null, check `nightly_rate > 0`                             |
| `status`             | `VARCHAR(20)`   | not null, constraint `reservation_status_ck` check `status IN ('BOOKED','CANCELLED')` |
| `created_at`         | `TIMESTAMP`     | not null, default `CURRENT_TIMESTAMP`                          |

Index on `(room_id, arrival_date)` for the overlap query. Only the statuses this change needs are allowed;
`add-check-in-out` widens the check in its own migration.

The nightly rate (not the total) is saved, so `add-reports` can split revenue by month per night.

### 2. Overlap rule as one query

Two stays overlap when `a.arrival_date < b.departure_date AND b.arrival_date < a.departure_date`, counting only
reservations with `status <> 'CANCELLED'`. `AvailabilityService` uses the same condition with `NOT EXISTS` to list
free rooms joined with `room_type` (capacity ≥ guests), sorted by room number.

### 3. No double booking under concurrency: lock the room row

H2 has no exclusion constraint. `ReservationService.create/change` runs in one transaction: `SELECT ... FROM room
WHERE id = ? FOR UPDATE`, then the overlap check (excluding the reservation itself on change), then insert/update.
A second transaction booking the same room waits for the lock and then sees the first booking. Alternatives:
optimistic retry (more code), `SERIALIZABLE` isolation (H2 support is limited). Rejected for that reason.

### 4. Reservation number

`SecureRandom`, 8 characters from `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (32 characters, ~10^12 combinations). On the
rare unique-constraint violation the service generates a new number and retries (max. 3 times).

### 5. Validation

Binder validators on the guest form (required name and email, `EmailValidator`), as in `add-room-management`; the
project has no Bean Validation dependency. The service checks guests (at least 1), dates (arrival not before today,
departure after arrival), a non-blank guest name and email (so `add-online-booking` can reuse it), capacity and
overlap. Service errors are a `ReservationException` with the spec's message text: search errors and "No rooms
available for these dates" appear in the dialog, save errors in an error `Notification`.

### 6. Services, views and packages

| Class                  | Package                                      | Route          | Access |
|------------------------|----------------------------------------------|----------------|--------|
| `AvailabilityService`  | `ch.martinelli.edu.hrs.reservation`          | –              | –      |
| `ReservationService`   | `ch.martinelli.edu.hrs.reservation`          | –              | –      |
| `ReservationView`      | `ch.martinelli.edu.hrs.reservation.ui`       | `reservations` | `@RolesAllowed("RECEPTIONIST")`, `@AccessDeniedErrorRouter` |
| `ReservationDialog`    | `ch.martinelli.edu.hrs.reservation.ui`       | –              | –      |

- `ReservationView` (in `StaffLayout`): search field (number or name), grid of reservations with departure ≥ today
  sorted by arrival date, button "New reservation".
- `ReservationDialog`: step 1 availability search (arrival, departure `DatePicker`, guests `IntegerField`) with a
  grid of free rooms and total prices; step 2 guest data form. The same dialog edits a Booked reservation
  (pre-filled, search excludes the reservation itself) and has "Cancel reservation" with a `ConfirmDialog`. For
  non-Booked reservations it is read-only.
- Today comes from an injected `Clock` bean, so tests can fix the date.

## Risks / Trade-offs

- [Room lock serialises bookings of one room] → Ten rooms and a handful of users; waiting is milliseconds.
- [Reservation number guessed] → 32^8 combinations and lookup also needs the email (`add-online-booking`).
- [Guest data kept forever] → See Open Questions.

## Migration Plan

New migration `V3`; no existing data.

## Open Questions

- How long guest data is kept after the stay (GDPR retention) — can be added later as a deletion job without
  changing these requirements.
