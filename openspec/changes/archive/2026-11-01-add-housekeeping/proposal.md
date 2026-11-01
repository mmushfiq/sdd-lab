# Proposal

## Why

Guests sometimes wait for rooms that are not yet clean, because housekeeping and reception coordinate by phone.
Housekeeping needs one list of the rooms that need cleaning, on a tablet, and a way to mark a room as cleaned so
reception can give it to the next guest. Check-in (next change) relies on this cleaning status.

## What Changes

- Every room has a cleaning status: clean or dirty. Rooms start clean.
- Housekeeping sees a cleaning list of all dirty rooms, sorted by room number.
- Housekeeping marks a room as cleaned; it leaves the cleaning list at once.
- The cleaning list is usable on a tablet.

## Capabilities

### New Capabilities

- `housekeeping`: the room cleaning status (clean, dirty), the cleaning list and marking rooms as cleaned.

### Modified Capabilities

None.

## Impact

- New Flyway migration adding the cleaning status to table `room`.
- New Housekeeping view (cleaning list) in the staff layout.
- `add-check-in-out` sets rooms to dirty on check-out and requires clean rooms for check-in.
