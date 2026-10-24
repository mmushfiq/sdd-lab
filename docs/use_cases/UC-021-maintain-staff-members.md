# Use Case: Maintain Staff Members

## Overview

**Use Case ID:** UC-021  
**Use Case Name:** Maintain Staff Members  
**Primary Actor:** Manager  
**Goal:** The manager creates, edits and deactivates staff members with their role, so that only current employees can use the staff functions.  
**Trigger:** An employee joins, changes role or leaves the hotel  
**Status:** Reviewed  

**Requirements:** [FR-023, NFR-009, NFR-010](../requirements.md)

## Preconditions

- The manager is logged in.

## Main Success Scenario

1. Manager opens the staff members.
2. System displays all staff members with username, first name, last name, role and whether they are active.
3. Manager creates a new staff member or selects an existing one.
4. Manager enters or changes the username, first name, last name, role and whether the staff member is active, and for a new staff member an initial password.
5. Manager saves the staff member.
6. System checks the entered values.
7. System records the staff member; a password is stored so that it can never be shown again.
8. System displays the updated list of staff members; the staff member can use exactly the functions of their role while active.

## Alternative Flows

### A1: Invalid Values

**Trigger:** The username, first name, last name or role is missing, a value is too long, or the initial password is shorter than 10 characters (step 6)  
**Flow:**

1. System marks the invalid values; the entered values except the password are kept.
2. Use case continues at step 4.

### A2: Username Already Used

**Trigger:** Another staff member already has the entered username (step 6)  
**Flow:**

1. System informs the manager that the username is already used.
2. Use case continues at step 4.

### A3: Manager Changes Their Own Account

**Trigger:** The manager deactivates their own account or changes their own role (step 6)  
**Flow:**

1. System refuses the change and informs the manager that another manager has to make it (BR-004).
2. Use case continues at step 4.

### A4: Manager Resets a Password

**Trigger:** An existing staff member forgot their password (step 4)  
**Flow:**

1. Manager enters a new password for the staff member.
2. Use case continues at step 5.

### A5: Staff Member Deactivated or Role Changed

**Trigger:** The manager deactivated a staff member or changed their role (step 7)  
**Flow:**

1. System ends every open session of the staff member, so the change applies at their next login.
2. Use case continues at step 8.

### A6: Manager Discards the Changes

**Trigger:** Manager leaves the staff member without saving (step 5)  
**Flow:**

1. System discards the entered values.
2. Use case ends.

## Postconditions

### Success Postconditions

- The staff member exists with the saved username, names, role and active flag.
- An active staff member can log in with their password and reach only the functions of their role; an inactive one cannot log in.
- No password is shown, logged or stored in readable form.

### Failure Postconditions

- The staff member keeps their previous data, or no new staff member exists.
- At least one active manager remains.
- No password is shown, logged or stored in readable form.

## Business Rules

### BR-001: Unique Username

Every staff member has a username that no other staff member has, compared case-insensitively.

### BR-002: Exactly One Role

Every staff member has exactly one role: Receptionist, Manager or Housekeeper.

### BR-003: Password

A password has at least 10 characters. It is stored only in a form from which it cannot be read back, and it is never shown or logged.

### BR-004: Own Account

A manager cannot deactivate their own account or change their own role, so that at least one active manager always remains.

### BR-005: No Deletion

Staff members are never deleted; a staff member who leaves is deactivated and can no longer log in.
