package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.db.tables.records.SeasonalRateRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 003 US1: the Manager maintains seasonal rates per room type.
 */
@SpringBootTest
@Transactional
public class SeasonalRateServiceTest {

    private static final String SUMMER_OVERLAP = "Overlaps the seasonal rate \"Summer\" (1 July 2027 – 31 August 2027).";

    @Autowired
    private SeasonalRateService seasonalRateService;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private Clock clock;

    private long doubleId;
    private long singleId;

    @BeforeEach
    void createRoomTypes() {
        doubleId = roomTypeService.save(roomType("Test Season Double", 2, "100.00")).getId();
        singleId = roomTypeService.save(roomType("Test Season Single", 1, "80.00")).getId();
    }

    @Test
    void savedRatesAreListedByRoomTypeAndStartDate() {
        var christmas = seasonalRateService.save(rate(doubleId, "Christmas", "2027-12-20", "2028-01-02", "180"));
        var summer = seasonalRateService.save(rate(doubleId, "Summer", "2027-07-01", "2027-08-31", "150"));
        var single = seasonalRateService.save(rate(singleId, "Summer", "2027-07-01", "2027-08-31", "120"));

        assertThat(seasonalRateService.findAll())
                .filteredOn(r -> r.getRoomTypeId() == doubleId || r.getRoomTypeId() == singleId)
                .extracting(SeasonalRateRecord::getId)
                .containsExactly(summer.getId(), christmas.getId(), single.getId());
    }

    @Test
    void changeAndDelete() {
        var saved = seasonalRateService.save(rate(doubleId, "Summer", "2027-07-01", "2027-08-31", "150"));

        saved.setName("High summer");
        saved.setStartDate(LocalDate.parse("2027-07-15"));
        saved.setEndDate(LocalDate.parse("2027-08-15"));
        saved.setNightlyRate(new BigDecimal("170.00"));
        seasonalRateService.save(saved);

        assertThat(seasonalRateService.findAll())
                .filteredOn(r -> r.getId().equals(saved.getId()))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.getName()).isEqualTo("High summer");
                    assertThat(r.getStartDate()).isEqualTo("2027-07-15");
                    assertThat(r.getEndDate()).isEqualTo("2027-08-15");
                    assertThat(r.getNightlyRate()).isEqualByComparingTo("170.00");
                });

        seasonalRateService.delete(saved.getId());

        assertThat(seasonalRateService.findAll()).noneMatch(r -> r.getId().equals(saved.getId()));
    }

    @Test
    void invalidValuesAreRejected() {
        assertRejected(rate(doubleId, " ", "2027-07-01", "2027-08-31", "150"), "Name is required.");
        assertRejected(rate(doubleId, null, "2027-07-01", "2027-08-31", "150"), "Name is required.");
        assertRejected(rate(doubleId, "x".repeat(51), "2027-07-01", "2027-08-31", "150"),
                "Name can have at most 50 characters.");
        assertRejected(rate(doubleId, "Summer", null, "2027-08-31", "150"), "Start date and end date are required.");
        assertRejected(rate(doubleId, "Summer", "2027-07-01", null, "150"), "Start date and end date are required.");
        assertRejected(rate(doubleId, "Summer", "2027-07-01", "2027-06-30", "150"),
                "End date must not be before start date.");
        assertRejected(rate(doubleId, "Summer", "2027-07-01", "2027-08-31", "0"), "Nightly rate must be greater than 0.");
        assertRejected(rate(doubleId, "Summer", "2027-07-01", "2027-08-31", "-10"),
                "Nightly rate must be greater than 0.");
        assertRejected(rate(doubleId, "Summer", "2027-07-01", "2027-08-31", null),
                "Nightly rate must be greater than 0.");
        assertRejected(rate(null, "Summer", "2027-07-01", "2027-08-31", "150"), "Room type is required.");
    }

    @Test
    void fiftyCharacterNameAndOneNightPeriodAreAccepted() {
        var saved = seasonalRateService.save(rate(doubleId, "x".repeat(50), "2027-07-01", "2027-07-01", "150"));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void overlapWithAnotherRateOfTheSameRoomTypeIsRejected() {
        seasonalRateService.save(rate(doubleId, "Summer", "2027-07-01", "2027-08-31", "150"));

        assertRejected(rate(doubleId, "Late summer", "2027-08-31", "2027-09-15", "130"), SUMMER_OVERLAP);
        assertRejected(rate(doubleId, "Early summer", "2027-06-15", "2027-07-01", "130"), SUMMER_OVERLAP);
        assertRejected(rate(doubleId, "Inside", "2027-07-10", "2027-07-20", "130"), SUMMER_OVERLAP);
        assertRejected(rate(doubleId, "Around", "2027-06-01", "2027-09-30", "130"), SUMMER_OVERLAP);
    }

    @Test
    void adjacentPeriodsDoNotOverlap() {
        seasonalRateService.save(rate(doubleId, "Summer", "2027-07-01", "2027-08-31", "150"));

        seasonalRateService.save(rate(doubleId, "June", "2027-06-01", "2027-06-30", "120"));
        seasonalRateService.save(rate(doubleId, "September", "2027-09-01", "2027-09-30", "120"));

        assertThat(seasonalRateService.findAll()).filteredOn(r -> r.getRoomTypeId() == doubleId).hasSize(3);
    }

    @Test
    void samePeriodForAnotherRoomTypeIsAccepted() {
        seasonalRateService.save(rate(doubleId, "Summer", "2027-07-01", "2027-08-31", "150"));

        var single = seasonalRateService.save(rate(singleId, "Summer", "2027-07-01", "2027-08-31", "120"));

        assertThat(single.getId()).isNotNull();
    }

    @Test
    void changingARateWithinItsOwnPeriodIsAccepted() {
        var summer = seasonalRateService.save(rate(doubleId, "Summer", "2027-07-01", "2027-08-31", "150"));

        summer.setEndDate(LocalDate.parse("2027-08-20"));
        summer.setNightlyRate(new BigDecimal("160"));
        seasonalRateService.save(summer);

        assertThat(seasonalRateService.findAll())
                .filteredOn(r -> r.getId().equals(summer.getId()))
                .singleElement()
                .satisfies(r -> assertThat(r.getNightlyRate()).isEqualByComparingTo("160"));
    }

    @Test
    void periodInThePastIsAccepted() {
        var today = LocalDate.now(clock);

        var past = seasonalRateService.save(
                rate(doubleId, "Last year", today.minusYears(1).toString(), today.minusYears(1).plusDays(10).toString(),
                        "150"));

        assertThat(past.getId()).isNotNull();
    }

    @Test
    void deletingAnUnusedRoomTypeDeletesItsSeasonalRates() {
        var summer = seasonalRateService.save(rate(singleId, "Summer", "2027-07-01", "2027-08-31", "120"));

        roomTypeService.delete(singleId);

        assertThat(seasonalRateService.findAll()).noneMatch(r -> r.getId().equals(summer.getId()));
    }

    private void assertRejected(SeasonalRateRecord rate, String message) {
        assertThatThrownBy(() -> seasonalRateService.save(rate))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(message);
    }

    public static SeasonalRateRecord rate(Long roomTypeId, String name, String start, String end, String nightlyRate) {
        var rate = new SeasonalRateRecord();
        rate.setRoomTypeId(roomTypeId);
        rate.setName(name);
        rate.setStartDate(start == null ? null : LocalDate.parse(start));
        rate.setEndDate(end == null ? null : LocalDate.parse(end));
        rate.setNightlyRate(nightlyRate == null ? null : new BigDecimal(nightlyRate));
        return rate;
    }
}
