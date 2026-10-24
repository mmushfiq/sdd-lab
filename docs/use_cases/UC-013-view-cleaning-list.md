# Use Case: View Cleaning List

## Overview

**Use Case ID:** UC-013  
**Use Case Name:** View Cleaning List  
**Primary Actor:** Housekeeper  
**Goal:** The housekeeper sees all dirty rooms with departures first, so that they know which rooms to clean and in which order.  
**Trigger:** Housekeeper asks for the cleaning list  
**Status:** Implemented  

**Requirements:** [FR-015, NFR-002, NFR-007, NFR-009](../requirements.md)

## Preconditions

- The housekeeper is logged in.

## Main Success Scenario

1. Housekeeper opens the cleaning list.
2. System displays every active, dirty room that is not blocked today, with room number, room type, occupancy (Free or Occupied) and whether its guest checked out today or departs today, in the order of the cleaning list.
3. Housekeeper selects a room from the list.
4. System displays the room with the option to mark it clean (UC-014 Mark Room Clean); the housekeeper knows which room to clean next.

## Alternative Flows

### A1: No Dirty Rooms

**Trigger:** No active room that is not blocked today is dirty (step 2)  
**Flow:**

1. System informs the housekeeper that all rooms are clean.
2. Use case ends.

### A2: Housekeeper Only Reviews the List

**Trigger:** Housekeeper does not select a room (step 3)  
**Flow:**

1. Housekeeper leaves the cleaning list.
2. Use case ends.

## Postconditions

### Success Postconditions

- The housekeeper has seen all dirty rooms in cleaning order (BR-001).
- No room data is changed.

### Failure Postconditions

- No room data is changed.

## Business Rules

### BR-001: Cleaning Order

The cleaning list shows first the dirty rooms of today's departures: free rooms whose guest checked out today, then occupied rooms whose guest departs today. Then the other free dirty rooms, then the other occupied dirty rooms. Within each group, rooms are ordered by room number.

### BR-002: Blocked Rooms

Rooms blocked today are not on the cleaning list.

### BR-003: Tablet Use

The cleaning list is fully usable without horizontal scrolling at a screen width of 768 px.
