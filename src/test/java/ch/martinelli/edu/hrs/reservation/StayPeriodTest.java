package ch.martinelli.edu.hrs.reservation;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class StayPeriodTest {

    private static final LocalDate DAY = LocalDate.of(2027, 3, 10);

    @Test
    void countsNightsBetweenArrivalAndDeparture() {
        assertThat(new StayPeriod(DAY, DAY.plusDays(1)).nights()).isEqualTo(1);
        assertThat(new StayPeriod(DAY, DAY.plusDays(3)).nights()).isEqualTo(3);
    }

    @Test
    void rejectsDepartureNotAfterArrival() {
        assertThatIllegalArgumentException().isThrownBy(() -> new StayPeriod(DAY, DAY));
        assertThatIllegalArgumentException().isThrownBy(() -> new StayPeriod(DAY, DAY.minusDays(1)));
    }
}
