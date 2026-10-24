# Use Case: Mark Room Clean

## Overview

**Use Case ID:** UC-014  
**Use Case Name:** Mark Room Clean  
**Primary Actor:** Housekeeper  
**Goal:** The housekeeper marks a room as clean after cleaning it, so that reception can assign it to the next guest.  
**Trigger:** Housekeeper has finished cleaning a room  
**Status:** Implemented  

**Requirements:** [FR-016, NFR-007, NFR-009](../requirements.md)

## Preconditions

- The housekeeper is logged in.

## Main Success Scenario

1. Housekeeper selects the cleaned room on the cleaning list (UC-013).
2. Housekeeper marks the room clean.
3. System checks that the room is still dirty.
4. System records the cleaning status Clean for the room.
5. System removes the room from the cleaning list; the room is clean and can be assigned at check-in.

## Alternative Flows

### A1: Room Already Clean

**Trigger:** Another housekeeper marked the room clean since it was displayed (step 3)  
**Flow:**

1. System informs the housekeeper that the room is already clean and refreshes the cleaning list.
2. Use case ends.

### A2: Housekeeper Selected the Wrong Room

**Trigger:** Housekeeper notices that the selected room is not the cleaned one before marking it (step 2)  
**Flow:**

1. Housekeeper selects another room on the cleaning list.
2. Use case continues at step 2.

## Postconditions

### Success Postconditions

- The room has cleaning status Clean and is no longer on the cleaning list.
- A free, clean room can be assigned at check-in (UC-009).

### Failure Postconditions

- The cleaning status of the room is unchanged.

## Business Rules

### BR-001: Only Dirty Rooms

Only a room with cleaning status Dirty can be marked clean. Marking it clean changes neither its occupancy nor any reservation.

### BR-002: Occupied Rooms

An occupied room can be marked clean as well; it stays occupied by its guest.
