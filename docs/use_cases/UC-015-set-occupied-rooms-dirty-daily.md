# Use Case: Set Occupied Rooms Dirty Daily

## Overview

**Use Case ID:** UC-015  
**Use Case Name:** Set Occupied Rooms Dirty Daily  
**Primary Actor:** Scheduler  
**Goal:** Every occupied room is set to dirty at the start of each day, so that staying guests get their room cleaned daily.  
**Trigger:** Start of each day at 00:00 hotel local time  
**Status:** Approved  

**Requirements:** [FR-017](../requirements.md)

## Preconditions

- None.

## Main Success Scenario

1. Scheduler starts the daily cleaning run.
2. System determines every room that is assigned to a reservation with status Checked In.
3. System records the cleaning status Dirty for each of these rooms; every occupied room is on today's cleaning list.

## Alternative Flows

### A1: No Occupied Room

**Trigger:** No room is assigned to a reservation with status Checked In (step 2)  
**Flow:**

1. System changes no cleaning status.
2. Use case ends.

### A2: Run Missed at 00:00

**Trigger:** The system was not running at 00:00, so the scheduler did not start the run (step 1)  
**Flow:**

1. No room is set to dirty for that day; the next run at 00:00 of the following day includes all occupied rooms again (BR-001).
2. Use case ends.

## Postconditions

### Success Postconditions

- Every room assigned to a Checked In reservation at the time of the run has cleaning status Dirty.

### Failure Postconditions

- Each room either keeps its previous cleaning status or is Dirty; no room is set to Clean by the run.
- No reservation is changed.

## Business Rules

### BR-001: Only at 00:00

The scheduler starts the daily cleaning run only at 00:00 hotel local time. A missed run is not made up later that day, so that a room cleaned during the day is never set to dirty again on the same day.

### BR-002: Only Occupied Rooms

Only rooms assigned to a Checked In reservation are set to Dirty; free and blocked rooms keep their cleaning status. Rooms that are already dirty stay dirty.
