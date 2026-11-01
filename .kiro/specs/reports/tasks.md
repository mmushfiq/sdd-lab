# Implementation Plan

Prerequisites: foundation (security, staff layout and menu, `Clock` bean, test base classes with the fixed clock
2026-06-01, test accounts `manager1`, `reception1`, `housekeeper1`), reservation-core (reservation schema `V10`,
`ReservationStatus`) and reception (stay schema `V50`) are implemented. No main migration is added. No task is marked
(P): every task runs the same Maven build, which rebuilds the shared jOOQ code-generation database and the shared test
database, and tasks 2.1–2.3 extend the same service and test class.

- [ ] 1. Add the reports test data
  - Test migration version 170 inserts the nine reservations 8000–8008 and the five stays from the design's test-data table: rooms resolved by room number, codes `TEST<id>`, one guest each, the documented booked rates (8002 booked at 100.00 although its room type costs 90.00), no guest rows and no open stay, all dates inside 2026-01-01 – 2026-04-30
  - Creates the report service test with one case: the nine reservations and five stays exist with their documented statuses, dates, rates and stay days
  - Done when that test case passes on a clean build and every other test already in the build still passes with the new rows present (the rows lie outside every other spec's date window and contain no open stay)
  - _Requirements: 3.2, 3.4, 3.5, 4.2_

- [ ] 2. Report computation in the service
- [ ] 2.1 Period validation, monthly rows and available room nights
  - Contract types for a report (month rows and total row) and the invalid-period exception
  - Read-only repository counts the rooms; the service validates the period against today from the shared clock (missing night, last before first, last after today) with the documented messages before any query
  - Build one row per calendar month of the period, clipped to the period, plus the total row over the whole period; nights include first and last night; available room nights = rooms × nights; occupied nights and revenue are zero until 2.2 and 2.3
  - Service test cases: Jan–Apr 2026 gives four rows 2026-01 … 2026-04 and a total with 31·R, 28·R, 31·R, 30·R and 120·R available room nights (R read at test time); 2026-01-31 – 2026-02-01 gives two one-night rows; with test-only room 8900 (number `T8900`) added the January row shows (R + 1) × 31 (room removed afterwards); the three refusals with their messages ("2026-06-01" named) and last night = today accepted
  - Done when these cases pass
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 3.1_

- [ ] 2.2 Occupied nights and occupancy rate
  - Repository reads the stays overlapping the period in one query: reservations Checked in or Checked out, joined with their stay record when present, only the columns of the design, never guest data
  - Service derives each stay's occupied range: recorded check-in day to check-out day, an open stay up to and including tonight, no stay record → arrival to departure; sums the overlap per row; occupancy rate in percent rounded half up to one decimal, empty when a row has no available room nights
  - Service test cases: occupied nights 4, 3, 4, 5 and 16 for Jan–Apr and total, and the documented occupancy rates; night 2026-02-12 occupied 1 and night 2026-02-13 occupied 0; with the test-only open stay 8100, April 25–30 counts 3 occupied nights and 2026-04-27 – 2026-06-01 counts 35 more than without it (row removed afterwards); the rounding cases 12.5, 33.3, 66.7, 6.3, 0.0 and empty for zero available nights
  - Done when these cases pass
  - _Requirements: 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- [ ] 2.3 Revenue, read-only behaviour and guest data independence
  - Service adds, per row, the booked nightly rate for each booked night (arrival to night before departure) of every stay in the row, as an exact two-decimal sum starting at 0.00; Reserved and Cancelled reservations earn nothing
  - The report runs in a read-only transaction and the repository offers no write operation
  - Service test cases: revenue 440.00, 590.00, 520.00, 770.00 and 2320.00 for Jan–Apr and total; nights 2026-02-12 and 2026-02-13 each 100.00 (booked rate, early check-out keeps revenue); April 25–30 with 8100 gives 380.00 and the 2026-04-27 – 2026-06-01 difference is +380.00; reservation and stay rows 8000–8999 and the room count are identical before and after a report; February figures are identical with and without a test-only guest row for 8002 (removed afterwards)
  - Done when these cases and all earlier report service test cases pass
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.3_

- [ ] 3. Report page
- [ ] 3.1 Reports page with default period, grid, explanation and access
  - Staff page "Reports" in the staff layout, menu order 90, managers only; on every navigation it sets the first and last night of the previous calendar month from the shared clock and shows that report
  - Period pickers, "Show report" action, one full-width grid with all rows visible (Month, Room nights, Occupied nights, Occupancy, Revenue; month rows then "Total"), no guest column, and the explanation text of the design
  - View test cases: the manager opens the page with 2026-05-01 – 2026-05-31 and sees the rows "2026-05" and "Total" and the explanation; Jan–Apr 2026 shows the rows 2026-01 … 2026-04 and Total with January's available room nights, occupied nights 4 and revenue 440.00 and Total revenue 2320.00; the grid headers contain no guest column; the manager's menu lists "Reports"; receptionist and housekeeper get the not-available view and no menu entry; an anonymous visitor ends on the login page
  - Done when these view test cases pass
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.6, 2.1, 5.3_

- [ ] 3.2 Period messages, value formatting and refresh
  - Show the invalid-period message in the page and hide the grid; hide the message after a successful report
  - Format the occupancy rate as "<rate> %" or "–" when empty, and revenue with two decimals and no currency sign
  - View test cases: messages for a missing last night, last night before first night and last night 2026-06-02 (naming 2026-06-01) with the grid hidden; the rate formatter returns "12.5 %" and "–"; revenue cells read like "590.00"; after inserting the test-only open stay 8100 and showing again, April's occupied nights increase (row removed afterwards); navigating away and back restores the default period
  - Done when these view test cases pass
  - _Requirements: 2.5, 2.6, 2.7, 3.6, 3.7, 4.5, 5.2_

- [ ] 4. Browser test of the manager journey
  - Real login as the test manager at 1280×800; open the reports page, choose 2026-01-01 – 2026-04-30 and show the report
  - The Total row shows revenue 2320.00 and the page does not scroll horizontally
  - Done when `./mvnw verify` passes with the new browser test counted in the failsafe reports
  - _Requirements: 1.5, 2.1_

- [ ] 5. Final validation
  - Review: production code of the report package writes nothing and never reads guest data; only the reports page calls the report service; every test class names its requirement ids; no main migration was added
  - Run the full build with all tests
  - Done when `./mvnw verify` passes and the surefire and failsafe reports list the three report test classes with no skipped tests
  - _Requirements: 5.1, 5.2, 5.3_
