package ch.martinelli.edu.hrs.reservation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * UC-001 Reserve Room Online: searches available room types and records an online reservation.
 */
@Service
public class OnlineReservationService {

    public static final int MAX_DAYS_AHEAD = 365;
    public static final int MIN_GUESTS = 1;
    public static final int MAX_GUESTS = 10;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final RoomTypeAvailabilityRepository availabilityRepository;
    private final NightlyRates nightlyRates;
    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationCodeGenerator codeGenerator;
    private final ConfirmationEmailSender confirmationEmailSender;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public OnlineReservationService(RoomTypeAvailabilityRepository availabilityRepository, NightlyRates nightlyRates,
                                    GuestRepository guestRepository, ReservationRepository reservationRepository,
                                    ReservationCodeGenerator codeGenerator,
                                    ConfirmationEmailSender confirmationEmailSender,
                                    TransactionTemplate transactionTemplate, Clock clock) {
        this.availabilityRepository = availabilityRepository;
        this.nightlyRates = nightlyRates;
        this.guestRepository = guestRepository;
        this.reservationRepository = reservationRepository;
        this.codeGenerator = codeGenerator;
        this.confirmationEmailSender = confirmationEmailSender;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * Step 3: the room types available for every night of the date range that fit the number of guests.
     */
    public List<AvailableRoomType> searchAvailableRoomTypes(LocalDate arrivalDate, LocalDate departureDate,
                                                            int numberOfGuests) {
        checkSearch(arrivalDate, departureDate, numberOfGuests);
        long nights = nights(arrivalDate, departureDate);
        List<RoomTypeOffer> offers = availabilityRepository.findAvailableRoomTypes(arrivalDate, departureDate,
                numberOfGuests);
        // UC-001 BR-004: Every night is priced at the nightly rate valid for it (UC-025 BR-003).
        Map<Long, List<NightlyRate>> rates = nightlyRates.of(offers.stream().map(RoomTypeOffer::roomTypeId).toList(),
                arrivalDate, departureDate);
        return offers.stream()
                .map(offer -> {
                    List<NightPrice> nightPrices = NightPrice.of(rates.get(offer.roomTypeId()));
                    return new AvailableRoomType(offer.roomTypeId(), offer.name(), offer.description(),
                            offer.maxGuests(), lowest(nightPrices), highest(nightPrices), nights, nightPrices,
                            NightPrice.total(nightPrices));
                })
                .toList();
    }

    /**
     * Steps 8 to 10: checks the request, records guest and reservation, and sends the confirmation email.
     *
     * @throws RoomTypeNoLongerAvailableException when the room type was taken since the search (A5)
     * @throws IllegalArgumentException when the dates, number of guests or guest details are invalid
     */
    public ReservationConfirmation reserve(OnlineReservationRequest request) {
        checkSearch(request.arrivalDate(), request.departureDate(), request.numberOfGuests());
        GuestDetails guest = checkGuestDetails(request.guestDetails());
        LocalDateTime recordedAt = LocalDateTime.now(clock);

        ReservationConfirmation recorded = transactionTemplate.execute(_ -> record(request, guest, recordedAt));

        boolean emailAccepted = confirmationEmailSender.send(guest.email(), recorded.reservationCode(),
                recorded.arrivalDate(), recorded.departureDate(), recorded.roomTypeName(),
                recorded.numberOfGuests(), recorded.totalPrice(), recorded.onlineCancellationDeadline(), recordedAt);
        return new ReservationConfirmation(recorded.reservationCode(), recorded.arrivalDate(),
                recorded.departureDate(), recorded.roomTypeName(), recorded.numberOfGuests(), recorded.nights(),
                recorded.totalPrice(), recorded.onlineCancellationDeadline(), emailAccepted);
    }

    private ReservationConfirmation record(OnlineReservationRequest request, GuestDetails guest,
                                           LocalDateTime recordedAt) {
        // UC-001 BR-005: Lock the room type so that the check below and the insert are one indivisible action.
        availabilityRepository.lockRoomType(request.roomTypeId());
        RoomTypeOffer roomType = availabilityRepository
                .findAvailableRoomType(request.roomTypeId(), request.arrivalDate(), request.departureDate(),
                        request.numberOfGuests())
                .orElseThrow(RoomTypeNoLongerAvailableException::new);

        // UC-001 BR-006: A guest is identified by their email address, compared case-insensitively.
        Long guestId = guestRepository.findIdByEmail(guest.email())
                .map(existingId -> {
                    guestRepository.updateNameAndPhone(existingId, guest.firstName(), guest.lastName(),
                            guest.phone());
                    return existingId;
                })
                .orElseGet(() -> guestRepository.insert(guest.firstName(), guest.lastName(), guest.email(),
                        guest.phone()));

        String reservationCode = uniqueReservationCode();
        long nights = nights(request.arrivalDate(), request.departureDate());
        // UC-001 BR-004: Every night gets the nightly rate valid for it when the reservation is recorded; the total
        // price is the sum of the night prices.
        List<NightPrice> nightPrices = NightPrice.of(nightlyRates.of(roomType.roomTypeId(), request.arrivalDate(),
                request.departureDate()));
        BigDecimal totalPrice = NightPrice.total(nightPrices);

        // UC-001 step 9, UC-003 BR-001: The online cancellation deadline is fixed when the reservation is recorded.
        LocalDateTime onlineCancellationDeadline = OnlineCancellationDeadline.forArrival(request.arrivalDate());
        reservationRepository.insert(reservationCode, guestId, roomType.roomTypeId(), request.arrivalDate(),
                request.departureDate(), request.numberOfGuests(), nightPrices, ReservationStatus.CONFIRMED,
                ReservationChannel.ONLINE, recordedAt, onlineCancellationDeadline);

        return new ReservationConfirmation(reservationCode, request.arrivalDate(), request.departureDate(),
                roomType.name(), request.numberOfGuests(), nights, totalPrice, onlineCancellationDeadline, false);
    }

    // UC-001 BR-007: Every reservation receives a unique reservation code.
    private String uniqueReservationCode() {
        String code;
        do {
            code = codeGenerator.newCode();
        } while (reservationRepository.existsByReservationCode(code));
        return code;
    }

    static BigDecimal lowest(List<NightPrice> nightPrices) {
        return nightPrices.stream().map(NightPrice::price).min(BigDecimal::compareTo).orElseThrow();
    }

    static BigDecimal highest(List<NightPrice> nightPrices) {
        return nightPrices.stream().map(NightPrice::price).max(BigDecimal::compareTo).orElseThrow();
    }

    static long nights(LocalDate arrivalDate, LocalDate departureDate) {
        return ChronoUnit.DAYS.between(arrivalDate, departureDate);
    }

    // UC-001 BR-001: Arrival today up to 365 days ahead, departure after arrival.
    // UC-001 BR-003: The number of guests is between 1 and 10.
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

    // UC-001 BR-008: First name, last name, email address and phone number are required.
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
