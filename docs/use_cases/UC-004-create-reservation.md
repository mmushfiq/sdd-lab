# Use Case: Create Reservation

## Overview

**Use Case ID:** UC-004  
**Use Case Name:** Create Reservation  
**Primary Actor:** Receptionist  
**Secondary Actors:** Email Service  
**Goal:** The receptionist records a reservation for a guest who calls or walks in, so that phone and walk-in guests are in the same system as online reservations.  
**Trigger:** A guest asks the receptionist by phone or at the front desk for a room  
**Status:** Approved  

**Requirements:** [FR-006, FR-001, NFR-001, NFR-003, NFR-004, NFR-009, C-007, C-008](../requirements.md)

## Preconditions

- The receptionist is logged in.
- At least one active room type with active rooms is configured (UC-016 Maintain Room Types, UC-017 Maintain Rooms).

## Main Success Scenario

1. Receptionist opens a new reservation.
2. Receptionist enters the arrival date, the departure date and the number of guests.
3. System displays the room types that are available for every night of the date range and fit the number of guests, each with its nightly rate, total price and number of available rooms.
4. Receptionist selects a room type.
5. Receptionist enters the guest's email address.
6. System displays the stored first name, last name and phone number when a guest with this email address exists.
7. Receptionist enters or corrects the first name, last name and phone number.
8. Receptionist confirms the reservation.
9. System checks the guest details and that the selected room type is still available for every night of the date range.
10. System records the guest and the reservation for the selected room type with status Confirmed, channel Reception and a new reservation code.
11. System sends a confirmation email with the reservation code, dates, room type, number of guests and total price to the guest through the Email Service.
12. System displays the reservation code and the total price; the guest has a reserved room type.

## Alternative Flows

### A1: Invalid Dates or Number of Guests

**Trigger:** The arrival date is in the past or more than 365 days ahead, the departure date is not after the arrival date, or the number of guests is not between 1 and 10 (step 2)  
**Flow:**

1. System marks the invalid input and explains the allowed values.
2. Use case continues at step 2.

### A2: No Room Type Available

**Trigger:** No room type is available for every night of the date range with the requested number of guests (step 3)  
**Flow:**

1. System informs the receptionist that no room type is available for these dates and number of guests.
2. Use case continues at step 2.

### A3: Guest Not Yet Known

**Trigger:** No guest with the entered email address exists (step 6)  
**Flow:**

1. System displays empty fields for first name, last name and phone number.
2. Use case continues at step 7.

### A4: Invalid Guest Details

**Trigger:** A required guest detail is missing or the email address is not a valid email address (step 9)  
**Flow:**

1. System marks the missing or invalid details; the entered values are kept.
2. Use case continues at step 5.

### A5: Room Type No Longer Available

**Trigger:** Another reservation took the last available room of the selected room type for at least one night since step 3 (step 9)  
**Flow:**

1. System informs the receptionist that the room type is no longer available for these dates; the entered guest details are kept.
2. Use case continues at step 3.

### A6: Receptionist Discards the Reservation

**Trigger:** Receptionist leaves the new reservation before confirming it (step 8)  
**Flow:**

1. System discards the entered data.
2. Use case ends.

### A7: Email Service Does Not Accept the Confirmation

**Trigger:** The Email Service refuses the confirmation email or does not answer (step 11)  
**Flow:**

1. System keeps the reservation unchanged and retries sending the confirmation email for at most 24 hours (BR-007).
2. System advises the receptionist to tell the guest the reservation code directly.
3. Use case continues at step 12.

## Postconditions

### Success Postconditions

- A reservation with status Confirmed and channel Reception exists for the selected room type, dates and number of guests, linked to the guest identified by the entered email address.
- The reservation has a unique reservation code and a total price fixed at the nightly rate valid when it was recorded.
- The availability of the room type is reduced by one for every night of the reservation.
- The stored guest has the first name, last name and phone number entered in this reservation.
- The confirmation email is sent to the guest; when the Email Service did not accept it (A7), it is retried for at most 24 hours.

### Failure Postconditions

- No reservation is recorded.
- The availability of every room type remains unchanged.
- Stored data of an existing guest is not changed.
- No room type is ever reserved for more nights than it has rooms available (no overbooking).

## Business Rules

### BR-001: Reservation Window

The arrival date is today or a later date at most 365 days ahead, and the departure date is after the arrival date. A walk-in guest can be reserved for today.

### BR-002: Available Room Type

A room type is offered under the same availability and number-of-guests rules as online (UC-001 BR-002, UC-001 BR-003).

### BR-003: No Overbooking

The availability check in step 9 and the recording in step 10 happen as one indivisible action, so that online and reception reservations never together exceed the available rooms (UC-001 BR-005).

### BR-004: Total Price

The total price is the number of nights times the nightly rate of the room type, in CHF, fixed when the reservation is recorded (UC-001 BR-004).

### BR-005: Guest Identified by Email Address

A guest is identified by their email address, compared case-insensitively. When it already belongs to a guest, the reservation is linked to that guest and the stored first name, last name and phone number are replaced with the entered values.

### BR-006: Guest Details

First name, last name, email address and phone number are required; no other personal data is collected. No payment data is recorded; the guest pays at the hotel.

### BR-007: Confirmation Email

While the Email Service accepts emails, the confirmation email is sent within 1 minute after the reservation is recorded. When it does not accept the email, the system retries for at most 24 hours. A confirmation email that could not be sent never cancels or changes the reservation.
