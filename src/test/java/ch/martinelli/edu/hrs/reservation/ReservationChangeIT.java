package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.security.Role;
import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import ch.martinelli.edu.hrs.staff.StaffUserService;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.db.Tables.STAFF_USER;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class ReservationChangeIT extends PlaywrightIT {

    private static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    @Autowired
    private DSLContext dsl;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private StaffUserService staffUserService;

    @Autowired
    private Clock clock;

    private long roomId;
    private LocalDate arrival;
    private ReservationRecord reservation;

    @BeforeEach
    void createData() {
        var type = roomTypeService.save(roomType("IT Change Double", 2, "100.00"));
        var room = new RoomRecord();
        room.setRoomNumber("IT-601");
        room.setRoomTypeId(type.getId());
        roomId = roomService.save(room).getId();
        arrival = LocalDate.now(clock).plusDays(14);
        reservation = reservationService.book(new BookingRequest(roomId, arrival, arrival.plusDays(2), 2,
                "IT Change Guest", "it.change@example.com", "+41 79 555 55 55", BookingSource.RECEPTION));
        staffUserService.create("it-rosa", Role.RECEPTIONIST, "secret123");
    }

    @AfterEach
    void cleanUp() {
        dsl.deleteFrom(RESERVATION).where(RESERVATION.ROOM_ID.eq(roomId)).execute();
        dsl.deleteFrom(ROOM).where(ROOM.ID.eq(roomId)).execute();
        dsl.deleteFrom(ROOM_TYPE).where(ROOM_TYPE.NAME.eq("IT Change Double")).execute();
        dsl.deleteFrom(STAFF_USER).where(STAFF_USER.USERNAME.eq("it-rosa")).execute();
    }

    @Test
    void changeDatesThenCancelAndRoomIsOfferedAgain() {
        logIn("it-rosa", "secret123");
        page.navigate(url("reception/reservations"));
        mopo.waitForConnectionToSettle();

        find(reservation.getReservationNumber());
        button("Change").click();
        var departure = page.locator("#change-departure input");
        departure.fill(US_DATE.format(arrival.plusDays(3)));
        departure.press("Enter");
        button("Save changes").click();
        assertThat(page.getByText("Reservation " + reservation.getReservationNumber() + " changed.")).isVisible();
        org.assertj.core.api.Assertions.assertThat(dsl.fetchSingle(RESERVATION,
                RESERVATION.ID.eq(reservation.getId())).getDepartureDate()).isEqualTo(arrival.plusDays(3));

        find(reservation.getReservationNumber());
        button("Cancel reservation").click();
        // The confirm dialog's button has the same name as the view's button, so pick it by its slot
        page.locator("vaadin-confirm-dialog vaadin-button[slot='confirm-button']").click();
        assertThat(page.getByText("Reservation " + reservation.getReservationNumber() + " cancelled.")).isVisible();

        fillDate("Arrival", arrival);
        fillDate("Departure", arrival.plusDays(3));
        page.getByLabel("Guests", new Page.GetByLabelOptions().setExact(true)).fill("2");
        button("Search").click();
        mopo.waitForConnectionToSettle();
        assertThat(page.locator("#available").getByText("IT-601")).isVisible();
    }

    private void find(String query) {
        page.getByLabel("Reservation number or guest name").fill(query);
        button("Find").click();
        mopo.waitForConnectionToSettle();
        page.locator("#found").getByText(reservation.getReservationNumber()).click();
        mopo.waitForConnectionToSettle();
    }

    private void fillDate(String label, LocalDate date) {
        var field = page.getByLabel(label, new Page.GetByLabelOptions().setExact(true));
        field.fill(US_DATE.format(date));
        field.press("Enter");
    }

    private Locator button(String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }

    private void logIn(String username, String password) {
        page.navigate(url("login"));
        mopo.waitForConnectionToSettle();
        page.locator("input[name='username']").fill(username);
        page.locator("input[name='password']").fill(password);
        button("Log in").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        mopo.waitForConnectionToSettle();
    }

    private String url(String route) {
        return "http://localhost:" + localServerPort + "/" + route;
    }
}
