package ch.martinelli.edu.hrs.core.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class AccessDeniedViewTest extends HrsBrowserlessTest {

    // Scenario "Role is not allowed"
    @Test
    @WithUserDetails("housekeeping")
    void housekeeping_cannot_open_manager_view() {
        navigate("test/manager-only", AccessDeniedView.class);

        assertThat($(H1.class).single().getText()).isEqualTo(AccessDeniedView.TEXT);
        assertThat($(Paragraph.class).withText(ManagerOnlyTestView.CONTENT).exists()).isFalse();
    }

    // Scenario "Role is allowed"
    @Test
    @WithUserDetails("manager")
    void manager_opens_manager_view() {
        navigate(ManagerOnlyTestView.class);

        assertThat($(Paragraph.class).withText(ManagerOnlyTestView.CONTENT).exists()).isTrue();
    }

    // Scenario "Guest opens a guest page"
    @Test
    @WithAnonymousUser
    void anonymous_visitor_opens_guest_page() {
        navigate(GuestTestView.class);

        assertThat($(Paragraph.class).withText(GuestTestView.CONTENT).exists()).isTrue();
    }
}
