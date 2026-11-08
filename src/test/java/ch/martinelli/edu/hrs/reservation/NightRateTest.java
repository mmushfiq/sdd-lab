package ch.martinelli.edu.hrs.reservation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 003 contracts/ui-changes.md: price per night text and total.
 */
class NightRateTest {

    @Test
    void consecutiveEqualRatesAreGrouped() {
        assertThat(NightRate.pricePerNight(nights("100.00", "150.00", "150.00")))
                .isEqualTo("1 night × 100.00, 2 nights × 150.00");
        assertThat(NightRate.pricePerNight(nights("120.00", "120.00", "120.00"))).isEqualTo("3 nights × 120.00");
    }

    @Test
    void nonConsecutiveEqualRatesStaySeparate() {
        assertThat(NightRate.pricePerNight(nights("150.00", "100.00", "150.00")))
                .isEqualTo("1 night × 150.00, 1 night × 100.00, 1 night × 150.00");
    }

    @Test
    void totalSumsTheRates() {
        assertThat(NightRate.total(nights("100.00", "150.00", "150.00"))).isEqualByComparingTo("400.00");
        assertThat(NightRate.total(List.of())).isEqualByComparingTo("0");
    }

    private static List<NightRate> nights(String... rates) {
        var nights = new ArrayList<NightRate>();
        var night = LocalDate.of(2027, 6, 30);
        for (var rate : rates) {
            nights.add(new NightRate(night, new BigDecimal(rate)));
            night = night.plusDays(1);
        }
        return nights;
    }
}
