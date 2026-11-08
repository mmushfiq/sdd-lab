package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.db.tables.records.SeasonalRateRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static ch.martinelli.edu.hrs.core.BusinessRuleException.check;
import static ch.martinelli.edu.hrs.db.Sequences.SEASONAL_RATE_SEQ;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.db.Tables.SEASONAL_RATE;

/**
 * Seasonal rates of a room type (003 US1, FR-002, FR-003). Past dates are allowed; saved reservations keep their
 * night rates.
 */
@Service
@Transactional
public class SeasonalRateService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    private final DSLContext dsl;

    public SeasonalRateService(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional(readOnly = true)
    public List<SeasonalRateRecord> findAll() {
        return dsl.select(SEASONAL_RATE.fields()).from(SEASONAL_RATE)
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(SEASONAL_RATE.ROOM_TYPE_ID))
                .orderBy(ROOM_TYPE.NAME, SEASONAL_RATE.START_DATE)
                .fetchInto(SEASONAL_RATE);
    }

    public SeasonalRateRecord save(SeasonalRateRecord rate) {
        check(rate.getRoomTypeId() != null, "Room type is required.");
        check(rate.getName() != null && !rate.getName().isBlank(), "Name is required.");
        check(rate.getName().length() <= 50, "Name can have at most 50 characters.");
        check(rate.getStartDate() != null && rate.getEndDate() != null, "Start date and end date are required.");
        check(!rate.getEndDate().isBefore(rate.getStartDate()), "End date must not be before start date.");
        check(rate.getNightlyRate() != null && rate.getNightlyRate().signum() > 0,
                "Nightly rate must be greater than 0.");

        // Two Managers saving at once for the same room type wait for each other here (research R5)
        dsl.select(ROOM_TYPE.ID).from(ROOM_TYPE).where(ROOM_TYPE.ID.eq(rate.getRoomTypeId())).forUpdate().fetchSingle();
        var overlapped = dsl.selectFrom(SEASONAL_RATE)
                .where(SEASONAL_RATE.ROOM_TYPE_ID.eq(rate.getRoomTypeId()))
                .and(SEASONAL_RATE.START_DATE.le(rate.getEndDate()))
                .and(SEASONAL_RATE.END_DATE.ge(rate.getStartDate()))
                .and(rate.getId() == null ? SEASONAL_RATE.ID.isNotNull() : SEASONAL_RATE.ID.ne(rate.getId()))
                .orderBy(SEASONAL_RATE.START_DATE)
                .limit(1)
                .fetchOne();
        if (overlapped != null) {
            throw new BusinessRuleException("Overlaps the seasonal rate \"%s\" (%s – %s).".formatted(
                    overlapped.getName(), DATE.format(overlapped.getStartDate()), DATE.format(overlapped.getEndDate())));
        }

        if (rate.getId() == null) {
            rate.setId(dsl.nextval(SEASONAL_RATE_SEQ));
            dsl.executeInsert(rate);
        } else {
            dsl.executeUpdate(rate);
        }
        return rate;
    }

    public void delete(long id) {
        dsl.deleteFrom(SEASONAL_RATE).where(SEASONAL_RATE.ID.eq(id)).execute();
    }
}
