package ch.martinelli.edu.hrs.management.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import ch.martinelli.edu.hrs.management.RoomTypeData;
import ch.martinelli.edu.hrs.management.RoomTypeOverview;
import ch.martinelli.edu.hrs.management.RoomTypeService;
import ch.martinelli.edu.hrs.reservation.ReceptionReservationService;
import ch.martinelli.edu.hrs.reservation.ReceptionRoomType;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLink;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Use case test for UC-016 "Maintain Room Types". Test data: V114__uc016_test_data.sql (inactive Garden Room with two
 * active rooms), room types from V100__uc001_test_data.sql; staff members from V103__uc022_test_data.sql (password
 * Secret-123). Today is 1 June 2030.
 */
@WithAnonymousUser
class UC016MaintainRoomTypesTest extends HrsBrowserlessTest {

    /** Rows created by the tests get sequence values below the IDs of the test data. */
    private static final long FIRST_TEST_DATA_ID = 1001;
    private static final long GARDEN_ROOM = 2601;

    @TestBean
    Clock clock;

    @Autowired
    ReceptionReservationService receptionReservationService;

    @Autowired
    RoomTypeService roomTypeService;

    @Autowired
    DSLContext ctx;

    private Result<RoomTypeRecord> originalRoomTypes;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-06-01T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    @BeforeEach
    void rememberTestData() {
        originalRoomTypes = ctx.selectFrom(ROOM_TYPE).where(ROOM_TYPE.ID.ge(FIRST_TEST_DATA_ID)).fetch();
    }

    @AfterEach
    void restoreTestData() {
        SecurityContextHolder.clearContext();
        ctx.deleteFrom(ROOM_TYPE).where(ROOM_TYPE.ID.lt(FIRST_TEST_DATA_ID)).execute();
        originalRoomTypes.forEach(original -> {
            original.changed(true);
            original.changed(ROOM_TYPE.ID, false);
            ctx.update(ROOM_TYPE).set(original).where(ROOM_TYPE.ID.eq(original.getId())).execute();
        });
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-016", businessRules = {"BR-001"})
    void manager_creates_a_new_room_type() {
        logIn("mark");

        // Step 1
        test($(RouterLink.class).withText("Room types").single()).click();
        assertThat(getCurrentView()).isInstanceOf(RoomTypesView.class);

        // Step 2: name, maximum number of guests, nightly rate, number of active rooms and whether active
        Grid<RoomTypeOverview> grid = grid();
        assertThat(grid.getColumns()).extracting(Grid.Column::getHeaderText)
                .containsExactly("Name", "Max. guests", "Nightly rate", "Active rooms", "Status");
        assertThat(row("Double Room")).containsExactly("Double Room", "2", "CHF 150.00", "2", "Active");
        assertThat(row("Garden Room")).containsExactly("Garden Room", "3", "CHF 200.00", "2", "Inactive");
        assertThat(row("Suite")).containsExactly("Suite", "4", "CHF 400.00", "1", "Inactive");

        // Step 3
        test($(Button.class).withText("New room type").single()).click();
        // Step 4
        enter("Penthouse", "The top floor with a terrace", 4, "550.00", true);
        // Step 5
        save();

        // Step 8: the updated list
        assertThat(message()).isEqualTo("Room type Penthouse is saved.");
        assertThat(row("Penthouse")).containsExactly("Penthouse", "4", "CHF 550.00", "0", "Active");

        // Step 7 / success postcondition
        RoomTypeRecord saved = ctx.selectFrom(ROOM_TYPE).where(ROOM_TYPE.NAME.eq("Penthouse")).fetchSingle();
        assertThat(saved.getDescription()).isEqualTo("The top floor with a terrace");
        assertThat(saved.getMaxGuests()).isEqualTo(4);
        assertThat(saved.getNightlyRate()).isEqualByComparingTo("550.00");
        assertThat(saved.getActive()).isTrue();
    }

    @Test
    @UseCase(id = "UC-016", businessRules = {"BR-002", "BR-004"})
    void changed_room_type_is_offered_with_the_saved_data_and_existing_prices_stay() {
        // The inactive Garden Room is not offered
        assertThat(offer("Garden Room")).isEmpty();
        openRoomTypes();

        // Step 3: select an existing room type; step 4: new nightly rate and active
        select("Garden Room");
        assertThat(nameField().getValue()).isEqualTo("Garden Room");
        test(nightlyRateField()).setValue(new BigDecimal("220.00"));
        test($(Checkbox.class).withCaption("Active").single()).click();
        save();

        assertThat(row("Garden Room")).containsExactly("Garden Room", "3", "CHF 220.00", "2", "Active");
        // New searches use the saved data: 2 nights at 220.00
        ReceptionRoomType offered = offer("Garden Room").orElseThrow();
        assertThat(offered.nightlyRate()).isEqualByComparingTo("220.00");
        assertThat(offered.totalPrice()).isEqualByComparingTo("440.00");
        // BR-002: total prices of existing reservations are unchanged
        assertThat(reservation("GARDEN0001").getTotalPrice()).isEqualByComparingTo("400.00");
    }

    @Test
    @UseCase(id = "UC-016", businessRules = {"BR-004"})
    void deactivated_room_type_is_no_longer_offered_and_its_reservations_stay_valid() {
        ctx.update(ROOM_TYPE).set(ROOM_TYPE.ACTIVE, true).where(ROOM_TYPE.ID.eq(GARDEN_ROOM)).execute();
        assertThat(offer("Garden Room")).isPresent();
        openRoomTypes();

        select("Garden Room");
        test($(Checkbox.class).withCaption("Active").single()).click();
        save();

        assertThat(row("Garden Room")).last().isEqualTo("Inactive");
        assertThat(offer("Garden Room")).isEmpty();
        // Room types are never deleted, and the reservation stays valid
        assertThat(ctx.fetchExists(ROOM_TYPE, ROOM_TYPE.ID.eq(GARDEN_ROOM))).isTrue();
        assertThat(reservation("GARDEN0001").getStatus()).isEqualTo("CONFIRMED");
        assertThat($(Button.class).withText("Delete").exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Invalid values
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-016", scenario = "A1: Invalid Values")
    void missing_name_and_nightly_rate_are_marked_and_the_entered_values_are_kept() {
        openRoomTypes();
        test($(Button.class).withText("New room type").single()).click();
        test(maxGuestsField()).setValue(2);
        test($(TextArea.class).withCaption("Description").single()).setValue("Kept");

        save();

        assertThat(nameField().isInvalid()).isTrue();
        assertThat(nightlyRateField().isInvalid()).isTrue();
        assertThat(maxGuestsField().isInvalid()).isFalse();
        // The entered values are kept
        assertThat(maxGuestsField().getValue()).isEqualTo(2);
        assertThat($(TextArea.class).withCaption("Description").single().getValue()).isEqualTo("Kept");
        assertThat(ctx.fetchCount(ROOM_TYPE, ROOM_TYPE.ID.lt(FIRST_TEST_DATA_ID))).isZero();
    }

    @Test
    @UseCase(id = "UC-016", scenario = "A1: Invalid Values", businessRules = {"BR-003"})
    void values_outside_the_allowed_ranges_are_marked() {
        openRoomTypes();
        test($(Button.class).withText("New room type").single()).click();

        // Name and description cannot get longer than 50 and 500 characters: the fields take no more
        assertThat(nameField().getMaxLength()).isEqualTo(50);
        assertThat($(TextArea.class).withCaption("Description").single().getMaxLength()).isEqualTo(500);
        assertThatThrownBy(() -> roomTypeService.save(null,
                new RoomTypeData("N".repeat(51), null, 2, BigDecimal.ONE, true)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> roomTypeService.save(null,
                new RoomTypeData("Long", "D".repeat(501), 2, BigDecimal.ONE, true)))
                .isInstanceOf(IllegalArgumentException.class);

        enter("   ", "", 11, "-1.00", true);
        save();

        assertThat(nameField().isInvalid()).isTrue();
        assertThat(maxGuestsField().isInvalid()).isTrue();
        assertThat(maxGuestsField().getErrorMessage()).isEqualTo("The maximum number of guests must be between 1 and 10");
        assertThat(nightlyRateField().isInvalid()).isTrue();

        test(maxGuestsField()).setValue(0);
        save();
        assertThat(maxGuestsField().isInvalid()).isTrue();
        assertThat(ctx.fetchCount(ROOM_TYPE, ROOM_TYPE.ID.lt(FIRST_TEST_DATA_ID))).isZero();

        // A1 step 2: continues at step 4
        enter("N".repeat(50), "D".repeat(500), 10, "0.00", false);
        save();
        assertThat(message()).isEqualTo("Room type " + "N".repeat(50) + " is saved.");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Name already used
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-016", scenario = "A2: Name Already Used", businessRules = {"BR-001"})
    void manager_is_informed_when_the_name_is_already_used() {
        RoomTypeRecord gardenBefore = roomType(GARDEN_ROOM);
        openRoomTypes();
        select("Garden Room");

        test(nameField()).setValue("Double Room");
        save();

        assertThat(message()).isEqualTo(RoomTypesView.NAME_USED);
        assertThat(nameField().isInvalid()).isTrue();
        // Failure postcondition: the room type keeps its previous data
        assertThat(roomType(GARDEN_ROOM)).isEqualTo(gardenBefore);

        // A2 step 2: continues at step 4
        test(nameField()).setValue("Garden Suite");
        save();
        assertThat(roomType(GARDEN_ROOM).getName()).isEqualTo("Garden Suite");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A3: Maximum below existing reservations
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-016", scenario = "A3: Maximum Below Existing Reservations", businessRules = {"BR-003"})
    void maximum_cannot_be_set_below_the_guests_of_confirmed_reservations() {
        openRoomTypes();
        select("Garden Room");

        test(maxGuestsField()).setValue(2);
        save();

        // A3 step 1: the affected reservations are listed; the cancelled GARDEN0002 does not count
        assertThat(message()).isEqualTo("The maximum number of guests cannot be lower than the number of guests of "
                + "these reservations: GARDEN0001 (3 guests).");
        assertThat(roomType(GARDEN_ROOM).getMaxGuests()).isEqualTo(3);

        test(maxGuestsField()).setValue(1);
        save();
        assertThat(message()).contains("GARDEN0001 (3 guests), GARDEN0003 (2 guests)");
        // Failure postcondition: no existing reservation is changed
        assertThat(reservation("GARDEN0001").getNumberOfGuests()).isEqualTo(3);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A4: Manager discards the changes
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-016", scenario = "A4: Manager Discards the Changes")
    void entered_values_are_discarded_when_the_manager_leaves_the_room_type() {
        RoomTypeRecord gardenBefore = roomType(GARDEN_ROOM);
        openRoomTypes();
        select("Garden Room");
        test(nightlyRateField()).setValue(new BigDecimal("999.00"));

        test($(Button.class).withText("Discard").single()).click();

        assertThat(roomType(GARDEN_ROOM)).isEqualTo(gardenBefore);
        assertThat(row("Garden Room")).contains("CHF 200.00");
        assertThat($(TextField.class).withCaption("Name").exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Precondition: the manager is logged in (NFR-009)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-016")
    void only_a_logged_in_manager_can_maintain_room_types() {
        navigate("room-types", LoginView.class);

        logIn("rita");
        assertThat($(RouterLink.class).withText("Room types").exists()).isFalse();
        assertThat(navigate("room-types", RouteNotFoundError.class)).isNotNull();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    private void logIn(String username) {
        navigate(LoginView.class);
        test($(TextField.class).withCaption("Username").single()).setValue(username);
        test($(PasswordField.class).withCaption("Password").single()).setValue("Secret-123");
        test($(Button.class).withText("Log in").single()).click();
    }

    private void openRoomTypes() {
        logIn("mark");
        navigate(RoomTypesView.class);
    }

    @SuppressWarnings("unchecked")
    private Grid<RoomTypeOverview> grid() {
        return $(Grid.class).withId("room-types").single();
    }

    private int rowIndex(String name) {
        Grid<RoomTypeOverview> grid = grid();
        return IntStream.range(0, test(grid).size())
                .filter(index -> test(grid).getRow(index).name().equals(name))
                .findFirst().orElseThrow(() -> new AssertionError(name + " is not listed"));
    }

    private List<String> row(String name) {
        Grid<RoomTypeOverview> grid = grid();
        int index = rowIndex(name);
        return IntStream.range(0, grid.getColumns().size()).mapToObj(column -> test(grid).getCellText(index, column))
                .toList();
    }

    private void select(String name) {
        test(grid()).select(rowIndex(name));
    }

    private TextField nameField() {
        return $(TextField.class).withCaption("Name").single();
    }

    private IntegerField maxGuestsField() {
        return $(IntegerField.class).withCaption("Maximum number of guests").single();
    }

    private BigDecimalField nightlyRateField() {
        return $(BigDecimalField.class).withCaption("Nightly rate (CHF)").single();
    }

    private void enter(String name, String description, int maxGuests, String nightlyRate, boolean active) {
        test(nameField()).setValue(name);
        test($(TextArea.class).withCaption("Description").single()).setValue(description);
        test(maxGuestsField()).setValue(maxGuests);
        test(nightlyRateField()).setValue(new BigDecimal(nightlyRate));
        Checkbox activeBox = $(Checkbox.class).withCaption("Active").single();
        if (activeBox.getValue() != active) {
            test(activeBox).click();
        }
    }

    private void save() {
        test($(Button.class).withText("Save").single()).click();
    }

    private String message() {
        return $(Paragraph.class).withId("room-type-message").single().getText();
    }

    private Optional<ReceptionRoomType> offer(String name) {
        return receptionReservationService.searchAvailableRoomTypes(LocalDate.of(2030, 12, 20),
                        LocalDate.of(2030, 12, 22), 1).stream()
                .filter(roomType -> roomType.name().equals(name))
                .findFirst();
    }

    private RoomTypeRecord roomType(long id) {
        return ctx.selectFrom(ROOM_TYPE).where(ROOM_TYPE.ID.eq(id)).fetchSingle();
    }

    private ReservationRecord reservation(String reservationCode) {
        return ctx.selectFrom(RESERVATION).where(RESERVATION.RESERVATION_CODE.eq(reservationCode)).fetchSingle();
    }
}
