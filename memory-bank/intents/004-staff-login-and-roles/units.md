---
intent: 004-staff-login-and-roles
phase: inception
status: units-decomposed
updated: 2026-10-29T07:58:01Z
---

# Staff Login and Roles - Unit Decomposition

## Units Overview

One vertical unit.

### Unit 1: 001-staff-access

**Description**: Spring Security configuration, login view, role rules on all views and services, staff user management.

**Stories**:

- 001-log-in-and-out
- 002-restrict-views-by-role
- 003-manage-staff-users
- 004-reset-staff-password

**Deliverables**:

- Migration for `staff_user` and `staff_user_role`
- `security` package: `SecurityConfig`, `LoginView`, user details service, initial manager bootstrap
- `staff` package: `StaffUserView`, `StaffUserService`
- `@RolesAllowed` / `@AnonymousAllowed` on all existing views and services; test login support for existing tests

**Dependencies**:

- Depends on: intents 001, 002, 003 (views to protect)
- Depended by: intent 005 (reports are Manager-only)

**Estimated Complexity**: M

## Requirement-to-Unit Mapping

- **FR-1** Log in and out → `001-staff-access`
- **FR-2** Initial manager account → `001-staff-access`
- **FR-3** Role-based access → `001-staff-access`
- **FR-4** Manage staff users → `001-staff-access`
- **FR-5** Reset a staff password → `001-staff-access`

## Unit Dependency Graph

```text
[intents 001-003] ──> [001-staff-access]
```

## Execution Order

1. 001-staff-access (bolt 007, then bolt 008)
