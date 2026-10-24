# Use Case: Block Room

## Overview

**Use Case ID:** UC-018  
**Use Case Name:** Block Room  
**Primary Actor:** Manager  
**Goal:** The manager blocks a room for a date range, so that a room under repair is neither reserved nor assigned.  
**Trigger:** Manager learns that a room will be out of service  
**Status:** Reviewed  

**Requirements:** [FR-020, NFR-004, NFR-009](../requirements.md)

## Preconditions

- The manager is logged in.
- At least one active room exists (UC-017 Maintain Rooms).

## Main Success Scenario

1. Manager opens the room blocks.
2. System displays the current and future room blocks with room number, first blocked date, last blocked date and reason.
3. Manager selects a room and enters the first blocked date, the last blocked date and a reason.
4. Manager saves the room block.
5. System checks the dates, that the block does not overlap another block of the room, that the room is not occupied during the block, and that every Confirmed and Checked In reservation of the room type still has a room for every blocked night.
6. System records the room block.
7. System displays the updated list of room blocks; the room is neither reserved nor assigned for the blocked dates.

## Alternative Flows

### A1: Invalid Dates

**Trigger:** The first blocked date is in the past, the last blocked date is before the first blocked date, or the reason is longer than 200 characters (step 5)  
**Flow:**

1. System marks the invalid values; the entered values are kept.
2. Use case continues at step 3.

### A2: Overlapping Block

**Trigger:** The date range overlaps another block of the same room (step 5)  
**Flow:**

1. System displays the overlapping block and refuses the new one.
2. Use case continues at step 3.

### A3: Room Occupied During the Block

**Trigger:** The room is assigned to a Checked In reservation whose stay includes a blocked night (step 5)  
**Flow:**

1. System displays the reservation and refuses the block (BR-002).
2. Use case continues at step 3.

### A4: Reservations Would Lose Their Room

**Trigger:** Blocking the room would leave its room type with more Confirmed and Checked In reservations than available rooms for at least one night (step 5)  
**Flow:**

1. System lists the affected nights and refuses the block (BR-003).
2. Use case continues at step 3.

### A5: Manager Discards the Block

**Trigger:** Manager leaves without saving the room block (step 4)  
**Flow:**

1. System discards the entered values.
2. Use case ends.

## Postconditions

### Success Postconditions

- A room block exists for the room from the first to the last blocked date with the given reason.
- The availability of the room type is reduced by one for every blocked night.
- The room is not offered at check-in while blocked.

### Failure Postconditions

- No room block is recorded and existing room blocks are unchanged.
- The availability of every room type remains unchanged.
- No room type is ever reserved for more nights than it has rooms available (no overbooking).

## Business Rules

### BR-001: Block Dates

The first blocked date is today or later, and the last blocked date is on or after the first blocked date. Both dates are blocked. Blocks of the same room do not overlap.

### BR-002: No Block of an Occupied Room

A room cannot be blocked for a night on which it is assigned to a Checked In reservation.

### BR-003: Reservations Keep Their Room

A room can be blocked only when, for every blocked night, its room type still has at least as many active, unblocked rooms as Confirmed and Checked In reservations. The check and the recording happen as one indivisible action.
