package ch.martinelli.edu.hrs.reservation;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 002 FR-003, SC-002: V4 gives reservations that exist before it the old rule (15 h) and later ones 48 h.
 */
class CancellationRuleMigrationTest {

    @Test
    void existingReservationsKeepTheOldRule() throws SQLException {
        // One connection for Flyway and the test, so the in-memory database lives for the whole test
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:v4test", "sa", "")) {
            migrate(connection, "3");
            var statement = connection.createStatement();
            statement.execute("insert into room_type (id, name, capacity, nightly_rate) values (1, 'Double', 2, 100)");
            statement.execute("insert into room (id, room_number, room_type_id) values (1, '101', 1)");
            statement.execute(reservation(1, "OLDRULE1"));

            migrate(connection, null);
            statement.execute(reservation(2, "NEWRULE1"));

            var result = statement.executeQuery("select cancellation_hours_before from reservation order by id");
            result.next();
            assertThat(result.getInt(1)).isEqualTo(15);
            result.next();
            assertThat(result.getInt(1)).isEqualTo(48);
        }
    }

    private static void migrate(Connection connection, String target) {
        var config = Flyway.configure().dataSource(new SingleConnectionDataSource(connection, true));
        if (target != null) {
            config.target(target);
        }
        config.load().migrate();
    }

    /**
     * A reservation row without cancellation_hours_before, as V3-era code would insert it.
     */
    private static String reservation(int id, String number) {
        return "insert into reservation (id, reservation_number, room_id, arrival_date, departure_date, "
                + "number_of_guests, guest_name, guest_email, guest_phone, nightly_rate, total_price, status, "
                + "source, created_at) values (" + id + ", '" + number + "', 1, date '2030-03-1" + id + "', "
                + "date '2030-03-2" + id + "', 2, 'Guest', 'guest@example.com', '+41', 100, 200, 'BOOKED', "
                + "'ONLINE', current_timestamp)";
    }
}
