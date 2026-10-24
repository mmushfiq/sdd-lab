# Use Case: Maintain Rooms

## Overview

**Use Case ID:** UC-017  
**Use Case Name:** Maintain Rooms  
**Primary Actor:** Manager  
**Goal:** The manager creates and edits rooms with room number and room type, so that availability is calculated from the rooms the hotel really has.  
**Trigger:** Manager wants to add a room or change an existing one  
**Status:** Approved  

**Requirements:** [FR-019, NFR-004, NFR-009, C-006](../requirements.md)

## Preconditions

- The manager is logged in.
- At least one room type exists (UC-016 Maintain Room Types).

## Main Success Scenario

1. Manager opens the rooms.
2. System displays all rooms with room number, room type, cleaning status and whether they are active, ordered by room number.
3. Manager creates a new room or selects an existing one.
4. Manager enters or changes the room number, the room type and whether the room is active.
5. Manager saves the room.
6. System checks the entered values and that every Confirmed and Checked In reservation still has a room for every night (BR-003).
7. System records the room.
8. System displays the updated list of rooms; availability is calculated with the saved room.

## Alternative Flows

### A1: Invalid Values

**Trigger:** The room number is missing or longer than 10 characters, or no room type is selected (step 6)  
**Flow:**

1. System marks the invalid values; the entered values are kept.
2. Use case continues at step 4.

### A2: Room Number Already Used

**Trigger:** Another room already has the entered room number (step 6)  
**Flow:**

1. System informs the manager that the room number is already used.
2. Use case continues at step 4.

### A3: Reservations Would Lose Their Room

**Trigger:** Deactivating the room or changing its room type would leave a room type with more Confirmed and Checked In reservations than available rooms for at least one night (step 6)  
**Flow:**

1. System lists the affected nights and room type and refuses the change (BR-003).
2. Use case continues at step 4.

### A4: Room Is Occupied

**Trigger:** The room is assigned to a Checked In reservation and the manager deactivates it or changes its room type (step 6)  
**Flow:**

1. System informs the manager that an occupied room can be deactivated or moved to another room type only after check-out.
2. Use case continues at step 4.

### A5: Manager Discards the Changes

**Trigger:** Manager leaves the room without saving (step 5)  
**Flow:**

1. System discards the entered values.
2. Use case ends.

## Postconditions

### Success Postconditions

- The room exists with the saved room number, room type and active flag.
- Availability of the affected room types is calculated with the saved room.

### Failure Postconditions

- The room keeps its previous data, or no new room exists.
- No room type is ever reserved for more nights than it has rooms available (no overbooking).

## Business Rules

### BR-001: Unique Room Number

Every room has a room number that no other room has.

### BR-002: New Room

A new room starts with cleaning status Clean.

### BR-003: Reservations Keep Their Room

A room can be deactivated or moved to another room type only when, for every future night, every affected room type still has at least as many active, unblocked rooms as Confirmed and Checked In reservations.

### BR-004: No Deletion

Rooms are never deleted; a room that is no longer used is deactivated. An inactive room does not count for availability and cannot be assigned.
