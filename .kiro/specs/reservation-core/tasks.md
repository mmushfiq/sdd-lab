# Implementation Plan

- [ ] 1. Reservation schema and test data
- [ ] 1.1 Create the reservation and guest schema
  - Prerequisite: foundation tasks 1.1 (security dependencies), 1.2 (production clock bean and fixed test clock 2026-06-01 in the Browserless test base) and 2.1 (room and room type schema) are implemented; this migration references rooms and every test of this spec relies on the fixed clock
  - Add the main migration (version 10) creating reservations (unique eight-character code, room reference, arrival and departure dates with departure after arrival, at least one guest, booked nightly rate snapshot above zero, status limited to the four statuses) and guests (one per reservation, name up to 100, e-mail up to 254 and phone up to 30 characters, name required, e-mail and phone optional but not both missing, removed with their reservation)
  - Add the schema test: each invalid insert (departure not after arrival, zero guests, unknown status, duplicate code, unknown room, guest without e-mail and phone, second guest for one reservation) fails
  - Done when `./mvnw compile` generates the reservation and guest jOOQ classes and the schema test passes
  - _Requirements: 1.1, 1.3, 3.1, 3.2, 3.3, 3.4, 4.2_

- [ ] 1.2 Add the reservation-core test data
  - Add the test migration (version 110) with the eight reservations and their guests from the design's test-data table (ids 2000–2004 in February 2027, 2010–2012 in December 2024), rooms resolved by room number, obviously fake guest data
  - Extend the schema test with one case that the eight test reservations exist with their documented statuses and rates
  - Done when the schema test, including the test-data case, passes on a clean build
  - _Requirements: 1.1, 2.3_

- [ ] 2. Define the public contract types
  - Not (P) with 1.x: runs after 1.1's build has finished, because every `./mvnw` run rebuilds the shared jOOQ code-generation database
  - Status vocabulary with display labels, the allowed-transition check and the "occupies nights" check
  - Input records for create, change and search; read records for available rooms (price as a plain value filled in by the service) and reservation details (nights and price derived from dates and booked rate)
  - Rule-violation exception carrying one of four reasons and a readable message
  - These types have no tests of their own: the transition matrix is tested in 3.6 and nights and price in 3.3
  - Done when `./mvnw compile` succeeds and the types do not refer to the database layer or to generated jOOQ classes
  - _Requirements: 1.2, 4.3, 8.4, 11.2_

- [ ] 3. Reservation rules in the service
- [ ] 3.1 Availability for a date range and number of guests
  - Query rooms whose capacity fits and that have no reserved or checked-in reservation overlapping the half-open range, ordered by room number, with the current nightly rate; the service adds the price for the range
  - Refuse an end not after the start, an arrival before today (from the shared clock) and fewer than one guest as invalid input
  - Availability test (AvailabilityTest) with the February 2027 test data: exact room list and prices for 11–12 February and 2 guests, back-to-back case 12–14 February, 3 guests, the three refusals, empty result for 5 guests
  - Done when AvailabilityTest passes
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 11.3_

- [ ] 3.2 Find reservations by id, code and search criteria
  - Read reservation details with room, room type and guest data in one query; look up by id, by code (trimmed, case-insensitive), and search by part of the guest name and/or date range, ordered by arrival date and room number
  - Search with a name part excludes reservations without guest data; an end not after the start is invalid input
  - Creates ReservationServiceTest with the find cases on the test data: id and lower-case code return all fields; unknown id or code returns nothing; name-only, range-only and combined searches return the documented rows in order; the invalid range is refused
  - Done when these ReservationServiceTest find and search cases pass
  - _Requirements: 9.1, 9.2, 9.3_

- [ ] 3.3 Create a reservation without double booking
  - Validate dates, number of guests and room existence; lock the room, check capacity and overlap, store the reservation with the room type's current nightly rate and its own guest row, and return the details with status Reserved
  - Use a simple placeholder code for now (eight characters, unique per call, fits the unique code column); the real code generation follows in 3.4
  - ReservationServiceTest create cases (rows created in March 2027 and deleted after each test): status, booked rate, 3 nights × 130.00 = 390.00 and 1–3 June counts as two nights; rate kept after a room type rate change (restored afterwards); every refusal of 4.4 with its reason and an unchanged row count; overlap with test reservation 2000 refused as room not available; arrival on its departure date accepted
  - Done when these create cases pass
  - _Requirements: 1.2, 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.3, 11.2_

- [ ] 3.4 Validate guest data and generate reservation codes
  - Before any lock, validate guest data: trim values, treat blank e-mail and phone as missing, require a name and at least one contact, check e-mail and phone formats and the lengths 100/254/30; messages never contain guest data and the service writes no log output
  - Replace the placeholder with codes of eight characters from the unambiguous alphabet drawn from a secure random source, regenerated while the code exists
  - ReservationServiceTest guest and code cases: each 3.2/3.3 refusal with its message and no row stored; no refusal message contains the guest name; same guest data twice gives two guest rows; codes match the alphabet and two creates get different codes
  - Done when these cases and all earlier ReservationServiceTest cases pass
  - _Requirements: 1.3, 3.2, 3.3, 3.4, 3.5, 11.2_

- [ ] 3.5 Change a reservation
  - Lock the reservation, allow changes only while reserved, re-use the create checks for the new room, dates and guests while ignoring the reservation itself, keep the booked rate within the same room type and take the current rate of a different room type
  - ReservationServiceTest change cases: a date change in the same room keeps the rate with a new price; a move from a double to a family room takes 190.00; code, guest data and status unchanged; a move onto test reservation 2000's nights is refused and the stored values are unchanged; shortening within its own nights is accepted; changing a cancelled reservation is refused as a status problem; an unknown id is not found
  - Done when these change cases pass
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 9.2_

- [ ] 3.6 Cancel, check in and check out
  - Lock the reservation and apply only the allowed transitions; refused transitions name the current status and leave it unchanged
  - ReservationServiceTest cancel cases: cancel frees the nights (visible in availability); a second cancel is refused
  - ReservationLifecycleTest (created here): check-in of a reserved and check-out of a checked-in reservation, after which the nights are free; every disallowed transition refused with status unchanged; the full 16-pair transition matrix; an unknown id is not found for cancel, check-in and check-out; no reservation row disappears after any operation
  - Done when the cancel cases and ReservationLifecycleTest pass
  - _Requirements: 1.4, 7.1, 7.2, 8.1, 8.2, 8.3, 8.4, 9.2, 11.2_

- [ ] 4. Concurrency and guest-data retention
- [ ] 4.1 Prove protection against simultaneous requests
  - ReservationConcurrencyTest without any test-managed transaction (the base test class is not transactional). It creates its own reservations in March 2027, including the one the change and the cancel race against, and deletes them afterwards
  - 10 rounds of two threads released together that create the same room and nights; a create racing a change into the same nights; a cancel racing a check-in of one reservation
  - In each round exactly one request succeeds; the other gets "room not available" or "status change not allowed"; at most one occupying reservation per room and night remains
  - If a round fails, this task may change only the lock calls in the repository and service; any change to a rule goes back to the 3.x task that owns it
  - Done when ReservationConcurrencyTest passes in three consecutive runs
  - _Depends: 3.4, 3.5, 3.6_
  - _Requirements: 5.1, 5.2, 8.5_

- [ ] 4.2 Erase expired guest data daily
  - Delete the guest data of reservations that are not checked in and departed before a cutoff; the reservation itself stays
  - Daily scheduled job (03:00, server time zone) with a cutoff of today minus 90 days from the shared clock
  - GuestDataRetentionTest with an explicit December 2024 cutoff: 2010 erased with all its reservation data kept; 2011 (boundary) and 2012 (checked in) kept; the erased reservation reads as "no guest data" and is not found by name; the job's cutoff is 2026-03-03; the scheduler holds the job's cron task; the erased guest row is restored after the test
  - Done when GuestDataRetentionTest passes
  - _Requirements: 10.1, 10.2, 10.3, 11.3_

- [ ] 5. Final validation
  - Review the service: no parameter or branch depends on who calls it, no logging of guest data, no method deletes a reservation
  - Run the full build with all tests
  - Done when `./mvnw verify` passes and the surefire reports list all six reservation test classes (ReservationSchemaTest, AvailabilityTest, ReservationServiceTest, ReservationLifecycleTest, ReservationConcurrencyTest, GuestDataRetentionTest) with no skipped tests
  - _Requirements: 1.4, 3.5, 11.1_
