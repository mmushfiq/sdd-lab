package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.reservation.InvalidChangeException.Field;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * UC-005 Change Reservation: the receptionist changes the dates, room type or number of guests of a reservation.
 */
@Service
public class ReservationChangeService {

    private final ReservationRepository reservationRepository;
    private final RoomTypeAvailabilityRepository availabilityRepository;
    private final NightlyRates nightlyRates;
    private final ChangeEmailSender changeEmailSender;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public ReservationChangeService(ReservationRepository reservationRepository,
                                    RoomTypeAvailabilityRepository availabilityRepository, NightlyRates nightlyRates,
                                    ChangeEmailSender changeEmailSender, TransactionTemplate transactionTemplate,
                                    Clock clock) {
        this.reservationRepository = reservationRepository;
        this.availabilityRepository = availabilityRepository;
        this.nightlyRates = nightlyRates;
        this.changeEmailSender = changeEmailSender;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * Step 2: the reservation with this reservation code.
     */
    public Optional<ChangeableReservation> findReservation(String reservationCode) {
        if (reservationCode == null || reservationCode.isBlank()) {
            return Optional.empty();
        }
        return reservationRepository.findChangeable(reservationCode.strip());
    }

    /**
     * Step 3: the room types the reservation can be changed to.
     */
    public List<RoomTypeChoice> roomTypesFor(ChangeableReservation reservation) {
        return availabilityRepository.findRoomTypesForChange(reservation.roomTypeId());
    }

    // UC-005 BR-001: Confirmed reservations change dates, room type and guests; Checked In only the departure date.
    public static boolean isChangeable(ReservationStatus status) {
        return status == ReservationStatus.CONFIRMED || status == ReservationStatus.CHECKED_IN;
    }

    /**
     * Steps 4 and 5: checks the new values and the availability and returns the old and the new total price.
     *
     * @throws ReservationNotChangeableException when the status does not allow a change (A1)
     * @throws InvalidChangeException when a new value is invalid (A3)
     * @throws NightsNotAvailableException when the room type or the assigned room is not available (A4)
     */
    public ChangePreview check(String reservationCode, ReservationChange change) {
        ChangeableReservation reservation = reservationRepository.findChangeable(reservationCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown reservation " + reservationCode));
        RoomTypeChoice roomType = checkValues(reservation, change);
        checkAvailability(reservation, change);
        return new ChangePreview(reservation.totalPrice(), NightPrice.total(newNightPrices(reservation, change)),
                roomType.name(), isChanged(reservation, change));
    }

    /**
     * Steps 6 to 8: checks again, records the changed reservation and sends the change confirmation email.
     *
     * @throws ReservationNotChangeableException when the status does not allow a change (A1)
     * @throws InvalidChangeException when a new value is invalid (A3)
     * @throws NightsNotAvailableException when another reservation took the last room since the check (A7)
     */
    public ChangeResult change(String reservationCode, ReservationChange change) {
        LocalDateTime changedAt = LocalDateTime.now(clock);
        ChangedReservation changed = transactionTemplate.execute(_ -> record(reservationCode, change));
        if (changed == null) {
            // A5: nothing changed, no email
            return new ChangeResult(false, false);
        }
        // UC-005 step 8, UC-003 BR-006: The email shows the online cancellation deadline of a Confirmed reservation.
        Optional<LocalDateTime> shownDeadline = changed.status() == ReservationStatus.CONFIRMED
                ? Optional.of(changed.onlineCancellationDeadline())
                : Optional.empty();
        boolean emailAccepted = changeEmailSender.send(changed.guestEmail(), reservationCode, change.arrivalDate(),
                change.departureDate(), changed.roomTypeName(), change.numberOfGuests(), changed.totalPrice(),
                shownDeadline, changedAt);
        return new ChangeResult(true, emailAccepted);
    }

    private ChangedReservation record(String reservationCode, ReservationChange change) {
        ChangeableReservation reservation = reservationRepository.findChangeableForUpdate(reservationCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown reservation " + reservationCode));
        RoomTypeChoice roomType = checkValues(reservation, change);
        // UC-005 A5: The reservation stays unchanged when no value differs.
        if (!isChanged(reservation, change)) {
            return null;
        }
        // UC-005 BR-004: Lock the room type so that the check and the recording are one indivisible action
        // (UC-001 BR-005).
        availabilityRepository.lockRoomType(change.roomTypeId());
        checkAvailability(reservation, change);

        // UC-005 step 7: the night prices of the new date range and their sum as the new total price
        List<NightPrice> nightPrices = newNightPrices(reservation, change);
        BigDecimal totalPrice = NightPrice.total(nightPrices);
        LocalDateTime onlineCancellationDeadline = newOnlineCancellationDeadline(reservation, change);
        reservationRepository.change(reservation.id(), change.arrivalDate(), change.departureDate(),
                change.roomTypeId(), change.numberOfGuests(), totalPrice, onlineCancellationDeadline);
        reservationRepository.replaceNightPrices(reservation.id(), nightPrices);
        return new ChangedReservation(reservation.guestEmail(), roomType.name(), totalPrice, reservation.status(),
                onlineCancellationDeadline);
    }

    /**
     * A3: checks the new values against the status and the rules; returns the new room type.
     */
    private RoomTypeChoice checkValues(ChangeableReservation reservation, ReservationChange change) {
        if (!isChangeable(reservation.status())) {
            throw new ReservationNotChangeableException(reservation.status());
        }
        LocalDate today = LocalDate.now(clock);
        if (reservation.status() == ReservationStatus.CHECKED_IN) {
            // UC-005 BR-001: A Checked In reservation can change only its departure date.
            if (!reservation.arrivalDate().equals(change.arrivalDate())
                    || !reservation.roomTypeId().equals(change.roomTypeId())
                    || reservation.numberOfGuests() != change.numberOfGuests()) {
                throw new IllegalArgumentException("A checked-in reservation can change only its departure date");
            }
            // UC-005 BR-003: The new departure date is after today.
            if (change.departureDate() == null || !change.departureDate().isAfter(today)) {
                throw new InvalidChangeException(Field.DEPARTURE_DATE, "The departure date must be after today");
            }
            return availabilityRepository.findRoomType(change.roomTypeId()).orElseThrow();
        }

        // UC-005 BR-002: Arrival today up to 365 days ahead, departure after arrival (UC-001 BR-001).
        if (change.arrivalDate() == null || change.arrivalDate().isBefore(today)
                || change.arrivalDate().isAfter(today.plusDays(OnlineReservationService.MAX_DAYS_AHEAD))) {
            throw new InvalidChangeException(Field.ARRIVAL_DATE, "The arrival date must be between today and "
                    + OnlineReservationService.MAX_DAYS_AHEAD + " days ahead");
        }
        if (change.departureDate() == null || !change.departureDate().isAfter(change.arrivalDate())) {
            throw new InvalidChangeException(Field.DEPARTURE_DATE, "The departure date must be after the arrival date");
        }
        RoomTypeChoice roomType = change.roomTypeId() == null ? null
                : availabilityRepository.findRoomType(change.roomTypeId()).orElse(null);
        // UC-005 BR-004: A reservation can be moved only to an active room type; it can keep an inactive one.
        if (roomType == null || (!roomType.active() && !roomType.roomTypeId().equals(reservation.roomTypeId()))) {
            throw new InvalidChangeException(Field.ROOM_TYPE, "Select an active room type");
        }
        // UC-005 A3: The number of guests is between 1 and 10 and fits the room type.
        if (change.numberOfGuests() < OnlineReservationService.MIN_GUESTS
                || change.numberOfGuests() > OnlineReservationService.MAX_GUESTS) {
            throw new InvalidChangeException(Field.NUMBER_OF_GUESTS, "The number of guests must be between "
                    + OnlineReservationService.MIN_GUESTS + " and " + OnlineReservationService.MAX_GUESTS);
        }
        if (change.numberOfGuests() > roomType.maxGuests()) {
            throw new InvalidChangeException(Field.NUMBER_OF_GUESTS, "The " + roomType.name() + " takes at most "
                    + roomType.maxGuests() + " guests");
        }
        return roomType;
    }

    /**
     * A2 step 3 and A4: the room type must be available for every night of the new date range, without the
     * reservation itself; for a checked-in guest only added nights are checked, and the assigned room must not be
     * blocked on them.
     */
    private void checkAvailability(ChangeableReservation reservation, ReservationChange change) {
        List<LocalDate> unavailable;
        if (reservation.status() == ReservationStatus.CHECKED_IN) {
            // UC-005 BR-003: A longer stay requires availability and an unblocked assigned room for every added night.
            LocalDate firstAddedNight = reservation.departureDate();
            List<LocalDate> nights = new ArrayList<>(availabilityRepository.findUnavailableNights(
                    reservation.roomTypeId(), firstAddedNight, change.departureDate(), reservation.id()));
            availabilityRepository.findBlockedNights(reservation.roomId(), firstAddedNight, change.departureDate())
                    .stream()
                    .filter(night -> !nights.contains(night))
                    .forEach(nights::add);
            nights.sort(null);
            unavailable = nights;
        } else {
            // UC-005 BR-004: Availability for the new values without the reservation being changed.
            unavailable = availabilityRepository.findUnavailableNights(change.roomTypeId(), change.arrivalDate(),
                    change.departureDate(), reservation.id());
        }
        if (!unavailable.isEmpty()) {
            throw new NightsNotAvailableException(unavailable);
        }
    }

    // UC-005 BR-005: A night already part of the reservation keeps its night price while the room type stays; added
    // nights, and all nights after a change of the room type, get the nightly rate valid now (UC-001 BR-004).
    // Nights no longer part of the reservation are removed.
    private List<NightPrice> newNightPrices(ChangeableReservation reservation, ReservationChange change) {
        Map<LocalDate, BigDecimal> kept = new HashMap<>();
        if (reservation.roomTypeId().equals(change.roomTypeId())) {
            reservationRepository.findNightPrices(reservation.id())
                    .forEach(nightPrice -> kept.put(nightPrice.night(), nightPrice.price()));
        }
        return nightlyRates.of(change.roomTypeId(), change.arrivalDate(), change.departureDate()).stream()
                .map(rate -> new NightPrice(rate.night(), kept.getOrDefault(rate.night(), rate.rate())))
                .toList();
    }

    // UC-005 BR-007: A new arrival date of a Confirmed reservation sets the deadline again to 48 hours before the
    // check-in time (UC-003 BR-001), also after 24 hours; other changes keep the deadline.
    static LocalDateTime newOnlineCancellationDeadline(ChangeableReservation reservation, ReservationChange change) {
        if (reservation.status() == ReservationStatus.CONFIRMED
                && !reservation.arrivalDate().equals(change.arrivalDate())) {
            return OnlineCancellationDeadline.forArrival(change.arrivalDate());
        }
        return reservation.onlineCancellationDeadline();
    }

    private static boolean isChanged(ChangeableReservation reservation, ReservationChange change) {
        return !reservation.arrivalDate().equals(change.arrivalDate())
                || !reservation.departureDate().equals(change.departureDate())
                || !reservation.roomTypeId().equals(change.roomTypeId())
                || reservation.numberOfGuests() != change.numberOfGuests();
    }

    private record ChangedReservation(String guestEmail, String roomTypeName, BigDecimal totalPrice,
                                      ReservationStatus status, LocalDateTime onlineCancellationDeadline) {
    }
}
