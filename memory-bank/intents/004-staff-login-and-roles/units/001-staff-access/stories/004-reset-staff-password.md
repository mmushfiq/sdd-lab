---
id: 004-reset-staff-password
unit: 001-staff-access
intent: 004-staff-login-and-roles
status: ready
priority: must
created: 2026-10-29T07:58:01Z
assigned_bolt: 008-staff-access
implemented: false
---

# Story: 004-reset-staff-password

## User Story

**As a** manager
**I want** to set a new password for a staff member
**So that** someone who forgot their password can work again

## Acceptance Criteria

- [ ] **Given** a user, **When** I enter a new password (≥ 10 characters) twice identically and save, **Then** the user can log in with it and the old password no longer works
- [ ] **Given** the two entries differ, **When** I save, **Then** I see "Passwords do not match"
- [ ] **Given** a password shorter than 10 characters, **When** I save, **Then** I see a length error

## Technical Notes

- Separate dialog; password fields never pre-filled.

## Dependencies

### Requires
- 003-manage-staff-users

### Enables
- None

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Reset the manager's own password | Allowed |

## Out of Scope

- Password reset by email
