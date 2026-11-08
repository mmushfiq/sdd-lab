package ch.martinelli.edu.hrs.reservation;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 002 FR-002, FR-003, FR-005 and the edge cases (exclusive deadline, daylight saving).
 */
class CancellationDeadlineTest {

    private static final LocalDate ARRIVAL = LocalDate.of(2030, 3, 12);

    @Test
    void newRuleIs15HoursTwoDaysBeforeArrival() {
        assertThat(CancellationDeadline.of(ARRIVAL, 48).deadline()).isEqualTo(LocalDateTime.of(2030, 3, 10, 15, 0));
    }

    @Test
    void oldRuleIsMidnightOfTheArrivalDate() {
        assertThat(CancellationDeadline.of(ARRIVAL, 15).deadline()).isEqualTo(LocalDateTime.of(2030, 3, 12, 0, 0));
    }

    @Test
    void deadlineIsExclusive() {
        var deadline = CancellationDeadline.of(ARRIVAL, 48);

        assertThat(deadline.allowsOnlineCancellationAt(LocalDateTime.of(2030, 3, 10, 14, 59))).isTrue();
        assertThat(deadline.allowsOnlineCancellationAt(LocalDateTime.of(2030, 3, 10, 15, 0))).isFalse();
    }

    @Test
    void daylightSavingChangeKeepsTheWallClockTime() {
        // Europe/Zurich switches to summer time on 31 March 2030
        assertThat(CancellationDeadline.of(LocalDate.of(2030, 3, 31), 48).deadline())
                .isEqualTo(LocalDateTime.of(2030, 3, 29, 15, 0));
    }

    @Test
    void texts() {
        var deadline = CancellationDeadline.of(ARRIVAL, 48);

        assertThat(deadline.text()).isEqualTo("Free online cancellation until 10 March 2030, 15:00 (Zurich time)");
        assertThat(deadline.passedText()).isEqualTo("Online cancellation was possible until 10 March 2030, 15:00 "
                + "(Zurich time). Please call reception to cancel.");
    }
}
