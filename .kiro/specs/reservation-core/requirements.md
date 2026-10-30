# Requirements Document

## Project Description (Input)
Paper lists cause double bookings and lost reservations at the hotel. Guests (online) and receptionists (walk-in and
phone guests) both create reservations, so the booking rules must live in one place that both use. Today the HRS has
rooms and room types (foundation) but no reservation data and no availability logic. This feature adds reservations
that link a guest, one room and a date range (arrival inclusive, departure exclusive); an availability query that
returns the rooms free for every night of a range that fit the number of guests; create, change and cancel that can
never produce two reservations for the same room on a common night, even when two people book at the same moment; the
lifecycle transitions (reserved, cancelled, checked in, checked out) with their allowed transitions enforced in one
place; the price of a reservation (nights × nightly rate) and whether it is fixed at booking time; and the minimal guest
data a reservation needs (GDPR), including how long it is kept. Out of scope: any screens (guest-booking,
front-desk-reservations, reception), cleaning rules (housekeeping), how guests find their reservation online
(guest-booking), check-in conditions such as clean rooms (reception). Source: `.kiro/specs/reservation-core/brief.md`.

## Introduction
The reservation core is the single place where the Hotel Reservation System (HRS) decides which rooms are available,
creates, changes and cancels reservations, moves them through their lifecycle and prices them. The guest pages, the
receptionist's reservation pages, check-in and check-out, the manager's rate maintenance and the reports all rely on
these rules instead of repeating them, so a room can never be reserved twice for the same night.

## Boundary Context
- **In scope**: reservation data (guest, room, arrival and departure date, number of guests, status, booked nightly
  rate, reservation code); the minimal guest personal data and its erasure; availability for a date range and number
  of guests; creating, changing and cancelling reservations; the check-in and check-out status changes and the rules
  for which status changes are allowed; the price of a reservation; finding reservations by id, code, guest name and
  date range; protection against double bookings, including simultaneous requests.
- **Out of scope**: every page or screen (guest-booking, front-desk-reservations, reception); how a guest proves that
  a reservation is theirs and until when a guest may cancel online (guest-booking); which roles may trigger which
  operation (the calling feature's pages); whether a room is clean before check-in, early or late check-in and
  check-out, no-show handling and moving a guest at check-in (reception); marking a room dirty after check-out
  (reception with housekeeping); changing rooms, room types and nightly rates (management); report figures (reports).
- **Adjacent expectations**: guest-booking, front-desk-reservations and reception create, change, cancel, check in and
  check out reservations only through this feature and show its rejection messages unchanged; reception checks the
  cleaning status before it asks for a check-in; management changes nightly rates knowing that existing reservations
  keep their booked rate (D2); reports read reservations, their statuses and booked nightly rates without changing
  them.

## Assumptions and Decisions
These were left open by the brief, `domain.md` and the roadmap and are decided here; changing one requires revisiting
this spec and the downstream specs named.

- **D1 Statuses**: exactly four: **Reserved**, **Checked in**, **Checked out**, **Cancelled**. Allowed changes:
  Reserved → Checked in, Reserved → Cancelled, Checked in → Checked out. Checked out and Cancelled are final. There is no
  separate no-show status; reception decides how a no-show is handled (for example by cancelling).
- **D2 Price fixed at booking**: a reservation keeps the nightly rate its room type had when it was created; later rate
  changes by the manager do not change it. The price of a reservation is its number of nights × its booked nightly
  rate. A change to a room of another room type takes that room type's nightly rate at the time of the change.
- **D3 Guest data**: per reservation only the guest's name (required), e-mail address and phone number (at least one of
  the two required). No address, birth date, document or payment data. Each reservation has its own guest data; guest
  data of different reservations is never merged into a guest profile.
- **D4 Retention**: guest data is erased 90 days after the departure date of a reservation that is not checked in; the
  reservation itself (room, dates, number of guests, status, price) is kept for reporting.
- **D5 Reservation code**: every reservation gets a unique eight-character code of upper-case letters and digits that
  guests and staff can quote; guest-booking decides how a guest uses it to find their reservation.
- **D6 Today** is the date of the shared time source of the foundation (foundation A6). Arrival dates before today are
  refused for new reservations, changes and availability requests. There is no maximum stay length and no maximum
  booking horizon.
- **D7 One room per reservation**: a group needs one reservation per room.
- **D8 What can be changed**: only a Reserved reservation can be changed, and only its room, arrival date, departure
  date and number of guests; its code and guest data stay as they are.
- **D9 Status rules only**: check-in and check-out are checked here only against the status; date and cleaning
  conditions belong to reception. Cancelling is possible at any time while Reserved; online deadlines belong to
  guest-booking.

## Requirements

### Requirement 1: Reservations and their statuses
**Objective:** As a receptionist, I want every reservation to say exactly which guest has which room for which nights
and in which status, so that nobody relies on paper lists.

#### Acceptance Criteria
1. The HRS shall link every reservation to the data of exactly one guest (until it is erased, 10.1), exactly one room, an arrival date, a departure date, a number of guests, a booked nightly rate and exactly one status out of Reserved, Checked in, Checked out and Cancelled (D1).
2. The HRS shall count the nights of a reservation from its arrival date inclusive to its departure date exclusive, so that a reservation from 1 to 3 June has two nights.
3. The HRS shall give every reservation a unique reservation code of eight upper-case letters and digits that does not reveal the order in which reservations were created (D5).
4. The HRS shall keep cancelled and checked-out reservations and never delete a reservation.

### Requirement 2: Availability
**Objective:** As a guest or receptionist, I want to see which rooms are free for my dates and party size, so that I
only try to book rooms that can be booked.

#### Acceptance Criteria
1. When availability is requested for an arrival date, a departure date and a number of guests, the HRS shall return every room whose capacity is at least the number of guests and that has no Reserved or Checked-in reservation on any night from the arrival date to the night before the departure date, ordered by room number.
2. The HRS shall show for every available room its room number, room type, capacity, current nightly rate and the price for the requested nights.
3. The HRS shall treat Cancelled and Checked-out reservations as occupying no night.
4. When an existing reservation's departure date equals the requested arrival date, or the requested departure date equals an existing reservation's arrival date, the HRS shall treat the room as free for the requested range with respect to that reservation.
5. If the departure date is not after the arrival date, the arrival date is before today, or the number of guests is less than one, the HRS shall refuse the availability request with a message naming the problem.
6. If no room fits the request, the HRS shall return an empty result and no error.

### Requirement 3: Guest personal data
**Objective:** As the hotel, I want to keep only the guest data a reservation needs, so that we meet GDPR and can still
reach the guest.

#### Acceptance Criteria
1. The HRS shall store for the guest of a reservation only the name, the e-mail address and the phone number (D3).
2. If the guest name is missing or blank, or both the e-mail address and the phone number are missing, the HRS shall refuse the reservation with a message naming the missing data.
3. If the e-mail address is not of the form text@text.text, the phone number contains characters other than digits, spaces and the characters + - ( ), or the name is longer than 100, the e-mail address longer than 254 or the phone number longer than 30 characters, the HRS shall refuse the reservation with a message naming the invalid field.
4. The HRS shall store separate guest data for every reservation and shall not merge or link guest data of different reservations.
5. The HRS shall not show guest personal data in any rejection message and shall not write it to log output.

### Requirement 4: Creating a reservation
**Objective:** As a guest or receptionist, I want to reserve an available room for a date range, so that the room is
held for the guest.

#### Acceptance Criteria
1. When a reservation is requested for a room, an arrival date, a departure date, a number of guests and valid guest data, and the room is free for every night of the range, the HRS shall store the reservation with status Reserved and return its reservation code, room, dates, number of guests, booked nightly rate, number of nights and price.
2. The HRS shall set the booked nightly rate of a new reservation to the nightly rate of the room's room type at the moment of creation and keep it when the nightly rate of the room type changes later (D2).
3. The HRS shall calculate the price of a reservation as its number of nights multiplied by its booked nightly rate.
4. If the room does not exist, the arrival date is before today, the departure date is not after the arrival date, the number of guests is less than one, or the number of guests exceeds the capacity of the room, the HRS shall refuse the reservation with a message naming the problem and store nothing.
5. If the room has a Reserved or Checked-in reservation on at least one night of the range, the HRS shall refuse the reservation with a message that the room is not available for these dates and store nothing.

### Requirement 5: No double bookings
**Objective:** As the hotel, I want it to be impossible to reserve a room twice for the same night, so that no guest
arrives to an occupied room.

#### Acceptance Criteria
1. The HRS shall never hold two Reserved or Checked-in reservations for the same room that have at least one common night.
2. When two requests to create or change reservations would give the same room a common night and arrive at the same moment, the HRS shall accept at most one of them and refuse the other with the message that the room is not available for these dates.
3. When a new or changed reservation arrives on the departure date of another reservation for the same room, the HRS shall accept it if no other night overlaps.

### Requirement 6: Changing a reservation
**Objective:** As a receptionist, I want to change the room, dates or number of guests of a reservation, so that I can
follow the guest's wishes without cancelling and re-booking.

#### Acceptance Criteria
1. While a reservation is Reserved, when a change of its room, arrival date, departure date or number of guests is requested, the HRS shall check the new values with the same rules as for a new reservation (4.4, 4.5), disregarding the reservation being changed, and store them if they pass.
2. When a change keeps the room type of the reservation, the HRS shall keep the booked nightly rate and recalculate the price for the new number of nights.
3. When a change moves the reservation to a room of a different room type, the HRS shall set the booked nightly rate to that room type's nightly rate at the moment of the change and recalculate the price.
4. The HRS shall keep the reservation code, the guest data and the status of a reservation unchanged when it is changed.
5. If a change is refused, the HRS shall keep the reservation exactly as it was before the request.
6. If a change is requested for a reservation that is not Reserved, the HRS shall refuse it with a message that a reservation in its current status cannot be changed.

### Requirement 7: Cancelling a reservation
**Objective:** As a guest or receptionist, I want to cancel a reservation, so that the room becomes free again while the
record of the reservation stays.

#### Acceptance Criteria
1. While a reservation is Reserved, when its cancellation is requested, the HRS shall set its status to Cancelled and treat its nights as free from then on.
2. If cancellation is requested for a reservation that is not Reserved, the HRS shall refuse it with a message that a reservation in its current status cannot be cancelled and keep its status.

### Requirement 8: Check-in and check-out status changes
**Objective:** As a receptionist, I want check-in and check-out to move a reservation through its lifecycle in a fixed
order, so that the status always reflects reality.

#### Acceptance Criteria
1. While a reservation is Reserved, when its check-in is requested, the HRS shall set its status to Checked in.
2. While a reservation is Checked in, when its check-out is requested, the HRS shall set its status to Checked out and treat its remaining nights as free from then on.
3. If check-in is requested for a reservation that is not Reserved, or check-out for a reservation that is not Checked in, the HRS shall refuse it with a message naming the current status and keep the status.
4. The HRS shall allow no status changes other than Reserved to Checked in, Reserved to Cancelled and Checked in to Checked out, and shall not change the status of a Cancelled or Checked-out reservation (D1).
5. If two status changes or a change and a status change for the same reservation are requested at the same moment, the HRS shall apply them one after the other and judge the second against the status left by the first.

### Requirement 9: Finding reservations
**Objective:** As a guest-facing or staff feature, I want to read reservations by reference or by guest and date, so
that guests and staff see the current state.

#### Acceptance Criteria
1. When a reservation is requested by its id or by its reservation code, the HRS shall return its code, status, room number, room type, arrival date, departure date, number of guests, booked nightly rate, number of nights, price and guest data.
2. If no reservation has the requested id or reservation code, the HRS shall report that the reservation was not found.
3. When reservations are searched by a part of the guest name, by a date range, or both, the HRS shall return every reservation in any status whose guest name contains the text regardless of upper and lower case and that has at least one night in the date range, ordered by arrival date and room number.

### Requirement 10: Retention of guest data
**Objective:** As the hotel, I want guest personal data to disappear when it is no longer needed, so that we keep only
what GDPR allows while reports stay correct.

#### Acceptance Criteria
1. When the departure date of a reservation that is not Checked in lies more than 90 days before today, the HRS shall erase the name, e-mail address and phone number of its guest within one day without any staff action (D4).
2. The HRS shall keep the room, dates, number of guests, status, booked nightly rate and price of a reservation whose guest data was erased.
3. When a reservation whose guest data was erased is read, the HRS shall show that the guest data was erased instead of a name or contact data, and shall no longer find it by guest name.

### Requirement 11: One set of rules and clear rejections
**Objective:** As every feature that works with reservations, I want the same rules and the same kind of rejection
everywhere, so that online booking and the front desk never disagree.

#### Acceptance Criteria
1. The HRS shall apply the rules of requirements 2 to 8 in the same way whether a guest or a staff member triggers the operation.
2. If an operation is refused, the HRS shall change no reservation and report exactly one reason, distinguishable as reservation not found, invalid input, room not available, or status change not allowed, together with a message that a guest or receptionist can read.
3. The HRS shall take "today" for all date rules from the shared time source of the HRS, so that the rules behave identically for a fixed date.
