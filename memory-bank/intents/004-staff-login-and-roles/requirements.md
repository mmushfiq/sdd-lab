---
intent: 004-staff-login-and-roles
phase: inception
status: inception-complete
created: 2026-10-29T07:58:01Z
updated: 2026-10-29T07:58:01Z
---

# Requirements: Staff Login and Roles

## Intent Overview

Protect all staff views with a login and give each staff member access only to the functions of their role (Receptionist, Manager, Housekeeping). Guest pages under `/booking` stay public. The Manager maintains staff accounts in the app.

Source: `docs/hotel-requirements.md` ("Login and role-based access for receptionist, manager and housekeeping").

## Business Goals

| Goal | Success Metric | Priority |
|------|----------------|----------|
| Only staff can use staff functions | Every staff view requires login; verified by tests | Must |
| Least privilege | Each role sees only its functions | Must |
| No IT help needed for accounts | Manager creates, deactivates and resets accounts in the app | Must |

---

## Functional Requirements

### FR-1: Log in and out
- **Description**: Staff log in with username and password and can log out.
- **Acceptance Criteria**:
  - Opening any staff view without being logged in redirects to the login view.
  - Valid credentials of an active user log in and open the start view of the user's role.
  - Wrong username or password, or a deactivated user, show one generic message "Invalid username or password".
  - Logout ends the session and shows the login view.
- **Priority**: Must
- **Related Stories**: 001-log-in-and-out

### FR-2: Initial manager account
- **Description**: On first start with no users, the system creates a Manager account.
- **Acceptance Criteria**:
  - Username `manager`, password from configuration property `hrs.initial-manager-password`.
  - If no users exist and the property is missing or shorter than 10 characters, the application fails to start with a clear message.
  - If users already exist, nothing is created.
- **Priority**: Must
- **Related Stories**: 001-log-in-and-out

### FR-3: Role-based access
- **Description**: Views are accessible by role.
- **Acceptance Criteria**:
  - Housekeeping: cleaning list only.
  - Receptionist: reservation search/booking, reservation list (change, cancel, anonymize), room overview, check-in/out.
  - Manager: everything a Receptionist can do, plus room types, rooms, reports and staff users; also the cleaning list.
  - Opening a view without the required role shows "Access denied" (no data).
  - Navigation shows only the views the user may open.
  - Services enforce the same roles (not only the UI).
  - Guest pages under `/booking` stay accessible without login.
- **Priority**: Must
- **Related Stories**: 002-restrict-views-by-role

### FR-4: Manage staff users
- **Description**: The Manager creates, edits and deactivates staff accounts.
- **Acceptance Criteria**:
  - A user has a unique username (case-insensitive), a display name, one or more roles, an active flag and a password.
  - Passwords have at least 10 characters and are stored as BCrypt hashes.
  - Deactivated users cannot log in; their history stays.
  - The last active Manager cannot be deactivated or lose the Manager role.
- **Priority**: Must
- **Related Stories**: 003-manage-staff-users

### FR-5: Reset a staff password
- **Description**: The Manager sets a new password for a staff user.
- **Acceptance Criteria**:
  - New password must have at least 10 characters and be entered twice identically.
  - The old password stops working immediately.
- **Priority**: Must
- **Related Stories**: 004-reset-staff-password

---

## Non-Functional Requirements

### Security
| Requirement | Standard | Notes |
|-------------|----------|-------|
| Authentication | Spring Security form login (Vaadin `LoginForm`, `VaadinSecurityConfigurer`) | Session-based |
| Authorization | RBAC on views (`@RolesAllowed`) and services (method security) | Roles RECEPTIONIST, MANAGER, HOUSEKEEPING |
| Password storage | BCrypt | Never logged or shown |
| Public area | `/booking/**` | `@AnonymousAllowed` |

### Performance
| Requirement | Metric | Target |
|-------------|--------|--------|
| Login | Time to start view | < 1 s |

### Compliance
| Requirement | Standard | Notes |
|-------------|----------|-------|
| Access to guest data | GDPR | Only logged-in staff with a role can see guest data |

---

## Constraints

### Technical Constraints

**Project-wide standards**: loaded from memory-bank standards folder by Construction Agent (`tech-stack.md`: Spring Security + Vaadin login form, with roles)

**Intent-specific constraints**:
- All existing Browserless and Playwright tests must log in with a test user of the right role.

### Business Constraints
- No external identity provider, no MFA, no self-registration.

---

## Assumptions

| Assumption | Risk if Invalid | Mitigation |
|------------|-----------------|------------|
| Few staff (< 20 accounts) | None at this scale | - |
| No password expiry or lockout needed | Brute-force attempts | Run on the hotel network; add lockout later |
| Staff do not change their own password | Managers must reset passwords | Add "change my password" later |

---

## Open Questions

| Question | Owner | Due Date | Resolution |
|----------|-------|----------|------------|
| Should staff be able to change their own password? | Manager | After go-live | Pending |
