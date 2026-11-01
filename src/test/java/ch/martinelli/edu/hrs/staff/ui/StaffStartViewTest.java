package ch.martinelli.edu.hrs.staff.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.core.ui.LoginView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class StaffStartViewTest extends HrsBrowserlessTest {

    // Scenario "Start page shows the logged-in user"
    @Test
    @WithUserDetails("reception")
    void receptionist_sees_name_role_and_logout() {
        navigate(StaffStartView.class);

        assertThat($(Paragraph.class).id("staff-full-name").getText()).isEqualTo("Rita Reception");
        assertThat($(Paragraph.class).id("staff-role").getText()).isEqualTo("Receptionist");
        assertThat($(Button.class).id("logout").getText()).isEqualTo("Log out");
    }

    // Scenario "Anonymous visitor opens a staff page"
    @Test
    @WithAnonymousUser
    void anonymous_visitor_is_sent_to_login() {
        navigate(StaffStartView.ROUTE, LoginView.class);

        assertThat(getCurrentView()).isInstanceOf(LoginView.class);
    }
}
