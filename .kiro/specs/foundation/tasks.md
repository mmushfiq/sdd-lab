# Implementation Plan

- [ ] 1. Project setup: security dependencies and shared clock
- [ ] 1.1 Add Spring Security to the build
  - Add the security starter and the security test starter (test scope) with versions managed by the Spring Boot parent
  - Keep all existing dependencies and plugins unchanged
  - Expected and temporary: until 4.2, Spring Boot's default security is active and views run without Vaadin view access control; Browserless tests in tasks 3.x are unaffected because they bypass the HTTP filter chain
  - Done when `./mvnw compile` succeeds and `@WithMockUser` / `@WithUserDetails` resolve in test sources; the existing `HrsApplicationTests` still passes
  - _Requirements: 2.3, 4.4_

- [ ] 1.2 Provide the shared clock and fix "today" in tests
  - Add the production clock bean in the system default time zone
  - Give both test base classes (Browserless and Playwright) a fixed clock at 2026-06-01T12:00:00Z in the system default zone
  - Add a plain unit test that the production clock uses the system default zone
  - Done when the clock unit test (ClockConfigurationTest) passes and both base classes compile with the fixed test clock bean; the fixed date itself is proven by the staff home test in 5.1 and the browser test in 6.1
  - _Requirements: 7.1, 7.2_

- [ ] 2. Reference schema and test data
- [ ] 2.1 Create the room type and room schema with the ten seeded rooms
  - First migration creates room types and rooms with unique room number, unique type name, capacity above zero, nightly rate above zero and a declared foreign key; capacity and rate exist only on the room type
  - Seed the three room types and ten rooms of assumption A1 without explicit ids, resolving the room type by name
  - Add a data test that checks the seeded content against A1 (through the join), that exactly ten rooms have ids below 1000, and that each invalid insert (duplicate number, duplicate type name, unknown room type, capacity 0, rate 0) fails and leaves the row counts unchanged
  - Done when `./mvnw compile` generates the room and room type jOOQ classes and the data test passes
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 2.2 Create the staff member schema and the test staff accounts
  - Second migration creates the staff member table (unique user name, password hash, display name, role limited to the three roles) and inserts no rows
  - Test migration (version 100) adds the three test accounts with ids 1000–1002 and bcrypt hashes of the documented test password
  - Not (P) with 2.1: both rebuild the shared jOOQ code-generation database and generated package, so they run one after the other
  - Done when `./mvnw compile` generates the staff member jOOQ class and the main migrations contain no staff rows; the test accounts are proven by the service test in 4.1
  - _Requirements: 2.1, 2.2, 2.4_

- [ ] 3. Public pages
- [ ] 3.1 (P) Build the public entry page
  - Root route open to anonymous visitors, outside the staff layout, showing the hotel name and a link to the staff home that triggers the staff login
  - Browserless test: an anonymous visitor sees the hotel name and the staff login link (presence and target only; the staff home arrives in 5.1) and no staff navigation
  - Done when the public entry page test passes
  - _Boundary: PublicHomeView_
  - _Requirements: 4.1, 6.1, 6.2_

- [ ] 3.2 (P) Build the login page
  - Login route open to anonymous visitors with the login form posting to the login endpoint
  - Show the form's generic error state when the page is opened after a failed login
  - Browserless test: the form shows without error normally and in error state after a failed login
  - Done when the login page test passes
  - _Boundary: LoginView_
  - _Requirements: 3.2, 4.1_

- [ ] 4. Authentication and access control
- [ ] 4.1 (P) Load staff members for authentication
  - Role vocabulary with display labels and compile-time role-name constants; authenticated staff principal carrying display name and role
  - Read-only lookup of a staff member by user name and the user-details service mapping it to the principal with its role authority
  - Service test: a test account loads with its role authority and display name; the stored hash is bcrypt, matches the test password and differs from it; an unknown user name is rejected; duplicate user name and an unknown role cannot be inserted; no staff row with id below 1000 exists; role-name constants equal the role names
  - Done when the service test passes and the lookup offers no write operation
  - _Boundary: StaffRole, StaffPrincipal, StaffMemberRepository, StaffUserDetailsService_
  - _Depends: 2.2_
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 3.2_

- [ ] 4.2 Secure the application with view-based access control
  - Security configuration registers the login page, logs out to the public entry page, sends a successful login to the staff home by default and provides the delegating password encoder
  - No URL-based view rules; every view without an access annotation is denied
  - Done when the application context starts with the Vaadin security configurer, the existing context-load test passes, and the public entry and login page tests still pass with view access control active; the anonymous redirect is proven in 5.2
  - _Depends: 3.1, 3.2, 4.1_
  - _Requirements: 2.3, 3.1, 3.3, 3.4, 4.1, 4.4_

- [ ] 5. Staff layout and navigation
- [ ] 5.1 Build the staff layout and the staff home page
  - Layout with drawer toggle, hotel name, display name with role label (fallback to the user name for non-staff principals), log-out button and a side navigation built from the access-filtered menu entries
  - Staff home route inside the layout, open to all three roles, menu title "Home" with order 0, showing a greeting with name and role and today's date from the clock
  - StaffHomeViewTest (per role, with the test accounts): the staff home opens, the greeting shows the account's name and role, today shows 2026-06-01
  - StaffLayoutTest, header cases: the header shows "Test Manager (Manager)" and the log-out button
  - Done when StaffHomeViewTest passes for all three roles and the StaffLayoutTest header cases pass
  - _Depends: 4.1, 4.2_
  - _Requirements: 3.1, 3.3, 5.1, 5.3, 5.5, 7.2_

- [ ] 5.2 Verify role-filtered navigation and wrong-role refusal with a test-only view
  - Add ManagerOnlyTestView in the test sources: route `test-manager-only` inside the staff layout, menu title "Manager only", manager role only; it exists only in test and TestHrsApplication runs, never in the main build
  - StaffLayoutTest, menu cases: the manager's menu lists "Home" and "Manager only"; the housekeeper's lists "Home" only
  - AccessControlTest: an anonymous visitor opening the staff home ends on the login page; a housekeeper opening `test-manager-only` by address gets the not-available view and no instance of the test view; a manager reaches it
  - Fallback if route scanning misses the test view: register it in `@BeforeEach` with `RouteConfiguration.forApplicationScope().setAnnotatedRoute(ManagerOnlyTestView.class)` and remove it in `@AfterEach`
  - Done when the StaffLayoutTest menu cases and AccessControlTest pass
  - _Depends: 4.2, 5.1_
  - _Requirements: 4.2, 4.3, 4.4, 5.2, 5.4_

- [ ] 6. End-to-end validation
- [ ] 6.1 Browser test of login, logout and tablet/desktop layout
  - Real login as the test receptionist via the public page link (lands on the staff home because the saved request is replayed) and via the login page directly (lands there through the default target); after login the date shown as today is 2026-06-01
  - Wrong password stays on the login page with the error message
  - Log out lands on the public entry page; opening the staff home afterwards shows the login page
  - At 768×1024 and 1280×800: no horizontal scrolling, log-out visible, navigation reachable directly or through the drawer toggle
  - Done when `./mvnw verify` passes with the new browser test counted in the failsafe reports
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 5.5, 7.2_
