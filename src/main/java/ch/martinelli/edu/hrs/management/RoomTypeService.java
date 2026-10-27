package ch.martinelli.edu.hrs.management;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

/**
 * UC-016 Maintain Room Types: the manager creates and edits room types. Room types are never deleted
 * (UC-016 BR-004).
 */
@Service
public class RoomTypeService {

    public static final int MAX_NAME_LENGTH = 50;
    public static final int MAX_DESCRIPTION_LENGTH = 500;
    public static final int MIN_GUESTS = 1;
    public static final int MAX_GUESTS = 10;
    public static final BigDecimal MAX_NIGHTLY_RATE = new BigDecimal("99999999.99");

    private final RoomTypeRepository repository;
    private final TransactionTemplate transactionTemplate;

    public RoomTypeService(RoomTypeRepository repository, TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Step 2.
     */
    public List<RoomTypeOverview> roomTypes() {
        return repository.findAll();
    }

    /**
     * Steps 6 and 7: checks the values and records a new room type ({@code roomTypeId} null) or changes one.
     *
     * @throws IllegalArgumentException when a value is invalid (A1)
     * @throws RoomTypeNameUsedException when another room type has the name (A2)
     * @throws MaxGuestsBelowReservationsException when reservations have more guests than the new maximum (A3)
     */
    public Long save(Long roomTypeId, RoomTypeData entered) {
        RoomTypeData data = checkValues(entered);
        try {
            return transactionTemplate.execute(_ -> {
                // UC-016 BR-001: Every room type has a name that no other room type has.
                if (repository.existsOtherWithName(data.name(), roomTypeId)) {
                    throw new RoomTypeNameUsedException(data.name());
                }
                if (roomTypeId == null) {
                    return repository.insert(data);
                }
                repository.lock(roomTypeId)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown room type " + roomTypeId));
                // UC-016 BR-003: Not below the guests of a Confirmed or Checked In reservation.
                List<AffectedReservation> affected = repository.findReservationsAbove(roomTypeId, data.maxGuests());
                if (!affected.isEmpty()) {
                    throw new MaxGuestsBelowReservationsException(affected);
                }
                // UC-016 BR-004: An inactive room type is no longer offered; its reservations stay valid.
                repository.update(roomTypeId, data);
                return roomTypeId;
            });
        } catch (DuplicateKeyException _) {
            // UC-016 BR-001: another manager saved the same name at the same time
            throw new RoomTypeNameUsedException(data.name());
        }
    }

    // UC-016 A1: name 1 to 50 characters, description at most 500, 1 to 10 guests, normal rate not missing or
    // negative (with at most 2 decimals and at most 99999999.99, entity model).
    static RoomTypeData checkValues(RoomTypeData data) {
        String name = data.name() == null ? "" : data.name().strip();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("The name must have 1 to " + MAX_NAME_LENGTH + " characters");
        }
        String description = data.description() == null || data.description().isBlank() ? null
                : data.description().strip();
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("The description must have at most " + MAX_DESCRIPTION_LENGTH
                    + " characters");
        }
        if (data.maxGuests() == null || data.maxGuests() < MIN_GUESTS || data.maxGuests() > MAX_GUESTS) {
            throw new IllegalArgumentException("The maximum number of guests must be between " + MIN_GUESTS
                    + " and " + MAX_GUESTS);
        }
        if (!isValidNightlyRate(data.nightlyRate())) {
            throw new IllegalArgumentException("The normal rate must be between 0 and " + MAX_NIGHTLY_RATE);
        }
        return new RoomTypeData(name, description, data.maxGuests(), data.nightlyRate(), data.active());
    }

    public static boolean isValidNightlyRate(BigDecimal rate) {
        return rate != null && rate.signum() >= 0 && rate.compareTo(MAX_NIGHTLY_RATE) <= 0
                && rate.stripTrailingZeros().scale() <= 2;
    }
}
