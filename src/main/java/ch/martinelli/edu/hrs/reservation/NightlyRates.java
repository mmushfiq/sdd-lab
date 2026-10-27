package ch.martinelli.edu.hrs.reservation;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.db.Tables.SEASON;
import static ch.martinelli.edu.hrs.db.Tables.SEASON_RATE;

/**
 * The one pricing rule for every night of a stay, used by search, reservation, change and check-out.
 */
@Repository
public class NightlyRates {

    private final DSLContext ctx;

    public NightlyRates(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * The nightly rate of the room type for every night from {@code firstNight} up to the day before
     * {@code departure}, in date order.
     */
    public List<NightlyRate> of(Long roomTypeId, LocalDate firstNight, LocalDate departure) {
        return of(List.of(roomTypeId), firstNight, departure).getOrDefault(roomTypeId, List.of());
    }

    /**
     * The nightly rates of several room types for the nights from {@code firstNight} up to the day before
     * {@code departure}, by room type id.
     */
    public Map<Long, List<NightlyRate>> of(Collection<Long> roomTypeIds, LocalDate firstNight, LocalDate departure) {
        Map<Long, List<NightlyRate>> result = new HashMap<>();
        if (roomTypeIds.isEmpty() || !departure.isAfter(firstNight)) {
            return result;
        }
        LocalDate lastNight = departure.minusDays(1);
        Map<Long, BigDecimal> normalRates = ctx.select(ROOM_TYPE.ID, ROOM_TYPE.NIGHTLY_RATE)
                .from(ROOM_TYPE)
                .where(ROOM_TYPE.ID.in(roomTypeIds))
                .fetchMap(ROOM_TYPE.ID, ROOM_TYPE.NIGHTLY_RATE);
        List<SeasonRate> seasonRates = ctx.select(SEASON_RATE.ROOM_TYPE_ID, SEASON.FIRST_NIGHT, SEASON.LAST_NIGHT,
                        SEASON_RATE.NIGHTLY_RATE)
                .from(SEASON_RATE)
                .join(SEASON).on(SEASON.ID.eq(SEASON_RATE.SEASON_ID))
                .where(SEASON_RATE.ROOM_TYPE_ID.in(roomTypeIds))
                // UC-025 BR-002: A season covers its first night to its last night, both included.
                .and(SEASON.FIRST_NIGHT.le(lastNight))
                .and(SEASON.LAST_NIGHT.ge(firstNight))
                .fetch(r -> new SeasonRate(r.value1(), r.value2(), r.value3(), r.value4()));
        for (Map.Entry<Long, BigDecimal> normalRate : normalRates.entrySet()) {
            List<NightlyRate> rates = new ArrayList<>();
            for (LocalDate night = firstNight; night.isBefore(departure); night = night.plusDays(1)) {
                rates.add(new NightlyRate(night, rateOf(normalRate.getKey(), night, normalRate.getValue(),
                        seasonRates)));
            }
            result.put(normalRate.getKey(), rates);
        }
        return result;
    }

    // UC-025 BR-003: The season rate of the season covering the night, otherwise the normal rate (UC-016).
    private static BigDecimal rateOf(Long roomTypeId, LocalDate night, BigDecimal normalRate,
                                     List<SeasonRate> seasonRates) {
        for (SeasonRate seasonRate : seasonRates) {
            if (seasonRate.roomTypeId().equals(roomTypeId) && !night.isBefore(seasonRate.firstNight())
                    && !night.isAfter(seasonRate.lastNight())) {
                return seasonRate.rate();
            }
        }
        return normalRate;
    }

    /**
     * The sum of the nightly rates.
     */
    public static BigDecimal total(List<NightlyRate> rates) {
        return rates.stream().map(NightlyRate::rate).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record SeasonRate(Long roomTypeId, LocalDate firstNight, LocalDate lastNight, BigDecimal rate) {
    }
}
