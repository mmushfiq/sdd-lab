package ch.martinelli.edu.hrs.reservation;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ch.martinelli.edu.hrs.core.BusinessRuleException.check;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.db.Tables.SEASONAL_RATE;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    static final int MAX_NIGHTS = 30;

    private final DSLContext dsl;
    private final Clock clock;

    public AvailabilityService(DSLContext dsl, Clock clock) {
        this.dsl = dsl;
        this.clock = clock;
    }

    /**
     * Rooms that fit the guests and have no booked or checked-in reservation for any night of the stay (FR-006).
     * The total is the sum of the stay's night rates (003 FR-005); seasonal rates are read once for all rooms.
     */
    public List<AvailableRoom> search(LocalDate arrival, LocalDate departure, int numberOfGuests) {
        checkStay(arrival, departure, numberOfGuests);
        var seasons = seasonalRates(SEASONAL_RATE.ROOM_TYPE_ID.isNotNull(), arrival, departure).stream()
                .collect(Collectors.groupingBy(SeasonalRate::roomTypeId));
        // NOT IN instead of NOT EXISTS: H2 answered EXISTS on varchar conditions wrongly (see RoomTypeService)
        return dsl.select(ROOM.ID, ROOM.ROOM_NUMBER, ROOM_TYPE.NAME, ROOM_TYPE.CAPACITY, ROOM_TYPE.NIGHTLY_RATE,
                        ROOM_TYPE.ID)
                .from(ROOM)
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(ROOM.ROOM_TYPE_ID))
                .where(ROOM_TYPE.CAPACITY.ge(numberOfGuests))
                .and(ROOM.ID.notIn(dsl.select(RESERVATION.ROOM_ID).from(RESERVATION)
                        .where(blocks(arrival, departure))))
                .orderBy(ROOM.ROOM_NUMBER)
                .fetch(r -> new AvailableRoom(r.value1(), r.value2(), r.value3(), r.value4(), NightRate.total(
                        nightRates(r.value5(), seasons.getOrDefault(r.value6(), List.of()), arrival, departure))));
    }

    /**
     * 003 FR-004: one rate per night from arrival to the day before departure, in date order: the seasonal rate
     * covering the night, otherwise the room type's standard nightly rate.
     */
    public List<NightRate> nightRates(long roomTypeId, LocalDate arrival, LocalDate departure) {
        var standard = dsl.select(ROOM_TYPE.NIGHTLY_RATE).from(ROOM_TYPE).where(ROOM_TYPE.ID.eq(roomTypeId))
                .fetchSingle().value1();
        return nightRates(standard, seasonalRates(SEASONAL_RATE.ROOM_TYPE_ID.eq(roomTypeId), arrival, departure),
                arrival, departure);
    }

    private static List<NightRate> nightRates(BigDecimal standard, List<SeasonalRate> seasons,
                                              LocalDate arrival, LocalDate departure) {
        var nights = new ArrayList<NightRate>();
        for (var night = arrival; night.isBefore(departure); night = night.plusDays(1)) {
            var day = night;
            var rate = seasons.stream().filter(s -> !day.isBefore(s.start()) && !day.isAfter(s.end()))
                    .map(SeasonalRate::rate).findFirst().orElse(standard);
            nights.add(new NightRate(night, rate));
        }
        return nights;
    }

    private List<SeasonalRate> seasonalRates(Condition roomTypes, LocalDate arrival, LocalDate departure) {
        return dsl.select(SEASONAL_RATE.ROOM_TYPE_ID, SEASONAL_RATE.START_DATE, SEASONAL_RATE.END_DATE,
                        SEASONAL_RATE.NIGHTLY_RATE)
                .from(SEASONAL_RATE)
                .where(roomTypes)
                .and(SEASONAL_RATE.START_DATE.lt(departure))
                .and(SEASONAL_RATE.END_DATE.ge(arrival))
                .fetch(r -> new SeasonalRate(r.value1(), r.value2(), r.value3(), r.value4()));
    }

    private record SeasonalRate(long roomTypeId, LocalDate start, LocalDate end, BigDecimal rate) {
    }

    /**
     * FR-008: departure after arrival, arrival not in the past and at most one year ahead, at most 30 nights.
     */
    public void checkStay(LocalDate arrival, LocalDate departure, int numberOfGuests) {
        check(arrival != null && departure != null, "Arrival and departure are required.");
        check(numberOfGuests >= 1, "Number of guests must be at least 1.");
        check(departure.isAfter(arrival), "Departure must be after arrival.");
        var today = LocalDate.now(clock);
        check(!arrival.isBefore(today), "Arrival cannot be in the past.");
        check(!arrival.isAfter(today.plusYears(1)), "Arrival can be at most one year ahead.");
        check(nights(arrival, departure) <= MAX_NIGHTS, "A stay can be at most 30 nights.");
    }

    /**
     * Booked or checked-in reservations that share at least one night with the stay; the departure day is not a night.
     */
    static Condition blocks(LocalDate arrival, LocalDate departure) {
        return RESERVATION.STATUS.in(ReservationStatus.BOOKED.name(), ReservationStatus.CHECKED_IN.name())
                .and(RESERVATION.ARRIVAL_DATE.lt(departure))
                .and(RESERVATION.DEPARTURE_DATE.gt(arrival));
    }

    static long nights(LocalDate arrival, LocalDate departure) {
        return ChronoUnit.DAYS.between(arrival, departure);
    }
}
