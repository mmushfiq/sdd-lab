# Use Case: Check In Guest

## Overview

**Use Case ID:** UC-009  
**Use Case Name:** Check In Guest  
**Primary Actor:** Receptionist  
**Goal:** The receptionist checks in an arriving guest by assigning a clean, free room of the reserved room type, so that the guest gets a room that is ready for them.  
**Trigger:** A guest with a reservation arrives at the front desk  
**Status:** Implemented  

**Requirements:** [FR-011, NFR-006, NFR-009](../requirements.md)

## Preconditions

- The receptionist is logged in.

## Main Success Scenario

1. Receptionist opens the guest's reservation from today's arrivals (UC-008) or the reservation search (UC-007).
2. Receptionist requests the check-in.
3. System checks that the reservation has status Confirmed and that its arrival date is today.
4. System displays the rooms of the reserved room type that can be assigned, with the first of them by room number proposed.
5. Receptionist confirms the proposed room or selects another listed room.
6. System checks that the room can still be assigned.
7. System records the reservation with status Checked In, the assigned room and the time of check-in.
8. System displays the room number; the guest is checked in and has a room.

## Alternative Flows

### A1: Reservation Not Confirmed

**Trigger:** The reservation has status Checked In, Checked Out, Cancelled or No-Show (step 3)  
**Flow:**

1. System informs the receptionist that a reservation with this status cannot be checked in.
2. Use case ends.

### A2: Not the Arrival Date

**Trigger:** The arrival date of the reservation is not today (step 3)  
**Flow:**

1. System informs the receptionist that check-in is only possible on the arrival date and that the dates can be changed with UC-005 Change Reservation.
2. Use case ends.

### A3: No Room Ready

**Trigger:** No room of the reserved room type can be assigned (step 4)  
**Flow:**

1. System informs the receptionist that no clean room of the room type is free and lists the free rooms of the room type that are still dirty.
2. Use case ends.

### A4: Room No Longer Assignable

**Trigger:** The selected room was assigned to another guest, set to dirty or blocked since step 4 (step 6)  
**Flow:**

1. System informs the receptionist that the room can no longer be assigned.
2. Use case continues at step 4.

### A5: Receptionist Stops the Check-in

**Trigger:** Receptionist leaves the check-in without confirming a room (step 5)  
**Flow:**

1. System keeps the reservation unchanged.
2. Use case ends.

## Postconditions

### Success Postconditions

- The reservation has status Checked In, an assigned room of the reserved room type and the time of check-in.
- The room is occupied by this reservation and is no longer offered to other check-ins.

### Failure Postconditions

- The reservation keeps its status and has no assigned room.
- No room is assigned to more than one checked-in reservation at a time.

## Business Rules

### BR-001: Check-in Date

A reservation can be checked in only on its arrival date, in the hotel's local time. Check-in before the check-in time (14:00) is allowed when a room can be assigned.

### BR-002: Assignable Room

A room can be assigned when it is active, belongs to the reserved room type, has cleaning status Clean, is not assigned to another Checked In reservation, and is not blocked on any night of the reservation.

### BR-003: One Guest per Room

The check in step 6 and the recording in step 7 happen as one indivisible action, so that two receptionists never assign the same room at the same time.

### BR-004: Room Stays With the Reservation

The assigned room stays with the reservation until check-out; the cleaning status of the room is not changed by the check-in.
