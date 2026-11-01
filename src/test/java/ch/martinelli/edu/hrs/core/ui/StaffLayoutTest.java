package ch.martinelli.edu.hrs.core.ui;

import ch.martinelli.edu.hrs.staff.ui.StaffStartView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.sidenav.SideNavItem;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithUserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StaffLayoutTest extends HrsBrowserlessTest {

    @Test
    @WithUserDetails("manager")
    void manager_sees_room_pages_in_navigation() {
        navigate(StaffStartView.class);

        assertThat(navLabels()).containsExactly("Start", "Room types", "Rooms");
        assertThat($(Span.class).id("layout-user-name").getText()).isEqualTo("Mark Manager");
        assertThat($(Button.class).id("layout-logout").getText()).isEqualTo("Log out");
    }

    @Test
    @WithUserDetails("reception")
    void receptionist_does_not_see_room_pages() {
        navigate(StaffStartView.class);

        assertThat(navLabels()).containsExactly("Start", "Reservations");
    }

    private List<String> navLabels() {
        return $(SideNavItem.class).all().stream().map(SideNavItem::getLabel).toList();
    }
}
