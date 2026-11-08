package ch.martinelli.edu.hrs.staff;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.core.security.Role;
import ch.martinelli.edu.hrs.db.tables.records.StaffUserRecord;
import org.jooq.DSLContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ch.martinelli.edu.hrs.core.BusinessRuleException.check;
import static ch.martinelli.edu.hrs.db.Sequences.STAFF_USER_SEQ;
import static ch.martinelli.edu.hrs.db.Tables.STAFF_USER;

@Service
@Transactional
public class StaffUserService {

    private final DSLContext dsl;
    private final PasswordEncoder passwordEncoder;

    public StaffUserService(DSLContext dsl, PasswordEncoder passwordEncoder) {
        this.dsl = dsl;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<StaffUserRecord> findAll() {
        return dsl.selectFrom(STAFF_USER).orderBy(STAFF_USER.USERNAME).fetch();
    }

    public StaffUserRecord create(String username, Role role, String password) {
        check(username != null && !username.isBlank(), "Username is required.");
        check(role != null, "Role is required.");
        checkPassword(password);
        if (dsl.fetchCount(STAFF_USER, STAFF_USER.USERNAME.eq(username)) > 0) {
            throw new BusinessRuleException("Username is already used.");
        }
        var user = dsl.newRecord(STAFF_USER);
        user.setId(dsl.nextval(STAFF_USER_SEQ));
        user.setUsername(username);
        user.setRole(role.name());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setActive(true);
        user.insert();
        return user;
    }

    public void changeRole(long id, Role role) {
        check(role != null, "Role is required.");
        dsl.update(STAFF_USER).set(STAFF_USER.ROLE, role.name()).where(STAFF_USER.ID.eq(id)).execute();
    }

    public void resetPassword(long id, String password) {
        checkPassword(password);
        dsl.update(STAFF_USER)
                .set(STAFF_USER.PASSWORD_HASH, passwordEncoder.encode(password))
                .where(STAFF_USER.ID.eq(id))
                .execute();
    }

    public void deactivate(long id) {
        dsl.update(STAFF_USER).set(STAFF_USER.ACTIVE, false).where(STAFF_USER.ID.eq(id)).execute();
    }

    private static void checkPassword(String password) {
        check(password != null && !password.isBlank(), "Password is required.");
    }
}
