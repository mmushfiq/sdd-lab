# Feature Specification: Seasonal Room Rates

**Feature Branch**: `hotel-reservation-spec-kit`

**Created**: 2026-11-08

**Status**: Draft

**Input**: User description: "Right now every room type has just one nightly rate for the whole year. That doesn't work for us. In summer and around Christmas we're fully booked weeks ahead and could easily charge more. In November the rooms sit empty because our price is too high. At the moment I change the room type rate by hand before each season, and I sometimes forget to change it back."

**Changes**: Feature 001-hotel-reservation-system, FR-001, FR-009, User Story 1 (acceptance scenario 5),
User Story 6 (acceptance scenario 1, recalculated price) and the assumption "The nightly rate is the same for
every night (no seasonal or weekend rates)".

## Clarifications

### Session 2026-11-08

- Q: Is a seasonal period defined per room type, or once for the whole hotel with a rate for each room type? →
  A: Per room type; each room type has its own seasonal periods and rates.
- Q: Must the Manager enter each year's seasons again, or should a seasonal rate repeat every year
  automatically? → A: Fixed dates with a year, entered again each year; no repeating seasons.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manager sets a seasonal rate for a date period (Priority: P1)

The Manager enters, ahead of time, a seasonal rate for a date period, e.g. a higher rate for summer and
Christmas and a lower one for November. The system applies it to the nights in that period and uses the
standard nightly rate again afterwards, so the Manager no longer changes rates by hand or forgets to change
them back.

**Why this priority**: This is the core of the request: different prices in different periods without manual
switching.

**Independent Test**: Log in as Manager, add a seasonal rate for a period of a room type, then search as Guest
for a stay inside the period (seasonal rate used) and one outside it (standard rate used).

**Acceptance Scenarios**:

1. **Given** the Manager is logged in, **When** they add a seasonal rate for one room type with a start date,
   an end date and a nightly rate, **Then** it appears in the list of seasonal rates.
2. **Given** a seasonal rate exists, **When** the Manager changes its dates or rate or deletes it, **Then** the
   change applies to new bookings and price calculations only.
3. **Given** a seasonal rate exists for a period, **When** the Manager adds another one that overlaps it for the
   same room type, **Then** the system rejects it and says the periods overlap.
4. **Given** the Manager adds a seasonal rate, **When** the end date is before the start date or the rate is not
   a positive amount, **Then** the system rejects it.
5. **Given** a user is logged in as Receptionist or Housekeeping, **When** they try to open seasonal rates,
   **Then** access is denied.

---

### User Story 2 - Prices follow the rate of each night (Priority: P1)

When a Guest or Receptionist searches and books, the total price is the sum of the rate of every night of the
stay: the seasonal rate for nights inside a seasonal period, the standard nightly rate for all other nights.

**Why this priority**: Without it, seasonal rates have no effect on what guests pay.

**Independent Test**: Search a stay that starts before a seasonal period and ends inside it; the total price is
the standard rate for the nights before the period plus the seasonal rate for the nights inside it.

**Acceptance Scenarios**:

1. **Given** a room type with standard rate 100 and a seasonal rate 150 from 1 July to 31 August, **When** a
   Guest searches 10–13 July, **Then** the total price shown is 450.
2. **Given** the same rates, **When** a Guest searches 30 June – 2 July, **Then** the total price is 100 + 150 =
   250 (night of 30 June at standard rate, night of 1 July at seasonal rate).
3. **Given** a seasonal rate lower than the standard rate (e.g. November), **When** a stay in that period is
   searched, **Then** the lower rate is used.
4. **Given** a room is booked, **When** the reservation is saved, **Then** it records the rate of each night and
   the total price, and the booking confirmation and confirmation email show that total price.

---

### User Story 3 - Existing reservations keep their price (Priority: P1)

A reservation keeps the rates it was booked with, whatever the Manager later changes in seasonal or standard
rates.

**Why this priority**: Guests were quoted a price; changing it afterwards would break the hotel's word and
falsify revenue reports.

**Independent Test**: Book a stay, then add or change a seasonal rate covering it; the reservation's total price
and the revenue report do not change.

**Acceptance Scenarios**:

1. **Given** a booked reservation, **When** the Manager adds, changes or deletes a seasonal rate covering its
   nights, **Then** the reservation keeps its total price and the rate of each night.
2. **Given** reservations made before this change, **When** they are looked up or reported, **Then** every night
   keeps the single nightly rate they were booked with.
3. **Given** a booked reservation, **When** a Receptionist changes its dates or room, **Then** the total price is
   recalculated with the rates that apply to each new night at the time of the change (001 User Story 6).

---

### User Story 4 - Revenue reflects the rate of each night (Priority: P2)

The monthly revenue report counts the rate each night was booked with, so a stay crossing a season boundary or a
month end is split correctly.

**Why this priority**: The Manager needs to see whether seasonal pricing pays off; the report already exists.

**Independent Test**: Check out a stay crossing the start of a seasonal period and the end of a month; each
month's revenue counts only its own nights at their booked rate.

**Acceptance Scenarios**:

1. **Given** a checked-out stay of 30 June – 2 July with nights at 100 and 150, **When** the Manager opens the
   report for June and for July, **Then** June shows 100 and July shows 150 from this stay.

---

### Edge Cases

- A seasonal period starts or ends in the middle of a stay: each night is priced by the period it belongs to; a
  night belongs to the date it starts on.
- A seasonal period crosses the end of the year (e.g. 20 December – 6 January): allowed, the start and end dates
  carry their year. Seasonal rates do not repeat; the Manager enters each year's seasons again. Nights of a
  year without entered seasons get the standard nightly rate.
- A seasonal rate is entered for dates in the past: rejected, it could no longer affect any booking.
- The standard nightly rate of a room type is changed while seasonal rates exist: nights outside seasonal periods
  use the new standard rate for new bookings; seasonal nights are unaffected.
- A room type with seasonal rates is deleted: its seasonal rates are deleted with it (deleting a room type in use
  by a room stays rejected, 001 FR-003).
- A Guest searches dates where no seasonal rate exists: the standard nightly rate applies, as today.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Each room type MUST keep a standard nightly rate, which applies to every night not covered by a
  seasonal rate.
- **FR-002**: The Manager MUST be able to add, change and delete seasonal rates. A seasonal rate has a start
  date, an end date (inclusive: the night that starts on the end date is the last one covered) and a nightly
  rate, and applies to a room type.
- **FR-003**: The system MUST reject a seasonal rate whose end date is before its start date, whose end date is
  in the past, whose rate is not a positive amount, or which overlaps another seasonal rate of the same room
  type.
- **FR-004**: The rate of a night MUST be the seasonal rate covering that night if one exists, otherwise the
  standard nightly rate of the room type.
- **FR-005**: The total price of a stay shown in the search, booking confirmation, confirmation email, guest
  lookup and reservation details MUST be the sum of the rates of its nights.
- **FR-006**: A reservation MUST record the rate of each of its nights at booking time (replaces "the nightly
  rate at booking time" in 001 FR-009). Later changes of standard or seasonal rates MUST NOT change existing
  reservations.
- **FR-007**: When a Receptionist changes the dates or room of a reservation, the system MUST recalculate the
  rate of each night and the total price with the rates in force at the time of the change.
- **FR-008**: Reservations made before this change MUST keep their prices; each of their nights has the single
  nightly rate they were booked with.
- **FR-009**: Monthly revenue (001 FR-021) MUST sum the recorded rates of the month's nights of checked-in and
  checked-out reservations.
- **FR-010**: Only the Manager MUST be able to see and maintain seasonal rates.

### Key Entities

- **Room Type**: keeps name, capacity and its standard nightly rate.
- **Seasonal Rate**: a nightly rate for a room type that applies to the nights from a start date to an end date.
- **Reservation**: records the rate of each night instead of one nightly rate; the total price is their sum.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The Manager can enter a year's seasonal rates for all room types in under 15 minutes and changes no
  rate by hand at the start or end of a season.
- **SC-002**: 100% of nights booked inside a seasonal period are priced at that period's rate, and 100% of
  nights outside any period at the standard rate.
- **SC-003**: No existing reservation's total price changes because of a rate change.
- **SC-004**: Within one year, room revenue in the summer and Christmas periods rises and November occupancy
  rises compared with the year before, as shown in the monthly report.

## Assumptions

- Seasonal rates are absolute nightly amounts, not percentages of the standard rate; they can be higher or lower
  than it.
- Rates are per room per night, independent of the number of guests, as today.
- Guests see the total price as today; no per-night price breakdown is shown.
- No weekend, length-of-stay or last-minute pricing; only date periods.
- Prices are in the same currency as today; payment stays at the hotel (vision: out of scope).
- Changing the dates of a reservation reprices it with current rates, as 001 User Story 6 already does; this may
  raise or lower its price.
