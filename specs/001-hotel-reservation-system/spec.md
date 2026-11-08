# Feature Specification: Hotel Reservation System

**Feature Branch**: `001-hotel-reservation-system`

**Created**: 2026-11-08

**Status**: Draft

**Input**: User description: "Build the Hotel Reservation System described in docs/vision.md."

## Clarifications

### Session 2026-11-08

- Q: Does the user book a specific room or a room type? → A: A specific room; the search lists free rooms
  and the user picks one.
- Q: How are staff accounts created and managed? → A: The Manager creates staff users, sets role and
  password, and can deactivate them.
- Q: What happens to guest personal data after the stay (GDPR)? → A: Name, email and phone are anonymized
  automatically 12 months after departure or cancellation; the reservation stays for reports.
- Q: How long can a stay be, and how far ahead can it be booked? → A: At most 30 nights, with arrival at
  most one year ahead.
- Q: What language is the UI in? → A: English only, for guests and staff.
- Analyze follow-up: Which rooms can be deleted? → A: Only rooms without any reservation, including past ones.
- Analyze follow-up: Which stays count as occupied for the report? → A: Checked-in and checked-out stays, as
  for revenue.
- Analyze follow-up: How is SC-007 (half of reservations online) measured? → A: Each reservation records
  its booking source (online or reception); the monthly report shows the online share.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manager maintains room types, rooms and rates (Priority: P1)

The Manager logs in and maintains the hotel's room types (name, capacity, nightly rate) and its rooms
(room number, room type). Every other story depends on rooms existing, so this comes first.

**Why this priority**: Without rooms and rates nothing can be searched, booked or cleaned.

**Independent Test**: Log in as Manager, create a room type and a room, change the nightly rate, and see
both in the room list.

**Acceptance Scenarios**:

1. **Given** the Manager is logged in, **When** they create a room type with name, capacity and nightly
   rate, **Then** the room type appears in the room type list.
2. **Given** a room type exists, **When** the Manager creates a room with a room number and that room type,
   **Then** the room appears in the room list with cleaning status clean.
3. **Given** a room with number 101 exists, **When** the Manager creates another room with number 101,
   **Then** the system rejects it and says the room number is already used.
4. **Given** a room type is used by a room, **When** the Manager tries to delete the room type, **Then**
   the system rejects it and says the room type is still in use.
5. *(Changed by 003-seasonal-room-rates, FR-001, FR-006.)* **Given** a reservation exists for a room, **When** the Manager changes the room type's nightly rate,
   **Then** the existing reservation keeps the rate it was booked with.
6. **Given** a user is logged in as Receptionist or Housekeeping, **When** they try to open room
   management or staff user management, **Then** access is denied.
7. **Given** the Manager is logged in, **When** they create a staff user with username, role and initial
   password, **Then** that user can log in and sees only the functions of their role.
8. **Given** a staff user exists, **When** the Manager deactivates them, **Then** that user can no longer
   log in.
9. **Given** a username is already used, **When** the Manager creates another staff user with it, **Then**
   the system rejects it.

---

### User Story 2 - Receptionist books a room for a phone or walk-in guest (Priority: P1)

The Receptionist searches available rooms for a date range and number of guests and creates a reservation
with the guest's name and contact details.

**Why this priority**: It replaces the paper list and delivers the most important goal: no double
bookings.

**Independent Test**: With rooms in place, log in as Receptionist, search for two nights for two guests,
book a room, and confirm the same room is no longer offered for overlapping nights.

**Acceptance Scenarios**:

1. **Given** rooms exist, **When** the Receptionist searches with arrival date, departure date and number of
   guests, **Then** only rooms whose capacity fits and that have no reservation for any of those nights
   are shown, with the total price for the stay.
2. **Given** an available room is shown, **When** the Receptionist enters the guest's name, email and phone
   and books it, **Then** a reservation with status booked and a reservation number is created.
3. **Given** room 101 is booked for nights 10–12 of a month, **When** someone tries to book room 101 for
   nights 11–13, **Then** the system rejects it as a double booking.
4. **Given** room 101 is booked with departure on the 12th, **When** someone books room 101 with arrival on
   the 12th, **Then** the booking succeeds (the departure day is not a night of the stay).
5. **Given** two users book the last free night of the same room at the same time, **When** both submit,
   **Then** exactly one booking succeeds and the other gets a "no longer available" message.
6. **Given** the departure date is on or before the arrival date, or the arrival date is in the past,
   **When** the Receptionist searches, **Then** the system rejects the search with a clear message.
7. **Given** no room fits the request, **When** the Receptionist searches, **Then** the system says no room
   is available for these dates and number of guests.

---

### User Story 3 - Guest books a room online (Priority: P2)

A Guest, without logging in, searches available rooms for a date range and number of guests, picks a room,
enters name and contact details, and receives a reservation number.

**Why this priority**: Online booking is a key success measure (half of all reservations online), but the
hotel can operate with reception bookings alone.

**Independent Test**: Without logging in, search, book a room and receive a reservation number; the room is
then no longer offered for those nights.

**Acceptance Scenarios**:

1. **Given** the Guest is on the public booking page, **When** they search with dates and number of
   guests, **Then** they see available rooms with room type, capacity and total price, following the
   same availability rules as User Story 2.
2. **Given** an available room, **When** the Guest enters name, email and phone and confirms, **Then** a
   reservation with status booked is created and the reservation number is shown.
3. **Given** the Guest leaves a required field empty or enters an invalid email, **When** they confirm,
   **Then** the booking is not created and the field is marked.
4. **Given** the room was booked by someone else while the Guest was filling in the form, **When** they
   confirm, **Then** they get a "no longer available" message and can search again.
5. **Given** the Guest is not logged in, **When** they try to open any staff page, **Then** they are sent
   to the staff login.

---

### User Story 4 - Receptionist checks guests in and out (Priority: P2)

The Receptionist sees today's arrivals and departures, checks a guest in to a clean room, and checks a
guest out, which marks the room dirty.

**Why this priority**: It enforces the rule "no check-in to a room that is not clean" and feeds the
housekeeping list.

**Independent Test**: With a booked reservation arriving today for a clean room, check the guest in, then
check them out and see the room's status change to dirty.

**Acceptance Scenarios**:

1. **Given** a reservation with status booked arrives today and its room is clean, **When** the
   Receptionist checks the guest in, **Then** the reservation status becomes checked in.
2. **Given** the reserved room is dirty, **When** the Receptionist tries to check the guest in, **Then** the
   system rejects it and says the room is not clean yet.
3. **Given** a guest is checked in, **When** the Receptionist checks them out, **Then** the reservation
   status becomes checked out and the room's cleaning status becomes dirty.
4. **Given** a reservation's arrival date is in the future, **When** the Receptionist tries to check the
   guest in, **Then** the system rejects it.
5. **Given** the Receptionist opens the room overview, **When** it loads, **Then** every room shows whether
   it is free, booked or occupied today and whether it is clean or dirty.

---

### User Story 5 - Housekeeping works through the cleaning list (Priority: P2)

Housekeeping logs in on a tablet, sees all dirty rooms, and marks a room as cleaned.

**Why this priority**: Guests should not wait for rooms that are not clean; needed together with
User Story 4 to run a full stay.

**Independent Test**: With a dirty room, log in as Housekeeping, see the room in the cleaning list, mark it
cleaned, and see it disappear from the list.

**Acceptance Scenarios**:

1. **Given** a guest was just checked out of room 101, **When** Housekeeping opens the cleaning list,
   **Then** room 101 is in it.
2. **Given** room 101 is in the cleaning list, **When** Housekeeping marks it cleaned, **Then** its cleaning
   status becomes clean and it leaves the list.
3. **Given** Housekeeping is logged in, **When** they try to open reservations or reports, **Then** access
   is denied.

---

### User Story 6 - Receptionist changes and cancels reservations (Priority: P2)

The Receptionist finds a reservation by reservation number or guest name, changes its dates, room or
guest details, or cancels it.

**Why this priority**: Phone guests change plans often; without it reception falls back to paper.

**Independent Test**: Change the dates of a booked reservation to free dates, then cancel it and see the
room offered again for those nights.

**Acceptance Scenarios**:

1. *(Changed by 003-seasonal-room-rates, FR-007.)* **Given** a booked reservation, **When** the Receptionist changes its dates or room to ones that are
   free, **Then** the reservation is updated and the total price is recalculated with the current rate.
2. **Given** a booked reservation, **When** the Receptionist changes it to nights the room is already
   booked for, **Then** the change is rejected as a double booking.
3. **Given** a booked reservation, **When** the Receptionist cancels it, **Then** its status becomes
   cancelled and its nights are available again.
4. **Given** a reservation is checked in, checked out or cancelled, **When** the Receptionist tries to
   change or cancel it, **Then** the system rejects it.

---

### User Story 7 - Guest views and cancels their own reservation online (Priority: P3)

A Guest, without calling the hotel, looks up their reservation and cancels it if needed.

**Why this priority**: Saves phone calls, but reception can cancel on the guest's behalf (User Story 6).

**Independent Test**: Book online, look up the reservation with its reservation number, cancel it, and
see the room offered again.

**Acceptance Scenarios**:

1. *(Changed by 003-seasonal-room-rates, FR-005.)* **Given** a booked reservation, **When** the Guest enters the reservation number and the email used for
   the booking, **Then** they see dates, room
   type, total price and status.
2. **Given** wrong identification data, **When** the Guest looks up a reservation, **Then** no reservation
   data is shown and the message does not reveal whether the reservation number exists.
3. *(Changed by 002-cancellation-deadline-48h, FR-002–FR-005.)* **Given** a booked reservation whose arrival date is after today, **When** the Guest cancels it,
   **Then** its status becomes cancelled and its nights are available again.
4. *(Changed by 002-cancellation-deadline-48h, FR-002–FR-005.)* **Given** a booked reservation whose arrival date is today, **When** the Guest tries to cancel it
   online, **Then** the system rejects it and tells the Guest to call reception.
5. **Given** a reservation is checked in, checked out or cancelled, **When** the Guest tries to cancel it,
   **Then** the system rejects it.

---

### User Story 8 - Manager views occupancy and revenue reports (Priority: P3)

The Manager selects a month and sees occupancy and revenue for it.

**Why this priority**: Replaces spreadsheet work but does not affect daily operations.

**Independent Test**: With reservations in a month, open the report for that month and compare occupancy
and revenue with a manual count.

**Acceptance Scenarios**:

1. **Given** reservations in a month, **When** the Manager opens the report for that month, **Then** they
   see occupancy as occupied room-nights (nights of reservations with status checked in or checked out)
   divided by available room-nights (rooms × days in month), in percent, and the share of that month's
   new reservations that were booked online.
2. **Given** reservations in a month, **When** the Manager opens the report, **Then** they see revenue as
   the sum of the nightly rate of every night in that month of reservations with status checked in or
   checked out (booked and cancelled reservations do not count).
3. **Given** a stay crosses the end of a month, **When** reports for both months are opened, **Then** each
   month counts only its own nights.
4. **Given** a Receptionist or Housekeeping user, **When** they try to open reports, **Then** access is
   denied.

---

### Edge Cases

- A stay that starts on the departure day of another stay in the same room is allowed (no overlap).
- Two bookings for the same room and nights submitted at the same moment: exactly one succeeds.
- A guest's reserved room is dirty when they arrive: check-in is refused until Housekeeping marks it clean;
  the Receptionist may instead move the reservation to a free clean room (User Story 6).
- A guest does not arrive (no-show): the reservation stays booked until the Receptionist cancels it.
- The Manager deletes a room that has any reservation, including past ones: rejected (keeps report history).
- The Manager lowers a room type's capacity below the guest count of existing reservations: existing
  reservations stay; new searches use the new capacity.
- Number of guests larger than any room's capacity: no rooms are shown.
- A stay longer than 30 nights, or an arrival more than one year ahead, is rejected (FR-008).
- A guest looks up an anonymized reservation online: it can no longer be found, since the email is gone.
- Reports for months older than 12 months still show correct occupancy and revenue after anonymization.

## Requirements *(mandatory)*

### Functional Requirements

**Rooms and rates**

- **FR-001** *(changed by 003-seasonal-room-rates, FR-001–FR-003)*: The Manager MUST be able to create, change and delete room types with name, capacity (number
  of guests) and nightly rate.
- **FR-002**: The Manager MUST be able to create, change and delete rooms with a unique room number and a
  room type.
- **FR-003**: The system MUST reject deleting a room type used by a room, and deleting a room that has
  any reservation, including past ones.
- **FR-004**: A new room MUST start with cleaning status clean.

**Availability and booking**

- **FR-005**: Guests and Receptionists MUST be able to search available rooms by arrival date, departure
  date and number of guests, and book by picking one specific room from the result.
- **FR-006**: A room MUST be shown as available only if its capacity is at least the number of guests and
  no booked or checked-in reservation of it covers any night from arrival up to the night before
  departure.
- **FR-007**: The system MUST NOT allow two booked or checked-in reservations of the same room with
  overlapping nights, also when they are submitted at the same time.
- **FR-008**: The system MUST reject searches and bookings where departure is not after arrival, arrival
  is in the past, arrival is more than one year ahead, or the stay is longer than 30 nights.
- **FR-009** *(changed by 003-seasonal-room-rates, FR-006)*: A reservation MUST record the room, arrival and departure dates, number of guests, guest
  name, email and phone, the nightly rate at booking time, the total price, a unique reservation number,
  a status and the booking source (online or reception).
- **FR-010**: Reservation status MUST be one of booked, checked in, checked out, cancelled.
- **FR-011**: Guests MUST be able to book online without logging in and MUST be shown their reservation
  number after booking.
- **FR-012**: Receptionists MUST be able to create reservations for phone and walk-in guests.

**Changing and cancelling**

- **FR-013**: Receptionists MUST be able to find reservations by reservation number or guest name and change
  dates, room, number of guests and guest details of a booked reservation, subject to FR-006 to FR-008.
- **FR-014**: Receptionists MUST be able to cancel a booked reservation; cancelled reservations no longer
  block their nights.
- **FR-015** *(changed by 002-cancellation-deadline-48h, FR-002–FR-005)*: Guests MUST be able to view their own reservation online by entering its reservation number
  and the booking email, without seeing any other guest's data, and MUST be able to cancel it online
  until the day before the arrival date.
- **FR-016**: Only reservations with status booked MAY be changed or cancelled.

**Check-in, check-out and cleaning**

- **FR-017**: Receptionists MUST be able to check in a booked reservation on its arrival date only if its
  room is clean.
- **FR-018**: Receptionists MUST be able to check out a checked-in reservation; check-out MUST set the room's
  cleaning status to dirty.
- **FR-019**: Receptionists MUST see a room overview showing, for today, each room's occupancy (free,
  booked, occupied) and cleaning status.
- **FR-020**: Housekeeping MUST see a cleaning list of all dirty rooms, which reflects a check-out as soon as
  it happens, and MUST be able to mark a room cleaned.

**Reports**

- **FR-021**: The Manager MUST be able to see, per selected month, occupancy (room-nights of checked-in and
  checked-out reservations / available room-nights), revenue (nightly rates of the month's nights of
  checked-in and checked-out reservations) and the share of reservations created that month online.

**Access**

- **FR-022**: Receptionist, Manager and Housekeeping MUST log in; each sees only the functions of their
  role: Receptionist — reservations, check-in/out, room overview; Manager — rooms, room types, rates,
  reports, staff users; Housekeeping — cleaning list.
- **FR-023**: Guest functions (search, book, view, cancel own reservation) MUST be available without login.
- **FR-025**: The Manager MUST be able to create staff users (unique username, role, initial password),
  change their role, reset their password and deactivate them; deactivated users MUST NOT be able to log
  in.

**Personal data**

- **FR-024**: Guest personal data (name, email, phone) MUST be visible only to staff with a need for it
  (Receptionist and Manager) and to the guest for their own reservation, in line with GDPR.
- **FR-026**: The system MUST automatically anonymize the guest's name, email and phone of every
  reservation whose departure date or cancellation lies more than 12 months in the past; dates, room,
  prices and status stay so reports remain correct.

**Language**

- **FR-027**: All guest and staff screens MUST be in English; there is no language switch.

### Key Entities

- **Room Type**: a kind of room; name, capacity (number of guests), nightly rate.
- **Room**: one of the hotel's rooms; unique room number, its room type, cleaning status (clean, dirty).
- **Reservation**: a booking of one room for a stay; reservation number, room, arrival and departure dates,
  number of guests, nightly rate at booking, total price, status (booked, checked in, checked out,
  cancelled), and the guest's details.
- **Guest**: the person a reservation is for; name, email, phone. Not a user account.
- **Staff User**: a person who logs in; unique username, password, role (Receptionist, Manager,
  Housekeeping), active or deactivated.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Zero double bookings: no room ever has two booked or checked-in reservations for the same
  night.
- **SC-002**: No guest is ever checked in to a room whose cleaning status is dirty.
- **SC-003**: A guest can complete an online booking (search to reservation number) in under 3 minutes.
- **SC-004**: A Receptionist can check a guest in or out in under 1 minute.
- **SC-005**: A checked-out room appears in Housekeeping's cleaning list without a manual refresh step by
  reception.
- **SC-006**: The Manager can read monthly occupancy and revenue without any manual spreadsheet work.
- **SC-007**: Within six months of go-live, at least half of all reservations are made online by guests.
- **SC-008**: Staff screens are usable on a reception desktop, and the cleaning list on a tablet.

## Assumptions

- One hotel with ten rooms; the Manager enters the rooms after go-live (no import).
- Room types group rooms with the same capacity and nightly rate.
- *(Changed by 003-seasonal-room-rates, FR-004.)* The nightly rate is the same for every night (no seasonal or weekend rates); total price = nights ×
  nightly rate at booking time.
- One reservation is for one room; a group needing two rooms makes two reservations.
- Payment is settled at the hotel; the system records prices but no payments (vision: out of scope).
- One initial Manager account is provided at installation; all other staff users are created by the
  Manager. There is no self-registration and no "forgot password" flow; the Manager resets passwords.
- No emails are sent by the system; the guest notes the reservation number shown after booking.
- No-shows stay booked until a Receptionist cancels them.
- The guest lookup has no limit on attempts; the random 8-character reservation number plus the email
  makes guessing impractical.
- The initial Manager password is not forced to change; the Manager resets it right after installation.
- Booking channels, extra services billing, multiple hotels and loyalty programs are out of scope
  (vision).
