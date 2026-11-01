package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class RoomServiceTest {

    @Autowired
    RoomService service;

    // Scenario "Room type created"
    @Test
    void creates_room_type() {
        service.saveRoomType(new RoomTypeRecord(null, "Suite", 3, new BigDecimal("250.00")));

        assertThat(service.findAllRoomTypes()).extracting(RoomTypeRecord::getName)
                .containsExactly("Double", "Family", "Single", "Suite");
    }

    // Scenario "Nightly rate changed"
    @Test
    void updates_room_type() {
        RoomTypeRecord dbl = roomType("Double");

        service.saveRoomType(new RoomTypeRecord(dbl.getId(), "Double", 2, new BigDecimal("150.00")));

        assertThat(roomType("Double").getNightlyRate()).isEqualByComparingTo("150.00");
    }

    // Scenarios "Room type name already used" and "Changed name already used"
    @Test
    void duplicate_room_type_name_is_reported() {
        assertThatThrownBy(() -> service.saveRoomType(new RoomTypeRecord(null, "Double", 2, BigDecimal.TEN)))
                .isInstanceOf(RoomService.DuplicateException.class)
                .hasMessage("A room type with this name already exists");
        assertThatThrownBy(() -> service.saveRoomType(new RoomTypeRecord(roomType("Single").getId(), "Double", 1, BigDecimal.TEN)))
                .hasMessage("A room type with this name already exists");
    }

    // Scenarios "Room created" and "Room type of a room changed"
    @Test
    void creates_and_updates_room() {
        service.saveRoom(newRoom(null, "111", roomType("Double").getId()));
        RoomRecord room104 = room("104");
        service.saveRoom(newRoom(room104.getId(), "104", roomType("Family").getId()));

        assertThat(room("111").getRoomTypeId()).isEqualTo(roomType("Double").getId());
        assertThat(room("104").getRoomTypeId()).isEqualTo(roomType("Family").getId());
        assertThat(service.findAllRooms()).hasSize(11);
    }

    // Scenarios "Room number already used" and "Changed room number already used"
    @Test
    void duplicate_room_number_is_reported() {
        Long single = roomType("Single").getId();

        assertThatThrownBy(() -> service.saveRoom(newRoom(null, "101", single)))
                .isInstanceOf(RoomService.DuplicateException.class)
                .hasMessage("A room with this number already exists");
        assertThatThrownBy(() -> service.saveRoom(newRoom(room("102").getId(), "101", single)))
                .hasMessage("A room with this number already exists");
    }

    // Scenario "New room is clean"
    @Test
    void new_room_is_clean() {
        service.saveRoom(newRoom(null, "111", roomType("Double").getId()));

        assertThat(room("111").getCleaningStatus()).isEqualTo("CLEAN");
    }

    // Sets only the given fields, so the database fills in the defaults
    private static RoomRecord newRoom(Long id, String number, Long roomTypeId) {
        RoomRecord room = new RoomRecord();
        if (id != null) {
            room.setId(id);
        }
        room.setRoomNumber(number);
        room.setRoomTypeId(roomTypeId);
        return room;
    }

    private RoomTypeRecord roomType(String name) {
        return service.findAllRoomTypes().stream().filter(t -> t.getName().equals(name)).findFirst().orElseThrow();
    }

    private RoomRecord room(String number) {
        return service.findAllRooms().stream().filter(r -> r.getRoomNumber().equals(number)).findFirst().orElseThrow();
    }
}
