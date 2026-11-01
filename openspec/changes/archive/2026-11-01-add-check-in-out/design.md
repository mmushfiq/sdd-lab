# Design

## Context

Builds on `add-reception-reservations` (table `reservation` with constraint `reservation_status_ck`,
`ReservationService`, `Clock` bean, `ReservationView`) and `add-housekeeping` (`room.cleaning_status`,
`CleaningListView`). Migrations `V1`–`V4` exist. Requirements are in `specs/check-in-out/spec.md`.

## Goals / Non-Goals

**Goals:**

- Check-in and check-out in one click plus confirmation from one overview page (vision: under 1 minute).
- Check-out and "room dirty" in one transaction, so the cleaning list can never miss a departure.

**Non-Goals:**

- No-show handling, late check-out fees, extending a stay at check-out (reception changes the reservation first).
- Undoing a check-in or check-out.

## Decisions

### 1. New statuses (migration `V5__add_reservation_check_in_out_status.sql`)

```sql
ALTER TABLE reservation DROP CONSTRAINT reservation_status_ck;
ALTER TABLE reservation ADD CONSTRAINT reservation_status_ck
    CHECK (status IN ('BOOKED', 'CANCELLED', 'CHECKED_IN', 'CHECKED_OUT'));
```

No new columns. The overlap rule (`status <> 'CANCELLED'`) already blocks the room for Checked in and Checked out
stays, so availability needs no change.

### 2. Service `FrontDeskService` in `ch.martinelli.edu.hrs.frontdesk`

- `roomOverview()` (today from the injected `Clock`): all rooms (join `room_type`) left-joined with the reservation
  that is `CHECKED_IN`, or else `BOOKED` with `arrival_date = today`; state Occupied / Arrival today / Free.
- `checkIn(reservationId)`, one transaction with the room row locked (`FOR UPDATE`, as in `ReservationService`):
  status must be `BOOKED`, `arrival_date = today`, `cleaning_status = 'CLEAN'`, and no `CHECKED_IN` reservation
  on the room; then set `CHECKED_IN`. Errors use the spec's messages.
- `checkOut(reservationId)`, one transaction: status must be `CHECKED_IN`; set `CHECKED_OUT`, set
  `departure_date = max(arrival_date + 1, min(today, departure_date))`, and set the room's `cleaning_status` to
  `DIRTY`.

Shortening `departure_date` on early check-out frees the remaining nights through the existing overlap rule and
gives `add-reports` the nights actually stayed. Alternative: a separate `checked_out_on` column. Rejected: two
dates to keep consistent, and availability would need to know about it.

### 3. View

| Class          | Package                                | Route        | Access |
|----------------|----------------------------------------|--------------|--------|
| `FrontDeskView`| `ch.martinelli.edu.hrs.frontdesk.ui`   | `front-desk` | `@RolesAllowed("RECEPTIONIST")`, `@AccessDeniedErrorRouter` |

- `Grid` of ten rooms: room number, room type, state badge, cleaning status badge, guest name, departure date and
  one action button: "Check in" (only for Arrival today) or "Check out" (only for Occupied), each with a
  `ConfirmDialog`. Check-in on a dirty or still occupied room shows the error in a `Notification`.
- `ReservationView` shows the new statuses as "Checked in" and "Checked out" (display mapping only).

## Risks / Trade-offs

- [Guest arrives a day late] → Check-in is only offered on the arrival date; reception first changes the arrival
  date of the reservation, then checks in.
- [Previous guest departing today not checked out yet] → Check-in refused with "The room is still occupied";
  reception checks the previous guest out first.

## Migration Plan

New migration `V5`; existing rows keep their statuses.
