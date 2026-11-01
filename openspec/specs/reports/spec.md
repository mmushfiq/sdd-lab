# reports Specification

## Purpose

Shows the manager the hotel's occupancy and revenue per month, computed from the stays in the system, so no manual
spreadsheet work is needed.

## Requirements

### Requirement: Counted stays
Reports SHALL count only reservations with the status Checked in or Checked out. Each night from the arrival date up
to the night before the departure date SHALL count as one occupied room-night in the month the night falls in. A
Checked in stay SHALL count with its current departure date.

#### Scenario: Booked and cancelled reservations not counted
- **WHEN** a month has only Booked and Cancelled reservations
- **THEN** the report shows 0 occupied room-nights and revenue 0.00 for that month

#### Scenario: Stay across two months
- **WHEN** a Checked out reservation runs from 30 March to 2 April
- **THEN** the report counts 2 room-nights in March and 1 room-night in April

### Requirement: Monthly occupancy
The occupancy of a month SHALL be the occupied room-nights divided by (number of rooms × days in the month), shown
in percent with one decimal.

#### Scenario: Occupancy computed
- **WHEN** April has 60 occupied room-nights and the hotel has 10 rooms
- **THEN** the report shows the occupancy 20.0 % for April

### Requirement: Monthly revenue
The revenue of a month SHALL be the sum of the saved nightly rates of its occupied room-nights, shown in CHF with two
decimals. Later changes of a room type's nightly rate SHALL NOT change past revenue.

#### Scenario: Revenue from saved rates
- **WHEN** April has a Checked out stay of 3 nights at nightly rate 140.00 and one of 2 nights at 220.00
- **THEN** the report shows revenue 860.00 for April

#### Scenario: Rate changed after the stay
- **WHEN** the manager changes the nightly rate of the room type after a stay in April
- **THEN** the revenue for April does not change

### Requirement: Year report
The manager SHALL choose a year, by default the current year, and SHALL see one row per month from January to
December with occupied room-nights, occupancy and revenue, followed by a total row for the year.

#### Scenario: Report for the current year
- **WHEN** the manager opens the report page
- **THEN** the system shows twelve monthly rows and a total row for the current year

#### Scenario: Other year chosen
- **WHEN** the manager chooses the year 2025
- **THEN** the system shows the twelve months and the total for 2025

#### Scenario: Year without stays
- **WHEN** the manager chooses a year without counted stays
- **THEN** every month shows 0 room-nights, 0.0 % and 0.00

### Requirement: Only the manager sees reports
The report page SHALL be open only to the Manager role.

#### Scenario: Receptionist opens the report page
- **WHEN** a logged-in Receptionist opens the report page
- **THEN** the system does not show the page or any figures
- **AND** tells the user that access is denied
