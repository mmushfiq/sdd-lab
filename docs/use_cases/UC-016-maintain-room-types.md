# Use Case: Maintain Room Types

## Overview

**Use Case ID:** UC-016  
**Use Case Name:** Maintain Room Types  
**Primary Actor:** Manager  
**Goal:** The manager creates and edits room types with name, description, maximum number of guests and nightly rate, so that guests see correct offers and prices.  
**Trigger:** Manager wants to add a room type or change an existing one  
**Status:** Implemented  

**Requirements:** [FR-018, NFR-009](../requirements.md)

## Preconditions

- The manager is logged in.

## Main Success Scenario

1. Manager opens the room types.
2. System displays all room types with name, maximum number of guests, nightly rate, number of active rooms and whether they are active.
3. Manager creates a new room type or selects an existing one.
4. Manager enters or changes the name, description, maximum number of guests, nightly rate and whether the room type is active.
5. Manager saves the room type.
6. System checks the entered values.
7. System records the room type.
8. System displays the updated list of room types; the room type is offered with the saved data.

## Alternative Flows

### A1: Invalid Values

**Trigger:** The name is missing or longer than 50 characters, the description is longer than 500 characters, the maximum number of guests is not between 1 and 10, or the nightly rate is missing or negative (step 6)  
**Flow:**

1. System marks the invalid values; the entered values are kept.
2. Use case continues at step 4.

### A2: Name Already Used

**Trigger:** Another room type already has the entered name (step 6)  
**Flow:**

1. System informs the manager that the name is already used.
2. Use case continues at step 4.

### A3: Maximum Below Existing Reservations

**Trigger:** The new maximum number of guests is lower than the number of guests of a Confirmed or Checked In reservation of the room type (step 6)  
**Flow:**

1. System lists the affected reservations and refuses the change (BR-003).
2. Use case continues at step 4.

### A4: Manager Discards the Changes

**Trigger:** Manager leaves the room type without saving (step 5)  
**Flow:**

1. System discards the entered values.
2. Use case ends.

## Postconditions

### Success Postconditions

- The room type exists with the saved name, description, maximum number of guests, nightly rate and active flag.
- New searches and reservations use the saved data; total prices of existing reservations are unchanged.

### Failure Postconditions

- The room type keeps its previous data, or no new room type exists.
- No existing reservation is changed.

## Business Rules

### BR-001: Unique Name

Every room type has a name that no other room type has.

### BR-002: Nightly Rate Change

A changed nightly rate applies to reservations recorded or changed afterwards. The total price of an existing reservation is never changed by it.

### BR-003: Maximum Number of Guests

The maximum number of guests is between 1 and 10 and cannot be set below the number of guests of a Confirmed or Checked In reservation of the room type.

### BR-004: Deactivation

An inactive room type is no longer offered for new reservations, and no reservation can be changed to it. Existing reservations of the room type stay valid: they can change their dates and number of guests (UC-005 BR-004), and they can be checked in and checked out. Room types are never deleted.
