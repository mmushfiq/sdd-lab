package ch.martinelli.edu.hrs.reservation;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.GUEST;

/**
 * Stores the guests who reserve online (UC-001 step 9 and A7) or at the reception (UC-004 steps 6 and 10).
 */
@Repository
public class GuestRepository {

    /**
     * Domain of the replacement email addresses of erased guests (UC-024 BR-001). The domain .invalid can never
     * receive email.
     */
    public static final String ERASED_EMAIL_DOMAIN = "erased.invalid";

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

    /**
     * Finds the stored details of the guest with this email address, without locking (UC-004 step 6). The email
     * address must already be normalised to lower case.
     */
    // UC-004 BR-005: A guest is identified by their email address, compared case-insensitively (stored in lower case).
    public Optional<GuestDetails> findDetailsByEmail(String normalisedEmail) {
        return ctx.select(GUEST.FIRST_NAME, GUEST.LAST_NAME, GUEST.EMAIL, GUEST.PHONE)
                .from(GUEST)
                .where(GUEST.EMAIL.eq(normalisedEmail))
                .fetchOptional(Records.mapping(GuestDetails::new));
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
