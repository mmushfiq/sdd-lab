package ch.martinelli.edu.hrs.frontdesk.ui;

import ch.martinelli.edu.hrs.core.ui.AccessDeniedView;
import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.frontdesk.FrontDeskService.RoomOverview;
import ch.martinelli.edu.hrs.housekeeping.HousekeepingService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.insert;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WithUserDetails("reception")
class FrontDeskViewTest extends HrsBrowserlessTest {

    static final LocalDate TODAY = LocalDate.now();
    static final int STATE = 2;
    static final int CLEANING = 3;
    static final int ACTION = 6;

    @Autowired
    DSLContext ctx;

    Grid<RoomOverview> grid;

    @SuppressWarnings("unchecked")
    private void open() {
        navigate(FrontDeskView.class);
        grid = $(Grid.class).id("front-desk-rooms");
    }

    // Scenarios "Free room", "Arrival today", "Occupied room" and "Dirty room"
    @Test
    void shows_states_guests_and_cleaning_status() {
        insert(ctx, "ANNA0001", "104", TODAY, TODAY.plusDays(2), "Anna Meier", "BOOKED");
        insert(ctx, "BEN00001", "105", TODAY.minusDays(1), TODAY.plusDays(1), "Ben Keller", "CHECKED_IN");
        setCleaning(HousekeepingService.DIRTY, "106");
        open();

        assertThat(row("101")).isEqualTo("101|Single|Free|Clean||");
        assertThat(row("104")).isEqualTo("104|Double|Arrival today|Clean|Anna Meier|" + TODAY.plusDays(2));
        assertThat(row("105")).isEqualTo("105|Double|Occupied|Clean|Ben Keller|" + TODAY.plusDays(1));
        assertThat(cell("106", CLEANING)).isEqualTo("Dirty");
        assertThat(action("104").getText()).isEqualTo("Check in");
        assertThat(action("105").getText()).isEqualTo("Check out");
    }

    // Scenario "Successful check-in"
    @Test
    void check_in_moves_room_to_occupied() {
        ReservationRecord r = insert(ctx, "ANNA0001", "104", TODAY, TODAY.plusDays(2), "Anna Meier", "BOOKED");
        open();

        test(action("104")).click();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(cell("104", STATE)).isEqualTo("Occupied");
        assertThat(status(r)).isEqualTo("CHECKED_IN");
        assertThat(action("104").getText()).isEqualTo("Check out");
    }

    // Scenario "Room is not clean"
    @Test
    void check_in_on_dirty_room_shows_message() {
        ReservationRecord r = insert(ctx, "ANNA0001", "104", TODAY, TODAY.plusDays(2), "Anna Meier", "BOOKED");
        setCleaning(HousekeepingService.DIRTY, "104");
        open();

        test(action("104")).click();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(test($(Notification.class).last()).getText()).isEqualTo("The room is not clean yet");
        assertThat(status(r)).isEqualTo("BOOKED");
        assertThat(cell("104", STATE)).isEqualTo("Arrival today");
    }

    // Scenario "Successful check-out"
    @Test
    void check_out_shows_room_free_and_dirty() {
        ReservationRecord r = insert(ctx, "BEN00001", "105", TODAY.minusDays(1), TODAY.plusDays(1), "Ben Keller",
                "CHECKED_IN");
        open();

        test(action("105")).click();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(cell("105", STATE)).isEqualTo("Free");
        assertThat(cell("105", CLEANING)).isEqualTo("Dirty");
        assertThat(status(r)).isEqualTo("CHECKED_OUT");
    }

    // Scenarios "Not the arrival date" and "Only Checked in reservations can be checked out"
    @Test
    void no_check_in_for_arrival_tomorrow() {
        insert(ctx, "ANNA0001", "104", TODAY.plusDays(1), TODAY.plusDays(3), "Anna Meier", "BOOKED");
        open();

        assertThat(cell("104", STATE)).isEqualTo("Free");
        assertThat(test(grid).getCellComponent(rowOf("104"), ACTION)).isNotInstanceOf(Button.class);
    }

    // Scenario "Housekeeping opens the room overview"
    @Test
    @WithUserDetails("housekeeping")
    void housekeeping_gets_access_denied() {
        navigate(FrontDeskView.ROUTE, AccessDeniedView.class);

        assertThat($(H1.class).single().getText()).isEqualTo(AccessDeniedView.TEXT);
        assertThat($(Grid.class).exists()).isFalse();
    }

    private void setCleaning(String status, String... rooms) {
        ctx.update(ROOM).set(ROOM.CLEANING_STATUS, status).where(ROOM.ROOM_NUMBER.in(rooms)).execute();
    }

    private String status(ReservationRecord r) {
        return ctx.select(RESERVATION.STATUS).from(RESERVATION).where(RESERVATION.ID.eq(r.getId()))
                .fetchSingle(RESERVATION.STATUS);
    }

    private int rowOf(String roomNumber) {
        return IntStream.range(0, test(grid).size())
                .filter(r -> test(grid).getRow(r).roomNumber().equals(roomNumber)).findFirst().orElseThrow();
    }

    private String cell(String roomNumber, int column) {
        return ((Span) test(grid).getCellComponent(rowOf(roomNumber), column)).getText();
    }

    private Button action(String roomNumber) {
        return (Button) test(grid).getCellComponent(rowOf(roomNumber), ACTION);
    }

    private String row(String roomNumber) {
        int i = rowOf(roomNumber);
        return String.join("|", test(grid).getCellText(i, 0), test(grid).getCellText(i, 1), cell(roomNumber, STATE),
                cell(roomNumber, CLEANING), test(grid).getCellText(i, 4), test(grid).getCellText(i, 5));
    }
}
