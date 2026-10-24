package ch.martinelli.edu.hrs.reservation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * UC-006 Cancel Reservation: the receptionist cancels a reservation before check-in.
 */
@Service
public class ReceptionCancellationService {

    private final ReservationRepository reservationRepository;
    private final CancellationEmailSender cancellationEmailSender;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public ReceptionCancellationService(ReservationRepository reservationRepository,
                                        CancellationEmailSender cancellationEmailSender,
                                        TransactionTemplate transactionTemplate, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.cancellationEmailSender = cancellationEmailSender;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * Step 4: checks the current status of the reservation.
     *
     * @throws ReservationNotCancellableException when the reservation does not have status Confirmed (A1)
     */
    public void checkCancellable(String reservationCode) {
        ChangeableReservation reservation = reservationRepository.findChangeable(reservationCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown reservation " + reservationCode));
        checkConfirmed(reservation);
    }

    /**
     * Steps 7 and 8, A3: checks the status again, records the cancellation and sends the cancellation email.
     *
     * @return true when the Email Service accepted the cancellation email; false when it is being retried (A4)
     * @throws ReservationNotCancellableException when the status changed since step 4 (A3)
     */
    public boolean cancel(String reservationCode) {
        LocalDateTime cancelledAt = LocalDateTime.now(clock);
        ChangeableReservation cancelled = transactionTemplate.execute(_ -> {
            ChangeableReservation reservation = reservationRepository.findChangeableForUpdate(reservationCode)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown reservation " + reservationCode));
            // A3: the status is checked again under a lock when the receptionist confirms
            checkConfirmed(reservation);
            // UC-006 BR-002: No cancellation fee is recorded and no payment is processed.
            reservationRepository.cancel(reservationCode, cancelledAt);
            return reservation;
        });

        // UC-006 BR-003: Sent right after the cancellation, retried for at most 24 hours; never undoes it.
        return cancellationEmailSender.send(cancelled.guestEmail(), cancelled.reservationCode(),
                cancelled.arrivalDate(), cancelled.departureDate(), cancelled.roomTypeName(), cancelledAt);
    }

    // UC-006 BR-001: Any reservation with status Confirmed can be cancelled, also after the online deadline and on
    // the arrival date.
    private static void checkConfirmed(ChangeableReservation reservation) {
        if (reservation.status() != ReservationStatus.CONFIRMED) {
            throw new ReservationNotCancellableException(ReservationNotCancellableException.Reason.NOT_CONFIRMED);
        }
    }
}
