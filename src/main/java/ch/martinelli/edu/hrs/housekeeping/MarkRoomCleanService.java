package ch.martinelli.edu.hrs.housekeeping;

import ch.martinelli.edu.hrs.reception.RoomCleaningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * UC-014 Mark Room Clean: the housekeeper marks a cleaned room as clean.
 */
@Service
public class MarkRoomCleanService {

    private final RoomCleaningRepository roomCleaningRepository;
    private final TransactionTemplate transactionTemplate;

    public MarkRoomCleanService(RoomCleaningRepository roomCleaningRepository,
                                TransactionTemplate transactionTemplate) {
        this.roomCleaningRepository = roomCleaningRepository;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Steps 3 and 4: checks that the room is still dirty and records it clean.
     *
     * @throws RoomAlreadyCleanException when another housekeeper marked it clean meanwhile (A1)
     */
    public void markClean(Long roomId) {
        transactionTemplate.executeWithoutResult(_ -> {
            // UC-014 BR-001: Only a dirty room can be marked clean.
            if (!roomCleaningRepository.lockAndCheckDirty(roomId)) {
                throw new RoomAlreadyCleanException();
            }
            // UC-014 BR-001, BR-002: Neither the occupancy nor any reservation changes; an occupied room stays
            // occupied.
            roomCleaningRepository.setClean(roomId);
        });
    }
}
