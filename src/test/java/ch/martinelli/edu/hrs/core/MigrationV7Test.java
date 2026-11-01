package ch.martinelli.edu.hrs.core;

import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V7 copies every existing reservation into one night row per night at the reservation's rate.
 */
class MigrationV7Test {

    @Test
    void existing_reservation_gets_one_row_per_night() {
        // One connection for Flyway and the test: H2 checks constraints in the session that created them
        SingleConnectionDataSource dataSource =
                new SingleConnectionDataSource("jdbc:h2:mem:migration-v7", "sa", "", true);
        try {
            DSLContext ctx = DSL.using(dataSource, SQLDialect.H2);
            migrate(dataSource, "6");
            ctx.execute("""
                    INSERT INTO reservation (reservation_number, room_id, arrival_date, departure_date, number_of_guests,
                                             guest_name, guest_email, nightly_rate, status)
                    SELECT 'MIGRATE7', id, DATE '2030-03-10', DATE '2030-03-13', 1, 'Anna Meier', 'anna@example.com',
                           140.00, 'BOOKED'
                    FROM room WHERE room_number = '104'""");

            migrate(dataSource, "7");

            var nights = ctx.resultQuery("""
                    SELECT n.night_date, n.nightly_rate FROM reservation_night n
                    JOIN reservation r ON r.id = n.reservation_id
                    WHERE r.reservation_number = 'MIGRATE7' ORDER BY n.night_date""").fetch();
            assertThat(nights).hasSize(3);
            assertThat(nights.getValues(0, LocalDate.class)).containsExactly(
                    LocalDate.of(2030, 3, 10), LocalDate.of(2030, 3, 11), LocalDate.of(2030, 3, 12));
            assertThat(nights.getValues(1, BigDecimal.class))
                    .allSatisfy(rate -> assertThat(rate).isEqualByComparingTo("140.00"));
        } finally {
            dataSource.destroy();
        }
    }

    private static void migrate(SingleConnectionDataSource dataSource, String target) {
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target(target).load()
                .migrate();
    }
}
