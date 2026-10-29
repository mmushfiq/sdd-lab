---
stage: plan
bolt: 001-room-catalog
created: 2026-10-29T10:24:21Z
---

## Implementation Plan: room-catalog

### Objective

The manager can maintain room types (name, capacity, nightly rate) and rooms (number, type, out-of-service flag) in two Vaadin views. Three placeholder room types and rooms 101-110 are seeded. The reservation unit gets a `findBookableRooms(minCapacity)` service method.

### Deliverables

- `src/main/resources/db/migration/V1__create_room_catalog.sql`
  - `room_type`: id (identity), name VARCHAR(50) NOT NULL, capacity INT CHECK 1-10, nightly_rate DECIMAL(10,2) CHECK > 0, case-insensitive unique name (H2 has no expression index, so a generated `lower(name)` column with a unique constraint)
  - `room`: id (identity), number VARCHAR(10) NOT NULL UNIQUE, room_type_id FK → room_type, out_of_service BOOLEAN NOT NULL DEFAULT FALSE
  - Seed: Single (1, 120.00), Double (2, 150.00), Family (4, 220.00); rooms 101-103 Single, 104-108 Double, 109-110 Family
- `roomcatalog/RoomCatalogService.java` - jOOQ queries: `findRoomTypes`, `saveRoomType`, `deleteRoomType`, `findRooms`, `saveRoom`, `deleteRoom`, `findBookableRooms(minCapacity)`
- `roomcatalog/RoomListItem.java` - record for the room grid (room joined with its type: number, type name, capacity, rate, out of service)
- Business exceptions (one per rule the UI reacts to): `RoomTypeNameExistsException`, `RoomNumberExistsException`, `RoomTypeInUseException`, `RoomTypeNotFoundException`
- `roomcatalog/RoomTypeView.java` - route `room-types`: Grid sorted by name + form (name, capacity, rate), New / Save / Delete with confirm dialog
- `roomcatalog/RoomView.java` - route `rooms`: Grid sorted by number (number, type, capacity, rate, out of service) + form (number, type combo box, out-of-service checkbox), New / Save / Delete with confirm dialog
- `core/ui/MainLayout.java` - `AppLayout` with side navigation to the two views (needed to move between them; later intents add their entries)

### Dependencies

- Vaadin Flow (Grid, Binder, BigDecimalField, IntegerField, ComboBox, ConfirmDialog) - already in `pom.xml`
- jOOQ code generation from the Flyway migration - already wired in `pom.xml`
- No new dependencies

### Technical Approach

- Schema first: write V1, run `./mvnw compile` to generate jOOQ classes, then the service and views.
- Forms bind the generated jOOQ records (`RoomTypeRecord`, `RoomRecord`) with `Binder`; field rules live in the Binder:
  - name required, trimmed; capacity 1-10; rate > 0 with at most 2 decimals
  - room number required, trimmed, max 10 characters; room type required
- Uniqueness and "in use" are checked in the service and raised as the exceptions above; the DB constraints are the safety net. Name/number duplicates show as a field error on the form, the others as a `Notification`.
- `deleteRoomType` on an id that no longer exists throws `RoomTypeNotFoundException`; the view shows "Room type no longer exists" and refreshes the grid.
- `findBookableRooms(minCapacity)` returns rooms with `out_of_service = false` and capacity ≥ minCapacity, sorted by number.
- Reservation-dependent rules (block delete / out-of-service while reservations exist) are left to `002-reservation` story 006.

### Acceptance Criteria

Story 001 - manage room types
- [ ] Room type list shows name, capacity, nightly rate, sorted by name
- [ ] Saving "Double", 2, 150.00 adds it to the list
- [ ] Saving "double" when "Double" exists shows field error "Name already exists", nothing saved
- [ ] Capacity 0 or 11 shows a capacity validation error
- [ ] Rate 0, negative, or more than two decimals shows a rate validation error
- [ ] Changing a type's rate and saving shows the new rate
- [ ] Leading/trailing spaces in the name are trimmed; empty name shows a required error

Story 002 - delete room type
- [ ] Deleting an unused type after confirming removes it
- [ ] Deleting a type used by rooms shows "Room type is used by rooms and cannot be deleted"; it stays
- [ ] Cancelling the confirmation deletes nothing
- [ ] Deleting a type removed meanwhile shows "Room type no longer exists" and refreshes the list

Story 003 - manage rooms
- [ ] Room list shows number, type, capacity, rate, out-of-service state, sorted by number
- [ ] Saving room "111" with type "Double" adds it to the list
- [ ] Saving a second "101" shows "Room number already exists", nothing saved
- [ ] Saving without a room type shows a required error
- [ ] Changing a room's type and saving shows the new type
- [ ] Deleting a room after confirming removes it
- [ ] Room number is trimmed; more than 10 characters shows a validation error

Story 004 - out of service
- [ ] Setting a room out of service and saving shows it as out of service
- [ ] Setting it back in service and saving shows it as in service
- [ ] `findBookableRooms` excludes out-of-service rooms; returns empty when all are out of service

### Test Approach (Stage 3)

- `RoomTypeViewTest`, `RoomViewTest` (extend `HrsBrowserlessTest`) cover every UI criterion above against the real Spring context and H2.
- `RoomCatalogServiceTest` covers `findBookableRooms` and the "deleted meanwhile" case.
- Tests run `@Transactional` so seeded data is restored after each test.
