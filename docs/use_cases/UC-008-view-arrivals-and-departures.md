# Use Case: View Arrivals and Departures

## Overview

**Use Case ID:** UC-008  
**Use Case Name:** View Arrivals and Departures  
**Primary Actor:** Receptionist  
**Goal:** The receptionist sees today's arrivals and departures, so that they can prepare check-ins and check-outs.  
**Trigger:** Receptionist asks for today's arrivals and departures  
**Status:** Reviewed  

**Requirements:** [FR-010, NFR-002, NFR-009](../requirements.md)

## Preconditions

- The receptionist is logged in.

## Main Success Scenario

1. Receptionist opens the arrivals and departures.
2. System displays today's arrivals with reservation code, guest name, room type, number of nights, number of guests and status, ordered by guest last name.
3. System displays today's departures with reservation code, guest name, assigned room, total price and status, ordered by room number.
4. Receptionist selects a reservation from one of the lists.
5. System displays the reservation and the action its status allows (check in for an arrival, check out for a departure); the receptionist is prepared for the check-in or check-out.

## Alternative Flows

### A1: No Arrivals or No Departures Today

**Trigger:** No reservation arrives today or none departs today (step 2)  
**Flow:**

1. System shows that the list is empty for today.
2. Use case continues at step 3.

### A2: Overdue Departures

**Trigger:** A reservation still has status Checked In although its departure date is before today (step 3)  
**Flow:**

1. System additionally lists it among the departures and marks it as overdue.
2. Use case continues at step 4.

### A3: Receptionist Only Reviews the Lists

**Trigger:** Receptionist does not select a reservation (step 4)  
**Flow:**

1. Receptionist leaves the lists.
2. Use case ends.

## Postconditions

### Success Postconditions

- The receptionist has seen all of today's arrivals and departures and the selected reservation.
- No reservation or room data is changed.

### Failure Postconditions

- No reservation or room data is changed.

## Business Rules

### BR-001: Today's Arrivals

An arrival is a reservation with arrival date today and status Confirmed or Checked In. Checked-in arrivals are marked as done.

### BR-002: Today's Departures

A departure is a reservation with departure date today and status Checked In or Checked Out, plus every overdue reservation (status Checked In, departure date before today). Checked-out departures are marked as done.

### BR-003: Hotel Date

"Today" is the current date in the hotel's local time.
