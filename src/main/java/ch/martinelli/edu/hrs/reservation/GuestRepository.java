package ch.martinelli.edu.hrs.reservation;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.GUEST;

/**
 * Stores the guests who reserve online (UC-001 step 9 and A7).
 */
@Repository
public class GuestRepository {

    private final DSLContext ctx;

    public GuestRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Finds a guest by email address. The email address must already be normalised to lower case.
     */
    public Optional<Long> findIdByEmail(String normalisedEmail) {
        return ctx.select(GUEST.ID)
                .from(GUEST)
                .where(GUEST.EMAIL.eq(normalisedEmail))
                .forUpdate()
                .fetchOptional(GUEST.ID);
    }

    public Long insert(String firstName, String lastName, String normalisedEmail, String phone) {
        return ctx.insertInto(GUEST)
                .set(GUEST.FIRST_NAME, firstName)
                .set(GUEST.LAST_NAME, lastName)
                .set(GUEST.EMAIL, normalisedEmail)
                .set(GUEST.PHONE, phone)
                .returning(GUEST.ID)
                .fetchSingle(GUEST.ID);
    }

    // UC-001 BR-006: The stored first name, last name and phone number are replaced with the entered values.
    public void updateNameAndPhone(Long guestId, String firstName, String lastName, String phone) {
        ctx.update(GUEST)
                .set(GUEST.FIRST_NAME, firstName)
                .set(GUEST.LAST_NAME, lastName)
                .set(GUEST.PHONE, phone)
                .where(GUEST.ID.eq(guestId))
                .execute();
    }
}
