# Testing Standards

What to test, where tests live, and how they get their data. The build commands are in `tech.md`; framework usage
inside views and services is in `vaadin-jooq.md`.

## Philosophy
- Test behavior through the UI a user would use, against the real database. No mocks of our own services or
  repositories.
- Every acceptance criterion in a spec's `requirements.md` has at least one test that would fail without the code
  that satisfies it, including the error and alternative cases, not just the happy path.
- The business invariants in `product.md` (no double bookings, clean before check-in, immediate cleaning list) get
  explicit tests in every feature that can break them.
- A test that fails because the code disagrees with the spec stays failing until the code is fixed; never change the
  expected value to match the code.

## Test Types

| Type | Tool | Base class | Name | Runs with | Scope |
|------|------|------------|------|-----------|-------|
| View test | Vaadin Browserless | `core.ui.HrsBrowserlessTest` | `<View>Test` | `./mvnw test` | Every view: navigation, input, validation, grid content, notifications, role restrictions |
| Service test | JUnit + Spring context | `core.ui.HrsBrowserlessTest` (one shared context, test clock) | `<Service>Test` | `./mvnw test` | Rules that are hard to reach through a view (concurrency, scheduled jobs, edge dates) |
| Browser test | Playwright + Mopo | `core.ui.PlaywrightIT` | `<Feature>IT` | `./mvnw verify` | One main journey per feature in a real browser |

- Browserless tests are the default: fast, server-side, with the full Spring context. Most acceptance criteria are
  covered here.
- Playwright tests are few and slow: one per feature for its main success path, plus checks that only a browser can
  do (layout on tablet width, real login and logout).
- Tests live in `src/test/java` in the same package as the code under test (`reception/ui/CheckInViewTest`).

## Structure

Arrange, act, assert; one behavior per test method; the method name says the expected behavior.

```java
class CheckInViewTest extends HrsBrowserlessTest {

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void refusesCheckInWhenRoomIsNotClean() {
        navigate(CheckInView.class);                                // arrange: test data has room 102 dirty
        $(ComboBox.class).id("room").selectItem("102");
        test($(Button.class).id("check-in")).click();               // act

        assertThat($(Notification.class).last().getText())          // assert
                .contains("not clean");
    }
}
```
_Class, id and role names are examples; the real ones come from the specs' designs._

- Find components by the kebab-case ids the views set (see `vaadin-jooq.md`), not by position or caption text.
- Assert on what the user sees (grid rows, field errors, notification text) and, where the spec demands it, on the
  resulting database state through a repository or `DSLContext`.
- Playwright: open `http://localhost:` + `localServerPort` + route, then let Mopo wait for the Vaadin round trip
  (`mopo.waitForConnectionToSettle()`) before asserting.

## Test Data

- Tests use the same Flyway schema as production, on the embedded H2 test database.
- Test-only data is inserted by test migrations in `src/test/resources/db/migration`, one file per spec:
  `V<n>__<feature>_test_data.sql`. Main migrations use versions below 100, test migrations 100 and above, so the two
  never collide.
- Each spec gets its own ID range (for example 1000–1999 for the first spec with test data, 2000–2999 for the next)
  and its own dates, so tests of different features don't see each other's rows.
- A later main migration that adds a `NOT NULL` column must give existing rows a value (default or back-fill),
  because test data is already present when it runs.
- Tests that change shared rows restore them in `@AfterEach`, or create the rows they change inside the test.
- Use plain, obviously fake personal data (`Test Guest`, `guest1@example.com`): the GDPR rule in `product.md` applies
  to test data too.

## Time

- "Today" in tests is fixed at 2026-06-01: the test base classes provide one `ch.martinelli.edu.hrs.core.TestClock`
  bean (foundation), and code reads time only from the injected `Clock`.
- A test that needs another "today" (deadlines, overdue stays) calls `testClock.setToday(...)`; the base classes reset
  it after each test. Don't add your own clock beans: that would start an extra Spring context.
- Pick test dates relative to that fixed day, never relative to the real current date.

## Security in Tests

- Staff views are tested with the role that may use them (`@WithMockUser(roles = ...)`) and at least once with a role
  or anonymous user that must be refused.
- Login and logout themselves are tested through the real login view, in Browserless and once in Playwright.
- Test user passwords live only in test migrations, never in main migrations.

## Traceability

- Each test class or method names the requirement it checks from the spec's `requirements.md`, in a short comment or
  `@DisplayName`: `// guest-booking 2.3`.
- When a requirement has no test, say so in the task's review rather than leaving it silent.

## Done Means
- `./mvnw verify` passes: all Browserless, service and Playwright tests green.
- The new tests ran (check the count in `target/surefire-reports` and `target/failsafe-reports`), and none were
  skipped or disabled to get there.

---
_Focus on patterns and decisions. Tool-specific config lives elsewhere._
