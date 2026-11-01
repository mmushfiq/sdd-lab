# Spec Delta

## Purpose

Lets the manager set a different nightly rate for a room type during fixed date ranges such as summer, Christmas or
November, so prices follow the season without changing the base rate by hand.

## ADDED Requirements

### Requirement: Seasonal rates
The system SHALL keep seasonal rates. A seasonal rate SHALL belong to exactly one room type and SHALL have a name, a
first night, a last night on or after the first night, and a nightly rate greater than zero in CHF with two
decimals. It covers every night from the first night up to and including the last night.

#### Scenario: Manager lists seasonal rates
- **WHEN** the manager opens the seasonal rates page
- **THEN** the system lists every seasonal rate sorted by room type and first night, with room type, name, first
  night, last night and nightly rate

### Requirement: Rate of a night
The rate of a night for a room type SHALL be the nightly rate of the seasonal rate of that room type that covers the
night. When no seasonal rate of that room type covers the night, the rate SHALL be the room type's base rate.

#### Scenario: Night inside a season
- **WHEN** room type "Double" has the base rate 140.00 and the seasonal rate "Christmas" from 20 December 2030 to
  6 January 2031 at 180.00
- **THEN** the rate of "Double" for the night of 24 December 2030 is 180.00

#### Scenario: Night outside every season
- **WHEN** room type "Double" has the base rate 140.00 and the seasonal rate "Christmas" from 20 December 2030 to
  6 January 2031 at 180.00
- **THEN** the rate of "Double" for the night of 7 January 2031 is 140.00

#### Scenario: Season cheaper than the base rate
- **WHEN** room type "Double" has the base rate 140.00 and the seasonal rate "November" from 1 November 2030 to
  30 November 2030 at 110.00
- **THEN** the rate of "Double" for the night of 15 November 2030 is 110.00

#### Scenario: Season of another room type
- **WHEN** only room type "Family" has a seasonal rate covering 24 December 2030
- **THEN** the rate of "Double" for the night of 24 December 2030 is the base rate of "Double"

### Requirement: Create a seasonal rate
The manager SHALL be able to create a seasonal rate by choosing a room type and entering name, first night, last
night and nightly rate.

#### Scenario: Seasonal rate created
- **WHEN** the manager chooses room type "Double", enters the name "Christmas", first night 20 December 2030, last
  night 6 January 2031 and nightly rate 180.00 and saves
- **THEN** the system stores the seasonal rate
- **AND** lists it on the seasonal rates page

#### Scenario: Missing or invalid seasonal rate values
- **WHEN** the manager saves a seasonal rate without a room type, without a name, without a first night, without a
  last night, or with a nightly rate of zero or less or with more than two decimals
- **THEN** the system does not save it
- **AND** marks each invalid field with an error message

#### Scenario: Last night before first night
- **WHEN** the manager saves a seasonal rate with first night 6 January 2031 and last night 20 December 2030
- **THEN** the system does not save it
- **AND** shows the message "Last night cannot be before first night"

#### Scenario: Overlapping seasonal rate
- **WHEN** room type "Double" has the seasonal rate "Christmas" from 20 December 2030 to 6 January 2031
- **AND** the manager saves a seasonal rate for "Double" from 1 January 2031 to 15 January 2031
- **THEN** the system does not save it
- **AND** shows the message "The dates overlap the seasonal rate \"Christmas\" of this room type"

#### Scenario: Same dates for another room type
- **WHEN** room type "Double" has the seasonal rate "Christmas" from 20 December 2030 to 6 January 2031
- **AND** the manager saves a seasonal rate for "Family" from 20 December 2030 to 6 January 2031
- **THEN** the system stores it

### Requirement: Change a seasonal rate
The manager SHALL be able to change the room type, name, first night, last night and nightly rate of a seasonal rate,
with the same rules as for creating one. The seasonal rate itself SHALL NOT count as an overlap. A change SHALL apply
only to prices calculated after it; nights already saved with a reservation SHALL keep their rate.

#### Scenario: Seasonal rate changed
- **WHEN** the manager changes the nightly rate of the seasonal rate "Christmas" of "Double" from 180.00 to 190.00 and
  saves
- **THEN** the seasonal rates page shows "Christmas" with the nightly rate 190.00
- **AND** nights of "Double" from 20 December 2030 to 6 January 2031 are offered at 190.00 from now on

#### Scenario: Existing reservation keeps its rates
- **WHEN** a reservation for a "Double" room was made for the night of 24 December 2030 at 180.00
- **AND** the manager then changes the seasonal rate "Christmas" of "Double" to 190.00
- **THEN** the reservation still has the rate 180.00 for the night of 24 December 2030

#### Scenario: Changed dates overlap
- **WHEN** the manager changes a seasonal rate of "Double" so that it overlaps another seasonal rate of "Double"
- **THEN** the system does not save the change
- **AND** shows the message that names the overlapped seasonal rate

### Requirement: Delete a seasonal rate
The manager SHALL be able to delete a seasonal rate. Nights it covered SHALL then have the base rate for prices
calculated afterwards. Nights already saved with a reservation SHALL keep their rate.

#### Scenario: Seasonal rate deleted
- **WHEN** the manager deletes the seasonal rate "Christmas" of "Double"
- **THEN** the seasonal rates page no longer lists it
- **AND** the night of 24 December 2030 of "Double" is offered at the base rate of "Double"

#### Scenario: Reservation after deletion unchanged
- **WHEN** a reservation for a "Double" room was made for the night of 24 December 2030 at 180.00
- **AND** the manager then deletes the seasonal rate "Christmas" of "Double"
- **THEN** the reservation still has the rate 180.00 for the night of 24 December 2030

### Requirement: Only the manager maintains seasonal rates
The seasonal rates page SHALL be open only to the Manager role.

#### Scenario: Receptionist opens the seasonal rates page
- **WHEN** a logged-in Receptionist opens the seasonal rates page
- **THEN** the system does not show the page
- **AND** tells the user that access is denied
