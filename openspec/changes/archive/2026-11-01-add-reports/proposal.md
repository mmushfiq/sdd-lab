# Proposal

## Why

The manager works out occupancy and revenue by hand in spreadsheets today. The vision's success measure is that
the manager sees monthly occupancy and revenue without manual spreadsheet work.

## What Changes

- The manager opens a report for a year with one row per month: occupied room-nights, occupancy in percent and
  revenue in CHF, plus a total for the year.
- Only stays that were checked in count (Checked in or Checked out). Booked, cancelled and never checked-in
  reservations count neither as occupancy nor as revenue.
- Revenue uses the nightly rate saved on each reservation, split by the month of each night.

## Capabilities

### New Capabilities

- `reports`: monthly occupancy and revenue for the manager.

### Modified Capabilities

None.

## Impact

- New report service and a manager view; no database change.
- Reads `reservation` (statuses from `add-check-in-out`, saved nightly rate from `add-reception-reservations`) and
  the number of rooms from `room`.
