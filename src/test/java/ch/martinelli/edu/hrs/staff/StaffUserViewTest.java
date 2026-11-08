package ch.martinelli.edu.hrs.staff;

import ch.martinelli.edu.hrs.core.security.Role;
import ch.martinelli.edu.hrs.core.security.StaffUserDetailsService;
import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.StaffUserRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WithMockUser(roles = "MANAGER")
class StaffUserViewTest extends HrsBrowserlessTest {

    @Autowired
    private StaffUserService staffUserService;

    @Autowired
    private StaffUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createUser() {
        navigate(StaffUserView.class);

        test($(TextField.class).withLabel("Username").single()).setValue("test-fay");
        test($(Select.class).withLabel("Role").single()).selectItem("Receptionist");
        test($(PasswordField.class).withLabel("Password").single()).setValue("secret123");
        test($(Button.class).withText("Create").single()).click();

        var user = userDetailsService.loadUserByUsername("test-fay");
        assertThat(user.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_RECEPTIONIST");
        assertThat(rowOf("test-fay")).isNotNegative();
    }

    @Test
    void changeRole() {
        staffUserService.create("test-gus", Role.RECEPTIONIST, "secret123");
        navigate(StaffUserView.class);

        test(grid()).select(rowOf("test-gus"));
        test($(Select.class).withLabel("Role").single()).selectItem("Manager");
        test($(Button.class).withText("Save role").single()).click();

        assertThat(userDetailsService.loadUserByUsername("test-gus").getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_MANAGER");
    }

    @Test
    void resetPassword() {
        staffUserService.create("test-hal", Role.HOUSEKEEPING, "secret123");
        navigate(StaffUserView.class);

        test(grid()).select(rowOf("test-hal"));
        test($(PasswordField.class).withLabel("Password").single()).setValue("newSecret456");
        test($(Button.class).withText("Reset password").single()).click();

        var password = userDetailsService.loadUserByUsername("test-hal").getPassword();
        assertThat(passwordEncoder.matches("newSecret456", password)).isTrue();
    }

    @Test
    void deactivateUser() {
        staffUserService.create("test-ida", Role.HOUSEKEEPING, "secret123");
        navigate(StaffUserView.class);

        test(grid()).select(rowOf("test-ida"));
        test($(Button.class).withText("Deactivate").single()).click();

        assertThat(userDetailsService.loadUserByUsername("test-ida").isEnabled()).isFalse();
        assertThat(test(grid()).getCellText(rowOf("test-ida"), 2)).isEqualTo("No");
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistIsDenied() {
        assertThat(opens(StaffUserView.class)).isFalse();
    }

    @Test
    @WithMockUser(roles = "HOUSEKEEPING")
    void housekeepingIsDenied() {
        assertThat(opens(StaffUserView.class)).isFalse();
    }

    @SuppressWarnings("unchecked")
    private Grid<StaffUserRecord> grid() {
        return $(Grid.class).single();
    }

    private int rowOf(String username) {
        var grid = test(grid());
        for (int i = 0; i < grid.size(); i++) {
            if (grid.getRow(i).getUsername().equals(username)) {
                return i;
            }
        }
        return -1;
    }
}
