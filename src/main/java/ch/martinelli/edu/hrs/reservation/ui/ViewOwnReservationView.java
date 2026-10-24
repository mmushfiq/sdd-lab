package ch.martinelli.edu.hrs.reservation.ui;

import ch.martinelli.edu.hrs.reservation.OnlineCancellation;
import ch.martinelli.edu.hrs.reservation.OnlineCancellationService;
import ch.martinelli.edu.hrs.reservation.OwnReservation;
import ch.martinelli.edu.hrs.reservation.ReservationNotCancellableException;
import ch.martinelli.edu.hrs.reservation.ReservationLookupLockedException;
import ch.martinelli.edu.hrs.reservation.ReservationLookupService;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

/**
 * UC-002 View Own Reservation: the guest looks up their reservation with the reservation code and email address.
 * <p>
 * UC-003 Cancel Own Reservation: from the displayed reservation the guest cancels it online. Apart from that, the
 * view changes nothing except the count of failed lookup attempts.
 */
@Route("reservation")
@PageTitle("My Reservation")
// UC-022 BR-003: Guest functions stay reachable without login.
@AnonymousAllowed
public class ViewOwnReservationView extends VerticalLayout {

    static final String NOT_FOUND =
            "No reservation matches this reservation code and email address. Please check your input.";
    static final String LOCKED =
            "Too many failed lookups. The reservation lookup is locked for 15 minutes. Please try again later.";
    static final String CANCELLATION_CANNOT_BE_UNDONE = "The cancellation cannot be undone.";
    static final String CANCELLED = "Your reservation is cancelled. We have sent you a cancellation email.";
    static final String CANCELLED_EMAIL_DELAYED = "Your reservation is cancelled. The cancellation email may be delayed.";
    static final String DEADLINE_PASSED = "This reservation can no longer be cancelled online. "
            + "The hotel's reception can still cancel it.";

    private final ReservationLookupService lookupService;
    private final OnlineCancellationService cancellationService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private final TextField reservationCode = new TextField("Reservation code");
    private final EmailField email = new EmailField("Email address");
    private final Button lookUpButton = new Button("Show reservation");
    private final Binder<LookupInput> binder = new Binder<>();
    private final LookupInput lookupInput = new LookupInput();

    private final Div message = new Div();
    private final Div reservationDetails = new Div();

    /** Reservation code and email address of the reservation currently displayed. */
    private String foundReservationCode;
    private String foundEmail;

    public ViewOwnReservationView(ReservationLookupService lookupService,
                                  OnlineCancellationService cancellationService) {
        this.lookupService = lookupService;
        this.cancellationService = cancellationService;

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
            reservation.ifPresent(_ -> {
                foundReservationCode = lookupInput.getReservationCode();
                foundEmail = lookupInput.getEmail();
            });
            reservation.ifPresentOrElse(this::showReservation,
                    // UC-002 BR-002: The same answer whether the code, the email address or both are wrong.
                    () -> message.add(errorMessage(NOT_FOUND)));
        } catch (ReservationLookupLockedException _) {
            message.add(errorMessage(LOCKED));
        }
    }

    private void showReservation(OwnReservation reservation) {
        reservationDetails.removeAll();
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

        // UC-002 A4: offer the online cancellation (UC-003)
        if (lookupService.canBeCancelledOnline(reservation)) {
            Button cancel = new Button("Cancel reservation");
            cancel.setId("cancel-reservation");
            cancel.addThemeVariants(ButtonVariant.ERROR);
            cancel.addClickListener(_ -> requestCancellation(reservation));
            reservationDetails.add(cancel);
        } else {
            showWhyNotCancellable(reservation);
        }
    }

    /**
     * UC-003 steps 5 to 7.
     */
    private void requestCancellation(OwnReservation reservation) {
        message.removeAll();
        try {
            // UC-003 step 6
            cancellationService.checkCancellable(reservation);
        } catch (ReservationNotCancellableException _) {
            showReservation(reservation);
            return;
        }
        // UC-003 step 7: confirm and state that the cancellation cannot be undone
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Cancel reservation " + reservation.reservationCode() + "?");
        dialog.setText(CANCELLATION_CANNOT_BE_UNDONE);
        dialog.setConfirmText("Cancel reservation");
        dialog.setConfirmButtonTheme("error primary");
        // UC-003 A6: the guest keeps the reservation, which stays displayed unchanged
        dialog.setCancelable(true);
        dialog.setCancelText("Keep reservation");
        dialog.addConfirmListener(_ -> confirmCancellation());
        dialog.open();
    }

    /**
     * UC-003 steps 8 to 11, A7, A8.
     */
    private void confirmCancellation() {
        message.removeAll();
        try {
            OnlineCancellation cancellation = cancellationService.cancel(foundReservationCode, foundEmail);
            showReservation(cancellation.reservation());
            Paragraph cancelled = new Paragraph(cancellation.cancellationEmailAccepted()
                    ? CANCELLED : CANCELLED_EMAIL_DELAYED);
            cancelled.setId("cancelled-message");
            cancelled.getElement().setAttribute("role", "status");
            message.add(cancelled);
        } catch (ReservationNotCancellableException _) {
            // UC-003 A7: refused; continue at step 6 with the current data of the reservation
            lookupService.reload(foundReservationCode, foundEmail)
                    .ifPresentOrElse(this::showReservation, this::reservationGone);
        } catch (IllegalArgumentException _) {
            reservationGone();
        }
    }

    private void reservationGone() {
        reservationDetails.removeAll();
        message.add(errorMessage(NOT_FOUND));
    }

    // UC-003 A4, A5: tell the guest why the reservation cannot be cancelled online
    private void showWhyNotCancellable(OwnReservation reservation) {
        Paragraph reason = new Paragraph(reservation.status() == ReservationStatus.CONFIRMED
                ? DEADLINE_PASSED
                : notConfirmedMessage(reservation.status()));
        reason.setId("not-cancellable");
        reservationDetails.add(reason);
    }

    static String notConfirmedMessage(ReservationStatus status) {
        return "A reservation with status " + statusText(status) + " cannot be cancelled.";
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
