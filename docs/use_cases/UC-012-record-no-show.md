# Use Case: Record No-Show

## Overview

**Use Case ID:** UC-012  
**Use Case Name:** Record No-Show  
**Primary Actor:** Receptionist  
**Goal:** The receptionist marks a reservation whose guest did not check in by the end of the arrival date as a no-show, so that the room type becomes available again.  
**Trigger:** Receptionist reviews the reservations of guests who did not arrive  
**Status:** Implemented  

**Requirements:** [FR-014, NFR-009, C-007](../requirements.md)

## Preconditions

- The receptionist is logged in.

## Main Success Scenario

1. Receptionist opens the list of missed arrivals.
2. System displays every reservation with status Confirmed whose arrival date is before today, with reservation code, guest name, phone number, arrival date, departure date and room type.
3. Receptionist selects a reservation.
4. Receptionist requests to record the no-show.
5. System asks the receptionist to confirm and states that the reservation cannot be checked in afterwards.
6. Receptionist confirms.
7. System checks that the reservation still has status Confirmed and an arrival date before today.
8. System records the reservation with status No-Show.
9. System displays that the reservation is recorded as a no-show; the room type is available again for its remaining nights.

## Alternative Flows

### A1: No Missed Arrivals

**Trigger:** No reservation with status Confirmed has an arrival date before today (step 2)  
**Flow:**

1. System informs the receptionist that there are no missed arrivals.
2. Use case ends.

### A2: Guest Still Expected

**Trigger:** The guest contacted the hotel and is still coming (step 4)  
**Flow:**

1. Receptionist changes the arrival date instead with UC-005 Change Reservation.
2. Use case ends.

### A3: Receptionist Does Not Confirm

**Trigger:** Receptionist does not confirm the no-show (step 6)  
**Flow:**

1. System keeps the reservation unchanged.
2. Use case continues at step 2.

### A4: Status Changed Before Confirmation

**Trigger:** The reservation was changed, cancelled or recorded as no-show by someone else since step 2 (step 7)  
**Flow:**

1. System refuses to record the no-show and displays the current status.
2. Use case continues at step 2.

## Postconditions

### Success Postconditions

- The reservation has status No-Show and keeps its dates, room type and total price.
- The availability of the reserved room type is increased by one for every night of the reservation from today on.

### Failure Postconditions

- The reservation keeps its status and data.
- The availability of every room type remains unchanged.

## Business Rules

### BR-001: When a No-Show Can Be Recorded

A no-show can be recorded only for a reservation with status Confirmed whose arrival date is before today in the hotel's local time, so the end of the arrival date has passed.

### BR-002: No Automatic No-Show

The system never records a no-show on its own; a receptionist decides.

### BR-003: No No-Show Fee

The system records no fee for a no-show and processes no payment. A no-show sends no email to the guest.

### BR-004: No-Show Is Final

A reservation with status No-Show cannot be checked in, changed or cancelled.
