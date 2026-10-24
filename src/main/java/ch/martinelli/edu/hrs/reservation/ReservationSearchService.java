package ch.martinelli.edu.hrs.reservation;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * UC-007 Search Reservations: finds reservations by guest name, reservation code or date. Changes no data.
 */
@Service
public class ReservationSearchService {

    public static final int MAX_RESULTS = 100;

    private final ReservationSearchRepository searchRepository;

    public ReservationSearchService(ReservationSearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    /**
     * Step 3, A2, A3: the first {@value #MAX_RESULTS} matching reservations, ordered by arrival date.
     *
     * @throws IllegalArgumentException when no criterion is entered (A1)
     */
    public ReservationSearchResults search(ReservationSearchCriteria criteria) {
        if (criteria == null || criteria.isEmpty()) {
            throw new IllegalArgumentException("Enter at least one search criterion");
        }
        // UC-007 BR-003: At most 100 matches are listed; one more is read to know whether there are more.
        List<ReservationSearchResult> matches = searchRepository.search(criteria, MAX_RESULTS + 1);
        boolean moreMatches = matches.size() > MAX_RESULTS;
        return new ReservationSearchResults(moreMatches ? matches.subList(0, MAX_RESULTS) : matches, moreMatches);
    }

    /**
     * Step 5: all data of the selected reservation.
     */
    public Optional<ReservationDetail> findReservation(String reservationCode) {
        if (ReservationSearchCriteria.isBlank(reservationCode)) {
            return Optional.empty();
        }
        return searchRepository.findDetail(reservationCode.strip());
    }
}
