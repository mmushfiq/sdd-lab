# Tasks

## 1. Cleaning status

- [x] 1.1 Create `src/main/resources/db/migration/V4__add_room_cleaning_status.sql` adding `room.cleaning_status`
  (design.md, decision 1); verify `./mvnw compile` regenerates `ROOM` with `CLEANING_STATUS` and all ten rooms are
  clean
- [x] 1.2 Add a test that a room created through `RoomService` is clean and that the database rejects the value
  `'WET'`; verify `./mvnw test` passes (scenario "New room is clean")

## 2. Service

- [x] 2.1 Implement `HousekeepingService` in `ch.martinelli.edu.hrs.housekeeping` with `findDirtyRooms` and
  `markCleaned`; verify with a service test: dirty rooms sorted by room number, marking cleaned removes the room
  from the result, marking an already clean room keeps it clean

## 3. Cleaning list view

- [x] 3.1 Implement `CleaningListView` (route `cleaning`, Housekeeping only, `@AccessDeniedErrorRouter`, in
  `StaffLayout`) with room cards, large "Cleaned" buttons, "Refresh" and the message "All rooms are clean"; verify
  `./mvnw compile` succeeds
- [x] 3.2 Write a Browserless test for `CleaningListView`: as `housekeeping` with rooms "104" and "102" dirty the
  list shows "102" then "104"; "Cleaned" on "104" removes it; with no dirty rooms "All rooms are clean" is shown;
  room cleaned by someone else then "Cleaned" again leaves it clean and gone; `manager` gets "Access denied"; verify
  `./mvnw test` passes

## 4. End-to-end

- [x] 4.1 Write `CleaningListIT` extending `PlaywrightIT`: set room "104" dirty with jOOQ, set the viewport to 768 ×
  1024, log in as `housekeeping`, open "Cleaning", check that the page has no horizontal scrollbar and the
  "Cleaned" button is at least 44 px high, press it and see room "104" disappear; verify
  `./mvnw verify -Dit.test=CleaningListIT` passes (scenario "Tablet width")
- [x] 4.2 Run `./mvnw verify`; verify the build ends green

## Workflow follow-up

- Run `/opsx:verify add-housekeeping`, then `/opsx:archive add-housekeeping`.
