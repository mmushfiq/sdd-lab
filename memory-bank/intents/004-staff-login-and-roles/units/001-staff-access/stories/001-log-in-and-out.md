---
id: 001-log-in-and-out
unit: 001-staff-access
intent: 004-staff-login-and-roles
status: ready
priority: must
created: 2026-10-29T07:58:01Z
assigned_bolt: 007-staff-access
implemented: false
---

# Story: 001-log-in-and-out

## User Story

**As a** staff member
**I want** to log in with my username and password and log out again
**So that** only staff can use the hotel's internal functions

## Acceptance Criteria

- [ ] **Given** I am not logged in, **When** I open any staff view, **Then** I am redirected to the login view
- [ ] **Given** an active user, **When** I log in with correct credentials, **Then** I see my role's start view (Receptionist: room overview, Housekeeping: cleaning list, Manager: room overview)
- [ ] **Given** a wrong password, unknown username or deactivated user, **When** I log in, **Then** I see "Invalid username or password"
- [ ] **Given** I am logged in, **When** I log out, **Then** the session ends and I see the login view
- [ ] **Given** an empty database and `hrs.initial-manager-password` set (≥ 10 chars), **When** the app starts, **Then** user `manager` with role MANAGER exists
- [ ] **Given** an empty database and the property missing or too short, **When** the app starts, **Then** startup fails with a message naming the property
- [ ] **Given** users exist, **When** the app starts, **Then** no user is created

## Technical Notes

- Add `spring-boot-starter-security`; migration for `staff_user` and `staff_user_role`.
- Set the property in test and dev configuration.

## Dependencies

### Requires
- intents 001-003 complete

### Enables
- 002-restrict-views-by-role, 003-manage-staff-users

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Username with different letter case | Login succeeds |

## Out of Scope

- "Remember me", password change by the user
