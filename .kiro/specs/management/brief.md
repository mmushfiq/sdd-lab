# Brief: management

## Problem
The manager cannot maintain rooms, room types and rates in the system; today the seeded data can only change through
migrations.

## Current State
Room and room type tables with seed data (foundation); reservations reference rooms (reservation-core).

## Desired Outcome
- The manager creates and edits room types (name, capacity, nightly rate).
- The manager creates and edits rooms (number, room type).
- Changes never break existing reservations: no room or room type in use disappears, and rate changes follow the
  pricing rule defined by reservation-core.

## Approach
Manager-only Vaadin views in the staff layout with grid + form for room types and rooms; a management service that
owns writes to the room and room type tables.

## Scope
- **In**: room type and room maintenance; nightly rate changes; validation (unique room number, capacity > 0, rate
  > 0).
- **Out**: seasonal rates, staff account maintenance, reports.

## Boundary Candidates
- Room type maintenance
- Room maintenance

## Out of Boundary
- Pricing of existing reservations (reservation-core).

## Upstream / Downstream
- **Upstream**: foundation, reservation-core
- **Downstream**: none

## Existing Spec Touchpoints
- **Extends**: none (takes over writes to tables that foundation created)
- **Adjacent**: reservation-core (rate and room changes vs. reservations)

## Constraints
- The hotel has ten rooms; decide here whether rooms can be taken out of service instead of deleted (open question in
  `domain.md`).
