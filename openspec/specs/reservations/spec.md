# reservations Specification

## Purpose

Records which guest has reserved which room for which nights, guarantees that a room is never reserved twice for
overlapping nights, and lets reception create, change and cancel reservations.

## Requirements

### Requirement: Reservation content
A reservation SHALL be for exactly one room and SHALL hold the arrival date, the departure date, the number of
guests, the guest's name and email, an optional phone number, the nightly rate, a reservation number and a status.

#### Scenario: Reservation details shown
- **WHEN** the receptionist opens a reservation
- **THEN** the system shows its reservation number, room, arrival date, departure date, number of guests, guest
  name, email, phone number, nightly rate, total price and status

### Requirement: No double booking
The system SHALL NOT allow two reservations that are not cancelled to overlap on the same room. Two reservations
overlap when they share at least one night; the departure day is not a night of the stay. This SHALL hold even
when two bookings for the same room are saved at the same moment.

#### Scenario: Overlapping reservation rejected
- **WHEN** room "104" has a Booked reservation from 10 March to 12 March
- **AND** the receptionist saves a reservation for room "104" from 11 March to 14 March
- **THEN** the system does not save it
- **AND** shows the message "The room is no longer available for these dates"

#### Scenario: Back-to-back reservations allowed
- **WHEN** room "104" has a Booked reservation from 10 March to 12 March
- **AND** the receptionist saves a reservation for room "104" from 12 March to 14 March
- **THEN** the system saves it

#### Scenario: Two bookings at the same moment
- **WHEN** two reservations for room "104" with overlapping nights are saved at the same moment
- **THEN** the system saves exactly one of them
- **AND** rejects the other with the message "The room is no longer available for these dates"

### Requirement: Create a reservation
The receptionist SHALL be able to create a reservation by searching availability, choosing a listed room and
entering the guest's name, email and optional phone number. A new reservation SHALL have the status Booked.

#### Scenario: Reservation created
- **WHEN** the receptionist searches from 10 March to 12 March for 2 guests, chooses room "104" and enters the name
  "Anna Meier" and the email "anna@example.com"
- **THEN** the system saves a Booked reservation for room "104" from 10 March to 12 March for 2 guests
- **AND** shows its reservation number

#### Scenario: Missing guest data
- **WHEN** the receptionist saves a reservation without a guest name or without an email
- **THEN** the system does not save it
- **AND** marks each missing field with an error message

#### Scenario: Invalid email
- **WHEN** the receptionist enters an email that is not a valid email address
- **THEN** the system does not save the reservation
- **AND** shows the message "Enter a valid email address"

#### Scenario: Too many guests for the room
- **WHEN** the receptionist saves a reservation with more guests than the capacity of the room's room type
- **THEN** the system does not save it
- **AND** shows the message "Too many guests for this room"

### Requirement: Reservation number
Every reservation SHALL get a reservation number that is unique and hard to guess: 8 characters chosen at random
from capital letters and digits, without the easily confused characters 0, O, 1 and I.

#### Scenario: Reservation number format
- **WHEN** a reservation is created
- **THEN** its reservation number has 8 characters from the allowed set
- **AND** no other reservation has the same reservation number

### Requirement: Nightly rate saved with the reservation
A reservation SHALL keep the nightly rate of its room type at the time it was created. Its total price SHALL be the
saved nightly rate × number of nights. Later changes of the room type's nightly rate SHALL NOT change it.

#### Scenario: Rate change after booking
- **WHEN** a reservation for room "104" was created at nightly rate 140.00
- **AND** the manager then changes the nightly rate of "Double" to 150.00
- **THEN** the reservation still shows nightly rate 140.00

### Requirement: List reservations
The receptionist SHALL be able to list reservations sorted by arrival date, by default those whose departure date
is today or later, and SHALL be able to search them by reservation number or guest name.

#### Scenario: Upcoming reservations listed
- **WHEN** the receptionist opens the reservations page
- **THEN** the system lists all reservations with a departure date of today or later, sorted by arrival date, with
  reservation number, guest name, room, arrival date, departure date and status

#### Scenario: Search by guest name
- **WHEN** the receptionist searches for "Meier"
- **THEN** the system lists only reservations whose guest name contains "Meier"

### Requirement: Change a reservation
The receptionist SHALL be able to change the arrival date, departure date, room and number of guests of a Booked
reservation. The changed reservation SHALL meet the same rules as a new one; the reservation itself SHALL NOT count
as an overlap. The saved nightly rate SHALL stay, unless the reservation moves to a room of another room type; then
it SHALL become that room type's current nightly rate.

#### Scenario: Moved to another room type
- **WHEN** the receptionist moves a Booked reservation from room "104" (Double, saved rate 140.00) to room "109"
  (Family, current rate 220.00)
- **THEN** the reservation's nightly rate is 220.00

#### Scenario: Dates extended
- **WHEN** the receptionist changes a Booked reservation for room "104" from 10–12 March to 10–13 March and the
  night of 12 March is free
- **THEN** the system saves the change

#### Scenario: Change creates an overlap
- **WHEN** the receptionist changes a Booked reservation so that it overlaps another reservation on the same room
  that is not cancelled
- **THEN** the system does not save the change
- **AND** shows the message "The room is no longer available for these dates"

#### Scenario: Reservation is not Booked
- **WHEN** the receptionist opens a reservation whose status is not Booked
- **THEN** the system does not offer to change it

### Requirement: Cancel a reservation
The receptionist SHALL be able to cancel a Booked reservation after confirming. A cancelled reservation SHALL keep
its data and SHALL no longer block its room.

#### Scenario: Reservation cancelled
- **WHEN** the receptionist cancels a Booked reservation and confirms
- **THEN** the reservation's status is Cancelled
- **AND** its room is available again for those nights

#### Scenario: Cancellation not confirmed
- **WHEN** the receptionist chooses to cancel a reservation but does not confirm
- **THEN** the reservation stays Booked

#### Scenario: Only Booked reservations can be cancelled
- **WHEN** the receptionist opens a reservation whose status is not Booked
- **THEN** the system does not offer to cancel it

### Requirement: Only reception manages reservations
The reservations page SHALL be open only to the Receptionist role.

#### Scenario: Housekeeping opens the reservations page
- **WHEN** a logged-in Housekeeping user opens the reservations page
- **THEN** the system does not show the page or any guest data
- **AND** tells the user that access is denied

### Requirement: Changed reservation keeps its deadline rule
When the receptionist changes the arrival date of a reservation, the reservation SHALL keep the online cancellation
deadline rule it was created with, and its deadline SHALL be calculated again from the new arrival date.

#### Scenario: New-rule reservation moved
- **WHEN** the receptionist changes a reservation created under the 48-hour rule from arrival 10 March 2030 to arrival
  20 March 2030
- **THEN** its online cancellation deadline is 18 March 2030, 14:00

#### Scenario: Reservation from before the rule moved
- **WHEN** the receptionist changes a reservation created before the 48-hour rule from arrival 10 March 2030 to
  arrival 20 March 2030
- **THEN** its online cancellation deadline is 20 March 2030, 00:00
