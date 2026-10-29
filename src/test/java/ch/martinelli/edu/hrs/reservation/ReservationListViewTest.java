package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ReservationListViewTest extends HrsBrowserlessTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private DSLContext dsl;

    private String mueller;
    private String meier;
    private String rossi;

    @BeforeEach
    void createReservationsAndNavigate() {
        meier = book("104", 5, 7, "Hans Meier");
        mueller = book("101", 1, 3, "Anna Müller");
        rossi = book("105", 2, 4, "Carla Rossi");
        dsl.update(RESERVATION).set(RESERVATION.STATUS, "CANCELLED")
                .where(RESERVATION.BOOKING_NUMBER.eq(rossi)).execute();
        navigate(ReservationListView.class);
    }

    @Test
    void listsUpcomingConfirmedReservationsSortedByArrival() {
        var grid = test(grid());

        assertThat(grid.size()).isEqualTo(2);
        assertThat(grid.getCellText(0, 0)).isEqualTo(mueller);
        assertThat(grid.getCellText(0, 1)).isEqualTo("Anna Müller");
        assertThat(grid.getCellText(0, 2)).isEqualTo("101");
        assertThat(grid.getCellText(0, 3)).isEqualTo(day(1).toString());
        assertThat(grid.getCellText(0, 4)).isEqualTo(day(3).toString());
        assertThat(grid.getCellText(0, 5)).isEqualTo("1");
        assertThat(grid.getCellText(0, 6)).isEqualTo("240.00");
        assertThat(grid.getCellText(0, 7)).isEqualTo("CONFIRMED");
        assertThat(grid.getCellText(1, 0)).isEqualTo(meier);
    }

    @Test
    void filtersByBookingNumberInAnyStatus() {
        test(textField("Booking number")).setValue(rossi.toLowerCase());

        var grid = test(grid());
        assertThat(grid.size()).isEqualTo(1);
        assertThat(grid.getCellText(0, 0)).isEqualTo(rossi);
        assertThat(grid.getCellText(0, 7)).isEqualTo("CANCELLED");
    }

    @Test
    void filtersByPartOfGuestNameIgnoringCase() {
        test(textField("Guest name")).setValue("mül");

        var grid = test(grid());
        assertThat(grid.size()).isEqualTo(1);
        assertThat(grid.getCellText(0, 1)).isEqualTo("Anna Müller");
    }

    @Test
    void filtersByStaysOverlappingDateRange() {
        test($view(DatePicker.class).withLabel("From").single()).setValue(day(6));
        test($view(DatePicker.class).withLabel("To").single()).setValue(day(8));

        var grid = test(grid());
        assertThat(grid.size()).isEqualTo(1);
        assertThat(grid.getCellText(0, 0)).isEqualTo(meier);
    }

    @Test
    void showCancelledIncludesCancelledReservations() {
        test($view(Checkbox.class).withLabel("Show cancelled").single()).click();

        var grid = test(grid());
        assertThat(grid.size()).isEqualTo(3);
        assertThat(grid.getCellText(1, 0)).isEqualTo(rossi);
    }

    @Test
    void showsMessageWhenNothingMatches() {
        assertThat($view(Span.class).withText("No reservations found").exists()).isFalse();

        test(textField("Guest name")).setValue("zzz");

        assertThat(test(grid()).size()).isZero();
        assertThat(noReservations().isVisible()).isTrue();
    }

    private String book(String roomNumber, int arrivalInDays, int departureInDays, String name) {
        long roomId = dsl.select(ROOM.ID).from(ROOM).where(ROOM.NUMBER.eq(roomNumber)).fetchSingle(ROOM.ID);
        return reservationService.createReservation(roomId, new StayPeriod(day(arrivalInDays), day(departureInDays)),
                1, new Guest(name, "guest@example.com", "000"));
    }

    private TextField textField(String label) {
        return $view(TextField.class).withLabel(label).single();
    }

    private Span noReservations() {
        return $view(Span.class).withText("No reservations found").single();
    }

    @SuppressWarnings("unchecked")
    private Grid<ReservationListItem> grid() {
        return $view(Grid.class).single();
    }

    private static LocalDate day(int inDays) {
        return LocalDate.now().plusDays(inDays);
    }
}
