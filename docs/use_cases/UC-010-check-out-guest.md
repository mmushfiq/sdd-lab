# Use Case: Check Out Guest

## Overview

**Use Case ID:** UC-010  
**Use Case Name:** Check Out Guest  
**Primary Actor:** Receptionist  
**Goal:** The receptionist checks out a guest and sees the total price of the stay, so that the room is released and marked dirty for housekeeping.  
**Trigger:** A checked-in guest asks to check out at the front desk  
**Status:** Implemented  

**Requirements:** [FR-012, NFR-006, NFR-009, C-007](../requirements.md)

## Preconditions

- The receptionist is logged in.

## Main Success Scenario

1. Receptionist opens the guest's reservation from today's departures (UC-008) or the reservation search (UC-007).
2. Receptionist requests the check-out.
3. System checks that the reservation has status Checked In and that its departure date is today.
4. System displays the assigned room, the arrival date, the departure date, the number of nights and the total price to be paid.
5. Receptionist confirms the check-out.
6. System records the reservation with status Checked Out and the time of check-out, releases the assigned room and sets its cleaning status to Dirty.
7. System displays the total price of the stay; the guest is checked out and the room is ready for housekeeping.

## Alternative Flows

### A1: Reservation Not Checked In

**Trigger:** The reservation has status Confirmed, Checked Out, Cancelled or No-Show (step 3)  
**Flow:**

1. System informs the receptionist that only a checked-in guest can be checked out.
2. Use case ends.

### A2: Early Check-out

**Trigger:** The departure date is after today (step 3)  
**Flow:**

1. System informs the receptionist that the guest leaves early and calculates the new departure date and the total price for the nights stayed (BR-002).
2. System displays the reserved and the new departure date and total price.
3. Use case continues at step 5.

### A3: Overdue Check-out

**Trigger:** The departure date is before today (step 3)  
**Flow:**

1. System informs the receptionist that the guest stayed beyond the departure date and calculates the new departure date (today) and the total price including the additional nights (BR-003).
2. System displays the reserved and the new departure date and total price.
3. Use case continues at step 5.

### A4: Receptionist Stops the Check-out

**Trigger:** Receptionist does not confirm the check-out (step 5)  
**Flow:**

1. System keeps the reservation unchanged.
2. Use case ends.

### A5: Status Changed Before Confirmation

**Trigger:** The reservation was checked out by someone else since step 3 (step 5)  
**Flow:**

1. System refuses the check-out and displays the current status.
2. Use case ends.

## Postconditions

### Success Postconditions

- The reservation has status Checked Out, the time of check-out and its final total price.
- The assigned room stays recorded on the reservation but is free for the next check-in, and its cleaning status is Dirty.
- After an early or overdue check-out, the departure date and total price reflect the nights stayed; after an early check-out, the released nights are available again.

### Failure Postconditions

- The reservation keeps its status, dates, total price and assigned room.
- The cleaning status of the room is unchanged.

## Business Rules

### BR-001: Check-out Status

Only a reservation with status Checked In can be checked out.

### BR-002: Early Check-out Price

When the guest leaves before the departure date, the departure date becomes today, but at least the day after the arrival date. The total price becomes the new number of nights times the nightly rate on which the reserved total price was based (reserved total price ÷ reserved number of nights), in CHF.

### BR-003: Overdue Check-out

When a guest checks out after the departure date, the departure date becomes today and the total price is calculated for the new number of nights in the same way as in BR-002.

### BR-004: Payment Outside the System

The total price is displayed so that the receptionist can collect it; the system records no payment.

### BR-005: Room Becomes Dirty

At check-out, the cleaning status of the assigned room is set to Dirty, regardless of its previous value.
