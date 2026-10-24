package ch.martinelli.edu.hrs.staff;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.STAFF_MEMBER;
import static org.jooq.impl.DSL.lower;
import static org.jooq.impl.DSL.noCondition;

/**
 * Reads staff members for the login (UC-022) and records the staff members the manager maintains (UC-021).
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

    /**
     * All staff members by username (UC-021 step 2).
     */
    public List<StaffMemberOverview> findAll() {
        return ctx.select(STAFF_MEMBER.ID, STAFF_MEMBER.USERNAME, STAFF_MEMBER.FIRST_NAME, STAFF_MEMBER.LAST_NAME,
                        STAFF_MEMBER.ROLE.convertFrom(StaffRole::valueOf), STAFF_MEMBER.ACTIVE)
                .from(STAFF_MEMBER)
                .orderBy(lower(STAFF_MEMBER.USERNAME))
                .fetch(Records.mapping(StaffMemberOverview::new));
    }

    /**
     * The staff member, locked until the end of the transaction.
     */
    public Optional<StaffMemberOverview> findForUpdate(Long id) {
        return ctx.select(STAFF_MEMBER.ID, STAFF_MEMBER.USERNAME, STAFF_MEMBER.FIRST_NAME, STAFF_MEMBER.LAST_NAME,
                        STAFF_MEMBER.ROLE.convertFrom(StaffRole::valueOf), STAFF_MEMBER.ACTIVE)
                .from(STAFF_MEMBER)
                .where(STAFF_MEMBER.ID.eq(id))
                .forUpdate()
                .fetchOptional(Records.mapping(StaffMemberOverview::new));
    }

    // UC-021 BR-001: Usernames are unique, compared case-insensitively.
    public boolean existsOtherWithUsername(String username, Long staffMemberId) {
        return ctx.fetchExists(STAFF_MEMBER, lower(STAFF_MEMBER.USERNAME).eq(lower(username))
                .and(staffMemberId == null ? noCondition() : STAFF_MEMBER.ID.ne(staffMemberId)));
    }

    public Long insert(StaffMemberData data, String passwordHash) {
        return ctx.insertInto(STAFF_MEMBER)
                .set(STAFF_MEMBER.USERNAME, data.username())
                .set(STAFF_MEMBER.PASSWORD_HASH, passwordHash)
                .set(STAFF_MEMBER.FIRST_NAME, data.firstName())
                .set(STAFF_MEMBER.LAST_NAME, data.lastName())
                .set(STAFF_MEMBER.ROLE, data.role().name())
                .set(STAFF_MEMBER.ACTIVE, data.active())
                .returning(STAFF_MEMBER.ID)
                .fetchSingle(STAFF_MEMBER.ID);
    }

    /**
     * Changes the staff member; a null {@code passwordHash} keeps the current password.
     */
    public void update(Long id, StaffMemberData data, String passwordHash) {
        var update = ctx.update(STAFF_MEMBER)
                .set(STAFF_MEMBER.USERNAME, data.username())
                .set(STAFF_MEMBER.FIRST_NAME, data.firstName())
                .set(STAFF_MEMBER.LAST_NAME, data.lastName())
                .set(STAFF_MEMBER.ROLE, data.role().name())
                .set(STAFF_MEMBER.ACTIVE, data.active());
        if (passwordHash != null) {
            update = update.set(STAFF_MEMBER.PASSWORD_HASH, passwordHash);
        }
        update.where(STAFF_MEMBER.ID.eq(id)).execute();
    }
}
