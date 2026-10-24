package ch.martinelli.edu.hrs.staff;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.STAFF_MEMBER;

/**
 * Reads staff members for the login (UC-022).
 */
@Repository
public class StaffMemberRepository {

    private final DSLContext ctx;

    public StaffMemberRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Finds the active staff member with this username; an inactive staff member is not found.
     */
    public Optional<StaffAccount> findActiveAccount(String username) {
        return ctx.select(STAFF_MEMBER.USERNAME, STAFF_MEMBER.PASSWORD_HASH,
                        STAFF_MEMBER.ROLE.convertFrom(StaffRole::valueOf))
                .from(STAFF_MEMBER)
                .where(STAFF_MEMBER.USERNAME.eq(username))
                .and(STAFF_MEMBER.ACTIVE.isTrue())
                .fetchOptional(Records.mapping(StaffAccount::new));
    }
}
