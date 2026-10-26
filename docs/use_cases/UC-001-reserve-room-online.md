# Use Case: Reserve Room Online

## Overview

**Use Case ID:** UC-001  
**Use Case Name:** Reserve Room Online  
**Primary Actor:** Guest  
**Secondary Actors:** Email Service  
**Goal:** The guest reserves an available room type for their dates and receives a reservation code, so that they have a guaranteed room without calling the hotel.  
**Trigger:** A guest asks to reserve a room on the hotel's website  
**Status:** Approved  

**Requirements:** [FR-001, FR-002, FR-003, NFR-001, NFR-003, NFR-004, NFR-005, NFR-008, NFR-011, C-007, C-008, C-009](../requirements.md)

## Preconditions

- At least one active room type with active rooms is configured (UC-016 Maintain Room Types, UC-017 Maintain Rooms).

## Main Success Scenario

1. Guest opens the online reservation.
2. Guest enters the arrival date, the departure date and the number of guests.
3. System displays the room types that are available for every night of the date range and fit the number of guests, each with its description, nightly rate and total price.
4. Guest selects a room type.
5. System displays a summary of the dates, room type, number of nights and total price, the form for the guest details, and the privacy notice.
6. Guest enters first name, last name, email address and phone number.
7. Guest confirms the reservation.
8. System checks the guest details and that the selected room type is still available for every night of the date range.
9. System records the guest with the entered details and records the reservation for the selected room type with status Confirmed, channel Online, a new reservation code and its online cancellation deadline (UC-003 BR-001).
10. System sends a confirmation email with the reservation code, dates, room type, total price and online cancellation deadline to the guest through the Email Service.
11. System displays the confirmation with the reservation code and the total price; the guest has a reserved room type.

## Alternative Flows

### A1: Invalid Search Input

**Trigger:** The arrival date is in the past or more than 365 days ahead, the departure date is not after the arrival date, or the number of guests is not between 1 and 10 (step 2)  
**Flow:**

1. System marks the invalid input and explains the allowed values.
2. Use case continues at step 2.

### A2: No Room Type Available

**Trigger:** No room type is available for every night of the date range with the requested number of guests (step 3)  
**Flow:**

1. System informs the guest that no room is available for these dates and number of guests.
2. Use case continues at step 2.

### A3: Guest Changes the Search

**Trigger:** Guest wants other dates or another number of guests instead of selecting a room type (step 4)  
**Flow:**

1. Guest changes the arrival date, the departure date or the number of guests.
2. Use case continues at step 3.

### A4: Invalid Guest Details

**Trigger:** A required guest detail is missing or the email address is not a valid email address (step 8)  
**Flow:**

1. System marks the missing or invalid details; the entered values are kept.
2. Use case continues at step 6.

### A5: Room Type No Longer Available

**Trigger:** Another reservation took the last available room of the selected room type for at least one night since step 3 (step 8)  
**Flow:**

1. System informs the guest that the selected room type is no longer available for these dates; the entered guest details are kept.
2. Use case continues at step 3.

### A6: Guest Abandons the Reservation

**Trigger:** Guest leaves the online reservation at any point before confirming it (step 7)  
**Flow:**

1. System discards the entered dates and guest details.
2. Use case ends.

### A7: Guest Already Known

**Trigger:** A guest with the entered email address already exists (step 9)  
**Flow:**

1. System links the reservation to the existing guest and replaces the stored first name, last name and phone number with the entered values.
2. System records the reservation for the selected room type with status Confirmed, channel Online, a new reservation code and its online cancellation deadline (UC-003 BR-001).
3. Use case continues at step 10.

### A8: Email Service Does Not Accept the Confirmation

**Trigger:** The Email Service refuses the confirmation email or does not answer (step 10)  
**Flow:**

1. System keeps the reservation unchanged and retries sending the confirmation email until the Email Service accepts it or 24 hours have passed since the reservation was recorded (BR-012); after 24 hours it stops retrying, and the reservation stays Confirmed.
2. System advises the guest to note the reservation code because the confirmation email may be delayed.
3. Use case continues at step 11.

## Postconditions

### Success Postconditions

- A reservation with status Confirmed and channel Online exists for the selected room type, dates and number of guests, linked to the guest identified by the entered email address.
- The reservation has a unique reservation code, a total price fixed at the nightly rate valid when it was recorded, and an online cancellation deadline 48 hours before the check-in time on the arrival date (UC-003 BR-001).
- The availability of the room type is reduced by one for every night of the reservation.
- The stored guest has the first name, last name and phone number entered in this reservation.
- The confirmation email with the reservation code is sent to the guest; when the Email Service did not accept it (A8), it is retried for at most 24 hours (BR-012).

### Failure Postconditions

- No reservation is recorded.
- The availability of every room type remains unchanged.
- Stored data of an existing guest is not changed.
- No room type is ever reserved for more nights than it has rooms available (no overbooking).

## Business Rules

### BR-001: Reservation Window

The arrival date is today or a later date at most 365 days ahead. The departure date is after the arrival date, so a reservation has at least one night. There is no maximum number of nights.

### BR-002: Available Room Type

A room type is offered only when it is active and, for every night from the arrival date to the night before the departure date, the number of its active rooms that are not blocked that night is greater than the number of its Confirmed and Checked In reservations for that night.

### BR-003: Number of Guests

The number of guests is between 1 and 10 and must not exceed the maximum number of guests of the room type. Room types with a lower maximum are not offered. One reservation covers one room type; a larger party makes several reservations.

### BR-004: Total Price

The total price is the number of nights (departure date − arrival date) times the nightly rate of the room type, in CHF. It is fixed when the reservation is recorded and does not change when the nightly rate changes later.

### BR-005: No Overbooking

The availability check in step 8 and the recording of the reservation in step 9 happen as one indivisible action. When several guests confirm the last available room of a room type for the same night at the same time, exactly one reservation succeeds and the others follow A5.

### BR-006: Guest Identified by Email Address

A guest is identified by their email address, compared case-insensitively. When the email address already belongs to a guest, the reservation is linked to that guest and the stored first name, last name and phone number are replaced with the values entered in this reservation.

### BR-007: Reservation Code

Every reservation receives a unique reservation code of at least 8 random characters that cannot be derived from other reservation data.

### BR-008: Guest Details

First name, last name, email address and phone number are required. No other personal data is collected.

### BR-009: Privacy Notice

The privacy notice is displayed to the guest before they can confirm the reservation.

### BR-010: No Online Payment

The guest pays at the hotel. The online reservation neither asks for nor processes payment data.

### BR-011: Confirmation Email

While the Email Service accepts emails, the confirmation email is sent within 1 minute after the reservation is recorded. It contains the reservation code, the arrival date, the departure date, the room type, the number of guests, the total price and the online cancellation deadline as described in UC-003 BR-006.

### BR-012: Confirmation Email Retry

When the Email Service does not accept the confirmation email, the system retries sending it for at most 24 hours after the reservation was recorded and then stops. A confirmation email that could not be sent never cancels or changes the reservation.
