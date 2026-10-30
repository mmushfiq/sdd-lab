# Requirements Document

## Project Description (Input)
The manager builds monthly occupancy and revenue figures by hand in spreadsheets. Reservations with their statuses and
booked nightly rates exist (reservation-core); check-in and check-out days are recorded per stay (reception). The
manager picks a period (for example a month) and sees the occupancy rate and the revenue for the same period, with a
monthly breakdown; the figures match the definitions recorded in `domain.md`. Approach: manager-only, read-only staff
page backed by aggregate queries over reservations and stays; no new tables. In scope: occupancy report and revenue
report for a chosen period, with monthly breakdown. Out of scope: export, charts, forecasting, writing any data,
pricing rules (reservation-core). To decide here: which nights count as occupied and which reservations count as
revenue (open question in `domain.md`). Source: `.kiro/specs/reports/brief.md`.

## Introduction
Reports give the manager of the Hotel Reservation System (HRS) the occupancy rate and the revenue of a chosen period,
broken down by calendar month, on one staff page. The figures follow fixed, documented definitions (RP1–RP8) so that
the manager no longer needs spreadsheets and every month is counted the same way.

## Boundary Context
- **In scope**: one report page for managers; choosing a period of nights; one row per calendar month of the period and
  a total row; room nights available, occupied nights, occupancy rate and revenue per row; the definitions RP1–RP8.
- **Out of scope**: export (file, print), charts, forecasts and figures for nights after today; any change to
  reservations, stays, rooms or rates; pricing rules (reservation-core); recording check-in and check-out days
  (reception); room and rate maintenance (management); per-room or per-room-type breakdowns; payments.
- **Adjacent expectations**: reservation-core keeps the booked nightly rate and the status of every reservation and
  never deletes reservations, also after guest data is erased; reception records the check-in day and check-out day of
  every stay made through its pages and defines what a stay is (status Checked in or Checked out); management can add
  rooms and can remove only rooms that never had a reservation; foundation provides the staff layout, the manager role
  and "today".

## Assumptions and Decisions
Decided here because `domain.md` leaves the report definitions open; changing one requires revisiting this spec and
`domain.md`.

- **RP1 Night of a date**: the night of a date is the night from that date to the next day, as for reservations
  (arrival inclusive, departure exclusive). A period is given by its first and its last night, both included.
- **RP2 Period limit**: a period may end at the latest with tonight (today's night). Reports show what happened, not
  forecasts; future nights are never counted.
- **RP3 Available room nights**: the number of rooms the hotel has when the report is shown, multiplied by the nights of
  the period. The HRS keeps no history of when a room was added; a room added later also counts for earlier periods. A
  removed room never had a reservation (management), so removing it changes only past available room nights, never
  occupied nights or revenue.
- **RP4 Occupied night**: a room night is occupied when a guest actually stayed in the room that night: a reservation
  that is a stay (Checked in or Checked out) occupies every night from its check-in day up to the night before its
  check-out day. A guest still checked in occupies every night from the check-in day up to and including tonight, also
  past the departure date (Overdue). A stay without a recorded check-in day (set outside the reception pages, reception
  read contract) counts from its arrival date to the night before its departure date, also while Checked in. Reserved reservations (including no-shows) and Cancelled reservations never occupy a
  night.
- **RP5 Revenue**: revenue is earned by night, not at check-out: every reservation that is a stay earns its booked
  nightly rate for each of its booked nights (arrival date up to the night before the departure date), in the month of
  that night. Because the price of a reservation does not change with a late check-in, an early check-out or a late
  check-out (reception R3–R5), revenue counts all booked nights of a stay and no night after the departure date.
  Reserved and Cancelled reservations earn nothing.
- **RP6 Rounding**: the occupancy rate is occupied nights divided by available room nights, in percent, rounded half up
  to one decimal place. Revenue is the exact sum of booked nightly rates and is shown with two decimal places, without
  a currency sign (as elsewhere in the HRS).
- **RP7 Total row**: the total row is computed from the summed nights and amounts of the whole period, never as an
  average of monthly rates.
- **RP8 Default period**: the page opens with the previous calendar month, the usual monthly figure.

## Requirements

### Requirement 1: Reports page and access
**Objective:** As a manager, I want one staff page with my occupancy and revenue figures, so that I no longer build them
in spreadsheets.

#### Acceptance Criteria
1. When a manager opens the reports page, the HRS shall show the period selection with the first and last night of the previous calendar month and the report for that period (RP8).
2. The HRS shall list the reports page in the manager's staff navigation menu.
3. If a receptionist or housekeeper opens the reports page, including by its address, the HRS shall refuse it in the same way as any staff page the role may not open and shall not list it in their navigation menu.
4. If a person who is not signed in opens the reports page, the HRS shall show the login page instead.
5. When the reports page is shown at desktop width, the HRS shall show every column and row of the report without horizontal scrolling.
6. The HRS shall show on the reports page a short explanation of how available room nights, occupied nights and revenue are counted (RP3–RP5).

### Requirement 2: Period and monthly breakdown
**Objective:** As a manager, I want to choose any period and see it month by month, so that I can compare months and
read the figure for the whole period.

#### Acceptance Criteria
1. When the manager chooses a first night and a last night and shows the report, the HRS shall show one row per calendar month that has at least one night in the period, in chronological order and labelled with year and month, followed by one total row.
2. The HRS shall compute the figures of a month row only from the nights of that month that lie inside the period, so that the first and the last month may be partial months.
3. The HRS shall compute the figures of the total row from all nights of the period (RP7).
4. The HRS shall include both the first night and the last night in the period, where the night of a date is the night from that date to the next day (RP1).
5. If the first night or the last night is missing, the HRS shall show a message asking for both and show no figures.
6. If the last night is before the first night, the HRS shall show a message that the last night must not be before the first night and show no figures.
7. If the last night is after today, the HRS shall show a message that the period must end on or before today, naming today's date, and show no figures (RP2).

### Requirement 3: Occupancy
**Objective:** As a manager, I want the occupancy rate to reflect the nights guests really stayed, so that I know how
well the hotel was used.

#### Acceptance Criteria
1. The HRS shall show for every row the available room nights as the number of rooms of the hotel at the moment the report is shown multiplied by the number of nights of the row (RP3).
2. The HRS shall show for every row the occupied nights as the number of nights of the row on which a reservation that is a stay occupied its room, counting each stay from its check-in day up to the night before its check-out day (RP4).
3. While a guest is checked in with a recorded check-in day, the HRS shall count as occupied every night from the check-in day up to and including tonight, also nights after the departure date.
4. Where a reservation is a stay but has no recorded check-in day, the HRS shall count its nights from the arrival date up to the night before the departure date as occupied, also while it is Checked in.
5. The HRS shall not count as occupied any night of a Reserved or Cancelled reservation, nor a booked night before the check-in day or from the check-out day on.
6. The HRS shall show for every row the occupancy rate as occupied nights divided by available room nights in percent, rounded half up to one decimal place and followed by a percent sign (RP6).
7. If a row has no available room nights, the HRS shall show its occupancy rate as "–" instead of a number.

### Requirement 4: Revenue
**Objective:** As a manager, I want the revenue of each month to match the prices of the reservations that were stayed,
so that I can compare it with what the hotel charged.

#### Acceptance Criteria
1. The HRS shall show for every row the revenue as the sum, over every reservation that is a stay, of its booked nightly rate for each of its booked nights (arrival date up to the night before the departure date) that lies in the row (RP5).
2. The HRS shall use the nightly rate booked with the reservation and not the current nightly rate of its room type.
3. The HRS shall count all booked nights of a stay as revenue also when the guest checked in after the arrival date or checked out before the departure date, and shall count no revenue for nights after the departure date.
4. The HRS shall count no revenue for Reserved or Cancelled reservations.
5. The HRS shall show revenue as the exact sum with two decimal places and without a currency sign (RP6).

### Requirement 5: Read-only, current and private
**Objective:** As the hotel, I want reports to read the data as it is without changing it or exposing guests, so that
reports are safe to open at any time.

#### Acceptance Criteria
1. The HRS shall not change any reservation, stay, room, rate or cleaning status when a report is shown.
2. When the manager shows the report again or opens the reports page again, the HRS shall compute the figures from the data at that moment, including check-ins and check-outs made meanwhile by other staff members.
3. The HRS shall show no guest personal data in the report and shall show the same figures for a period before and after the guest data of its reservations was erased.
