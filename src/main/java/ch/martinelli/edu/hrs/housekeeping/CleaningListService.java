package ch.martinelli.edu.hrs.housekeeping;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * UC-013 View Cleaning List: the dirty rooms in cleaning order. Changes no data.
 */
@Service
public class CleaningListService {

    private final CleaningListRepository repository;
    private final Clock clock;

    public CleaningListService(CleaningListRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Step 2: the dirty rooms not blocked today, in cleaning order.
     */
    // UC-013 BR-001: Grouped by cleaning group, within each group ordered by room number.
    public List<CleaningListEntry> cleaningList() {
        return repository.findDirtyRooms(LocalDate.now(clock)).stream()
                .sorted(Comparator.comparingInt(CleaningListEntry::cleaningGroup)
                        .thenComparing(CleaningListEntry::roomNumber))
                .toList();
    }
}
