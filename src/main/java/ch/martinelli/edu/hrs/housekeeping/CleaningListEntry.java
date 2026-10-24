package ch.martinelli.edu.hrs.housekeeping;

/**
 * A dirty room on the cleaning list (UC-013 step 2).
 *
 * @param occupied        assigned to a reservation with status Checked In
 * @param checkedOutToday its guest checked out today
 * @param departsToday    its checked-in guest departs today
 */
public record CleaningListEntry(Long roomId, String roomNumber, String roomTypeName, boolean occupied,
                                boolean checkedOutToday, boolean departsToday) {

    /**
     * The group of the room in the cleaning order (UC-013 BR-001): 1 free rooms whose guest checked out today,
     * 2 occupied rooms whose guest departs today, 3 other free rooms, 4 other occupied rooms.
     */
    // UC-013 BR-001: Today's departures first, then the other free and then the other occupied dirty rooms.
    public int cleaningGroup() {
        if (!occupied && checkedOutToday) {
            return 1;
        }
        if (occupied && departsToday) {
            return 2;
        }
        return occupied ? 4 : 3;
    }
}
