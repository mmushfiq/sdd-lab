# Use Case: Change Reservation

## Overview

**Use Case ID:** UC-005  
**Use Case Name:** Change Reservation  
**Primary Actor:** Receptionist  
**Secondary Actors:** Email Service  
**Goal:** The receptionist changes the dates, room type or number of guests of a reservation, so that a guest's change request is handled without cancelling and re-creating the reservation.  
**Trigger:** A guest asks the receptionist to change their reservation  
**Status:** Approved  

**Requirements:** [FR-007, NFR-003, NFR-004, NFR-009](../requirements.md)

## Preconditions

- The receptionist is logged in.

## Main Success Scenario

1. Receptionist opens a reservation found with UC-007 Search Reservations.
2. System displays the reservation with its status, dates, room type, number of guests and total price, and the data that can be changed.
3. Receptionist changes the arrival date, the departure date, the room type or the number of guests.
4. System checks the new values and that the room type is available for every night of the new date range, not counting this reservation itself.
5. System displays the old and the new total price.
6. Receptionist confirms the change.
7. System checks the availability again and records the changed reservation with the night prices of the new date range (BR-005), the new total price and, when the arrival date changed, the new online cancellation deadline (BR-007).
8. System sends a change confirmation email with the reservation code and the new dates, room type, number of guests, total price and, for a reservation with status Confirmed, the online cancellation deadline (UC-003 BR-006) to the guest through the Email Service.
9. System displays the changed reservation; the reservation reflects the guest's request.

## Alternative Flows

### A1: Reservation Cannot Be Changed

**Trigger:** The reservation has status Checked Out, Cancelled or No-Show (step 2)  
**Flow:**

1. System displays the reservation read-only and informs the receptionist that a reservation with this status cannot be changed.
2. Use case ends.

### A2: Guest Already Checked In

**Trigger:** The reservation has status Checked In (step 2)  
**Flow:**

1. System offers only the departure date for change and displays the assigned room (BR-003).
2. Receptionist changes the departure date.
3. System checks that the new departure date is after today and, for every added night, that the room type is available and the assigned room is not blocked.
4. Use case continues at step 5.

### A3: Invalid New Values

**Trigger:** The new dates violate the reservation window, the number of guests is not between 1 and 10 or exceeds the maximum number of guests of the room type, or the selected new room type is inactive (step 4)  
**Flow:**

1. System marks the invalid values and explains the allowed values.
2. Use case continues at step 3.

### A4: Not Available for the New Values

**Trigger:** The room type is not available for at least one night of the new date range, or the assigned room of a checked-in guest is blocked on an added night (step 4)  
**Flow:**

1. System informs the receptionist which nights are not available; the reservation stays unchanged.
2. Use case continues at step 3.

### A5: Nothing Changed

**Trigger:** The receptionist confirms without having changed any value (step 6)  
**Flow:**

1. System keeps the reservation unchanged and sends no email.
2. Use case ends.

### A6: Receptionist Discards the Change

**Trigger:** Receptionist leaves the reservation without confirming the change (step 6)  
**Flow:**

1. System discards the entered values; the reservation stays unchanged.
2. Use case ends.

### A7: No Longer Available at Confirmation

**Trigger:** Another reservation took the last available room for at least one night of the new date range since step 4 (step 7)  
**Flow:**

1. System informs the receptionist that the room type is no longer available for these nights; the reservation stays unchanged.
2. Use case continues at step 3.

### A8: Email Service Does Not Accept the Change Confirmation

**Trigger:** The Email Service refuses the change confirmation email or does not answer (step 8)  
**Flow:**

1. System keeps the changed reservation and retries sending the email for at most 24 hours (BR-006).
2. Use case continues at step 9.

## Postconditions

### Success Postconditions

- The reservation has the new dates, room type and number of guests, the same reservation code, guest and channel, and the new total price.
- The availability reflects the changed reservation: nights no longer reserved are available again, and added nights are reserved.
- A checked-in guest keeps the assigned room.
- A Confirmed reservation whose arrival date changed has an online cancellation deadline 48 hours before the check-in time on the new arrival date (BR-007).
- The change confirmation email is sent to the guest; when the Email Service did not accept it (A8), it is retried for at most 24 hours.

### Failure Postconditions

- The reservation keeps all its previous data.
- The availability of every room type remains unchanged.
- No room type is ever reserved for more nights than it has rooms available (no overbooking).

## Business Rules

### BR-001: Changeable Status

A reservation with status Confirmed can change its arrival date, departure date, room type and number of guests. A reservation with status Checked In can change only its departure date. Reservations with any other status cannot be changed.

### BR-002: Reservation Window

For a Confirmed reservation, the new arrival date is today or later and at most 365 days ahead, and the departure date is after the arrival date (UC-001 BR-001).

### BR-003: Change During a Stay

For a Checked In reservation, the new departure date is after today. A longer stay requires that the room type is available and the assigned room is not blocked for every added night; the guest keeps the assigned room. Leaving today is a check-out (UC-010).

### BR-004: Availability Without the Reservation Itself

The availability for the new values is calculated as if the reservation being changed did not exist, so that a reservation can keep the nights it already holds. A reservation can be moved only to an active room type; a reservation of an inactive room type can keep it and change its dates or number of guests (UC-016 BR-004). The check and the recording happen as one indivisible action (UC-001 BR-005).

### BR-005: New Total Price

After a change, every night of the new date range has a night price. A night that was already part of the reservation keeps its night price as long as the room type stays the same. Every added night, and every night after a change of the room type, gets the nightly rate valid for that night at the time of the change (UC-001 BR-004). Nights no longer part of the reservation are removed. The total price is the sum of the night prices, in CHF. For a Checked In reservation, only the departure date changes, so the nights already reserved keep their price and only added nights get the current nightly rate.

### BR-006: Change Confirmation Email

While the Email Service accepts emails, the change confirmation email is sent within 1 minute after the change is recorded. When it does not accept the email, the system retries for at most 24 hours. An email that could not be sent never undoes the change.

### BR-007: Online Cancellation Deadline After a Change

When the arrival date of a Confirmed reservation is changed, its online cancellation deadline is set again to 48 hours before the check-in time (14:00) on the new arrival date (UC-003 BR-001), also when the reservation had a deadline of 24 hours. A change of only the departure date, the room type or the number of guests keeps the deadline.
