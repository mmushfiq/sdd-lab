package ch.martinelli.edu.hrs.booking.ui;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.reservation.AvailabilityService;
import ch.martinelli.edu.hrs.reservation.AvailabilityService.AvailableRoom;
import ch.martinelli.edu.hrs.reservation.ReservationException;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.reservation.ReservationService.Guest;
import ch.martinelli.edu.hrs.reservation.ReservationService.Stay;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.List;

import static ch.martinelli.edu.hrs.room.ui.RoomTypeView.formatRate;

/**
 * Public booking page in three states on one page: search with result cards, guest form for the chosen room, and
 * the confirmation. Guests see room types, never room numbers.
 */
@Route(value = BookingView.ROUTE, layout = GuestLayout.class)
@PageTitle("Book a room")
@AnonymousAllowed
public class BookingView extends VerticalLayout {

    public static final String ROUTE = "";
    public static final String PRIVACY_NOTICE =
            "We use your name, email and phone number only to manage your reservation.";
    public static final String INVALID_EMAIL = "Enter a valid email address";
    public static final String DATES_REQUIRED = "Arrival and departure date are required";
    public static final String KEEP_NUMBER =
            "Please keep your reservation number. You need it with your email to view or cancel your reservation.";

    private final AvailabilityService availabilityService;
    private final ReservationService reservationService;

    private final DatePicker arrival = new DatePicker("Arrival");
    private final DatePicker departure = new DatePicker("Departure");
    private final IntegerField guests = new IntegerField("Guests");
    private final Div searchStep = new Div();
    private final Span message = new Span();
    private final FlexLayout results = new FlexLayout();
    private final Div guestStep = new Div();
    private final Paragraph chosenRoom = new Paragraph();
    private final TextField guestName = new TextField("Name");
    private final TextField guestEmail = new TextField("Email");
    private final TextField guestPhone = new TextField("Phone (optional)");
    private final Binder<ReservationRecord> guestBinder = new Binder<>();
    private final Div confirmation = new Div();

    private Stay searched;
    private AvailableRoom chosen;

    public BookingView(AvailabilityService availabilityService, ReservationService reservationService) {
        this.availabilityService = availabilityService;
        this.reservationService = reservationService;

        guests.setMin(1);
        guests.setStepButtonsVisible(true);
        arrival.setValue(availabilityService.today());
        departure.setValue(arrival.getValue().plusDays(1));
        guests.setValue(1);
        Button search = new Button("Search", _ -> search());
        search.setId("search-rooms");
        search.addThemeVariants(ButtonVariant.PRIMARY);
        FormLayout searchForm = new FormLayout(arrival, departure, guests);
        searchStep.add(new H2("Book a room"), searchForm, search);

        message.setId("booking-message");
        results.setId("room-results");
        results.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        results.getStyle().set("gap", "var(--lumo-space-m)");

        guestName.setMaxLength(100);
        guestEmail.setMaxLength(254);
        guestPhone.setMaxLength(30);
        guestBinder.forField(guestName).asRequired("Name is required")
                .withValidator(v -> !v.isBlank(), "Name is required")
                .bind(ReservationRecord::getGuestName, ReservationRecord::setGuestName);
        guestBinder.forField(guestEmail).asRequired("Email is required")
                .withValidator(new EmailValidator(INVALID_EMAIL))
                .bind(ReservationRecord::getGuestEmail, ReservationRecord::setGuestEmail);
        guestBinder.forField(guestPhone)
                .bind(ReservationRecord::getGuestPhone, ReservationRecord::setGuestPhone);
        Paragraph privacy = new Paragraph(PRIVACY_NOTICE);
        privacy.setId("privacy-notice");
        chosenRoom.setId("chosen-room");
        Button book = new Button("Book", _ -> book());
        book.setId("book");
        book.addThemeVariants(ButtonVariant.PRIMARY);
        guestStep.add(new H3("Your details"), chosenRoom, new FormLayout(guestName, guestEmail, guestPhone),
                privacy, book);
        guestStep.setVisible(false);

        confirmation.setId("booking-confirmation");
        confirmation.setVisible(false);

        add(searchStep, message, results, guestStep, confirmation);
    }

    private void search() {
        searched = null;
        chosen = null;
        results.removeAll();
        guestStep.setVisible(false);
        if (arrival.isEmpty() || departure.isEmpty()) {
            message.setText(DATES_REQUIRED);
            return;
        }
        List<AvailableRoom> found;
        try {
            found = availabilityService.findAvailableRooms(arrival.getValue(), departure.getValue(),
                    guests.getValue(), null);
        } catch (ReservationException e) {
            message.setText(e.getMessage());
            return;
        }
        if (found.isEmpty()) {
            message.setText(AvailabilityService.NO_ROOMS);
            return;
        }
        message.setText("");
        searched = new Stay(0, arrival.getValue(), departure.getValue(), guests.getValue());
        found.forEach(room -> results.add(card(room)));
    }

    private Div card(AvailableRoom room) {
        Button choose = new Button("Choose", _ -> choose(room));
        choose.addClassName("choose-room");
        Div card = new Div(new H3(room.roomTypeName()),
                new Paragraph("Up to " + room.capacity() + " guests"),
                new Paragraph("Nightly rate: " + formatRate(room.nightlyRate())),
                new Paragraph("Total price: " + formatRate(room.totalPrice())),
                choose);
        card.addClassName("room-card");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-m)")
                .set("flex", "1 1 14rem");
        return card;
    }

    private void choose(AvailableRoom room) {
        chosen = room;
        chosenRoom.setText(room.roomTypeName() + ", " + searched.arrival() + " to " + searched.departure() + ", "
                + searched.guests() + " guests, total " + formatRate(room.totalPrice()));
        guestStep.setVisible(true);
    }

    private void book() {
        ReservationRecord form = new ReservationRecord();
        if (chosen == null || !guestBinder.writeBeanIfValid(form)) {
            return;
        }
        Stay stay = new Stay(chosen.roomId(), searched.arrival(), searched.departure(), searched.guests());
        ReservationRecord created;
        try {
            created = reservationService.create(stay,
                    new Guest(form.getGuestName(), form.getGuestEmail(), form.getGuestPhone()));
        } catch (ReservationException e) {
            // Back to the results of the same search; the search clears the message, so set it afterwards
            arrival.setValue(stay.arrival());
            departure.setValue(stay.departure());
            guests.setValue(stay.guests());
            search();
            message.setText(e.getMessage());
            return;
        }
        showConfirmation(created);
    }

    private void showConfirmation(ReservationRecord created) {
        searchStep.setVisible(false);
        results.setVisible(false);
        guestStep.setVisible(false);
        message.setText("");
        Span number = new Span(created.getReservationNumber());
        number.setId("reservation-number");
        number.getStyle().set("font-weight", "bold");
        confirmation.add(new H2("Thank you, your room is booked"),
                new Paragraph(new Span("Reservation number: "), number),
                new Paragraph("Room type: " + chosen.roomTypeName()),
                new Paragraph("Arrival: " + created.getArrivalDate()),
                new Paragraph("Departure: " + created.getDepartureDate()),
                new Paragraph("Guests: " + created.getNumberOfGuests()),
                new Paragraph("Total price: " + formatRate(AvailabilityService.totalPrice(created.getNightlyRate(),
                        created.getArrivalDate(), created.getDepartureDate()))),
                new Paragraph(KEEP_NUMBER));
        confirmation.setVisible(true);
    }
}
