package ch.martinelli.edu.hrs.reservation;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 003 FR-008, US3/AC2: V5 gives every night of an existing reservation its old nightly rate.
 */
class SeasonalRateMigrationTest {

    @Test
    void existingReservationsGetOneNightRowPerNightAtTheirOldRate() throws SQLException {
        // One connection for Flyway and the test, so the in-memory database lives for the whole test
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:v5test", "sa", "")) {
            migrate(connection, "4");
            var statement = connection.createStatement();
            statement.execute("insert into room_type (id, name, capacity, nightly_rate) values (1, 'Double', 2, 100)");
            statement.execute("insert into room (id, room_number, room_type_id) values (1, '101', 1)");
            statement.execute("insert into reservation (id, reservation_number, room_id, arrival_date, departure_date, "
                    + "number_of_guests, guest_name, guest_email, guest_phone, nightly_rate, total_price, status, "
                    + "source, created_at) values (1, 'OLDPRICE', 1, date '2030-03-10', date '2030-03-13', 2, "
                    + "'Guest', 'guest@example.com', '+41', 100, 300, 'BOOKED', 'ONLINE', current_timestamp)");

            migrate(connection, "5");

            var result = statement.executeQuery(
                    "select night_date, nightly_rate from reservation_night where reservation_id = 1 order by night_date");
            var nights = new ArrayList<LocalDate>();
            while (result.next()) {
                nights.add(result.getObject(1, LocalDate.class));
                assertThat(result.getBigDecimal(2)).isEqualByComparingTo("100");
            }
            assertThat(nights).containsExactly(
                    LocalDate.of(2030, 3, 10), LocalDate.of(2030, 3, 11), LocalDate.of(2030, 3, 12));
        }
    }

    private static void migrate(Connection connection, String target) {
        Flyway.configure().dataSource(new SingleConnectionDataSource(connection, true)).target(target).load().migrate();
    }
}
