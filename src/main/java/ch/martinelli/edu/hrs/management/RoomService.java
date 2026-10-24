package ch.martinelli.edu.hrs.management;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * UC-017 Maintain Rooms: the manager creates and edits rooms. Rooms are never deleted (UC-017 BR-004).
 */
@Service
public class RoomService {

    public static final int MAX_ROOM_NUMBER_LENGTH = 10;

    private final RoomRepository repository;
    private final RoomTypeRepository roomTypeRepository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public RoomService(RoomRepository repository, RoomTypeRepository roomTypeRepository,
                       TransactionTemplate transactionTemplate, Clock clock) {
        this.repository = repository;
        this.roomTypeRepository = roomTypeRepository;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * Step 2.
     */
    public List<RoomOverview> rooms() {
        return repository.findAll();
    }

    /**
     * Step 4: the room types a room can belong to.
     */
    public List<RoomTypeOverview> roomTypes() {
        return roomTypeRepository.findAll();
    }

    /**
     * Steps 6 and 7: checks the values and records a new room ({@code roomId} null) or changes one.
     *
     * @throws IllegalArgumentException when a value is invalid (A1)
     * @throws RoomNumberUsedException when another room has the room number (A2)
     * @throws ReservationsWouldLoseRoomException when reservations would lose their room (A3)
     * @throws RoomOccupiedException when an occupied room would be deactivated or moved (A4)
     */
    public Long save(Long roomId, RoomData entered) {
        RoomData data = checkValues(entered);
        try {
            return transactionTemplate.execute(_ -> {
                // UC-017 BR-001: Every room has a room number that no other room has.
                if (repository.existsOtherWithNumber(data.roomNumber(), roomId)) {
                    throw new RoomNumberUsedException(data.roomNumber());
                }
                if (roomId == null) {
                    return repository.insert(data);
                }
                RoomOverview room = repository.findForUpdate(roomId)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown room " + roomId));
                boolean leavesRoomType = room.active()
                        && (!data.active() || !room.roomTypeId().equals(data.roomTypeId()));
                if (leavesRoomType) {
                    // UC-017 A4: An occupied room can be deactivated or moved only after check-out.
                    if (repository.isOccupied(roomId)) {
                        throw new RoomOccupiedException();
                    }
                    // UC-017 BR-003, NFR-004: The check and the recording are one action with the room type locked.
                    repository.lockRoomType(room.roomTypeId());
                    List<LocalDate> nights = repository.findNightsWithoutRoom(room.roomTypeId(), roomId,
                            LocalDate.now(clock));
                    if (!nights.isEmpty()) {
                        throw new ReservationsWouldLoseRoomException(room.roomTypeName(), nights);
                    }
                }
                // UC-017 BR-004: An inactive room does not count for availability and cannot be assigned.
                repository.update(roomId, data);
                return roomId;
            });
        } catch (DuplicateKeyException _) {
            throw new RoomNumberUsedException(data.roomNumber());
        }
    }

    // UC-017 A1: The room number is required and at most 10 characters; a room type is selected.
    private RoomData checkValues(RoomData data) {
        String roomNumber = data.roomNumber() == null ? "" : data.roomNumber().strip();
        if (roomNumber.isEmpty() || roomNumber.length() > MAX_ROOM_NUMBER_LENGTH) {
            throw new IllegalArgumentException("The room number must have 1 to " + MAX_ROOM_NUMBER_LENGTH
                    + " characters");
        }
        if (data.roomTypeId() == null || !repository.roomTypeExists(data.roomTypeId())) {
            throw new IllegalArgumentException("Select a room type");
        }
        return new RoomData(roomNumber, data.roomTypeId(), data.active());
    }
}
