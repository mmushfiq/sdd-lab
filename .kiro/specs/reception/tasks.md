# Implementation Plan

Prerequisites: foundation (security, staff layout and menu, `Clock` bean, test base classes with the fixed clock
2026-06-01, test accounts), reservation-core (reservation and guest schema `V10`, `ReservationService` with check-in,
check-out and search) and housekeeping (cleaning status `V20`, `HousekeepingService`) are implemented. No task is
marked (P): every task runs the same Maven build, which rebuilds the shared jOOQ code-generation database and the
shared test database, and the view tasks share the confirm-and-notify helper.

- [ ] 1. Stay schema and test data
- [ ] 1.1 Create the stay table
  - Main migration version 50 creates the stay record: one row per reservation (declared foreign key, removed with its reservation), the check-in day required, the check-out day optional and never before the check-in day, no personal data
  - Done when `./mvnw compile` succeeds and generates the stay jOOQ class
  - _Requirements: 9.1, 9.3_

- [ ] 1.2 Add the reception test data and the restore helper
  - Test migration version 150 inserts the eleven reservations 6000–6010 with their guests and the four stays from the design's test-data table, rooms resolved by room number, obviously fake guest data, dates inside 2026-05-25 – 2026-06-15
  - Test helper restores the statuses of 6000–6010, deletes every stay of 6000–6999 and re-inserts the four test stays, and marks all ten rooms clean through the housekeeping service
  - Creates the reception service test with one case: the eleven reservations and four stays exist with their documented statuses and days, and calling the restore helper leaves them unchanged
  - Done when that test case passes on a clean build
  - _Requirements: 9.1, 9.2_

- [ ] 2. Reception service
- [ ] 2.1 Arrivals, departures and room overview data
  - Contract types: the room-overview row (room, cleaning status, staying guest, reservation for tonight) and the check-in refusal with its four reasons
  - Stay repository reads the open stays with room and reservation dates; service builds the arrivals list (reserved reservations with a night on the day, upstream order), the departures list (open stays due on or before the day, by departure date and room number) and the room overview (all rooms from housekeeping, open stay, reserved reservation for tonight), loading guest details with one reservation search per call
  - Service test cases: the documented rows and order for 2026-06-01, 2026-06-03 and 2026-05-26; cancelled, checked-out and no-show reservations never on today's lists; the documented room-overview rows including 202 with both a stay and a reservation for tonight and 104 empty
  - Done when these cases pass and the stay repository is not visible outside the reception package
  - _Requirements: 1.2, 1.4, 2.1, 6.1, 6.2, 6.3_

- [ ] 2.2 Check-in with date, occupancy and clean checks
  - One transaction in the documented order: reservation-core status change, date window from the shared clock, no other open stay in the room, room clean, then record the open stay with today; no room parameter; no exception caught inside the transaction; no logging
  - Service test cases: 6000 and the late arrival 6002 check in with an open stay of 2026-06-01 and leave the arrivals list; refusals with reason, reservation still Reserved and no stay row for the dirty room 102 (room stays dirty), the occupied room 202, the early arrival 6003 (message names 2026-06-03) and the no-show 6010; cancelled 6008, a second check-in of 6000 and unknown id 6999 refused with the upstream reason; no refusal message contains a guest name
  - Done when these cases pass
  - _Requirements: 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 5.1, 9.1, 9.2, 9.3_

- [ ] 2.3 Check-out marking the room dirty
  - One transaction in the documented order: reservation-core status change, close the open stay with today, mark the room dirty; no date condition
  - Service test cases: 6004 checked out with the stay closed on 2026-06-01, room 105 not clean and on the cleaning list; early check-out of 6005 frees room 201 for 2026-06-02 in availability; overdue 6006 checks out and the following check-in of 6007 is refused as not clean; check-out of reserved 6000 and checked-out 6009 refused with 6009's stay days unchanged; with 6005's stay check-in day set to 2026-06-02 the check-out fails, 6005 stays checked in with an open stay and room 201 stays clean
  - Done when these cases pass
  - _Requirements: 4.3, 4.4, 4.5, 4.6, 5.2, 5.3, 5.5, 9.1_

- [ ] 2.4 Prove the behaviour under simultaneous requests
  - Concurrency test without a test-managed transaction, restoring the test data after every round
  - 10 rounds of two threads released together that check in 6000: exactly one succeeds, the other is refused with the status reason, exactly one stay exists
  - 10 rounds of the check-out of 6006 racing the check-in of 6007: 6007 is never checked in and has no stay, 6006 is checked out, room 202 is dirty
  - If a round fails, this task may change only the order of steps in the service; rule changes go back to 2.2 or 2.3
  - Done when the concurrency test passes in three consecutive runs
  - _Requirements: 5.3, 5.4, 5.5_

- [ ] 3. Reception pages
- [ ] 3.1 Arrivals and departures page with day choice, refresh and access
  - Staff page "Arrivals and departures" in the staff layout, menu order 30, receptionists only; reloads on every navigation
  - Day picker starting at today, refresh action, arrivals list with "Late arrival" note and "No arrivals" message, departures list with "Overdue" note and "No departures" message; full-width lists with every row visible
  - View test cases: today's rows and notes; the documented rows for 2026-06-03; the empty messages for 2026-06-10 arrivals and 2026-05-26 departures; a check-in made through the service disappears after refresh; the receptionist sees the "Arrivals and departures" menu entry; manager and housekeeper get the not-available view and no menu entry; an anonymous visitor ends on the login page
  - Done when these view test cases pass
  - _Requirements: 1.1, 1.3, 1.5, 2.2, 2.3, 2.4, 8.1, 8.2, 8.3_

- [ ] 3.2 Check-in and check-out actions with confirmation
  - Shared helper opens the check-in or check-out confirmation (guest name, room, dates; early-departure notice when today is before the departure date), calls the service only on confirm, shows exactly one notification (success text, the refusal message unchanged, or the "could not be completed" text on an infrastructure failure) and reloads the lists
  - The arrivals rows get "Check in", the departures rows "Check out"
  - View test cases: cancel changes nothing; confirm checks 6000 in with "Checked in to room 101." in two actions; dirty room 102 and occupied room 202 show their messages and the rows stay; a reservation checked in meanwhile shows the status message and disappears; 6004 checks out with the cleaning-list text and room 105 is on the cleaning list; 6005 on 2026-06-03 shows the early notice; overdue 6006 checks out; the forced stay-date failure shows "Check-out could not be completed. Nothing was changed."
  - Done when these view test cases pass
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.7, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.1, 5.2, 7.1, 7.2_

- [ ] 3.3 Room overview page
  - Staff page "Room overview" in the staff layout, menu order 31, receptionists only; reloads on every navigation and on refresh
  - One full-width row per room in room-number order with cleaning status, staying guest with departure date and "Overdue" note, reservation for tonight, and "Check out" through the shared helper only where a guest is staying
  - View test cases: ten rows with their statuses and the documented guest cells; a room marked dirty shows "Dirty" after reopening; checking out 6005 from row 201 shows the early notice and leaves the row without guest and dirty; refresh shows a check-in made through the service; the receptionist sees both reception menu entries; manager, housekeeper and anonymous are refused as in 3.1
  - Done when these view test cases pass
  - _Requirements: 4.2, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 7.2, 8.1, 8.2, 8.3_

- [ ] 4. Browser test of the receptionist journey
  - Real login as the test receptionist at 1280×800; on the arrivals and departures page check in room 101 and check out room 105, each with its confirmation, and see the rows disappear
  - On the room overview see room 105 dirty and the guest of 101 staying; neither page scrolls horizontally; restore the test data afterwards
  - Done when `./mvnw verify` passes with the new browser test counted in the failsafe reports
  - _Requirements: 3.2, 4.3, 7.1, 7.2, 7.3_

- [ ] 5. Final validation
  - Review: the service catches no exception inside its transactions and logs nothing; only reception views call the reception service; nothing in reception writes reservations, guests or cleaning statuses except through the upstream services; every test class names its requirement ids
  - Run the full build with all tests
  - Done when `./mvnw verify` passes and the surefire and failsafe reports list all reception test classes with no skipped tests
  - _Requirements: 3.8, 5.1, 5.2, 9.3_
