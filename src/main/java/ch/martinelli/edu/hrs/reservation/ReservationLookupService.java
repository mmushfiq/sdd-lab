package ch.martinelli.edu.hrs.reservation;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * UC-002 View Own Reservation: finds a reservation by reservation code and email address.
 */
@Service
public class ReservationLookupService {

    private final ReservationRepository reservationRepository;
    private final ReservationLookupLock lookupLock;
    private final Clock clock;

    public ReservationLookupService(ReservationRepository reservationRepository, ReservationLookupLock lookupLock,
                                    Clock clock) {
        this.reservationRepository = reservationRepository;
        this.lookupLock = lookupLock;
        this.clock = clock;
    }

    /**
     * Steps 3 and 4, A2, A3.
     *
     * @return the reservation, or empty when no reservation matches the reservation code and email address (A2)
     * @throws ReservationLookupLockedException when the lookup is locked for the network address (A3)
     */
    public Optional<OwnReservation> lookUp(String reservationCode, String email, String networkAddress) {
        // UC-002 BR-003: A locked address is refused without checking the reservation code.
        if (lookupLock.isLocked(networkAddress)) {
            throw new ReservationLookupLockedException();
        }
        Optional<OwnReservation> reservation = isBlank(reservationCode) || isBlank(email)
                ? Optional.empty()
                : reservationRepository.findOwnReservation(reservationCode, email.strip().toLowerCase(Locale.ROOT));
        if (reservation.isEmpty()) {
            // UC-002 BR-003: Count the failed attempt for the network address.
            lookupLock.countFailedAttempt(networkAddress);
        }
        return reservation;
    }

    /**
     * Reads the reservation again for a guest who already found it in this view, e.g. after a refused cancellation
     * (UC-003 A7). This is not a new lookup and does not count towards the lookup lock.
     */
    public Optional<OwnReservation> reload(String reservationCode, String email) {
        return reservationRepository.findOwnReservation(reservationCode, email.strip().toLowerCase(Locale.ROOT));
    }

    /**
     * UC-002 A4: a Confirmed reservation can be cancelled online while the online cancellation deadline has not
     * passed.
     */
    public boolean canBeCancelledOnline(OwnReservation reservation) {
        // UC-003 BR-002: Only a reservation with status Confirmed can be cancelled.
        // UC-003 BR-001: Online until the stored online cancellation deadline of the reservation.
        return reservation.status() == ReservationStatus.CONFIRMED
                && OnlineCancellationDeadline.notPassed(reservation.onlineCancellationDeadline(),
                LocalDateTime.now(clock));
    }

    /**
     * UC-003 BR-006: the online cancellation deadline as shown to the guest for a Confirmed reservation, or empty for
     * any other status.
     */
    public Optional<String> shownOnlineCancellationDeadline(OwnReservation reservation) {
        if (reservation.status() != ReservationStatus.CONFIRMED) {
            return Optional.empty();
        }
        return Optional.of(OnlineCancellationDeadline.describe(reservation.onlineCancellationDeadline(),
                LocalDateTime.now(clock)));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
