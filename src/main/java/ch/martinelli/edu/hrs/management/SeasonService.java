package ch.martinelli.edu.hrs.management;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * UC-025 Maintain Seasonal Rates: the manager creates, changes and deletes seasons with their season rates.
 * <p>
 * The prices of reservations already recorded are never touched here (UC-025 BR-004): night prices are fixed when
 * a night is recorded, so a season only affects nights recorded afterwards.
 */
@Service
public class SeasonService {

    public static final int MAX_NAME_LENGTH = 50;

    private final SeasonRepository repository;
    private final TransactionTemplate transactionTemplate;
    // UC-025 BR-002: two managers saving overlapping seasons at the same time must not both succeed.
    private final ReentrantLock saveLock = new ReentrantLock();

    public SeasonService(SeasonRepository repository, TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Step 2.
     */
    public List<SeasonOverview> seasons() {
        return repository.findAll();
    }

    public Optional<SeasonOverview> season(Long seasonId) {
        return repository.findById(seasonId);
    }

    /**
     * Step 4: the room types of the season ({@code seasonId} null for a new season) with their normal rate and
     * season rate.
     */
    public List<SeasonRoomType> roomTypes(Long seasonId) {
        return repository.findRoomTypes(seasonId);
    }

    /**
     * Steps 6 to 8: checks the values and records a new season ({@code seasonId} null) or changes one.
     *
     * @throws IllegalArgumentException when a value is invalid (A1)
     * @throws SeasonNameUsedException  when another season has the name (A2)
     * @throws SeasonOverlapException   when another season covers one of the nights (A3)
     */
    public Long save(Long seasonId, SeasonData entered) {
        SeasonData data = checkValues(entered);
        saveLock.lock();
        try {
            return transactionTemplate.execute(_ -> {
                if (seasonId != null && !repository.exists(seasonId)) {
                    throw new IllegalArgumentException("Unknown season " + seasonId);
                }
                // UC-025 BR-001: Every season has a name that no other season has.
                if (repository.existsOtherWithName(data.name(), seasonId)) {
                    throw new SeasonNameUsedException(data.name());
                }
                // UC-025 BR-002: Every night belongs to at most one season.
                repository.findOverlapping(data.firstNight(), data.lastNight(), seasonId)
                        .ifPresent(other -> {
                            throw new SeasonOverlapException(other);
                        });
                checkRoomTypes(seasonId, data.seasonRates());
                if (seasonId == null) {
                    return repository.insert(data);
                }
                repository.update(seasonId, data);
                return seasonId;
            });
        } catch (DuplicateKeyException _) {
            // UC-025 BR-001: another manager saved the same name at the same time
            throw new SeasonNameUsedException(data.name());
        } finally {
            saveLock.unlock();
        }
    }

    /**
     * A4 step 3: removes the season and its season rates.
     */
    // UC-025 BR-005: A season can be deleted at any time, also when it has started or ended.
    public void delete(Long seasonId) {
        saveLock.lock();
        try {
            transactionTemplate.executeWithoutResult(_ -> repository.delete(seasonId));
        } finally {
            saveLock.unlock();
        }
    }

    // UC-025 BR-006: A season rate can be added only for an active room type; an existing season rate of an
    // inactive room type can be changed or removed.
    private void checkRoomTypes(Long seasonId, Map<Long, BigDecimal> rates) {
        if (rates.isEmpty()) {
            return;
        }
        Map<Long, Boolean> active = repository.findActive(rates.keySet());
        Map<Long, BigDecimal> existing = seasonId == null ? Map.of() : repository.findSeasonRates(seasonId);
        for (Long roomTypeId : rates.keySet()) {
            Boolean roomTypeActive = active.get(roomTypeId);
            if (roomTypeActive == null) {
                throw new IllegalArgumentException("Unknown room type " + roomTypeId);
            }
            if (!roomTypeActive && !existing.containsKey(roomTypeId)) {
                throw new IllegalArgumentException("A season rate can be added only for an active room type");
            }
        }
    }

    // UC-025 A1: name 1 to 50 characters, first and last night entered, last night not before the first night,
    // season rates between 0 and 99,999,999.99 (BR-006).
    static SeasonData checkValues(SeasonData data) {
        String name = data.name() == null ? "" : data.name().strip();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("The name must have 1 to " + MAX_NAME_LENGTH + " characters");
        }
        if (data.firstNight() == null || data.lastNight() == null) {
            throw new IllegalArgumentException("The first night and the last night are required");
        }
        if (data.lastNight().isBefore(data.firstNight())) {
            throw new IllegalArgumentException("The last night must be on or after the first night");
        }
        Map<Long, BigDecimal> rates = new LinkedHashMap<>();
        if (data.seasonRates() != null) {
            data.seasonRates().forEach((roomTypeId, rate) -> {
                // A room type left without a season rate keeps its normal rate (step 5).
                if (rate == null) {
                    return;
                }
                if (!isValidSeasonRate(rate)) {
                    throw new IllegalArgumentException("A season rate must be between 0 and "
                            + RoomTypeService.MAX_NIGHTLY_RATE);
                }
                rates.put(roomTypeId, rate);
            });
        }
        return new SeasonData(name, data.firstNight(), data.lastNight(), rates);
    }

    // UC-025 BR-006: A season rate is between 0 and 99,999,999.99 CHF (with at most 2 decimals, entity model).
    public static boolean isValidSeasonRate(BigDecimal rate) {
        return RoomTypeService.isValidNightlyRate(rate);
    }
}
