---

description: "Task list for feature 002-cancellation-deadline-48h"
---

# Tasks: Online Cancellation Deadline 48 Hours

**Input**: Design documents from `/specs/002-cancellation-deadline-48h/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-changes.md,
contracts/confirmation-email.md, quickstart.md

**Tests**: Included. Constitution principle IV requires a Browserless test per changed view, a Playwright
`*IT` per feature, and a test for every scenario. Tests come first in each story and must fail before the
implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- `main/` = `src/main/java/ch/martinelli/edu/hrs/`, `test/` = `src/test/java/ch/martinelli/edu/hrs/`
- Resources: `src/main/resources/`, `src/test/resources/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Mail dependency and settings (research R4)

- [X] T001 Add `spring-boot-starter-mail` (version from the Spring Boot parent) to `pom.xml`
- [X] T002 [P] Add mail settings to `src/main/resources/application.properties`: `spring.mail.host=localhost`, `spring.mail.port=25`, `hrs.mail.from=reservations@hotel.example`, and 5 s timeouts `spring.mail.properties.mail.smtp.connectiontimeout=5000`, `spring.mail.properties.mail.smtp.timeout=5000`, `spring.mail.properties.mail.smtp.writetimeout=5000`
- [X] T003 [P] Add `spring.mail.host=localhost`, `spring.mail.port=25`, `hrs.mail.from=reservations@hotel.example` and the same timeouts to `src/test/resources/application.properties`, so every test context has a `JavaMailSender` bean

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The stored rule and the one place that computes the deadline (data-model.md, research R1–R3)

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Create `src/main/resources/db/migration/V4__reservation_cancellation_hours.sql`: `alter table reservation add column cancellation_hours_before int default 15 not null;` then `alter table reservation alter column cancellation_hours_before set default 48;` ("`15` for reservations that existed before this feature", "`48` for every reservation booked afterwards"); do not edit V3
- [X] T005 Run `./mvnw compile` so Flyway applies V4 and jOOQ regenerates `ReservationRecord` with `getCancellationHoursBefore()`
- [X] T006 [P] Write `test/reservation/CancellationDeadlineTest.java` (plain JUnit, explicit `now`): arrival 12 March 2030 with 48 → deadline 10 March 2030 15:00; with 15 → 12 March 2030 00:00; allowed at 14:59, rejected at exactly 15:00 (exclusive); a DST weekend between (arrival 31 March 2030, 48 → 29 March 2030 15:00, wall clock); `text()` = `Free online cancellation until 10 March 2030, 15:00 (Zurich time)`; `passedText()` = `Online cancellation was possible until 10 March 2030, 15:00 (Zurich time). Please call reception to cancel.`
- [X] T007 Create `main/reservation/CancellationDeadline.java`: record with `LocalDateTime deadline`; `static of(LocalDate arrival, int hoursBefore)` = `arrival.atTime(15, 0).minusHours(hoursBefore)`; `static ZoneId ZONE = Europe/Zurich`; `allowsOnlineCancellationAt(LocalDateTime now)` = `now.isBefore(deadline)`; `text()` and `passedText()` with pattern `d MMMM yyyy, HH:mm`, `Locale.ENGLISH` (data-model.md)

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - New bookings can be cancelled online until 48 hours before check-in (Priority: P1) 🎯 MVP

**Goal**: Every reservation booked from now on gets the 48-hour rule and is enforced online.

**Independent Test**: Book, cancel online more than 48 hours before check-in (succeeds); book another one
within 48 hours and try to cancel (rejected, "Please call reception to cancel.").

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T008 [P] [US1] Update `test/reservation/GuestReservationServiceTest.java`: `book()` (online and reception source) stores `cancellation_hours_before = 48`; cancel of a reservation arriving in 5 days succeeds; a reservation arriving tomorrow or the day after has `cancellable = false` and `cancelByGuest` throws with `passedText()`; replace the old "until the day before arrival" assertions (US1 scenarios 1–3, FR-001, FR-002, FR-004)
- [X] T009 [P] [US1] Update `test/guest/MyReservationViewTest.java`: a reservation arriving tomorrow shows the paragraph `cancellation-deadline` with `passedText()` and no "Cancel reservation" button; one arriving in 5 days shows the button and can be cancelled (US1 scenarios 1, 3)
- [X] T010 [P] [US1] Update `test/guest/GuestCancelIT.java` (Playwright): book online with an arrival at least 3 days ahead, look up, cancel, see "Your reservation is cancelled." (the deadline text is checked in US3, T021)

### Implementation for User Story 1

- [X] T011 [US1] In `main/reservation/ReservationService.java`, `book()` sets `reservation.setCancellationHoursBefore(48)` (constant `ONLINE_CANCELLATION_HOURS = 48`); `change()` must not touch the column (FR-008)
- [X] T012 [US1] Add `CancellationDeadline cancellationDeadline` to `main/reservation/GuestReservation.java`; in `ReservationService.findForGuest` select `ARRIVAL_DATE` and `CANCELLATION_HOURS_BEFORE`, build the deadline, `cancellable = BOOKED && deadline.allowsOnlineCancellationAt(now)` with `now = LocalDateTime.now(clock.withZone(CancellationDeadline.ZONE))`
- [X] T013 [US1] In `ReservationService.cancelByGuest` replace the `arrival.isAfter(today)` check with `deadline.allowsOnlineCancellationAt(now)`, message `deadline.passedText()`; update the Javadoc (002 FR-004 replaces 001 FR-015)
- [X] T014 [US1] In `main/guest/MyReservationView.java` replace the "only possible until the day before arrival" paragraph with a `Paragraph` id `cancellation-deadline` showing `passedText()` for booked reservations that are not cancellable
- [X] T015 [US1] Run `./mvnw verify` — must end green

**Checkpoint**: New bookings follow the 48-hour rule

---

## Phase 4: User Story 2 - Existing bookings keep the deadline they were promised (Priority: P1)

**Goal**: Reservations from before the change keep "until 00:00 on the arrival date", also after a date change.

**Independent Test**: A reservation with `cancellation_hours_before = 15` arriving tomorrow can still be
cancelled online; on its arrival date it cannot.

### Tests for User Story 2 ⚠️

- [X] T016 [P] [US2] Add to `test/reservation/GuestReservationServiceTest.java`: set `cancellation_hours_before = 15` on a booked row (via `DSLContext`); arrival tomorrow → `cancellable = true` and `cancelByGuest` succeeds; arrival today → rejected with `passedText()` showing today 00:00 (US2 scenarios 1–2, FR-003)
- [X] T017 [P] [US2] Add to `test/reservation/ReservationServiceTest.java`: `change()` moving the arrival by 7 days keeps `cancellation_hours_before` (15 stays 15, 48 stays 48) and the deadline moves 7 days (FR-008, edge case); `cancel()` by reception of a booked reservation arriving tomorrow with rule 48 succeeds (FR-007)
- [X] T018 [P] [US2] Write `test/reservation/CancellationRuleMigrationTest.java` (plain JUnit, own in-memory H2 `jdbc:h2:mem:v4test`): Flyway `target("3")` migrate, insert one `room_type`, `room` and `reservation` row, then migrate to latest; assert `cancellation_hours_before = 15` for that row and `48` for a row inserted afterwards without the column (FR-003, SC-002)

### Implementation for User Story 2

- [X] T019 [US2] No production change expected beyond T004 and T011; if T016/T017/T018 fail, fix the cause in `main/reservation/ReservationService.java`
- [X] T020 [US2] Run `./mvnw verify` — must end green

**Checkpoint**: Old and new rule both enforced

---

## Phase 5: User Story 3 - Guest sees the exact last cancellation time on the lookup page (Priority: P2)

**Goal**: Guests and Receptionists see the exact deadline text.

**Independent Test**: Look up a booked reservation; the shown text matches its deadline.

### Tests for User Story 3 ⚠️

- [X] T021 [P] [US3] Add to `test/guest/MyReservationViewTest.java`: booked, deadline not passed → `cancellation-deadline` shows `Free online cancellation until <d MMMM yyyy>, 15:00 (Zurich time)`; cancelled reservation → no `cancellation-deadline` paragraph and no button (US3 scenarios 1–3, FR-005); and in `test/guest/GuestCancelIT.java` (Playwright): after the lookup, `#cancellation-deadline` shows "Free online cancellation until <arrival − 2 days>, 15:00 (Zurich time)" before cancelling
- [X] T022 [P] [US3] Add to `test/reservation/ReservationViewTest.java` (Receptionist): found grid has column "Online cancellation" with `text()` for a booked future reservation, the first sentence of `passedText()` for a passed one, empty for a cancelled one (US3 scenario 4, FR-009)

### Implementation for User Story 3

- [X] T023 [US3] In `main/guest/MyReservationView.java` show the `cancellation-deadline` paragraph with `text()` above the "Cancel reservation" button when cancellable; keep `passedText()` from T014 otherwise; nothing for non-booked
- [X] T024 [US3] In `main/reservation/ReservationView.java` add grid column "Online cancellation" after "Status": for `BOOKED` rows build `CancellationDeadline.of(arrivalDate, cancellationHoursBefore)` and show `text()` or `Online cancellation was possible until <time> (Zurich time).`; empty otherwise (contracts/ui-changes.md)
- [X] T025 [US3] Run `./mvnw verify` — must end green

**Checkpoint**: Deadline visible to Guest and Receptionist

---

## Phase 6: User Story 4 - Guest sees the last cancellation time in the confirmation email (Priority: P3)

**Goal**: A plain-text confirmation email after each online booking; mail failures never affect the booking.

**Independent Test**: Book online with a mocked mail sender: one message with the contract content; make it
throw: booking saved and confirmation shown.

### Tests for User Story 4 ⚠️

- [X] T026 [P] [US4] Write `test/reservation/BookingConfirmationEmailTest.java` (`@SpringBootTest`, `@MockitoBean JavaMailSender`): message from `hrs.mail.from`, to the booking email, subject `Your reservation <number>`, body with reservation number, room type, arrival, departure, guests, total price and `text()` (contracts/confirmation-email.md); sender throws `MailSendException` → `send` returns normally (US4 scenarios 1–2)
- [X] T027 [P] [US4] Add to `test/guest/GuestBookingViewTest.java` (`@MockitoBean JavaMailSender`): online booking sends exactly one message; with the sender throwing, the reservation exists and the confirmation is shown (US4 scenarios 1–2, FR-006)
- [X] T028 [P] [US4] Add to `test/reservation/ReservationViewTest.java` (`@MockitoBean JavaMailSender`): a reception booking sends no message (US4 scenario 3)

### Implementation for User Story 4

- [X] T029 [US4] Create `main/reservation/BookingConfirmationEmail.java` (`@Service`, `JavaMailSender`, `@Value("${hrs.mail.from}")`): `send(ReservationRecord reservation, String roomTypeName)` builds the `SimpleMailMessage` per contracts/confirmation-email.md; catches `MailException` and logs a warning with the reservation number
- [X] T030 [US4] In `main/guest/GuestBookingView.java` inject `BookingConfirmationEmail` and call `send(reservation, room.roomTypeName())` after `reservationService.book(...)` returned and before showing the confirmation; `ReservationView` is not changed
- [X] T031 [US4] Run `./mvnw verify` — must end green

**Checkpoint**: All user stories functional

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Keep feature 001 documents true and validate end to end

- [X] T032 [P] In `specs/001-hotel-reservation-system/spec.md` mark FR-015 and User Story 7 scenarios 3–4 as "Changed by 002-cancellation-deadline-48h (FR-002–FR-005)", without rewriting their text
- [X] T033 [P] In `specs/001-hotel-reservation-system/contracts/ui-routes.md` change the `my-reservation` row from "cancel until day before arrival" to "cancel until the online cancellation deadline (see 002)"
- [X] T034 Walk through `specs/002-cancellation-deadline-48h/quickstart.md` steps 2–7 by hand
- [X] T035 Run `./mvnw verify` — must end green

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: After Setup - BLOCKS all user stories (T005 needs T004; T007 after T006)
- **User Stories (Phase 3+)**: After Foundational
- **Polish (Phase 7)**: After all user stories

### User Story Dependencies

- **US1 (P1)**: After Foundational
- **US2 (P1)**: After US1 (uses `findForGuest`/`cancelByGuest` from T012/T013)
- **US3 (P2)**: After US1 (extends the paragraph from T014); independent of US2
- **US4 (P3)**: After Foundational and Setup; independent of US1–US3 except for `CancellationDeadline.text()`

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Service before views
- Story ends with `./mvnw verify`

### Parallel Opportunities

- T002, T003 together
- T008, T009, T010 together; T016, T017, T018 together; T021, T022 together; T026, T027, T028 together
- US4 can run next to US2/US3 (different files except `ReservationViewTest`, which T022 and T028 both touch: do them one after the other)
- T032, T033 together

---

## Parallel Example: User Story 1

```bash
Task: "Update GuestReservationServiceTest for the 48-hour rule"
Task: "Update MyReservationViewTest for the passed-deadline text"
Task: "Update GuestCancelIT to book at least 3 days ahead"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 + Phase 2
2. Phase 3 (US1) → `./mvnw verify`
3. **STOP and VALIDATE**: US1 alone already keeps old rows on the old rule (V4 sets 15); do not release
   it before US2's tests prove that

### Incremental Delivery

1. Setup + Foundational
2. US1 + US2 → release candidate (rule change and the promise to existing guests)
3. US3 → deadline visible
4. US4 → confirmation email
5. Polish → 001 documents updated

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Verify tests fail before implementing
- Commit only after the user approves

---

## Phase 8: Convergence

Scope of this convergence run: Setup, Foundational and User Story 1 (T001–T015).

- [X] T036 [P] Add a Browserless test to `test/guest/MyReservationViewTest.java`: look up a reservation whose "Cancel reservation" button is shown, then set its `cancellation_hours_before` so the deadline has passed (via `DSLContext`), confirm the cancel dialog, and assert an error notification with `CancellationDeadline.passedText()` and status still `BOOKED` per contracts/ui-changes.md (cancel rejected), US1/AC2, Constitution IV (partial)

---

## Phase 9: Convergence

Scope of this convergence run: User Story 4 (T026–T031).

- [X] T037 Review the email branch that sends `passedText()` when a booking is made after its own deadline (booked less than 48 hours before check-in); keep it and record the case in `specs/002-cancellation-deadline-48h/contracts/confirmation-email.md`, or remove it per FR-006, contracts/confirmation-email.md (unrequested)
