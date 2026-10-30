# Research & Design Decisions

## Summary
- **Feature**: `foundation`
- **Discovery Scope**: New Feature (greenfield on an empty Spring Boot / Vaadin skeleton); full discovery, kept short
  because the stack is fixed by steering.
- **Key Findings**:
  - Vaadin's `VaadinSecurityConfigurer` (replaces the removed `VaadinWebSecurity`) enables view-based access control
    (`@AnonymousAllowed`, `@RolesAllowed`) and registers the login view; `formLogin()` must not be configured
    directly.
  - `MenuConfiguration.getMenuEntries()` returns the `@Menu`-annotated routes already filtered by the UI's access
    control, so role-filtered navigation and "later specs add a menu entry only" need no custom code.
  - The build's jOOQ code generation runs Flyway only on `src/main/resources/db/migration`; on the test classpath,
    Flyway's default location `classpath:db/migration` merges main and test migrations. Test-only staff accounts
    therefore never reach the generated code or production.

## Research Log

### Vaadin 25 security integration
- **Context**: Requirements 3 and 4 need login, logout and per-view role checks.
- **Sources Consulted**: Vaadin docs "Enabling Security" and "Add Login" (`articles/flow/security/enabling-security.adoc`,
  `articles/building-apps/security/add-login.adoc`), via Context7.
- **Findings**: `http.with(VaadinSecurityConfigurer.vaadin(), c -> c.loginView(LoginView.class))` sets up CSRF
  handling for Vaadin requests, static resources, the login page and `NavigationAccessControl`. An anonymous request
  for a protected view redirects to the login view; after login Spring's saved request returns the user to the
  originally requested page. `AuthenticationContext` gives the authenticated principal and `logout()`.
- **Implications**: No custom access checker; views declare roles with annotations. The login view uses Vaadin
  `LoginForm` posting to `login`.

### Role-filtered menu
- **Context**: Requirements 5.2 and 5.4.
- **Sources Consulted**: Vaadin docs "Menu Configuration" (`articles/flow/routing/menu-configuration.adoc`).
- **Findings**: Server routes annotated with `@Menu` are collected and filtered by `NavigationAccessControl`; the
  list is sorted by `order` then path.
- **Implications**: `StaffLayout` renders a `SideNav` from `MenuConfiguration.getMenuEntries()`. A later spec adds a
  staff page by annotating its view with `@Route(layout = StaffLayout.class)`, `@Menu` and `@RolesAllowed`.

### Password storage
- **Context**: Requirement 2.3, brief constraint "passwords hashed".
- **Findings**: Spring Security's `PasswordEncoderFactories.createDelegatingPasswordEncoder()` stores
  `{bcrypt}$2a$...` (68 characters) and can verify older formats later.
- **Implications**: `password_hash VARCHAR(100)`; test data stores `{bcrypt}` hashes.

### Spring Boot 4 starters
- **Context**: Spring Security is not yet a dependency (tech.md).
- **Findings**: Boot 4 splits test support into per-technology test starters (the project already uses
  `spring-boot-starter-flyway-test`, `spring-boot-starter-jooq-test`). Security needs `spring-boot-starter-security`
  and `spring-boot-starter-security-test` (provides `@WithMockUser`, `@WithUserDetails`).
- **Implications**: Two new dependencies, versions managed by the Boot parent.

## Architecture Pattern Evaluation

| Option | Description | Strengths | Risks / Limitations | Notes |
|--------|-------------|-----------|---------------------|-------|
| Vaadin view access control + `@Menu` | Annotations on views, menu from `MenuConfiguration` | Zero custom security/menu code; later specs touch no foundation code | Behaviour bound to Vaadin's defaults (e.g. error view for denied access) | Selected |
| Custom menu registry in `core` | Each spec registers menu items in a foundation class | Full control of labels | Every spec edits foundation code; duplicates access rules | Rejected |
| URL-pattern security only | `requestMatchers` per path | Familiar Spring style | Vaadin navigation is client-side; route checks would be missed | Rejected |

## Design Decisions

### Decision: No staff passwords in main migrations
- **Context**: Brief asks to decide whether seeded staff get initial passwords; `testing.md` forbids passwords in main
  migrations.
- **Alternatives Considered**:
  1. Main migration seeds three staff members with known initial passwords — violates steering, ships known
     credentials.
  2. Startup bootstrap creating staff from configuration properties — extra code and secrets handling.
  3. Main migration creates the table only; each environment adds an environment-specific Flyway location with its
     staff rows; tests use the test migration.
- **Selected Approach**: Option 3. `spring.flyway.locations` of an environment may add e.g.
  `filesystem:/etc/hrs/db/migration`; those scripts use versions 900 and above and are not in this repository. A
  password change is a new script in that location with a new hash.
- **Rationale**: No code, no credentials in the repository, matches steering.
- **Trade-offs**: Local development logs in with test accounts (run `TestHrsApplication` /
  `./mvnw spring-boot:test-run`, which has the test migrations on the classpath).
- **Follow-up**: Deployment tooling is out of scope (roadmap); a self-service password change is a candidate later
  spec.

### Decision: Rooms have no foundation service
- **Context**: Rooms are reference data; `structure.md` allows other features to read through a read-only query in
  their own repository.
- **Selected Approach**: Foundation delivers only the schema and seed data for rooms. Management later owns writes
  (and may add a room service); readers query with generated jOOQ classes in their own repositories.
- **Rationale**: Simplification — no consumer in this spec needs a room API.

### Decision: Layout tolerates any authenticated principal
- **Context**: `testing.md` shows `@WithMockUser(roles = ...)` for view tests; that principal has no display name.
- **Selected Approach**: `StaffLayout` and `StaffHomeView` use `StaffPrincipal.displayName()` when the principal is
  a `StaffPrincipal` and fall back to the user name otherwise; the role label comes from the granted authorities.
- **Rationale**: Downstream specs keep using `@WithMockUser` without failing in the layout.

### Decision: Seed values and identities
- **Context**: Vision gives ten rooms but no numbers, types or rates.
- **Selected Approach**: Assumption A1 in requirements (Single/Double/Family; rooms 101–105, 201–205). Seeds insert
  without explicit ids (identity columns) and resolve `room_type_id` by room type name, so identity sequences stay
  consistent for management's later inserts.

### Generalization / Build vs. adopt / Simplification (synthesis)
- **Generalization**: "Navigation filtered by role" and "later specs add pages" are the same capability — a
  per-view role declaration that both access control and menu read. Adopted from Vaadin rather than built.
- **Build vs. adopt**: Adopt Spring Security + `VaadinSecurityConfigurer`, `LoginForm`, `AppLayout`/`SideNav`,
  `MenuConfiguration`, Vaadin's default not-found/denied view. Build only the jOOQ-backed `UserDetailsService`.
- **Simplification**: No custom access-denied view, no room service, no account-management code, no
  remember-me, one landing page for all roles.

## Risks & Mitigations
- Default target after a direct visit to `/login` is `/`, not the staff home — set the default target URL of the
  success handler to `/staff`; if that API is not available the public page links to `staff`, so the saved-request
  flow lands there (both covered by the Playwright journey).
- Test-only view (`ManagerOnlyTestView`) might not be discovered by route scanning in the test context — fallback:
  register it in the test with `RouteConfiguration.forApplicationScope().setRoute(...)`.
- Browserless cannot perform the real form POST login or logout — those are covered by the Playwright test.
- `{bcrypt}` hash in test data must match the documented test password — the service test verifies it with the
  application's `PasswordEncoder`.

## References
- Vaadin docs: Enabling Security — `VaadinSecurityConfigurer` usage.
- Vaadin docs: Menu Configuration — `MenuConfiguration.getMenuEntries()` filtered by access control.
- Spring Security reference: `DelegatingPasswordEncoder`, `@WithUserDetails`.
- Steering: `tech.md`, `structure.md`, `vaadin-jooq.md`, `testing.md`, `domain.md`.
