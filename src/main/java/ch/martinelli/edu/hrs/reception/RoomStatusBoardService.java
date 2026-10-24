package ch.martinelli.edu.hrs.reception;

import ch.martinelli.edu.hrs.reservation.RoomTypeAvailabilityRepository;
import ch.martinelli.edu.hrs.reservation.RoomTypeChoice;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * UC-011 View Room Status Board: every active room with its occupancy and cleaning status. Changes no data.
 */
@Service
public class RoomStatusBoardService {

    private final RoomStatusRepository roomStatusRepository;
    private final RoomTypeAvailabilityRepository roomTypeRepository;
    private final Clock clock;

    public RoomStatusBoardService(RoomStatusRepository roomStatusRepository,
                                  RoomTypeAvailabilityRepository roomTypeRepository, Clock clock) {
        this.roomStatusRepository = roomStatusRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.clock = clock;
    }

    /**
     * Steps 2 and 3, A1: the active rooms matching the filter, ordered by room number. A null filter value matches
     * every room.
     */
    public List<RoomStatus> rooms(Long roomTypeId, Occupancy occupancy, Boolean clean) {
        return roomStatusRepository.findRoomStatuses(LocalDate.now(clock)).stream()
                .filter(room -> roomTypeId == null || room.roomTypeId().equals(roomTypeId))
                .filter(room -> occupancy == null || room.occupancy() == occupancy)
                .filter(room -> clean == null || room.clean() == clean)
                .toList();
    }

    /**
     * The room types to filter by: every room type with an active room.
     */
    public List<RoomTypeChoice> roomTypes() {
        List<Long> roomTypeIds = roomStatusRepository.findRoomStatuses(LocalDate.now(clock)).stream()
                .map(RoomStatus::roomTypeId).distinct().toList();
        return roomTypeIds.stream()
                .map(roomTypeRepository::findRoomType)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(RoomTypeChoice::name))
                .toList();
    }
}
