package ch.martinelli.edu.hrs.room;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Requirements "Room types", "Rooms" and "Installed rooms" at database level.
 */
@SpringBootTest
@Transactional
class RoomTableTest {

    @Autowired
    DSLContext ctx;

    @Test
    void capacity_below_one_is_rejected() {
        assertThatThrownBy(() -> insertRoomType("Tiny", 0, "50.00")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nightly_rate_of_zero_is_rejected() {
        assertThatThrownBy(() -> insertRoomType("Free", 1, "0.00")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicate_room_type_name_is_rejected() {
        assertThatThrownBy(() -> insertRoomType("Double", 2, "100.00")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicate_room_number_is_rejected() {
        Long single = ctx.select(ROOM_TYPE.ID).from(ROOM_TYPE).where(ROOM_TYPE.NAME.eq("Single")).fetchSingle(ROOM_TYPE.ID);

        assertThatThrownBy(() -> ctx.insertInto(ROOM).set(ROOM.ROOM_NUMBER, "101").set(ROOM.ROOM_TYPE_ID, single).execute())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Scenario "Ten rooms after installation"
    @Test
    void ten_rooms_with_a_room_type_are_installed() {
        assertThat(ctx.fetchCount(ROOM.join(ROOM_TYPE).on(ROOM.ROOM_TYPE_ID.eq(ROOM_TYPE.ID)))).isEqualTo(10);
    }

    private void insertRoomType(String name, int capacity, String nightlyRate) {
        ctx.insertInto(ROOM_TYPE)
                .set(ROOM_TYPE.NAME, name)
                .set(ROOM_TYPE.CAPACITY, capacity)
                .set(ROOM_TYPE.NIGHTLY_RATE, new BigDecimal(nightlyRate))
                .execute();
    }
}
