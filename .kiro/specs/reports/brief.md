# Brief: reports

## Problem
The manager builds monthly occupancy and revenue figures by hand in spreadsheets.

## Current State
Reservations with statuses and prices (reservation-core); check-in and check-out data (reception).

## Desired Outcome
- The manager picks a period (for example a month) and sees the occupancy rate.
- The manager sees the revenue for the same period.
- Figures match the definitions recorded in `domain.md`.

## Approach
Manager-only read-only Vaadin views backed by aggregate jOOQ queries over reservations; no new tables.

## Scope
- **In**: occupancy report and revenue report for a chosen period, with monthly breakdown.
- **Out**: export, charts beyond simple tables unless the requirements ask for them, forecasting.

## Boundary Candidates
- Occupancy calculation
- Revenue calculation

## Out of Boundary
- Writing any data; pricing rules (reservation-core).

## Upstream / Downstream
- **Upstream**: reservation-core, reception, foundation
- **Downstream**: none

## Existing Spec Touchpoints
- **Extends**: none
- **Adjacent**: reservation-core (status and price definitions)

## Constraints
- Decide here: which nights count as occupied and which reservations count as revenue (open question in
  `domain.md`); record the answer in `domain.md`.
