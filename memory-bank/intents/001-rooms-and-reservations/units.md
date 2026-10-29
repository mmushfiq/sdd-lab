---
intent: 001-rooms-and-reservations
phase: inception
status: units-decomposed
updated: 2026-10-29T07:20:41Z
---

# Rooms and Reservations - Unit Decomposition

## Units Overview

This intent decomposes into 2 units of work. Each unit is **vertical**: domain logic, jOOQ data access, Flyway migrations, Vaadin views and tests together in one feature package.

> **Deviation from the full-stack-web default:** specsmd normally adds a separate `{intent}-ui` frontend unit. With Vaadin Flow the UI is server-side Java in the same feature package as its service, with no API between them, so a separate UI unit would split every story in two. See the decision in `inception-log.md`.

### Unit 1: 001-room-catalog

**Description**: Room types (name, capacity, nightly rate) and rooms (number, type, out-of-service flag), with their maintenance views.

**Stories**:

- 001-manage-room-types
- 002-delete-room-type
- 003-manage-rooms
- 004-set-room-out-of-service

**Deliverables**:

- Flyway migration for `room_type` and `room`, plus seed data for the ten rooms
- `roomcatalog` package: services, records, `RoomTypeView`, `RoomView`
- Browserless tests

**Dependencies**:

- Depends on: none
- Depended by: 002-reservation

**Estimated Complexity**: S

### Unit 2: 002-reservation

**Description**: Availability search, reservation lifecycle (create, change, cancel), reservation list, protection of rooms that have reservations, and guest data anonymization.

**Stories**:

- 001-search-availability
- 002-create-reservation
- 003-find-reservations
- 004-change-reservation
- 005-cancel-reservation
- 006-protect-rooms-with-reservations
- 007-anonymize-guest-data

**Deliverables**:

- Flyway migration for `reservation`
- `reservation` package: services, records, exceptions, `ReservationView` (search + book), `ReservationListView`
- Service integration tests for overlap and concurrency, Browserless tests for views, one Playwright IT for the booking flow

**Dependencies**:

- Depends on: 001-room-catalog
- Depended by: intents 002 (online booking), 003 (check-in/out), 005 (reports)

**Estimated Complexity**: M

## Requirement-to-Unit Mapping

- **FR-1** Manage room types → `001-room-catalog`
- **FR-2** Manage rooms → `001-room-catalog` (the two rules that need reservations, "cannot delete a room with reservations" and "cannot set out of service with future reservations", are delivered by `002-reservation` story 006, because reservations don't exist before that unit)
- **FR-3** Search availability → `002-reservation`
- **FR-4** Create a reservation → `002-reservation`
- **FR-5** Find and view reservations → `002-reservation`
- **FR-6** Change a reservation → `002-reservation`
- **FR-7** Cancel a reservation → `002-reservation`
- **FR-8** Anonymize guest data → `002-reservation`

## Unit Dependency Graph

```text
[001-room-catalog] ──> [002-reservation]
```

## Execution Order

1. 001-room-catalog (foundation: rooms must exist before they can be booked)
2. 002-reservation
