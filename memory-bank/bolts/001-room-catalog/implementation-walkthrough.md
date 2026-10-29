---
stage: implement
bolt: 001-room-catalog
created: 2026-10-29T10:31:32Z
---

## Implementation Walkthrough: room-catalog

### Summary

Room types and rooms are stored in two new tables, seeded with three placeholder types and rooms 101-110. A single service holds all room catalog queries and rules, and two Vaadin views (room types, rooms) let the manager list, create, edit and delete them, including the out-of-service flag. A shared main layout provides side navigation.

### Structure Overview

Feature package `roomcatalog` contains the service, the two views, a list record for the room grid and one exception per rule the UI reacts to. Views only call the service; the service runs jOOQ queries against the generated classes. Field rules (required, ranges, decimals, length, trimming) live in each view's Binder; uniqueness, "in use" and "no longer exists" are checked in the service, with database constraints as the safety net.

### Completed Work

- [x] `src/main/resources/db/migration/V1__create_room_catalog.sql` - creates `room_type` and `room` with check, unique and foreign key constraints; seeds Single/Double/Family and rooms 101-110
- [x] `src/main/java/ch/martinelli/edu/hrs/roomcatalog/RoomCatalogService.java` - list, save and delete room types and rooms; `findBookableRooms(minCapacity)` for the reservation unit
- [x] `src/main/java/ch/martinelli/edu/hrs/roomcatalog/RoomListItem.java` - room joined with its type for the room grid and bookable rooms
- [x] `src/main/java/ch/martinelli/edu/hrs/roomcatalog/RoomTypeView.java` - route `room-types`: grid sorted by name, form, save/new/delete with confirmation
- [x] `src/main/java/ch/martinelli/edu/hrs/roomcatalog/RoomView.java` - route `rooms`: grid sorted by number with type, capacity, rate and state; form with room type combo box and out-of-service checkbox
- [x] `src/main/java/ch/martinelli/edu/hrs/roomcatalog/RoomTypeNameExistsException.java` - duplicate room type name (shown as field error)
- [x] `src/main/java/ch/martinelli/edu/hrs/roomcatalog/RoomNumberExistsException.java` - duplicate room number (shown as field error)
- [x] `src/main/java/ch/martinelli/edu/hrs/roomcatalog/RoomTypeInUseException.java` - room type still used by rooms (notification)
- [x] `src/main/java/ch/martinelli/edu/hrs/roomcatalog/RoomTypeNotFoundException.java` - room type deleted meanwhile (notification, list refresh)
- [x] `src/main/java/ch/martinelli/edu/hrs/core/ui/MainLayout.java` - app shell with side navigation to Rooms and Room types

### Key Decisions

- **Generated lower-case name column**: H2 has no expression index, so a generated `name_lower` column carries the case-insensitive unique constraint.
- **Forms write into a fresh record**: a failed save never changes the item shown in the grid; the id of the selected row is set before saving.
- **Insert vs update by id**: the service inserts when the id is null and updates otherwise, so views never need attached jOOQ records.

### Deviations from Plan

None.

### Dependencies Added

None.

### Developer Notes

- Never set `name_lower` on a `RoomTypeRecord`; the database computes it.
- Room numbers sort as text, which matches the current three-digit numbers.
- No route is mapped to `/` yet.
