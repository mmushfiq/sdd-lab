# Feature Specification: Online Cancellation Deadline 48 Hours

**Feature Branch**: `hotel-reservation-spec-kit`

**Created**: 2026-11-08

**Status**: Draft

**Input**: User description: "Too many guests cancel online the day before they arrive, and we can't resell the room in time. Move the online cancellation deadline from 24 hours to 48 hours before check-in. Guests who already booked were promised 24 hours, so their reservations keep the old deadline. Also show guests the exact last cancellation time, on the lookup page and in the confirmation email."

**Changes**: Feature 001-hotel-reservation-system, FR-015 and User Story 7 (acceptance scenarios 3 and 4).

## Clarifications

### Session 2026-11-08

- Q: Which time zone is the hotel's local time, which every cancellation deadline is calculated and shown in? → A: Europe/Zurich
- Q: Should Receptionists also see a reservation's last online cancellation time in their reservation view, or only guests on the lookup page and in the email? → A: Receptionists see it in the reservation details
- Q: How exactly should the last cancellation time be written for guests and staff? → A: "Free online cancellation until 10 March 2030, 15:00 (Zurich time)"
- Q: Once a booked reservation's deadline has passed, what should the lookup page show instead of the "Free online cancellation until …" text? → A: "Online cancellation was possible until 10 March 2030, 15:00 (Zurich time). Please call reception to cancel." and no cancel button

## User Scenarios & Testing *(mandatory)*

### User Story 1 - New bookings can be cancelled online until 48 hours before check-in (Priority: P1)

A Guest who books after this change can cancel the reservation online only until 48 hours before check-in.
Later, the Guest has to call reception, so the hotel has two days to resell the room.

**Why this priority**: This is the business reason for the change: late online cancellations leave rooms
unsold.

**Independent Test**: Book a room after the change, look the reservation up and cancel it more than 48
hours before check-in (succeeds); book another one and try to cancel it less than 48 hours before check-in
(rejected with the advice to call reception).

**Acceptance Scenarios**:

1. **Given** a booked reservation made after the change, **When** the Guest cancels it online before its
   last online cancellation time, **Then** its status becomes cancelled and its nights are available again.
2. **Given** a booked reservation made after the change, **When** the Guest tries to cancel it online at or
   after its last online cancellation time, **Then** the system rejects it and tells the Guest to call
   reception.
3. **Given** a reservation made less than 48 hours before check-in, **When** the Guest looks it up,
   **Then** the system shows that online cancellation is no longer possible (FR-005).

---

### User Story 2 - Existing bookings keep the deadline they were promised (Priority: P1)

A Guest who booked before the change keeps the online cancellation deadline that applied when they booked.

**Why this priority**: Guests were promised that deadline; taking it away would break the hotel's word.

**Independent Test**: With a reservation booked before the change, cancel it online after the new
48-hour limit but before the old deadline; the cancellation succeeds.

**Acceptance Scenarios**:

1. **Given** a booked reservation made before the change, **When** the Guest cancels it online before the
   old deadline, **Then** its status becomes cancelled, even if check-in is less than 48 hours away.
2. **Given** a booked reservation made before the change, **When** the Guest tries to cancel it online
   after the old deadline, **Then** the system rejects it and tells the Guest to call reception.

---

### User Story 3 - Guest sees the exact last cancellation time on the lookup page (Priority: P2)

When a Guest looks up their reservation, they see the exact date and time until which they can cancel it
online.

**Why this priority**: Guests should not have to work out the deadline themselves; fewer calls to reception.

**Independent Test**: Look up a booked reservation and check that the shown date and time match the
deadline that applies to it.

**Acceptance Scenarios**:

1. **Given** a booked reservation, **When** the Guest looks it up, **Then** they see its last online
   cancellation time as date and time in the hotel's local time.
2. **Given** a booked reservation whose last online cancellation time has passed, **When** the Guest looks
   it up, **Then** they see "Online cancellation was possible until <time> (Zurich time). Please call
   reception to cancel." and no cancel button.
3. **Given** a reservation that is cancelled, checked in or checked out, **When** the Guest looks it up,
   **Then** no cancellation time is shown.
4. **Given** a booked reservation, **When** a Receptionist finds it, **Then** the list shows its last
   online cancellation time (FR-009).

---

### User Story 4 - Guest sees the last cancellation time in the confirmation email (Priority: P3)

After booking online, the Guest receives a confirmation email that states the last online cancellation
time.

**Why this priority**: Useful, but the lookup page already shows the time (User Story 3).

**Independent Test**: Book a room online and check that the confirmation email shows the same last
online cancellation time as the lookup page.

**Acceptance Scenarios**:

1. **Given** a Guest books a room online, **When** the booking is saved, **Then** the system sends a
   confirmation email to the booking email with the reservation number, room type, arrival and departure
   date, number of guests, total price and the last online cancellation time.
2. **Given** a Guest books a room online, **When** the email cannot be sent, **Then** the reservation is
   still saved as booked and the booking confirmation is shown as usual.
3. **Given** a Receptionist creates a reservation, **When** it is saved, **Then** the system sends no
   confirmation email.

---

### Edge Cases

- A reservation is booked less than 48 hours before check-in (e.g. for today or tomorrow): its last online
  cancellation time is already in the past, so it can never be cancelled online.
- The Guest cancels exactly at the last online cancellation time: rejected (the deadline is exclusive).
- A Receptionist changes the dates of a reservation: its last online cancellation time is recalculated
  from the new check-in, using the rule that applied when the reservation was booked.
- A reservation booked by a Receptionist (phone, walk-in) is looked up by the Guest online: the same
  deadline rules apply as for online bookings.
- Daylight saving time change between booking and check-in: the deadline is shown and enforced in hotel
  local time.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Every booked reservation MUST have a last online cancellation time (date and time).
- **FR-002**: For reservations booked after the change, the last online cancellation time MUST be 48 hours
  before check-in. Check-in is at 15:00 hotel local time on the arrival date, so the deadline is 15:00 two
  days before the arrival date.
- **FR-003**: Reservations booked before the change MUST keep the rule of 001 FR-015: online cancellation
  until the end of the day before the arrival date, i.e. the deadline is 00:00 on the arrival date.
- **FR-004**: Guests MUST be able to cancel a booked reservation online only before its last online
  cancellation time; afterwards the system MUST reject the cancellation and tell the Guest to call
  reception.
- **FR-005**: The guest lookup page MUST show the last online cancellation time of a booked reservation as
  "Free online cancellation until <d MMMM yyyy>, <HH:mm> (Zurich time)", e.g. "Free online cancellation
  until 10 March 2030, 15:00 (Zurich time)". The email (FR-006) and the reservation details (FR-009) use
  the same text. Once the time has passed, the lookup page MUST instead show "Online cancellation was
  possible until 10 March 2030, 15:00 (Zurich time). Please call reception to cancel." and no cancel
  button.
- **FR-006**: After a Guest books a room online, the system MUST send a confirmation email to the email
  used for booking. It MUST contain the reservation number, room type, arrival and departure date, number
  of guests, total price and the last online cancellation time. If the email cannot be sent, the booking
  MUST still be saved and confirmed to the Guest. Reservations created by a Receptionist send no email.
- **FR-007**: Receptionist cancellations MUST NOT be limited by the last online cancellation time.
- **FR-008**: When a Receptionist changes the dates of a reservation, its last online cancellation time
  MUST be recalculated from the new check-in using the rule that applied when it was booked.
- **FR-009**: Receptionists MUST see the last online cancellation time of a booked reservation in the
  list of found reservations: "Free online cancellation until <time> (Zurich time)" before the deadline,
  "Online cancellation was possible until <time> (Zurich time)." after it.

### Key Entities

- **Reservation**: gains a last online cancellation time, fixed by the rule in force when it was booked.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: No reservation booked after the change is cancelled online less than 48 hours before
  check-in.
- **SC-002**: No reservation booked before the change loses an online cancellation option it had at
  booking.
- **SC-003**: 100% of booked reservations show their last online cancellation time on the lookup page.
- **SC-004**: Within three months, room-nights left unsold because of online cancellations made less than
  48 hours before check-in drop by at least 80%.

## Assumptions

- "Booked before the change" means the reservation was created before this feature goes live.
- Hotel local time is Europe/Zurich; all deadlines are calculated, shown and enforced in it, including
  across daylight saving changes.
- The deadline depends on when a reservation was booked, not on who booked it (Guest online or
  Receptionist).
- No-shows and the receptionist cancellation flow from 001 stay unchanged.
- A mail server is available in production; failed emails are not retried, and no email is sent for
  changes or cancellations.
