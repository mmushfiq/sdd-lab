# Use Case: View Revenue Report

## Overview

**Use Case ID:** UC-020  
**Use Case Name:** View Revenue Report  
**Primary Actor:** Manager  
**Goal:** The manager sees the revenue per month and per room type for a chosen period, so that they can run the business without manual spreadsheet work.  
**Trigger:** Manager asks for the revenue report  
**Status:** Reviewed  

**Requirements:** [FR-022, NFR-002, NFR-009, C-007](../requirements.md)

## Preconditions

- The manager is logged in.

## Main Success Scenario

1. Manager opens the revenue report.
2. Manager enters the first and the last date of the period.
3. System checks the period.
4. System calculates the revenue of the period per month and per room type.
5. System displays a table with one row per month and one column per room type, with the totals per month, per room type and for the whole period in CHF; the manager sees the revenue of the period.

## Alternative Flows

### A1: Invalid Period

**Trigger:** A date is missing, the last date is before the first date, the last date is after today, or the period is longer than 366 days (step 3)  
**Flow:**

1. System marks the invalid dates and explains the allowed period.
2. Use case continues at step 2.

### A2: No Revenue in the Period

**Trigger:** No reservation was checked out in the period (step 4)  
**Flow:**

1. System displays the table with a revenue of 0.00 CHF for every month and room type.
2. Use case continues at step 5.

## Postconditions

### Success Postconditions

- The manager has seen the revenue per month, per room type and for the whole period.
- No data is changed.

### Failure Postconditions

- No data is changed.

## Business Rules

### BR-001: Revenue

The revenue is the sum of the total prices of all reservations with status Checked Out whose time of check-out lies in the period, in CHF with two decimal places. Cancelled and no-show reservations count no revenue.

### BR-002: Assignment to a Month

A reservation's total price counts entirely for the month of its check-out, not spread over the nights of the stay.

### BR-003: Room Types in the Report

Every room type with revenue in the period has a column, including inactive room types.

### BR-004: Report Period

The period is at most 366 days long and ends today or earlier.
