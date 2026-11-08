package ch.martinelli.edu.hrs.core.security;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import static ch.martinelli.edu.hrs.db.Sequences.STAFF_USER_SEQ;
import static ch.martinelli.edu.hrs.db.Tables.STAFF_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class StaffUserDetailsServiceTest {

    @Autowired
    private StaffUserDetailsService userDetailsService;

    @Autowired
    private DSLContext dsl;

    @Test
    void activeUserLoadsWithRole() {
        insertUser("test-anna", "RECEPTIONIST", true);

        var user = userDetailsService.loadUserByUsername("test-anna");

        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_RECEPTIONIST");
    }

    @Test
    void inactiveUserIsDisabled() {
        insertUser("test-ben", "HOUSEKEEPING", false);

        assertThat(userDetailsService.loadUserByUsername("test-ben").isEnabled()).isFalse();
    }

    @Test
    void unknownUserThrows() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("test-nobody"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private void insertUser(String username, String role, boolean active) {
        dsl.insertInto(STAFF_USER)
                .set(STAFF_USER.ID, dsl.nextval(STAFF_USER_SEQ))
                .set(STAFF_USER.USERNAME, username)
                .set(STAFF_USER.PASSWORD_HASH, "{noop}secret")
                .set(STAFF_USER.ROLE, role)
                .set(STAFF_USER.ACTIVE, active)
                .execute();
    }
}
