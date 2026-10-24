package ch.martinelli.edu.hrs.reservation.ui;

import ch.martinelli.edu.hrs.reservation.OwnReservation;
import ch.martinelli.edu.hrs.reservation.ReservationLookupLockedException;
import ch.martinelli.edu.hrs.reservation.ReservationLookupService;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

/**
 * UC-002 View Own Reservation: the guest looks up their reservation with the reservation code and email address.
 * Nothing is changed by this view except the count of failed lookup attempts.
 */
@Route("reservation")
@PageTitle("My Reservation")
public class ViewOwnReservationView extends VerticalLayout {

    static final String NOT_FOUND =
            "No reservation matches this reservation code and email address. Please check your input.";
    static final String LOCKED =
            "Too many failed lookups. The reservation lookup is locked for 15 minutes. Please try again later.";

    private final ReservationLookupService lookupService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private final TextField reservationCode = new TextField("Reservation code");
    private final EmailField email = new EmailField("Email address");
    private final Button lookUpButton = new Button("Show reservation");
    private final Binder<LookupInput> binder = new Binder<>();
    private final LookupInput lookupInput = new LookupInput();

    private final Div message = new Div();
    private final Div reservationDetails = new Div();

    public ViewOwnReservationView(ReservationLookupService lookupService) {
        this.lookupService = lookupService;

        setMaxWidth("60rem");
        getStyle().set("margin", "0 auto");

        add(new H1("My Reservation"));
        add(createLookupForm());

        message.setWidthFull();
        message.getElement().setAttribute("aria-live", "polite");
        add(message);

        reservationDetails.setWidthFull();
        add(reservationDetails);
    }

    private FormLayout createLookupForm() {
        reservationCode.setMaxLength(20);
        reservationCode.setRequiredIndicatorVisible(true);
        reservationCode.setHelperText("The code from your confirmation");
        email.setMaxLength(255);
        email.setRequiredIndicatorVisible(true);
        email.setAutocomplete(Autocomplete.EMAIL);

        // A1: missing input is marked
        binder.forField(reservationCode)
                .asRequired("Enter the reservation code")
                .bind(LookupInput::getReservationCode, LookupInput::setReservationCode);
        binder.forField(email)
                .asRequired("Enter your email address")
                .bind(LookupInput::getEmail, LookupInput::setEmail);
        binder.setBean(lookupInput);

        lookUpButton.addThemeVariants(ButtonVariant.PRIMARY);
        lookUpButton.addClickListener(_ -> lookUp());
        lookUpButton.addClickShortcut(Key.ENTER);

        FormLayout form = new FormLayout(reservationCode, email, lookUpButton);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("40rem", 3));
        form.setWidthFull();
        return form;
    }

    /**
     * Steps 3 and 4, A1 to A3.
     */
    private void lookUp() {
        message.removeAll();
        reservationDetails.removeAll();
        if (!binder.validate().isOk()) {
            return;
        }
        try {
            Optional<OwnReservation> reservation = lookupService.lookUp(lookupInput.getReservationCode(),
                    lookupInput.getEmail(), networkAddress());
            reservation.ifPresentOrElse(this::showReservation,
                    // UC-002 BR-002: The same answer whether the code, the email address or both are wrong.
                    () -> message.add(errorMessage(NOT_FOUND)));
        } catch (ReservationLookupLockedException _) {
            message.add(errorMessage(LOCKED));
        }
    }

    private void showReservation(OwnReservation reservation) {
        reservationDetails.add(new H2("Your reservation"));
        Paragraph code = new Paragraph("Reservation code: " + reservation.reservationCode());
        code.setId("reservation-code");
        reservationDetails.add(code,
                new Paragraph("Status: " + statusText(reservation.status())),
                new Paragraph("Arrival date: " + dateFormatter.format(reservation.arrivalDate())),
                new Paragraph("Departure date: " + dateFormatter.format(reservation.departureDate())),
                new Paragraph("Number of nights: " + reservation.nights()),
                new Paragraph("Room type: " + reservation.roomTypeName()),
                new Paragraph("Number of guests: " + reservation.numberOfGuests()),
                new Paragraph("Total price: " + chf(reservation.totalPrice())));

        // A4: offer the online cancellation (UC-003)
        if (lookupService.canBeCancelledOnline(reservation)) {
            Button cancel = new Button("Cancel reservation");
            cancel.setId("cancel-reservation");
            cancel.addThemeVariants(ButtonVariant.ERROR);
            reservationDetails.add(cancel);
        }
    }

    private static String networkAddress() {
        VaadinSession session = VaadinSession.getCurrent();
        String address = session == null || session.getBrowser() == null ? null : session.getBrowser().getAddress();
        return address == null ? "unknown" : address;
    }

    static String statusText(ReservationStatus status) {
        return switch (status) {
            case CONFIRMED -> "Confirmed";
            case CHECKED_IN -> "Checked In";
            case CHECKED_OUT -> "Checked Out";
            case CANCELLED -> "Cancelled";
            case NO_SHOW -> "No-Show";
        };
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

    private static class LookupInput {
        private String reservationCode;
        private String email;

        String getReservationCode() {
            return reservationCode;
        }

        void setReservationCode(String reservationCode) {
            this.reservationCode = reservationCode;
        }

        String getEmail() {
            return email;
        }

        void setEmail(String email) {
            this.email = email;
        }
    }
}
