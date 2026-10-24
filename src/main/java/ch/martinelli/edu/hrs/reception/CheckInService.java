package ch.martinelli.edu.hrs.reception;

import ch.martinelli.edu.hrs.reservation.ChangeableReservation;
import ch.martinelli.edu.hrs.reservation.ReservationRepository;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UC-009 Check In Guest: assigns a clean, free room of the reserved room type to an arriving guest.
 */
@Service
public class CheckInService {

    private final ReservationRepository reservationRepository;
    private final RoomAssignmentRepository roomRepository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public CheckInService(ReservationRepository reservationRepository, RoomAssignmentRepository roomRepository,
                          TransactionTemplate transactionTemplate, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    public Optional<ChangeableReservation> findReservation(String reservationCode) {
        return reservationRepository.findChangeable(reservationCode);
    }

    /**
     * Step 3: checks status and arrival date.
     *
     * @throws CheckInNotPossibleException when the status is not Confirmed (A1) or today is not the arrival date (A2)
     */
    public void checkCheckInPossible(ChangeableReservation reservation) {
        // UC-009 step 3: Only a reservation with status Confirmed can be checked in.
        if (reservation.status() != ReservationStatus.CONFIRMED) {
            throw new CheckInNotPossibleException(CheckInNotPossibleException.Reason.NOT_CONFIRMED,
                    reservation.status());
        }
        // UC-009 BR-001: Only on the arrival date in the hotel's local time; before 14:00 is allowed.
        if (!reservation.arrivalDate().equals(LocalDate.now(clock))) {
            throw new CheckInNotPossibleException(CheckInNotPossibleException.Reason.NOT_ARRIVAL_DATE,
                    reservation.status());
        }
    }

    /**
     * Step 4 and A3: the free rooms of the reserved room type for the stay, clean and dirty, by room number. The
     * clean ones can be assigned; the first clean one is proposed.
     */
    public List<RoomCandidate> freeRooms(ChangeableReservation reservation) {
        return roomRepository.findFreeRooms(reservation.roomTypeId(), reservation.arrivalDate(),
                reservation.departureDate());
    }

    /**
     * Steps 6 and 7: checks again and records the check-in with the assigned room.
     *
     * @throws CheckInNotPossibleException when status or date no longer allow the check-in
     * @throws RoomNoLongerAssignableException when the room can no longer be assigned (A4)
     */
    public void checkIn(String reservationCode, Long roomId) {
        LocalDateTime checkedInAt = LocalDateTime.now(clock);
        transactionTemplate.executeWithoutResult(_ -> {
            ChangeableReservation reservation = reservationRepository.findChangeableForUpdate(reservationCode)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown reservation " + reservationCode));
            checkCheckInPossible(reservation);
            // UC-009 BR-003: Check and recording in one transaction with the room locked.
            if (!roomRepository.lockAndCheckAssignable(roomId, reservation.roomTypeId(), reservation.arrivalDate(),
                    reservation.departureDate())) {
                throw new RoomNoLongerAssignableException();
            }
            // UC-009 BR-004: The room stays with the reservation; its cleaning status is not changed.
            reservationRepository.checkIn(reservation.id(), roomId, checkedInAt);
        });
    }
}
