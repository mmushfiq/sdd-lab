---
id: 002-restrict-views-by-role
unit: 001-staff-access
intent: 004-staff-login-and-roles
status: ready
priority: must
created: 2026-10-29T07:58:01Z
assigned_bolt: 007-staff-access
implemented: false
---

# Story: 002-restrict-views-by-role

## User Story

**As a** manager
**I want** each staff member to see only the functions of their role
**So that** guest data and settings are protected

## Acceptance Criteria

- [ ] **Given** a Housekeeping user, **When** logged in, **Then** navigation shows only the cleaning list, and opening the reservation list shows "Access denied"
- [ ] **Given** a Receptionist, **When** logged in, **Then** they can open reservation search/booking, reservation list, room overview and check-in/out, but not room types, rooms, reports or staff users
- [ ] **Given** a Manager, **When** logged in, **Then** they can open every staff view
- [ ] **Given** a Receptionist, **When** a manager-only service method is called, **Then** it is rejected (method security)
- [ ] **Given** no login, **When** a guest opens `/booking`, view or cancel pages, **Then** they work as before
- [ ] **Given** the existing tests of intents 001-003, **When** they run with the matching role, **Then** they pass

## Technical Notes

- `@RolesAllowed` on views and service methods, `@AnonymousAllowed` on guest views; `@EnableMethodSecurity`.
- Test helper to log in as a role in Browserless tests; Playwright base class gets a login step.

## Dependencies

### Requires
- 001-log-in-and-out

### Enables
- intent 005 (Manager-only reports)

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| User with two roles (e.g. Receptionist + Housekeeping) | Sees the union of both |

## Out of Scope

- Per-field permissions
