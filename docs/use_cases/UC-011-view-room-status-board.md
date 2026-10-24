# Use Case: View Room Status Board

## Overview

**Use Case ID:** UC-011  
**Use Case Name:** View Room Status Board  
**Primary Actor:** Receptionist  
**Goal:** The receptionist sees every room with its occupancy and cleaning status on one screen, so that they know at a glance which rooms they can assign.  
**Trigger:** Receptionist asks for the room status board  
**Status:** Reviewed  

**Requirements:** [FR-013, NFR-002, NFR-007, NFR-009](../requirements.md)

## Preconditions

- The receptionist is logged in.

## Main Success Scenario

1. Receptionist opens the room status board.
2. System displays every active room with its room number, room type, occupancy (Free, Occupied or Blocked) and cleaning status (Clean or Dirty), ordered by room number.
3. System displays, for every occupied room, the guest name and the departure date, and for every blocked room, the last blocked date and the reason; the receptionist knows which rooms can be assigned.

## Alternative Flows

### A1: Receptionist Filters the Board

**Trigger:** Receptionist wants to see only some rooms (step 2)  
**Flow:**

1. Receptionist selects a room type, an occupancy or a cleaning status.
2. System displays only the rooms that match the selection.
3. Use case continues at step 3.

### A2: Status Changed While Viewing

**Trigger:** A check-in, check-out, block or cleaning changes a room's status while the board is open (step 3)  
**Flow:**

1. Receptionist refreshes the board.
2. Use case continues at step 2.

## Postconditions

### Success Postconditions

- The receptionist has seen the occupancy and cleaning status of every active room as of the moment the board was displayed.
- No room or reservation data is changed.

### Failure Postconditions

- No room or reservation data is changed.

## Business Rules

### BR-001: Occupancy

A room is Occupied when it is assigned to a reservation with status Checked In. Otherwise it is Blocked when a room block covers today, and Free in all other cases. An occupied room that is also blocked today is shown as Occupied with a marker.

### BR-002: Assignable at a Glance

Rooms that are Free and Clean are highlighted as assignable.

### BR-003: Tablet Use

The board is fully usable without horizontal scrolling at a screen width of 768 px.
