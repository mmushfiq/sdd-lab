# Requirements Document

## Project Description (Input)
Check-in and check-out are slow and error-prone, guests are checked in to rooms that aren't clean, and reception has
no single view of all rooms. Reservations and their lifecycle exist (reservation-core); cleaning status exists
(housekeeping). The receptionist checks a guest in only if the reserved room is clean; otherwise the check-in is
refused with a clear reason. The receptionist checks a guest out; the room becomes dirty and appears on the cleaning
list at once. Each check-in or check-out takes under 1 minute. A room overview shows, for every room, who has booked
it, who is staying in it and whether it is clean. Approach: receptionist views in the staff layout (today's arrivals
and departures, check-in, check-out, room overview); status changes go through the reservation core, cleaning status
through housekeeping. In scope: arrivals and departures for a day; check-in with clean check; check-out marking the
room dirty; room overview of all ten rooms. Out of scope: creating or changing reservations (front-desk-reservations),
marking rooms clean (housekeeping), payment. To decide here: no-show handling, early or late check-in and check-out,
moving a guest to another room at check-in. Source: `.kiro/specs/reception/brief.md`.

## Introduction
Reception gives the receptionist of the Hotel Reservation System (HRS) the day's work lists of arrivals and
departures, a check-in that only succeeds for a clean, unoccupied room, a check-out that puts the room on the
housekeeper's cleaning list in the same step, and a room overview that shows for every room who is staying in it, who
has booked it for tonight and whether it is clean. Every check-in and check-out also records the day it happened, so
that the reports can tell which nights were actually stayed.

## Boundary Context
- **In scope**: the arrivals list and the departures list for a chosen day; check-in with its date, occupancy and
  cleaning conditions; check-out with marking the room dirty; recording the check-in day and check-out day of every
  stay; the room overview of all rooms; the decisions on no-show, early and late check-in and check-out, and room
  moves at check-in (R1–R6).
- **Out of scope**: creating, changing and cancelling reservations, including changing the room or dates of a
  reservation before check-in (front-desk-reservations, guest-booking); the rules for which status changes are allowed
  (reservation-core); marking a room clean and the cleaning list itself (housekeeping); payment and invoices; occupancy
  and revenue figures (reports); marking a room dirty by hand.
- **Adjacent expectations**: the reservation core accepts or refuses the status change and its refusal messages are
  shown unchanged; housekeeping answers whether a room is clean and shows a room marked dirty on its cleaning list on
  its next load or refresh; foundation provides the staff layout, the receptionist role and "today". Check-in and
  check-out happen only through the reception pages, so every stay has a recorded check-in day. The reports read the
  stay definition of requirement 9 without changing it.

## Assumptions and Decisions
Decided here because the brief and `domain.md` leave them open; changing one requires revisiting this spec and the
reports spec.

- **R1 No-show**: there is no no-show status and no automatic action. A Reserved reservation whose guest has not arrived
  stays on the arrivals list, marked "Late arrival", on every day from its arrival date to the day before its departure
  date and can be checked in on those days. From its departure date on it is no longer listed, cannot be checked in and
  stays Reserved; it never counts as a stay. To free the room earlier, the receptionist cancels the reservation in the
  reservation pages (front-desk-reservations).
- **R2 Early check-in**: check-in before the arrival date is refused. To let a guest in early, the receptionist first
  changes the reservation's arrival date in the reservation pages. On the arrival date check-in is possible at any time
  of day, provided the room is clean and not occupied.
- **R3 Late check-in**: check-in is possible on any day from the arrival date to the day before the departure date. The
  missed nights stay part of the reservation; its price does not change.
- **R4 Early check-out**: check-out is possible on any day while the guest is checked in. The remaining nights become
  free; the price of the reservation does not change.
- **R5 Late check-out**: a checked-in guest whose departure date has passed can still be checked out; until then the
  guest is listed as "Overdue" and the room counts as occupied, so no other guest can be checked in to it.
- **R6 No room move at check-in**: check-in always uses the room of the reservation. If that room is not clean or is
  occupied, the check-in is refused; the receptionist waits for housekeeping or changes the reservation's room in the
  reservation pages and then checks in.
- **R7 Roles**: only receptionists use the reception pages; managers and housekeepers are refused.
- **R8 Work lists**: the arrivals list shows the reservations still to be checked in, the departures list the guests
  still to be checked out; a row leaves its list once its check-in or check-out is done.
- **R9 Day of a check-in or check-out** is today's date from the shared time source (foundation).

## Requirements

### Requirement 1: Arrivals list
**Objective:** As a receptionist, I want to see which guests are due to arrive on a day, so that I can check them in
without searching for their reservations.

#### Acceptance Criteria
1. When a receptionist opens the arrivals and departures page, the HRS shall show the arrivals list and the departures list for today and let the receptionist choose another day for both lists.
2. The HRS shall show on the arrivals list for a chosen day every Reserved reservation that has a night on that day, with room number, guest name, arrival date, departure date and number of guests, ordered by arrival date and room number.
3. While a reservation on the arrivals list has an arrival date before the chosen day, the HRS shall mark it "Late arrival" (R1, R3).
4. The HRS shall not show on the arrivals list any Cancelled, Checked-in or Checked-out reservation, nor a Reserved reservation whose departure date is on or before the chosen day (R1, R8).
5. If the arrivals list for the chosen day is empty, the HRS shall show the message "No arrivals" instead of the list.

### Requirement 2: Departures list
**Objective:** As a receptionist, I want to see which guests are due to leave, so that I can check them out and no
guest is forgotten.

#### Acceptance Criteria
1. The HRS shall show on the departures list for a chosen day every checked-in guest whose departure date is on or before that day, with room number, guest name, arrival date and departure date, ordered by departure date and room number.
2. While a guest on the departures list has a departure date before the chosen day, the HRS shall mark the guest "Overdue" (R5).
3. If the departures list for the chosen day is empty, the HRS shall show the message "No departures" instead of the list.
4. When the receptionist chooses "Refresh" or opens the page again, the HRS shall show both lists as they are at that moment, including changes made by other staff members.

### Requirement 3: Check-in
**Objective:** As a receptionist, I want to check a guest in only to a clean and free room, so that no guest is given a
room that is not ready.

#### Acceptance Criteria
1. When a receptionist chooses "Check in" for a reservation on the arrivals list, the HRS shall ask for confirmation, showing the guest name, room number, arrival date and departure date, and shall change nothing if the receptionist cancels.
2. When the receptionist confirms a check-in while the reservation is Reserved, today is on or after its arrival date and before its departure date, its room is clean and no other guest is checked in to that room, the HRS shall set the reservation to Checked in, record today as its check-in day, remove it from the arrivals list and show a confirmation naming the room number.
3. If the room of the reservation is not clean, the HRS shall refuse the check-in with a message that names the room and says it is not clean.
4. If another guest is checked in to the room of the reservation, the HRS shall refuse the check-in with a message that names the room and says it is still occupied (R5).
5. If today is before the arrival date of the reservation, the HRS shall refuse the check-in with a message that names the arrival date (R2).
6. If today is on or after the departure date of the reservation, the HRS shall refuse the check-in with a message that the reservation can no longer be checked in (R1).
7. If the reservation is no longer Reserved when the check-in is confirmed, for example because another receptionist checked it in or it was cancelled meanwhile, the HRS shall refuse the check-in with a message naming its current status and show the refreshed lists.
8. The HRS shall check a guest in only to the room of the reservation and shall not offer a choice of another room during check-in (R6).

### Requirement 4: Check-out
**Objective:** As a receptionist, I want checking a guest out to also put the room on the cleaning list, so that
housekeeping knows at once which room to clean.

#### Acceptance Criteria
1. When a receptionist chooses "Check out" for a checked-in guest, the HRS shall ask for confirmation, showing the guest name, room number and departure date, and shall change nothing if the receptionist cancels.
2. While today is before the departure date of the reservation, the HRS shall state in the check-out confirmation that the guest leaves before the departure date and that the remaining nights become free (R4).
3. When the receptionist confirms the check-out while the reservation is Checked in, the HRS shall set it to Checked out, record today as its check-out day, set the room's cleaning status to Dirty, remove the guest from the departures list and show a confirmation naming the room and saying that it is on the cleaning list.
4. When a room has been checked out, the HRS shall show it on the housekeeper's cleaning list the next time that list is opened or refreshed, without any further step.
5. The HRS shall allow the check-out of a checked-in guest whose departure date has passed (R5).
6. If the reservation is no longer Checked in when the check-out is confirmed, the HRS shall refuse the check-out with a message naming its current status and show the refreshed lists.

### Requirement 5: All-or-nothing check-in and check-out
**Objective:** As a receptionist, I want a check-in or check-out to either happen completely or not at all, so that the
reservation, the stay and the cleaning status never contradict each other.

#### Acceptance Criteria
1. If a check-in is refused or fails for any reason, the HRS shall keep the reservation Reserved, record no check-in day and show exactly one message.
2. If a check-out is refused or fails for any reason, the HRS shall keep the reservation Checked in, record no check-out day, keep the room's cleaning status unchanged and show exactly one message.
3. The HRS shall never show any staff member a reservation as Checked out while its room is still Clean from before that check-out.
4. When two staff members check in the same reservation at the same moment, the HRS shall accept at most one check-in and refuse the other with a message naming the current status.
5. When a check-out of one guest and a check-in of another guest to the same room are confirmed at the same moment, the HRS shall never check the second guest in to that room unless the room is clean and no guest is checked in to it.

### Requirement 6: Room overview
**Objective:** As a receptionist, I want to see the state of every room at a glance, so that I know who is staying
where, who is expected tonight and which rooms are clean.

#### Acceptance Criteria
1. When a receptionist opens the room overview, the HRS shall show every room of the hotel once, ordered by room number, with its cleaning status (Dirty or Clean).
2. Where a guest is checked in to a room, the HRS shall show in that room's row the guest name and departure date and mark the guest "Overdue" if the departure date is before today.
3. Where a room has a Reserved reservation with a night on today, the HRS shall show in that room's row the guest name, arrival date and departure date of that reservation.
4. Where a room has neither a checked-in guest nor a Reserved reservation for tonight, the HRS shall show that room's guest fields as empty.
5. Where a guest is checked in to a room, the HRS shall offer "Check out" in that room's row with the same confirmation and result as in requirement 4.
6. When the receptionist chooses "Refresh" or opens the room overview again, the HRS shall show the rooms as they are at that moment, including changes made by other staff members.

### Requirement 7: Fast work at the desk
**Objective:** As a receptionist, I want check-in and check-out to take only a few clicks, so that each takes less than
a minute.

#### Acceptance Criteria
1. When the receptionist has opened the arrivals and departures page for today, the HRS shall complete a check-in after two actions, "Check in" and the confirmation, without any data entry.
2. When the receptionist has opened the arrivals and departures page or the room overview, the HRS shall complete a check-out after two actions, "Check out" and the confirmation, without any data entry.
3. When the reception pages are shown at desktop width (foundation A7), the HRS shall show every column and action of the lists and the room overview without horizontal scrolling.

### Requirement 8: Access to the reception pages
**Objective:** As the hotel, I want only receptionists to check guests in and out, so that stays are recorded by the
people at the desk.

#### Acceptance Criteria
1. The HRS shall let receptionists open the arrivals and departures page and the room overview and list both in their staff navigation menu (R7).
2. If a manager or housekeeper opens a reception page, including by its address, the HRS shall refuse it in the same way as any staff page the role may not open, and shall not list it in their navigation menu.
3. If a person who is not signed in opens a reception page, the HRS shall show the login page instead.

### Requirement 9: Stay record
**Objective:** As a manager, I want the HRS to know which reservations were actually stayed and on which days the guest
arrived and left, so that occupancy and revenue reports can be based on real stays.

#### Acceptance Criteria
1. The HRS shall keep for every check-in made through the reception pages the check-in day and, once checked out, the check-out day, and shall not change either afterwards.
2. The HRS shall treat a reservation as a stay exactly when its status is Checked in or Checked out; a Reserved reservation whose departure date has passed and a Cancelled reservation shall never count as a stay (R1).
3. The HRS shall store no guest personal data in the stay record, shall not show guest personal data in any refusal message of check-in or check-out and shall not write it to log output.
