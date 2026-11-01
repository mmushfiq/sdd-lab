# Proposal

## Why

Every later feature (availability search, reservations, check-in, cleaning, reports) needs the hotel's rooms, their
room types, capacities and nightly rates. Today this information lives on paper. The manager needs one place to
maintain it before anything can be booked.

## What Changes

- The manager maintains room types: name, capacity (number of guests) and nightly rate.
- The manager maintains rooms: room number and room type. Capacity and nightly rate come from the room type.
- Room numbers and room type names are unique.
- Rooms and room types can be created and changed, but not deleted (rooms and types will be referenced by
  reservations; the hotel has a fixed set of ten rooms).
- The system is installed with the hotel's ten rooms and their room types, so it is usable right away.
- Staff get a navigation menu that shows only the pages their role may open.

## Capabilities

### New Capabilities

- `room-management`: room types (name, capacity, nightly rate) and rooms (room number, room type) maintained by the
  manager.

### Modified Capabilities

None.

## Impact

- New Flyway migration with tables `room_type` and `room` and the seeded ten rooms.
- New manager views for room types and rooms; a staff layout with role-filtered navigation, which the staff start
  page from `add-staff-login` moves into.
- Depends on `add-staff-login` (Manager role, access annotations).
