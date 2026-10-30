# Implementation Plan

- [ ] 1. Test data and online rules
- [ ] 1.1 Add the guest-booking test data and the test cleanup helper
  - Prerequisite: foundation tasks 1.1–5.1 (security with default deny, shared clock fixed at 2026-06-01 in both test base classes, rooms, public entry page) and reservation-core tasks 1.1–3.6 (reservation schema and the reservation service with create, find by code and cancel) are implemented; every task of this spec relies on them
  - Add the test migration (version 130) with the three April 2027 reservations and their guests from the design's test-data table: 4000 Reserved in room 103 with e-mail, 4001 Cancelled in room 104 with e-mail, 4002 Reserved in room 105 with phone only; rooms resolved by room number, codes TEST4000–TEST4002, booked rate 130.00, obviously fake guest data; no main migration
  - Add the test helper that deletes every reservation with an arrival date from 2027-04-15 to 2027-04-30 (rows created by tests; guests go with them)
  - Create the service test class with one case: looking up TEST4000 through the reservation service returns room 103, status Reserved and guest4000@example.com
  - Done when `./mvnw test` applies the test migration on a clean build and this first case passes
  - Test support only: its requirement coverage comes from the tasks whose tests use this data (2.1, 2.2, 2.3)

- [ ] 1.2 Implement the online booking, look-up and cancellation rules
  - Booking online refuses a missing or blank e-mail address as invalid input ("E-mail address is required.") before anything is stored, and otherwise hands the reservation to the reservation core unchanged
  - Look-up keeps a reservation only when the code matches and its stored e-mail address equals the entered one, ignoring case and surrounding spaces; no e-mail, erased guest data or any mismatch gives "nothing found"
  - Online cancellation is refused for any status other than Reserved and from the arrival date on (allowed until the day before arrival, today from the shared clock); the date rule takes "today" as an explicit input for edge-date tests; a successful cancel goes through the reservation core
  - Cancelling re-checks code and e-mail ("No reservation was found for this reservation code and e-mail address." as not found) and the online period at request time; no logging, no mail, no guest data in any message
  - GuestBookingServiceTest calls the cleanup helper after each test; reservations it creates use room 201 with arrival dates inside the cleanup window (2027-04-15→16, 2027-04-17→18)
  - Service test cases: e-mail missing and blank refused with unchanged row count; valid booking for room 201 on 2027-04-15→16 is Reserved; look-up with lower-case code and upper-case, padded e-mail finds 4000; wrong e-mail, unknown code, 4002 and a test-created reservation whose guest row was removed find nothing; the period rule for 4000 is open on 2027-04-09 and closed on 2027-04-10 and 2027-04-11; 4001 is refused with the status message; cancel with wrong e-mail is not found and its message contains neither e-mail nor name; cancel of 4001 is refused and stays Cancelled; cancel of a test-created reservation returns Cancelled
  - Done when all GuestBookingServiceTest cases pass
  - _Requirements: 3.3, 3.5, 5.2, 5.3, 6.3, 6.5, 6.6, 7.3, 7.4_

- [ ] 2. Guest pages
  - Note for (P): parallel tasks share one build; run `./mvnw` builds one after another because every build rebuilds the shared code-generation database under `target/`
- [ ] 2.1 (P) Build the room search page
  - Public page "availability" with arrival date, departure date (no dates before today offered) and number of guests (at least 1), a search action and a list of available rooms with room number, room type, capacity, nightly rate, nights and total price, ordered by room number, plus a "Book" action per room that navigates to the booking page address with room id, dates and number of guests as query parameters (by route name, so this task does not need the booking page to exist)
  - Missing fields are marked and nothing is searched; rule rejections from the reservation core and the "no room available" text appear in the page's message element; the page is not inside the staff layout and is open to anonymous visitors; responsive layout for phone width
  - AvailabilityViewTest (anonymous): 2027-04-11→12 for 2 guests lists exactly 104, 201, 202, 203, 204, 205 with 1 night and prices 130.00/190.00 and no guest data; 5 guests shows the no-availability message; missing departure marks the field and lists nothing; same-day departure and arrival 2026-05-31 show the core messages; both date pickers start at 2026-06-01
  - The navigation check of 2.6 ("Book" lands on the booking page) lives in 3.1, because the booking page is built in parallel
  - Done when AvailabilityViewTest passes
  - _Boundary: AvailabilityView_
  - _Depends: 1.1_
  - _Requirements: 1.1, 1.3, 2.1, 2.2, 2.3, 2.4, 2.5, 7.2_

- [ ] 2.2 (P) Build the booking page with the confirmation
  - Read-only reservation details panel (code, status, room, room type, dates, nights, guests, nightly rate, total price, guest name, e-mail, phone) in a responsive form layout, used here for the confirmation and later by the "My reservation" page
  - Public page "book": on every entry re-checks the selection from the query parameters against current availability; an incomplete, unparsable or unavailable selection shows only the "cannot be booked" message and the link back to the search
  - Valid selection shows the stay summary, fields for name (required, up to 100), e-mail (required, up to 254) and phone (optional, up to 30), the privacy note, the payment note and a book action that cannot be clicked twice; service rejections appear unchanged in the message element with the form kept
  - Successful booking replaces the form with the confirmation: details panel, code hint, link to "My reservation" and payment note; the confirmation exists only in this page instance
  - BookingViewTest (anonymous, cleans up created rows): summary for 201, 2027-04-20→23, 2 guests shows 3 nights and 390.00 with exactly three data fields and both notes; empty name or e-mail and a 101-character name mark the field and store nothing; invalid e-mail and phone show the core messages and store nothing; a valid submit shows an 8-character code and all confirmation data and stores one Reserved reservation; the room booked through the reservation service after the form opened gives the "not available" message with one reservation left for those nights; missing guests, unparsable date and room 103 on 2027-04-11→12 show the "cannot be booked" message without data fields; opening the same address after a booking shows no confirmation and no guest data
  - Done when BookingViewTest passes
  - _Boundary: BookingView, ReservationDetailsPanel_
  - _Depends: 1.2_
  - _Requirements: 1.1, 1.3, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 4.1, 4.2, 4.4, 7.2, 7.4_

- [ ] 2.3 Build the "My reservation" page with online cancellation
  - Not (P) with 2.2: reuses the details panel created there
  - Public page "my-reservation" with reservation code and e-mail fields (both required) and a find action; a match shows the details panel and the hint that changes are made by cancelling and booking again or by contacting the hotel; any mismatch shows the single not-found message and no data
  - "Cancel reservation" shown only while the reservation can be cancelled online; for a Reserved reservation outside the period the "contact the hotel" hint instead; the confirmation dialog names code and dates and offers "Cancel reservation" and "Keep reservation"; cancel uses the code and e-mail of the look-up, refreshes the details and shows the cancelled message, or shows the refusal and the current state
  - MyReservationViewTest (anonymous, cleans up created rows): test4000 with upper-case e-mail shows all fields, status Reserved and the change hint; missing code marks the field; wrong e-mail and the code of 4002 show the identical not-found text without details; 4001 shows Cancelled with neither cancel action nor hint; for a reservation created on 2027-04-25→27 the cancel action is visible, the dialog shows code and dates, "Keep reservation" leaves it Reserved, confirming makes it Cancelled with the message and the room available again for those nights; a check-in through the reservation service after the look-up makes the confirmed cancel show the refusal and status Checked in
  - Done when MyReservationViewTest passes
  - _Boundary: MyReservationView_
  - _Depends: 1.2, 2.2_
  - _Requirements: 1.1, 1.3, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 7.2_

- [ ] 3. Integration and end-to-end journey
- [ ] 3.1 Link the guest pages from the public entry page and connect search to booking
  - Integration task: changes the foundation's public entry page (allowed by the foundation design) by adding "Book a room" to the search and "My reservation" links by route name, without importing guest-booking code into the shared core
  - PublicEntryLinksTest (anonymous): the entry page shows both links with their targets; each of the three guest pages opens without login and shows no staff navigation; from the search, "Book" on room 201 for 2027-04-11→12 and 2 guests ends on the booking page showing that room and selection
  - Done when PublicEntryLinksTest and the foundation's existing public entry page test both pass
  - _Boundary: PublicHomeView (foundation-owned, links only)_
  - _Depends: 2.1, 2.2, 2.3_
  - _Requirements: 1.1, 1.2, 1.3, 2.6_

- [ ] 3.2 Browser test of the full guest journey and page widths
  - GuestBookingIT in a real browser: from the start page follow "Book a room", search 2027-04-20→22 for 2 guests, book room 202 with fake data, read the code from the confirmation, follow "My reservation", look it up, cancel and confirm, and see status Cancelled; the confirmation is reached after the three steps search, guest data, confirmation
  - At 360×800 and 1280×800 the search, booking and "My reservation" pages show no horizontal scrolling and their main actions are visible
  - Deletes its reservation afterwards
  - Done when `./mvnw verify` runs GuestBookingIT green
  - _Depends: 3.1_
  - _Requirements: 7.1, 7.2_

- [ ] 4. Final validation
  - Review the guest-booking main code (`src/main/java/.../guestbooking` and the two entry-page links; tests may use the database for cleanup and assertions): no logger, no mail or messaging dependency, no access to reservation tables or the repository, no call of the staff-only reservation search, no guest data in any message or route parameter
  - Run the full build with all tests
  - Done when `./mvnw verify` passes and the reports list GuestBookingServiceTest, PublicEntryLinksTest, AvailabilityViewTest, BookingViewTest, MyReservationViewTest and GuestBookingIT with no skipped tests
  - _Requirements: 4.3, 7.3, 7.4_
