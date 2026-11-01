# Tasks

## 1. Statuses

- [x] 1.1 Create `src/main/resources/db/migration/V5__add_reservation_check_in_out_status.sql` (design.md,
  decision 1); verify `./mvnw compile` succeeds and a test can store a reservation with status `CHECKED_IN` while
  `'NO_SHOW'` is rejected

## 2. Service

- [x] 2.1 Implement `FrontDeskService.roomOverview` in `ch.martinelli.edu.hrs.frontdesk`; verify with a service test
  (fixed `Clock`) for Free, Arrival today and Occupied rooms with guest name, departure date and cleaning status
- [x] 2.2 Implement `checkIn` and `checkOut`; verify with a service test: successful check-in; refused for a dirty
  room ("The room is not clean yet"), a still occupied room ("The room is still occupied"), an arrival date other
  than today and a non-Booked reservation; check-out sets Checked out and the room dirty and the room appears in
  `HousekeepingService.findDirtyRooms`; early check-out shortens the departure date and frees the room in
  `AvailabilityService`; check-out on the arrival day keeps one night; check-out of a non-Checked-in reservation is
  refused

## 3. Front desk view

- [x] 3.1 Implement `FrontDeskView` (route `front-desk`, Receptionist only, `@AccessDeniedErrorRouter`, in
  `StaffLayout`) with the room grid, state and cleaning badges, and "Check in"/"Check out" with `ConfirmDialog`;
  verify `./mvnw compile` succeeds
- [x] 3.2 Write a Browserless test for `FrontDeskView`: rooms shown as Free, Arrival today and Occupied with guest
  name; dirty badge; check-in moves the room to Occupied; check-in on a dirty room shows "The room is not clean
  yet"; check-out shows the room Free and dirty; no "Check in" for a reservation arriving tomorrow; `housekeeping`
  gets "Access denied"; verify `./mvnw test` passes
- [x] 3.3 Show the statuses "Checked in" and "Checked out" in `ReservationView` and extend its Browserless test with
  a Checked in reservation; verify `./mvnw test` passes (scenario "Checked in reservation shown in the reservation
  list")

## 4. End-to-end

- [x] 4.1 Write `CheckInOutIT` extending `PlaywrightIT`: insert a Booked reservation for room "104" arriving today,
  log in as `reception`, open "Front desk", check the guest in, check the guest out, see room "104" Free and dirty;
  log out, log in as `housekeeping` and see room "104" on the cleaning list; verify
  `./mvnw verify -Dit.test=CheckInOutIT` passes
- [x] 4.2 Run `./mvnw verify`; verify the build ends green

## Workflow follow-up

- Run `/opsx:verify add-check-in-out`, then `/opsx:archive add-check-in-out`.
