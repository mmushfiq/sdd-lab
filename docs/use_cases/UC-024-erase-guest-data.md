# Use Case: Erase Guest Data

## Overview

**Use Case ID:** UC-024  
**Use Case Name:** Erase Guest Data  
**Primary Actor:** Manager  
**Goal:** The manager erases a guest's personal data on the guest's request, so that the hotel meets the right to erasure within 30 days.  
**Trigger:** A guest asks the hotel to erase their personal data  
**Status:** Reviewed  

**Requirements:** [FR-026, C-008, C-010, NFR-009](../requirements.md)

## Preconditions

- The manager is logged in.

## Main Success Scenario

1. Manager opens the guest erasure.
2. Manager enters the guest's email address.
3. System displays the guest with first name, last name, email address, phone number and all their reservations with reservation code, dates and status.
4. Manager requests the erasure.
5. System checks that the guest has no reservation with status Confirmed or Checked In.
6. System asks the manager to confirm and states that the erasure cannot be undone.
7. Manager confirms the erasure.
8. System replaces the guest's first name, last name, email address and phone number with values that do not identify the guest, and stops any pending email to the guest.
9. System displays that the guest data is erased; the guest can no longer be identified in the system.

## Alternative Flows

### A1: Guest Not Found

**Trigger:** No guest has the entered email address (step 3)  
**Flow:**

1. System informs the manager that no guest with this email address exists.
2. Use case continues at step 2.

### A2: Open Reservation

**Trigger:** The guest has a reservation with status Confirmed or Checked In (step 5)  
**Flow:**

1. System lists the open reservations and informs the manager that they must be cancelled (UC-006) or checked out (UC-010) before the erasure.
2. Use case ends.

### A3: Manager Does Not Confirm

**Trigger:** Manager does not confirm the erasure, for example because a legal retention duty applies (step 7)  
**Flow:**

1. System keeps the guest data unchanged.
2. Use case ends.

## Postconditions

### Success Postconditions

- The guest's first name, last name, email address and phone number no longer identify the guest.
- The guest's reservations still exist with their dates, room type, total price and status, so reports stay correct.
- The guest's reservations can no longer be found with the reservation code and the former email address (UC-002 BR-004).
- No email is sent to the guest any more.

### Failure Postconditions

- The guest data and all reservations are unchanged.

## Business Rules

### BR-001: What Is Erased

Erasure replaces exactly the guest's first name, last name, email address and phone number. The replacement email address is unique, has the format of an email address and cannot receive email, so that the rules of the stored guest data still hold. Reservations are kept without personal data, so that occupancy and revenue reports do not change.

### BR-002: No Open Reservation

A guest with a reservation of status Confirmed or Checked In cannot be erased.

### BR-003: Retention Duty

Whether a legal retention duty prevents the erasure is decided by the manager outside the system; the manager then does not confirm the erasure.

### BR-004: New Reservation After Erasure

When a former guest reserves again after the erasure, they are recorded as a new guest.

### BR-005: Deadline

The erasure is done within 30 days of the guest's request. The system does not track the request date; the hotel's own procedure ensures the deadline.
