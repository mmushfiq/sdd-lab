package ch.martinelli.edu.hrs.management;

import ch.martinelli.edu.hrs.reservation.GuestRepository;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.GUEST;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;

/**
 * Reads a guest with their reservations and replaces their personal data (UC-024 steps 3 and 8).
 */
@Repository
public class GuestErasureRepository {

    static final String ERASED_FIRST_NAME = "Erased";
    static final String ERASED_LAST_NAME = "Guest";
    static final String ERASED_PHONE = "Erased";

    private final DSLContext ctx;

    public GuestErasureRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * The guest with this email address; the address must already be normalised to lower case.
     */
    public Optional<ErasableGuest> findByEmail(String normalisedEmail) {
        return ctx.select(GUEST.ID, GUEST.FIRST_NAME, GUEST.LAST_NAME, GUEST.EMAIL, GUEST.PHONE)
                .from(GUEST)
                .where(GUEST.EMAIL.eq(normalisedEmail))
                .fetchOptional(Records.mapping(ErasableGuest::new));
    }

    /**
     * The guest, locked until the end of the transaction; reservations for an existing guest lock the same row
     * (UC-001, UC-004), so no reservation is added while the guest is erased.
     */
    public Optional<ErasableGuest> findForUpdate(Long guestId) {
        return ctx.select(GUEST.ID, GUEST.FIRST_NAME, GUEST.LAST_NAME, GUEST.EMAIL, GUEST.PHONE)
                .from(GUEST)
                .where(GUEST.ID.eq(guestId))
                .forUpdate()
                .fetchOptional(Records.mapping(ErasableGuest::new));
    }

    /**
     * All reservations of the guest, by arrival date.
     */
    public List<GuestReservation> findReservations(Long guestId) {
        return ctx.select(RESERVATION.RESERVATION_CODE, RESERVATION.ARRIVAL_DATE, RESERVATION.DEPARTURE_DATE,
                        RESERVATION.STATUS.convertFrom(ReservationStatus::valueOf))
                .from(RESERVATION)
                .where(RESERVATION.GUEST_ID.eq(guestId))
                .orderBy(RESERVATION.ARRIVAL_DATE, RESERVATION.RESERVATION_CODE)
                .fetch(Records.mapping(GuestReservation::new));
    }

    // UC-024 BR-001: Exactly first name, last name, email address and phone number are replaced; the email address
    // is unique, has the format of an email address and cannot receive email. The reservations are kept.
    public void erase(Long guestId) {
        ctx.update(GUEST)
                .set(GUEST.FIRST_NAME, ERASED_FIRST_NAME)
                .set(GUEST.LAST_NAME, ERASED_LAST_NAME)
                .set(GUEST.EMAIL, erasedEmail(guestId))
                .set(GUEST.PHONE, ERASED_PHONE)
                .where(GUEST.ID.eq(guestId))
                .execute();
    }

    static String erasedEmail(Long guestId) {
        return "guest-" + guestId + "@" + GuestRepository.ERASED_EMAIL_DOMAIN;
    }
}
