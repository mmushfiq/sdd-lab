# Design

## Context

The application is an empty Vaadin Flow skeleton (`HrsApplication` only): no Spring Security, no Flyway migration
and no views yet. Requirements are in `specs/staff-access/spec.md`; motivation in `proposal.md`.

## Goals / Non-Goals

**Goals:**

- One security setup that every later change reuses by putting an access annotation on its view.
- Views without an access annotation are denied by default, so a forgotten annotation never exposes a staff page.

**Non-Goals:**

- Changing passwords, password reset, account lockout, "remember me".
- Staff views other than the start page (they come with later changes).

## Decisions

### 1. Vaadin's Spring Security integration with view annotations

Add `spring-boot-starter-security` and configure it with Vaadin's `VaadinSecurityConfigurer`, login view at route
`login`. Access is declared on each view:

| Annotation          | Used for                                          |
|---------------------|---------------------------------------------------|
| `@AnonymousAllowed` | login view and guest views (public)               |
| `@PermitAll`        | staff start page (any logged-in staff user)       |
| `@RolesAllowed`     | role-specific staff views (`RECEPTIONIST`, `MANAGER`, `HOUSEKEEPING`) |

Vaadin's navigation access control denies every view without an annotation and sends anonymous visitors to the
login view. Alternative considered: URL patterns in the security filter chain. Rejected because Vaadin routes are
not all plain URLs and the annotation sits next to the view, where it is reviewed.

### 2. Staff users in the database, loaded with jOOQ

A `UserDetailsService` reads `staff_user` with jOOQ and maps `role` to the authority `ROLE_<role>`. Alternative
considered: in-memory users in the configuration. Rejected because the vision's GDPR and go-live needs require
changing passwords without a code change, and later changes may reference staff users from the database.

### 3. Table `staff_user` (migration `V1__create_staff_user.sql`)

| Column          | Type           | Constraints                                                    |
|-----------------|----------------|----------------------------------------------------------------|
| `id`            | `BIGINT`       | primary key, generated identity                                |
| `username`      | `VARCHAR(50)`  | not null, unique                                               |
| `password_hash` | `VARCHAR(100)` | not null                                                       |
| `full_name`     | `VARCHAR(100)` | not null                                                       |
| `role`          | `VARCHAR(20)`  | not null, check `role IN ('RECEPTIONIST','MANAGER','HOUSEKEEPING')` |

One `role` column (not a role table) enforces "exactly one role" in the database. The table is named `staff_user`
because `user` is a reserved word.

### 4. Passwords as BCrypt hashes

`PasswordEncoderFactories.createDelegatingPasswordEncoder()` (BCrypt, stored with `{bcrypt}` prefix), so the
algorithm can change later without a migration of the column. The migration seeds three users with precomputed
hashes:

| Username       | Full name           | Role         | Password (development only) |
|----------------|---------------------|--------------|-----------------------------|
| `reception`    | Rita Reception      | RECEPTIONIST | `reception`                 |
| `manager`      | Mark Manager        | MANAGER      | `manager`                   |
| `housekeeping` | Hanna Housekeeping  | HOUSEKEEPING | `housekeeping`              |

### 5. Views and packages

| Class              | Package                              | Route        | Access              |
|--------------------|--------------------------------------|--------------|---------------------|
| `SecurityConfig`, `StaffUserDetailsService` | `ch.martinelli.edu.hrs.core.security` | – | – |
| `LoginView`        | `ch.martinelli.edu.hrs.core.ui`      | `login`      | `@AnonymousAllowed` |
| `AccessDeniedView` | `ch.martinelli.edu.hrs.core.ui`      | error view   | –                   |
| `StaffStartView`   | `ch.martinelli.edu.hrs.staff.ui`     | `staff`      | `@PermitAll`        |

- `LoginView` uses Vaadin's `LoginForm` with an English i18n error text "Invalid username or password"; the
  `LoginForm` does not submit with an empty field. After login, the default target is `/staff`.
- `StaffStartView` reads the user from `AuthenticationContext` and shows full name and role; its logout button calls
  `AuthenticationContext.logout()`, which ends the session and returns to `login`.
- `AccessDeniedView` implements `HasErrorParameter<com.vaadin.flow.router.AccessDeniedException>` and shows
  "Access denied", so a logged-in user without the role gets a clear message. Vaadin reroutes a denied navigation to
  "not found" by default; role-restricted views therefore also carry `@AccessDeniedErrorRouter`, which sends the
  denial to `AccessDeniedException` instead. A Browserless test proves the message appears.
- Route `""` stays free for the guest start page of `add-online-booking`.

### 6. Testing role and public access without real views

No Manager-only or guest view exists yet. Tests register test-only views in `src/test` (one `@RolesAllowed("MANAGER")`,
one `@AnonymousAllowed`) to cover the scenarios "Role is not allowed" and "Guest opens a guest page". Browserless
tests log users in with Spring Security's test support (`@WithMockUser` or `@WithUserDetails` on the seeded users).

## Risks / Trade-offs

- [Seeded development passwords reach production] → Before go-live a new migration replaces the three hashes; noted
  in the tasks. There is no UI for it on purpose (no user administration in scope).
- [Generic "Access denied" view reveals that a route exists] → Acceptable: staff are trusted users and the page
  shows no data.
- [A later view forgets its annotation] → Denied by default; it fails visibly in its own tests instead of leaking.

## Migration Plan

First migration of the project (`V1`); no existing data. Rollback = revert the commit and delete the H2 database
file.
