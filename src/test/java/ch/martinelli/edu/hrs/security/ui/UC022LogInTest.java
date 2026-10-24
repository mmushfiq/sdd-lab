package ch.martinelli.edu.hrs.security.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.housekeeping.ui.CleaningListView;
import ch.martinelli.edu.hrs.reception.ui.ArrivalsAndDeparturesView;
import ch.martinelli.edu.hrs.report.ui.OccupancyReportView;
import ch.martinelli.edu.hrs.reservation.ui.ReserveRoomOnlineView;
import ch.martinelli.edu.hrs.reservation.ui.ViewOwnReservationView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.server.VaadinSession;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.STAFF_MEMBER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-022 "Log In". Test data: V103__uc022_test_data.sql; every staff member has the password
 * Secret-123.
 */
@WithAnonymousUser
@ExtendWith(OutputCaptureExtension.class)
class UC022LogInTest extends HrsBrowserlessTest {

    private static final String PASSWORD = "Secret-123";

    @Autowired
    DSLContext ctx;

    @Autowired
    Environment environment;

    @AfterEach
    void logOut() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-022", businessRules = {"BR-003"})
    void receptionist_logs_in_and_sees_arrivals_and_departures() {
        String sessionIdBefore = VaadinSession.getCurrent().getSession().getId();

        logIn("rita", PASSWORD);

        assertThat(getCurrentView()).isInstanceOf(ArrivalsAndDeparturesView.class);
        assertThat(roles()).containsExactly("ROLE_RECEPTIONIST");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("rita");
        // The session gets a new ID on login
        assertThat(VaadinSession.getCurrent().getSession().getId()).isNotEqualTo(sessionIdBefore);
    }

    @Test
    @UseCase(id = "UC-022", businessRules = {"BR-003"})
    void housekeeper_logs_in_and_sees_the_cleaning_list() {
        logIn("hanna", PASSWORD);

        assertThat(getCurrentView()).isInstanceOf(CleaningListView.class);
        assertThat(roles()).containsExactly("ROLE_HOUSEKEEPER");
    }

    @Test
    @UseCase(id = "UC-022", businessRules = {"BR-003"})
    void manager_logs_in_and_sees_the_occupancy_report() {
        logIn("mark", PASSWORD);

        assertThat(getCurrentView()).isInstanceOf(OccupancyReportView.class);
        assertThat(roles()).containsExactly("ROLE_MANAGER");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Input Missing
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-022", scenario = "A1: Input Missing")
    void missing_username_and_password_are_marked() {
        logIn("", "");

        TextField username = $(TextField.class).withCaption("Username").single();
        PasswordField password = $(PasswordField.class).withCaption("Password").single();
        assertThat(username.isInvalid()).isTrue();
        assertThat(username.getErrorMessage()).isEqualTo("Enter your username");
        assertThat(password.isInvalid()).isTrue();
        assertThat(password.getErrorMessage()).isEqualTo("Enter your password");
        assertThat(getCurrentView()).isInstanceOf(LoginView.class);
        assertNotLoggedIn();
    }

    @Test
    @UseCase(id = "UC-022", scenario = "A1: Input Missing")
    void missing_password_is_marked_and_staff_member_can_continue() {
        logIn("rita", "");

        assertThat($(PasswordField.class).withCaption("Password").single().isInvalid()).isTrue();
        assertNotLoggedIn();

        // Use case continues at step 2
        logIn("rita", PASSWORD);
        assertThat(getCurrentView()).isInstanceOf(ArrivalsAndDeparturesView.class);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Login Refused
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-022", scenario = "A2: Login Refused", businessRules = {"BR-001"})
    void unknown_username_wrong_password_and_inactive_account_give_the_same_answer() {
        logIn("nobody", PASSWORD);
        String unknownUsername = errorMessage();
        assertNotLoggedIn();

        logIn("rita", "wrong-password");
        String wrongPassword = errorMessage();
        assertNotLoggedIn();

        logIn("ina", PASSWORD);
        String inactive = errorMessage();
        assertNotLoggedIn();

        assertThat(unknownUsername).isEqualTo("The username or password is wrong.");
        assertThat(wrongPassword).isEqualTo(unknownUsername);
        assertThat(inactive).isEqualTo(unknownUsername);
        assertThat(getCurrentView()).isInstanceOf(LoginView.class);
    }

    @Test
    @UseCase(id = "UC-022", scenario = "A2: Login Refused")
    void staff_member_can_log_in_after_a_refused_login() {
        logIn("rita", "wrong-password");
        assertThat(errorMessage()).isEqualTo("The username or password is wrong.");

        logIn("rita", PASSWORD);

        assertThat(getCurrentView()).isInstanceOf(ArrivalsAndDeparturesView.class);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Business rules
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-022", businessRules = {"BR-002"})
    void session_ends_after_30_minutes_without_activity() {
        assertThat(environment.getProperty("server.servlet.session.timeout")).isEqualTo("30m");
        // The heartbeat of an open browser tab must not keep the session alive
        assertThat(environment.getProperty("vaadin.closeIdleSessions")).isEqualTo("true");
    }

    @Test
    @UseCase(id = "UC-022", businessRules = {"BR-003"})
    void without_login_staff_screens_lead_to_the_login() {
        navigate("arrivals-and-departures", LoginView.class);
        navigate("cleaning-list", LoginView.class);
        navigate("occupancy-report", LoginView.class);
    }

    @Test
    @UseCase(id = "UC-022", businessRules = {"BR-003"})
    void guest_functions_stay_reachable_without_login() {
        assertThat(navigate(ReserveRoomOnlineView.class)).isInstanceOf(ReserveRoomOnlineView.class);
        assertThat(navigate(ViewOwnReservationView.class)).isInstanceOf(ViewOwnReservationView.class);
    }

    @Test
    @UseCase(id = "UC-022", businessRules = {"BR-003"})
    void staff_member_reaches_only_the_functions_of_their_role() {
        logIn("hanna", PASSWORD);

        assertThat(navigate(CleaningListView.class)).isInstanceOf(CleaningListView.class);
        assertAccessDenied("arrivals-and-departures");
        assertAccessDenied("occupancy-report");
    }

    @Test
    @UseCase(id = "UC-022", businessRules = {"BR-004"})
    void password_is_never_shown_logged_or_stored_readably(CapturedOutput output) {
        navigate(LoginView.class);
        PasswordField password = $(PasswordField.class).withCaption("Password").single();
        assertThat(password.isRevealButtonVisible()).isFalse();

        logIn("rita", "wrong-password");
        logIn("rita", PASSWORD);

        assertThat(output.getAll()).doesNotContain(PASSWORD, "wrong-password");
        // Stored only as bcrypt hashes with cost factor 10
        assertThat(ctx.select(STAFF_MEMBER.PASSWORD_HASH).from(STAFF_MEMBER).fetch(STAFF_MEMBER.PASSWORD_HASH))
                .allSatisfy(hash -> assertThat(hash).startsWith("$2a$10$"));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    private void logIn(String username, String password) {
        if (!(getCurrentView() instanceof LoginView)) {
            navigate(LoginView.class);
        }
        test($(TextField.class).withCaption("Username").single()).setValue(username);
        test($(PasswordField.class).withCaption("Password").single()).setValue(password);
        test($(Button.class).withText("Log in").single()).click();
    }

    private String errorMessage() {
        return $(Paragraph.class).withId("error-message").single().getText();
    }

    private void assertAccessDenied(String route) {
        RouteNotFoundError error = navigate(route, RouteNotFoundError.class);
        assertThat(error).isNotNull();
    }

    private static void assertNotLoggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().startsWith("ROLE_")
                && !a.getAuthority().equals("ROLE_ANONYMOUS"))).isTrue();
    }

    private static List<String> roles() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                // Spring Security also grants factor authorities such as FACTOR_PASSWORD
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();
    }
}
