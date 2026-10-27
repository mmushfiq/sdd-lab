# Use Case: Maintain Seasonal Rates

## Overview

**Use Case ID:** UC-025  
**Use Case Name:** Maintain Seasonal Rates  
**Primary Actor:** Manager  
**Goal:** The manager defines seasons as date ranges with their own nightly rate per room type, so that prices rise in busy periods and fall in quiet ones without changing the normal rate by hand before and after each season.  
**Trigger:** Manager wants to add a season or change or delete an existing one  
**Status:** Tested  

**Requirements:** [FR-027, NFR-009](../requirements.md)

## Preconditions

- The manager is logged in.

## Main Success Scenario

1. Manager opens the seasons.
2. System displays all seasons ordered by first night, each with its name, first night, last night and number of room types with a season rate.
3. Manager creates a new season or selects an existing one.
4. System displays the season with its room types (BR-006), each with its normal rate and its season rate, if one is set.
5. Manager enters or changes the name, the first night, the last night and the season rate of each room type; a room type left without a season rate keeps its normal rate during the season.
6. Manager saves the season.
7. System checks the entered values.
8. System records the season and its season rates.
9. System displays the updated list of seasons; searches and reservations for nights of the season use its season rates from now on (BR-003).

## Alternative Flows

### A1: Invalid Values

**Trigger:** The name is missing or longer than 50 characters, the first night or the last night is missing, the last night is before the first night, or a season rate is negative or higher than 99,999,999.99 (step 7)  
**Flow:**

1. System marks the invalid values; the entered values are kept.
2. Use case continues at step 5.

### A2: Name Already Used

**Trigger:** Another season already has the entered name (step 7)  
**Flow:**

1. System informs the manager that the name is already used.
2. Use case continues at step 5.

### A3: Seasons Overlap

**Trigger:** At least one night from the first night to the last night already belongs to another season (step 7)  
**Flow:**

1. System names the other season and its first and last night and refuses the save (BR-002).
2. Use case continues at step 5.

### A4: Manager Deletes the Season

**Trigger:** Manager deletes the selected existing season instead of saving it (step 6)  
**Flow:**

1. System asks the manager to confirm the deletion.
2. Manager confirms.
3. System removes the season and its season rates (BR-005).
4. Use case continues at step 9.

### A5: Manager Discards the Changes

**Trigger:** Manager leaves the season without saving (step 6)  
**Flow:**

1. System discards the entered values.
2. Use case ends.

## Postconditions

### Success Postconditions

- The season exists with the saved name, first night, last night and season rates, or it no longer exists after a deletion.
- New searches and reservations for nights of the season use its season rates; room types without a season rate use their normal rate.
- The night prices and total prices of existing reservations are unchanged (BR-004).

### Failure Postconditions

- Every season keeps its previous data and season rates, or no new season exists.
- No two seasons overlap.
- No existing reservation is changed.

## Business Rules

### BR-001: Unique Name

Every season has a name that no other season has.

### BR-002: No Overlapping Seasons

A season covers every night from its first night to its last night, both included; a night belongs to the date on which it begins. Every night belongs to at most one season.

### BR-003: Nightly Rate of a Night

The nightly rate of a room type for a night is the season rate of the room type in the season that covers the night. When no season covers the night, or the season sets no season rate for the room type, it is the normal rate of the room type (UC-016).

### BR-004: Existing Reservations Keep Their Prices

Creating, changing or deleting a season applies only to nights recorded afterwards. The night prices and total prices of existing reservations are never changed by it (UC-001 BR-004).

### BR-005: Deleting a Season

A season can be deleted at any time, also when it has started or ended. Its nights use the normal rate for nights recorded afterwards.

### BR-006: Room Types of a Season

A season shows every active room type and every inactive room type that already has a season rate in it. A season rate can be added only for an active room type; an existing season rate of an inactive room type can be changed or removed. A season rate is between 0 and 99,999,999.99 CHF.
