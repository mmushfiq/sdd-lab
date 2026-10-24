# Use Case: Log In

## Overview

**Use Case ID:** UC-022  
**Use Case Name:** Log In  
**Primary Actor:** Staff Member  
**Goal:** The staff member logs in with their username and password, so that they can use the functions of their role while guests' data stays protected.  
**Trigger:** A staff member starts work at a reception desk, tablet or office computer  
**Status:** Approved  

**Requirements:** [FR-024, NFR-009, NFR-010, NFR-012, NFR-013](../requirements.md)

## Preconditions

- Staff member accounts are maintained by a manager (UC-021 Maintain Staff Members).

## Main Success Scenario

1. Staff member opens the staff login.
2. Staff member enters the username and the password.
3. System checks that an active staff member with this username exists and that the password matches.
4. System starts a session for the staff member with the functions of their role.
5. System displays the start screen of the role (Receptionist: arrivals and departures, Housekeeper: cleaning list, Manager: occupancy report); the staff member is logged in.

## Alternative Flows

### A1: Input Missing

**Trigger:** The username or the password is empty (step 2)  
**Flow:**

1. System marks the missing input.
2. Use case continues at step 2.

### A2: Login Refused

**Trigger:** No active staff member has this username, or the password does not match (step 3)  
**Flow:**

1. System informs the staff member that the username or password is wrong, without telling which, and without telling whether the account is inactive (BR-001).
2. Use case continues at step 2.

## Postconditions

### Success Postconditions

- The staff member has a session in which exactly the functions of their role are available.

### Failure Postconditions

- No session is started.
- The response does not reveal whether a username exists or whether an account is inactive.
- No password is shown or logged.

## Business Rules

### BR-001: No Disclosure on Failure

A refused login gives the same answer for an unknown username, a wrong password and an inactive staff member.

### BR-002: Session Timeout

A session ends automatically after 30 minutes without activity; the staff member then has to log in again.

### BR-003: Role-Based Functions

During the session, the staff member can reach only the use cases connected to their role: Receptionist UC-004 to UC-012, Housekeeper UC-013 and UC-014, Manager UC-016 to UC-021 and UC-024. Every staff member can log out (UC-023). Guest functions stay reachable without login.

### BR-004: Password Protection

The password is never shown, logged or stored in a form from which it can be read back.
