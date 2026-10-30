# Requirements Document

## Project Description (Input)
Walk-in and phone guests are handled on paper at the hotel, so reservations get lost or double-booked. The reservation
rules (availability, create, change, cancel, no double booking) already exist in reservation-core, but receptionists
have no staff pages for reservations. This feature gives the receptionist pages inside the staff layout to search
availability, create a reservation for a walk-in or phone guest, find existing reservations by guest name, date or
reservation code, change the dates, room or number of guests of a reservation, and cancel a reservation after a
confirmation. Reservations made online by guests are visible and manageable here too, and every change obeys the same
availability rules as online booking. The pages are used on a desktop at the reception desk and must be fast and
keyboard-friendly. Out of scope: check-in, check-out and the room overview (reception), guest self-service
(guest-booking), the availability and lifecycle rules themselves (reservation-core). The brief asks this spec to
decide whether the manager may also manage reservations. Source: `.kiro/specs/front-desk-reservations/brief.md`.

## Introduction
The receptionist's reservation pages let receptionists of the Hotel Reservation System (HRS) handle every
reservation of the hotel without paper: find it, look at it, create one for a guest standing at the desk or calling on
the phone, change it and cancel it. All decisions about availability, prices and allowed changes are taken by the
reservation rules of reservation-core; these pages show their results and their rejection messages unchanged.

## Boundary Context
- **In scope**: the receptionist's pages for finding reservations (by guest name, date range or reservation code),
  showing one reservation, searching availability and creating a reservation with guest data, changing room, dates and
  number of guests, and cancelling with confirmation; who may open these pages; a stable page address for one
  reservation that other staff pages can link to.
- **Out of scope**: check-in, check-out, arrivals and departures lists and the room overview (reception); the guest's
  own online pages (guest-booking); the availability, pricing, guest-data and lifecycle rules and their messages
  (reservation-core); changing guest data of an existing reservation (not allowed by reservation-core D8); rooms and
  rates (management); payment.
- **Adjacent expectations**: reservation-core decides availability, prices, allowed status changes and guest-data
  validity and gives a readable reason for every refusal; foundation provides staff login, the staff layout with its
  role-filtered navigation and the shared "today". Reception may link to the reservation page of requirement 4 by its
  address and does not need to rebuild it.

## Assumptions and Decisions
These were left open by the brief, `domain.md` and the roadmap and are decided here.

- **F1 Who may use the pages**: only the receptionist. Managers and housekeepers are refused, like any other role a
  page does not permit (foundation 4.3), and do not see the entries in the navigation. Rationale: `product.md` and
  `domain.md` give reservations to the receptionist and rooms, rates and reports to the manager; the brief's approach
  is "receptionist-only".
- **F2 Origin does not matter**: reservations made online by guests and those made at the desk are listed, shown,
  changed and cancelled in exactly the same way; the pages do not show or record where a reservation came from.
- **F3 Default list**: when the reservation list is opened, it shows the reservations that have at least one night on
  or after today (current and upcoming), in every status.
- **F4 Defaults for a new reservation**: arrival date today, departure date tomorrow, one guest, because most desk
  reservations are for walk-in guests arriving today.
- **F5 Dates** are shown and typed as year-month-day (for example 2027-05-20) on all pages of this feature, the same
  form the reservation rules use in their messages. Amounts are shown with two decimals and without a currency sign.
- **F6 Room choice when changing**: the receptionist chooses between the reservation's current room and every room
  that is available for the new dates and number of guests. The resulting nightly rate and price are shown after the
  change is saved, because only the reservation rules know whether the booked rate is kept (reservation-core D2).
- **F7 Guest data is not editable** here (reservation-core D8). A wrong name or contact is corrected by cancelling and
  creating a new reservation.

## Requirements

### Requirement 1: Access and navigation
**Objective:** As the hotel, I want only receptionists to manage reservations at the desk, so that guest data and
reservations are handled by the people responsible for them.

#### Acceptance Criteria
1. While a receptionist is signed in, the HRS shall list "Reservations" and "New reservation" in the staff navigation and open every page of this feature for the receptionist.
2. If a manager or housekeeper opens any page of this feature, including by its address, the HRS shall show no content of the page and show that the page is not available (F1).
3. While a manager or housekeeper is signed in, the HRS shall not list "Reservations" or "New reservation" in the staff navigation.
4. If a person who is not signed in opens any page of this feature, the HRS shall show the login page instead.

### Requirement 2: Finding reservations
**Objective:** As a receptionist, I want to find reservations by guest name and dates, so that I can answer a guest at
the desk or on the phone at once.

#### Acceptance Criteria
1. When the receptionist opens the reservation list, the HRS shall preset the "From" date to today, leave the guest name and the "To" date empty, and list every reservation with at least one night on or after today (F3).
2. When the receptionist searches with a part of a guest name, a "From" date, a "To" date or any combination of them, the HRS shall list every reservation in any status whose guest name contains the text regardless of upper and lower case and that has at least one night between "From" and the night before "To", ordered by arrival date and room number.
3. The HRS shall show for every listed reservation its reservation code, guest name, room number, arrival date, departure date, number of nights, number of guests and status.
4. The HRS shall list reservations made online by guests and reservations made by receptionists together and in the same way (F2).
5. When a listed reservation's guest data has been erased, the HRS shall show "Guest data erased" in place of the guest name.
6. If the search finds no reservation, the HRS shall show "No reservations found." and an empty list.
7. If the "To" date is not after the "From" date, the HRS shall show the refusal message of the reservation rules and keep the previous list unchanged.
8. When the receptionist selects a listed reservation, the HRS shall show that reservation's page (requirement 4).

### Requirement 3: Opening a reservation by its code
**Objective:** As a receptionist, I want to open a reservation by the code the guest quotes, so that I find it without
knowing names or dates.

#### Acceptance Criteria
1. When the receptionist enters a reservation code on the reservation list and confirms it, the HRS shall show that reservation's page, regardless of upper and lower case and of leading or trailing spaces.
2. If no reservation has the entered code, the HRS shall show "Reservation not found." and stay on the reservation list.

### Requirement 4: Reservation page
**Objective:** As a receptionist, I want one page that shows everything about a reservation and offers what I can do
with it, so that I can handle a guest's request from one place.

#### Acceptance Criteria
1. When a reservation's page is opened, the HRS shall show its reservation code, status, room number, room type, arrival date, departure date, number of nights, number of guests, booked nightly rate, price and the guest's name, e-mail address and phone number, as currently stored.
2. When the guest data of the reservation has been erased, the HRS shall show "Guest data erased" instead of the name, e-mail address and phone number.
3. While the reservation is Reserved, the HRS shall offer the actions "Change" and "Cancel reservation" on its page.
4. While the reservation is Checked in, Checked out or Cancelled, the HRS shall offer neither "Change" nor "Cancel reservation" on its page.
5. If the page is opened for a reservation that does not exist, the HRS shall show "Reservation not found." and a way back to the reservation list.
6. The HRS shall make every reservation's page reachable under the address `reservations/<reservation id>`, so that other staff pages can link to it.
7. The HRS shall offer a way back to the reservation list on every reservation page.

### Requirement 5: Availability search for a new reservation
**Objective:** As a receptionist, I want to see which rooms are free for the guest's dates and party size, with their
price, so that I can offer the guest a room immediately.

#### Acceptance Criteria
1. When the receptionist opens the page for a new reservation, the HRS shall preset the arrival date to today, the departure date to tomorrow and the number of guests to 1 (F4).
2. When the receptionist searches availability for an arrival date, a departure date and a number of guests, the HRS shall list every available room as given by the reservation rules with its room number, room type, capacity, nightly rate and the price for the requested nights, ordered by room number.
3. If no room is available, the HRS shall show "No room is available for these dates." and no room to choose.
4. If the arrival or departure date is missing, the HRS shall show "Arrival and departure date are required." and search nothing.
5. If the reservation rules refuse the search (departure not after arrival, arrival before today, fewer than one guest), the HRS shall show their message and no room to choose.
6. When the receptionist changes the arrival date, departure date or number of guests after a search, the HRS shall discard the listed rooms and the room choice until the receptionist searches again.

### Requirement 6: Creating a reservation
**Objective:** As a receptionist, I want to reserve a chosen room for a walk-in or phone guest in one short form, so
that the room is held for the guest at once.

#### Acceptance Criteria
1. When the receptionist has chosen a listed room, entered the guest's name and at least an e-mail address or a phone number, and chooses "Reserve", the HRS shall create the reservation through the reservation rules for the searched dates, number of guests and chosen room, show its reservation page and confirm "Reservation <code> created.".
2. If the receptionist chooses "Reserve" without having chosen a room, the HRS shall show "Choose a room first." and create nothing.
3. If the reservation rules refuse the guest data, the HRS shall show their message, create nothing and keep every entered value.
4. If the chosen room has been reserved by someone else since the search, the HRS shall show the reservation rules' message that the room is not available for these dates, create nothing, keep the guest data and list the rooms that are available now.
5. The HRS shall never create a reservation through these pages that gives a room a night already held by a Reserved or Checked-in reservation.

### Requirement 7: Changing a reservation
**Objective:** As a receptionist, I want to change the dates, room or number of guests of a reservation, so that I can
follow the guest's wishes without cancelling and re-booking.

#### Acceptance Criteria
1. When the receptionist chooses "Change" for a Reserved reservation, the HRS shall show a change page with the reservation code, the guest name and the current arrival date, departure date, number of guests and room preselected.
2. While the change page is shown, the HRS shall offer as room choices the current room and every room that the reservation rules report as available for the entered dates and number of guests, and shall refresh these choices whenever the dates or the number of guests change (F6).
3. If the reservation rules refuse the entered dates or number of guests while the choices are refreshed, the HRS shall show their message and offer only the current room.
4. When the receptionist chooses "Save changes", the HRS shall apply the change through the reservation rules, show the reservation page with the new dates, room, number of guests, nightly rate and price and confirm "Reservation <code> changed.".
5. If the reservation rules refuse the change (room not available, too many guests for the room, invalid dates, status no longer Reserved), the HRS shall show their message, stay on the change page with the entered values and leave the reservation exactly as it was.
6. If the change page is opened for a reservation that is not Reserved, the HRS shall show "Reservation <code> has status <status> and can no longer be changed." and offer no "Save changes".
7. If the change page is opened for a reservation that does not exist, the HRS shall show "Reservation not found." and a way back to the reservation list.
8. The HRS shall offer no way to change the guest's name, e-mail address or phone number (F7).

### Requirement 8: Cancelling a reservation
**Objective:** As a receptionist, I want to cancel a reservation after a confirmation, so that the room becomes free
again and nothing is cancelled by mistake.

#### Acceptance Criteria
1. When the receptionist chooses "Cancel reservation", the HRS shall ask "Cancel reservation <code>?" with the choices "Cancel reservation" and "Keep reservation" before anything changes.
2. When the receptionist confirms with "Cancel reservation", the HRS shall cancel the reservation through the reservation rules, show it with status Cancelled and without the actions "Change" and "Cancel reservation", and confirm "Reservation <code> cancelled.".
3. When the receptionist chooses "Keep reservation", the HRS shall close the question and leave the reservation unchanged.
4. If the reservation rules refuse the cancellation because the reservation is no longer Reserved, the HRS shall show their message and show the reservation with its current status.
5. When a reservation has been cancelled, the HRS shall offer its room as available for its former nights in the availability search (requirement 5).

### Requirement 9: Fast, keyboard-friendly work at the desk
**Objective:** As a receptionist, I want to work through the forms with the keyboard on the reception desktop, so that
a phone or walk-in reservation takes as little time as possible.

#### Acceptance Criteria
1. When the page for a new reservation or the change page is opened, the HRS shall place the input focus in its first input field.
2. While the input focus is in the search fields of the reservation list or of the new-reservation page, when the receptionist presses Enter, the HRS shall run the search.
3. While the input focus is in the reservation code field, when the receptionist presses Enter, the HRS shall open the reservation with that code (requirement 3).
4. While the input focus is in the guest fields of the new-reservation page, when the receptionist presses Enter, the HRS shall act as if "Reserve" was chosen.
5. The HRS shall let the receptionist reach every field, room choice and action of these pages with the Tab key and trigger it with the keyboard alone.
6. When a page of this feature is shown at desktop width (foundation A7), the HRS shall show it without horizontal scrolling.

### Requirement 10: Same rules as online booking and protection of guest data
**Objective:** As the hotel, I want desk reservations to follow exactly the same rules as online booking and to treat
guest data carefully, so that the reception desk and the website never disagree and GDPR is respected.

#### Acceptance Criteria
1. The HRS shall decide availability, prices, guest-data validity and allowed changes and cancellations for these pages only by the reservation rules that also serve online booking, and shall show their refusal messages unchanged.
2. The HRS shall not show guest personal data in any confirmation or notification of these pages; confirmations name the reservation only by its code.
3. The HRS shall not write guest personal data to log output from these pages.
