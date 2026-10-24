package ch.martinelli.edu.hrs.reservation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * UC-004 Create Reservation: the receptionist searches available room types and records a reservation for a phone or
 * walk-in guest.
 */
@Service
public class ReceptionReservationService {

    public static final int MAX_DAYS_AHEAD = 365;
    public static final int MIN_GUESTS = 1;
    public static final int MAX_GUESTS = 10;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final RoomTypeAvailabilityRepository availabilityRepository;
    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationCodeGenerator codeGenerator;
    private final ConfirmationEmailSender confirmationEmailSender;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public ReceptionReservationService(RoomTypeAvailabilityRepository availabilityRepository,
                                       GuestRepository guestRepository, ReservationRepository reservationRepository,
                                       ReservationCodeGenerator codeGenerator,
                                       ConfirmationEmailSender confirmationEmailSender,
                                       TransactionTemplate transactionTemplate, Clock clock) {
        this.availabilityRepository = availabilityRepository;
        this.guestRepository = guestRepository;
        this.reservationRepository = reservationRepository;
        this.codeGenerator = codeGenerator;
        this.confirmationEmailSender = confirmationEmailSender;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * Step 3: the room types available for every night of the date range that fit the number of guests, with the
     * number of available rooms.
     */
    public List<ReceptionRoomType> searchAvailableRoomTypes(LocalDate arrivalDate, LocalDate departureDate,
                                                            int numberOfGuests) {
        checkSearch(arrivalDate, departureDate, numberOfGuests);
        long nights = OnlineReservationService.nights(arrivalDate, departureDate);
        // UC-004 BR-002: Same availability and number-of-guests rules as online (UC-001 BR-002, UC-001 BR-003).
        return availabilityRepository.findAvailableRoomTypesWithRoomCount(arrivalDate, departureDate, numberOfGuests)
                .stream()
                .map(availability -> {
                    RoomTypeOffer offer = availability.roomType();
                    return new ReceptionRoomType(offer.roomTypeId(), offer.name(), offer.maxGuests(),
                            offer.nightlyRate(), nights, OnlineReservationService.totalPrice(offer.nightlyRate(),
                            nights), availability.availableRooms());
                })
                .toList();
    }

    /**
     * Step 6: the stored details of the guest with this email address; empty when no guest has it (A3) or the
     * email address is not valid.
     */
    public Optional<GuestDetails> findGuest(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        // UC-004 BR-005: The email address is compared case-insensitively.
        String normalisedEmail = email.strip().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalisedEmail).matches()) {
            return Optional.empty();
        }
        return guestRepository.findDetailsByEmail(normalisedEmail);
    }

    /**
     * Steps 8 to 11: checks the request, records guest and reservation, and sends the confirmation email.
     *
     * @throws RoomTypeNoLongerAvailableException when the room type was taken since the search (A5)
     * @throws IllegalArgumentException when the dates, number of guests or guest details are invalid
     */
    public ReservationConfirmation reserve(ReceptionReservationRequest request) {
        checkSearch(request.arrivalDate(), request.departureDate(), request.numberOfGuests());
        GuestDetails guest = checkGuestDetails(request.guestDetails());
        LocalDateTime recordedAt = LocalDateTime.now(clock);

        ReservationConfirmation recorded = transactionTemplate.execute(_ -> record(request, guest, recordedAt));

        // UC-004 BR-007: The confirmation email is sent right after recording and retried for at most 24 hours;
        // an email that could not be sent never cancels or changes the reservation.
        boolean emailAccepted = confirmationEmailSender.send(guest.email(), recorded.reservationCode(),
                recorded.arrivalDate(), recorded.departureDate(), recorded.roomTypeName(),
                recorded.numberOfGuests(), recorded.totalPrice(), recordedAt);
        return new ReservationConfirmation(recorded.reservationCode(), recorded.arrivalDate(),
                recorded.departureDate(), recorded.roomTypeName(), recorded.numberOfGuests(), recorded.nights(),
                recorded.totalPrice(), emailAccepted);
    }

    private ReservationConfirmation record(ReceptionReservationRequest request, GuestDetails guest,
                                           LocalDateTime recordedAt) {
        // UC-004 BR-003: Lock the room type so that the availability check and the insert are one indivisible
        // action shared with online reservations (UC-001 BR-005).
        availabilityRepository.lockRoomType(request.roomTypeId());
        RoomTypeOffer roomType = availabilityRepository
                .findAvailableRoomType(request.roomTypeId(), request.arrivalDate(), request.departureDate(),
                        request.numberOfGuests())
                .orElseThrow(RoomTypeNoLongerAvailableException::new);

        // UC-004 BR-005: An existing guest is linked and their name and phone number are replaced.
        Long guestId = guestRepository.findIdByEmail(guest.email())
                .map(existingId -> {
                    guestRepository.updateNameAndPhone(existingId, guest.firstName(), guest.lastName(),
                            guest.phone());
                    return existingId;
                })
                .orElseGet(() -> guestRepository.insert(guest.firstName(), guest.lastName(), guest.email(),
                        guest.phone()));

        String reservationCode = uniqueReservationCode();
        long nights = OnlineReservationService.nights(request.arrivalDate(), request.departureDate());
        // UC-004 BR-004: Total price = nights times the nightly rate in CHF, fixed when the reservation is recorded.
        BigDecimal totalPrice = OnlineReservationService.totalPrice(roomType.nightlyRate(), nights);

        reservationRepository.insert(reservationCode, guestId, roomType.roomTypeId(), request.arrivalDate(),
                request.departureDate(), request.numberOfGuests(), totalPrice, ReservationStatus.CONFIRMED,
                ReservationChannel.RECEPTION, recordedAt);

        return new ReservationConfirmation(reservationCode, request.arrivalDate(), request.departureDate(),
                roomType.name(), request.numberOfGuests(), nights, totalPrice, false);
    }

    private String uniqueReservationCode() {
        String code;
        do {
            code = codeGenerator.newCode();
        } while (reservationRepository.existsByReservationCode(code));
        return code;
    }

    // UC-004 BR-001: Arrival today (walk-in) up to 365 days ahead, departure after arrival.
    // UC-004 BR-002: The number of guests is between 1 and 10 (UC-001 BR-003).
    private void checkSearch(LocalDate arrivalDate, LocalDate departureDate, int numberOfGuests) {
        LocalDate today = LocalDate.now(clock);
        if (arrivalDate == null || arrivalDate.isBefore(today) || arrivalDate.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new IllegalArgumentException("The arrival date must be between today and " + MAX_DAYS_AHEAD
                    + " days ahead");
        }
        if (departureDate == null || !departureDate.isAfter(arrivalDate)) {
            throw new IllegalArgumentException("The departure date must be after the arrival date");
        }
        if (numberOfGuests < MIN_GUESTS || numberOfGuests > MAX_GUESTS) {
            throw new IllegalArgumentException("The number of guests must be between " + MIN_GUESTS + " and "
                    + MAX_GUESTS);
        }
    }

    // UC-004 BR-006: First name, last name, email address and phone number are required.
    private static GuestDetails checkGuestDetails(GuestDetails details) {
        if (details == null || isBlank(details.firstName()) || isBlank(details.lastName())
                || isBlank(details.email()) || isBlank(details.phone())) {
            throw new IllegalArgumentException("First name, last name, email address and phone number are required");
        }
        String email = details.email().strip().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("The email address is not valid");
        }
        return new GuestDetails(details.firstName().strip(), details.lastName().strip(), email,
                details.phone().strip());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
