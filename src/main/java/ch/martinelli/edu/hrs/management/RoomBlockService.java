package ch.martinelli.edu.hrs.management;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * UC-018 Block Room: the manager blocks a room for a date range, so that it is neither reserved nor assigned.
 */
@Service
public class RoomBlockService {

    public static final int MAX_REASON_LENGTH = 200;

    private final RoomBlockRepository repository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public RoomBlockService(RoomBlockRepository repository, TransactionTemplate transactionTemplate, Clock clock) {
        this.repository = repository;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * Step 2.
     */
    public List<RoomBlockOverview> roomBlocks() {
        return repository.findCurrentAndFuture(today());
    }

    /**
     * Step 3: the rooms the manager can block.
     */
    public List<BlockableRoom> rooms() {
        return repository.findActiveRooms();
    }

    /**
     * Steps 5 and 6: checks the block and records it.
     *
     * @throws IllegalArgumentException when a value is invalid (A1)
     * @throws RoomBlockOverlapException when the block overlaps another block of the room (A2)
     * @throws RoomOccupiedDuringBlockException when the room is occupied during the block (A3)
     * @throws ReservationsWouldLoseRoomException when reservations would lose their room (A4)
     */
    public Long block(RoomBlockData entered) {
        RoomBlockData data = checkValues(entered);
        // UC-018 BR-003, NFR-004: The check and the recording are one action with the room type locked.
        return transactionTemplate.execute(_ -> {
            BlockableRoom room = repository.findActiveRoomForUpdate(data.roomId())
                    .orElseThrow(() -> new IllegalArgumentException("Select an active room"));
            repository.lockRoomType(room.roomTypeId());
            // UC-018 BR-001: Blocks of the same room do not overlap.
            repository.findOverlapping(room.id(), data.startDate(), data.endDate()).ifPresent(block -> {
                throw new RoomBlockOverlapException(block);
            });
            // UC-018 BR-002: A room cannot be blocked while it is assigned to a Checked In reservation.
            repository.findCheckedInReservationDuring(room.id(), data.startDate(), data.endDate())
                    .ifPresent(code -> {
                        throw new RoomOccupiedDuringBlockException(code);
                    });
            // UC-018 BR-003: Every Confirmed and Checked In reservation still has a room for every blocked night.
            List<LocalDate> nights = repository.findNightsWithoutRoom(room.roomTypeId(), room.id(),
                    data.startDate(), data.endDate());
            if (!nights.isEmpty()) {
                throw new ReservationsWouldLoseRoomException(room.roomTypeName(), nights);
            }
            return repository.insert(data);
        });
    }

    // UC-018 BR-001: The first blocked date is today or later and the last blocked date is on or after it.
    // UC-018 A1: The reason has at most 200 characters.
    private RoomBlockData checkValues(RoomBlockData data) {
        if (data.roomId() == null) {
            throw new IllegalArgumentException("Select a room");
        }
        if (data.startDate() == null || data.startDate().isBefore(today())) {
            throw new IllegalArgumentException("The first blocked date must be today or later");
        }
        if (data.endDate() == null || data.endDate().isBefore(data.startDate())) {
            throw new IllegalArgumentException("The last blocked date must be on or after the first blocked date");
        }
        String reason = data.reason() == null ? null : data.reason().strip();
        if (reason != null && reason.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("The reason can have at most " + MAX_REASON_LENGTH + " characters");
        }
        return new RoomBlockData(data.roomId(), data.startDate(), data.endDate(),
                reason == null || reason.isEmpty() ? null : reason);
    }
}
