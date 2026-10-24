# Use Case: Search Reservations

## Overview

**Use Case ID:** UC-007  
**Use Case Name:** Search Reservations  
**Primary Actor:** Receptionist  
**Goal:** The receptionist finds a reservation by guest name, reservation code or date, so that they can answer a guest's question about it while the guest waits.  
**Trigger:** A guest asks the receptionist about a reservation  
**Status:** Implemented  

**Requirements:** [FR-009, NFR-002, NFR-009](../requirements.md)

## Preconditions

- The receptionist is logged in.

## Main Success Scenario

1. Receptionist opens the reservation search.
2. Receptionist enters a guest name, a reservation code or a date.
3. System displays the matching reservations, each with reservation code, guest name, arrival date, departure date, room type, number of guests and status, ordered by arrival date.
4. Receptionist selects a reservation.
5. System displays the reservation with all its data, the guest's email address and phone number, the assigned room when checked in, and the actions its status allows (change, cancel, check in, check out, record no-show); the receptionist has found the reservation.

## Alternative Flows

### A1: No Search Criterion

**Trigger:** The receptionist starts the search without entering a criterion (step 2)  
**Flow:**

1. System asks for at least one criterion.
2. Use case continues at step 2.

### A2: No Matching Reservation

**Trigger:** No reservation matches the criteria (step 3)  
**Flow:**

1. System informs the receptionist that no reservation matches.
2. Use case continues at step 2.

### A3: Too Many Matches

**Trigger:** More than 100 reservations match the criteria (step 3)  
**Flow:**

1. System displays the first 100 matches and asks the receptionist to narrow the search (BR-003).
2. Use case continues at step 4.

### A4: Search Refined

**Trigger:** The searched reservation is not in the list (step 4)  
**Flow:**

1. Receptionist changes or combines the criteria.
2. Use case continues at step 3.

## Postconditions

### Success Postconditions

- The receptionist sees the selected reservation and the actions its status allows.
- No reservation or guest data is changed.

### Failure Postconditions

- No reservation or guest data is changed.

## Business Rules

### BR-001: Search Criteria

The guest name matches when the entered text is part of the first name or last name, compared case-insensitively. The reservation code matches only exactly. A date matches every reservation whose arrival date is on or before the date and whose departure date is on or after the date. Several criteria combine so that all of them must match.

### BR-002: All Statuses

The search finds reservations of every status and channel.

### BR-003: Result Limit

At most 100 reservations are listed per search.
