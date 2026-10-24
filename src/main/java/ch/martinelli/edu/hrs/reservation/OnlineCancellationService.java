package ch.martinelli.edu.hrs.reservation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * UC-003 Cancel Own Reservation: the guest cancels a reservation found with reservation code and email address.
 */
@Service
public class OnlineCancellationService {

    private final ReservationRepository reservationRepository;
    private final CancellationEmailSender cancellationEmailSender;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public OnlineCancellationService(ReservationRepository reservationRepository,
                                     CancellationEmailSender cancellationEmailSender,
                                     TransactionTemplate transactionTemplate, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.cancellationEmailSender = cancellationEmailSender;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * Step 6: checks that the reservation may be cancelled online.
     *
     * @throws ReservationNotCancellableException when the status is not Confirmed (A4) or the deadline passed (A5)
     */
    public void checkCancellable(OwnReservation reservation) {
        // UC-003 BR-002: Only a reservation with status Confirmed can be cancelled.
        if (reservation.status() != ReservationStatus.CONFIRMED) {
            throw new ReservationNotCancellableException(ReservationNotCancellableException.Reason.NOT_CONFIRMED);
        }
        // UC-003 BR-001: Online until 24 hours before the check-in time (14:00) on the arrival date, local time.
        if (!LocalDateTime.now(clock).isBefore(ReservationLookupService.onlineCancellationDeadline(reservation))) {
            throw new ReservationNotCancellableException(ReservationNotCancellableException.Reason.DEADLINE_PASSED);
        }
    }

    /**
     * Steps 8 to 10 and A7: checks status and deadline again, records the cancellation and sends the cancellation
     * email.
     *
     * @throws ReservationNotCancellableException when the reservation can no longer be cancelled online (A7)
     * @throws IllegalArgumentException when no reservation matches the reservation code and email address
     */
    public OnlineCancellation cancel(String reservationCode, String email) {
        String normalisedEmail = email.strip().toLowerCase(Locale.ROOT);
        LocalDateTime cancelledAt = LocalDateTime.now(clock);

        OwnReservation cancelled = transactionTemplate.execute(_ -> {
            // UC-003 BR-005: The reservation is found under the rules of UC-002 BR-001 and UC-002 BR-004.
            Optional<OwnReservation> found = reservationRepository.findOwnReservationForUpdate(reservationCode,
                    normalisedEmail);
            OwnReservation reservation = found.orElseThrow(
                    () -> new IllegalArgumentException("No reservation matches the reservation code and email"));
            // A7: status and deadline are checked again when the guest confirms
            checkCancellable(reservation);
            reservationRepository.cancel(reservationCode, cancelledAt);
            return new OwnReservation(reservation.reservationCode(), ReservationStatus.CANCELLED,
                    reservation.arrivalDate(), reservation.departureDate(), reservation.roomTypeName(),
                    reservation.numberOfGuests(), reservation.totalPrice());
        });

        // UC-003 BR-004: A cancellation email that could not be sent never undoes the cancellation.
        boolean emailAccepted = cancellationEmailSender.send(normalisedEmail, cancelled.reservationCode(),
                cancelled.arrivalDate(), cancelled.departureDate(), cancelled.roomTypeName(), cancelledAt);
        return new OnlineCancellation(cancelled, emailAccepted);
    }
}
