# Implementation Plan

- [ ] 1. Test data and shared building blocks
- [ ] 1.1 Add the front-desk test data and the view test base
  - Prerequisite: foundation (security dependencies, clock, staff layout with role-filtered menu, test accounts, fixed test clock 2026-06-01) and reservation-core tasks 1.1, 1.2, 2 and 3.1–3.6 are implemented; every view of this spec calls the reservation service and lives in the staff layout
  - Add the test migration (version 140) with the five reservations of the design's test-data table (ids 5000–5004, 3–12 May 2027, rooms resolved by room number, codes DESK5000–DESK5004, obviously fake guest data; 5003 without guest data, 5004 with phone only)
  - Add the abstract view test base on top of the Browserless test base: a helper that creates a reservation through the reservation service for a room number, dates and number of guests, and a cleanup after each test that deletes every reservation arriving 15–31 May 2027
  - Done when `./mvnw test` passes with the new migration applied (the existing context-load test still starts) and the base class compiles
  - _Requirements: 2.5, 4.2, 4.4_

- [ ] 1.2 Provide the shared date, amount and error-message helpers
  - Date picker factory with year-month-day typing and display and a component id; amount formatting with two decimals and no currency sign; a hidden error-message element in the error colour with show and clear helpers
  - Used by every view of this spec; no rule code and no logger
  - Done when `./mvnw compile` succeeds and the helper offers exactly these functions (behaviour is proven by the view tests in 2.x and 3.x)
  - _Requirements: 4.1, 5.2, 10.1_

- [ ] 2. Reservation page
- [ ] 2.1 Show one reservation under its stable address
  - Receptionist-only staff page at `reservations/<reservation id>` (digits only, no menu entry) that loads the reservation on every navigation and shows code, status, room, room type, dates, nights, guests, booked nightly rate, price and guest data, "Guest data erased" when erased and "—" for a missing contact
  - Offer "Change" (navigating to the change page address) and "Cancel reservation" only while Reserved; show "Reservation not found." for an unknown id; a link back to the reservation list
  - ReservationDetailViewTest display cases with the test data: all fields of 5000 and both actions; opening by path `reservations/5000`; no actions for 5001, 5002, 5003; erased guest for 5003; "—" e-mail for 5004; not found and back link for 5999
  - Done when these ReservationDetailViewTest cases pass
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 10.1_

- [ ] 2.2 Cancel a reservation after confirmation
  - "Cancel reservation" opens the confirmation "Cancel reservation <code>?" with "Cancel reservation" and "Keep reservation"; confirming cancels through the reservation service, reloads the page and notifies "Reservation <code> cancelled."; a refusal shows the service's message and reloads the current status
  - ReservationDetailViewTest cancel cases on a reservation created in the test (room 205, 20–22 May 2027): question text; keep leaves it Reserved; confirm shows Cancelled without actions, the notification names only the code, and the room is available again for those nights; a check-in through the service after opening the page makes the confirm show "A reservation with status Checked in cannot be cancelled." and the status Checked in
  - Done when the cancel cases and all 2.1 cases pass
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 10.1, 10.2_

- [ ] 3. Find, create and change pages
  - The sub-tasks touch separate view and test files and can be built in parallel; they share the Maven build directory, so run `./mvnw` builds one at a time
- [ ] 3.1 (P) Find reservations and open one by code
  - Receptionist-only page `reservations` with menu entry "Reservations" (order 10): guest name, "From" preset to today and "To"; Search (Enter in the search fields) lists the service's search result with code link, guest, room, arrival, departure, nights, guests and status; "No reservations found." when empty; a refused range shows the service's message and keeps the previous rows; selecting a row or its code link opens the reservation page
  - Reservation code field with Open (Enter in the code field) opening the reservation page, or "Reservation not found." on the list
  - ReservationListViewTest: defaults and DESK5000 listed on open; range 1–15 May 2027 lists 5000–5004 in order with all columns and "Guest data erased" for 5003; name "desk guest" lists 5000, 5001, 5002, 5004; a reservation created through the service appears like the others; "No reservations found."; range end equal to start refused with rows kept; code link opens 5000; " desk5004 " opens 5004; unknown code stays on the list with the message
  - Done when ReservationListViewTest passes
  - _Boundary: ReservationListView_
  - _Depends: 2.1_
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 3.1, 3.2, 9.2, 9.3, 10.1_

- [ ] 3.2 (P) Search availability for a new reservation
  - Receptionist-only page `reservations/new` with menu entry "New reservation" (order 11): arrival today (focused), departure tomorrow, one guest; Search (Enter in these fields) lists the available rooms with room, room type, capacity, nightly rate and price in a single-choice list
  - "No room is available for these dates." when empty; "Arrival and departure date are required." for a missing date; the service's message for any other refusal; any change of dates or guests discards the listed rooms and the choice
  - NewReservationViewTest search cases: defaults 2026-06-01, 2026-06-02, 1; 4–5 May 2027 for 2 guests lists exactly 105, 201, 202, 203, 204, 205 with their prices (103 and 104 are held); 5 guests empty message; missing date message; same-day departure, past arrival and 0 guests show the service's messages; changing the departure after a search empties the list and the choice
  - Done when these NewReservationViewTest cases pass
  - _Boundary: NewReservationView_
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 6.5, 9.1, 9.2, 10.1_

- [ ] 3.3 Reserve the chosen room for the guest
  - Not (P) with 3.2: extends the same page and test class
  - Guest name, e-mail and phone fields; Reserve (Enter in the guest fields) creates the reservation through the service for the searched values and chosen room, notifies "Reservation <code> created." and opens the reservation page; "Choose a room first." without a choice; a guest-data refusal keeps all values; "room not available" shows the message, searches again and keeps the guest data
  - NewReservationViewTest reserve cases (20–23 May 2027, room 203): created page shows Reserved, 3 nights, 390.00 and the notification contains the code but not the guest name; no choice stores nothing; name only shows "E-mail address or phone number is required." with values kept and nothing stored; room 203 taken through the service after the search gives "Room 203 is not available from 2027-05-20 to 2027-05-23.", only the service's reservation holds room 203, the list no longer shows 203 and the guest name is still filled
  - Done when all NewReservationViewTest cases pass
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 9.4, 10.1, 10.2_

- [ ] 3.4 (P) Change dates, number of guests and room
  - Receptionist-only page `reservations/<reservation id>/change` (no menu entry): code and guest name read-only, arrival (focused), departure, guests and a room choice preselected with the current room; choices are the current room plus the rooms the service reports available, refreshed on every change of dates or guests; a refused refresh shows the message and offers only the current room
  - Save changes applies the change through the service, notifies "Reservation <code> changed." and opens the reservation page; a refusal keeps the page and values; a reservation that is not Reserved shows "Reservation <code> has status <status> and can no longer be changed." without Save changes; unknown id shows "Reservation not found."; no guest-data input fields
  - ChangeReservationViewTest with reservation A (room 203, 20–23 May 2027, 2 guests) created per test: "Change" on A's reservation page opens this page; prefill and current room first; choices for 2 and for 3 guests; past arrival message with only the current room; departure 24 May keeps 130.00 and shows 4 nights and 520.00; room 204 shows 190.00 and 570.00; 3 guests in 203 refused with A unchanged; overlap with reservation B (203, 25–27 May) refused with A unchanged; A cancelled through the service after opening is refused on save; 5001 shows the status message without Save changes; 5999 not found
  - Done when ChangeReservationViewTest passes
  - _Boundary: ChangeReservationView_
  - _Depends: 2.1_
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 9.1, 10.1, 10.2_

- [ ] 4. Access and end-to-end validation
- [ ] 4.1 Prove receptionist-only access and navigation
  - FrontDeskAccessTest: the receptionist's staff navigation lists "Reservations" and "New reservation"; a manager and a housekeeper opening each of the four page addresses (list, new, reservation 5000, change of 5000) get the not-available view and no instance of the page, and their navigation lists neither entry; an anonymous visitor opening the list ends on the login page
  - If a refusal case fails, only the role annotations of the views may change here; behaviour changes go back to the task that owns the view
  - Done when FrontDeskAccessTest passes
  - _Depends: 2.1, 3.1, 3.2, 3.4_
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ] 4.2 Browser test of the keyboard journey at desktop width
  - FrontDeskReservationsIT at 1280×800 with a real login as the test receptionist: the arrival field has focus on the new-reservation page; with the keyboard only enter 25–27 May 2027 and 2 guests, Enter lists rooms, choose room 203, enter guest name and phone, Enter shows the reservation page Reserved; on the list Enter on a name search finds it and Enter in the code field opens it; cancel and confirm with the keyboard shows Cancelled; no horizontal scrolling on each of the four pages
  - Cleans up every reservation arriving 15–31 May 2027 after the test
  - Done when `./mvnw verify` passes with FrontDeskReservationsIT counted in the failsafe reports
  - _Depends: 2.2, 3.1, 3.3, 3.4_
  - _Requirements: 8.2, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

- [ ] 5. Final validation
  - Review the package: views import only public reservation-core types, foundation's layout and roles and Vaadin; no repository, jOOQ or database access, no rule code beyond the documented view checks (missing date, no room chosen, status message on the change page), no logger, and no notification that contains guest data
  - Run the full build with all tests
  - Done when `./mvnw verify` passes and the surefire and failsafe reports list FrontDeskAccessTest, ReservationListViewTest, ReservationDetailViewTest, NewReservationViewTest, ChangeReservationViewTest and FrontDeskReservationsIT with no skipped tests
  - _Requirements: 10.1, 10.2, 10.3_
