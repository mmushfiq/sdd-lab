# Proposal

## Why

Check-in and check-out are tracked on paper today, so reception does not know at a glance which rooms are occupied,
and housekeeping learns about departures by phone. The vision requires that a guest is only checked in to a clean
room, that check-in or check-out takes under a minute, and that a room appears on the cleaning list right after
check-out.

## What Changes

- Reception sees a room overview: every room with its state (free, arrival today, occupied), cleaning status and
  the current or arriving guest.
- The receptionist checks a guest in on the arrival date, only if the room is clean and no longer occupied. The
  reservation becomes Checked in.
- The receptionist checks a guest out. The reservation becomes Checked out and the room becomes dirty at once, so it
  appears on the cleaning list.
- An early check-out shortens the stay to the actual nights; the remaining nights are free again.
- Reservations get two new statuses: Checked in and Checked out.

## Capabilities

### New Capabilities

- `check-in-out`: the reception room overview, check-in and check-out.

### Modified Capabilities

None. Change and cancel already apply only to Booked reservations (`reservations`), and `housekeeping` already
defines the cleaning status and list this change uses.

## Impact

- New Flyway migration allowing the reservation statuses `CHECKED_IN` and `CHECKED_OUT`.
- New service and a receptionist view (room overview with check-in and check-out).
- Depends on `add-reception-reservations` and `add-housekeeping`; `add-reports` counts the stays it produces.
