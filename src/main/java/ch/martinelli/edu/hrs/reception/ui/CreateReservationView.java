package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.reservation.GuestDetails;
import ch.martinelli.edu.hrs.reservation.ReceptionReservationRequest;
import ch.martinelli.edu.hrs.reservation.ReceptionReservationService;
import ch.martinelli.edu.hrs.reservation.ReceptionRoomType;
import ch.martinelli.edu.hrs.reservation.ReservationConfirmation;
import ch.martinelli.edu.hrs.reservation.RoomTypeNoLongerAvailableException;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StaffLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Optional;

/**
 * UC-004 Create Reservation: the receptionist searches available room types, selects one, enters the guest's details
 * and confirms the reservation for a phone or walk-in guest.
 * <p>
 * Nothing is stored before the receptionist confirms, so leaving the view discards the entered data (A6).
 */
@Route(value = "new-reservation", layout = StaffLayout.class)
@PageTitle("New Reservation")
// NFR-009: Only the receptionist can create reservations.
@RolesAllowed(StaffRole.RECEPTIONIST_ROLE)
public class CreateReservationView extends VerticalLayout {

    static final String NO_ROOM_TYPE_AVAILABLE =
            "No room type is available for these dates and number of guests.";
    static final String ROOM_TYPE_NO_LONGER_AVAILABLE =
            "The selected room type is no longer available for these dates. Please select another room type.";
    static final String TELL_GUEST_RESERVATION_CODE =
            "The confirmation email could not be sent yet and will be retried. Please tell the guest the "
                    + "reservation code directly.";

    private final ReceptionReservationService reservationService;
    private final Clock clock;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    // Step 2: dates and number of guests
    private final DatePicker arrivalDate = new DatePicker("Arrival date");
    private final DatePicker departureDate = new DatePicker("Departure date");
    private final IntegerField numberOfGuests = new IntegerField("Number of guests");
    private final Button searchButton = new Button("Search");
    private final Binder<SearchInput> searchBinder = new Binder<>();
    private final SearchInput searchInput = new SearchInput();

    // Step 3: available room types
    private final Div results = new Div();

    // Steps 5 to 8: selected room type and guest details
    private final VerticalLayout guestSection = new VerticalLayout();
    private final Div summary = new Div();
    private final EmailField email = new EmailField("Email address");
    private final TextField firstName = new TextField("First name");
    private final TextField lastName = new TextField("Last name");
    private final TextField phone = new TextField("Phone number");
    private final Paragraph guestStatus = new Paragraph();
    private final Button confirmButton = new Button("Confirm reservation");
    private final Binder<GuestInput> guestBinder = new Binder<>();
    private final GuestInput guestInput = new GuestInput();

    private Search currentSearch;
    private ReceptionRoomType selectedRoomType;
    private boolean guestDetailsFromStoredGuest;

    public CreateReservationView(ReceptionReservationService reservationService, Clock clock) {
        this.reservationService = reservationService;
        this.clock = clock;

        setMaxWidth("60rem");

        add(new H1("New Reservation"));
        add(createSearchForm());

        results.setWidthFull();
        results.getElement().setAttribute("aria-live", "polite");
        add(results);

        createGuestSection();
        add(guestSection);

        // A6: leaving the new reservation discards the entered data
        Button discardButton = new Button("Discard", _ -> UI.getCurrent().navigate(ArrivalsAndDeparturesView.class));
        discardButton.addThemeVariants(ButtonVariant.TERTIARY);
        add(discardButton);
    }

    private FormLayout createSearchForm() {
        LocalDate today = LocalDate.now(clock);

        arrivalDate.setRequiredIndicatorVisible(true);
        arrivalDate.setHelperText("Today or up to " + ReceptionReservationService.MAX_DAYS_AHEAD + " days ahead");
        departureDate.setRequiredIndicatorVisible(true);
        departureDate.setHelperText("After the arrival date");
        numberOfGuests.setRequiredIndicatorVisible(true);
        numberOfGuests.setStepButtonsVisible(true);
        numberOfGuests.setHelperText("Between " + ReceptionReservationService.MIN_GUESTS + " and "
                + ReceptionReservationService.MAX_GUESTS);

        // UC-004 BR-001: The arrival date is today (walk-in) or at most 365 days ahead.
        searchBinder.forField(arrivalDate)
                .asRequired("Enter the arrival date")
                .withValidator(date -> !date.isBefore(LocalDate.now(clock))
                                && !date.isAfter(LocalDate.now(clock).plusDays(ReceptionReservationService.MAX_DAYS_AHEAD)),
                        "The arrival date must be between today and "
                                + dateFormatter.format(today.plusDays(ReceptionReservationService.MAX_DAYS_AHEAD)))
                .bind(SearchInput::getArrivalDate, SearchInput::setArrivalDate);
        // UC-004 BR-001: The departure date is after the arrival date.
        searchBinder.forField(departureDate)
                .asRequired("Enter the departure date")
                .withValidator(date -> arrivalDate.getValue() == null || date.isAfter(arrivalDate.getValue()),
                        "The departure date must be after the arrival date")
                .bind(SearchInput::getDepartureDate, SearchInput::setDepartureDate);
        // UC-004 BR-002: The number of guests is between 1 and 10 (UC-001 BR-003).
        searchBinder.forField(numberOfGuests)
                .asRequired("Enter the number of guests")
                .withValidator(n -> n >= ReceptionReservationService.MIN_GUESTS
                                && n <= ReceptionReservationService.MAX_GUESTS,
                        "The number of guests must be between " + ReceptionReservationService.MIN_GUESTS + " and "
                                + ReceptionReservationService.MAX_GUESTS)
                .bind(SearchInput::getNumberOfGuests, SearchInput::setNumberOfGuests);
        // A walk-in guest arrives today
        searchInput.setArrivalDate(today);
        searchInput.setNumberOfGuests(ReceptionReservationService.MIN_GUESTS);
        searchBinder.setBean(searchInput);

        // A changed search makes the shown room types out of date
        arrivalDate.addValueChangeListener(_ -> searchChanged());
        departureDate.addValueChangeListener(_ -> searchChanged());
        numberOfGuests.addValueChangeListener(_ -> searchChanged());

        searchButton.addThemeVariants(ButtonVariant.PRIMARY);
        searchButton.addClickListener(_ -> search());

        FormLayout form = new FormLayout(arrivalDate, departureDate, numberOfGuests, searchButton);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("40rem", 4));
        form.setWidthFull();
        return form;
    }

    private void createGuestSection() {
        email.setMaxLength(255);
        firstName.setMaxLength(100);
        lastName.setMaxLength(100);
        phone.setMaxLength(30);
        email.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        // Step 6, A3: show the stored guest details for this email address
        email.addValueChangeListener(event -> lookUpGuest(event.getValue()));

        // UC-004 BR-006: First name, last name, email address and phone number are required; blank input counts as missing.
        guestBinder.forField(email)
                .asRequired("Enter the email address")
                .withValidator(value -> !value.isBlank(), "Enter the email address")
                .withValidator(new EmailValidator("Enter a valid email address, e.g. name@example.com"))
                .bind(GuestInput::getEmail, GuestInput::setEmail);
        guestBinder.forField(firstName)
                .asRequired("Enter the first name")
                .withValidator(value -> !value.isBlank(), "Enter the first name")
                .bind(GuestInput::getFirstName, GuestInput::setFirstName);
        guestBinder.forField(lastName)
                .asRequired("Enter the last name")
                .withValidator(value -> !value.isBlank(), "Enter the last name")
                .bind(GuestInput::getLastName, GuestInput::setLastName);
        guestBinder.forField(phone)
                .asRequired("Enter the phone number")
                .withValidator(value -> !value.isBlank(), "Enter the phone number")
                .bind(GuestInput::getPhone, GuestInput::setPhone);
        guestBinder.setBean(guestInput);

        guestStatus.setId("guest-status");
        guestStatus.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        // UC-004 BR-006: Only these four guest details are collected; no payment data is recorded.
        FormLayout guestForm = new FormLayout(email, firstName, lastName, phone);
        guestForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("30rem", 2));
        guestForm.setColspan(email, 2);
        guestForm.setWidthFull();

        confirmButton.addThemeVariants(ButtonVariant.PRIMARY);
        confirmButton.addClickListener(_ -> confirm());

        guestSection.setPadding(false);
        guestSection.add(new H2("Guest"), summary, guestForm, guestStatus, confirmButton);
        guestSection.setVisible(false);
    }

    /**
     * Step 3, A1, A2: shows the available room types for the entered dates and number of guests.
     */
    private void search() {
        if (!searchBinder.validate().isOk()) {
            return;
        }
        currentSearch = new Search(searchInput.getArrivalDate(), searchInput.getDepartureDate(),
                searchInput.getNumberOfGuests());
        showAvailableRoomTypes(null);
    }

    private void showAvailableRoomTypes(String message) {
        selectedRoomType = null;
        guestSection.setVisible(false);
        results.removeAll();

        if (message != null) {
            results.add(errorMessage(message));
        }

        List<ReceptionRoomType> roomTypes = reservationService.searchAvailableRoomTypes(
                currentSearch.arrivalDate(), currentSearch.departureDate(), currentSearch.numberOfGuests());
        if (roomTypes.isEmpty()) {
            // A2: continue at step 2
            Paragraph noRoomType = new Paragraph(NO_ROOM_TYPE_AVAILABLE);
            noRoomType.setId("no-room-type-available");
            results.add(noRoomType);
            return;
        }

        results.add(new H2("Available room types"));
        roomTypes.forEach(roomType -> results.add(roomTypeCard(roomType)));
    }

    private Div roomTypeCard(ReceptionRoomType roomType) {
        Div card = new Div();
        card.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderColor.CONTRAST_10,
                LumoUtility.BorderRadius.MEDIUM, LumoUtility.Padding.MEDIUM, LumoUtility.Margin.Bottom.MEDIUM);

        card.add(new H3(roomType.name()));
        // Step 3: the lowest and the highest nightly rate when the nights have different nightly rates
        card.add(new Paragraph("Nightly rate: " + rateText(roomType.lowestNightlyRate(),
                roomType.highestNightlyRate())));
        card.add(new Paragraph("Total price for " + nights(roomType.nights()) + ": " + chf(roomType.totalPrice())));
        card.add(new Paragraph("Available rooms: " + roomType.availableRooms()));

        Button select = new Button("Select");
        select.getElement().setAttribute("aria-label", "Select " + roomType.name());
        select.addClickListener(_ -> selectRoomType(roomType));
        card.add(select);
        return card;
    }

    private static String rateText(BigDecimal lowest, BigDecimal highest) {
        return lowest.compareTo(highest) == 0 ? chf(lowest) : chf(lowest) + " to " + chf(highest);
    }

    /**
     * Steps 4 and 5: shows the selected room type and the guest details; entered guest details are kept.
     */
    private void selectRoomType(ReceptionRoomType roomType) {
        selectedRoomType = roomType;
        summary.removeAll();
        summary.add(
                new Paragraph("Arrival date: " + dateFormatter.format(currentSearch.arrivalDate())),
                new Paragraph("Departure date: " + dateFormatter.format(currentSearch.departureDate())),
                new Paragraph("Room type: " + roomType.name()),
                new Paragraph("Number of guests: " + currentSearch.numberOfGuests()),
                new Paragraph("Total price: " + chf(roomType.totalPrice())));
        guestSection.setVisible(true);
        email.focus();
    }

    /**
     * Step 6 and A3: fills in the stored first name, last name and phone number of a known guest, or empty fields
     * for a guest not yet known.
     */
    private void lookUpGuest(String enteredEmail) {
        Optional<GuestDetails> storedGuest = reservationService.findGuest(enteredEmail);
        if (storedGuest.isPresent()) {
            GuestDetails guest = storedGuest.get();
            firstName.setValue(guest.firstName());
            lastName.setValue(guest.lastName());
            phone.setValue(guest.phone());
            guestDetailsFromStoredGuest = true;
            guestStatus.setText("Known guest: please check the stored details.");
        } else {
            // A3: empty fields for a guest not yet known; values typed in by the receptionist stay
            if (guestDetailsFromStoredGuest) {
                firstName.clear();
                lastName.clear();
                phone.clear();
                guestDetailsFromStoredGuest = false;
            }
            guestStatus.setText(enteredEmail == null || enteredEmail.isBlank() ? "" : "New guest.");
        }
    }

    /**
     * Steps 8 to 12, A4, A5, A7.
     */
    private void confirm() {
        // A4: invalid details are marked; the entered values stay in the fields
        if (selectedRoomType == null || !guestBinder.validate().isOk()) {
            return;
        }
        ReceptionReservationRequest request = new ReceptionReservationRequest(currentSearch.arrivalDate(),
                currentSearch.departureDate(), currentSearch.numberOfGuests(), selectedRoomType.roomTypeId(),
                new GuestDetails(guestInput.getFirstName(), guestInput.getLastName(), guestInput.getEmail(),
                        guestInput.getPhone()));
        try {
            showConfirmation(reservationService.reserve(request));
        } catch (RoomTypeNoLongerAvailableException _) {
            // A5: back to step 3; the guest details stay in the form
            showAvailableRoomTypes(ROOM_TYPE_NO_LONGER_AVAILABLE);
        }
    }

    private void showConfirmation(ReservationConfirmation confirmation) {
        removeAll();
        add(new H1("Reservation created"));

        // Step 12: the reservation code and the total price
        Paragraph code = new Paragraph("Reservation code: " + confirmation.reservationCode());
        code.setId("reservation-code");
        code.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);
        add(code);
        Paragraph total = new Paragraph("Total price: " + chf(confirmation.totalPrice()));
        total.setId("total-price");
        add(total,
                new Paragraph("Arrival date: " + dateFormatter.format(confirmation.arrivalDate())),
                new Paragraph("Departure date: " + dateFormatter.format(confirmation.departureDate())),
                new Paragraph("Room type: " + confirmation.roomTypeName()),
                new Paragraph("Number of guests: " + confirmation.numberOfGuests()));

        if (confirmation.confirmationEmailAccepted()) {
            add(new Paragraph("The confirmation email has been sent to the guest."));
        } else {
            // A7: advise the receptionist to tell the guest the reservation code directly
            Paragraph advice = new Paragraph(TELL_GUEST_RESERVATION_CODE);
            advice.setId("email-not-sent");
            add(advice);
        }

        Button done = new Button("Done", _ -> UI.getCurrent().navigate(ArrivalsAndDeparturesView.class));
        add(done);
    }

    private void searchChanged() {
        if (currentSearch == null) {
            return;
        }
        currentSearch = null;
        selectedRoomType = null;
        results.removeAll();
        guestSection.setVisible(false);
    }

    private static Paragraph errorMessage(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setId("error-message");
        paragraph.getElement().setAttribute("role", "alert");
        paragraph.addClassNames(LumoUtility.TextColor.ERROR);
        return paragraph;
    }

    private static String chf(BigDecimal amount) {
        return "CHF " + amount.setScale(2).toPlainString();
    }

    private static String nights(long nights) {
        return nights == 1 ? "1 night" : nights + " nights";
    }

    private record Search(LocalDate arrivalDate, LocalDate departureDate, int numberOfGuests) {
    }

    private static class SearchInput {
        private LocalDate arrivalDate;
        private LocalDate departureDate;
        private Integer numberOfGuests;

        LocalDate getArrivalDate() {
            return arrivalDate;
        }

        void setArrivalDate(LocalDate arrivalDate) {
            this.arrivalDate = arrivalDate;
        }

        LocalDate getDepartureDate() {
            return departureDate;
        }

        void setDepartureDate(LocalDate departureDate) {
            this.departureDate = departureDate;
        }

        Integer getNumberOfGuests() {
            return numberOfGuests;
        }

        void setNumberOfGuests(Integer numberOfGuests) {
            this.numberOfGuests = numberOfGuests;
        }
    }

    private static class GuestInput {
        private String firstName;
        private String lastName;
        private String email;
        private String phone;

        String getFirstName() {
            return firstName;
        }

        void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        String getLastName() {
            return lastName;
        }

        void setLastName(String lastName) {
            this.lastName = lastName;
        }

        String getEmail() {
            return email;
        }

        void setEmail(String email) {
            this.email = email;
        }

        String getPhone() {
            return phone;
        }

        void setPhone(String phone) {
            this.phone = phone;
        }
    }
}
