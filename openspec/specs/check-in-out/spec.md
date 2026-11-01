# check-in-out Specification

## Purpose

Gives reception an at-a-glance overview of every room and lets it check guests in to clean rooms and check them out,
handing the room to housekeeping right away.

## Requirements

### Requirement: Room overview
The receptionist SHALL see every room sorted by room number with its room type, its cleaning status and its state
for today: Occupied (a reservation is Checked in), Arrival today (a Booked reservation arrives today) or Free. For
Occupied and Arrival today the overview SHALL show the guest name and the departure date.

#### Scenario: Free room
- **WHEN** room "101" has no Checked in reservation and no Booked reservation arriving today
- **THEN** the overview shows room "101" as Free with its cleaning status

#### Scenario: Arrival today
- **WHEN** room "104" has a Booked reservation for "Anna Meier" arriving today
- **THEN** the overview shows room "104" as Arrival today with "Anna Meier" and the departure date

#### Scenario: Occupied room
- **WHEN** room "105" has a Checked in reservation for "Ben Keller"
- **THEN** the overview shows room "105" as Occupied with "Ben Keller" and the departure date

#### Scenario: Dirty room
- **WHEN** room "106" is dirty
- **THEN** the overview shows room "106" with the cleaning status dirty

### Requirement: Check in
The receptionist SHALL be able to check in a Booked reservation on its arrival date when its room is clean and no
other reservation for that room is Checked in. The reservation SHALL then be Checked in.

#### Scenario: Successful check-in
- **WHEN** room "104" is clean and free and has a Booked reservation arriving today
- **AND** the receptionist checks the guest in
- **THEN** the reservation's status is Checked in
- **AND** the overview shows room "104" as Occupied

#### Scenario: Room is not clean
- **WHEN** room "104" is dirty and has a Booked reservation arriving today
- **AND** the receptionist tries to check the guest in
- **THEN** the system does not check the guest in
- **AND** shows the message "The room is not clean yet"

#### Scenario: Room is still occupied
- **WHEN** room "104" has a Checked in reservation departing today that is not checked out yet
- **AND** the receptionist tries to check in the next reservation for room "104" arriving today
- **THEN** the system does not check the guest in
- **AND** shows the message "The room is still occupied"

#### Scenario: Not the arrival date
- **WHEN** a Booked reservation's arrival date is not today
- **THEN** the system does not offer to check it in

### Requirement: Check out
The receptionist SHALL be able to check out a Checked in reservation. The reservation SHALL then be Checked out and
its room SHALL be dirty at once, so it appears on the housekeeping cleaning list.

#### Scenario: Successful check-out
- **WHEN** the receptionist checks out the Checked in reservation of room "105"
- **THEN** the reservation's status is Checked out
- **AND** room "105" is dirty
- **AND** room "105" is on the cleaning list
- **AND** the overview shows room "105" as Free and dirty

#### Scenario: Early check-out
- **WHEN** a guest of room "105" checked in on 10 March with departure date 14 March checks out on 12 March
- **THEN** the reservation's departure date becomes 12 March
- **AND** room "105" is available again from 12 March

#### Scenario: Check-out on the arrival day
- **WHEN** a guest checks out on the same day they checked in
- **THEN** the reservation's departure date becomes the day after the arrival date, if it was later before

#### Scenario: Only Checked in reservations can be checked out
- **WHEN** a reservation is not Checked in
- **THEN** the system does not offer to check it out

### Requirement: Reservation statuses after check-in
A reservation SHALL have one of the statuses Booked, Cancelled, Checked in or Checked out. Checked in and Checked out
reservations SHALL still block their room for their nights.

#### Scenario: Checked in reservation shown in the reservation list
- **WHEN** the receptionist opens the reservations page after a guest has checked in
- **THEN** the guest's reservation shows the status Checked in

### Requirement: Only reception checks guests in and out
The room overview SHALL be open only to the Receptionist role.

#### Scenario: Housekeeping opens the room overview
- **WHEN** a logged-in Housekeeping user opens the room overview
- **THEN** the system does not show the page or any guest data
- **AND** tells the user that access is denied
