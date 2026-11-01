# housekeeping Specification

## Purpose

Tracks whether each room is clean or dirty and gives housekeeping the list of rooms to clean, so that no guest is
given a room that is not clean.

## Requirements

### Requirement: Cleaning status
Every room SHALL have the cleaning status clean or dirty. A newly created room SHALL be clean.

#### Scenario: New room is clean
- **WHEN** the manager creates a room
- **THEN** the room's cleaning status is clean

### Requirement: Cleaning list
Housekeeping SHALL see a cleaning list with every dirty room, sorted by room number, showing room number and room
type.

#### Scenario: Dirty rooms listed
- **WHEN** rooms "104" and "102" are dirty and all other rooms are clean
- **AND** a Housekeeping user opens the cleaning list
- **THEN** the list shows "102" and then "104", each with its room type

#### Scenario: No dirty rooms
- **WHEN** all rooms are clean
- **AND** a Housekeeping user opens the cleaning list
- **THEN** the system shows the message "All rooms are clean"

### Requirement: Mark a room as cleaned
Housekeeping SHALL be able to mark a dirty room on the cleaning list as cleaned. The room SHALL then be clean and
SHALL leave the cleaning list at once.

#### Scenario: Room marked as cleaned
- **WHEN** a Housekeeping user marks room "104" as cleaned
- **THEN** room "104" is clean
- **AND** the cleaning list no longer shows room "104"

#### Scenario: Room already marked by someone else
- **WHEN** room "104" was marked as cleaned by another Housekeeping user after this list was shown
- **AND** the Housekeeping user marks room "104" as cleaned
- **THEN** room "104" stays clean
- **AND** the cleaning list no longer shows room "104"

### Requirement: Cleaning list on a tablet
The cleaning list SHALL be usable on a tablet: no horizontal scrolling at a width of 768 pixels and touch targets
of at least 44 × 44 pixels for marking a room as cleaned.

#### Scenario: Tablet width
- **WHEN** the cleaning list is shown at a width of 768 pixels
- **THEN** every room and its mark-as-cleaned action are visible without horizontal scrolling

### Requirement: Only housekeeping uses the cleaning list
The cleaning list SHALL be open only to the Housekeeping role.

#### Scenario: Manager opens the cleaning list
- **WHEN** a logged-in Manager opens the cleaning list
- **THEN** the system does not show the page
- **AND** tells the user that access is denied
