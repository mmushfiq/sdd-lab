## MODIFIED Requirements

### Requirement: Reservation content
A reservation SHALL be for exactly one room and SHALL hold the arrival date, the departure date, the number of
guests, the guest's name and email, an optional phone number, the rate of each night, a reservation number and a
status.

#### Scenario: Reservation details shown
- **WHEN** the receptionist opens a reservation
- **THEN** the system shows its reservation number, room, arrival date, departure date, number of guests, guest
  name, email, phone number, price per night, total price and status

#### Scenario: Price per night shown
- **WHEN** the receptionist opens a reservation for room "104" from 18 December to 21 December 2030 with the night
  rates 140.00, 140.00 and 180.00
- **THEN** the system shows the price per night "2 nights × CHF 140.00, 1 night × CHF 180.00"
- **AND** the total price CHF 460.00

### Requirement: Nightly rate saved with the reservation
When a reservation is created, the system SHALL save for each night the rate of that night for the room's room type.
Its total price SHALL be the sum of the saved rates of its nights. Later changes of base rates or seasonal rates SHALL
NOT change them.

#### Scenario: Stay across the start of a season
- **WHEN** "Double" has the base rate 140.00 and the seasonal rate "Christmas" from 20 December 2030 at 180.00
- **AND** a reservation for room "104" (Double) from 18 December to 21 December 2030 is created
- **THEN** the reservation has the rate 140.00 for the nights of 18 and 19 December and 180.00 for the night of
  20 December
- **AND** its total price is 460.00

#### Scenario: Rate change after booking
- **WHEN** a reservation for room "104" was created with the rate 140.00 for each night
- **AND** the manager then changes the base rate of "Double" to 150.00
- **THEN** the reservation still has the rate 140.00 for each night

### Requirement: Change a reservation
The receptionist SHALL be able to change the arrival date, departure date, room and number of guests of a Booked
reservation. The changed reservation SHALL meet the same rules as a new one; the reservation itself SHALL NOT count
as an overlap. Nights the reservation already had SHALL keep their saved rate; added nights SHALL get the current rate
of that night. When the reservation moves to a room of another room type, every night SHALL get that room type's
current rate.

#### Scenario: Moved to another room type
- **WHEN** the receptionist moves a Booked reservation from room "104" (Double, saved rate 140.00 for each night) to
  room "109" (Family, current base rate 220.00 and no seasonal rate for these nights)
- **THEN** the reservation has the rate 220.00 for each night

#### Scenario: Dates extended
- **WHEN** the receptionist changes a Booked reservation for room "104" from 10–12 March to 10–13 March and the
  night of 12 March is free
- **THEN** the system saves the change

#### Scenario: Extended into a season
- **WHEN** a Booked reservation for room "104" from 18 December to 20 December 2030 has the saved rate 130.00 for
  both nights
- **AND** "Double" now has the base rate 140.00 and the seasonal rate "Christmas" from 20 December 2030 at 180.00
- **AND** the receptionist changes the departure date to 22 December 2030
- **THEN** the reservation keeps the rate 130.00 for the nights of 18 and 19 December
- **AND** has the rate 180.00 for the nights of 20 and 21 December

#### Scenario: Change creates an overlap
- **WHEN** the receptionist changes a Booked reservation so that it overlaps another reservation on the same room
  that is not cancelled
- **THEN** the system does not save the change
- **AND** shows the message "The room is no longer available for these dates"

#### Scenario: Reservation is not Booked
- **WHEN** the receptionist opens a reservation whose status is not Booked
- **THEN** the system does not offer to change it
