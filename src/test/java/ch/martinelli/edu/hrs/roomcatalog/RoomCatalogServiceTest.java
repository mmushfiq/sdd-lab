package ch.martinelli.edu.hrs.roomcatalog;

import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RoomCatalogServiceTest {

    @Autowired
    private RoomCatalogService roomCatalogService;

    @Test
    void bookableRoomsMatchMinimumCapacity() {
        assertThat(roomCatalogService.findBookableRooms(3)).extracting(RoomListItem::number)
                .containsExactly("109", "110");
    }

    @Test
    void bookableRoomsExcludeOutOfServiceRooms() {
        setOutOfService("104");

        assertThat(roomCatalogService.findBookableRooms(2)).extracting(RoomListItem::number)
                .containsExactly("105", "106", "107", "108", "109", "110");
    }

    @Test
    void noBookableRoomsWhenAllAreOutOfService() {
        roomCatalogService.findRooms().forEach(r -> setOutOfService(r.number()));

        assertThat(roomCatalogService.findBookableRooms(1)).isEmpty();
    }

    private void setOutOfService(String number) {
        var room = roomCatalogService.findRooms().stream().filter(r -> r.number().equals(number)).findFirst()
                .orElseThrow();
        roomCatalogService.saveRoom(new RoomRecord(room.id(), room.number(), room.roomTypeId(), true));
    }
}
