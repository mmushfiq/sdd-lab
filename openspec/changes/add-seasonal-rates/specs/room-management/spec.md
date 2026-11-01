## MODIFIED Requirements

### Requirement: Room types
The system SHALL keep room types with a unique name, a capacity of at least 1 guest and a base rate greater than
zero, in CHF with two decimals. The base rate is the nightly rate for every night that no seasonal rate of the room
type covers.

#### Scenario: Manager lists room types
- **WHEN** the manager opens the room types page
- **THEN** the system lists every room type with its name, capacity and base rate

### Requirement: Create a room type
The manager SHALL be able to create a room type by entering name, capacity and base rate.

#### Scenario: Room type created
- **WHEN** the manager enters the name "Suite", capacity 3 and base rate 250.00 and saves
- **THEN** the system stores the room type
- **AND** lists it on the room types page

#### Scenario: Room type name already used
- **WHEN** the manager saves a room type with a name that another room type already has
- **THEN** the system does not save it
- **AND** shows the message "A room type with this name already exists"

#### Scenario: Missing or invalid room type values
- **WHEN** the manager saves a room type with an empty name, a capacity below 1 or a base rate of zero or less
- **THEN** the system does not save it
- **AND** marks each invalid field with an error message

### Requirement: Change a room type
The manager SHALL be able to change the name, capacity and base rate of a room type, with the same rules as for
creating one. A changed base rate SHALL apply only to prices calculated after the change; nights already saved with a
reservation SHALL keep their rate.

#### Scenario: Nightly rate changed
- **WHEN** the manager changes the base rate of a room type from 140.00 to 150.00 and saves
- **THEN** the room types page shows the base rate 150.00
- **AND** nights of rooms of that room type that no seasonal rate covers are offered at 150.00 from now on

#### Scenario: Changed name already used
- **WHEN** the manager changes a room type's name to a name that another room type already has
- **THEN** the system does not save the change
- **AND** shows the message "A room type with this name already exists"

### Requirement: Rooms
The system SHALL keep rooms with a unique room number and exactly one room type. A room's capacity and base rate
SHALL be those of its room type.

#### Scenario: Manager lists rooms
- **WHEN** the manager opens the rooms page
- **THEN** the system lists every room sorted by room number, with its room type, capacity and base rate

### Requirement: Create a room
The manager SHALL be able to create a room by entering a room number and choosing a room type.

#### Scenario: Room created
- **WHEN** the manager enters room number "111", chooses the room type "Double" and saves
- **THEN** the system stores the room
- **AND** lists it on the rooms page with the capacity and base rate of "Double"

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
- **THEN** the rooms page shows room "104" with the capacity and base rate of "Family"

#### Scenario: Changed room number already used
- **WHEN** the manager changes a room's number to a number that another room already has
- **THEN** the system does not save the change
- **AND** shows the message "A room with this number already exists"
