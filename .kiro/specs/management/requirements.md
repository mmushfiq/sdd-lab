# Requirements Document

## Project Description (Input)
The manager cannot maintain rooms, room types and rates in the Hotel Reservation System; today the seeded data
(foundation: three room types, ten rooms) can only change through migrations, while reservations (reservation-core)
refer to rooms. This feature lets the manager create and edit room types (name, capacity, nightly rate) and rooms
(room number, room type) on manager-only staff pages, with validation (unique room number, capacity > 0, nightly rate
> 0). Changes must never break existing reservations: no room or room type in use disappears, and nightly rate changes
follow reservation-core's pricing rule (rate fixed at booking). It decides whether rooms can be taken out of service
instead of deleted (open question in `domain.md`). Out of scope: seasonal rates, staff account maintenance, reports,
pricing of existing reservations (reservation-core). Source: `.kiro/specs/management/brief.md`.

## Introduction
Management gives the manager two staff pages to maintain the hotel's room types and rooms. Room types carry the
capacity and nightly rate that availability and new reservations use; rooms carry the room number guests and staff
see. Every change is checked so that existing reservations keep their room, their booked nightly rate and a room that
fits their number of guests.

## Boundary Context
- **In scope**: listing, creating, editing and removing room types (name, capacity, nightly rate) and rooms (room
  number, room type); validation of these values; protecting existing reservations from room and room type changes;
  restricting these pages to the manager.
- **Out of scope**: seasonal or date-dependent rates; the price of existing reservations and every reservation
  change (reservation-core); cleaning status (housekeeping); staff account maintenance (foundation: provisioned per
  environment); reports; the guest, front-desk and reception pages.
- **Adjacent expectations**: reservation-core keeps the booked nightly rate of existing reservations and takes the
  current nightly rate of a room type for new reservations and for moves to another room type; availability and new
  reservations use the capacity and nightly rate currently stored. Housekeeping treats a room that has no recorded
  cleaning status as Dirty, so a newly created room appears on the cleaning list until a housekeeper marks it clean.
  The foundation's staff layout lists the manager's pages in its navigation.

## Assumptions and Decisions
These were left open by the brief, `domain.md` and the roadmap and are decided here; changing one requires revisiting
this spec.

- **M1 No out-of-service state; removal only when unused**: rooms and room types cannot be taken out of service. A room
  can be removed only if it never had any reservation (in any status, because reservations are kept forever); a room
  type can be removed only if no room has it. A room that was ever reserved stays in the system. Answers the open
  question "rooms out of service" in `domain.md`.
- **M2 Rate changes**: a new nightly rate applies to reservations created afterwards and to reservations moved to a
  room of that type afterwards; every existing reservation keeps its booked nightly rate (reservation-core D2).
- **M3 Capacity protection**: a capacity reduction or a change of a room's room type is refused if a Reserved or
  Checked-in reservation of an affected room has more guests than the new capacity. Cancelled and Checked-out
  reservations are not considered.
- **M4 Room number format**: 1 to 10 letters and digits, surrounding spaces ignored, stored in upper case; two room
  numbers that differ only in upper/lower case are the same room number.
- **M5 Value limits**: room type name 1 to 50 characters, unique ignoring upper/lower case and surrounding spaces;
  capacity a whole number from 1 to 99; nightly rate greater than 0.00, less than 100000.00, with at most two decimal
  places.
- **M6 Number of rooms**: the HRS does not limit the number of rooms; the hotel's ten rooms are the seeded starting
  point.
- **M7 New room cleaning status**: management does not record a cleaning status for a new room; housekeeping's rule
  (no recorded status = Dirty) applies.

## Requirements

### Requirement 1: Room type list
**Objective:** As a manager, I want to see all room types with their capacity and nightly rate, so that I know what
guests can currently book.

#### Acceptance Criteria
1. When the manager opens the room types page, the HRS shall list every room type with its name, capacity, nightly rate and the number of rooms that have it, ordered by name.
2. When the manager selects a room type in the list, the HRS shall show its name, capacity and nightly rate in an edit form.

### Requirement 2: Creating and editing room types
**Objective:** As a manager, I want to create room types and change their name, capacity and nightly rate, so that the
system matches the hotel's offer.

#### Acceptance Criteria
1. When the manager saves a new room type with a valid name, capacity and nightly rate, the HRS shall store it, show it in the room type list and show a confirmation naming the room type.
2. When the manager saves changes to the name, capacity or nightly rate of an existing room type with valid values, the HRS shall store them, show the changed values in the room type list and show a confirmation naming the room type.
3. If the room type name is missing, blank or longer than 50 characters, the HRS shall refuse to save with a message naming the name as the problem.
4. If the room type name equals the name of another room type, ignoring upper/lower case and surrounding spaces, the HRS shall refuse to save with a message that this room type name already exists.
5. If the capacity is missing or not a whole number from 1 to 99, the HRS shall refuse to save with a message naming the capacity as the problem.
6. If the nightly rate is missing, not greater than 0.00, not less than 100000.00 or has more than two decimal places, the HRS shall refuse to save with a message naming the nightly rate as the problem.
7. If saving a room type is refused, the HRS shall keep the stored room types unchanged and keep the entered values in the form.

### Requirement 3: Nightly rate changes and existing reservations
**Objective:** As a manager, I want to change a nightly rate without touching what guests already booked, so that
reservations keep the price they were given.

#### Acceptance Criteria
1. When the nightly rate of a room type has been changed, the HRS shall offer the new nightly rate for rooms of that type in availability and use it for reservations created afterwards (M2).
2. When the nightly rate of a room type is changed, the HRS shall keep the booked nightly rate and the price of every existing reservation unchanged (M2).
3. While the manager edits the nightly rate of a room type, the HRS shall show the hint that a new nightly rate applies to new reservations only.

### Requirement 4: Capacity changes and existing reservations
**Objective:** As a manager, I want the system to stop me from shrinking a room type below what guests already
booked, so that no reserved guest arrives to a room that is too small.

#### Acceptance Criteria
1. If the new capacity of a room type is lower than the number of guests of a Reserved or Checked-in reservation of a room of that type, the HRS shall refuse to save with a message naming the room number and that room's largest number of booked guests, and keep the room type unchanged (M3).
2. When the capacity of a room type is reduced and only Cancelled or Checked-out reservations of its rooms have more guests than the new capacity, the HRS shall save the new capacity.
3. When the capacity of a room type has been changed, the HRS shall use the new capacity for availability and for reservations created or changed afterwards.

### Requirement 5: Room list
**Objective:** As a manager, I want to see all rooms with their room type, so that I can check the hotel's set-up at a
glance.

#### Acceptance Criteria
1. When the manager opens the rooms page, the HRS shall list every room with its room number, room type, capacity and nightly rate, ordered by room number.
2. When the manager selects a room in the list, the HRS shall show its room number and room type in an edit form, offering every room type by name as a choice.

### Requirement 6: Creating and editing rooms
**Objective:** As a manager, I want to add rooms and change their room number or room type, so that the system matches
the building.

#### Acceptance Criteria
1. When the manager saves a new room with a valid room number and a room type, the HRS shall store it with the room number in upper case, show it in the room list and show a confirmation naming the room number and saying that the room counts as dirty until housekeeping marks it clean (M4, M7).
2. When the manager saves a changed room number or room type of an existing room with valid values, the HRS shall store them, show the changed values in the room list and show a confirmation naming the room number.
3. If the room number is missing, blank, longer than 10 characters or contains characters other than letters and digits after surrounding spaces are removed, the HRS shall refuse to save with a message naming the room number as the problem (M4).
4. If the room number equals the room number of another room, ignoring upper/lower case and surrounding spaces, the HRS shall refuse to save with a message that this room number already exists.
5. If no room type is chosen or the chosen room type no longer exists, the HRS shall refuse to save with a message naming the room type as the problem.
6. If the room type of a room is changed to a room type whose capacity is lower than the number of guests of a Reserved or Checked-in reservation of that room, the HRS shall refuse to save with a message naming that room's largest number of booked guests and the new capacity (M3).
7. When the room type of a room with existing reservations is changed, the HRS shall keep the booked nightly rate and price of those reservations unchanged (M2).
8. When the room number of a room with existing reservations is changed, the HRS shall keep those reservations on that room and show them with the new room number.
9. If saving a room is refused, the HRS shall keep the stored rooms unchanged and keep the entered values in the form.

### Requirement 7: Removing rooms and room types
**Objective:** As a manager, I want to remove a room or room type created by mistake, without ever losing a room or
room type that reservations depend on.

#### Acceptance Criteria
1. When the manager asks to remove a room or a room type, the HRS shall ask for confirmation naming the room number or room type name before removing anything.
2. If the manager does not confirm the removal, the HRS shall keep the room or room type unchanged.
3. When the manager confirms the removal of a room that never had any reservation, the HRS shall remove it, no longer list it on the rooms page, in availability or on the cleaning list, and show a confirmation naming the room number (M1).
4. If the manager confirms the removal of a room that has at least one reservation in any status, the HRS shall refuse with a message that the room has reservations and cannot be removed, and keep the room (M1).
5. When the manager confirms the removal of a room type that no room has, the HRS shall remove it, no longer list it and show a confirmation naming the room type (M1).
6. If the manager confirms the removal of a room type that at least one room has, the HRS shall refuse with a message naming the number of rooms that have it, and keep the room type (M1).
7. The HRS shall not offer any way to take a room or a room type out of service (M1).

### Requirement 8: Consistent results with simultaneous work
**Objective:** As the hotel, I want room maintenance and reservations made at the same moment to never contradict each
other, so that no reservation exceeds its room's capacity or loses its room.

#### Acceptance Criteria
1. If a reservation for a room and a capacity reduction, a room type change or the removal of that room are requested at the same moment, the HRS shall apply them one after the other and judge the second against the state left by the first, so that no Reserved or Checked-in reservation exceeds its room's capacity and no reservation refers to a removed room.
2. If the room or room type being edited or removed no longer exists when the manager saves or confirms, the HRS shall show a message that it no longer exists and refresh the list.

### Requirement 9: Manager-only access
**Objective:** As the hotel, I want only the manager to change rooms, room types and rates, so that the offer cannot be
changed by mistake at the front desk or in housekeeping.

#### Acceptance Criteria
1. Where the signed-in staff member is a manager, the HRS shall let them open the room types page and the rooms page and list both as "Room types" and "Rooms" in the navigation menu.
2. If a signed-in receptionist or housekeeper opens the room types page or the rooms page, the HRS shall not show any of its content, shall show that the page is not available, and shall not list either page in their navigation menu.
3. If a person who is not signed in opens the room types page or the rooms page, the HRS shall show the login page instead.
4. When the room types page or the rooms page is shown at desktop width (1280 px), the HRS shall show the list and the edit form without horizontal scrolling.
