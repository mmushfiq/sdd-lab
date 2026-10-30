# Requirements Document

## Project Description (Input)
Guests have to phone the hotel to book, check or cancel a reservation. The reservation rules and availability exist in
reservation-core, and foundation provides a public entry page, but there are no public guest screens. This feature lets
a guest, without logging in, search available rooms for a date range and number of guests, book a room by entering only
the personal data a reservation needs and get a confirmation they can use later, and find their own reservation again
to see its details and cancel it. A booking takes under 3 minutes. It decides how a guest identifies their reservation
without an account, whether and until when a guest may cancel online, and whether a guest may change a reservation
online. Out of scope: payment, staff screens, the availability and double-booking rules themselves (reservation-core),
and e-mail delivery unless the requirements ask for it. Source: `.kiro/specs/guest-booking/brief.md`.

## Introduction
Guest booking gives guests of the Hotel Reservation System (HRS) public pages to search availability, book a room,
receive an on-screen confirmation with their reservation code, and later find and cancel their own reservation with
that code and their e-mail address. All availability, booking, pricing and cancellation rules come from the reservation
core; this feature adds only the guest-facing pages and the online rules for identifying a reservation and cancelling
it.

## Boundary Context
- **In scope**: public room search; online booking of one room for one date range; the on-screen confirmation; finding
  one's own reservation with reservation code and e-mail address; viewing its details; cancelling it online within the
  online cancellation period; links to the guest pages on the public entry page.
- **Out of scope**: payment (settled at the hotel); sending e-mails or other messages; changing a reservation online;
  guest accounts or passwords; staff pages (front-desk-reservations, reception, management); the availability,
  double-booking, pricing, guest-data validation and retention rules (reservation-core); staff login (foundation).
- **Adjacent expectations**: the reservation core decides availability, refuses double bookings, fixes the price at
  booking time, validates guest data, keeps cancelled reservations and erases guest data 90 days after departure; this
  feature shows its results and rejection messages unchanged. The foundation's public entry page carries the links to
  the guest pages. Receptionists may create reservations with a phone number only; such reservations cannot be opened
  online (D2).

## Assumptions and Decisions
These were left open by the brief and `domain.md` (Open Questions) and are decided here; changing one requires
revisiting this spec.

- **D1 Identifying a reservation online**: a guest opens their reservation with its reservation code plus the e-mail
  address stored with it. There is no guest account, password or link sent by e-mail.
- **D2 Online booking needs an e-mail address**: name and e-mail address are required online, the phone number is
  optional. A reservation without an e-mail address (possible at the front desk) cannot be opened online.
- **D3 Online cancellation period**: a guest may cancel a Reserved reservation online until the day before its arrival
  date. From the arrival date on, or in any other status, only the hotel can act.
- **D4 No online change**: guests cannot change a reservation online; they cancel and book again, or contact the hotel.
- **D5 No e-mail delivery**: the on-screen confirmation is the only confirmation; the guest keeps the reservation code.
- **D6 Room choice**: the guest books a specific room from the search result (reservations are per room).
- **D7 Phone width** for guest pages means a viewport width of 360 px; desktop means 1280 px or wider.

## Requirements

### Requirement 1: Public access to the guest pages
**Objective:** As a guest, I want to reach the guest pages without an account, so that I can book and manage my
reservation without calling the hotel.

#### Acceptance Criteria
1. The HRS shall let anyone open the room search, the booking page and the "My reservation" page without signing in.
2. When anyone opens the public entry page, the HRS shall show a link "Book a room" to the room search and a link "My reservation" to the "My reservation" page.
3. The HRS shall show on the guest pages neither the staff navigation menu nor staff data, and shall show guest personal data only on the confirmation of the guest's own booking and on "My reservation" after the reservation code and e-mail address matched (5.1).

### Requirement 2: Searching available rooms
**Objective:** As a guest, I want to see which rooms are free for my dates and party size and what they cost, so that I
can choose a room.

#### Acceptance Criteria
1. When a guest enters an arrival date, a departure date and a number of guests and starts the search, the HRS shall list every room that the reservation rules report as available, with room number, room type, capacity, nightly rate, number of nights and total price for the stay, ordered by room number.
2. If no room is available for the request, the HRS shall show the message that no room is available for these dates and number of guests and list no rooms.
3. If the arrival date, the departure date or the number of guests is missing, the HRS shall mark the missing field and not search.
4. If the departure date is not after the arrival date, the arrival date is before today, or the number of guests is less than one, the HRS shall show the rejection message of the reservation rules and list no rooms.
5. The HRS shall not offer dates before today in the arrival and departure date selection.
6. When a guest chooses "Book" for a listed room, the HRS shall open the booking page for that room with the searched dates and number of guests.

### Requirement 3: Booking a room
**Objective:** As a guest, I want to book the chosen room by entering only the data the hotel needs, so that the room is
held for me.

#### Acceptance Criteria
1. When the booking page opens for an available room, dates and number of guests, the HRS shall show the room number, room type, arrival date, departure date, number of nights, number of guests and total price, and fields for name, e-mail address and phone number.
2. The HRS shall ask for no personal data other than name, e-mail address and phone number, and shall state on the booking page that this data is used only for the reservation and is erased 90 days after departure.
3. If the name or the e-mail address is missing, or the name is longer than 100, the e-mail address longer than 254 or the phone number longer than 30 characters, the HRS shall mark the field and store nothing (D2).
4. If the e-mail address or the phone number has an invalid format, the HRS shall show the rejection message of the reservation rules naming the invalid field and store nothing.
5. When the guest submits complete and valid data and the room is still free for every night, the HRS shall create the reservation with status Reserved for that room, those dates, that number of guests and that guest data, and show the confirmation (Requirement 4).
6. If the room has been reserved by someone else for at least one of the nights before the guest submits, the HRS shall show the message that the room is not available for these dates, store nothing, and offer a link back to the room search.
7. If the booking page is opened without a complete and valid room, date and number-of-guests selection, or for a room that is not available for that selection, the HRS shall show a message that the room cannot be booked for this selection, show no data entry fields, and offer a link back to the room search.
8. The HRS shall take no payment online and shall state on the booking page and on the confirmation that payment is made at the hotel.

### Requirement 4: Booking confirmation
**Objective:** As a guest, I want a confirmation with a reservation code, so that I can prove and find my reservation
later.

#### Acceptance Criteria
1. When a reservation has been created online, the HRS shall show its reservation code, room number, room type, arrival date, departure date, number of nights, number of guests, total price and the guest's name and e-mail address.
2. The HRS shall state on the confirmation that the guest needs the reservation code together with the e-mail address to view or cancel the reservation, and shall offer a link to "My reservation".
3. The HRS shall send no e-mail or other message for an online booking (D5).
4. If the booking page address is opened again after a booking, the HRS shall not show the confirmation or any guest data of that reservation.

### Requirement 5: Finding one's own reservation
**Objective:** As a guest, I want to open my reservation with my reservation code and e-mail address, so that I can check
it without an account.

#### Acceptance Criteria
1. When a guest enters a reservation code and an e-mail address on "My reservation" and both belong to the same reservation, the HRS shall show its reservation code, status, room number, room type, arrival date, departure date, number of nights, number of guests, nightly rate, total price, and the guest's name, e-mail address and phone number.
2. The HRS shall match the reservation code and the e-mail address regardless of upper and lower case and of leading or trailing spaces.
3. If no reservation has the code, the e-mail address does not match the reservation's e-mail address, the reservation has no e-mail address, or its guest data was erased, the HRS shall show the same message that no reservation was found for this code and e-mail address, and show no reservation data.
4. If the reservation code or the e-mail address is missing, the HRS shall mark the missing field and not search.
5. The HRS shall show a reservation in any status (Reserved, Checked in, Checked out, Cancelled) together with its status.
6. The HRS shall offer no change of a reservation online and shall state that a change is made by cancelling and booking again or by contacting the hotel (D4).

### Requirement 6: Cancelling one's own reservation
**Objective:** As a guest, I want to cancel my reservation online, so that the room is released without calling the
hotel.

#### Acceptance Criteria
1. While the shown reservation is Reserved and today is before its arrival date, the HRS shall offer the action "Cancel reservation" (D3).
2. When the guest chooses "Cancel reservation", the HRS shall ask for confirmation, naming the reservation code, arrival date and departure date, before cancelling.
3. When the guest confirms the cancellation, the HRS shall cancel the reservation, show its status as Cancelled and show a message that the reservation was cancelled, and the room shall be available again for those nights.
4. When the guest declines the confirmation question, the HRS shall leave the reservation unchanged.
5. While the shown reservation is not Reserved, or today is on or after its arrival date, the HRS shall not offer "Cancel reservation"; for a Reserved reservation it shall state that online cancellation is no longer possible and the guest should contact the hotel.
6. If a cancellation is requested for a reservation that can no longer be cancelled online at that moment (its status has changed, its arrival date is reached, or the code and e-mail address no longer match), the HRS shall refuse it with a message naming the reason and leave the reservation unchanged.

### Requirement 7: Quick, private and consistent guest pages
**Objective:** As the hotel, I want online booking to be quick, protect guest data and follow the same rules as the
front desk, so that guests book online instead of calling.

#### Acceptance Criteria
1. The HRS shall let a guest get from the public entry page to the booking confirmation in no more than three steps (search, enter guest data, see confirmation), entering only arrival date, departure date, number of guests, the room choice, name, e-mail address and, optionally, phone number, so that a booking can be completed in under 3 minutes.
2. When a guest page is shown at phone width or desktop width (D7), the HRS shall show it without horizontal scrolling and keep every field and action reachable.
3. The HRS shall not show guest personal data in any error or rejection message and shall not write it to log output.
4. The HRS shall apply the availability, double-booking, pricing, guest-data and cancellation rules of the reservation core unchanged for guests and show their rejection messages unchanged.
