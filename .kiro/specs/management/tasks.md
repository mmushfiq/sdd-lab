# Implementation Plan

- [ ] 1. Contract types, lists and test data helper
  - Prerequisite: foundation tasks 1.1, 1.2 and 2.1 (security dependencies, fixed test clock 2026-06-01, room and room type schema with the ten seeded rooms), reservation-core tasks 1.1, 3.1 and 3.3 to 3.6 (reservation schema, availability, create, change, cancel and check-in through the reservation service) and housekeeping tasks 1 and 2 (cleaning status table with cascade on room removal, housekeeping service) are implemented; this spec adds no migration
  - Add the public list rows for room types (with number of rooms) and rooms, the create/edit input records, and the rule-violation exception with the reasons invalid input, in use and not found
  - Add the repository reads for room types ordered by name with their room counts and rooms ordered by room number with room type, capacity and nightly rate, and the service list methods
  - Add the test helper that inserts test room types and rooms with ids 7000–7099 (names `Test 7…`, numbers `T7…`), creates June 2027 reservations through the reservation service with obviously fake guest data, and removes everything with these prefixes after each test
  - Create RoomManagementServiceTest with the list cases: Double, Family, Single and a test room type in name order with room counts 6, 2, 2 and 1; rooms in room number order with type, capacity and rate
  - Done when `./mvnw compile` succeeds and the list cases of RoomManagementServiceTest pass with no test row left afterwards
  - _Requirements: 1.1, 5.1_

- [ ] 2. Room type rules in the service
- [ ] 2.1 Create and edit room types with validation
  - Validate name (trimmed, 1–50 characters, unique ignoring case and surrounding spaces, excluding the edited room type), capacity (whole number 1–99) and nightly rate (above 0.00, below 100000.00, at most two decimals) before any write, with the documented messages
  - Lock the edited room type row; a room type that no longer exists is reported as not found
  - Write only the room type row, never a reservation
  - RoomManagementServiceTest cases: create and edit return the stored values; every refusal (blank name, 51 characters, `double` as duplicate, capacity 0, 100 and missing, rate 0, 100000, 12.345 and missing) has its message and leaves the stored rows unchanged; updating unknown id 7999 is not found; after a rate change from 100.00 to 150.00 an existing June 2027 reservation keeps 100.00 and its price, while availability shows 150.00 and a new reservation is booked at 150.00
  - Done when these cases pass
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 3.1, 3.2, 8.2_

- [ ] 2.2 Protect reservations when a capacity is reduced
  - When the capacity is lowered, lock the room rows of the room type after the room type row and find the largest Reserved or Checked-in booking of these rooms; refuse as in use with the room number and number of guests if it exceeds the new capacity
  - RoomManagementServiceTest cases: a Reserved 3-guest reservation on `T7000` (capacity 4) blocks a reduction to 2 with the documented message and the capacity stays 4; once that reservation is cancelled the reduction is saved; afterwards reservation-core refuses a 3-guest reservation on `T7000` and availability for 3 guests no longer lists it
  - Done when these cases and all earlier service cases pass
  - _Requirements: 4.1, 4.2, 4.3, 8.1_

- [ ] 2.3 Remove room types without rooms
  - Lock the room type row, refuse as in use with the number of rooms when any room has it, otherwise delete it; a vanished room type is not found
  - RoomManagementServiceTest cases: a test room type without rooms is removed and no longer listed; `Test 7000` with one room is refused naming 1 room; the seeded Double is refused naming 6 rooms; removing unknown id 7999 is not found
  - Done when these cases pass
  - _Requirements: 7.5, 7.6, 8.2_

- [ ] 3. Room rules in the service
- [ ] 3.1 Create and edit rooms with validation
  - Validate the room number (trimmed, upper-cased, 1–10 letters and digits) and the presence of a room type before any lock; lock the target room type row (missing means invalid input "Room type does not exist."), then the edited room row (missing means not found); check the room number is unique ignoring case, excluding the edited room
  - Write only the room row; write no cleaning status for a new room
  - RoomManagementServiceTest cases: ` t7001 ` is stored as `T7001` and the housekeeping service reports it not clean; renaming and changing to another room type are stored; refusals for blank, `T-1`, 11 characters, `101` and `t7000` as duplicates, missing and unknown room type each have their message and leave the rows unchanged; renaming a room with a June 2027 reservation keeps the reservation on it and the reservation shows the new number; updating unknown id 7999 is not found
  - Done when these cases pass
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.8, 6.9, 8.2_

- [ ] 3.2 Protect reservations when a room changes its room type
  - When the room type changes, find the largest Reserved or Checked-in booking of that room (after both locks) and refuse as in use if the target room type's capacity is lower, with the documented message
  - RoomManagementServiceTest cases: moving a room with a Reserved 2-guest reservation to a capacity-1 test room type is refused and the room keeps its type; moving it to a fitting room type with another rate keeps the reservation's booked rate and price
  - Done when these cases pass
  - _Requirements: 6.6, 6.7, 8.1_

- [ ] 3.3 Remove rooms that never had a reservation
  - Lock the room row, refuse as in use when any reservation in any status refers to it, otherwise delete it (its cleaning status disappears with it); a vanished room is not found
  - RoomManagementServiceTest cases: an unused test room is removed and is absent from the room list, from availability and from the housekeeping service's statuses; a room with only a cancelled reservation is refused with "has reservations"; removing unknown id 7999 is not found
  - Done when these cases and all RoomManagementServiceTest cases pass
  - _Requirements: 7.3, 7.4, 8.2_

- [ ] 4. Manager pages
- [ ] 4.1 (P) Room types page
  - Manager-only staff page "Room types" (menu order 80) with the room type grid, a form for name, capacity and nightly rate showing the hint that a new nightly rate applies to new reservations only, required-field checks, and New, Save, Remove and Cancel actions; it reloads on every navigation
  - Show success and refusal messages as notifications, keep entered values on refusal, reload and clear the form when the room type no longer exists; ask for confirmation naming the room type before removing
  - RoomTypeViewTest: grid content and columns; selecting fills the form; create and edit with success notification; duplicate name shows the service message with values kept; empty name marks the field and stores nothing; rate hint visible; confirmation opens, Cancel keeps and Confirm removes an unused test room type; a room type with rooms shows the count; a room type deleted by the test before Save shows "no longer exists"; manager sees "Room types" in the menu, receptionist and housekeeper get the not-available view without menu entry, anonymous ends on the login page
  - Done when RoomTypeViewTest passes
  - _Boundary: RoomTypeView_
  - _Depends: 2.3_
  - _Requirements: 1.1, 1.2, 2.1, 2.2, 2.3, 2.4, 2.7, 3.3, 7.1, 7.2, 7.5, 7.6, 8.2, 9.1, 9.2, 9.3_

- [ ] 4.2 (P) Rooms page
  - Manager-only staff page "Rooms" (menu order 81) with the room grid, a form with room number and a room type choice listing every room type by name, required-field checks, and New, Save, Remove and Cancel actions; no out-of-service field or action; it reloads grid and choices on every navigation
  - New room confirmation says the room counts as dirty until housekeeping marks it clean; refusals keep entered values; a vanished room reloads the list and clears the form; confirmation naming the room number before removing
  - RoomViewTest: grid content and columns; selecting fills number and room type with the three seeded types offered; create with the dirty hint and edit; duplicate number shows the service message with values kept; empty room type marks the field and stores nothing; Cancel in the confirmation keeps, Confirm removes an unused test room; a test room with a June 2027 reservation shows "has reservations" and stays; a room deleted by the test before Save shows "no longer exists"; the form offers only number and room type; access cases as on the room types page with "Rooms" in the manager's menu
  - Done when RoomViewTest passes
  - _Boundary: RoomView_
  - _Depends: 3.3_
  - _Requirements: 5.1, 5.2, 6.1, 6.2, 6.4, 6.5, 6.9, 7.1, 7.2, 7.3, 7.4, 7.7, 8.2, 9.1, 9.2, 9.3_

- [ ] 5. Validation
- [ ] 5.1 Prove consistency with simultaneous reservations
  - RoomManagementConcurrencyTest without a test-managed transaction, cleaning up its rows afterwards: 10 rounds each of two threads released together for a 3-guest reservation versus a capacity reduction to 2, a reservation versus removing the room, and a reservation versus a room type change to capacity 1
  - In every round no Reserved or Checked-in reservation exceeds its room's stored capacity, and either the room still exists with the reservation or the room is gone and the reservation was refused with "Room does not exist."
  - If a round fails, this task may change only the lock calls and their order in the management repository and service
  - Done when RoomManagementConcurrencyTest passes in three consecutive runs
  - _Requirements: 8.1_

- [ ] 5.2 Browser journey and final check
  - ManagementIT: real login as the test manager at 1280×800; create a room type and a room with a lower-case number through the pages, see the room in upper case in the grid, no horizontal scrolling on both pages, then remove room and room type through the confirmations; cleanup after the test
  - Review that no page or service method offers taking a room or room type out of service and that management main code imports no other feature package
  - Done when `./mvnw verify` passes and the surefire and failsafe reports list RoomManagementServiceTest, RoomManagementConcurrencyTest, RoomTypeViewTest, RoomViewTest and ManagementIT with no skipped tests
  - _Requirements: 2.1, 6.1, 7.3, 7.5, 7.7, 9.1, 9.4_
