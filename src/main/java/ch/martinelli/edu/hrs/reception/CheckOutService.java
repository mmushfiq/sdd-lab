package ch.martinelli.edu.hrs.reception;

import ch.martinelli.edu.hrs.reservation.ChangeableReservation;
import ch.martinelli.edu.hrs.reservation.NightPrice;
import ch.martinelli.edu.hrs.reservation.NightlyRates;
import ch.martinelli.edu.hrs.reservation.ReservationRepository;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * UC-010 Check Out Guest: checks out a guest, shows the total price of the stay and releases the room for
 * housekeeping.
 */
@Service
public class CheckOutService {

    private final ReservationRepository reservationRepository;
    private final NightlyRates nightlyRates;
    private final RoomCleaningRepository roomCleaningRepository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public CheckOutService(ReservationRepository reservationRepository, NightlyRates nightlyRates,
                           RoomCleaningRepository roomCleaningRepository, TransactionTemplate transactionTemplate,
                           Clock clock) {
        this.reservationRepository = reservationRepository;
        this.nightlyRates = nightlyRates;
        this.roomCleaningRepository = roomCleaningRepository;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    public Optional<ChangeableReservation> findReservation(String reservationCode) {
        return reservationRepository.findChangeable(reservationCode);
    }

    /**
     * Steps 3 and 4, A2, A3: checks the status and calculates the departure date and the total price of the stay.
     *
     * @throws CheckOutNotPossibleException when the reservation is not Checked In (A1)
     */
    public CheckOutPreview preview(ChangeableReservation reservation) {
        // UC-010 BR-001: Only a reservation with status Checked In can be checked out.
        if (reservation.status() != ReservationStatus.CHECKED_IN) {
            throw new CheckOutNotPossibleException(reservation.status());
        }
        LocalDate today = LocalDate.now(clock);
        CheckOutPreview.Kind kind;
        LocalDate newDeparture;
        if (reservation.departureDate().isAfter(today)) {
            // UC-010 BR-002: Early check-out: today, but at least the day after the arrival date.
            kind = CheckOutPreview.Kind.EARLY;
            newDeparture = today.isAfter(reservation.arrivalDate()) ? today : reservation.arrivalDate().plusDays(1);
        } else if (reservation.departureDate().isBefore(today)) {
            // UC-010 BR-003: Overdue check-out: the departure date becomes today.
            kind = CheckOutPreview.Kind.OVERDUE;
            newDeparture = today;
        } else {
            kind = CheckOutPreview.Kind.ON_TIME;
            newDeparture = reservation.departureDate();
        }
        // UC-010 BR-002, BR-003: The total price becomes the sum of the night prices of the stay.
        BigDecimal newTotalPrice = kind == CheckOutPreview.Kind.ON_TIME ? reservation.totalPrice()
                : NightPrice.total(nightPricesFor(reservation, newDeparture));
        return new CheckOutPreview(reservation.reservationCode(), reservation.roomNumber(),
                reservation.arrivalDate(), reservation.departureDate(), reservation.totalPrice(), newDeparture,
                newTotalPrice, kind);
    }

    /**
     * Step 6, A5: checks the status again, records the check-out, and sets the room to dirty.
     *
     * @return the total price of the stay
     * @throws CheckOutNotPossibleException when the reservation is no longer Checked In (A5)
     */
    public CheckOutPreview checkOut(String reservationCode) {
        LocalDateTime checkedOutAt = LocalDateTime.now(clock);
        return transactionTemplate.execute(_ -> {
            ChangeableReservation reservation = reservationRepository.findChangeableForUpdate(reservationCode)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown reservation " + reservationCode));
            // A5: checked again under a lock when the receptionist confirms
            CheckOutPreview result = preview(reservation);
            // UC-010 BR-004: The total price is recorded for collection; no payment is recorded.
            reservationRepository.checkOut(reservation.id(), result.newDepartureDate(), result.newTotalPrice(),
                    checkedOutAt);
            if (result.kind() != CheckOutPreview.Kind.ON_TIME) {
                reservationRepository.replaceNightPrices(reservation.id(),
                        nightPricesFor(reservation, result.newDepartureDate()));
            }
            // UC-010 BR-005: The room becomes dirty, regardless of its previous cleaning status.
            roomCleaningRepository.setDirty(reservation.roomId());
            return result;
        });
    }

    /**
     * The night prices of the stay up to the new departure date.
     */
    private List<NightPrice> nightPricesFor(ChangeableReservation reservation, LocalDate newDeparture) {
        List<NightPrice> nightPrices = new ArrayList<>();
        // UC-010 BR-002: The nights from the new departure date on are removed; the nights stayed keep their prices.
        reservationRepository.findNightPrices(reservation.id()).stream()
                .filter(nightPrice -> nightPrice.night().isBefore(newDeparture))
                .forEach(nightPrices::add);
        // UC-010 BR-003: Every additional night gets the nightly rate valid for it at check-out (UC-001 BR-004).
        if (newDeparture.isAfter(reservation.departureDate())) {
            nightPrices.addAll(NightPrice.of(nightlyRates.of(reservation.roomTypeId(), reservation.departureDate(),
                    newDeparture)));
        }
        return nightPrices;
    }
}
