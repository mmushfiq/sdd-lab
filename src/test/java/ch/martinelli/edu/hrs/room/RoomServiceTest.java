package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.reservation.BookingRequest;
import ch.martinelli.edu.hrs.reservation.BookingSource;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class RoomServiceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private Clock clock;

    private Long typeId;

    @BeforeEach
    void createRoomType() {
        typeId = roomTypeService.save(roomType("Test Double", 2, "120.00")).getId();
    }

    @Test
    void newRoomIsClean() {
        var saved = roomService.save(room("T101"));

        assertThat(roomService.findAll())
                .filteredOn(r -> r.getId().equals(saved.getId()))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.getRoomNumber()).isEqualTo("T101");
                    assertThat(r.getRoomTypeId()).isEqualTo(typeId);
                    assertThat(r.getCleaningStatus()).isEqualTo(CleaningStatus.CLEAN.name());
                });
    }

    @Test
    void duplicateRoomNumberIsRejected() {
        roomService.save(room("T102"));

        assertThatThrownBy(() -> roomService.save(room("T102")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Room number is already used.");
    }

    @Test
    void roomWithoutReservationsCanBeDeleted() {
        var saved = roomService.save(room("T103"));

        roomService.delete(saved.getId());

        assertThat(roomService.findAll()).noneMatch(r -> r.getId().equals(saved.getId()));
    }

    @Test
    void roomWithReservationCannotBeDeleted() {
        var saved = roomService.save(room("T107"));
        var arrival = LocalDate.now(clock).plusDays(1);
        reservationService.book(new BookingRequest(saved.getId(), arrival, arrival.plusDays(1), 1,
                "Test Guest", "guest@example.com", "+41 79 000 00 00", BookingSource.RECEPTION));

        assertThatThrownBy(() -> roomService.delete(saved.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Room has reservations and cannot be deleted.");
    }

    @Test
    void changeRoomNumberAndType() {
        var saved = roomService.save(room("T104"));
        var otherTypeId = roomTypeService.save(roomType("Test Single", 1, "80.00")).getId();

        saved.setRoomNumber("T105");
        saved.setRoomTypeId(otherTypeId);
        roomService.save(saved);

        assertThat(roomService.findAll())
                .filteredOn(r -> r.getId().equals(saved.getId()))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.getRoomNumber()).isEqualTo("T105");
                    assertThat(r.getRoomTypeId()).isEqualTo(otherTypeId);
                });
    }

    @Test
    void missingValuesAreRejected() {
        assertThatThrownBy(() -> roomService.save(room(" ")))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Room number is required.");
        var withoutType = room("T106");
        withoutType.setRoomTypeId(null);
        assertThatThrownBy(() -> roomService.save(withoutType))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Room type is required.");
    }

    @Test
    void checkedOutRoomIsInCleaningListUntilMarkedCleaned() {
        var room = roomService.save(room("T107"));
        roomService.save(room("T108"));
        var today = LocalDate.now(clock);
        var reservation = reservationService.book(new BookingRequest(room.getId(), today, today.plusDays(1), 1,
                "Test Guest", "guest@example.com", "+41 79 000 00 00", BookingSource.RECEPTION));
        reservationService.checkIn(reservation.getId());
        reservationService.checkOut(reservation.getId());

        assertThat(roomService.findDirtyRooms()).extracting(RoomRecord::getRoomNumber)
                .contains("T107").doesNotContain("T108");

        roomService.markCleaned(room.getId());

        assertThat(roomService.findDirtyRooms()).extracting(RoomRecord::getRoomNumber).doesNotContain("T107");
        assertThat(roomService.findAll()).filteredOn(r -> r.getRoomNumber().equals("T107"))
                .extracting(RoomRecord::getCleaningStatus).containsExactly("CLEAN");
    }

    private RoomRecord room(String number) {
        var room = new RoomRecord();
        room.setRoomNumber(number);
        room.setRoomTypeId(typeId);
        return room;
    }
}
