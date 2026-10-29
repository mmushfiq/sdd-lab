---
unit: 001-staff-access
intent: 004-staff-login-and-roles
phase: inception
status: ready
unit_type: backend
default_bolt_type: simple-construction-bolt
created: 2026-10-29T07:58:01Z
updated: 2026-10-29T07:58:01Z
---

# Unit Brief: Staff Access

## Purpose

Make sure only logged-in staff use staff functions, each according to their role, and let the Manager maintain staff accounts.

## Scope

### In Scope
- Login/logout, initial manager bootstrap
- Role rules on all views and services
- Staff user management and password reset

### Out of Scope
- Self-service password change, lockout, MFA, external identity providers
- Guest accounts

---

## Assigned Requirements

| FR | Requirement | Priority |
|----|-------------|----------|
| FR-1 | Log in and out | Must |
| FR-2 | Initial manager account | Must |
| FR-3 | Role-based access | Must |
| FR-4 | Manage staff users | Must |
| FR-5 | Reset a staff password | Must |

---

## Domain Concepts

### Key Entities
| Entity | Description | Attributes |
|--------|-------------|------------|
| StaffUser | A staff account | id, username (unique, case-insensitive), displayName, passwordHash, active, roles |
| Role (enum) | Access level | RECEPTIONIST, MANAGER, HOUSEKEEPING |

### Key Operations
| Operation | Description | Inputs | Outputs |
|-----------|-------------|--------|---------|
| loadUserByUsername | For Spring Security | username | UserDetails (active users only) |
| createUser / updateUser | Maintain accounts | username, displayName, roles, active, password (create) | StaffUser or validation error |
| resetPassword | Set new password | userId, password, confirmation | ok |
| bootstrapManager | First-start account | config property | created or skipped |

---

## Story Summary

- **Total Stories**: 4
- **Must Have**: 4
- **Should Have**: 0
- **Could Have**: 0

### Stories

- [ ] **001-log-in-and-out**: Log in, log out, initial manager - Must - Planned
- [ ] **002-restrict-views-by-role**: Role rules on views and services - Must - Planned
- [ ] **003-manage-staff-users**: Create, edit, deactivate staff - Must - Planned
- [ ] **004-reset-staff-password**: Manager resets a password - Must - Planned

---

## Dependencies

### Depends On
| Unit | Reason |
|------|--------|
| intents 001-003 | Views and services to protect |

### Depended By
| Unit | Reason |
|------|--------|
| intent 005 / 001-management-reports | Manager-only report view |

### External Dependencies
| System | Purpose | Risk |
|--------|---------|------|
| spring-boot-starter-security | New Maven dependency | Low |

---

## Technical Context

### Suggested Technology
Spring Security with Vaadin's `VaadinSecurityConfigurer`, `LoginForm`, `@RolesAllowed`/`@AnonymousAllowed`, `@EnableMethodSecurity`, BCrypt `PasswordEncoder`. Check the current Vaadin 25 docs for the exact security setup before implementing.

### Integration Points
| Integration | Type | Protocol |
|-------------|------|----------|
| All views and services | Annotations | in-process |

### Data Storage
| Data | Type | Volume | Retention |
|------|------|--------|-----------|
| staff_user, staff_user_role | SQL | < 20 users | Permanent (deactivate, don't delete) |

---

## Constraints

- Adding security must not break existing tests: provide a test helper to log in as a given role in Browserless tests and a login step for Playwright tests.
- Test configuration sets `hrs.initial-manager-password`.

---

## Success Criteria

### Functional
- [ ] FR-1 to FR-5 acceptance criteria pass
- [ ] A test per role proves allowed and denied views

### Non-Functional
- [ ] Passwords only stored as BCrypt hashes
- [ ] Services reject calls without the required role

### Quality
- [ ] All tests from intents 001-003 pass with login
- [ ] Code reviewed and approved

---

## Bolt Suggestions

| Bolt | Type | Stories | Objective |
|------|------|---------|-----------|
| 007-staff-access | Simple | 001, 002 | Login and role rules on everything |
| 008-staff-access | Simple | 003, 004 | Staff user management |

---

## Notes

- This intent touches every existing view; do it in one bolt (007) so the app is never half-protected.
