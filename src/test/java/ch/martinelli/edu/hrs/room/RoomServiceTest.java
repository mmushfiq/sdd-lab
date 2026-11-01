package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import ch.martinelli.edu.hrs.db.tables.records.SeasonalRateRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class RoomServiceTest {

    static final LocalDate JULY_1 = LocalDate.of(2030, 7, 1);
    static final LocalDate AUGUST_31 = LocalDate.of(2030, 8, 31);
    static final LocalDate DECEMBER_20 = LocalDate.of(2030, 12, 20);
    static final LocalDate JANUARY_6 = LocalDate.of(2031, 1, 6);

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
    // Requirement "Create a seasonal rate", scenario "Seasonal rate created"; listed by room type and first night
    @Test
    void creates_seasonal_rates_sorted_by_room_type_and_first_night() {
        service.saveSeasonalRate(seasonalRate(null, "Family", "Christmas", DECEMBER_20, JANUARY_6, "280.00"));
        service.saveSeasonalRate(seasonalRate(null, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00"));
        service.saveSeasonalRate(seasonalRate(null, "Double", "Summer", JULY_1, AUGUST_31, "170.00"));

        assertThat(service.findAllSeasonalRates())
                .extracting(r -> roomTypeName(r.getRoomTypeId()) + " " + r.getName())
                .containsExactly("Double Summer", "Double Christmas", "Family Christmas");
    }

    // Scenario "Last night before first night"
    @Test
    void last_night_before_first_night_is_rejected() {
        assertThatThrownBy(() -> service.saveSeasonalRate(
                seasonalRate(null, "Double", "Christmas", JANUARY_6, DECEMBER_20, "180.00")))
                .isInstanceOf(RoomService.SeasonalRateException.class)
                .hasMessage("Last night cannot be before first night");
        assertThat(service.findAllSeasonalRates()).isEmpty();
    }

    // Scenario "Overlapping seasonal rate"
    @Test
    void overlapping_seasonal_rate_is_rejected() {
        service.saveSeasonalRate(seasonalRate(null, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00"));

        assertThatThrownBy(() -> service.saveSeasonalRate(seasonalRate(null, "Double", "January",
                LocalDate.of(2031, 1, 1), LocalDate.of(2031, 1, 15), "120.00")))
                .isInstanceOf(RoomService.SeasonalRateException.class)
                .hasMessage("The dates overlap the seasonal rate \"Christmas\" of this room type");
        assertThat(service.findAllSeasonalRates()).hasSize(1);
    }

    // Seasons that only touch (last night of one, first night of the next) do not overlap
    @Test
    void back_to_back_seasonal_rates_are_allowed() {
        service.saveSeasonalRate(seasonalRate(null, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00"));
        service.saveSeasonalRate(seasonalRate(null, "Double", "January", JANUARY_6.plusDays(1),
                LocalDate.of(2031, 1, 31), "120.00"));

        assertThat(service.findAllSeasonalRates()).hasSize(2);
    }

    // Scenario "Same dates for another room type"
    @Test
    void same_dates_for_another_room_type_are_allowed() {
        service.saveSeasonalRate(seasonalRate(null, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00"));
        service.saveSeasonalRate(seasonalRate(null, "Family", "Christmas", DECEMBER_20, JANUARY_6, "280.00"));

        assertThat(service.findAllSeasonalRates()).hasSize(2);
    }

    // Scenario "Seasonal rate changed": the seasonal rate itself does not count as an overlap
    @Test
    void changes_seasonal_rate() {
        service.saveSeasonalRate(seasonalRate(null, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00"));
        long id = service.findAllSeasonalRates().getFirst().getId();

        service.saveSeasonalRate(seasonalRate(id, "Double", "Christmas", DECEMBER_20, JANUARY_6, "190.00"));

        assertThat(service.findAllSeasonalRates()).singleElement()
                .satisfies(r -> assertThat(r.getNightlyRate()).isEqualByComparingTo("190.00"));
    }

    // Scenario "Changed dates overlap"
    @Test
    void changed_dates_overlapping_another_seasonal_rate_are_rejected() {
        service.saveSeasonalRate(seasonalRate(null, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00"));
        service.saveSeasonalRate(seasonalRate(null, "Double", "Summer", JULY_1, AUGUST_31, "170.00"));
        long summer = service.findAllSeasonalRates().getFirst().getId();

        assertThatThrownBy(() -> service.saveSeasonalRate(
                seasonalRate(summer, "Double", "Summer", JULY_1, DECEMBER_20, "170.00")))
                .hasMessage("The dates overlap the seasonal rate \"Christmas\" of this room type");
    }

    // Scenario "Seasonal rate deleted"
    @Test
    void deletes_seasonal_rate() {
        service.saveSeasonalRate(seasonalRate(null, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00"));

        service.deleteSeasonalRate(service.findAllSeasonalRates().getFirst().getId());

        assertThat(service.findAllSeasonalRates()).isEmpty();
    }

    private SeasonalRateRecord seasonalRate(Long id, String roomTypeName, String name, LocalDate firstNight,
                                            LocalDate lastNight, String rate) {
        return new SeasonalRateRecord(id, roomType(roomTypeName).getId(), name, firstNight, lastNight,
                new BigDecimal(rate));
    }

    private String roomTypeName(Long roomTypeId) {
        return service.findAllRoomTypes().stream().filter(t -> t.getId().equals(roomTypeId)).findFirst()
                .orElseThrow().getName();
    }

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
