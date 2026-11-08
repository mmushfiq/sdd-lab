package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
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
public class RoomTypeServiceTest {

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Test
    void createAndChangeRate() {
        var saved = roomTypeService.save(roomType("Test Double", 2, "120.00"));

        saved.setNightlyRate(new BigDecimal("135.00"));
        roomTypeService.save(saved);

        assertThat(roomTypeService.findAll())
                .filteredOn(t -> t.getId().equals(saved.getId()))
                .singleElement()
                .satisfies(t -> {
                    assertThat(t.getName()).isEqualTo("Test Double");
                    assertThat(t.getCapacity()).isEqualTo(2);
                    assertThat(t.getNightlyRate()).isEqualByComparingTo("135.00");
                });
    }

    @Test
    void duplicateNameIsRejected() {
        roomTypeService.save(roomType("Test Single", 1, "80.00"));

        assertThatThrownBy(() -> roomTypeService.save(roomType("Test Single", 1, "90.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Room type name is already used.");
    }

    @Test
    void deletingTypeUsedByRoomIsRejected() {
        var type = roomTypeService.save(roomType("Test Suite", 4, "250.00"));
        var room = new RoomRecord();
        room.setRoomNumber("T901");
        room.setRoomTypeId(type.getId());
        roomService.save(room);

        assertThatThrownBy(() -> roomTypeService.delete(type.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Room type is still in use.");
    }

    @Test
    void unusedTypeCanBeDeleted() {
        var type = roomTypeService.save(roomType("Test Twin", 2, "110.00"));

        roomTypeService.delete(type.getId());

        assertThat(roomTypeService.findAll()).noneMatch(t -> t.getId().equals(type.getId()));
    }

    @Test
    void invalidValuesAreRejected() {
        assertThatThrownBy(() -> roomTypeService.save(roomType(" ", 2, "100.00")))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Name is required.");
        assertThatThrownBy(() -> roomTypeService.save(roomType("Test Zero", 0, "100.00")))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Capacity must be at least 1.");
        assertThatThrownBy(() -> roomTypeService.save(roomType("Test Free", 2, "0.00")))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Standard nightly rate must be greater than 0.");
    }

    public static RoomTypeRecord roomType(String name, int capacity, String rate) {
        var type = new RoomTypeRecord();
        type.setName(name);
        type.setCapacity(capacity);
        type.setNightlyRate(new BigDecimal(rate));
        return type;
    }
}
