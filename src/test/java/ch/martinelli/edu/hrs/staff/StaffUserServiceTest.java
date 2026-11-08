package ch.martinelli.edu.hrs.staff;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.core.security.Role;
import ch.martinelli.edu.hrs.core.security.StaffUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class StaffUserServiceTest {

    @Autowired
    private StaffUserService staffUserService;

    @Autowired
    private StaffUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createdUserCanLogInWithRole() {
        staffUserService.create("test-anna", Role.RECEPTIONIST, "secret123");

        var user = userDetailsService.loadUserByUsername("test-anna");

        assertThat(user.isEnabled()).isTrue();
        assertThat(passwordEncoder.matches("secret123", user.getPassword())).isTrue();
        assertThat(user.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_RECEPTIONIST");
    }

    @Test
    void duplicateUsernameIsRejected() {
        staffUserService.create("test-ben", Role.HOUSEKEEPING, "secret123");

        assertThatThrownBy(() -> staffUserService.create("test-ben", Role.MANAGER, "other123"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Username is already used.");
    }

    @Test
    void changeRole() {
        var created = staffUserService.create("test-cara", Role.RECEPTIONIST, "secret123");

        staffUserService.changeRole(created.getId(), Role.MANAGER);

        assertThat(userDetailsService.loadUserByUsername("test-cara").getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_MANAGER");
    }

    @Test
    void resetPassword() {
        var created = staffUserService.create("test-dan", Role.RECEPTIONIST, "secret123");

        staffUserService.resetPassword(created.getId(), "newSecret456");

        var password = userDetailsService.loadUserByUsername("test-dan").getPassword();
        assertThat(passwordEncoder.matches("newSecret456", password)).isTrue();
        assertThat(passwordEncoder.matches("secret123", password)).isFalse();
    }

    @Test
    void deactivatedUserCannotLogIn() {
        var created = staffUserService.create("test-eve", Role.HOUSEKEEPING, "secret123");

        staffUserService.deactivate(created.getId());

        assertThat(userDetailsService.loadUserByUsername("test-eve").isEnabled()).isFalse();
    }
}
