# Use Case: Cancel Reservation

## Overview

**Use Case ID:** UC-006  
**Use Case Name:** Cancel Reservation  
**Primary Actor:** Receptionist  
**Secondary Actors:** Email Service  
**Goal:** The receptionist cancels a reservation before check-in, so that the room type becomes available again for other guests.  
**Trigger:** A guest asks the receptionist to cancel their reservation  
**Status:** Approved  

**Requirements:** [FR-008, NFR-003, NFR-009](../requirements.md)

## Preconditions

- The receptionist is logged in.

## Main Success Scenario

1. Receptionist opens a reservation found with UC-007 Search Reservations.
2. System displays the reservation with its status, guest, dates, room type and total price.
3. Receptionist requests the cancellation of the reservation.
4. System checks that the reservation has status Confirmed.
5. System asks the receptionist to confirm the cancellation and states that it cannot be undone.
6. Receptionist confirms the cancellation.
7. System records the reservation with status Cancelled and the time of cancellation.
8. System sends a cancellation email with the reservation code, dates and room type to the guest through the Email Service.
9. System displays that the reservation is cancelled; the room type is available again for its nights.

## Alternative Flows

### A1: Reservation Not Confirmed

**Trigger:** The reservation has status Checked In, Checked Out, Cancelled or No-Show (step 4)  
**Flow:**

1. System informs the receptionist that a reservation with this status cannot be cancelled.
2. Use case ends.

### A2: Receptionist Keeps the Reservation

**Trigger:** Receptionist does not confirm the cancellation (step 6)  
**Flow:**

1. System keeps the reservation unchanged and displays it again.
2. Use case ends.

### A3: Status Changed Before Confirmation

**Trigger:** The reservation was checked in, cancelled or recorded as no-show by someone else since step 4 (step 6)  
**Flow:**

1. System refuses the cancellation and displays the current status.
2. Use case ends.

### A4: Email Service Does Not Accept the Cancellation Email

**Trigger:** The Email Service refuses the cancellation email or does not answer (step 8)  
**Flow:**

1. System keeps the reservation cancelled and retries sending the email for at most 24 hours (BR-003).
2. Use case continues at step 9.

## Postconditions

### Success Postconditions

- The reservation has status Cancelled and records the time of cancellation.
- The availability of the reserved room type is increased by one for every night of the reservation.
- The cancellation email is sent to the guest; when the Email Service did not accept it (A4), it is retried for at most 24 hours.

### Failure Postconditions

- The reservation keeps its status and data.
- The availability of every room type remains unchanged.

## Business Rules

### BR-001: Cancellation Before Check-in

A receptionist can cancel any reservation with status Confirmed at any time before check-in, also after the online cancellation deadline (UC-003 BR-001) and on the arrival date.

### BR-002: No Cancellation Fee

The system records no cancellation fee and processes no payment.

### BR-003: Cancellation Email

While the Email Service accepts emails, the cancellation email is sent within 1 minute after the cancellation is recorded. When it does not accept the email, the system retries for at most 24 hours. An email that could not be sent never undoes the cancellation.
