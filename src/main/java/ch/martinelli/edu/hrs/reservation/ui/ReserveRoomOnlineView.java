package ch.martinelli.edu.hrs.reservation.ui;

import ch.martinelli.edu.hrs.reservation.AvailableRoomType;
import ch.martinelli.edu.hrs.reservation.GuestDetails;
import ch.martinelli.edu.hrs.reservation.NightPrice;
import ch.martinelli.edu.hrs.reservation.OnlineReservationRequest;
import ch.martinelli.edu.hrs.reservation.OnlineReservationService;
import ch.martinelli.edu.hrs.reservation.ReservationConfirmation;
import ch.martinelli.edu.hrs.reservation.RoomTypeNoLongerAvailableException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

/**
 * UC-001 Reserve Room Online: the guest searches available room types, selects one, enters their details and
 * confirms the reservation.
 * <p>
 * Nothing is stored before the guest confirms, so leaving the view discards the entered dates and details (A6).
 */
@Route("reserve")
@RouteAlias("")
@PageTitle("Reserve a Room")
// UC-022 BR-003: Guest functions stay reachable without login.
@AnonymousAllowed
public class ReserveRoomOnlineView extends VerticalLayout {

    static final String NO_ROOM_AVAILABLE =
            "No room is available for these dates and number of guests. Please change your search.";
    static final String ROOM_TYPE_NO_LONGER_AVAILABLE =
            "The selected room type is no longer available for these dates. Please choose another room type.";
    static final String EMAIL_MAY_BE_DELAYED =
            "Please note your reservation code. The confirmation email may be delayed.";
    static final String PRIVACY_NOTICE = """
            Privacy notice: We collect your first name, last name, email address and phone number only to \
            handle your reservation and your stay. We use your email address to send you the confirmation and to \
            let you find your reservation again. We do not ask for or store any payment data; you pay at the \
            hotel.""";

    private final OnlineReservationService reservationService;
    private final Clock clock;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    // Step 2: search
    private final DatePicker arrivalDate = new DatePicker("Arrival date");
    private final DatePicker departureDate = new DatePicker("Departure date");
    private final IntegerField numberOfGuests = new IntegerField("Number of guests");
    private final Button searchButton = new Button("Search");
    private final Binder<SearchInput> searchBinder = new Binder<>();
    private final SearchInput searchInput = new SearchInput();

    // Step 3: available room types
    private final Div results = new Div();

    // Steps 5 and 6: summary, guest details, privacy notice
    private final VerticalLayout reservationSection = new VerticalLayout();
    private final Div summary = new Div();
    private final TextField firstName = new TextField("First name");
    private final TextField lastName = new TextField("Last name");
    private final EmailField email = new EmailField("Email address");
    private final TextField phone = new TextField("Phone number");
    private final Button confirmButton = new Button("Confirm reservation");
    private final Binder<GuestInput> guestBinder = new Binder<>();
    private final GuestInput guestInput = new GuestInput();

    private Search currentSearch;
    private AvailableRoomType selectedRoomType;

    public ReserveRoomOnlineView(OnlineReservationService reservationService, Clock clock) {
        this.reservationService = reservationService;
        this.clock = clock;

        setMaxWidth("60rem");
        getStyle().set("margin", "0 auto");

        add(new H1("Reserve a Room"));
        add(createSearchForm());

        results.setWidthFull();
        results.getElement().setAttribute("aria-live", "polite");
        add(results);

        createReservationSection();
        add(reservationSection);
    }

    private FormLayout createSearchForm() {
        LocalDate today = LocalDate.now(clock);

        arrivalDate.setRequiredIndicatorVisible(true);
        arrivalDate.setHelperText("Today or up to " + OnlineReservationService.MAX_DAYS_AHEAD + " days ahead");
        departureDate.setRequiredIndicatorVisible(true);
        departureDate.setHelperText("After the arrival date");
        numberOfGuests.setRequiredIndicatorVisible(true);
        numberOfGuests.setStepButtonsVisible(true);
        numberOfGuests.setHelperText("Between " + OnlineReservationService.MIN_GUESTS + " and "
                + OnlineReservationService.MAX_GUESTS);
        numberOfGuests.setValue(OnlineReservationService.MIN_GUESTS);

        // UC-001 BR-001: The arrival date is today or at most 365 days ahead.
        searchBinder.forField(arrivalDate)
                .asRequired("Enter the arrival date")
                .withValidator(date -> !date.isBefore(today)
                                && !date.isAfter(today.plusDays(OnlineReservationService.MAX_DAYS_AHEAD)),
                        "The arrival date must be between today and "
                                + dateFormatter.format(today.plusDays(OnlineReservationService.MAX_DAYS_AHEAD)))
                .bind(SearchInput::getArrivalDate, SearchInput::setArrivalDate);
        // UC-001 BR-001: The departure date is after the arrival date.
        searchBinder.forField(departureDate)
                .asRequired("Enter the departure date")
                .withValidator(date -> arrivalDate.getValue() == null || date.isAfter(arrivalDate.getValue()),
                        "The departure date must be after the arrival date")
                .bind(SearchInput::getDepartureDate, SearchInput::setDepartureDate);
        // UC-001 BR-003: The number of guests is between 1 and 10.
        searchBinder.forField(numberOfGuests)
                .asRequired("Enter the number of guests")
                .withValidator(n -> n >= OnlineReservationService.MIN_GUESTS
                                && n <= OnlineReservationService.MAX_GUESTS,
                        "The number of guests must be between " + OnlineReservationService.MIN_GUESTS + " and "
                                + OnlineReservationService.MAX_GUESTS)
                .bind(SearchInput::getNumberOfGuests, SearchInput::setNumberOfGuests);
        searchBinder.setBean(searchInput);

        // A3: a changed search makes the shown room types and the summary out of date
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

    private void createReservationSection() {
        firstName.setMaxLength(100);
        lastName.setMaxLength(100);
        email.setMaxLength(255);
        phone.setMaxLength(30);
        firstName.setAutocomplete(Autocomplete.GIVEN_NAME);
        lastName.setAutocomplete(Autocomplete.FAMILY_NAME);
        email.setAutocomplete(Autocomplete.EMAIL);
        phone.setAutocomplete(Autocomplete.TEL);

        // UC-001 BR-008: First name, last name, email address and phone number are required; blank input counts as missing.
        guestBinder.forField(firstName)
                .asRequired("Enter your first name")
                .withValidator(value -> !value.isBlank(), "Enter your first name")
                .bind(GuestInput::getFirstName, GuestInput::setFirstName);
        guestBinder.forField(lastName)
                .asRequired("Enter your last name")
                .withValidator(value -> !value.isBlank(), "Enter your last name")
                .bind(GuestInput::getLastName, GuestInput::setLastName);
        guestBinder.forField(email)
                .asRequired("Enter your email address")
                .withValidator(value -> !value.isBlank(), "Enter your email address")
                .withValidator(new EmailValidator("Enter a valid email address, e.g. name@example.com"))
                .bind(GuestInput::getEmail, GuestInput::setEmail);
        guestBinder.forField(phone)
                .asRequired("Enter your phone number")
                .withValidator(value -> !value.isBlank(), "Enter your phone number")
                .bind(GuestInput::getPhone, GuestInput::setPhone);
        guestBinder.setBean(guestInput);

        // UC-001 BR-008: Only these four guest details are collected.
        // UC-001 BR-010: The online reservation asks for no payment data.
        FormLayout guestForm = new FormLayout(firstName, lastName, email, phone);
        guestForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("30rem", 2));
        guestForm.setWidthFull();

        // UC-001 BR-009: The privacy notice is displayed before the guest can confirm.
        Paragraph privacyNotice = new Paragraph(PRIVACY_NOTICE);
        privacyNotice.setId("privacy-notice");
        privacyNotice.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        confirmButton.getElement().setAttribute("aria-describedby", "privacy-notice");

        confirmButton.addThemeVariants(ButtonVariant.PRIMARY);
        confirmButton.addClickListener(_ -> confirm());

        reservationSection.setPadding(false);
        reservationSection.add(new H2("Your reservation"), summary, guestForm, privacyNotice, confirmButton);
        reservationSection.setVisible(false);
    }

    /**
     * Step 3, A1, A2: shows the available room types for the entered search.
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
        reservationSection.setVisible(false);
        results.removeAll();

        if (message != null) {
            results.add(errorMessage(message));
        }

        List<AvailableRoomType> roomTypes = reservationService.searchAvailableRoomTypes(
                currentSearch.arrivalDate(), currentSearch.departureDate(), currentSearch.numberOfGuests());
        if (roomTypes.isEmpty()) {
            Paragraph noRoom = new Paragraph(NO_ROOM_AVAILABLE);
            noRoom.setId("no-room-available");
            results.add(noRoom);
            return;
        }

        results.add(new H2("Available room types"));
        roomTypes.forEach(roomType -> results.add(roomTypeCard(roomType)));
    }

    private Div roomTypeCard(AvailableRoomType roomType) {
        Div card = new Div();
        card.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderColor.CONTRAST_10,
                LumoUtility.BorderRadius.MEDIUM, LumoUtility.Padding.MEDIUM, LumoUtility.Margin.Bottom.MEDIUM);

        H3 name = new H3(roomType.name());
        card.add(name);
        if (roomType.description() != null && !roomType.description().isBlank()) {
            card.add(new Paragraph(roomType.description()));
        }
        // Step 3: the lowest and the highest nightly rate when the nights have different nightly rates (BR-004)
        card.add(new Paragraph("Nightly rate: " + rateText(roomType.lowestNightlyRate(),
                roomType.highestNightlyRate())));
        card.add(new Paragraph("Total price for " + nights(roomType.nights()) + ": " + chf(roomType.totalPrice())));

        Button select = new Button("Select");
        select.getElement().setAttribute("aria-label", "Select " + roomType.name());
        select.addClickListener(_ -> selectRoomType(roomType));
        card.add(select);
        return card;
    }

    /**
     * Steps 4 and 5: shows the summary and the guest details form.
     */
    private void selectRoomType(AvailableRoomType roomType) {
        selectedRoomType = roomType;
        summary.removeAll();
        summary.add(
                new Paragraph("Arrival date: " + dateFormatter.format(currentSearch.arrivalDate())),
                new Paragraph("Departure date: " + dateFormatter.format(currentSearch.departureDate())),
                new Paragraph("Room type: " + roomType.name()),
                new Paragraph("Number of guests: " + currentSearch.numberOfGuests()),
                new Paragraph("Number of nights: " + roomType.nights()),
                nightPriceList(roomType.nightPrices()),
                new Paragraph("Total price: " + chf(roomType.totalPrice())));
        reservationSection.setVisible(true);
        firstName.focus();
    }

    /**
     * Step 5: the night price of every night (BR-004).
     */
    private UnorderedList nightPriceList(List<NightPrice> nightPrices) {
        UnorderedList list = new UnorderedList();
        list.setId("night-prices");
        nightPrices.forEach(nightPrice -> list.add(new ListItem("Night of "
                + dateFormatter.format(nightPrice.night()) + ": " + chf(nightPrice.price()))));
        return list;
    }

    private static String rateText(BigDecimal lowest, BigDecimal highest) {
        return lowest.compareTo(highest) == 0 ? chf(lowest) : chf(lowest) + " to " + chf(highest);
    }

    /**
     * Steps 7 to 11, A4, A5, A8.
     */
    private void confirm() {
        // A4: invalid details are marked; the entered values stay in the fields
        if (selectedRoomType == null || !guestBinder.validate().isOk()) {
            return;
        }
        OnlineReservationRequest request = new OnlineReservationRequest(currentSearch.arrivalDate(),
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
        add(new H1("Reservation confirmed"));

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
                new Paragraph("Number of guests: " + confirmation.numberOfGuests()),
                new Paragraph("You pay at the hotel."));

        if (confirmation.confirmationEmailAccepted()) {
            add(new Paragraph("We have sent a confirmation email with your reservation code."));
        } else {
            // A8: advise the guest to note the reservation code
            Paragraph advice = new Paragraph(EMAIL_MAY_BE_DELAYED);
            advice.setId("email-delayed");
            add(advice);
        }
    }

    private void searchChanged() {
        if (currentSearch == null) {
            return;
        }
        currentSearch = null;
        selectedRoomType = null;
        results.removeAll();
        reservationSection.setVisible(false);
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
