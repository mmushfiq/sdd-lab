# Use Case: View Own Reservation

## Overview

**Use Case ID:** UC-002  
**Use Case Name:** View Own Reservation  
**Primary Actor:** Guest  
**Goal:** The guest sees the current details of their reservation, so that they can check their dates, room type and total price without calling the hotel.  
**Trigger:** A guest asks to view their reservation on the hotel's website  
**Status:** Approved  

**Requirements:** [FR-004, NFR-002, NFR-008, NFR-011, NFR-012](../requirements.md)

## Preconditions

- The guest has received a reservation code for a reservation made online (UC-001) or at reception (UC-004).

## Main Success Scenario

1. Guest opens the reservation lookup.
2. Guest enters the reservation code and the email address.
3. System checks that a reservation with this reservation code exists and belongs to a guest with this email address.
4. System displays the reservation with its reservation code, status, arrival date, departure date, number of nights, room type, number of guests and total price, and for a reservation with status Confirmed its online cancellation deadline (UC-003 BR-006); the guest sees their reservation.

## Alternative Flows

### A1: Lookup Input Missing

**Trigger:** The reservation code or the email address is empty (step 2)  
**Flow:**

1. System marks the missing input.
2. Use case continues at step 2.

### A2: Reservation Not Found

**Trigger:** No reservation with this reservation code belongs to a guest with this email address (step 3)  
**Flow:**

1. System counts a failed lookup attempt for the guest's network address (BR-003).
2. System informs the guest that no reservation matches the reservation code and email address, without telling which of the two is wrong (BR-002).
3. Use case continues at step 2.

### A3: Lookup Locked

**Trigger:** Five failed lookup attempts came from the guest's network address within the last 15 minutes (step 3)  
**Flow:**

1. System refuses the lookup without checking the reservation code and informs the guest that the lookup is locked for 15 minutes (BR-003).
2. Use case ends.

### A4: Reservation Can Be Cancelled Online

**Trigger:** The reservation has status Confirmed and the online cancellation deadline has not passed (step 4)  
**Flow:**

1. System additionally offers to cancel the reservation; the guest can continue with UC-003 Cancel Own Reservation.
2. Use case ends.

## Postconditions

### Success Postconditions

- The guest has seen the current data of the reservation that matches the reservation code and email address.
- No reservation or guest data is changed.

### Failure Postconditions

- No reservation or guest data is shown.
- No data is changed except the count of failed lookup attempts.
- The response does not reveal whether a reservation code or an email address exists.

## Business Rules

### BR-001: Lookup Identification

A reservation is found only when the reservation code matches exactly and the email address matches the guest's email address, compared case-insensitively. Reservations of every status are shown, including Cancelled, Checked Out and No-Show.

### BR-002: No Disclosure on Failure

A failed lookup gives the same answer whether the reservation code, the email address or both are wrong.

### BR-003: Lookup Lock

After 5 failed lookup attempts from one network address, every reservation lookup (UC-002 and UC-003) from that address is refused for 15 minutes. A successful lookup does not reset the lock.

### BR-004: Erased Guest Data

A reservation whose guest data was erased on the guest's request (UC-024 Erase Guest Data) can no longer be found with the reservation code and email address.
