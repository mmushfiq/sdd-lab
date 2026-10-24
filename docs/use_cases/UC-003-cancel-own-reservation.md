# Use Case: Cancel Own Reservation

## Overview

**Use Case ID:** UC-003  
**Use Case Name:** Cancel Own Reservation  
**Primary Actor:** Guest  
**Secondary Actors:** Email Service  
**Goal:** The guest cancels their reservation online before the cancellation deadline, so that they do not have to call the hotel when their plans change.  
**Trigger:** A guest asks to cancel their reservation on the hotel's website  
**Status:** Reviewed  

**Requirements:** [FR-005, NFR-002, NFR-003, NFR-008, NFR-011, NFR-012, C-007](../requirements.md)

## Preconditions

- The guest has received a reservation code for a reservation made online (UC-001) or at reception (UC-004).

## Main Success Scenario

1. Guest opens the reservation lookup.
2. Guest enters the reservation code and the email address.
3. System checks that a reservation with this reservation code exists and belongs to a guest with this email address.
4. System displays the reservation with its status, dates, room type, number of guests and total price.
5. Guest requests the cancellation of the reservation.
6. System checks that the reservation has status Confirmed and that the online cancellation deadline has not passed.
7. System asks the guest to confirm the cancellation and states that it cannot be undone.
8. Guest confirms the cancellation.
9. System records the reservation with status Cancelled and the time of cancellation.
10. System sends a cancellation email with the reservation code, dates and room type to the guest through the Email Service.
11. System displays that the reservation is cancelled; the guest's reservation is cancelled.

## Alternative Flows

### A1: Lookup Input Missing

**Trigger:** The reservation code or the email address is empty (step 2)  
**Flow:**

1. System marks the missing input.
2. Use case continues at step 2.

### A2: Reservation Not Found

**Trigger:** No reservation with this reservation code belongs to a guest with this email address (step 3)  
**Flow:**

1. System counts a failed lookup attempt for the guest's network address and informs the guest that no reservation matches, without telling which input is wrong (UC-002 BR-002, UC-002 BR-003).
2. Use case continues at step 2.

### A3: Lookup Locked

**Trigger:** Five failed lookup attempts came from the guest's network address within the last 15 minutes (step 3)  
**Flow:**

1. System refuses the lookup and informs the guest that the lookup is locked for 15 minutes (UC-002 BR-003).
2. Use case ends.

### A4: Reservation Not Confirmed

**Trigger:** The reservation has status Checked In, Checked Out, Cancelled or No-Show (step 6)  
**Flow:**

1. System informs the guest that a reservation with this status cannot be cancelled.
2. Use case ends.

### A5: Cancellation Deadline Passed

**Trigger:** Less than 24 hours remain until the check-in time on the arrival date (step 6)  
**Flow:**

1. System informs the guest that the reservation can no longer be cancelled online and that the hotel's reception can still cancel it (BR-001).
2. Use case ends.

### A6: Guest Keeps the Reservation

**Trigger:** Guest does not confirm the cancellation (step 8)  
**Flow:**

1. System keeps the reservation unchanged and displays it again.
2. Use case ends.

### A7: Deadline Passes Before Confirmation

**Trigger:** The cancellation deadline passed or the status changed between step 6 and the confirmation (step 8)  
**Flow:**

1. System checks the status and the deadline again when the guest confirms and refuses the cancellation.
2. Use case continues at step 6.

### A8: Email Service Does Not Accept the Cancellation Email

**Trigger:** The Email Service refuses the cancellation email or does not answer (step 10)  
**Flow:**

1. System keeps the reservation cancelled and retries sending the cancellation email for at most 24 hours (BR-004).
2. Use case continues at step 11.

## Postconditions

### Success Postconditions

- The reservation has status Cancelled and records the time of cancellation.
- The availability of the reserved room type is increased by one for every night of the reservation.
- The cancellation email is sent to the guest; when the Email Service did not accept it (A8), it is retried for at most 24 hours.

### Failure Postconditions

- The reservation keeps its status and data.
- The availability of every room type remains unchanged.
- No reservation of another guest is shown or changed.

## Business Rules

### BR-001: Online Cancellation Deadline

A guest can cancel online until 24 hours before the check-in time (14:00) on the arrival date, in the hotel's local time. Example: for an arrival on 10 May, the last online cancellation is on 9 May at 13:59. Later cancellations are only possible at reception (UC-006).

### BR-002: Cancellable Status

Only a reservation with status Confirmed can be cancelled.

### BR-003: No Cancellation Fee

Cancelling within the deadline is free of charge. The system does not process payments.

### BR-004: Cancellation Email

While the Email Service accepts emails, the cancellation email is sent within 1 minute after the cancellation is recorded. When the Email Service does not accept it, the system retries for at most 24 hours. A cancellation email that could not be sent never undoes the cancellation.

### BR-005: Lookup Rules

The reservation is found and the lookup is locked under the same rules as in UC-002 (UC-002 BR-001 to UC-002 BR-004).
