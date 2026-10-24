package ch.martinelli.edu.hrs.security.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.housekeeping.ui.CleaningListView;
import ch.martinelli.edu.hrs.reception.ui.ArrivalsAndDeparturesView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-023 "Log Out". Test data: V103__uc022_test_data.sql; every staff member has the password
 * Secret-123.
 */
@WithAnonymousUser
class UC023LogOutTest extends HrsBrowserlessTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @UseCase(id = "UC-023")
    void staff_member_logs_out_and_the_session_can_no_longer_be_used() {
        logIn("rita");
        assertThat(getCurrentView()).isInstanceOf(ArrivalsAndDeparturesView.class);
        assertThat($(Span.class).withId("staff-member").single().getText()).isEqualTo("rita");

        // Step 1
        test($(Button.class).withText("Log out").single()).click();

        // Step 2: the session is ended
        assertNotLoggedIn();
        // Step 3: the staff login is displayed for every staff screen
        navigate("arrivals-and-departures", LoginView.class);
    }

    @Test
    @UseCase(id = "UC-023", businessRules = {"BR-001"})
    void going_back_after_logout_shows_the_staff_login() {
        logIn("hanna");
        assertThat(getCurrentView()).isInstanceOf(CleaningListView.class);

        test($(Button.class).withText("Log out").single()).click();

        // Going back in the browser navigates to the previous staff screen again
        navigate("cleaning-list", LoginView.class);
        assertThat($(Button.class).withText("Log out").exists()).isFalse();
    }

    @Test
    @UseCase(id = "UC-023", scenario = "A1: Session Already Ended")
    void ended_session_displays_the_staff_login() {
        logIn("rita");

        // The session ended, e.g. after 30 minutes without activity: the servlet container invalidates it
        VaadinSession.getCurrent().getSession().invalidate();
        SecurityContextHolder.clearContext();

        navigate("arrivals-and-departures", LoginView.class);
    }

    private void logIn(String username) {
        navigate(LoginView.class);
        test($(TextField.class).withCaption("Username").single()).setValue(username);
        test($(PasswordField.class).withCaption("Password").single()).setValue("Secret-123");
        test($(Button.class).withText("Log in").single()).click();
    }

    private static void assertNotLoggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication == null
                || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().startsWith("ROLE_")
                && !a.getAuthority().equals("ROLE_ANONYMOUS"))).isTrue();
    }
}
