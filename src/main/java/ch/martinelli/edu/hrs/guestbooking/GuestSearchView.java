package ch.martinelli.edu.hrs.guestbooking;

import ch.martinelli.edu.hrs.reservation.StayPeriod;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Public entry page for guests. Not in the staff layout, so no staff navigation is reachable from here.
 */
@PageTitle("Book a room")
@Route(value = "booking", autoLayout = false)
public class GuestSearchView extends VerticalLayout {

    private final GuestBookingService guestBookingService;
    private final Clock clock;

    private final DatePicker arrival = new DatePicker("Arrival");
    private final DatePicker departure = new DatePicker("Departure");
    private final IntegerField guests = new IntegerField("Guests");
    private final Grid<RoomTypeAvailability> grid = new Grid<>();
    private final Span noRooms = new Span("No rooms available for these dates");
    // validation only: values are read from the fields when searching
    private final Binder<Object> binder = new Binder<>();

    // the criteria the shown results belong to; the fields may have been edited since
    private StayPeriod searchedPeriod;
    private int searchedGuests;

    public GuestSearchView(GuestBookingService guestBookingService, Clock clock) {
        this.guestBookingService = guestBookingService;
        this.clock = clock;
        setSizeFull();

        var today = LocalDate.now(clock);
        arrival.setValue(today);
        departure.setValue(today.plusDays(1));
        guests.setValue(1);
        bindFields();

        var search = new Button("Search", _ -> search());
        search.addThemeVariants(ButtonVariant.PRIMARY);
        var criteria = new HorizontalLayout(arrival, departure, guests, search);
        criteria.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        criteria.setWrap(true);

        grid.addColumn(RoomTypeAvailability::roomTypeName).setHeader("Room type");
        grid.addColumn(RoomTypeAvailability::capacity).setHeader("Capacity");
        grid.addColumn(RoomTypeAvailability::nightlyRate).setHeader("Nightly rate (CHF)");
        grid.addColumn(RoomTypeAvailability::totalPrice).setHeader("Total (CHF)");
        grid.addColumn(RoomTypeAvailability::freeRooms).setHeader("Free rooms");
        grid.addComponentColumn(roomType -> new Button("Book", _ -> book(roomType))).setHeader("");
        grid.setItems(List.of());

        noRooms.setVisible(false);
        add(new H1("Book a room"), new RouterLink("View or cancel my reservation", GuestReservationView.class),
                criteria, noRooms, grid);
    }

    private void bindFields() {
        binder.forField(arrival)
                .asRequired("Arrival is required")
                .withValidator(d -> !d.isBefore(LocalDate.now(clock)), "Arrival must be today or later")
                .bind(_ -> null, (_, _) -> {
                });
        binder.forField(departure)
                .asRequired("Departure is required")
                .withValidator(d -> arrival.getValue() == null || d.isAfter(arrival.getValue()),
                        "Departure must be after arrival")
                .bind(_ -> null, (_, _) -> {
                });
        binder.forField(guests)
                .asRequired("Guests is required")
                .withValidator(n -> n >= 1, "At least 1 guest")
                .bind(_ -> null, (_, _) -> {
                });
    }

    private void search() {
        if (!binder.validate().isOk()) {
            grid.setItems(List.of());
            noRooms.setVisible(false);
            return;
        }
        searchedPeriod = new StayPeriod(arrival.getValue(), departure.getValue());
        searchedGuests = guests.getValue();
        var roomTypes = guestBookingService.searchByRoomType(searchedPeriod, searchedGuests);
        grid.setItems(roomTypes);
        noRooms.setVisible(roomTypes.isEmpty());
    }

    private void book(RoomTypeAvailability roomType) {
        new GuestBookingDialog(guestBookingService, roomType, searchedPeriod, searchedGuests, this::search).open();
    }
}
