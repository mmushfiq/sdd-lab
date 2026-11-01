package ch.martinelli.edu.hrs.housekeeping;

import ch.martinelli.edu.hrs.housekeeping.HousekeepingService.DirtyRoom;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Transactional
class HousekeepingServiceTest {

    @Autowired
    HousekeepingService service;

    @Autowired
    DSLContext ctx;

    @BeforeEach
    void makeRooms104And102Dirty() {
        ctx.update(ROOM).set(ROOM.CLEANING_STATUS, HousekeepingService.DIRTY)
                .where(ROOM.ROOM_NUMBER.in("104", "102")).execute();
    }

    // Scenario "Dirty rooms listed"
    @Test
    void dirty_rooms_sorted_by_room_number() {
        assertThat(service.findDirtyRooms()).extracting(DirtyRoom::roomNumber, DirtyRoom::roomTypeName)
                .containsExactly(tuple("102", "Single"),
                        tuple("104", "Double"));
    }

    // Scenario "Room marked as cleaned"
    @Test
    void marked_room_leaves_the_list() {
        service.markCleaned(roomId("104"));

        assertThat(service.findDirtyRooms()).extracting(DirtyRoom::roomNumber).containsExactly("102");
        assertThat(status("104")).isEqualTo(HousekeepingService.CLEAN);
    }

    // Scenario "Room already marked by someone else"
    @Test
    void marking_a_clean_room_keeps_it_clean() {
        service.markCleaned(roomId("104"));
        service.markCleaned(roomId("104"));

        assertThat(status("104")).isEqualTo(HousekeepingService.CLEAN);
        assertThat(service.findDirtyRooms()).extracting(DirtyRoom::roomNumber).containsExactly("102");
    }

    private long roomId(String number) {
        return ctx.select(ROOM.ID).from(ROOM).where(ROOM.ROOM_NUMBER.eq(number)).fetchSingle(ROOM.ID);
    }

    private String status(String number) {
        return ctx.select(ROOM.CLEANING_STATUS).from(ROOM).where(ROOM.ROOM_NUMBER.eq(number))
                .fetchSingle(ROOM.CLEANING_STATUS);
    }
}
