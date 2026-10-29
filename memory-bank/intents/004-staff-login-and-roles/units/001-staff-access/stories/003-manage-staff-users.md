---
id: 003-manage-staff-users
unit: 001-staff-access
intent: 004-staff-login-and-roles
status: ready
priority: must
created: 2026-10-29T07:58:01Z
assigned_bolt: 008-staff-access
implemented: false
---

# Story: 003-manage-staff-users

## User Story

**As a** manager
**I want** to create, edit and deactivate staff accounts
**So that** new staff can start working and leavers lose access

## Acceptance Criteria

- [ ] **Given** the staff user view, **When** I open it, **Then** I see all users with username, display name, roles and active flag
- [ ] **Given** a new username, display name, at least one role and a password of ≥ 10 characters, **When** I save, **Then** the user can log in
- [ ] **Given** a username that exists in any letter case, **When** I save, **Then** I see "Username already exists"
- [ ] **Given** no role selected or a password shorter than 10 characters, **When** I save, **Then** I see a validation error
- [ ] **Given** an active user, **When** I deactivate them, **Then** they can no longer log in
- [ ] **Given** I am the last active Manager, **When** I try to deactivate myself or remove my Manager role, **Then** I see "At least one active manager is required" and nothing changes

## Technical Notes

- BCrypt via the Spring `PasswordEncoder`; never show or log hashes.

## Dependencies

### Requires
- 001-log-in-and-out

### Enables
- 004-reset-staff-password

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Deactivated user is currently logged in | Access ends at the next login (session expiry is acceptable) |

## Out of Scope

- Deleting users
