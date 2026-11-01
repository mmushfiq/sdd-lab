# Proposal

## Why

Receptionist, Manager and Housekeeping will each get their own views (reservations, room administration, reports,
cleaning list), and guest personal data in those views must not be visible to anyone else (GDPR). Login and
role-based access come first, so every staff view built in later changes is protected from the start instead of
being secured afterwards.

## What Changes

- Staff log in with username and password and log out again.
- Each staff user has exactly one role: Receptionist, Manager or Housekeeping.
- Staff users are provided by the system (seeded); there is no screen to create or edit staff users.
- After login, staff land on a start page that shows who is logged in and their role, with a logout action.
- Staff views can only be opened by a logged-in user with the matching role; an anonymous visitor who opens a staff
  view is sent to the login page.
- Pages meant for guests stay public and never require a login.
- A failed login shows one generic error message, without saying whether the username or the password was wrong.

## Capabilities

### New Capabilities

- `staff-access`: staff login and logout, the staff roles, and which pages require which role versus which are
  public for guests.

### Modified Capabilities

None.

## Impact

- New dependency: Spring Security (`spring-boot-starter-security`) in `pom.xml`.
- New Flyway migration with the staff user table and the seeded staff users (passwords stored only as hashes).
- New security configuration, login view and staff start page under `ch.martinelli.edu.hrs`.
- Later changes put their views behind the roles defined here; guest views from `add-online-booking` are public.
