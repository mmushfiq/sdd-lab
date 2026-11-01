# Tasks

## 1. Tables and seeded rooms

- [x] 1.1 Create `src/main/resources/db/migration/V2__create_room.sql` with tables `room_type` and `room` (design.md,
  decision 1) and the seeded room types and ten rooms (decision 2); verify `./mvnw compile` generates the jOOQ
  classes `ROOM_TYPE` and `ROOM`
- [x] 1.2 Add a test that the database rejects a room type with capacity 0, a nightly rate of 0, a duplicate name,
  and a room with a duplicate room number; and that exactly ten rooms exist (scenario "Ten rooms after
  installation"); verify `./mvnw test` passes

## 2. Room service

- [x] 2.1 Implement `RoomService` in `ch.martinelli.edu.hrs.room` to list, create and update room types and rooms,
  translating duplicate keys into "A room type with this name already exists" and "A room with this number already
  exists"; verify with a service test covering both duplicate messages and a successful create and update

## 3. Staff layout

- [x] 3.1 Implement `StaffLayout` (`AppLayout` + `SideNav` filtered by `AccessAnnotationChecker`, user name, logout)
  and move `StaffStartView` into it; verify `./mvnw compile` succeeds
- [x] 3.2 Write a Browserless test for `StaffLayout`: as `manager` the navigation shows "Rooms" and "Room types"; as
  `reception` it shows neither; existing `StaffStartView` tests still pass; verify `./mvnw test` passes

## 4. Room types view

- [x] 4.1 Implement `RoomTypeView` (route `room-types`, Manager only, `@AccessDeniedErrorRouter`) with grid and
  create/edit form, no delete action; verify `./mvnw compile` succeeds
- [x] 4.2 Write a Browserless test for `RoomTypeView`: list shows the three seeded types; create "Suite" (3, 250.00);
  change a nightly rate; duplicate name shows the message; empty name, capacity 0 and rate 0 mark the fields; no
  delete button; `reception` gets "Access denied"; verify `./mvnw test` passes (scenarios of requirements "Room
  types", "Create a room type", "Change a room type", "No deletion", "Only the manager maintains rooms")

## 5. Rooms view

- [x] 5.1 Implement `RoomView` (route `rooms`, Manager only, `@AccessDeniedErrorRouter`) with grid sorted by room
  number showing room type, capacity and nightly rate, and a create/edit form with a room type `ComboBox`, no delete
  action; verify `./mvnw compile` succeeds
- [x] 5.2 Write a Browserless test for `RoomView`: ten rooms listed; create room "111" as "Double"; change room
  "104" to "Family"; duplicate room number shows the message; missing number or type marks the fields; no delete
  button; verify `./mvnw test` passes (scenarios of requirements "Rooms", "Create a room", "Change a room")

## 6. End-to-end

- [x] 6.1 Write `RoomManagementIT` extending `PlaywrightIT`: log in as `manager`, open "Room types" from the
  navigation, change the nightly rate of "Double", open "Rooms" and see the new rate on room "104"; verify
  `./mvnw verify -Dit.test=RoomManagementIT` passes
- [x] 6.2 Run `./mvnw verify`; verify the build ends green

## Workflow follow-up

- Run `/opsx:verify add-room-management`, then `/opsx:archive add-room-management`.
