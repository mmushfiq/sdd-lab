# Use Case: View Occupancy Report

## Overview

**Use Case ID:** UC-019  
**Use Case Name:** View Occupancy Report  
**Primary Actor:** Manager  
**Goal:** The manager sees the occupancy rate per day and per month for a chosen period, so that they understand how well the hotel is used.  
**Trigger:** Manager asks for the occupancy report  
**Status:** Reviewed  

**Requirements:** [FR-021, NFR-002, NFR-009](../requirements.md)

## Preconditions

- The manager is logged in.

## Main Success Scenario

1. Manager opens the occupancy report.
2. Manager enters the first and the last date of the period.
3. System checks the period.
4. System calculates, for every day and every month of the period, the occupied room nights, the available room nights and the occupancy rate.
5. System displays the occupancy rate per day and per month, and for the whole period; the manager sees how well the hotel was used.

## Alternative Flows

### A1: Invalid Period

**Trigger:** A date is missing, the last date is before the first date, the last date is after today, or the period is longer than 366 days (step 3)  
**Flow:**

1. System marks the invalid dates and explains the allowed period.
2. Use case continues at step 2.

### A2: No Available Room Nights

**Trigger:** On a day all rooms are inactive or blocked, so there are no available room nights (step 4)  
**Flow:**

1. System shows no occupancy rate for that day and marks it as without available rooms.
2. Use case continues at step 5.

## Postconditions

### Success Postconditions

- The manager has seen the occupancy rate per day, per month and for the whole period.
- No data is changed.

### Failure Postconditions

- No data is changed.

## Business Rules

### BR-001: Occupancy Rate

The occupancy rate is the number of occupied room nights divided by the number of available room nights, in percent, rounded to one decimal place. A month's and the period's rate are calculated from the summed room nights, not as the average of daily rates.

### BR-002: Occupied Room Night

A night counts as an occupied room night for every reservation with status Checked In or Checked Out whose stay includes that night (from the arrival date to the night before the departure date).

### BR-003: Available Room Night

A night counts as an available room night for every room that is currently active and not blocked that night.

### BR-004: Report Period

The period is at most 366 days long and ends today or earlier. A month that is only partly in the period is calculated for the days in the period.
