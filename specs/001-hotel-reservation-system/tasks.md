---

description: "Task list for feature 001-hotel-reservation-system"
---

# Tasks: Hotel Reservation System

**Input**: Design documents from `/specs/001-hotel-reservation-system/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-routes.md, quickstart.md

**Tests**: Included. Constitution principle IV requires a Browserless test per view and a Playwright `*IT` per
feature; research R10 adds service tests for the rules. Tests come first in each story and must fail before the
implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US8)
- Paths: `main/` = `src/main/java/ch/martinelli/edu/hrs/`, `test/` = `src/test/java/ch/martinelli/edu/hrs/`,
  `migration/` = `src/main/resources/db/migration/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Dependencies and shared beans

- [X] T001 Add `spring-boot-starter-security` and `spring-boot-starter-security-test` (scope test) to `pom.xml` (research R1)
- [X] T002 [P] Create `Clock` bean (system default zone) in `main/core/ClockConfiguration.java` (research R5)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Room and staff tables, login, layout — needed by every user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T003 Create `migration/V1__room_type_and_room.sql`: sequences; `room_type` (id BIGINT PK; name "VARCHAR(50) NOT NULL, UNIQUE"; capacity "INT NOT NULL, ≥ 1"; nightly_rate "DECIMAL(10,2) NOT NULL, > 0"); `room` (id BIGINT PK; room_number "VARCHAR(10) NOT NULL, UNIQUE"; room_type_id "NOT NULL, FK → room_type (no cascade)"; cleaning_status "VARCHAR(10) NOT NULL, `CLEAN` or `DIRTY`, default `CLEAN`")
- [X] T004 Create `migration/V2__staff_user.sql`: sequence; `staff_user` (id BIGINT PK; username "VARCHAR(50) NOT NULL, UNIQUE"; password_hash "VARCHAR(100) NOT NULL, `{bcrypt}…`"; role "VARCHAR(20) NOT NULL, `RECEPTIONIST`, `MANAGER` or `HOUSEKEEPING`"; active "BOOLEAN NOT NULL, default true"); insert Manager `manager` with `{bcrypt}` hash of `manager` (research R2)
- [X] T005 Run `./mvnw compile` to generate jOOQ classes into `ch.martinelli.edu.hrs.db` (depends on T003, T004)
- [X] T006 [P] Write `test/core/security/StaffUserDetailsServiceTest.java`: active user loads with role, inactive user is disabled, unknown user throws
- [X] T007 [P] Create `Role` enum and `StaffUserDetailsService` (jOOQ on `staff_user`, inactive → disabled) in `main/core/security/`
- [X] T008 Create `SecurityConfiguration` in `main/core/security/SecurityConfiguration.java`: `VaadinSecurityConfigurer.vaadin()` with `loginView(LoginView.class)`, delegating `PasswordEncoder` bean (research R1)
- [X] T009 Create `LoginView` (route `login`, `@AnonymousAllowed`) in `main/core/security/LoginView.java`; after login go to the role's first view: Receptionist → `reception/front-desk`, Manager → `manager/rooms`, Housekeeping → `housekeeping` (contracts/ui-routes.md)
- [X] T010 Create `MainLayout` in `main/core/ui/MainLayout.java`: app layout with side navigation from `MenuConfiguration.getMenuEntries()` and a logout button (research R9)
- [X] T011 [P] Write `test/core/security/LoginViewTest.java` (Browserless, extends `HrsBrowserlessTest`): login form shown to anonymous users
- [X] T012 Run `./mvnw verify` — must end green

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 - Manager maintains room types, rooms and rates (Priority: P1) 🎯 MVP

**Goal**: Manager logs in and maintains room types, rooms and staff users (FR-001–FR-004, FR-022, FR-025)

**Independent Test**: Log in as Manager, create a room type and a room, change the rate, create a Receptionist
user who then sees only reception menus

### Tests for User Story 1

- [X] T013 [P] [US1] Write `test/room/RoomTypeServiceTest.java`: create, change rate, duplicate name rejected, delete of type used by a room rejected (US1 scenarios 1, 4)
- [X] T014 [P] [US1] Write `test/room/RoomServiceTest.java`: create room with type, new room is `CLEAN`, duplicate room number rejected (US1 scenarios 2, 3)
- [X] T015 [P] [US1] Write `test/staff/StaffUserServiceTest.java`: create user, duplicate username rejected, change role, reset password, deactivated user cannot authenticate (US1 scenarios 7–9)
- [X] T016 [P] [US1] Write `test/room/RoomTypeViewTest.java` and `test/room/RoomViewTest.java` (Browserless, `@WithMockUser(roles = "MANAGER")`): grid lists data, form creates/changes/deletes, error shown on rejected delete; access denied for `RECEPTIONIST` and `HOUSEKEEPING` (US1 scenario 6)
- [X] T017 [P] [US1] Write `test/staff/StaffUserViewTest.java` (Browserless, Manager): create, change role, reset password, deactivate; denied for other roles
- [X] T018 [P] [US1] Write `test/room/ManagerIT.java` (Playwright, extends `PlaywrightIT`): log in as `manager`, create room type and room, create Receptionist user, log in as that user and see only reception menu entries; clean up created data

### Implementation for User Story 1

- [X] T019 [P] [US1] Implement `RoomTypeService` in `main/room/RoomTypeService.java` (jOOQ; user-facing messages for duplicate name and type in use)
- [X] T020 [P] [US1] Implement `CleaningStatus` enum and `RoomService` in `main/room/` (duplicate room number, delete blocked by FK → message, research R8)
- [X] T021 [P] [US1] Implement `StaffUserService` in `main/staff/StaffUserService.java` (password encoded with the `PasswordEncoder` bean)
- [X] T022 [US1] Implement `RoomTypeView` (route `manager/room-types`, `@RolesAllowed("MANAGER")`, `@Menu`, layout `MainLayout`) in `main/room/RoomTypeView.java` with `Binder` validation (research R11)
- [X] T023 [US1] Implement `RoomView` (route `manager/rooms`, `@RolesAllowed("MANAGER")`, `@Menu`) in `main/room/RoomView.java`
- [X] T024 [US1] Implement `StaffUserView` (route `manager/staff`, `@RolesAllowed("MANAGER")`, `@Menu`) in `main/staff/StaffUserView.java`
- [X] T025 [US1] Run `./mvnw verify` — must end green

**Checkpoint**: User Story 1 works on its own

---

## Phase 4: User Story 2 - Receptionist books a room for a phone or walk-in guest (Priority: P1)

**Goal**: Availability search and reservation creation without double bookings (FR-005–FR-010, FR-012)

**Independent Test**: As Receptionist, search two nights for two guests, book a room, and see it no longer
offered for overlapping nights

### Tests for User Story 2

- [ ] T026 [P] [US2] Write `test/reservation/AvailabilityServiceTest.java`: capacity filter, overlapping nights excluded, departure day of one stay = arrival of next allowed, `CHECKED_OUT`/`CANCELLED` do not block, date rules of FR-008 (departure after arrival, arrival not past, arrival ≤ 1 year ahead, ≤ 30 nights), no room found (US2 scenarios 1, 3, 4, 6, 7)
- [ ] T027 [P] [US2] Write `test/reservation/ReservationServiceTest.java`: booking creates `BOOKED` reservation with 8-character number, nightly rate, total price and source `RECEPTION`; overlapping booking rejected; rate change of room type keeps existing reservation's rate (US1 scenario 5, US2 scenarios 2, 3)
- [ ] T028 [P] [US2] Write `test/reservation/ReservationConcurrencyTest.java` (not transactional): two threads book the same room and nights, exactly one succeeds (US2 scenario 5, research R3)
- [ ] T029 [P] [US2] Extend `test/room/RoomServiceTest.java`: deleting a room with a reservation is rejected (FR-003)
- [ ] T030 [P] [US2] Write `test/reservation/ReservationViewTest.java` (Browserless, `RECEPTIONIST`): search shows available rooms with total price, booking form validation, reservation number shown, "no room available" message
- [ ] T031 [P] [US2] Write `test/reservation/ReceptionBookingIT.java` (Playwright): Receptionist books a room; same dates no longer offer that room

### Implementation for User Story 2

- [ ] T032 [US2] Create `migration/V3__reservation.sql`: sequence; `reservation` with reservation_number "CHAR(8) NOT NULL, UNIQUE"; room_id "NOT NULL, FK → room"; arrival_date "DATE NOT NULL"; departure_date "DATE NOT NULL, > arrival_date"; number_of_guests "INT NOT NULL, ≥ 1"; guest_name "VARCHAR(100) NOT NULL"; guest_email "VARCHAR(254)", guest_phone "VARCHAR(30)" (NULL only after anonymization); nightly_rate and total_price "DECIMAL(10,2) NOT NULL"; status "VARCHAR(12) NOT NULL, `BOOKED`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED`"; source "VARCHAR(10) NOT NULL, `ONLINE` or `RECEPTION`"; cancelled_at TIMESTAMP; anonymized "BOOLEAN NOT NULL, default false"; created_at "TIMESTAMP NOT NULL"; index on `(room_id, arrival_date, departure_date)`
- [ ] T033 [US2] Run `./mvnw compile` to regenerate jOOQ classes
- [ ] T034 [P] [US2] Implement `ReservationStatus` enum and `ReservationNumberGenerator` (alphabet `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`, `SecureRandom`, research R4) in `main/reservation/`
- [ ] T035 [US2] Implement `AvailabilityService` in `main/reservation/AvailabilityService.java` (overlap: `arrival < newDeparture AND departure > newArrival` for `BOOKED`/`CHECKED_IN`; FR-008 rules using `Clock`)
- [ ] T036 [US2] Implement `ReservationService.book` in `main/reservation/ReservationService.java`: one transaction, `SELECT … FOR UPDATE` on the room, overlap check, insert with rate at booking and total = nights × rate, source passed by the caller (research R3)
- [ ] T037 [US2] Implement `ReservationView` (route `reception/reservations`, `@RolesAllowed("RECEPTIONIST")`, `@Menu`) in `main/reservation/ReservationView.java`: search form, result grid, booking dialog with name, email, phone (source `RECEPTION`)
- [ ] T038 [US2] Run `./mvnw verify` — must end green

**Checkpoint**: User Stories 1 and 2 work

---

## Phase 5: User Story 3 - Guest books a room online (Priority: P2)

**Goal**: Public search and booking without login (FR-005, FR-011, FR-023)

**Independent Test**: Without login, search, book and receive a reservation number; the room is then no longer
offered

### Tests for User Story 3

- [ ] T039 [P] [US3] Write `test/guest/GuestBookingViewTest.java` (Browserless, anonymous): search results with room type, capacity, total price; required fields and email format; "no longer available" when the room was taken meanwhile (US3 scenarios 1–4)
- [ ] T040 [P] [US3] Write `test/guest/GuestBookingIT.java` (Playwright): book online and see the reservation number; opening `reception/reservations` without login lands on `login` (US3 scenario 5)

### Implementation for User Story 3

- [ ] T041 [US3] Implement `GuestBookingView` (route root, `@AnonymousAllowed`, no staff menu) in `main/guest/GuestBookingView.java` using `AvailabilityService` and `ReservationService.book` with source `ONLINE`
- [ ] T042 [US3] Run `./mvnw verify` — must end green

**Checkpoint**: Online booking works

---

## Phase 6: User Story 4 - Receptionist checks guests in and out (Priority: P2)

**Goal**: Check-in only to clean rooms, check-out marks room dirty, room overview (FR-017–FR-019)

**Independent Test**: Check in a reservation arriving today for a clean room, check out, see the room dirty

### Tests for User Story 4

- [ ] T043 [P] [US4] Write `test/reservation/CheckInOutServiceTest.java`: check-in `BOOKED` + arrival today + room `CLEAN` → `CHECKED_IN`; dirty room rejected; future arrival rejected; check-out → `CHECKED_OUT` and room `DIRTY`; room overview `FREE`/`BOOKED`/`OCCUPIED` per room (US4 scenarios 1–5)
- [ ] T044 [P] [US4] Write `test/frontdesk/FrontDeskViewTest.java` (Browserless, `RECEPTIONIST`): arrivals and departures of today, check-in/out buttons, error on dirty room, overview grid
- [ ] T045 [P] [US4] Write `test/frontdesk/FrontDeskIT.java` (Playwright): check in and check out a guest; overview shows the room dirty

### Implementation for User Story 4

- [ ] T046 [US4] Add `checkIn`, `checkOut`, `findArrivals(today)`, `findDepartures(today)` and `roomOverview(today)` to `main/reservation/ReservationService.java` (status transitions per data-model.md)
- [ ] T047 [US4] Implement `FrontDeskView` (route `reception/front-desk`, `@RolesAllowed("RECEPTIONIST")`, `@Menu`) in `main/frontdesk/FrontDeskView.java`
- [ ] T048 [US4] Run `./mvnw verify` — must end green

**Checkpoint**: A full stay can be checked in and out

---

## Phase 7: User Story 5 - Housekeeping works through the cleaning list (Priority: P2)

**Goal**: Dirty-room list and mark cleaned (FR-020)

**Independent Test**: With a dirty room, log in as Housekeeping, mark it cleaned, it leaves the list

### Tests for User Story 5

- [ ] T049 [P] [US5] Extend `test/room/RoomServiceTest.java`: `findDirtyRooms` and `markCleaned` (US5 scenarios 1, 2)
- [ ] T050 [P] [US5] Write `test/housekeeping/CleaningListViewTest.java` (Browserless, `HOUSEKEEPING`): dirty rooms listed with room number and status only, no guest data (FR-024), mark cleaned removes the row; `HOUSEKEEPING` denied on `reception/reservations` and `manager/rooms` (US5 scenario 3)
- [ ] T051 [P] [US5] Write `test/housekeeping/HousekeepingIT.java` (Playwright, tablet viewport 1024×768): checked-out room appears, mark cleaned

### Implementation for User Story 5

- [ ] T052 [US5] Add `findDirtyRooms` and `markCleaned` to `main/room/RoomService.java`
- [ ] T053 [US5] Implement `CleaningListView` (route `housekeeping`, `@RolesAllowed("HOUSEKEEPING")`, `@Menu`, poll every 30 s, refresh button, large touch targets) in `main/housekeeping/CleaningListView.java` (research R7)
- [ ] T054 [US5] Run `./mvnw verify` — must end green

**Checkpoint**: Check-out to clean room loop complete

---

## Phase 8: User Story 6 - Receptionist changes and cancels reservations (Priority: P2)

**Goal**: Find, change and cancel booked reservations (FR-013, FR-014, FR-016)

**Independent Test**: Change a booked reservation to free dates, cancel it, the room is offered again

### Tests for User Story 6

- [ ] T055 [P] [US6] Extend `test/reservation/ReservationServiceTest.java`: find by number or guest name; change dates/room to free nights recalculates total with current rate; change into booked nights rejected; cancel sets `CANCELLED` and `cancelled_at` and frees nights; non-`BOOKED` cannot be changed or cancelled (US6 scenarios 1–4)
- [ ] T056 [P] [US6] Extend `test/reservation/ReservationViewTest.java`: search reservations, edit dialog, cancel with confirmation
- [ ] T057 [P] [US6] Write `test/reservation/ReservationChangeIT.java` (Playwright): change dates, then cancel; room offered again

### Implementation for User Story 6

- [ ] T058 [US6] Add `find`, `change` (room lock + overlap check as in T036) and `cancel` to `main/reservation/ReservationService.java`
- [ ] T059 [US6] Extend `main/reservation/ReservationView.java` with reservation search, change dialog and cancel action
- [ ] T060 [US6] Run `./mvnw verify` — must end green

**Checkpoint**: Reception can manage reservations without paper

---

## Phase 9: User Story 7 - Guest views and cancels their own reservation online (Priority: P3)

**Goal**: Guest lookup with number + email, cancel until the day before arrival, GDPR anonymization (FR-015, FR-024, FR-026)

**Independent Test**: Book online, look up with number and email, cancel, the room is offered again

### Tests for User Story 7

- [ ] T061 [P] [US7] Write `test/reservation/GuestReservationServiceTest.java`: lookup by number + email (email case-insensitive); wrong data gives the same "not found" message whether or not the number exists; cancel when arrival > today; arrival today rejected with "call reception"; non-`BOOKED` rejected (US7 scenarios 1–5)
- [ ] T062 [P] [US7] Write `test/reservation/AnonymizationTest.java`: reservations with departure or cancellation > 12 months ago get name `Anonymized`, email and phone `NULL`, `anonymized = true`; dates, room, prices, status kept; younger ones untouched; anonymized reservation not found by guest lookup (FR-026, edge cases)
- [ ] T063 [P] [US7] Write `test/guest/MyReservationViewTest.java` (Browserless, anonymous): lookup form, details shown, cancel button only when allowed
- [ ] T064 [P] [US7] Write `test/guest/GuestCancelIT.java` (Playwright): book online, look up, cancel

### Implementation for User Story 7

- [ ] T065 [US7] Add `findForGuest(number, email)` and `cancelByGuest` to `main/reservation/ReservationService.java`
- [ ] T066 [US7] Add `anonymizeOlderThan(12 months)` to `ReservationService` and `AnonymizationJob` (`@Scheduled(cron = "0 0 3 * * *")`) in `main/reservation/AnonymizationJob.java`; add `@EnableScheduling` to `main/HrsApplication.java` (research R6)
- [ ] T067 [US7] Implement `MyReservationView` (route `my-reservation`, `@AnonymousAllowed`) in `main/guest/MyReservationView.java`
- [ ] T068 [US7] Run `./mvnw verify` — must end green

**Checkpoint**: Guests can self-serve; guest data is anonymized on time

---

## Phase 10: User Story 8 - Manager views occupancy and revenue reports (Priority: P3)

**Goal**: Monthly occupancy and revenue (FR-021)

**Independent Test**: Open the report for a month with stays and compare with a manual count

### Tests for User Story 8

- [ ] T069 [P] [US8] Write `test/report/ReportServiceTest.java`: occupancy = nights in month of `CHECKED_IN`/`CHECKED_OUT` ÷ (rooms × days in month); revenue = those nights × nightly_rate; `BOOKED`/`CANCELLED` excluded; stay across month end split; online share = reservations created that month with source `ONLINE` ÷ all created that month (US8 scenarios 1–3)
- [ ] T070 [P] [US8] Write `test/report/ReportViewTest.java` (Browserless, `MANAGER`): month picker, occupancy % and revenue shown; denied for `RECEPTIONIST` and `HOUSEKEEPING` (US8 scenario 4)
- [ ] T071 [P] [US8] Write `test/report/ReportIT.java` (Playwright): Manager opens the current month's report

### Implementation for User Story 8

- [ ] T072 [US8] Implement `ReportService` in `main/report/ReportService.java` (data-model.md "Derived values", including the online share)
- [ ] T073 [US8] Implement `ReportView` (route `manager/reports`, `@RolesAllowed("MANAGER")`, `@Menu`) in `main/report/ReportView.java`
- [ ] T074 [US8] Run `./mvnw verify` — must end green

**Checkpoint**: All user stories work

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Checks across all stories

- [ ] T075 Walk through `quickstart.md` manually, including the negative checks; time one online booking (must be under 3 minutes, SC-003) and one check-in and check-out (each under 1 minute, SC-004)
- [ ] T076 [P] Check every view at desktop width and `CleaningListView` at tablet width (SC-008)
- [ ] T077 Run `./mvnw verify` — must end green

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: none
- **Foundational (Phase 2)**: after Setup — blocks all user stories
- **User stories (Phase 3–10)**: after Foundational, in priority order
- **Polish (Phase 11)**: after all user stories

### User Story Dependencies

- **US1 (P1)**: after Foundational
- **US2 (P1)**: after US1 (needs rooms)
- **US3 (P2)**: after US2 (`AvailabilityService`, `ReservationService.book`)
- **US4 (P2)**: after US2
- **US5 (P2)**: after US4 (check-out makes rooms dirty); `RoomService` part can start after US1
- **US6 (P2)**: after US2
- **US7 (P3)**: after US3 (online bookings)
- **US8 (P3)**: after US4 (needs checked-in/out stays)

### Within Each User Story

- Tests first, and they fail before the implementation
- Migration → jOOQ generation → services → views
- The story's last task runs `./mvnw verify`

### Parallel Opportunities

- All test tasks of a story are [P] (different files)
- Services of different areas within a story are [P] (T019–T021)
- After US2: US3, US4 and US6 touch different views and can run in parallel; they share
  `ReservationService`, so merge its changes with care

---

## Parallel Example: User Story 1

```bash
# Tests for User Story 1 together:
Task: "RoomTypeServiceTest in src/test/java/ch/martinelli/edu/hrs/room/RoomTypeServiceTest.java"
Task: "RoomServiceTest in src/test/java/ch/martinelli/edu/hrs/room/RoomServiceTest.java"
Task: "StaffUserServiceTest in src/test/java/ch/martinelli/edu/hrs/staff/StaffUserServiceTest.java"

# Services for User Story 1 together:
Task: "RoomTypeService in src/main/java/ch/martinelli/edu/hrs/room/RoomTypeService.java"
Task: "RoomService in src/main/java/ch/martinelli/edu/hrs/room/RoomService.java"
Task: "StaffUserService in src/main/java/ch/martinelli/edu/hrs/staff/StaffUserService.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup and Phase 2 Foundational
2. Phase 3 User Story 1
3. **STOP and VALIDATE**: `./mvnw verify` and a manual check of the Manager screens

### Incremental Delivery

One `/speckit-implement` run per user story in the order above (guide step 5), each ending with a green
`./mvnw verify` and its own commit.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps a task to its user story for traceability
- Tests create their own data and remove it (research R10)
- Never edit committed migrations or generated jOOQ code (constitution III)
