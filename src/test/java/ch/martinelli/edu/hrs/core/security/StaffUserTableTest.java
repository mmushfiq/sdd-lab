package ch.martinelli.edu.hrs.core.security;

import org.jooq.DSLContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static ch.martinelli.edu.hrs.db.Tables.STAFF_USER;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Scenario "Staff user has one role": the database accepts only the three staff roles and one row per username.
 */
@SpringBootTest
@Transactional
class StaffUserTableTest {

    @Autowired
    DSLContext ctx;

    @Test
    void role_outside_the_staff_roles_is_rejected() {
        assertThatThrownBy(() -> insert("guest", "GUEST")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existing_username_is_rejected() {
        assertThatThrownBy(() -> insert("reception", "MANAGER")).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insert(String username, String role) {
        ctx.insertInto(STAFF_USER)
                .set(STAFF_USER.USERNAME, username)
                .set(STAFF_USER.PASSWORD_HASH, "{noop}x")
                .set(STAFF_USER.FULL_NAME, "Test User")
                .set(STAFF_USER.ROLE, role)
                .execute();
    }
}
