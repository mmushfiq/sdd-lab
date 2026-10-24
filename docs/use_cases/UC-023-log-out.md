# Use Case: Log Out

## Overview

**Use Case ID:** UC-023  
**Use Case Name:** Log Out  
**Primary Actor:** Staff Member  
**Goal:** The staff member ends their session, so that nobody else can use it at a shared reception desk or tablet.  
**Trigger:** Staff member asks to log out  
**Status:** Reviewed  

**Requirements:** [FR-025, NFR-009, NFR-013](../requirements.md)

## Preconditions

- The staff member has started a session with UC-022 Log In.

## Main Success Scenario

1. Staff member requests to log out.
2. System ends the staff member's session.
3. System displays the staff login; the session can no longer be used.

## Alternative Flows

### A1: Session Already Ended

**Trigger:** The session already ended after 30 minutes without activity or because a manager deactivated the staff member or changed their role (step 1)  
**Flow:**

1. System displays the staff login.
2. Use case ends.

## Postconditions

### Success Postconditions

- The session is ended; no staff function can be reached in it without logging in again.

### Failure Postconditions

- No staff function can be reached in an ended session.

## Business Rules

### BR-001: Logged Out Everywhere in the Session

After logging out, going back in the browser does not show staff data or allow a staff action; the staff login is displayed instead.

### BR-002: Unsaved Input

Input that was not saved before logging out is discarded.
