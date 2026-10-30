# Roadmap

## Overview
Build the Hotel Reservation System from `docs/vision.md`: one system for a ten-room hotel where guests book online
and staff (receptionist, manager, housekeeper) share one up-to-date view of every room's reservations, stays and
cleaning status. The stack is fixed (Java 25, Spring Boot 4, Vaadin 25 Flow, jOOQ, Flyway, H2; see `tech.md`).

The work is split into a foundation, a shared reservation core that owns the booking rules, and one vertical slice
per business area. Each slice delivers usable screens end to end (migration, jOOQ, service, Vaadin view, tests).

## Approach Decision
- **Chosen**: Foundation + reservation core + vertical slices (8 specs).
- **Why**: the no-double-booking rule is needed by both guest and front-desk booking, so it lives in one core spec
  instead of being built twice; every other spec is small enough to review in one sitting and maps to one package
  (`structure.md`); after the core, several slices can be specified and built in parallel.
- **Rejected alternatives**:
  - Three large specs (core-booking, operations, back-office): fewer approval rounds, but each spec exceeds 20
    tasks and mixes several roles, which makes requirements and reviews harder to judge.
  - Horizontal layers (data model, services, UI): no spec delivers value alone and every change crosses all three.

## Scope
- **In**: everything listed in scope in `docs/vision.md`: rooms, room types, rates; availability search; online
  booking, viewing and cancelling by guests; reservation management by reception; check-in and check-out; cleaning
  status and cleaning list; occupancy and revenue reports; login and role-based access for staff.
- **Out**: online payment, booking-channel integrations, extra-service billing, multi-property, loyalty programs.

## Constraints
- Single property with exactly ten rooms; no scaling work.
- Stack and conventions from `tech.md`, `structure.md`, `vaadin-jooq.md`, `testing.md`; vocabulary from `domain.md`.
- GDPR for guest personal data.
- Staff UI usable on a reception desktop and a housekeeping tablet.

## Boundary Strategy
- **Why this split**: each spec owns its tables and rules (one owner per table, `structure.md`); slices talk to the
  core and to each other only through services, so they can be built and reviewed independently.
- **Shared seams to watch**:
  - **Room data**: `foundation` creates the room and room type tables and seeds the ten rooms; `management` owns
    changing them afterwards. Other specs only read rooms.
  - **Reservation lifecycle**: `reservation-core` owns the reservation table, its statuses and every status change.
    `guest-booking`, `front-desk-reservations` and `reception` call its service; none writes reservations directly.
  - **Cleaning status**: `housekeeping` owns it. `reception` asks it whether a room is clean before check-in and
    tells it a room is dirty after check-out.
  - **Rates vs. existing reservations**: `reservation-core` decides whether a reservation keeps the price from booking
    time; `management` must respect that when rates change.
  - **Report definitions**: `reports` only reads; it depends on the statuses defined by `reservation-core` and
    `reception`.
  - **Open questions** in `domain.md` (guest access to own reservation, online cancel/change rules, no-show, rooms out
    of service, report definitions, guest data) are decided by the spec named in its brief.

## Reserved Numbers per Spec

Specs are written in parallel, so each one gets its own Flyway versions, test-data ID range and reservation date
window (`testing.md`). Tests run with "today" fixed at 2026-06-01 (foundation). A spec's test reservations stay inside
its date window, so availability and list tests of different specs never see each other's rows.

| Spec | Main migrations | Test migrations | Test data IDs | Test reservation dates |
|------|-----------------|-----------------|---------------|------------------------|
| foundation | V1–V9 | V100–V109 | 1000–1999 | none |
| reservation-core | V10–V19 | V110–V119 | 2000–2999 | 2027-02-01 – 2027-03-31, 2024-12 (retention) |
| housekeeping | V20–V29 | V120–V129 | 3000–3999 | none |
| guest-booking | V30–V39 | V130–V139 | 4000–4999 | 2027-04-01 – 2027-04-30 |
| front-desk-reservations | V40–V49 | V140–V149 | 5000–5999 | 2027-05-01 – 2027-05-31 |
| reception | V50–V59 | V150–V159 | 6000–6999 | 2026-05-25 – 2026-06-15 (around "today") |
| management | V60–V69 | V160–V169 | 7000–7999 | 2027-06-01 – 2027-06-30 |
| reports | V70–V79 | V170–V179 | 8000–8999 | 2026-01-01 – 2026-04-30 (past months) |

Reservations that departed more than 90 days before "today" lose their guest data to reservation-core's retention
job; tests that need guest data use dates after 2026-03-03.

## Specs (dependency order)
- [ ] foundation -- Rooms and room types schema with the ten seeded rooms, staff login with roles, staff layout and navigation. Dependencies: none
- [ ] reservation-core -- Reservation model and lifecycle, availability by date range and guests, create/change/cancel with no double booking. Dependencies: foundation
- [ ] housekeeping -- Cleaning status per room, housekeeper's cleaning list, mark room clean (tablet). Dependencies: foundation
- [ ] guest-booking -- Public availability search, online booking, guest views and cancels own reservation. Dependencies: reservation-core
- [ ] front-desk-reservations -- Receptionist creates, changes and cancels reservations for walk-in and phone guests. Dependencies: reservation-core
- [ ] reception -- Check-in (clean rooms only), check-out (room becomes dirty), room overview for reception. Dependencies: reservation-core, housekeeping
- [ ] management -- Manager maintains rooms, room types, capacities and nightly rates. Dependencies: foundation, reservation-core
- [ ] reports -- Occupancy and revenue reports for the manager by period. Dependencies: reservation-core, reception
