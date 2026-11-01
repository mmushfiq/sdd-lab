# Tasks

## 1. Dependencies

- [ ] 1.1 Add `spring-boot-starter-security` and test dependency `spring-security-test` to `pom.xml`; verify
  `./mvnw compile` succeeds

## 2. Staff user table and seeded users

- [ ] 2.1 Create `src/main/resources/db/migration/V1__create_staff_user.sql` with table `staff_user` (columns and
  constraints from design.md, decision 3) and insert the three seeded users with `{bcrypt}` hashes of their
  development passwords; verify `./mvnw compile` runs the migration and generates the jOOQ class `STAFF_USER` in
  `ch.martinelli.edu.hrs.db`
- [ ] 2.2 Add a test that inserts a `staff_user` row with role `GUEST` and a second row with an existing username;
  verify both inserts are rejected by the database (scenario "Staff user has one role")

## 3. Authentication

- [ ] 3.1 Implement `StaffUserDetailsService` in `ch.martinelli.edu.hrs.core.security` that loads a staff user by
  username with jOOQ and grants the authority `ROLE_<role>`; verify with a test that `reception`, `manager` and
  `housekeeping` load with `ROLE_RECEPTIONIST`, `ROLE_MANAGER` and `ROLE_HOUSEKEEPING`, and an unknown username
  throws `UsernameNotFoundException` (scenario "One staff user per role after installation")
- [ ] 3.2 Add a test that each seeded `password_hash` starts with `{bcrypt}`, does not contain the plain password,
  and matches the development password through the `PasswordEncoder` bean (requirement "Passwords are not stored
  readable")
- [ ] 3.3 Implement `SecurityConfig` with `VaadinSecurityConfigurer`, login view `LoginView`, the delegating
  `PasswordEncoder` bean and default success URL `/staff`; verify `HrsApplicationTests.contextLoads` passes

## 4. Login view

- [ ] 4.1 Implement `LoginView` (route `login`, `@AnonymousAllowed`) with Vaadin `LoginForm` and the error text
  "Invalid username or password"; verify `./mvnw compile` succeeds
- [ ] 4.2 Write a Browserless test for `LoginView` extending `HrsBrowserlessTest`: the form is shown to an anonymous
  visitor, and with `?error` in the URL the error "Invalid username or password" is shown; verify `./mvnw test`
  passes

## 5. Staff start view and logout

- [ ] 5.1 Implement `StaffStartView` in `ch.martinelli.edu.hrs.staff.ui` (route `staff`, `@PermitAll`) showing full
  name and role of the logged-in user from `AuthenticationContext`, with a logout button calling
  `AuthenticationContext.logout()`; verify `./mvnw compile` succeeds
- [ ] 5.2 Write a Browserless test for `StaffStartView` with the seeded user `reception` logged in: the page shows
  "Rita Reception" and "Receptionist" and a logout button; and an anonymous visitor navigating to `staff` ends up on
  `LoginView`; verify `./mvnw test` passes (scenarios "Start page shows the logged-in user", "Anonymous visitor opens
  a staff page")

## 6. Access denied and public pages

- [ ] 6.1 Implement `AccessDeniedView` in `ch.martinelli.edu.hrs.core.ui` implementing
  `HasErrorParameter<AccessDeniedException>` that shows "Access denied"; verify `./mvnw compile` succeeds
- [ ] 6.2 Add test-only views in `src/test`: `ManagerOnlyTestView` (`@RolesAllowed("MANAGER")`,
  `@AccessDeniedErrorRouter`) and `GuestTestView` (`@AnonymousAllowed`); verify `./mvnw test-compile` succeeds
- [ ] 6.3 Write a Browserless test for `AccessDeniedView`: `housekeeping` navigating to `ManagerOnlyTestView` sees
  "Access denied" and none of the view's content; `manager` sees the view; an anonymous visitor sees `GuestTestView`
  without login; verify `./mvnw test` passes (scenarios "Role is allowed", "Role is not allowed", "Guest opens a
  guest page")

## 7. End-to-end

- [ ] 7.1 Write `StaffLoginIT` extending `PlaywrightIT`: log in as `reception`/`reception` and see the start page
  with "Rita Reception"; log out and land on the login page; open `/staff` again and land on the login page; log in
  with a wrong password and with an unknown username and see "Invalid username or password" both times; submit with
  an empty password and stay on the login page; verify `./mvnw verify -Dit.test=StaffLoginIT` passes (scenarios
  "Successful login", "Wrong password", "Unknown username", "Empty username or password", "Logout", "Staff page
  after logout")
- [ ] 7.2 Run `./mvnw verify`; verify the build ends green

## Workflow follow-up

- Before go-live, add a new migration that replaces the three development password hashes (design.md, risks).
- Run `/opsx:verify add-staff-login`, then `/opsx:archive add-staff-login`.
