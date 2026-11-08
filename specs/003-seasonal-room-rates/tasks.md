---

description: "Task list for feature 003-seasonal-room-rates"
---

# Tasks: Seasonal Room Rates

**Input**: Design documents from `/specs/003-seasonal-room-rates/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-changes.md, quickstart.md

**Tests**: Included. Constitution principle IV requires a Browserless test per new or changed view, a Playwright
`*IT` per feature, and a test for every scenario. Tests come first in each story and must fail before the
implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- `main/` = `src/main/java/ch/martinelli/edu/hrs/`, `test/` = `src/test/java/ch/martinelli/edu/hrs/`
- Resources: `src/main/resources/`
- Test dates are relative to `LocalDate.now(clock)` like the existing tests (e.g. a seasonal rate from
  `today + 10` to `today + 20`), so the checks of 001 FR-008 (arrival not in the past, at most one year ahead)
  pass.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: None needed. No new dependency or setting (plan Technical Context).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The two new tables and the copy of existing prices (data-model.md, research R1, R2)

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T001 Create `src/main/resources/db/migration/V5__seasonal_rates.sql`: `create sequence seasonal_rate_seq start with 1;` table `seasonal_rate` (`id bigint primary key`, `room_type_id bigint not null references room_type (id) on delete cascade`, `name varchar(50) not null`, `start_date date not null`, `end_date date not null`, `nightly_rate decimal(10, 2) not null check (nightly_rate > 0)`, `check (end_date >= start_date)`) with index `seasonal_rate_room_type_dates on seasonal_rate (room_type_id, start_date)`; table `reservation_night` (`reservation_id bigint not null references reservation (id)`, `night_date date not null`, `nightly_rate decimal(10, 2) not null check (nightly_rate > 0)`, `primary key (reservation_id, night_date)`); copy every existing reservation: `insert into reservation_night select r.id, dateadd(day, n.x, r.arrival_date), r.nightly_rate from reservation r join system_range(0, 29) n on n.x < datediff(day, r.arrival_date, r.departure_date)`; then `alter table reservation alter column nightly_rate set null`. Do not edit V1–V4
- [ ] T002 Run `./mvnw compile` so Flyway applies V5 and jOOQ generates `SEASONAL_RATE`, `RESERVATION_NIGHT`, `SEASONAL_RATE_SEQ`
- [ ] T003 [P] Write `test/reservation/SeasonalRateMigrationTest.java` (plain JUnit, own in-memory H2 `jdbc:h2:mem:v5test`, same pattern as `CancellationRuleMigrationTest`): Flyway `target("4")`, insert a `room_type`, a `room` and a 3-night reservation at `nightly_rate` 100 / `total_price` 300; migrate to V5; assert 3 `reservation_night` rows for the arrival date and the next two dates, each 100, and none for the departure date (FR-008, US3/AC2)

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Manager sets a seasonal rate for a date period (Priority: P1) 🎯 MVP

**Goal**: The Manager maintains named seasonal rates per room type; overlaps are impossible.

**Independent Test**: As Manager add "Summer" to a room type, see it in the grid; an overlapping one and one
without a name are rejected; Receptionist and Housekeeping get access denied.

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T004 [P] [US1] Write `test/room/SeasonalRateServiceTest.java` (`@SpringBootTest`, `@Transactional`, room types from `RoomTypeServiceTest.roomType`): save and `findAll()` sorted by room type name, then start date (US1/AC1); change of name, dates and rate, and delete (US1/AC2); rejected with `Name is required.` (blank name), `Start date and end date are required.`, `End date must not be before start date.`, `Nightly rate must be greater than 0.` (0 and negative), `Room type is required.` (US1/AC4); an overlap with "Summer" of the same room type is rejected with `Overlaps the seasonal rate "Summer" (1 July 2027 – 31 August 2027).` (dates formatted `d MMMM yyyy`, `Locale.ENGLISH`) for a period touching only its first or last night (US1/AC3, AC5); the same period for another room type and a change of a rate onto its own period are accepted; a period entirely in the past is accepted (clarification); deleting an unused room type deletes its seasonal rates (edge case)
- [ ] T005 [P] [US1] Write `test/room/SeasonalRateViewTest.java` (extends `HrsBrowserlessTest`, logged in as Manager): grid id `seasonal-rates` shows Room type, Name, Start date, End date, Nightly rate; the form saves a new rate and it appears in the grid; Delete is disabled without a selection and deletes the selected row; an overlap shows an error notification with the service message; Receptionist and Housekeeping are denied access to `manager/seasonal-rates` (US1/AC1–AC6, contracts/ui-changes.md)
- [ ] T006 [P] [US1] Update `test/room/RoomTypeViewTest.java`, `test/room/RoomTypeServiceTest.java` and `test/room/ManagerIT.java`: label `Nightly rate` → `Standard nightly rate`, message `Nightly rate must be greater than 0.` → `Standard nightly rate must be greater than 0.` (FR-001, research R8)
- [ ] T007 [P] [US1] Write `test/room/SeasonalRatesIT.java` (extends `PlaywrightIT`): log in as `manager`, open "Seasonal rates" from the menu, add a seasonal rate "Test Summer" for an existing room type, see it in the grid, then delete it (US1/AC1–AC2)

### Implementation for User Story 1

- [ ] T008 [US1] Create `main/room/SeasonalRateService.java` (`@Service`, `@Transactional`, pattern of `RoomTypeService`): `findAll()` joined with `ROOM_TYPE` ordered by `ROOM_TYPE.NAME`, `SEASONAL_RATE.START_DATE`; `save(SeasonalRateRecord)` checks in this order: room type set (`Room type is required.`), name not blank and "`name` varchar(50), not null" (`Name is required.`), both dates set, `end_date >= start_date`, `nightly_rate > 0`; then locks the room type row with `select … for update`, rejects an overlap (`start_date <= other.end_date and end_date >= other.start_date`, same room type, other id) with the message from data-model.md naming the other rate; inserts with `SEASONAL_RATE_SEQ` or updates; `delete(long id)`. No check against today (past dates allowed)
- [ ] T009 [US1] Create `main/room/SeasonalRateView.java`: `@Route(value = "manager/seasonal-rates", layout = MainLayout.class)`, `@PageTitle("Seasonal rates | Hotel Reservation System")`, `@Menu(title = "Seasonal rates", order = 52.5)`, `@RolesAllowed("MANAGER")`; built like `RoomTypeView`: grid id `seasonal-rates` (Room type name, Name, Start date, End date, Nightly rate), form with `ComboBox` Room type (from `RoomTypeService.findAll()`), `TextField` Name (max length 50), `DatePicker` Start date and End date, `BigDecimalField` Nightly rate; binder `asRequired` for each field; buttons Save, Delete (enabled only with a selection), New seasonal rate; `BusinessRuleException` → `Notifications.showError`
- [ ] T010 [US1] In `main/room/RoomTypeView.java` rename header, field label and required message to "Standard nightly rate"; in `main/room/RoomTypeService.java` change the message to `Standard nightly rate must be greater than 0.`
- [ ] T011 [US1] Run `./mvnw verify` — must end green

**Checkpoint**: Seasonal rates can be maintained (they do not affect prices yet)

---

## Phase 4: User Story 2 - Prices follow the rate of each night (Priority: P1)

**Goal**: Search, booking and the reservation views price every night on its own and save the night rates.

**Independent Test**: Standard 100, "Summer" 150 from day 10 to day 20: a search day 12–15 costs 450, day 9–11
costs 250; a booked stay day 9–12 shows "1 night × 100.00, 2 nights × 150.00" and 400.00.

### Tests for User Story 2 ⚠️

- [ ] T012 [P] [US2] Write `test/reservation/NightRateTest.java` (plain JUnit): `pricePerNight` gives `1 night × 100.00, 2 nights × 150.00`; `3 nights × 120.00`; non-consecutive equal rates stay separate (`1 night × 150.00, 1 night × 100.00, 1 night × 150.00`); `total` sums the rates (contracts/ui-changes.md)
- [ ] T013 [P] [US2] Update `test/reservation/AvailabilityServiceTest.java`: with a seasonal rate 150 on a room type with standard rate 100 from `today + 10` to `today + 20`, `search` for days 12–15 has total 450, for days 9–11 total 250 (night of day 9 standard, day 10 seasonal), a stay ending on day 21 prices day 20 seasonal and nothing for the departure day; a seasonal rate 80 lower than the standard rate is used; another room type's seasonal rate is ignored; `nightRates` returns one `NightRate` per night in date order (US2/AC1–AC3, FR-004)
- [ ] T014 [P] [US2] Update `test/reservation/ReservationServiceTest.java`: `book` across the season start stores one `RESERVATION_NIGHT` row per night with its rate and `total_price` = their sum (US2/AC4, FR-006); replace the `getNightlyRate()` assertions in `bookingCreatesBookedReservation` (and similar) by night-row assertions; `find` returns the nights for each found reservation
- [ ] T015 [P] [US2] Update `test/reservation/GuestReservationServiceTest.java`: `findForGuest` returns the nights of the reservation in date order and the total as their sum (US2/AC5)
- [ ] T016 [P] [US2] Update `test/guest/GuestBookingViewTest.java`: the search grid has no "Nightly rate" column and shows the total across a season boundary; the confirmation email (mocked `JavaMailSender`) shows that total (US2/AC4, FR-005)
- [ ] T017 [P] [US2] Update `test/reservation/ReservationViewTest.java`: the search grid has no "Nightly rate" column; the found-reservations grid has a column "Price per night" before "Total price" with `1 night × 100.00, 2 nights × 150.00` for a stay across a season start (US2/AC5)
- [ ] T018 [P] [US2] Update `test/guest/MyReservationViewTest.java`: the lookup shows the form item "Price per night" with the grouped text before "Total price" (US2/AC5)
- [ ] T019 [US2] Extend `test/room/SeasonalRatesIT.java`: after adding the seasonal rate, a Guest search on `/` for a stay inside it shows the total at the seasonal rate (US2/AC1)

### Implementation for User Story 2

- [ ] T020 [US2] Create `main/reservation/NightRate.java`: `record NightRate(LocalDate night, BigDecimal rate)` with `static BigDecimal total(List<NightRate>)` and `static String pricePerNight(List<NightRate>)` grouping consecutive equal rates, `"1 night"` singular, rates with `toPlainString()` (research R6)
- [ ] T021 [US2] In `main/reservation/AvailabilityService.java` add `List<NightRate> nightRates(long roomTypeId, LocalDate arrival, LocalDate departure)`: standard rate of the room type plus its seasonal rates with `START_DATE.lt(departure)` and `END_DATE.ge(arrival)`, one entry per night from arrival to the day before departure (FR-004); in `search` read the seasonal rates of all room types for the stay with one query, price each room from them and set `totalPrice` = `NightRate.total`; remove `nightlyRate` from `main/reservation/AvailableRoom.java` (research R3)
- [ ] T022 [US2] In `main/reservation/ReservationService.java` `book`: get `nightRates` for the room's type, set `total_price` = sum, stop setting `nightly_rate`, and insert one `RESERVATION_NIGHT` row per night after the reservation row; add `List<NightRate> nights(long reservationId)` ordered by `NIGHT_DATE`. Keep `change` working with its current behavior for now, but make it write night rows too (replaced by T028)
- [ ] T023 [US2] Add `List<NightRate> nights` to `main/reservation/FoundReservation.java` and `main/reservation/GuestReservation.java`; `ReservationService.find` reads the night rows of all found reservations with one query (`RESERVATION_ID.in(...)`) and groups them; `findForGuest` reads them with `nights(id)`
- [ ] T024 [US2] In `main/guest/GuestBookingView.java` and `main/reservation/ReservationView.java` remove the "Nightly rate" search column; in `ReservationView` add the found-grid column "Price per night" (`NightRate.pricePerNight(f.nights())`) before "Total price"; in `main/guest/MyReservationView.java` add the form item "Price per night" before "Total price" (contracts/ui-changes.md)
- [ ] T025 [US2] Run `./mvnw verify` — must end green

**Checkpoint**: Guests and reception pay seasonal prices

---

## Phase 5: User Story 3 - Existing reservations keep their price (Priority: P1)

**Goal**: Saved night rates never change through rate maintenance; a reception change keeps the nights the
reservation already had.

**Independent Test**: Book day 10–12 at 120, add a seasonal rate 150 from day 10 to day 20: the price stays
240; extend to day 13: 390.

### Tests for User Story 3 ⚠️

- [ ] T026 [P] [US3] Add to `test/reservation/ReservationServiceTest.java`: after booking, adding, changing and deleting a seasonal rate over its nights and changing the standard rate leave its night rows and total unchanged (US3/AC1, SC-003); booked day 10–12 at 120, seasonal 150 added from day 10, extended to day 13 → rows 120, 120, 150 and total 390 (US3/AC3); moved to a room of another room type → every night at that type's current rates (US3/AC4); moved to another room of the same type, same dates → unchanged (US3/AC5); shortened by one night → that night's row is gone, the others keep their rates (edge case); update `changeToFreeNightsRecalculatesWithCurrentRate` to the new rule (nights 1–2 at 120, change to 2–5 after the standard rate became 150 → 120 + 150 + 150 = 420) and `changeOfGuestDetailsKeepsBookedRate` to night-row assertions (FR-007)
- [ ] T027 [P] [US3] Add to `test/guest/MyReservationViewTest.java`: a reservation inserted with `nightly_rate` only and night rows like V5 creates them shows "Price per night: 2 nights × 120.00" (US3/AC2, FR-008)

### Implementation for User Story 3

- [ ] T028 [US3] In `main/reservation/ReservationService.java` remove the `reprice` parameter of `apply`; build the new night set as research R4: same room type → saved rate for nights the reservation already had, `nightRates` rate for new nights; other room type → `nightRates` of the new type for every night; delete the reservation's `RESERVATION_NIGHT` rows, insert the new set, set `total_price` to the sum; update the Javadoc of `change` (003 FR-007 replaces 001 US6/AC1); this replaces the interim `change` code of T022
- [ ] T029 [US3] Run `./mvnw verify` — must end green

**Checkpoint**: Prices are frozen per night

---

## Phase 6: User Story 4 - Revenue reflects the rate of each night (Priority: P2)

**Goal**: Monthly revenue and room-nights come from the night rows.

**Independent Test**: A checked-out stay 30 June – 2 July with nights 100 and 150: June revenue 100, July 150.

### Tests for User Story 4 ⚠️

- [ ] T030 [US4] Update `test/report/ReportServiceTest.java`: helper `stay(...)` inserts one `RESERVATION_NIGHT` row per night at `rate` (and no longer sets `NIGHTLY_RATE`); new overload or test with per-night rates: a checked-out stay 30 June – 2 July 2029 with rates 100 and 150 → June revenue 100, July revenue 150, one room-night each (US4/AC1, FR-009); existing month-split and status tests stay green

### Implementation for User Story 4

- [ ] T031 [US4] In `main/report/ReportService.java` replace the loop over stays: `count(*)` and `sum(RESERVATION_NIGHT.NIGHTLY_RATE)` of night rows joined with `RESERVATION` where status is checked in or checked out and `NIGHT_DATE >= first and < next` (research R7); revenue `0` when there are no rows
- [ ] T032 [US4] Run `./mvnw verify` — must end green

**Checkpoint**: All user stories functional

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Drop the old column, keep feature 001 documents true, validate end to end

- [ ] T033 Create `src/main/resources/db/migration/V6__drop_reservation_nightly_rate.sql`: `alter table reservation drop column nightly_rate;` then `./mvnw compile` and remove every remaining `RESERVATION.NIGHTLY_RATE` / `getNightlyRate` / `setNightlyRate` use of the reservation in `main/` and `test/` (research R2)
- [ ] T034 Update `test/reservation/CancellationRuleMigrationTest.java`: the reservation inserted after the full migration no longer names `nightly_rate` (keep it in the V3-era insert); `SeasonalRateMigrationTest` keeps its `target("4")`/`target("5")` steps
- [ ] T035 [P] In `specs/001-hotel-reservation-system/spec.md` mark FR-001, FR-009, User Story 1 scenario 5, User Story 6 scenario 1, User Story 7 scenario 1 and the assumption "The nightly rate is the same for every night" as "Changed by 003-seasonal-room-rates", without rewriting their text
- [ ] T036 [P] In `specs/001-hotel-reservation-system/contracts/ui-routes.md` add the row `manager/seasonal-rates` | `SeasonalRateView` | MANAGER | 003 US1 | create, change, delete seasonal rates
- [ ] T037 Walk through `specs/003-seasonal-room-rates/quickstart.md` steps 2–7 and 9 by hand
- [ ] T038 Run `./mvnw verify` — must end green

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Empty
- **Foundational (Phase 2)**: BLOCKS all user stories (T002 needs T001; T003 after T002)
- **User Stories (Phase 3+)**: After Foundational
- **Polish (Phase 7)**: After all user stories (V6 needs every reader moved to night rows)

### User Story Dependencies

- **US1 (P1)**: After Foundational
- **US2 (P1)**: After US1 (prices read the seasonal rates that US1 lets the Manager enter; T019 extends US1's IT)
- **US3 (P1)**: After US2 (changes the night rows that `book` writes in T022)
- **US4 (P2)**: After Foundational; independent of US1–US3 (its test inserts night rows directly), but run
  after US2 so newly booked stays have night rows

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Service before views
- Story ends with `./mvnw verify`

### Parallel Opportunities

- T004, T005, T006, T007 together
- T012–T018 together (different test files); T019 after T007
- T026, T027 together
- US4 (T030–T032) next to US3, different files
- T035, T036 together

---

## Parallel Example: User Story 2

```bash
Task: "Write NightRateTest for the price per night text"
Task: "Update AvailabilityServiceTest for nights across a season boundary"
Task: "Update ReservationViewTest for the Price per night column"
Task: "Update MyReservationViewTest for the Price per night item"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 2 (Foundational)
2. Phase 3 (US1) → `./mvnw verify`
3. **STOP and VALIDATE**: the Manager can enter seasons; prices do not use them yet, so do not release US1
   alone

### Incremental Delivery

1. Foundational
2. US1 + US2 + US3 → release candidate (seasonal prices with frozen night rates)
3. US4 → revenue per night
4. Polish → old column dropped, 001 documents updated

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Verify tests fail before implementing
- Commit only after the user approves
