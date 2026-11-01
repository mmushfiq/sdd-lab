package ch.martinelli.edu.hrs.core.security;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static ch.martinelli.edu.hrs.db.Tables.STAFF_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class StaffUserDetailsServiceTest {

    @Autowired
    StaffUserDetailsService service;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    DSLContext ctx;

    // Scenario "One staff user per role after installation"
    @ParameterizedTest
    @CsvSource({"reception, ROLE_RECEPTIONIST", "manager, ROLE_MANAGER", "housekeeping, ROLE_HOUSEKEEPING"})
    void seeded_user_loads_with_one_role(String username, String authority) {
        StaffUser user = service.loadUserByUsername(username);

        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly(authority);
    }

    @Test
    void unknown_username_is_not_found() {
        assertThatThrownBy(() -> service.loadUserByUsername("nobody")).isInstanceOf(UsernameNotFoundException.class);
    }

    // Requirement "Passwords are not stored readable"; development password = username
    @ParameterizedTest
    @CsvSource({"reception", "manager", "housekeeping"})
    void stored_password_is_a_bcrypt_hash(String username) {
        String hash = ctx.select(STAFF_USER.PASSWORD_HASH).from(STAFF_USER)
                .where(STAFF_USER.USERNAME.eq(username)).fetchSingle(STAFF_USER.PASSWORD_HASH);

        assertThat(hash).startsWith("{bcrypt}");
        assertThat(hash.toLowerCase()).doesNotContain(username);
        assertThat(passwordEncoder.matches(username, hash)).isTrue();
    }
}
