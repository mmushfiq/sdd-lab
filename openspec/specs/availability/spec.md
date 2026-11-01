# availability Specification

## Purpose

Finds the rooms that are free for every night of a date range and large enough for the number of guests, so that
reception and guests only ever book rooms that can be booked.

## Requirements

### Requirement: Availability search
The system SHALL list, for an arrival date, a departure date and a number of guests, every room whose capacity is at
least the number of guests and that has no reservation other than a cancelled one for any night from the arrival
date up to the night before the departure date.

#### Scenario: Free room is listed
- **WHEN** someone searches from 10 March to 12 March for 2 guests and room "104" (Double) has no reservation in
  that period
- **THEN** room "104" is listed

#### Scenario: Room with an overlapping reservation is not listed
- **WHEN** room "104" has a Booked reservation from 11 March to 13 March
- **AND** someone searches from 10 March to 12 March
- **THEN** room "104" is not listed

#### Scenario: Departure day is free for the next guest
- **WHEN** room "104" has a Booked reservation from 8 March to 10 March
- **AND** someone searches from 10 March to 12 March
- **THEN** room "104" is listed

#### Scenario: Cancelled reservation does not block the room
- **WHEN** room "104" has only a cancelled reservation from 10 March to 12 March
- **AND** someone searches from 10 March to 12 March
- **THEN** room "104" is listed

#### Scenario: Room too small is not listed
- **WHEN** someone searches for 3 guests
- **THEN** no room whose room type has a capacity below 3 is listed

#### Scenario: No room available
- **WHEN** no room fits the search
- **THEN** the system shows the message "No rooms available for these dates"

### Requirement: Search result details
Each listed room SHALL show its room number, room type, capacity and the total price for the stay. The total price
SHALL be the sum of the rates of its nights for the room's room type, each night priced on its own.

#### Scenario: Total price shown
- **WHEN** room "104" (Double, base rate 140.00, no seasonal rate for these nights) is listed for a stay of 2 nights
- **THEN** the result shows the total price 280.00

#### Scenario: Total price across the start of a season
- **WHEN** "Double" has the base rate 140.00 and the seasonal rate "Christmas" from 20 December 2030 at 180.00
- **AND** room "104" (Double) is listed for a stay from 18 December to 21 December 2030
- **THEN** the result shows the total price 460.00

#### Scenario: No single nightly rate shown
- **WHEN** rooms are listed for a stay
- **THEN** the result shows no nightly rate column

### Requirement: Valid search input
The system SHALL search only when the arrival date is today or later, the departure date is after the arrival
date, and the number of guests is at least 1.

#### Scenario: Arrival date in the past
- **WHEN** someone searches with an arrival date before today
- **THEN** the system does not search
- **AND** shows the message "Arrival date cannot be in the past"

#### Scenario: Departure not after arrival
- **WHEN** someone searches with a departure date on or before the arrival date
- **THEN** the system does not search
- **AND** shows the message "Departure date must be after arrival date"

#### Scenario: No guests
- **WHEN** someone searches with fewer than 1 guest
- **THEN** the system does not search
- **AND** shows the message "At least 1 guest is required"
