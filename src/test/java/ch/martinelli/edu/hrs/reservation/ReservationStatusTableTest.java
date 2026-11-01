package ch.martinelli.edu.hrs.reservation;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static ch.martinelli.edu.hrs.reservation.ReservationTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Requirement "Reservation statuses after check-in" at database level.
 */
@SpringBootTest
@Transactional
class ReservationStatusTableTest {

    @Autowired
    DSLContext ctx;

    @Test
    void checked_in_and_checked_out_are_stored() {
        assertThat(insert(ctx, "CHECKIN1", "104", MARCH_10, MARCH_12, "Anna", "CHECKED_IN").getStatus())
                .isEqualTo("CHECKED_IN");
        assertThat(insert(ctx, "CHECKOU1", "105", MARCH_10, MARCH_12, "Ben", "CHECKED_OUT").getStatus())
                .isEqualTo("CHECKED_OUT");
    }

    @Test
    void unknown_status_is_rejected() {
        assertThatThrownBy(() -> insert(ctx, "NOSHOW01", "104", MARCH_10, MARCH_12, "Anna", "NO_SHOW"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
