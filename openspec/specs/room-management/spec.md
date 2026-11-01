# room-management Specification

## Purpose

Lets the manager maintain the hotel's rooms and room types, with the capacity and nightly rate that availability
search, reservations and reports rely on.

## Requirements

### Requirement: Room types
The system SHALL keep room types with a unique name, a capacity of at least 1 guest and a nightly rate greater than
zero, in CHF with two decimals.

#### Scenario: Manager lists room types
- **WHEN** the manager opens the room types page
- **THEN** the system lists every room type with its name, capacity and nightly rate

### Requirement: Create a room type
The manager SHALL be able to create a room type by entering name, capacity and nightly rate.

#### Scenario: Room type created
- **WHEN** the manager enters the name "Suite", capacity 3 and nightly rate 250.00 and saves
- **THEN** the system stores the room type
- **AND** lists it on the room types page

#### Scenario: Room type name already used
- **WHEN** the manager saves a room type with a name that another room type already has
- **THEN** the system does not save it
- **AND** shows the message "A room type with this name already exists"

#### Scenario: Missing or invalid room type values
- **WHEN** the manager saves a room type with an empty name, a capacity below 1 or a nightly rate of zero or less
- **THEN** the system does not save it
- **AND** marks each invalid field with an error message

### Requirement: Change a room type
The manager SHALL be able to change the name, capacity and nightly rate of a room type, with the same rules as for
creating one. A changed nightly rate SHALL apply only to reservations made after the change.

#### Scenario: Nightly rate changed
- **WHEN** the manager changes the nightly rate of a room type from 140.00 to 150.00 and saves
- **THEN** the room types page shows the nightly rate 150.00
- **AND** rooms of that room type are offered at 150.00 from now on

#### Scenario: Changed name already used
- **WHEN** the manager changes a room type's name to a name that another room type already has
- **THEN** the system does not save the change
- **AND** shows the message "A room type with this name already exists"

### Requirement: Rooms
The system SHALL keep rooms with a unique room number and exactly one room type. A room's capacity and nightly rate
SHALL be those of its room type.

#### Scenario: Manager lists rooms
- **WHEN** the manager opens the rooms page
- **THEN** the system lists every room sorted by room number, with its room type, capacity and nightly rate

### Requirement: Create a room
The manager SHALL be able to create a room by entering a room number and choosing a room type.

#### Scenario: Room created
- **WHEN** the manager enters room number "111", chooses the room type "Double" and saves
- **THEN** the system stores the room
- **AND** lists it on the rooms page with the capacity and nightly rate of "Double"

#### Scenario: Room number already used
- **WHEN** the manager saves a room with a room number that another room already has
- **THEN** the system does not save it
- **AND** shows the message "A room with this number already exists"

#### Scenario: Missing room values
- **WHEN** the manager saves a room without a room number or without a room type
- **THEN** the system does not save it
- **AND** marks each missing field with an error message

### Requirement: Change a room
The manager SHALL be able to change a room's room number and room type, with the same rules as for creating one.

#### Scenario: Room type of a room changed
- **WHEN** the manager changes room "104" from room type "Double" to "Family" and saves
- **THEN** the rooms page shows room "104" with the capacity and nightly rate of "Family"

#### Scenario: Changed room number already used
- **WHEN** the manager changes a room's number to a number that another room already has
- **THEN** the system does not save the change
- **AND** shows the message "A room with this number already exists"

### Requirement: No deletion of rooms and room types
The system SHALL NOT offer deleting rooms or room types.

#### Scenario: No delete action
- **WHEN** the manager opens the rooms page or the room types page
- **THEN** the system offers no action to delete a room or a room type

### Requirement: Installed rooms
The system SHALL be installed with the hotel's ten rooms and their room types.

#### Scenario: Ten rooms after installation
- **WHEN** the system has just been installed
- **THEN** the rooms page lists ten rooms, each with a room type

### Requirement: Only the manager maintains rooms
The room types page and the rooms page SHALL be open only to the Manager role.

#### Scenario: Receptionist opens the rooms page
- **WHEN** a logged-in Receptionist opens the rooms page
- **THEN** the system does not show the page
- **AND** tells the user that access is denied
