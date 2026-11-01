package ch.martinelli.edu.hrs.core.ui;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class StaffLoginIT extends PlaywrightIT {

    private String url(String path) {
        return "http://localhost:" + localServerPort + "/" + path;
    }

    private void logIn(String username, String password) {
        page.navigate(url("login"));
        page.getByLabel("Username").fill(username);
        page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();
    }

    // Scenarios "Successful login", "Logout", "Staff page after logout"
    @Test
    void log_in_log_out_and_staff_page_needs_login_again() {
        logIn("reception", "reception");
        page.waitForURL(url("staff"));
        assertThat(page.getByText("Rita Reception")).isVisible();
        assertThat(page.getByText("Receptionist")).isVisible();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log out")).click();
        page.waitForURL(url("login"));

        page.navigate(url("staff"));
        page.waitForURL(url("login"));
        assertThat(page.getByText("Rita Reception")).not().isVisible();
    }

    // Scenario "Wrong password"
    @Test
    void wrong_password_shows_generic_error() {
        logIn("reception", "wrong");

        assertThat(page.getByText("Invalid username or password")).isVisible();
    }

    // Scenario "Unknown username"
    @Test
    void unknown_username_shows_generic_error() {
        logIn("nobody", "reception");

        assertThat(page.getByText("Invalid username or password")).isVisible();
    }

    // Scenario "Empty username or password"
    @Test
    void empty_password_stays_on_login() {
        logIn("reception", "");

        assertThat(page).hasURL(url("login"));
        page.navigate(url("staff"));
        page.waitForURL(url("login"));
    }

    // Scenario "Empty username or password"
    @Test
    void empty_username_stays_on_login() {
        logIn("", "reception");

        assertThat(page).hasURL(url("login"));
        page.navigate(url("staff"));
        page.waitForURL(url("login"));
    }
}
