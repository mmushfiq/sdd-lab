package ch.martinelli.edu.hrs.reservation.ui;

import ch.martinelli.edu.hrs.reservation.AvailabilityService.NightRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricePerNightTest {

    @Test
    void one_night() {
        assertThat(ReservationView.pricePerNight(nights("140.00"))).isEqualTo("1 night × CHF 140.00");
    }

    @Test
    void same_rate_for_every_night() {
        assertThat(ReservationView.pricePerNight(nights("140.00", "140.00", "140.00")))
                .isEqualTo("3 nights × CHF 140.00");
    }

    @Test
    void consecutive_runs_in_date_order() {
        assertThat(ReservationView.pricePerNight(nights("140.00", "180.00", "180.00", "140.00")))
                .isEqualTo("1 night × CHF 140.00, 2 nights × CHF 180.00, 1 night × CHF 140.00");
    }

    private static List<NightRate> nights(String... rates) {
        List<NightRate> nights = new ArrayList<>();
        for (int i = 0; i < rates.length; i++) {
            nights.add(new NightRate(LocalDate.of(2030, 12, 18).plusDays(i), new BigDecimal(rates[i])));
        }
        return nights;
    }
}
